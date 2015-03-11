package it.quartara.boser.servlet;

import it.quartara.boser.action.ActionException;
import it.quartara.boser.action.handlers.ActionHandler;
import it.quartara.boser.action.handlers.SearchResultPersisterHandler;
import it.quartara.boser.model.Index;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.Search;
import it.quartara.boser.model.SearchAction;
import it.quartara.boser.model.SearchConfig;
import it.quartara.boser.model.SearchKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Effettua una nuova ricerca
 * @author webny
 *
 */
@WebServlet("/search")
public class SearchServlet extends BoserServlet {

	/**	 */
	private static final long serialVersionUID = 2118191087236072826L;
	
	private static final Logger log = LoggerFactory.getLogger(SearchServlet.class);

	/*
	 * 1) recupera la configurazione di ricerca per l'utente (chiavi e azioni)
	 * 2) effettua la ricerca per ogni chiave (ed eventuali figli) (API SOLRJ)
	 * 3) applica le azioni sui risultati di ricerca (scrive su disco e su DB)
	 * 4) crea il file zip dei risultati
	 * 
	 * (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Date now = new Date();
		String searchConfigId = req.getParameter("searchConfigId");
		EntityManager em = getEntityManager();
		
		em.getTransaction().begin();
		
		SearchConfig searchConfig = em.find(SearchConfig.class, Long.valueOf(searchConfigId));
		Index currentIndex = getCurrentIndex(em);
		Search search = new Search();
		search.setConfig(searchConfig);
		search.setIndex(currentIndex);
		search.setTimestamp(now);
		em.persist(search);
		
		Parameter solrUrlParam = em.find(Parameter.class, "SOLR_URL");
		HttpSolrServer solr = new HttpSolrServer(solrUrlParam.getValue());
		ActionHandler handlers = null;
		try {
			handlers = createHandlerChain(searchConfig.getActions(), em);
		} catch (ClassNotFoundException | NoSuchMethodException
				| SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			log.error("errore durante la creazione della catena di handlers", e);
			em.getTransaction().rollback();
			em.close();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		for (SearchKey key : searchConfig.getKeys()) {
			/*
			 * TODO gestire chiavi figlie
			 */
			String queryText = key.getText();
			SolrQuery query = new SolrQuery();
			query.setQuery(queryText);
			QueryResponse queryResponse = null;
			try {
				log.debug("ricerca per chiave: "+key.getText());
				queryResponse = solr.query(query);
			} catch (SolrServerException e) {
				log.error("errore durante l'esecuzione della ricerca su Solr", e);
				em.getTransaction().rollback();
				em.close();
				throw new ServletException(e);
			}
			SolrDocumentList docList = queryResponse.getResults();
			
			if (!docList.isEmpty()) {
				try {
					log.debug("avvio gestione actions");
					handlers.handle(search, key, docList);
				} catch (ActionException e) {
					StringBuilder buffer = new StringBuilder();
					buffer.append("Si è verificato un errore durante l'esecuzione delle action ");
					buffer.append("per la chiave di ricerca "+key.getText());
					log.error(buffer.toString(), e);
					//continue; // or not continue???
					/*
					 * TODO impostare stato ERRORE per la ricerca??
					 */
				}
			}
		}
		/*
		 * TODO creazione del file zip e creazione oggetto Search
		 */
		Parameter param = em.find(Parameter.class, "SEARCH_REPO");
		String repo = param.getValue();
		String searchPath = repo+File.separator+searchConfig.getId()+File.separator+search.getId();
		File zipFile = null;
		try {
			zipFile = createZipFile(searchPath, now);
		} catch (IOException e) {
			log.error("errore durante la creazione del file zip", e);
			em.getTransaction().rollback();
			em.close();
			throw new ServletException(e);
		}
		search.setZipFilePath(zipFile.getAbsolutePath());
		em.merge(search);
		
		em.getTransaction().commit();
		em.close();
		
		RequestDispatcher rd = req.getRequestDispatcher("/searchHome");
		rd.forward(req, resp);
	}

	private Index getCurrentIndex(EntityManager em) {
		/*
		 * TODO rivedere e spostare la query
		 */
		String query = "from Index i where i.whenTerminated = "
				+ "(select max(whenTerminated) from Index)";
		TypedQuery<Index> index = em.createQuery(query, Index.class);
		return index.getSingleResult();
	}

	private ActionHandler createHandlerChain(Set<SearchAction> actions,	EntityManager em) 
			throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
				   IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ActionHandler firstHandler = null, currentHandler = null;
		for (SearchAction action : actions) {
			Class<?> handlerClass = Class.forName(action.getHandlerClass());
			Constructor<?> handlerConstructor = handlerClass.getConstructor(EntityManager.class);
			ActionHandler handler = (ActionHandler) handlerConstructor.newInstance(em);
			if (firstHandler == null) {
				firstHandler = handler;
			}
			if (currentHandler == null) {
				currentHandler = handler;
			} else {
				currentHandler.setNextHandler(handler);
				currentHandler = handler;
			}
		}
		currentHandler.setNextHandler(new SearchResultPersisterHandler(em));
		return firstHandler;
	}
	
	private File createZipFile(String searchPath, Date timestamp) throws IOException {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		File zipFile = new File(searchPath+File.separator+format.format(timestamp)+".zip");
		FileOutputStream fos = new FileOutputStream(zipFile);
		ZipOutputStream zos = new ZipOutputStream(fos);
		
		byte[] buffer = new byte[1024];
		File[] files = new File(searchPath).listFiles(new FilenameFilter(){
			
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".txt") || name.toLowerCase().endsWith(".pdf")
						|| name.toLowerCase().endsWith(".xls");
			}
		});
		for (File file : files) {
			ZipEntry ze = new ZipEntry(file.getName());
			zos.putNextEntry(ze);
			FileInputStream inputFile = new FileInputStream(file);
			int len;
			while ((len = inputFile.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}
			inputFile.close();
			zos.closeEntry();
		}
		zos.close();
		return zipFile;
	}

}

package it.quartara.boser.servlet;

import it.quartara.boser.action.ActionException;
import it.quartara.boser.action.handlers.ActionHandler;
import it.quartara.boser.action.handlers.PdfResultWriterHandler;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.Search;
import it.quartara.boser.model.SearchAction;
import it.quartara.boser.model.SearchConfig;
import it.quartara.boser.model.SearchKey;
import it.quartara.boser.model.SearchResult;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
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
		String searchConfigId = req.getParameter("searchConfigId");
		EntityManager em = getEntityManager();
		Parameter solrUrlParam = em.find(Parameter.class, "SOLR_URL");
		HttpSolrServer solr = new HttpSolrServer(solrUrlParam.getValue());
		SearchConfig searchConfig = em.find(SearchConfig.class, Long.valueOf(searchConfigId));
		ActionHandler handlers = createHandlerChain(searchConfig.getActions(), em);
		for (SearchKey key : searchConfig.getKeys()) {
			/*
			 * TODO gestire chiavi figlie
			 */
			String queryText = key.getText();
			SolrQuery query = new SolrQuery();
			query.setQuery(queryText);
			QueryResponse queryResponse = null;
			try {
				queryResponse = solr.query(query);
			} catch (SolrServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			SolrDocumentList docList = queryResponse.getResults();
			
			if (!docList.isEmpty()) {
				try {
					handlers.handle(searchConfig, key, docList);
				} catch (ActionException e) {
					StringBuilder buffer = new StringBuilder();
					buffer.append("Si è verificato un errore durante l'esecuzione delle action ");
					buffer.append("per la chiave di ricerca "+key.getText());
					log.error(buffer.toString(), e);
					//continue; // or not continue???
				}
			}
		}
		/*
		 * TODO creazione del file zip e creazione oggetto Search
		 */
		
		RequestDispatcher rd = req.getRequestDispatcher("/searchHome.jsp");
		rd.forward(req, resp);
		em.close();
	}

	private ActionHandler createHandlerChain(Set<SearchAction> actions,
			EntityManager em) {
		// TODO Auto-generated method stub
		return null;
	}

}

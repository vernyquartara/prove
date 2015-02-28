package it.quartara.boser.action.handlers;

import it.quartara.boser.action.ActionException;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.SearchConfig;
import it.quartara.boser.model.SearchKey;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.EntityManager;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * Scrive su file i risultati di ricerca.
 * Crea un file con il nome della chiave.
 * Il percorso del file viene letto da un Parameter con chiave SEARCH_REPO.
 * 
 * @author webny
 *
 */
public class TxtResultWriterHandler extends AbstractActionHandler {
	
	private static final String HEADER = "BOSER - Boring Search Engine\r\nRealizzato da Verny Quartara per CP Informatica\r\nwww.boring.it\r\nanno 2008\r\n\r\n";
	private static final String TITLE = "RISULTATI DELLA RICERCA\r\n";
	

	public TxtResultWriterHandler(EntityManager em) {
		super(em);
	}

	@Override
	public void handle(SearchConfig config, SearchKey key, SolrDocumentList documents) throws ActionException {
		Parameter param = em.find(Parameter.class, "SEARCH_REPO");
		String repo = param.getValue();
		Date now = new Date();
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		File repoDir = new File(repo+File.separator+config.getId()+File.separator+format.format(now));
		repoDir.mkdirs();
		File outputFile = new File(repoDir.getAbsolutePath()+File.separator+key.getText()+".txt");
		try {
			PrintWriter writer = new PrintWriter(outputFile);
			writer.println(HEADER);
			writer.println(TITLE);
			writer.println(documents.size()+" risultati per "+key.getText()+"\r\n");
			for (int i = 0; i < documents.size(); i++) {
				SolrDocument doc = documents.get(i);
				writer.println(i+1+")"+doc.getFieldValue("url"));
				writer.println(doc.getFieldValue("title")+"\r\n");
			}
			writer.close();
		} catch (IOException e) {
			String msg = "problema di scrittura file dei risultati";
			throw new ActionException(msg, e);
		}
	}

}

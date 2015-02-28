package it.quartara.boser.action.handlers;

import it.quartara.boser.model.SearchConfig;
import it.quartara.boser.model.SearchKey;
import it.quartara.boser.model.SearchResult;

import javax.persistence.EntityManager;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * Gestisce la persistenza dei risultati di ricerca.
 * @author webny
 *
 */
public class SearchResultPersisterHandler extends AbstractActionHandler {

	public SearchResultPersisterHandler(EntityManager em) {
		super(em);
	}

	@Override
	public void handle(SearchConfig config, SearchKey key, SolrDocumentList documents) {
		for (SolrDocument doc : documents) {
			SearchResult searchResult = new SearchResult();
			searchResult.setKey(key);
			searchResult.setUrl(doc.getFieldValue("url").toString());
			searchResult.setContent(doc.getFieldValue("content").toString());
			searchResult.setTitle(doc.getFieldValue("title").toString());
			em.persist(searchResult);
		}
	}


}

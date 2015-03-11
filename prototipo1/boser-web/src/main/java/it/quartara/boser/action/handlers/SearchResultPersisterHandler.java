package it.quartara.boser.action.handlers;

import static it.quartara.boser.model.IndexField.*;
import it.quartara.boser.model.Search;
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
	protected void execute(Search search, SearchKey key, SolrDocumentList documents) {
		for (SolrDocument doc : documents) {
			SearchResult searchResult = new SearchResult();
			searchResult.setSearch(search);
			searchResult.setKey(key);
			searchResult.setUrl((String) doc.getFieldValue(URL.toString()));
			//searchResult.setContent((String) doc.getFieldValue(CONTENT.toString()));
			searchResult.setTitle((String) doc.getFieldValue(TITLE.toString()));
			em.persist(searchResult);
		}
	}


}

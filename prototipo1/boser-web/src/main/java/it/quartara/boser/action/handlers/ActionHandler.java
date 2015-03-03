package it.quartara.boser.action.handlers;

import it.quartara.boser.action.ActionException;
import it.quartara.boser.model.Search;
import it.quartara.boser.model.SearchKey;

import org.apache.solr.common.SolrDocumentList;

public interface ActionHandler {

	void setNextHandler(ActionHandler nextHandler);
	void handle(Search search, SearchKey key, SolrDocumentList documents) throws ActionException;
}

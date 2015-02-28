package it.quartara.boser.action.handlers;

import it.quartara.boser.action.ActionException;
import it.quartara.boser.model.SearchConfig;
import it.quartara.boser.model.SearchKey;

import org.apache.solr.common.SolrDocumentList;

public interface ActionHandler {

	void setNextHandler(ActionHandler nextHandler);
	void handle(SearchConfig config, SearchKey key, SolrDocumentList documents) throws ActionException;
}

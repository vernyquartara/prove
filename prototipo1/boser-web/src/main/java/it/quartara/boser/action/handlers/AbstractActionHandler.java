package it.quartara.boser.action.handlers;

import it.quartara.boser.action.ActionException;
import it.quartara.boser.model.Search;
import it.quartara.boser.model.SearchKey;

import javax.persistence.EntityManager;

import org.apache.solr.common.SolrDocumentList;

abstract public class AbstractActionHandler implements ActionHandler {
	
	private ActionHandler nextHandler;
	
	protected EntityManager em;

	public AbstractActionHandler(ActionHandler nextHandler, EntityManager em) {
		super();
		this.nextHandler = nextHandler;
		this.em = em;
	}

	public AbstractActionHandler(EntityManager em) {
		super();
		this.em = em;
	}
	
	@Override
	public void handle(Search search, SearchKey key, SolrDocumentList documents) throws ActionException {
		/*
		 * TODO gestire le eccezioni in modo che l'elaborazione continui fino
		 * alla fine della catena
		 */
		this.execute(search, key, documents);
		if (nextHandler != null) {
			nextHandler.handle(search, key, documents);
		}
	}
	
	abstract protected void execute(Search search, SearchKey key, SolrDocumentList documents) throws ActionException;

	@Override
	public void setNextHandler(ActionHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
	
}

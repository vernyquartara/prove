package it.quartara.boser.action.handlers;

import javax.persistence.EntityManager;

abstract public class AbstractActionHandler implements ActionHandler {
	
	@SuppressWarnings("unused")
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
	public void setNextHandler(ActionHandler nextHandler) {
		this.nextHandler = nextHandler;
	}

}

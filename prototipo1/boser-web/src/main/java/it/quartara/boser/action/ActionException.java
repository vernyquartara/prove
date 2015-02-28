package it.quartara.boser.action;


public class ActionException extends Exception {

	/** */
	private static final long serialVersionUID = 7945677577503435835L;

	public ActionException(String msg, Exception e) {
		super(msg, e);
	}

	public ActionException(String msg) {
		super(msg);
	}

}

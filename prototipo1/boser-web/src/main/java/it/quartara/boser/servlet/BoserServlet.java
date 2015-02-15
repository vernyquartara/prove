package it.quartara.boser.servlet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServlet;

public abstract class BoserServlet extends HttpServlet {

	/** */
	private static final long serialVersionUID = -1859454805296449374L;

	protected EntityManager getEntityManager() {
		EntityManagerFactory emf =
		           (EntityManagerFactory)getServletContext().getAttribute("emf");
		EntityManager em = emf.createEntityManager();
		return em;
	}
}

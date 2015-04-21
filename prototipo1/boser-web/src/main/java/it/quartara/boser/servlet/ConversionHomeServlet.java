package it.quartara.boser.servlet;

import it.quartara.boser.model.PdfConversion;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/conversionHome")
public class ConversionHomeServlet extends BoserServlet {

	private static final long serialVersionUID = -2977049294995216216L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		EntityManager em = getEntityManager();
		List<PdfConversion> convertions = em.createQuery("from PdfConversion order by id desc", PdfConversion.class).getResultList();
		request.setAttribute("convertions", convertions);
		RequestDispatcher rd = request.getRequestDispatcher("/conversionHome.jsp");
		rd.forward(request, response);
		em.close();
	}

	/*
	 * richiamato nel caso in cui questa servlet venga invocata
	 * a partire da un RequestDispatcher.
	 * (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

}

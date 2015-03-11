package it.quartara.boser.servlet;

import it.quartara.boser.model.Search;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/searchHome")
public class SearchHomeServlet extends BoserServlet {

	/** */
	private static final long serialVersionUID = -2020345743148750301L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		EntityManager em = getEntityManager();
		List<Search> searches = em.createQuery("from Search", Search.class).getResultList();
		request.setAttribute("searches", searches);
		RequestDispatcher rd = request.getRequestDispatcher("/searchHome.jsp");
		rd.forward(request, response);
		em.close();
	}

}

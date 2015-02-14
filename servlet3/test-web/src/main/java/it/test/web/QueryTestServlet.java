package it.test.web;

import it.test.model.SearchKey;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/query")
public class QueryTestServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Obtain a database connection:
        EntityManagerFactory emf =
           (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        
        try {
            // Display the list of guests:
            List<SearchKey> scList = em.createQuery(
                "SELECT sc FROM SearchKey sc", SearchKey.class).getResultList();
            request.setAttribute("sclist", scList);
            
            request.getRequestDispatcher("/sclist.jsp").forward(request, response);
 
        } finally {
            // Close the database connection:
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
	}

}

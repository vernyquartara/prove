package it.quartara.boser.servlet;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import it.quartara.boser.model.Search;
import it.quartara.boser.model.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class SearchHomeServletTest {

	@Test
	public void test() throws ServletException, IOException {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		RequestDispatcher rd = mock(RequestDispatcher.class);
		EntityManager em = mock(EntityManager.class);
		@SuppressWarnings("unchecked")
		TypedQuery<Search> query = mock(TypedQuery.class);
		List<Search> searches = new ArrayList<Search>();
		searches.add(new Search());
		SearchHomeServlet servlet = spy(new SearchHomeServlet());
		
		doReturn(em).when(servlet).getEntityManager();
		when(em.createQuery(anyString(), eq(Search.class))).thenReturn(query);
		when(query.getResultList()).thenReturn(searches);
		when(request.getRequestDispatcher("/searchHome.jsp")).thenReturn(rd);
		
		servlet.doGet(request, response);
		
		verify(request).setAttribute("searches", searches);
		verify(rd).forward(request, response);
		verify(em).close();
	}
}

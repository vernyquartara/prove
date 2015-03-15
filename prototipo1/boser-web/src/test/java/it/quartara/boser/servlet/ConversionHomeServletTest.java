package it.quartara.boser.servlet;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import it.quartara.boser.model.PdfConversion;

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

public class ConversionHomeServletTest {

	@Test
	public void testDoGet() throws ServletException, IOException {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		RequestDispatcher rd = mock(RequestDispatcher.class);
		EntityManager em = mock(EntityManager.class);
		@SuppressWarnings("unchecked")
		TypedQuery<PdfConversion> query = mock(TypedQuery.class);
		List<PdfConversion> convertions = new ArrayList<PdfConversion>();
		convertions.add(new PdfConversion());
		
		ConversionHomeServlet servlet = spy(new ConversionHomeServlet());
		
		doReturn(em).when(servlet).getEntityManager();
		when(em.createQuery(anyString(), eq(PdfConversion.class))).thenReturn(query);
		when(query.getResultList()).thenReturn(convertions);
		when(request.getRequestDispatcher("/conversionHome.jsp")).thenReturn(rd);
		
		servlet.doGet(request, response);
		
		verify(request).setAttribute("convertions", convertions);
		verify(rd).forward(request, response);
		verify(em).close();
	}
}

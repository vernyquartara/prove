package it.quartara.boser.servlet;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import it.quartara.boser.model.Search;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class SearchDowloadServletTest {

	@Test
	public void test() throws ServletException, IOException {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		ServletOutputStream out = mock(ServletOutputStream.class);
		ServletContext context = mock(ServletContext.class);
		Long searchId = 10L;
		Search search  = new Search();
		String zipFilePath = "src/test/resources/searchResult.zip";
		search.setZipFilePath(zipFilePath);
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		SearchDownloadServlet servlet = spy(new SearchDownloadServlet());
		
		
		when(request.getParameter("searchId")).thenReturn(searchId.toString());
		doReturn(context).when(servlet).getServletContext();
		when(context.getAttribute("emf")).thenReturn(emf);
		when(emf.createEntityManager()).thenReturn(em);
		when(em.find(Search.class, searchId)).thenReturn(search);
		when(response.getOutputStream()).thenReturn(out);
		
		servlet.doGet(request, response);
		
		verify(response).setContentType("application/octet-stream");
		verify(response).setHeader("Content-Disposition","attachment;filename=searchResult.zip");
		verify(response).setContentLengthLong(13903L);
		verify(out, atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
		verify(out, times(1)).flush();
		verify(out, times(1)).close();
		verify(em).close();
	}

	
	/*
	@Test
	public void test() throws ServletException, IOException {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		ServletOutputStream out = mock(ServletOutputStream.class);
		ServletContext context = mock(ServletContext.class);
		Long searchConfigId = 10L;
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		SearchConfig searchConfig = createSearchConfig();
		SearchDownloadServlet servlet = spy(new SearchDownloadServlet());
		
		
		when(request.getParameter("searchConfigId")).thenReturn(searchConfigId.toString());
		doReturn(context).when(servlet).getServletContext();
		when(context.getAttribute("emf")).thenReturn(emf);
		when(emf.createEntityManager()).thenReturn(em);
		when(em.find(SearchConfig.class, searchConfigId)).thenReturn(searchConfig);
		//when(em.createQuery(anyString(), eq(SearchConfig.class))).thenReturn(query);
		
		
		when(response.getOutputStream()).thenReturn(out);
		//out.write(any(byte[].class), anyInt(), anyInt());
		
		servlet.doGet(request, response);
		
		assertThat(response.getContentType(), equalTo("application/octet-stream"));
		assertThat(response.getHeader("Content-Disposition"), equalTo("attachment;filename="));
		verify(request);
		verify(response);
		verify(out, atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
		verify(out, times(1)).flush();
		verify(out, times(1)).close();
	}

	private SearchConfig createSearchConfig() {
		// TODO Auto-generated method stub
		return null;
	}
	*/
}

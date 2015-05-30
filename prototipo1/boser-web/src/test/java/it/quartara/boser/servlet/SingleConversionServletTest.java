package it.quartara.boser.servlet;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.service.PdfConversionFactory;
import it.quartara.boser.service.PdfConversionService;

import java.io.File;
import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SingleConversionServlet.class, PdfConversionFactory.class})
public class SingleConversionServletTest {

	@Test
	public void testInvalidUrl() throws ServletException, IOException {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		String url = "foo";
		when(mockRequest.getParameter("url")).thenReturn(url);
		
		RequestDispatcher rd = mock(RequestDispatcher.class);
		when(mockRequest.getRequestDispatcher("/singleConversion.jsp")).thenReturn(rd);
		
		SingleConversionServlet servlet = new SingleConversionServlet();
		servlet.doPost(mockRequest, mockResponse);
		
		verify(mockRequest).setAttribute(eq("errorMsg"), notNull());
		verify(rd).forward(mockRequest, mockResponse);
	}
	
	@Test
	public void testValidUrl() throws ServletException, IOException {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		ServletOutputStream out = mock(ServletOutputStream.class);
		String url = "http://boser.quartara.it";
		when(mockRequest.getParameter("url")).thenReturn(url);
		when(mockResponse.getOutputStream()).thenReturn(out);
		
		Long crawlerId = 3L;
		String repo = "target/test-output/repo";
		Parameter param = new Parameter();
		param.setValue(repo);
		when(mockRequest.getParameter("crawlerId")).thenReturn(crawlerId.toString());
		
		RequestDispatcher rd = mock(RequestDispatcher.class);
		when(mockRequest.getRequestDispatcher("/conversionHome")).thenReturn(rd);
		
		ServletContext context = mock(ServletContext.class);
		when(mockRequest.getServletContext()).thenReturn(context);
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		when(em.find(Parameter.class, "SEARCH_REPO")).thenReturn(param);
		when(context.getAttribute("emf")).thenReturn(emf);
		
		PdfConversionService mockService = mock(PdfConversionService.class);
		mockStatic(PdfConversionFactory.class);
		when(PdfConversionFactory.create()).thenReturn(mockService);
		when(mockService.convertToPdf(anyString(), anyString(), anyString()))
			.thenReturn(new File("src/test/resources/prova.pdf"));
		
		SingleConversionServlet servlet = spy(new SingleConversionServlet());
		doReturn(em).when(servlet).getEntityManager();
		servlet.doPost(mockRequest, mockResponse);
		
		verify(mockResponse).setContentType("application/octet-stream");
		verify(mockResponse).setHeader("Content-Disposition","attachment;filename=prova.pdf");
		verify(mockResponse).setContentLengthLong(anyLong());
		verify(out, atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
		verify(out, times(1)).flush();
		verify(out, times(1)).close();
		verify(em).close();
	}
	
	
}

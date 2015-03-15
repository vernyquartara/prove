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
import it.quartara.boser.model.PdfConversion;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class ConversionDowloadServletTest {

	@Test
	public void test() throws ServletException, IOException {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		ServletOutputStream out = mock(ServletOutputStream.class);
		ServletContext context = mock(ServletContext.class);
		Long conversionId = 10L;
		PdfConversion conversion  = new PdfConversion();
		String filePath = "src/test/resources/searchResult.zip";
		conversion.setFilePath(filePath);
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		ConversionDownloadServlet servlet = spy(new ConversionDownloadServlet());
		
		
		when(request.getParameter("conversionId")).thenReturn(conversionId.toString());
		doReturn(context).when(servlet).getServletContext();
		when(context.getAttribute("emf")).thenReturn(emf);
		when(emf.createEntityManager()).thenReturn(em);
		when(em.find(PdfConversion.class, conversionId)).thenReturn(conversion);
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

}

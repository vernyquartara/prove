package it.quartara.boser.servlet;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import it.quartara.boser.model.Crawler;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.PdfConversion;
import it.quartara.boser.model.PdfConversionItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConversionServlet.class, WorkbookFactory.class})
@MockPolicy(Slf4jMockPolicy.class)
public class ConversionServletTest {

	/*
	 * testa lo scenario di successo in cui si devono convertire due pdf
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testHappyPath() throws Exception {
		Long crawlerId = 3L;
		String repo = "target/test-output/repo";
		Parameter param = new Parameter();
		param.setValue(repo);
		Crawler crawler = new Crawler();
		crawler.setId(crawlerId);
		
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("crawlerId")).thenReturn(crawlerId.toString());
		
		RequestDispatcher rd = mock(RequestDispatcher.class);
		when(request.getRequestDispatcher("/conversionHome")).thenReturn(rd);
		
		HttpServletResponse response = mock(HttpServletResponse.class);
		
		ServletContext context = mock(ServletContext.class);
		when(context.getAttribute("javax.servlet.context.tempdir")).thenReturn(new File("target/test-output"));
		
		ServletConfig config = mock(ServletConfig.class);
		when(config.getServletContext()).thenReturn(context);
		
		EntityManager em = mock(EntityManager.class);
		when(em.find(Parameter.class, "SEARCH_REPO")).thenReturn(param);
		when(em.find(Crawler.class, 3L)).thenReturn(crawler);

		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		doNothing().when(transaction).begin();
		doNothing().when(transaction).commit();

		List<FileItem> items = new ArrayList<FileItem>();
		FileItem item = mock(FileItem.class);
		when(item.getFieldName()).thenReturn("file");
		items.add(item);
		FileItem reqParam = mock(FileItem.class);
		when(reqParam.getFieldName()).thenReturn("crawlerId");
		when(reqParam.getString()).thenReturn(crawlerId.toString());
		items.add(reqParam);
		when(item.getName()).thenReturn("C:\\dir\\file.xls");
		
		Workbook wb = WorkbookFactory.create(new File("src/test/resources/land_rover.xls"));
		mockStatic(WorkbookFactory.class);
		when(WorkbookFactory.create(any(File.class))).thenReturn(wb);

		ServletFileUpload upload = mock(ServletFileUpload.class);
		when(upload.parseRequest(request)).thenReturn(items);

		PdfConversion conv = new PdfConversion();
		whenNew(PdfConversion.class).withNoArguments().thenReturn(conv);

		Date startDate = new GregorianCalendar(2015, 4, 12, 15, 11).getTime();
		whenNew(Date.class).withNoArguments().thenReturn(startDate);
		
		List<PdfConversionItem> pdfConversionItems = new ArrayList<PdfConversionItem>();
		whenNew(ArrayList.class).withNoArguments().thenReturn((ArrayList) pdfConversionItems);
		
		/*
		 * questi sono i link nel file xls in test/resources
		String url1 = "http://www.omniauto.it/magazine/28983/schumacher-marzo-persi-altri-10-kg";
		String url2 = "http://www.omniauto.it/magazine/28323/pedaggi-autostradali-tutti-i-rincari-2015";
		 */
		
		ConversionServlet servlet = spy(new ConversionServlet());
		
		when(servlet.getServletConfig()).thenReturn(config);
		when(servlet.getServletContext()).thenReturn(context);
		doReturn(em).when(servlet).getEntityManager();
		whenNew(ServletFileUpload.class).withAnyArguments().thenReturn(upload);
		
		servlet.doPost(request, response);
		
		assertEquals(2, pdfConversionItems.size());
		verify(item).write((File) any(File.class));
		assertEquals(ExecutionState.READY, conv.getState());
		assertNotNull(conv.getCreationDate());
		assertEquals("file.xls", conv.getXlsFileName());
		assertNotNull(conv.getDestDir());
		assertEquals(pdfConversionItems.size(), conv.getItems().size());
		verify(em).merge(conv);
		verify(em).persist(conv);
		verify(em).close();
		verify(response).sendRedirect("/conversionHome");
	}
	
}

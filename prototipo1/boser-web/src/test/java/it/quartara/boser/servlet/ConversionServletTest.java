package it.quartara.boser.servlet;


import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import it.quartara.boser.model.Crawler;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.PdfConversion;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.fop.svg.PDFTranscoder;
import org.fit.cssbox.demo.ImageRenderer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConversionServlet.class})
public class ConversionServletTest {

	@Test
	public void testDoGet() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		RequestDispatcher rd = mock(RequestDispatcher.class);
		EntityManager em = mock(EntityManager.class);
		EntityTransaction transaction = mock(EntityTransaction.class);
		String repo = "target/test-output/repo";
		Parameter param = new Parameter();
		param.setValue(repo);
		ServletConfig config = mock(ServletConfig.class);
		ServletContext context = mock(ServletContext.class);
		ServletFileUpload upload = mock(ServletFileUpload.class);
		Long crawlerId = 3L;
		Crawler crawler = new Crawler();
		crawler.setId(crawlerId);
		String pdfRepo = repo+"/"+crawlerId+"/pdfs";
		List<FileItem> items = new ArrayList<FileItem>();
		FileItem item = mock(FileItem.class);
		when(item.getFieldName()).thenReturn("file");
		items.add(item);
		FileItem reqParam = mock(FileItem.class);
		when(reqParam.getFieldName()).thenReturn("crawlerId");
		when(reqParam.getString()).thenReturn(crawlerId.toString());
		items.add(reqParam);
		List<String> urls = new ArrayList<String>();
		urls.add("http://www.omniauto.it/magazine/28961/school-snow-omniauto-2015-posizione-guida");
		urls.add("http://www.omniauto.it/magazine/28329/2014-da-record-per-fca-jeep-impenna");
		PdfConversion conv = new PdfConversion();
		File zipFile = mock(File.class);
		Date startDate = new GregorianCalendar(2015, 4, 12, 15, 11).getTime();
		
		ConversionServlet servlet = spy(new ConversionServlet());
		
		when(servlet.getServletConfig()).thenReturn(config);
		when(config.getServletContext()).thenReturn(context);
		when(context.getAttribute("javax.servlet.context.tempdir")).thenReturn(new File("target/test-output"));
		when(request.getParameter("crawlerId")).thenReturn(crawlerId.toString());
		whenNew(ServletFileUpload.class).withAnyArguments().thenReturn(upload);
		when(upload.parseRequest(request)).thenReturn(items);
		doReturn(em).when(servlet).getEntityManager();
		when(em.getTransaction()).thenReturn(transaction);
		doNothing().when(transaction).begin();
		doNothing().when(transaction).commit();
		when(em.find(Parameter.class, "SEARCH_REPO")).thenReturn(param);
		when(em.find(Crawler.class, 3L)).thenReturn(crawler);
		when(item.getName()).thenReturn("C:\\dir\\file.xls");
		whenNew(PdfConversion.class).withNoArguments().thenReturn(conv);
		doReturn(urls).when(servlet,"getUrls", eq(new File(pdfRepo+"/file.xls")));
		doReturn((short)2).when(servlet,"convertToPdf", pdfRepo+"/file", urls);
		doReturn(zipFile).when(servlet,"createZipFile", pdfRepo+"/file");
		when(zipFile.getAbsolutePath()).thenReturn(pdfRepo+"/file.zip");
		when(zipFile.length()).thenReturn(985997L);
		when(request.getRequestDispatcher("/conversionHome")).thenReturn(rd);
		whenNew(Date.class).withNoArguments().thenReturn(startDate);
		
		servlet.doPost(request, response);
		
		assertThat(conv.getFilePath(), equalTo(pdfRepo+"/file.zip"));
		assertThat(conv.getNumberOfLinks(), equalTo((short)2));
		assertThat(conv.getSize(), equalTo(985997L));
		assertThat(conv.getStartDate(), equalTo(startDate));
		assertThat(conv.getState(), equalTo(ExecutionState.STARTED));
		
		verify(item).write((File) any(File.class));
		verify(em).persist(conv);
		verify(em).close();
		verify(rd).forward(request, response);
	}
	
	@Test
	public void testGetUrls() throws Exception {
		File inputFile = new File("src/test/resources/land_rover.xls");
		ConversionServlet servlet = new ConversionServlet();
		List<String> urls = Whitebox.invokeMethod(servlet, "getUrls", inputFile);
		assertFalse(urls.isEmpty());
		assertThat(urls.size(), equalTo(2));
		assertThat(urls.get(0), equalTo("http://www.omniauto.it/magazine/28983/schumacher-marzo-persi-altri-10-kg"));
		assertThat(urls.get(1), equalTo("http://www.omniauto.it/magazine/28323/pedaggi-autostradali-tutti-i-rincari-2015"));
	}
	
	@Test
	public void testConvertToPdf() throws Exception {
		List<String> urls = new ArrayList<String>();
		urls.add("http://www.omniauto.it/magazine/28983/schumacher-marzo-persi-altri-10-kg");
		urls.add("http://www.omniauto.it/magazine/28323/pedaggi-autostradali-tutti-i-rincari-2015");
		String destDir = "target/test-output/pdfs";
		ImageRenderer imageRenderer = mock(ImageRenderer.class);
		PDFTranscoder transcoder = mock(PDFTranscoder.class);
		
		whenNew(ImageRenderer.class).withNoArguments().thenReturn(imageRenderer);
		whenNew(PDFTranscoder.class).withNoArguments().thenReturn(transcoder);
		whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mock(FileOutputStream.class));
		
		ConversionServlet servlet = new ConversionServlet();
		short result = Whitebox.invokeMethod(servlet, "convertToPdf", destDir, urls);
		assertEquals((short)2, result);
		
		verify(imageRenderer, times(2)).renderURL(any(String.class), any(ByteArrayOutputStream.class), eq(ImageRenderer.Type.SVG));
		verify(transcoder, times(2)).transcode(any(TranscoderInput.class), any(TranscoderOutput.class));
	}
}

package it.quartara.boser.action.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.SearchConfig;
import it.quartara.boser.model.SearchKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.persistence.EntityManager;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.apache.solr.common.SolrDocumentList;
import org.fit.cssbox.demo.ImageRenderer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.xml.sax.SAXException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;


@RunWith(PowerMockRunner.class)
@PrepareForTest({PdfResultWriterHandler.class, ImageRenderer.class, Transcoder.class, TranscoderOutput.class, PdfWriter.class})
@MockPolicy(Slf4jMockPolicy.class)
public class PdfResultWriterHandlerTest {

	@Test 
	public void testHappyPath() throws Exception {
		EntityManager em = mock(EntityManager.class);
		String repo = "target/test-output/searchRepo";
		Parameter param = new Parameter();
		param.setValue(repo);
		SearchConfig config = new SearchConfig();
		config.setId(10L);
		SearchKey key = new SearchKey();
		key.setText("key");
		
		SolrDocumentList docs = ActionHandlerTestHelper.createSolrDocumentList();
		
		ImageRenderer imageRenderer = mock(ImageRenderer.class);
		PDFTranscoder transcoder = mock(PDFTranscoder.class);
		
		when(em.find(Parameter.class, "SEARCH_REPO")).thenReturn(param);
		Calendar calendar = new GregorianCalendar(2015, 1, 18, 22, 05);
		whenNew(Date.class).withNoArguments().thenReturn(calendar.getTime());
		whenNew(ImageRenderer.class).withNoArguments().thenReturn(imageRenderer);
		whenNew(PDFTranscoder.class).withNoArguments().thenReturn(transcoder);
		
		PdfResultWriterHandler handler = spy(new PdfResultWriterHandler(em));
		handler.handle(config, key, docs);
		
		verify(imageRenderer, times(2)).renderURL(any(String.class), any(ByteArrayOutputStream.class), eq(ImageRenderer.Type.SVG));
		verify(transcoder, times(2)).transcode(any(TranscoderInput.class), any(TranscoderOutput.class));
	}
	
	@Test
	public void testSVGRenderingProblem() throws Exception {
		EntityManager em = mock(EntityManager.class);
		String repo = "target/test-output/searchRepo";
		Parameter param = new Parameter();
		param.setValue(repo);
		SearchConfig config = new SearchConfig();
		config.setId(10L);
		SearchKey key = new SearchKey();
		key.setText("key");
		
		SolrDocumentList docs = ActionHandlerTestHelper.createSolrDocumentList();
		
		ImageRenderer imageRenderer = mock(ImageRenderer.class);
		PDFTranscoder transcoder = mock(PDFTranscoder.class);
		
		when(em.find(Parameter.class, "SEARCH_REPO")).thenReturn(param);
		Calendar calendar = new GregorianCalendar(2015, 1, 18, 22, 05);
		whenNew(Date.class).withNoArguments().thenReturn(calendar.getTime());
		whenNew(ImageRenderer.class).withNoArguments().thenReturn(imageRenderer);
		whenNew(PDFTranscoder.class).withNoArguments().thenReturn(transcoder);
		
		doThrow(new IOException()).doThrow(new SAXException()).when(imageRenderer)
			.renderURL(any(String.class), any(ByteArrayOutputStream.class), eq(ImageRenderer.Type.SVG));
		
		PdfResultWriterHandler handler = spy(new PdfResultWriterHandler(em));
		doNothing().when(handler, "createErrorFile", any(String.class), any(File.class), any(Exception.class));
		handler.handle(config, key, docs);
		
		verifyPrivate(handler, times(2)).invoke("createErrorFile", any(String.class), any(File.class), any(Exception.class));
	}
	
	@Test
	public void testOutputFileProblemAndTranscodingProblem() throws Exception {
		EntityManager em = mock(EntityManager.class);
		String repo = "target/test-output/searchRepo";
		Parameter param = new Parameter();
		param.setValue(repo);
		SearchConfig config = new SearchConfig();
		config.setId(10L);
		SearchKey key = new SearchKey();
		key.setText("key");
		
		SolrDocumentList docs = ActionHandlerTestHelper.createSolrDocumentList();
		
		ImageRenderer imageRenderer = mock(ImageRenderer.class);
		PDFTranscoder transcoder = mock(PDFTranscoder.class);
		
		when(em.find(Parameter.class, "SEARCH_REPO")).thenReturn(param);
		Calendar calendar = new GregorianCalendar(2015, 1, 18, 22, 05);
		whenNew(Date.class).withNoArguments().thenReturn(calendar.getTime());
		whenNew(ImageRenderer.class).withNoArguments().thenReturn(imageRenderer);
		whenNew(PDFTranscoder.class).withNoArguments().thenReturn(transcoder);
		whenNew(TranscoderOutput.class).withParameterTypes(OutputStream.class).withArguments(any(FileOutputStream.class))
			.thenThrow(new FileNotFoundException())
			.thenReturn(new TranscoderOutput());
		doThrow(new TranscoderException("")).when(transcoder).transcode(any(TranscoderInput.class), any(TranscoderOutput.class));
		
		PdfResultWriterHandler handler = new PdfResultWriterHandler(em);
		handler.handle(config, key, docs);
		
		verify(imageRenderer, times(2)).renderURL(any(String.class), any(ByteArrayOutputStream.class), eq(ImageRenderer.Type.SVG));
		verify(transcoder).transcode(any(TranscoderInput.class), any(TranscoderOutput.class));
	}
	
	@Test
	public void testCreateErrorFile() throws Exception {
		String url = "http://abc.domain.com";
		File output = new File("target/test-output/error.pdf");
		Exception e = new SAXException();
		PdfResultWriterHandler handler = new PdfResultWriterHandler(null);
		Whitebox.invokeMethod(handler, "createErrorFile", url, output, e);
		assertTrue(output.exists());
	}
	
	@Test
	public void testCreateErrorFileWithException() throws Exception {
		String url = "http://abc.domain.com";
		File output = new File("target/test-output/error2.pdf");
		Exception e = new SAXException();
		
		mockStatic(PdfWriter.class);
		when(PdfWriter.getInstance(any(Document.class), any(OutputStream.class))).thenThrow(new DocumentException());
		
		PdfResultWriterHandler handler = new PdfResultWriterHandler(null);
		Whitebox.invokeMethod(handler, "createErrorFile", url, output, e);
		assertTrue(output.exists());
		assertEquals(output.length(), 0);
		output.delete();
	}
	

	
	/*
	 * codice usato per le prove techiche delle varie librerie possibili
	 */
	public void testLibraries() throws IOException, SAXException, TranscoderException {
		//String urlstring = "http://www.cpinfo.it/index.asp";
		//String urlstring = "http://wkhtmltopdf.org/";
		//String urlstring = "http://www.nuvolari.tv/articoli/freelander-2-limited-edition/";
		String urlstring = "http://www.fao.org/home/en/";
		
		
		/* parsing con CSSBOX
		//Open the network connection 
		DocumentSource docSource = new DefaultDocumentSource(urlstring);

		//Parse the input document
		DOMSource parser = new DefaultDOMSource(docSource);
		Document doc = parser.parse(); //doc represents the obtained DOM
		
		//init media spec
		//we will use the "screen" media type for rendering
		//MediaSpec media = new MediaSpec("print");
		
		DOMAnalyzer da = new DOMAnalyzer(doc, docSource.getURL());
		//da.setMediaSpec(media);
		da.attributesToStyles(); //convert the HTML presentation attributes to inline styles
		da.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the standard style sheet
		da.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the additional style sheet
		da.addStyleSheet(null, CSSNorm.formsStyleSheet(), DOMAnalyzer.Origin.AGENT); //(optional) use the forms style sheet
		da.getStyleSheets(); //load the author style sheets
		*/
		
		
//		printDoc(doc.getFirstChild());
//		docSource.close();
		
		
		/*
		 * FS: produce un pdf di testo ma non mantiene lo stile
		ITextRenderer renderer = new ITextRenderer();
		renderer.setDocument(doc, null);
		renderer.layout();
		FileOutputStream output = new FileOutputStream("target/test-output/test-fs.pdf");
		renderer.createPDF(output);
		output.close();
		 */
		
		/*
		 * CssBox ImageRenderer + Batik: mantiene il testo!!
		 */
		ImageRenderer imageRenderer = new ImageRenderer();
//		imageRenderer.renderURL("http://wkhtmltopdf.org/", 
//				new FileOutputStream(new File("target/test-output/test.svg")), 
//				ImageRenderer.Type.SVG);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		imageRenderer.renderURL(urlstring, outputStream, ImageRenderer.Type.SVG);
		//imageRenderer.renderURL(urlstring, new FileOutputStream(new File("src/test/resources/fao.svg")), ImageRenderer.Type.SVG);
		byte[] svgBytes = outputStream.toByteArray();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(svgBytes);

		Transcoder transcoder = new PDFTranscoder();
        //TranscoderInput transcoderInput = new TranscoderInput(new FileInputStream(new File("/tmp/test.svg")));
        //TranscoderInput transcoderInput = new TranscoderInput(da.getRoot().getOwnerDocument());
        //TranscoderInput transcoderInput = new TranscoderInput(doc);              // ClassCastException! forse va convertito??
        TranscoderInput transcoderInput = new TranscoderInput(inputStream);
        TranscoderOutput transcoderOutput = new TranscoderOutput(new FileOutputStream(new File("target/test-output/test-batik.pdf")));
        transcoder.transcode(transcoderInput, transcoderOutput);
	}

}

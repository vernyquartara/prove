package it.quartara.boser.service.impl;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import it.quartara.boser.service.PdfConversionService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.fit.cssbox.demo.ImageRenderer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.RectangleReadOnly;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PdfConversionServiceImpl.class, PdfWriter.class})
@MockPolicy(Slf4jMockPolicy.class)
public class PdfConversionServiceImplTest {

	@Test
	public void testConversionOk() throws Exception {
		String destDir = "target/test-output";
		String urlToConvert = "http://www.quattroruote.it/news/mobilita_alternativa/2015/03/19/car_sharing_enjoy_supera_i_2_5_milioni_di_noleggi_.html";
		String testata = "quattroruote.it";
		ImageRenderer imageRenderer = mock(ImageRenderer.class);
		PDFTranscoder transcoder = mock(PDFTranscoder.class);
		whenNew(ImageRenderer.class).withNoArguments().thenReturn(imageRenderer);
		whenNew(PDFTranscoder.class).withNoArguments().thenReturn(transcoder);
		whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mock(FileOutputStream.class));
		PdfReader pdfReader = mock(PdfReader.class);
		whenNew(PdfReader.class).withAnyArguments().thenReturn(pdfReader);
		//Rectangle r = PageSize.A4;
		Rectangle r = new RectangleReadOnly(538.33f, 2551.51f);
		when(pdfReader.getPageSize(1)).thenReturn(r);
		PdfWriter writer = mock(PdfWriter.class);
		mockStatic(PdfWriter.class);
		when(PdfWriter.getInstance(any(Document.class), any(OutputStream.class))).thenReturn(writer);
		PdfContentByte content = mock(PdfContentByte.class);
		when(writer.getDirectContent()).thenReturn(content);
		when(writer.getImportedPage(pdfReader, 1)).thenReturn(mock(PdfImportedPage.class));
		
		
		PdfConversionService service = new PdfConversionServiceImpl();
		File pdfFile = service.convertToPdf(destDir, urlToConvert, testata);
		
		assertThat(pdfFile.getAbsolutePath(), 
				   endsWith("target/test-output/"+testata+"_car_sharing_enjoy_supera_i_2_5_milioni_di_noleggi_.html.pdf"));
		verify(imageRenderer).renderURL(any(String.class), any(ByteArrayOutputStream.class), eq(ImageRenderer.Type.SVG));
		verify(transcoder).transcode(any(TranscoderInput.class), any(TranscoderOutput.class));
		verify(content).addTemplate(any(PdfImportedPage.class), eq(1.0f), eq(0f), eq(0f), eq(1.0f), eq(0f), anyFloat());
	}
	
	@Test
	public void testSVGConvertionFailure() throws Exception {
		String destDir = "target/test-output";
		String urlToConvert = "http://www.quattroruote.it/news/mobilita_alternativa/2015/03/19/car_sharing_enjoy_supera_i_2_5_milioni_di_noleggi_.html";
		String testata = "quattroruote.it";
		ImageRenderer imageRenderer = mock(ImageRenderer.class);
		whenNew(ImageRenderer.class).withNoArguments().thenReturn(imageRenderer);
		whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mock(FileOutputStream.class));
		doThrow(Exception.class).when(imageRenderer).renderURL(any(String.class), any(ByteArrayOutputStream.class), eq(ImageRenderer.Type.SVG));
		
		PdfConversionService service = new PdfConversionServiceImpl();
		File pdfFile = service.convertToPdf(destDir, urlToConvert, testata);
		assertNull(pdfFile);
	}
	
	@Test
	public void testPDFConversionFailure() throws Exception {
		String destDir = "target/test-output";
		String urlToConvert = "http://www.quattroruote.it/news/mobilita_alternativa/2015/03/19/car_sharing_enjoy_supera_i_2_5_milioni_di_noleggi_.html";
		String testata = "quattroruote.it";
		ImageRenderer imageRenderer = mock(ImageRenderer.class);
		PDFTranscoder transcoder = mock(PDFTranscoder.class);
		whenNew(ImageRenderer.class).withNoArguments().thenReturn(imageRenderer);
		whenNew(PDFTranscoder.class).withNoArguments().thenReturn(transcoder);
		whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mock(FileOutputStream.class));
		doThrow(Exception.class).when(transcoder).transcode(any(TranscoderInput.class), any(TranscoderOutput.class));
		FileOutputStream fos = mock(FileOutputStream.class);
		whenNew(FileOutputStream.class).withAnyArguments().thenReturn(fos);
		
		PdfConversionService service = new PdfConversionServiceImpl();
		File svgFile = service.convertToPdf(destDir, urlToConvert, testata);
		
		assertThat(svgFile.getAbsolutePath(), 
				   endsWith("target/test-output/"+testata+"_car_sharing_enjoy_supera_i_2_5_milioni_di_noleggi_.html.svg"));
		verify(imageRenderer).renderURL(any(String.class), any(ByteArrayOutputStream.class), eq(ImageRenderer.Type.SVG));
		verify(fos).write(any(byte[].class));
		verify(fos).close();
	}
	
}

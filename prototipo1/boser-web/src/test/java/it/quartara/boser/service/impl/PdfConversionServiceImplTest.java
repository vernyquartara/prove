package it.quartara.boser.service.impl;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import it.quartara.boser.service.PdfConversionFactory;
import it.quartara.boser.service.PdfConversionService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.fit.cssbox.demo.ImageRenderer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.lowagie.text.pdf.codec.Base64.OutputStream;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PdfConversionServiceImpl.class})
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
		
		
		PdfConversionService service = PdfConversionFactory.create();
		File pdfFile = service.convertToPdf(destDir, urlToConvert, testata);
		
		assertThat(pdfFile.getAbsolutePath(), 
				   endsWith("target/test-output/"+testata+"_car_sharing_enjoy_supera_i_2_5_milioni_di_noleggi_.html.pdf"));
		verify(imageRenderer).renderURL(any(String.class), any(ByteArrayOutputStream.class), eq(ImageRenderer.Type.SVG));
		verify(transcoder).transcode(any(TranscoderInput.class), any(TranscoderOutput.class));
	}
	@Test
	public void testConversionKO() throws Exception {
		String destDir = "target/test-output";
		String urlToConvert = "http://www.quattroruote.it/news/mobilita_alternativa/2015/03/19/car_sharing_enjoy_supera_i_2_5_milioni_di_noleggi_.html";
		String testata = "quattroruote.it";
		ImageRenderer imageRenderer = mock(ImageRenderer.class);
		PDFTranscoder transcoder = mock(PDFTranscoder.class);
		whenNew(ImageRenderer.class).withNoArguments().thenReturn(imageRenderer);
		whenNew(PDFTranscoder.class).withNoArguments().thenReturn(transcoder);
		whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mock(FileOutputStream.class));
		when(imageRenderer.renderURL(any(String.class), any(OutputStream.class), eq(ImageRenderer.Type.SVG))).thenThrow(new IOException());
		
		PdfConversionService service = PdfConversionFactory.create();
		File pdfFile = service.convertToPdf(destDir, urlToConvert, testata);
		
		assertNull(pdfFile);
	}
	@Test
	public void testConversionOkWithoutPrefix() throws Exception {
		String destDir = "target/test-output";
		String urlToConvert = "http://www.quattroruote.it/news/mobilita_alternativa/2015/03/19/car_sharing_enjoy_supera_i_2_5_milioni_di_noleggi_.html";
		ImageRenderer imageRenderer = mock(ImageRenderer.class);
		PDFTranscoder transcoder = mock(PDFTranscoder.class);
		whenNew(ImageRenderer.class).withNoArguments().thenReturn(imageRenderer);
		whenNew(PDFTranscoder.class).withNoArguments().thenReturn(transcoder);
		whenNew(FileOutputStream.class).withAnyArguments().thenReturn(mock(FileOutputStream.class));
		
		
		PdfConversionService service = PdfConversionFactory.create();
		File pdfFile = service.convertToPdf(destDir, urlToConvert);
		
		assertThat(pdfFile.getAbsolutePath(), 
				endsWith("target/test-output/car_sharing_enjoy_supera_i_2_5_milioni_di_noleggi_.html.pdf"));
		verify(imageRenderer).renderURL(any(String.class), any(ByteArrayOutputStream.class), eq(ImageRenderer.Type.SVG));
		verify(transcoder).transcode(any(TranscoderInput.class), any(TranscoderOutput.class));
	}
}

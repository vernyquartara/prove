package it.quartara.boser.service.impl;

import it.quartara.boser.helper.UrlHelper;
import it.quartara.boser.service.PdfConversionService;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.fit.cssbox.demo.ImageRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.RectangleReadOnly;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

/**
 * http://xmlgraphics.apache.org/fop/1.1/graphics.html#batik
 * http://xmlgraphics.apache.org/fop/1.1/graphics.html#svg-pdf-text
 * http://wiki.apache.org/xmlgraphics-fop/SvgNotes/PdfTranscoderTrueTypeEmbedding
 * @author webny
 *
 */
public class PdfConversionServiceImpl implements PdfConversionService, Serializable {
	
	private static final long serialVersionUID = -6957321264439905450L;
	transient private static final Logger log = LoggerFactory.getLogger(PdfConversionServiceImpl.class);
	
	public PdfConversionServiceImpl() {
	}
	
	@Override
	public File convertToPdf(String destDir, String url) {
		return convertToPdf(destDir, url, null);
	}

	@Override
	public File convertToPdf(String destDir, String url, String fileNamePrefix) {
		log.debug("avvio conversione in pdf url={}", url);
		String fileName = (fileNamePrefix != null ? fileNamePrefix+"_" : "")
						  +UrlHelper.getLastPart(url)+".pdf";
		File destFile = new File(destDir, fileName);
		log.debug("file destinazione={}", destFile.getAbsolutePath());
		ImageRenderer imageRenderer = new ImageRenderer();
		imageRenderer.setLoadImages(Boolean.TRUE, Boolean.FALSE);
		Transcoder transcoder = new PDFTranscoder();
		byte[] svgBytes = null;
		ByteArrayOutputStream outputStream = null;
		try {
			/*
			 * conversione html in SVG
			 */
			outputStream = new ByteArrayOutputStream();
			//File tempFile = File.createTempFile("svg", null);
			//tempFile.deleteOnExit();
			//FileOutputStream outputStream = new FileOutputStream(tempFile);
			log.debug("avvio rendering SVG");
			imageRenderer.renderURL(url, outputStream, ImageRenderer.Type.SVG);
			log.debug("fine rendering SVG");
			svgBytes = outputStream.toByteArray();
		} catch (Exception e) {
			log.error("problema di conversione in SVG per l'url: "+url, e);
			return null;
		}
		try {
			/*
			 * conversione SVG in PDF
			 */
			ByteArrayInputStream inputStream = new ByteArrayInputStream(svgBytes);
			//FileInputStream inputStream = new FileInputStream(tempFile);
			TranscoderInput transcoderInput = new TranscoderInput(inputStream);
			ByteArrayOutputStream intermediatePdf = new ByteArrayOutputStream();
	        TranscoderOutput transcoderOutput 
	        	= new TranscoderOutput(new BufferedOutputStream(intermediatePdf));
	        log.debug("conversione in pdf...");
			transcoder.transcode(transcoderInput, transcoderOutput);
			log.debug("conversione in pdf effettuata correttamente");
			/*
			 * ridimensionamento PDF e suddivisione in pagine
			 */
			log.debug("ridimensionamento pdf e suddivisione in pagine");
			PdfReader reader = new PdfReader(new ByteArrayInputStream(intermediatePdf.toByteArray()));
			Rectangle originalRectangle = reader.getPageSize(1);
			
//			float wOut = PageSize.A4.getWidth();
//			float hOut = PageSize.A4.getHeight();
			float wOut = 538.33f;
			float hOut = 2551.51f;
			
			float scale = wOut / originalRectangle.getWidth(); //rapporto la larghezza A4 e la larghezza originale
			float scaledHeigth = originalRectangle.getHeight() * scale; //altezza originale scalata
			int numPages = (int) Math.ceil(scaledHeigth / hOut); //numero di pagine calcolato in base all'altezza scalata
			
		    //Document doc = new Document(PageSize.A4);
		    Document doc = new Document(new RectangleReadOnly(wOut,hOut));
		    PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(destFile));
		    doc.open();
		    PdfContentByte content = writer.getDirectContent();
		    PdfImportedPage page = writer.getImportedPage(reader, 1);
		    
		    float yOffset = hOut - scaledHeigth; //offset verticale iniziale
		    int i = 0;
	        do {
	        	content.addTemplate(page, scale, 0, 0, scale, 0, yOffset);
	        	doc.newPage();
	        	yOffset += hOut;
	        	i++;
	        } while (i<numPages);
		    
		    doc.close();
		    log.debug("ridimensionamento effettuato correttamente");
		    /*
		     * chiusura streams
		     */
			outputStream.close();
			inputStream.close();
		} catch (Exception e) {
			log.error("problema di conversione in PDF per l'url: "+url, e);
			/*
			 * si restituisce l'SVG
			 */
			try {
				File svgFile = new File(destDir, fileName.replace(".pdf", ".svg"));
				FileOutputStream fileOut = new FileOutputStream(svgFile);
				fileOut.write(svgBytes);
				fileOut.close();
				return svgFile;
			} catch (IOException e1) {
				log.error("problema di scrittura file SVG", e1);
				return null;
			}
		}
		return destFile;
	}
	
}

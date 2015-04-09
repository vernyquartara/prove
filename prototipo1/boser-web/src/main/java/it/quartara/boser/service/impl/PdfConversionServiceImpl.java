package it.quartara.boser.service.impl;

import it.quartara.boser.helper.UrlHelper;
import it.quartara.boser.service.PdfConversionService;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

/**
 * http://xmlgraphics.apache.org/fop/1.1/graphics.html#batik
 * http://xmlgraphics.apache.org/fop/1.1/graphics.html#svg-pdf-text
 * http://wiki.apache.org/xmlgraphics-fop/SvgNotes/PdfTranscoderTrueTypeEmbedding
 * @author webny
 *
 */
public class PdfConversionServiceImpl implements PdfConversionService, Serializable {
	
	private static final long serialVersionUID = -6957321264439905450L;
	private static final Logger log = LoggerFactory.getLogger(PdfConversionServiceImpl.class);

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
		try {
			ImageRenderer imageRenderer = new ImageRenderer();
			Transcoder transcoder = new PDFTranscoder();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			//File tempFile = File.createTempFile("svg", null);
			//tempFile.deleteOnExit();
			//FileOutputStream outputStream = new FileOutputStream(tempFile);
			log.debug("avvio rendering SVG");
			imageRenderer.renderURL(url, outputStream, ImageRenderer.Type.SVG);
			log.debug("fine rendering SVG");
			byte[] svgBytes = outputStream.toByteArray();
			ByteArrayInputStream inputStream = new ByteArrayInputStream(svgBytes);
			//FileInputStream inputStream = new FileInputStream(tempFile);
			TranscoderInput transcoderInput = new TranscoderInput(inputStream);
	        TranscoderOutput transcoderOutput = new TranscoderOutput(new FileOutputStream(destFile));
	        log.debug("conversione in pdf...");
			transcoder.transcode(transcoderInput, transcoderOutput);
			log.debug("conversione in pdf effettuata correttamente");
		} catch (Exception e) {
			log.error("problema di conversione in pdf per l'url: "+url, e);
			return null;
		}
		return destFile;
	}
	
	/*
	 * for debug purpose
	 */
	public static void main(String[] a) throws IOException, DocumentException {
		PdfConversionServiceImpl service = new PdfConversionServiceImpl();
		File pdfFile = service.convertToPdf("/home/webny/work/Boser/test/pdf", "http://www.riders.org/get-involved/motorcycling-events", "riders.org");
		PdfReader reader = new PdfReader(pdfFile.getAbsolutePath());
		PdfStamper pdfStamper = new PdfStamper(reader,
	            new FileOutputStream("/home/webny/work/Boser/test/pdf/HelloWorld-Stamped.pdf"));
		for(int i=1; i<= reader.getNumberOfPages(); i++){

	          PdfContentByte content = pdfStamper.getUnderContent(i);
	          /*
	          PdfPTable pdffooter = new PdfPTable(1);
              pdffooter.setTotalWidth(550); 
              pdffooter.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
              pdffooter.getDefaultCell().setVerticalAlignment(Element.ALIGN_BOTTOM); 
              pdffooter.getDefaultCell().setBorderColor(new Color(255, 255, 255));
              pdffooter.addCell(new Phrase("---------http://www.riders.org/get-involved/motorcycling-events--------", new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(0, 0, 0)) ));
              pdffooter.writeSelectedRows(0, -1, 0, content.getPdfDocument().bottom(), content);
	          */

	          content.beginText();
	          content.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED), 18);
	          content.setTextMatrix(30, 30);
	          content.showText("---------http://www.riders.org/get-involved/motorcycling-events--------");
	          content.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED), 32);
	          content.setColorStroke(Color.BLACK);
	          content.showTextAligned(Element.ALIGN_CENTER, "DUPLICATE", 230, 1000, 45);
	          content.endText();
	          
	          /*
	          content.beginText();
	          content.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, "UTF-8", false),12);
	          String text = "---------http://www.riders.org/get-involved/motorcycling-events--------";
	          content.showTextAligned(PdfContentByte.ALIGN_LEFT,text,300,40, 0);
	          content.endText();
	          */
	      }
		pdfStamper.close();
		
		/*
		Document document = new Document(reader.getPageSizeWithRotation(1));
		//reader.get
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("/home/webny/work/Boser/test/pdf/HelloWorld.pdf"));
		HeaderFooter footer = new HeaderFooter(new Phrase("Add Footer Here"), new Phrase("."));
		footer.setBorder(Rectangle.NO_BORDER);
        footer.setAlignment(Element.ALIGN_CENTER);
		writer.setPageEvent(footer);
		document.open();
		
		document.setFooter(footer);
		document.close();
		*/
	}

}

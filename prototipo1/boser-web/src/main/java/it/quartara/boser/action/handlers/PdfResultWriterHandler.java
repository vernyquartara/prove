package it.quartara.boser.action.handlers;

import static it.quartara.boser.model.IndexField.TITLE;
import static it.quartara.boser.model.IndexField.URL;
import it.quartara.boser.action.ActionException;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.Search;
import it.quartara.boser.model.SearchKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.persistence.EntityManager;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.fit.cssbox.demo.ImageRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Per ogni risultato di ricerca (documento) preleva il codice html
 * presente alla relativa URL e ne crea un file PDF, dopo aver ripulito
 * il codice usando TagSoup. (?)
 * @author webny
 *
 */
public class PdfResultWriterHandler extends AbstractActionHandler {
	
	private static final Logger log = LoggerFactory.getLogger(PdfResultWriterHandler.class);

	public PdfResultWriterHandler(EntityManager em) {
		super(em);
	}

	@Override
	protected void execute(Search search, SearchKey key, SolrDocumentList documents) throws ActionException {
		Parameter param = em.find(Parameter.class, "SEARCH_REPO");
		String repo = param.getValue();
		File repoDir = new File(repo+File.separator+search.getConfig().getId()+File.separator+search.getId());
		repoDir.mkdirs();
		
		ImageRenderer imageRenderer = new ImageRenderer();
		Transcoder transcoder = new PDFTranscoder();
		for (SolrDocument doc : documents) {
			String docUrl = (String) doc.getFieldValue(URL.toString());
			String docTitle = (String) doc.getFieldValue(TITLE.toString());
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			File outputFile = new File(repoDir.getAbsolutePath()+File.separator+docTitle+".pdf");
			try {
				imageRenderer.renderURL(docUrl, outputStream, ImageRenderer.Type.SVG);
			} catch (IOException | SAXException e) {
				log.error("SVG rendering problem", e);
				createErrorFile(docUrl, outputFile, e);
				continue;
			}
			byte[] svgBytes = outputStream.toByteArray();
			ByteArrayInputStream inputStream = new ByteArrayInputStream(svgBytes);
			TranscoderInput transcoderInput = new TranscoderInput(inputStream);
	        TranscoderOutput transcoderOutput = null;
			try {
				transcoderOutput = new TranscoderOutput(new FileOutputStream(outputFile));
			} catch (FileNotFoundException e) {
				log.warn("this shouldn't happen!", e);
				createErrorFile(docUrl, outputFile, e);
				continue;
			}
	        try {
				transcoder.transcode(transcoderInput, transcoderOutput);
			} catch (TranscoderException e) {
				log.warn("Transcoding problem", e);
				createErrorFile(docUrl, outputFile, e);
				continue;
			}
		}
	}

	/*
	 * crea un file pdf contenente lo stack trace dell'eccezione
	 */
	private void createErrorFile(String url, File outputFile, Exception exception) {
		Document document = new Document();
		PdfWriter pdfwriter = null;
		try {
			pdfwriter = PdfWriter.getInstance(document, new FileOutputStream(outputFile));
			document.open();
			StringBuilder buffer = new StringBuilder("Non è stato possibile generare");
			buffer.append(" il file pdf per l'URL\n\n");
			buffer.append(url);
			buffer.append("\n\na causa del seguente errore:\n\n");
			document.add(new Paragraph(buffer.toString()));
			StringWriter stringWriter = new StringWriter();
			exception.printStackTrace(new PrintWriter(stringWriter));
			document.add(new Paragraph(stringWriter.toString()));
			document.close();
			pdfwriter.close();
		} catch (FileNotFoundException | DocumentException e) {
			log.error("errore di scrittura del file di errore", e);
		}
	}

}

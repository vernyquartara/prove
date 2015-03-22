package it.quartara.boser.workers;

import it.quartara.boser.helper.UrlHelper;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.model.PdfConversion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.fit.cssbox.demo.ImageRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class PdfConversionWorker implements Runnable {
	
	private static final Logger log = LoggerFactory.getLogger(PdfConversionWorker.class);
	
	private AsyncContext asyncContext;

	public PdfConversionWorker(AsyncContext asyncContext) {
		this.asyncContext = asyncContext;
	}

	@Override
	public void run() {
		/*
		 * recupero parametri
		 */
		HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
		String pdfDestDir = (String) request.getAttribute("pdfDestDir");
		@SuppressWarnings("unchecked")
		List<String> urls = (List<String>) request.getAttribute("urlsList");
		Long pdfConversionId = (Long) request.getAttribute("pdfConversionId");
		/*
		 * apertura transazione e recupero oggetto PdfConversion
		 */
		EntityManagerFactory emf =
		           (EntityManagerFactory)request.getServletContext().getAttribute("emf");
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		PdfConversion pdfConversion = em.find(PdfConversion.class, pdfConversionId);
		
		/*
		 * conversione
		 */
		short numberOfLinks = convertToPdf(pdfDestDir, urls);
		if (numberOfLinks == 0) {
			/*
			 * si assume che in input arrivi sempre una lista di urls
			 * composta almeno di un elemento. se la conversione
			 * restituisce 0 vuol dire che tutte le conversioni sono
			 * andate male.
			 */
			log.error("nessuna conversione è andata a buon fine");
			pdfConversion.setState(ExecutionState.ERROR);
			em.merge(pdfConversion);
			em.getTransaction().commit();
			asyncContext.complete();
		}
		/*
		 * creazione zip
		 */
		String zipFilePath = null;
		Long zipFileSize = null;
		try {
			File zipFile = createZipFile(pdfDestDir);
			zipFilePath = zipFile.getAbsolutePath();
			zipFileSize = zipFile.length();
		} catch (IOException e) {
			/*
			 * se va male la creazione dello zip
			 * non esisterà alcun file da scaricare
			 */
			pdfConversion.setState(ExecutionState.ERROR);
			em.merge(pdfConversion);
			em.getTransaction().commit();
			asyncContext.complete();
		}
		
		/*
		 * completamento operazioni e chiusura transazione
		 */
		//pdfConversion.setNumberOfLinks(numberOfLinks);
		pdfConversion.setFilePath(zipFilePath);
		pdfConversion.setFileSize(zipFileSize);
		pdfConversion.setState(ExecutionState.COMPLETED);
		em.merge(pdfConversion);
		em.getTransaction().commit();
		em.close();
		asyncContext.dispatch("/conversionHome");
	}
	
	/*
	 * Crea un file pdf per ogni link della lista in input.
	 */
	private short convertToPdf(String whereToWrite, List<String> urls) {
		File destDir = new File(whereToWrite);
		destDir.mkdirs();
		short numberOfLinks = 0;
		for (String url : urls) {
			String fileName = UrlHelper.getLastPart(url)+".pdf";
			File destFile = new File(destDir, fileName);
			
			try {
				ImageRenderer imageRenderer = new ImageRenderer();
				Transcoder transcoder = new PDFTranscoder();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					imageRenderer.renderURL(url, outputStream, ImageRenderer.Type.SVG);
				byte[] svgBytes = outputStream.toByteArray();
				ByteArrayInputStream inputStream = new ByteArrayInputStream(svgBytes);
				TranscoderInput transcoderInput = new TranscoderInput(inputStream);
		        TranscoderOutput transcoderOutput = null;
				transcoderOutput = new TranscoderOutput(new FileOutputStream(destFile));
				transcoder.transcode(transcoderInput, transcoderOutput);
			} catch (Exception e) {
				log.error("problema di conversione in pdf per l'url: "+url, e);
				continue;
			}
			
			numberOfLinks++;
		}
		return numberOfLinks;
	}
	
	/*
	 * crea il file zip a partire da
	 * 1) la cartella che contiene i pdf
	 * 2) il nome del file xls originale
	 * 3) la cartella destinazione
	 */
	private File createZipFile(String dirToZip) throws IOException {
		String zipFileName = dirToZip.substring(0, dirToZip.lastIndexOf("/"))+"/"
							+dirToZip.substring(dirToZip.lastIndexOf("/")+1)
							+".zip";
		File zipFile = new File(zipFileName);
		
		FileOutputStream fos = new FileOutputStream(zipFile);
		ZipOutputStream zos = new ZipOutputStream(fos);
		
		byte[] buffer = new byte[1024];
		File[] files = new File(dirToZip).listFiles(new FilenameFilter(){
			
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".pdf");
			}
		});
		for (File file : files) {
			ZipEntry ze = new ZipEntry(file.getName());
			zos.putNextEntry(ze);
			FileInputStream inputFile = new FileInputStream(file);
			int len;
			while ((len = inputFile.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}
			inputFile.close();
			zos.closeEntry();
		}
		zos.close();
		return zipFile;
	}

}

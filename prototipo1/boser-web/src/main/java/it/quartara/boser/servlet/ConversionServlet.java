package it.quartara.boser.servlet;

import it.quartara.boser.helper.UrlHelper;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.PdfConversion;
import it.quartara.boser.workers.PdfConversionWorker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;
import javax.servlet.AsyncContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.fop.svg.PDFTranscoder;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.fit.cssbox.demo.ImageRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@WebServlet(asyncSupported = true, urlPatterns = { "/xlsToPdf" })
public class ConversionServlet extends BoserServlet {

	private static final long serialVersionUID = -6738304809439599277L;
	
	private static final Logger log = LoggerFactory.getLogger(ConversionServlet.class);

	/*
	 * Legge il file caricato dall'utente e lo scrive nel repository
	 * Per ogni link crea un file pdf nella cartella temporanea (sottocartella nome del file xls)
	 * Crea un file zip nella cartella "xlsToPdf" del repository
	 * Crea un oggetto PdfConversion e lo persiste
	 * 
	 * (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		/*
		 * upload del file xls
		 * TODO validazione input (il nome del file non deve contenere spazi, il file deve essere un xls)
		 */
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(16384);
		ServletContext servletContext = this.getServletConfig().getServletContext();
		File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
		factory.setRepository(repository);

		ServletFileUpload upload = new ServletFileUpload(factory);

		List<FileItem> items = null;
		try {
			items = upload.parseRequest(request);
		} catch (FileUploadException e) {
			throw new ServletException("problemi parsing della request", e);
		}
		FileItem uploadedFile = getFormField(items, "file");
		String originalName = uploadedFile.getName();
		if (originalName.contains("/")) {
			originalName = originalName.substring(originalName.lastIndexOf("/")+1);
		}
		else if (originalName.contains("\\")) {
			originalName = originalName.substring(originalName.lastIndexOf("\\")+1);
		}
		
		/*
		 * avvio transazione
		 * recupero parametri (cartelle di lavoro)
		 * e scrittura file xls uploadato
		 */
		String crawlerId = getFormField(items, "crawlerId").getString();
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		Parameter param = em.find(Parameter.class, "SEARCH_REPO");
		String repo = param.getValue();
		String pdfRepo = repo+"/"+crawlerId+"/pdfs";
		new File(pdfRepo).mkdirs();
		File xlsFile = new File(pdfRepo+"/"+originalName);
		try {
			uploadedFile.write(xlsFile);
		} catch (Exception e) {
			em.getTransaction().rollback();
			em.close();
			throw new ServletException("problemi di scrittura file xls", e);
		}
		/*
		 * estrazione urls dal file xls
		 */
		List<String> urls = null;
		try {
			urls = getUrls(xlsFile);
		} catch (InvalidFormatException e) {
			em.getTransaction().rollback();
			em.close();
			throw new ServletException("problemi estrazione dei links dal file xls", e);
		}
		
		/*
		 * creazione oggetto PdfConversion
		 * e avvio worker asincrono
		 */
		PdfConversion pdfConversion = new PdfConversion();
		pdfConversion.setState(ExecutionState.STARTED);
		pdfConversion.setStartDate(new Date());
		em.persist(pdfConversion);
		em.getTransaction().commit();
		
		String workDir = pdfRepo+"/"+originalName.substring(0, originalName.lastIndexOf("."));
		request.setAttribute("pdfDestDir", workDir);
		request.setAttribute("urlsList", urls);
		request.setAttribute("pdfConversionId", pdfConversion.getId());
		ThreadPoolExecutor executor = 
				(ThreadPoolExecutor)request.getServletContext().getAttribute("executor");
		AsyncContext asyncCtx = request.startAsync(request, response);
		asyncCtx.setTimeout(0);
		executor.execute(new PdfConversionWorker(asyncCtx));
		
		/*
		 * conversione in pdf
		String workDir = pdfRepo+"/"+originalName.substring(0, originalName.lastIndexOf("."));
		PdfConversion pdfConversion = new PdfConversion();
		pdfConversion.setState(ExecutionState.STARTED);
		pdfConversion.setStartDate(new Date());
		short numberOfLinks;
		try {
			numberOfLinks = convertToPdf(workDir, urls);
		} catch (SAXException | TranscoderException e) {
			em.getTransaction().rollback();
			em.close();
			throw new ServletException("problemi di conversione in pdf", e);
		}
		pdfConversion.setNumberOfLinks(numberOfLinks);
		 */
		/*
		 * creazione file zip
		File zipFile = createZipFile(workDir);
		pdfConversion.setFilePath(zipFile.getAbsolutePath());
		pdfConversion.setSize(zipFile.length());
		 */
		/*
		 * chiusura transazione
		 */
		em.close();
		
		
		/*
		 * non si effettua il dispatch nel caso di gestione asincrona
		RequestDispatcher rd = request.getRequestDispatcher("/conversionHome");
		rd.forward(request, response);
		 */
	}

	private FileItem getFormField(List<FileItem> items, String fieldName) {
		for (FileItem fileItem : items) {
			if (fileItem.getFieldName().equals(fieldName)) {
				return fileItem;
			}
		}
		return null;
	}

	/*
	 * Legge il file xls in input usando POI
	 * Crea una lista con tutti i links
	 */
	private List<String> getUrls(File xlsFile) throws InvalidFormatException, IOException {
		List<String> urls = new ArrayList<String>();
		Workbook wb = WorkbookFactory.create(xlsFile);
		Sheet sheet = wb.getSheetAt(0);
		for (Row row : sheet) {
			Cell cell = row.getCell(0);
			Hyperlink link = cell.getHyperlink();
			if (link != null) {
				urls.add(link.getAddress());
			}
		}
		return urls;
	}
	
	/*
	 * Crea un file pdf per ogni link della lista in input.
	 */
	private short convertToPdf(String whereToWrite, List<String> urls) throws IOException, SAXException, TranscoderException {
		File destDir = new File(whereToWrite);
		destDir.mkdirs();
		short numberOfLinks = 0;
		for (String url : urls) {
			String fileName = UrlHelper.getLastPart(url)+".pdf";
			File destFile = new File(destDir, fileName);
			
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

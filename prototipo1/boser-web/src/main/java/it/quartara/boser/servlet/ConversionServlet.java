package it.quartara.boser.servlet;

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import it.quartara.boser.jobs.PdfConversionControllerJob;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.PdfConversion;
import it.quartara.boser.model.PdfConversionItem;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.ee.servlet.QuartzInitializerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(loadOnStartup=-1,urlPatterns={"/xlsToPdf"})
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
		log.debug("nome file xls uploadato: {}", originalName);
		originalName = originalName.replaceAll("\\s", "");
		log.debug("con rimozione spazi: {}", originalName);
		/*
		 * avvio transazione
		 * recupero parametri (cartelle di lavoro)
		 * e scrittura file xls uploadato
		 */
		String crawlerId = getFormField(items, "crawlerId").getString();
		log.debug("crawler id: {}", crawlerId);
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		Parameter param = em.find(Parameter.class, "SEARCH_REPO");
		String repo = param.getValue();
		log.debug("SEARCH_REPO: {}", repo);
		if (param.getValue().startsWith("$")) {
			repo = System.getenv(param.getValue().substring(1));
			log.debug("converting variable {} into {}",  param.getValue().substring(1), repo);
		}
		String pdfRepo = repo+"/"+crawlerId+"/pdfs";
		log.debug("pdf repo: {}", pdfRepo);
		new File(pdfRepo).mkdirs();
		File xlsFile = new File(pdfRepo+"/"+originalName);
		try {
			uploadedFile.write(xlsFile);
			log.debug("written xls file: {}", xlsFile.getAbsolutePath());
		} catch (Exception e) {
			em.getTransaction().rollback();
			em.close();
			throw new ServletException("problemi di scrittura file xls", e);
		}
		/*
		 * creazione oggetto PdfConversion
		 */
		Date now = new Date();
		String fileNamePrefix = new SimpleDateFormat("yyyyMMdd").format(now);
		PdfConversion pdfConversion = new PdfConversion();
		pdfConversion.setState(ExecutionState.READY);
		pdfConversion.setCreationDate(now);
		pdfConversion.setXlsFileName(originalName);
		String destDir = pdfRepo+"/"+originalName.substring(0, originalName.lastIndexOf("."));
		log.debug("dest dir: {}", destDir);
		pdfConversion.setDestDir(destDir);
		em.persist(pdfConversion);
		File destDirFile = new File(destDir);
		if (destDirFile.exists()) {
			FileUtils.deleteDirectory(destDirFile);
		}
		destDirFile.mkdirs();
		Workbook wb = null;
		try {
			wb = WorkbookFactory.create(xlsFile);
		} catch (InvalidFormatException e) {
			em.getTransaction().rollback();
			em.close();
			throw new ServletException("File Excel non valido", e);
		}
		Sheet sheet = wb.getSheetAt(0);
		List<PdfConversionItem> pdfConversionItems = new ArrayList<PdfConversionItem>();
		for (Row row : sheet) {
			if (row.getPhysicalNumberOfCells()>0) {
				Cell cell = row.getCell(0);
				if (cell!=null) {
					Hyperlink link = cell.getHyperlink();
					if (link != null) {
						String url = link.getAddress();
						String testata = cell.getRichStringCellValue().getString();
						log.debug("crezione item per url={}, testata={}", url, testata);
						PdfConversionItem pdfConvItem = new PdfConversionItem();
						pdfConvItem.setUrl(url);
						pdfConvItem.setState(ExecutionState.READY);
						pdfConvItem.setPdfFileNamePrefix(fileNamePrefix+testata);
						pdfConvItem.setConversion(pdfConversion);
						pdfConversionItems.add(pdfConvItem);
					}
				}
			}
		}
		pdfConversion.setItems(pdfConversionItems);
		em.merge(pdfConversion);
		log.debug("creato nuovo oggetto PdfConversion id={}", pdfConversion.getId());
		/*
		 * chiusura transazione
		 */
		log.debug("chiusura transazione");
		em.getTransaction().commit();
		em.close();
		
		/*
		 * redirect alla home page di conversione
		 */
		response.sendRedirect("/conversionHome");
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
	 * Crea il job controller che schedulerà tutti i job.
	 * (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init()
	 */
	@Override
	public void init() throws ServletException {
		log.debug("inizializzazione job Controller");
		EntityManagerFactory emf =
		           (EntityManagerFactory)getServletContext().getAttribute("emf");
		SchedulerFactory schedulerFactory = (StdSchedulerFactory) getServletContext()
                .getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY);
		Scheduler scheduler;
		try {
			scheduler = schedulerFactory.getScheduler();
			
			JobDataMap jobDataMap = new JobDataMap();
			jobDataMap.put("entityManagerFactory", emf);
			JobDetail jobDetail =  newJob(PdfConversionControllerJob.class)
									.withIdentity("JOBCTRL", "BOSER")
									.usingJobData(jobDataMap)
									.build();
			Trigger trigger = newTrigger()
								.withIdentity("JOBCTRLTRG", "BOSER")
								.withSchedule(simpleSchedule()
											.withIntervalInSeconds(15)
											.withMisfireHandlingInstructionNextWithRemainingCount()
											.repeatForever())
								.startAt(futureDate(30, IntervalUnit.SECOND))
								.build();
		
			scheduler.scheduleJob(jobDetail, trigger);
			log.info("job controller schedulato");
		} catch (SchedulerException e) {
			log.error("scheduler non trovato!!", e);
		}
	}

}

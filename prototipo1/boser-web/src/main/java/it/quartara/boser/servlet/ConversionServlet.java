package it.quartara.boser.servlet;

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import it.quartara.boser.jobs.PdfConversionControllerJob;
import it.quartara.boser.jobs.PdfConversionJob;
import it.quartara.boser.model.AsyncRequest;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.PdfConversion;
import it.quartara.boser.service.PdfConversionFactory;
import it.quartara.boser.service.PdfConversionService;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.ee.servlet.QuartzInitializerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/xlsToPdf")
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
		EntityManagerFactory emf =
		           (EntityManagerFactory)getServletContext().getAttribute("emf");
		EntityManager em = emf.createEntityManager();
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
		 * creazione oggetto AsyncRequest
		 */
		Date now = new Date();
		AsyncRequest asyncRequest = new AsyncRequest();
		asyncRequest.setState(ExecutionState.STARTED);
		asyncRequest.setCreationDate(now);
		em.persist(asyncRequest);
		log.debug("creato nuovo oggetto AsyncRequest id={}", asyncRequest.getId());
		Map<String, String> asyncRequestParams = new HashMap<String, String>();
		/*
		 * creazione e schedulazione di un job per ogni url
		 * a partire dal file xls
		 */
		String destDir = pdfRepo+"/"+originalName.substring(0, originalName.lastIndexOf("."));
		log.debug("dest dir: {}", destDir);
		new File(destDir).mkdirs();
		Workbook wb = null;
		try {
			wb = WorkbookFactory.create(xlsFile);
		} catch (InvalidFormatException e) {
			em.getTransaction().rollback();
			em.close();
			throw new ServletException("File Excel non valido", e);
		}
		Sheet sheet = wb.getSheetAt(0);
		Integer groupId = Math.abs(originalName.hashCode());
		SchedulerFactory schedulerFactory = (StdSchedulerFactory) request.getServletContext()
                .getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY);
		Scheduler scheduler;
		try {
			scheduler = schedulerFactory.getScheduler();
			scheduler.standby(); //per evitare che i job partano prima che il thread corrente abbia terminato
		} catch (SchedulerException e) {
			em.getTransaction().rollback();
			em.close();
			throw new ServletException("Errore di creazione dello scheduler", e);
		}
		float scaleFactor = Float.valueOf(getFormField(items, "scale").getString());
		PdfConversionService service = PdfConversionFactory.create(scaleFactor);
		short countTotal = 0;
		for (Row row : sheet) {
			if (row.getPhysicalNumberOfCells()>0) {
				Cell cell = row.getCell(0);
				if (cell == null) {
					log.warn("trovata cella nulla riga={}, skip", row.getRowNum());
					continue;
				}
				Hyperlink link = cell.getHyperlink();
				if (link != null) {
					countTotal++;
					String url = link.getAddress();
					String testata = cell.getRichStringCellValue().getString();
					log.debug("crezione job per url={}, testata={}", url, testata);
					JobDataMap jobDataMap = new JobDataMap();
					jobDataMap.put("url", url);
					jobDataMap.put("destDir", destDir);
					jobDataMap.put("pdfFileNamePrefix", testata);
					jobDataMap.put("requestId", asyncRequest.getId());
					jobDataMap.put("entityManagerFactory", emf);
					jobDataMap.put("service", service);
					//Integer jobId = url.hashCode();
					Integer jobId = new Long(System.currentTimeMillis()).intValue();
					JobDetail jobDetail = createJob(PdfConversionJob.class, "job"+jobId.toString(), groupId.toString(), jobDataMap);
					asyncRequestParams.put(jobDetail.getKey().toString()+".state", ExecutionState.STARTED.toString());
					asyncRequestParams.put(jobDetail.getKey().toString()+".url", url);
					Trigger trigger = createTrigger("trg"+jobId.toString(), groupId.toString());
					try {
						scheduler.scheduleJob(jobDetail, trigger);
					} catch (SchedulerException e) {
						em.getTransaction().rollback();
						em.close();
						throw new ServletException("Errore di schedulazione del job", e);
					}
				}
			}
		}
		asyncRequest.setParameters(asyncRequestParams);
		em.merge(asyncRequest);
		/*
		 * creazione oggetto PdfConversion
		 */
		PdfConversion pdfConversion = new PdfConversion();
		pdfConversion.setState(ExecutionState.STARTED);
		pdfConversion.setStartDate(now);
		pdfConversion.setAsyncRequest(asyncRequest);
		pdfConversion.setCountTotal(countTotal);
		em.persist(pdfConversion);
		log.debug("creato nuovo oggetto PdfConversion id={}", pdfConversion.getId());
		/*
		 * creazione e schedulazione del job controller
		 */
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("entityManagerFactory", emf);
		jobDataMap.put("requestId", asyncRequest.getId());
		jobDataMap.put("pdfConversionId", pdfConversion.getId());
		jobDataMap.put("destDir", destDir);
		jobDataMap.put("originalXlsFilePath", xlsFile.getAbsolutePath());
		jobDataMap.put("originalXlsFileName", originalName);
		JobDetail jobDetail = createJob(PdfConversionControllerJob.class, "ctrl", groupId.toString(), jobDataMap);
		Trigger trigger = createControllerTrigger("ctrlTrg", groupId.toString());
		try {
			log.debug("schedulazione job controller");
			scheduler.scheduleJob(jobDetail, trigger);
			scheduler.start();
		} catch (SchedulerException e) {
			em.getTransaction().rollback();
			em.close();
			throw new ServletException("Errore di schedulazione del job controller", e);
		}
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

	private Trigger createControllerTrigger(String triggerId, String groupId) {
		Trigger trigger = newTrigger()
				.withIdentity(triggerId, groupId)
				.withSchedule(simpleSchedule()
							.withIntervalInSeconds(15)
							.withMisfireHandlingInstructionNextWithRemainingCount()
							.repeatForever())
				.startAt(futureDate(120, IntervalUnit.SECOND))
				.build();
		return trigger;
	}

	private Trigger createTrigger(String triggerId, String groupId) {
		Trigger trigger = newTrigger()
				.withIdentity(triggerId, groupId)
				.withSchedule(simpleSchedule().withMisfireHandlingInstructionFireNow())
				.startAt(futureDate(30, IntervalUnit.SECOND))
				.build();
		return trigger;
	}

	private JobDetail createJob(Class<? extends Job> jobClass, String jobId, String groupId, JobDataMap jobDataMap) {
		JobDetail job = newJob(jobClass)
				.withIdentity(jobId, groupId)
				.usingJobData(jobDataMap)
				.build();
		return job;
	}

	private FileItem getFormField(List<FileItem> items, String fieldName) {
		for (FileItem fileItem : items) {
			if (fileItem.getFieldName().equals(fieldName)) {
				return fileItem;
			}
		}
		return null;
	}

}

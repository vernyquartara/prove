package it.quartara.boser.jobs;

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import it.quartara.boser.helper.PdfConversionHelper;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.PdfConversion;
import it.quartara.boser.model.PdfConversionItem;
import it.quartara.boser.service.PdfConversionFactory;
import it.quartara.boser.service.impl.PdfConversionMockServiceImpl;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlla periodicamente se la richiesta asincrona il cui ID è passato
 * nella JobDataMap è stata completata, quindi aggiorna lo stato.
 * @author webny
 *
 */
@DisallowConcurrentExecution
public class PdfConversionControllerJob implements Job {
	
	private static final Logger log = LoggerFactory.getLogger(PdfConversionControllerJob.class);
	
	private EntityManagerFactory emf;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		log.debug("avvio transazione");
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		
		/*
		 * se esiste ua conversione attualmente in corso,
		 * ne effettua il controllo dello stato
		 */
		PdfConversion currentConversion = null;
		TypedQuery<PdfConversion> query = 
				em.createQuery("from PdfConversion where state='STARTED'", PdfConversion.class);
		try {
			currentConversion = query.getSingleResult();
			log.info("conversione in corso id={}", currentConversion.getId());
		} catch (NoResultException e) {
			log.info("nessuna conversione attualmente in corso");
		} catch (NonUniqueResultException e) {
			em.getTransaction().rollback();
			em.close();
			throw new IllegalStateException("trovate più conversioni già avviate!" ,e);
		}
		if (currentConversion != null) {
			checkCurrentConversionState(em, currentConversion, context.getScheduler());
		} else {
			/*
			 * altrimenti controlla se ci sono conversioni da avviare
			 * ed avvia la meno recente in stato READY
			 */
			query =	em.createQuery("from PdfConversion where state='READY' order by creationDate", PdfConversion.class);
			List<PdfConversion> conversions = query.getResultList();
			if (conversions != null && conversions.size() > 0) {
				PdfConversion conversion = conversions.get(0);
				log.info("trovata nuova conversione da avviare: {}", conversion.getId());
				startConversion(em, conversion, context.getScheduler());
			} else {
				log.info("nessuna conversione da avviare");
			}
		}
		log.debug("esecuzione commit");
		em.getTransaction().commit();
		em.close();
		log.debug("transazione completata");
	}
	
	private void checkCurrentConversionState(EntityManager em, PdfConversion currentConversion, Scheduler scheduler) {
		Date now = new Date();
		/*
		 * se ci sono items in lavorazione non si effettua alcuna operazione
		 */
		log.info("controllo stato conversione in corso");
		if (currentConversion.getCountWorking() > 0) {
			log.info("sono presenti items in lavorazione");
			return;
		}
		/*
		 * se ci sono items READY si schedulano i relativi jobs e si termina
		 */
		if (currentConversion.getCountReady() > 0) {
			log.info("sono presenti jobs da schedulare");
			Parameter param = em.find(Parameter.class, "MAX_JOBS");
			int maxJobs = Math.min(currentConversion.getCountReady(), Integer.valueOf(param.getValue()));
			int count = 0;
			for (PdfConversionItem item : currentConversion.getItems()) {
				if (item.getState()==ExecutionState.READY && count < maxJobs) {
					try {
						scheduleConversionJob(scheduler, item, currentConversion.getDestDir());
						item.setState(ExecutionState.STARTED);
						item.setStartDate(now);
						em.merge(item);
						count++;
					} catch (SchedulerException e) {
						em.getTransaction().rollback();
						em.close();
						throw new IllegalStateException("Errore di schedulazione del job", e);
					}
				}
			}
			currentConversion.setLastUpdate(now);
			em.merge(currentConversion);
			return;
		}
		/*
		 * altrimenti la conversione è terminata
		 */
		short countCompleted = 0, countFailed = 0;
		log.debug("controllo stato per pdfConversionID: {}", currentConversion.getId());
		for (PdfConversionItem item : currentConversion.getItems()) {
			switch (item.getState()) {
			case	READY:
			case	STARTED:
				log.error("errore! item {} in stato {}", item.getId(), item.getState());
				throw new IllegalStateException("stato inconsistente per la conversione "+currentConversion.getId());
			case 	ERROR:
				countFailed++; break;
			default:
				countCompleted++;
			}
		}
		log.debug("tutti i job del gruppo hanno completato");
		/*
		 * se si arriva qui tutti i job hanno stato COMPLETED oppure ERROR
		 * creo una copia del xls originale, all'interno indico quali sono
		 * andati in errore, lo includo nello zip
		 */
		try {
			PdfConversionHelper.createXlsReport(currentConversion);
		} catch (IOException | InvalidFormatException e) {
			log.error("errore di elaborazione del report xls", e);
			em.getTransaction().rollback();
			em.close();
			return;
		}
		
		/*
		 * si può aggiornare lo stato della richiesta e quindi anche della conversione
		 * e creare il file zip
		 */
		currentConversion.setLastUpdate(now);
		currentConversion.setEndDate(now);
		
		File zipFile = null;
		try {
			zipFile = PdfConversionHelper.createZipFile(currentConversion.getDestDir());
		} catch (IOException e) {
			log.error("errore di creazione del file zip", e);
			em.getTransaction().rollback();
			em.close();
			return;
		}
		
		currentConversion.setState(countCompleted > 0 ? ExecutionState.COMPLETED : ExecutionState.ERROR);
		currentConversion.setCountCompleted(countCompleted);
		currentConversion.setCountFailed(countFailed);
		currentConversion.setZipFilePath(zipFile.getAbsolutePath());
		currentConversion.setFileSize(zipFile.length());
		log.debug("aggiornamento pdfConversion id={}, stato={}, completati={}, errori={}",
				currentConversion.getId(), currentConversion.getState(), countCompleted, countFailed);
		em.merge(currentConversion);
	}

	private void startConversion(EntityManager em, PdfConversion conversion, Scheduler scheduler) {
		/*
		 * avvia la conversione, creando un job per ogni item
		 * ma solo fino ad un massimo di MAX_JOBS items.
		 */
		Parameter param = em.find(Parameter.class, "MAX_JOBS");
		
		Date now = new Date();
		conversion.setState(ExecutionState.STARTED);
		conversion.setStartDate(now);
		conversion.setLastUpdate(now);
		int maxJobs = Math.min(Integer.valueOf(param.getValue()), conversion.getCountTotal());
		for (int i = 0; i < maxJobs; i++) {
			PdfConversionItem item = conversion.getItems().get(i);
			try {
				scheduleConversionJob(scheduler, item, conversion.getDestDir());
				item.setState(ExecutionState.STARTED);
				item.setStartDate(now);
				em.merge(item);
			} catch (SchedulerException e) {
				em.getTransaction().rollback();
				em.close();
				throw new IllegalStateException("Errore di schedulazione del job", e);
			}
		}
		em.detach(param);
		em.merge(conversion);
	}
	
	private void scheduleConversionJob(Scheduler scheduler, PdfConversionItem item, String destDir) 
																						throws SchedulerException {
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("url", item.getUrl());
		jobDataMap.put("destDir", destDir);
		jobDataMap.put("pdfFileNamePrefix", item.getPdfFileNamePrefix());
		jobDataMap.put("entityManagerFactory", emf);
		jobDataMap.put("service", PdfConversionFactory.create());
		jobDataMap.put("conversionItemId", item.getId());
		String jobId = "JOB"+item.getId();
		String groupId = "GRP"+item.getConversion().getId().toString();
		JobDetail jobDetail = createJob(PdfConversionJob.class, jobId, groupId, jobDataMap);
		Trigger trigger = createTrigger("trg"+jobId.toString(), groupId.toString());
		log.info("schedulazione job per url={}", item.getUrl());
		scheduler.scheduleJob(jobDetail, trigger);
	}
	
	private JobDetail createJob(Class<? extends Job> jobClass, String jobId, String groupId, JobDataMap jobDataMap) {
		JobDetail job = newJob(jobClass)
				.withIdentity(jobId, groupId)
				.usingJobData(jobDataMap)
				.build();
		return job;
	}
	
	private Trigger createTrigger(String triggerId, String groupId) {
		Trigger trigger = newTrigger()
				.withIdentity(triggerId, groupId)
				.withSchedule(simpleSchedule().withMisfireHandlingInstructionFireNow())
				.startAt(futureDate(10, IntervalUnit.SECOND))
				.build();
		return trigger;
	}

	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}

}

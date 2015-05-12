package it.quartara.boser.jobs;


import static org.quartz.DateBuilder.futureDate;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.model.PdfConversion;
import it.quartara.boser.model.PdfConversionItem;
import it.quartara.boser.service.PdfConversionService;

import java.io.File;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;

import org.apache.commons.lang3.time.DateUtils;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Effettua la conversione da html a PDF di un singolo URL.
 * Legge dalla JobDataMap l'id della richiesta asincrona, dalla quale
 * ricava i parametri.
 * Effettua la conversione quindi aggiorna la richiesta asincrona.
 * @author webny
 *
 */
public class PdfConversionJob implements Job {
	
	private static final Logger log = LoggerFactory.getLogger(PdfConversionJob.class);
	
	private String url;
	private String destDir;
	private String pdfFileNamePrefix;
	private Long conversionItemId;
	private EntityManagerFactory emf;
	private PdfConversionService service;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		String jobKey = context.getJobDetail().getKey().toString();
		EntityManager em = emf.createEntityManager();
		/*
		 * conversione
		 */
		File pdfFile = null;
		try {
			log.debug("avvio conversione in pdf, job: {}", jobKey);
			pdfFile = service.convertToPdf(destDir, url, pdfFileNamePrefix);
		} catch (OutOfMemoryError ofme) {
			log.error ("OutOfMemoryError sul job {}", jobKey);
			Date now = new Date();
			em.getTransaction().begin();
			PdfConversionItem conversionItem = em.find(PdfConversionItem.class, conversionItemId, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
			PdfConversion currentConversion = conversionItem.getConversion();
			if ( now.before(DateUtils.addSeconds(currentConversion.getCreationDate(), 300))) {
				/*
				 * se non sono ancora passati cinque minuti dalla creazione della richiesta
				 * il job viene rischedulato per l'esecuzione
				 */
				log.info("Il job {} viene rischedulato per l'esecuzione tra 10 secondi", jobKey);
				Trigger originalTrigger = context.getTrigger();
				@SuppressWarnings("unchecked")
				TriggerBuilder<Trigger> builder = (TriggerBuilder<Trigger>) originalTrigger.getTriggerBuilder();
				Trigger deplayedTrigger = builder.startAt(futureDate(10, IntervalUnit.SECOND)).build();
				Scheduler scheduler = context.getScheduler();
				try {
					scheduler.rescheduleJob(originalTrigger.getKey(), deplayedTrigger);
				} catch (SchedulerException e) {
					log.error ("errore di ri-schedulazione del job {}, si imposta stato=ERROR", jobKey);
					conversionItem.setState(ExecutionState.ERROR);
					conversionItem.setEndDate(now);
					currentConversion.setLastUpdate(now);
				}
			} else {
				/*
				 * altrimenti si imposta lo stato a ERROR per evitare che riesegua all'infinito
				 */
				log.info("Il job {} si considera in timeout, impostazione stato=ERROR", jobKey);
				conversionItem.setState(ExecutionState.ERROR);
				conversionItem.setEndDate(now);
				currentConversion.setLastUpdate(now);
			}
			em.getTransaction().commit();
			em.close();
			throw new JobExecutionException(ofme);
		}
		
		/*
		 * aggiornamento request
		 */
		em.getTransaction().begin();
		try {
			Date now = new Date();
			PdfConversionItem conversionItem = em.find(PdfConversionItem.class, conversionItemId, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
			PdfConversion currentConversion = conversionItem.getConversion();
			ExecutionState state = ExecutionState.COMPLETED;
			if (pdfFile == null) {
				state = ExecutionState.ERROR;
			}
			conversionItem.setState(state);
			conversionItem.setEndDate(now);
			currentConversion.setLastUpdate(now);
			log.debug("aggiornamento stato={} per il job {}", state, jobKey);
			em.merge(currentConversion);
			em.getTransaction().commit();
		} catch (Exception e) {
			/*
			 * di solito si tratta di un problema di optimistick lock.
			 * si riesegue il job.
			 */
			log.warn("problema di commit della transazione, "
					+ "il job {} viene rischedulato per l'esecuzione", jobKey);
			throw new JobExecutionException(e, Boolean.TRUE);
		} finally {
			em.close();
		}
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setPdfFileNamePrefix(String pdfFileNamePrefix) {
		this.pdfFileNamePrefix = pdfFileNamePrefix;
	}

	public void setDestDir(String destDir) {
		this.destDir = destDir;
	}

	public void setConversionItemId(Long conversionItemId) {
		this.conversionItemId = conversionItemId;
	}

	public void setService(PdfConversionService service) {
		this.service = service;
	}

	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}


}

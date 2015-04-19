package it.quartara.boser.jobs;


import static org.quartz.DateBuilder.futureDate;
import it.quartara.boser.model.AsyncRequest;
import it.quartara.boser.model.ExecutionState;
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
	private Long requestId;
	private EntityManagerFactory emf;
	private PdfConversionService service;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		String jobKey = context.getJobDetail().getKey().toString();
		EntityManager em = emf.createEntityManager();
		try {
			log.debug("avvio conversione in pdf, job: {}", jobKey);
			File pdfFile = service.convertToPdf(destDir, url, pdfFileNamePrefix);
			
			em.getTransaction().begin();
			AsyncRequest request = em.find(AsyncRequest.class, requestId, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
			ExecutionState state = ExecutionState.COMPLETED;
			if (pdfFile == null) {
				state = ExecutionState.ERROR;
			}
			request.getParameters().put(jobKey+".state", state.toString());
			request.setLastUpdate(new Date());
			
			log.debug("aggiornamento stato={} per il job {}", state, jobKey);
			try {
				em.getTransaction().commit();
			} catch (Exception e) {
				/*
				 * di solito si tratta di un problema di optimistick lock.
				 */
				log.warn("problema di commit della transazione, "
						+ "il job {} viene rischedulato per l'esecuzione", jobKey);
				throw new JobExecutionException(e, Boolean.TRUE);
			}
		} catch (OutOfMemoryError ofme) {
			log.error ("OutOfMemoryError sul job {}", jobKey);
			Date now = new Date();
			em.getTransaction().begin();
			AsyncRequest request = em.find(AsyncRequest.class, requestId, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
			if ( now.before(DateUtils.addSeconds(request.getCreationDate(), 120))) {
				/*
				 * se non sono ancora passati due minuti dalla creazione della richiesta
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
					request.getParameters().put(jobKey+".state", ExecutionState.ERROR.toString());
					request.setLastUpdate(now);
				}
			} else {
				/*
				 * altrimenti si imposta lo stato a ERROR per evitare che riesegua all'infinito
				 */
				log.info("Il job {} si considera in timeout, impostazione stato=ERROR", jobKey);
				request.getParameters().put(jobKey+".state", ExecutionState.ERROR.toString());
				request.setLastUpdate(now);
			}
			em.getTransaction().commit();
			throw new JobExecutionException(ofme);
		} catch (Exception e) {
			/*
			 * generic exception other than OutOfMemoryError
			 * dovrebbe essere un errore estemporaneo, si riprova
			 */
			log.error("rilevata eccezione non prevista");
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

	public void setRequestId(Long requestId) {
		this.requestId = requestId;
	}

	public void setService(PdfConversionService service) {
		this.service = service;
	}

	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}


}

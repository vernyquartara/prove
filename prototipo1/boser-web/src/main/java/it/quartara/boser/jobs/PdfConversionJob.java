package it.quartara.boser.jobs;

import it.quartara.boser.helper.UrlHelper;
import it.quartara.boser.model.AsyncRequest;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.service.PdfConversionFactory;
import it.quartara.boser.service.PdfConversionService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.fit.cssbox.demo.ImageRenderer;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
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
		log.debug("avvio conversione in pdf. url: {}", url);
		File pdfFile = service.convertToPdf(destDir, url, pdfFileNamePrefix);
		
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		AsyncRequest request = em.find(AsyncRequest.class, requestId, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
		String jobKey = context.getJobDetail().getKey().toString();
		ExecutionState state = ExecutionState.COMPLETED;
		if (pdfFile == null) {
			state = ExecutionState.ERROR;
		}
		request.getParameters().put(jobKey+".state", state.toString());
		request.setLastUpdate(new Date());
		
		log.debug("aggiornamento stato={} per il job {}", state, jobKey);
		em.getTransaction().commit();
		em.close();
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

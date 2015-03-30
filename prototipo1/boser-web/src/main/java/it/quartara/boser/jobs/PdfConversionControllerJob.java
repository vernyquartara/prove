package it.quartara.boser.jobs;

import it.quartara.boser.model.AsyncRequest;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.model.PdfConversion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlla periodicamente se la richiesta asincrona il cui ID è passato
 * nella JobDataMap è stata completata, quindi aggiorna lo stato.
 * @author webny
 *
 */
public class PdfConversionControllerJob implements Job {
	
	private static final Logger log = LoggerFactory.getLogger(PdfConversionControllerJob.class);
	
	private EntityManagerFactory emf;
	private Long requestId;
	private Long pdfConversionId;
	private String destDir;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		log.debug("avvio transazione");
		String jobGroup = context.getJobDetail().getKey().getGroup();
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		AsyncRequest request = em.find(AsyncRequest.class, requestId, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
		Map<String, String> params = request.getParameters();
		short countCompleted = 0, countFailed = 0;
		log.debug("controllo stato per il gruppo: {}", jobGroup);
		for (Entry<String, String> param : params.entrySet()) {
			/*
			 * TODO si potrebbe controllare anche il gruppo e parametrizzare le stringhe
			 */
			if (param.getKey().contains("state")) {
				ExecutionState state = ExecutionState.valueOf(param.getValue());
				log.debug("jobKey={}, state={}", param.getKey(), state);
				switch (state) {
				case	STARTED:
					log.debug("not completed, rolling back");
					em.getTransaction().rollback();
					em.close();
					return;
				case 	ERROR:
					countFailed++; break;
				default:
					countCompleted++;
				}
			}
		}
		log.debug("tutti i job del gruppo hanno completato");
		/*
		 * se si arriva qui tutti i job hanno stato COMPLETED oppure ERROR
		 * si può aggiornare lo stato della richiesta e quindi anche della conversione
		 * e creare il file zip
		 */
		Date now = new Date();
		request.setState(ExecutionState.COMPLETED);
		request.setLastUpdate(now);
		em.merge(request);
		
		File zipFile = null;
		try {
			zipFile = createZipFile(destDir);
		} catch (IOException e) {
			log.error("errore di creazione del file zip", e);
			em.getTransaction().rollback();
			em.close();
			return;
		}
		
		PdfConversion conversion = em.find(PdfConversion.class, pdfConversionId);
		conversion.setState(countCompleted > 0 ? ExecutionState.COMPLETED : ExecutionState.ERROR);
		conversion.setCountCompleted(countCompleted);
		conversion.setCountFailed(countFailed);
		conversion.setFilePath(zipFile.getAbsolutePath());
		conversion.setFileSize(zipFile.length());
		log.debug("aggiornamento pdfConversion id={}, stato={}, completati={}, errori={}",
				pdfConversionId, conversion.getState(), countCompleted, countFailed);
		em.merge(conversion);
		log.debug("committing transaction");
		em.getTransaction().commit();
		em.close();
		try {
			/*
			 * lavoro terminato, questo job può essere deschedulato
			 */
			log.debug("unscheduling controller job");
			context.getScheduler().unscheduleJob(context.getTrigger().getKey());
		} catch (SchedulerException e) {
			throw new JobExecutionException(e);
		}
	}

	private File createZipFile(String dirToZip) throws IOException {
		String zipFileName = dirToZip.substring(0, dirToZip.lastIndexOf("/"))
				+ "/" + dirToZip.substring(dirToZip.lastIndexOf("/") + 1)
				+ ".zip";
		File zipFile = new File(zipFileName);
		log.debug("avvio creazione file zip: {}", zipFile.getAbsolutePath());

		FileOutputStream fos = new FileOutputStream(zipFile);
		ZipOutputStream zos = new ZipOutputStream(fos);

		byte[] buffer = new byte[65536];
		File[] files = new File(dirToZip).listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".pdf");
			}
		});
		for (File file : files) {
			log.debug("aggiunta file: {}", file.getName());
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
		log.debug("file zip creato");
		return zipFile;
	}

	public void setRequestId(Long requestId) {
		this.requestId = requestId;
	}

	public void setPdfConversionId(Long pdfConversionId) {
		this.pdfConversionId = pdfConversionId;
	}

	public void setDestDir(String destDir) {
		this.destDir = destDir;
	}
	
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}

}

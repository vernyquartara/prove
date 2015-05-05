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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.quartz.DisallowConcurrentExecution;
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
@DisallowConcurrentExecution
public class PdfConversionControllerJob implements Job {
	
	private static final Logger log = LoggerFactory.getLogger(PdfConversionControllerJob.class);
	
	private EntityManagerFactory emf;
	private Long requestId;
	private Long pdfConversionId;
	private String destDir;
	private String originalXlsFilePath;
	private String originalXlsFileName;

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
		 * creo una copia del xls originale, all'interno indico quali sono
		 * andati in errore, lo includo nello zip
		 */
		try {
			createXlsReport(params);
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
	
	private void createXlsReport(Map<String, String> params) throws IOException, InvalidFormatException {
		Map<String, ExecutionState> urlStateMap = getUrlStateMap(params);
		File originalXlsFile = new File(originalXlsFilePath);
		FileUtils.copyFileToDirectory(originalXlsFile, new File(destDir));
		File xlsReport = new File(destDir, originalXlsFileName);
		FileInputStream in = new FileInputStream(xlsReport);
		Workbook wb = WorkbookFactory.create(in);
		Sheet sheet = wb.getSheetAt(0);
		Font defaultFont = wb.createFont();
	    defaultFont.setFontHeightInPoints((short)8);
	    defaultFont.setFontName("Arial");
		CellStyle cellStyle = wb.createCellStyle();
		cellStyle.setFont(defaultFont);
		cellStyle.setFillForegroundColor(HSSFColor.RED.index);
		cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
		cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		for (Row row : sheet) {
			if (row.getPhysicalNumberOfCells()>0) {
				Cell cell = row.getCell(0);
				if (cell == null) {
					log.debug("trovata cella nulla riga={}, skip", row.getRowNum());
					continue;
				}
				Hyperlink link = cell.getHyperlink();
				if (link != null && urlStateMap.get(link.getAddress())==ExecutionState.ERROR) {
					cell.setCellStyle(cellStyle);
				}
			}
		}
		in.close();
		FileOutputStream out = new FileOutputStream(xlsReport);
		wb.write(out);
		out.close();
		wb.close();
	}
	
	private Map<String, ExecutionState> getUrlStateMap(Map<String, String> params) {
		Map<String, ExecutionState> urlStateMap = new HashMap<String, ExecutionState>();
		for (Entry<String, String> param : params.entrySet()) {
			String paramKey = param.getKey();
			if (paramKey.contains("state")) {
				String jobKey = paramKey.substring(0, paramKey.indexOf(".state"));
				String url = params.get(jobKey+".url");
				ExecutionState state = ExecutionState.valueOf(param.getValue());
				urlStateMap.put(url, state);
			}
		}
		return urlStateMap;
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
				return name.toLowerCase().endsWith(".pdf")
						|| name.toLowerCase().endsWith(".xls");
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
	
	public void setOriginalXlsFilePath(String originalXlsFilePath) {
		this.originalXlsFilePath = originalXlsFilePath;
	}
	
	public void setOriginalXlsFileName(String originalXlsFileName) {
		this.originalXlsFileName = originalXlsFileName;
	}

}

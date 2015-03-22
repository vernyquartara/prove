package it.quartara.boser.jobs;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import it.quartara.boser.model.AsyncRequest;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.service.PdfConversionFactory;
import it.quartara.boser.service.PdfConversionService;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PdfConversionFactory.class})
public class PdfConversionJobTest {

	@Test
	public void testShouldUpdateStateToCompletedIfConversionIsSuccesful() throws JobExecutionException {
		String url = "http://openjpa.apache.org/builds/1.2.3/apache-openjpa/docs/ref_guide_dbsetup_isolation.html";
		String destDir = "target/test-output";
		String prefix = "apache.org";

		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		AsyncRequest request = new AsyncRequest();
		request.setState(ExecutionState.STARTED);
		Map<String, String> params = new HashMap<String, String>();
		Long requestId = 11L;
		params.put("job1group.job1key.state", "STARTED");
		request.setParameters(params);
		when(em.find(AsyncRequest.class, requestId, LockModeType.OPTIMISTIC_FORCE_INCREMENT)).thenReturn(request);
		
		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		
		PdfConversionService service = mock(PdfConversionService.class);
		mockStatic(PdfConversionFactory.class);
		when(PdfConversionFactory.create()).thenReturn(service);
		
		File pdfFile = mock(File.class);
		when(service.convertToPdf(destDir, url, prefix)).thenReturn(pdfFile);
		
		PdfConversionJob job = new PdfConversionJob();
		JobExecutionContext context = mock(JobExecutionContext.class);
		JobDetail jobDetail = mock(JobDetail.class);
		JobKey jobKey = new JobKey("job1key", "job1group");
		when(context.getJobDetail()).thenReturn(jobDetail);
		when(jobDetail.getKey()).thenReturn(jobKey);
		
		job.setUrl(url);
		job.setDestDir(destDir);
		job.setRequestId(requestId);
		job.setEntityManagerFactory(emf);
		job.setPdfFileNamePrefix(prefix);
		job.setService(service);
		job.execute(context);
		
		assertEquals("COMPLETED", params.get("job1group.job1key.state"));
		verify(transaction).begin();
		verify(transaction).commit();
		verify(em).close();
		
	}
	
	@Test
	public void testShouldUpdateStateToErrorIfConversionFails() throws JobExecutionException {
		String url = "http://openjpa.apache.org/builds/1.2.3/apache-openjpa/docs/ref_guide_dbsetup_isolation.html";
		String destDir = "target/test-output";
		String prefix = "apache.org";
		
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		AsyncRequest request = new AsyncRequest();
		request.setState(ExecutionState.STARTED);
		Map<String, String> params = new HashMap<String, String>();
		Long requestId = 11L;
		params.put("job1group.job1key.state", "STARTED");
		request.setParameters(params);
		when(em.find(AsyncRequest.class, requestId, LockModeType.OPTIMISTIC_FORCE_INCREMENT)).thenReturn(request);
		
		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		
		PdfConversionService service = mock(PdfConversionService.class);
		mockStatic(PdfConversionFactory.class);
		when(PdfConversionFactory.create()).thenReturn(service);
		
		when(service.convertToPdf(destDir, url, prefix)).thenReturn(null);
		
		PdfConversionJob job = new PdfConversionJob();
		JobExecutionContext context = mock(JobExecutionContext.class);
		JobDetail jobDetail = mock(JobDetail.class);
		JobKey jobKey = new JobKey("job1key", "job1group");
		when(context.getJobDetail()).thenReturn(jobDetail);
		when(jobDetail.getKey()).thenReturn(jobKey);
		
		job.setUrl(url);
		job.setDestDir(destDir);
		job.setRequestId(requestId);
		job.setEntityManagerFactory(emf);
		job.setPdfFileNamePrefix(prefix);
		job.setService(service);
		job.execute(context);
		
		assertEquals("ERROR", params.get("job1group.job1key.state"));
		verify(transaction).begin();
		verify(transaction).commit();
		verify(em).close();
		
	}
}

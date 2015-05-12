package it.quartara.boser.jobs;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import it.quartara.boser.model.AsyncRequest;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.model.PdfConversion;

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
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PdfConversionControllerJob.class})
public class PdfConversionControllerJobTest {
/*
	@Test
	public void testShouldDoNothingIfNotAllJobsHaveCompleted() throws JobExecutionException {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		AsyncRequest request = new AsyncRequest();
		request.setState(ExecutionState.STARTED);
		Map<String, String> params = new HashMap<String, String>();
		Long requestId = 1L;
		params.put("job1key.job1group.state", "STARTED");
		params.put("job2key.job1group.state", "COMPLETED");
		request.setParameters(params);
		when(em.find(AsyncRequest.class, requestId, LockModeType.OPTIMISTIC_FORCE_INCREMENT)).thenReturn(request);
		
		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		
		JobExecutionContext context = mock(JobExecutionContext.class);
		
		PdfConversionControllerJob job = new PdfConversionControllerJob();
		job.setEntityManagerFactory(emf);
		job.setRequestId(requestId);
		job.execute(context);
		
		assertEquals(ExecutionState.STARTED, request.getState());
		verify(transaction).begin();
		verify(transaction).rollback();
		verify(em).close();
	}
	
	@Test
	public void testShouldSetCompletedStateIfAtLeastOneJobHaveCompletedAndOthersFailed() throws Exception {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		AsyncRequest request = new AsyncRequest();
		request.setState(ExecutionState.STARTED);
		Map<String, String> params = new HashMap<String, String>();
		Long requestId = 1L;
		params.put("job1key.job1group.state", "COMPLETED");
		params.put("job2key.job1group.state", "ERROR");
		request.setParameters(params);
		when(em.find(AsyncRequest.class, requestId, LockModeType.OPTIMISTIC_FORCE_INCREMENT)).thenReturn(request);
		
		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		
		Long pdfConversionId = 55L;
		PdfConversion conversion = new PdfConversion();
		conversion.setState(ExecutionState.STARTED);
		when(em.find(PdfConversion.class, pdfConversionId)).thenReturn(conversion);
		
		JobExecutionContext context = mock(JobExecutionContext.class);
		Scheduler scheduler = mock(Scheduler.class);
		Trigger trigger = mock(Trigger.class);
		TriggerKey triggerKey = new TriggerKey("");
		when(context.getScheduler()).thenReturn(scheduler);
		when(context.getTrigger()).thenReturn(trigger);
		when(trigger.getKey()).thenReturn(triggerKey);
		
		String destDir = "target/test-output/conversion";
		PdfConversionControllerJob job = spy(new PdfConversionControllerJob());
		doReturn(new File(destDir+".zip")).when(job,"createZipFile", destDir);
		
		job.setEntityManagerFactory(emf);
		job.setRequestId(requestId);
		job.setPdfConversionId(pdfConversionId);
		job.setDestDir(destDir);
		job.execute(context);
		
		assertEquals(ExecutionState.COMPLETED, request.getState());
		assertEquals(ExecutionState.COMPLETED, conversion.getState());
		assertEquals((short)1, conversion.getCountCompleted());
		assertEquals((short)1, conversion.getCountFailed());
		assertThat(conversion.getZipFilePath(), endsWith("target/test-output/conversion.zip"));
		verify(transaction).begin();
		verify(transaction).commit();
		verify(em).merge(request);
		verify(em).merge(conversion);
		verify(em).close();
		verify(scheduler).unscheduleJob(triggerKey);
	}
	@Test
	public void testShouldSetErrorStateIfAllJobsFailed() throws Exception {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		AsyncRequest request = new AsyncRequest();
		request.setState(ExecutionState.STARTED);
		Map<String, String> params = new HashMap<String, String>();
		Long requestId = 1L;
		params.put("job1key.job1group.state", "ERROR");
		params.put("job2key.job1group.state", "ERROR");
		request.setParameters(params);
		when(em.find(AsyncRequest.class, requestId, LockModeType.OPTIMISTIC_FORCE_INCREMENT)).thenReturn(request);
		
		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		
		Long pdfConversionId = 55L;
		PdfConversion conversion = new PdfConversion();
		conversion.setState(ExecutionState.STARTED);
		when(em.find(PdfConversion.class, pdfConversionId)).thenReturn(conversion);
		
		JobExecutionContext context = mock(JobExecutionContext.class);
		Scheduler scheduler = mock(Scheduler.class);
		Trigger trigger = mock(Trigger.class);
		TriggerKey triggerKey = new TriggerKey("");
		when(context.getScheduler()).thenReturn(scheduler);
		when(context.getTrigger()).thenReturn(trigger);
		when(trigger.getKey()).thenReturn(triggerKey);
		
		String destDir = "target/test-output/conversion";
		PdfConversionControllerJob job = spy(new PdfConversionControllerJob());
		doReturn(new File(destDir+".zip")).when(job,"createZipFile", destDir);
		
		job.setEntityManagerFactory(emf);
		job.setRequestId(requestId);
		job.setPdfConversionId(pdfConversionId);
		job.setDestDir(destDir);
		job.execute(context);
		
		assertEquals(ExecutionState.COMPLETED, request.getState());
		assertEquals(ExecutionState.ERROR, conversion.getState());
		assertEquals((short)0, conversion.getCountCompleted());
		assertEquals((short)2, conversion.getCountFailed());
		assertThat(conversion.getZipFilePath(), endsWith("target/test-output/conversion.zip"));
		verify(transaction).begin();
		verify(transaction).commit();
		verify(em).merge(request);
		verify(em).merge(conversion);
		verify(em).close();
		verify(scheduler).unscheduleJob(triggerKey);
	}
	*/
}

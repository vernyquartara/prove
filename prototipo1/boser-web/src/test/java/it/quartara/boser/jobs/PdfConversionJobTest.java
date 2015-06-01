package it.quartara.boser.jobs;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.model.PdfConversion;
import it.quartara.boser.model.PdfConversionItem;
import it.quartara.boser.service.PdfConversionFactory;
import it.quartara.boser.service.PdfConversionService;

import java.io.File;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PdfConversionFactory.class, PdfConversionJob.class})
@MockPolicy(Slf4jMockPolicy.class)
public class PdfConversionJobTest {
	
	@SuppressWarnings("unchecked")
	@Test(expected=JobExecutionException.class)
	public void testShouldRescheduleIfOutOfMemoryErrorBeforeTimeout() throws Exception {
		String url = "http://openjpa.apache.org/builds/1.2.3/apache-openjpa/docs/ref_guide_dbsetup_isolation.html";
		String destDir = "target/test-output";
		String prefix = "apache.org";
		
		PdfConversionJob job = new PdfConversionJob();
		JobExecutionContext context = mock(JobExecutionContext.class);
		JobDetail jobDetail = mock(JobDetail.class);
		JobKey jobKey = new JobKey("job1key", "job1group");
		when(context.getJobDetail()).thenReturn(jobDetail);
		when(jobDetail.getKey()).thenReturn(jobKey);
		
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		
		PdfConversionItem mockItem = new PdfConversionItem();
		PdfConversion mockConversion = new PdfConversion();
		mockItem.setConversion(mockConversion);
		when(em.find(eq(PdfConversionItem.class),  anyLong(), eq(LockModeType.OPTIMISTIC_FORCE_INCREMENT))).thenReturn(mockItem);
		
		Date creationDate = DateUtils.parseDate("30/05/15 19.10", "dd/MM/yy HH.mm");
		mockConversion.setCreationDate(creationDate);
		Date checkDate = DateUtils.parseDate("30/05/15 19.12", "dd/MM/yy HH.mm");
		whenNew(Date.class).withNoArguments().thenReturn(checkDate);
		
		Trigger originalTrigger = mock(Trigger.class);
		when(context.getTrigger()).thenReturn(originalTrigger);
		@SuppressWarnings({ "rawtypes" })
		TriggerBuilder builder = mock(TriggerBuilder.class);
		when(originalTrigger.getTriggerBuilder()).thenReturn(builder);
		when(builder.startAt(any(Date.class))).thenReturn(builder);
		
		Scheduler scheduler = mock(Scheduler.class);
		when(context.getScheduler()).thenReturn(scheduler);
		
		PdfConversionService service = mock(PdfConversionService.class);
		when(service.convertToPdf(destDir, url, prefix)).thenThrow(new OutOfMemoryError());
		
		job.setUrl(url);
		job.setDestDir(destDir);
		job.setEntityManagerFactory(emf);
		job.setPdfFileNamePrefix(prefix);
		job.setService(service);
		job.execute(context);
		
		verify(scheduler).rescheduleJob(any(TriggerKey.class), any(Trigger.class));
		verify(transaction).begin();
		verify(transaction).commit();
		verify(em).close();
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected=JobExecutionException.class)
	public void testShouldRescheduleIfOutOfMemoryErrorBeforeTimeoutKO() throws Exception {
		String url = "http://openjpa.apache.org/builds/1.2.3/apache-openjpa/docs/ref_guide_dbsetup_isolation.html";
		String destDir = "target/test-output";
		String prefix = "apache.org";
		
		PdfConversionJob job = new PdfConversionJob();
		JobExecutionContext context = mock(JobExecutionContext.class);
		JobDetail jobDetail = mock(JobDetail.class);
		JobKey jobKey = new JobKey("job1key", "job1group");
		when(context.getJobDetail()).thenReturn(jobDetail);
		when(jobDetail.getKey()).thenReturn(jobKey);
		
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		
		PdfConversionItem mockItem = new PdfConversionItem();
		PdfConversion mockConversion = new PdfConversion();
		mockItem.setConversion(mockConversion);
		when(em.find(eq(PdfConversionItem.class),  anyLong(), eq(LockModeType.OPTIMISTIC_FORCE_INCREMENT))).thenReturn(mockItem);
		
		Date creationDate = DateUtils.parseDate("30/05/15 19.10", "dd/MM/yy HH.mm");
		mockConversion.setCreationDate(creationDate);
		Date checkDate = DateUtils.parseDate("30/05/15 19.12", "dd/MM/yy HH.mm");
		whenNew(Date.class).withNoArguments().thenReturn(checkDate);
		
		Trigger originalTrigger = mock(Trigger.class);
		when(context.getTrigger()).thenReturn(originalTrigger);
		@SuppressWarnings({ "rawtypes" })
		TriggerBuilder builder = mock(TriggerBuilder.class);
		when(originalTrigger.getTriggerBuilder()).thenReturn(builder);
		when(builder.startAt(any(Date.class))).thenReturn(builder);
		
		Scheduler scheduler = mock(Scheduler.class);
		when(context.getScheduler()).thenReturn(scheduler);
		doThrow(SchedulerException.class).when(scheduler).rescheduleJob(any(TriggerKey.class), any(Trigger.class));
		
		PdfConversionService service = mock(PdfConversionService.class);
		when(service.convertToPdf(destDir, url, prefix)).thenThrow(new OutOfMemoryError());
		
		job.setUrl(url);
		job.setDestDir(destDir);
		job.setEntityManagerFactory(emf);
		job.setPdfFileNamePrefix(prefix);
		job.setService(service);
		job.execute(context);
		
		assertEquals(ExecutionState.ERROR, mockItem.getState());
		assertEquals(checkDate, mockItem.getEndDate());
		assertEquals(checkDate, mockConversion.getLastUpdate());
		verify(transaction).begin();
		verify(transaction).commit();
		verify(em).close();
	}
	
	@Test(expected=JobExecutionException.class)
	public void testShouldSetErrorIfOutOfMemoryErrorAfterTimeout() throws Exception {
		String url = "http://openjpa.apache.org/builds/1.2.3/apache-openjpa/docs/ref_guide_dbsetup_isolation.html";
		String destDir = "target/test-output";
		String prefix = "apache.org";
		
		PdfConversionJob job = new PdfConversionJob();
		JobExecutionContext context = mock(JobExecutionContext.class);
		JobDetail jobDetail = mock(JobDetail.class);
		JobKey jobKey = new JobKey("job1key", "job1group");
		when(context.getJobDetail()).thenReturn(jobDetail);
		when(jobDetail.getKey()).thenReturn(jobKey);
		
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		
		PdfConversionItem mockItem = new PdfConversionItem();
		PdfConversion mockConversion = new PdfConversion();
		mockItem.setConversion(mockConversion);
		when(em.find(eq(PdfConversionItem.class),  anyLong(), eq(LockModeType.OPTIMISTIC_FORCE_INCREMENT))).thenReturn(mockItem);
		
		Date creationDate = DateUtils.parseDate("30/05/15 19.10", "dd/MM/yy HH.mm");
		mockConversion.setCreationDate(creationDate);
		Date checkDate = DateUtils.parseDate("30/05/15 19.16", "dd/MM/yy HH.mm");
		whenNew(Date.class).withNoArguments().thenReturn(checkDate);
		
		
		PdfConversionService service = mock(PdfConversionService.class);
		when(service.convertToPdf(destDir, url, prefix)).thenThrow(new OutOfMemoryError());
		
		job.setUrl(url);
		job.setDestDir(destDir);
		job.setEntityManagerFactory(emf);
		job.setPdfFileNamePrefix(prefix);
		job.setService(service);
		job.execute(context);
		
		assertEquals(ExecutionState.ERROR, mockItem.getState());
		assertEquals(checkDate, mockItem.getEndDate());
		assertEquals(checkDate, mockConversion.getLastUpdate());
		verify(transaction).begin();
		verify(transaction).commit();
		verify(em).close();
	}
	
	@Test
	public void testConversionOk() throws Exception {
		String url = "http://openjpa.apache.org/builds/1.2.3/apache-openjpa/docs/ref_guide_dbsetup_isolation.html";
		String destDir = "target/test-output";
		String prefix = "apache.org";
		
		PdfConversionJob job = new PdfConversionJob();
		JobExecutionContext context = mock(JobExecutionContext.class);
		JobDetail jobDetail = mock(JobDetail.class);
		JobKey jobKey = new JobKey("job1key", "job1group");
		when(context.getJobDetail()).thenReturn(jobDetail);
		when(jobDetail.getKey()).thenReturn(jobKey);
		
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		
		PdfConversionItem mockItem = new PdfConversionItem();
		PdfConversion mockConversion = new PdfConversion();
		mockItem.setConversion(mockConversion);
		when(em.find(eq(PdfConversionItem.class),  anyLong(), eq(LockModeType.OPTIMISTIC_FORCE_INCREMENT))).thenReturn(mockItem);
		
		PdfConversionService service = mock(PdfConversionService.class);
		File pdfFile = mock(File.class);
		when(service.convertToPdf(destDir, url, prefix)).thenReturn(pdfFile);
		
		Date checkDate = DateUtils.parseDate("30/05/15 19.16", "dd/MM/yy HH.mm");
		whenNew(Date.class).withNoArguments().thenReturn(checkDate);
		
		job.setUrl(url);
		job.setDestDir(destDir);
		job.setEntityManagerFactory(emf);
		job.setPdfFileNamePrefix(prefix);
		job.setService(service);
		job.execute(context);
		
		assertEquals(ExecutionState.COMPLETED, mockItem.getState());
		assertEquals(checkDate, mockItem.getEndDate());
		assertEquals(checkDate, mockConversion.getLastUpdate());
		verify(transaction).begin();
		verify(transaction).commit();
		verify(em).merge(mockConversion);
		verify(em).close();
	}

	@Test(expected=JobExecutionException.class)
	public void testConversionOkButTransactionFails() throws Exception {
		String url = "http://openjpa.apache.org/builds/1.2.3/apache-openjpa/docs/ref_guide_dbsetup_isolation.html";
		String destDir = "target/test-output";
		String prefix = "apache.org";
		
		PdfConversionJob job = new PdfConversionJob();
		JobExecutionContext context = mock(JobExecutionContext.class);
		JobDetail jobDetail = mock(JobDetail.class);
		JobKey jobKey = new JobKey("job1key", "job1group");
		when(context.getJobDetail()).thenReturn(jobDetail);
		when(jobDetail.getKey()).thenReturn(jobKey);
		
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		doThrow(Exception.class).when(transaction).commit();
		
		PdfConversionItem mockItem = new PdfConversionItem();
		PdfConversion mockConversion = new PdfConversion();
		mockItem.setConversion(mockConversion);
		when(em.find(eq(PdfConversionItem.class),  anyLong(), eq(LockModeType.OPTIMISTIC_FORCE_INCREMENT))).thenReturn(mockItem);
		
		PdfConversionService service = mock(PdfConversionService.class);
		File pdfFile = mock(File.class);
		when(service.convertToPdf(destDir, url, prefix)).thenReturn(pdfFile);
		
		Date checkDate = DateUtils.parseDate("30/05/15 19.16", "dd/MM/yy HH.mm");
		whenNew(Date.class).withNoArguments().thenReturn(checkDate);
		
		job.setUrl(url);
		job.setDestDir(destDir);
		job.setEntityManagerFactory(emf);
		job.setPdfFileNamePrefix(prefix);
		job.setService(service);
		job.execute(context);
		
		assertEquals(ExecutionState.COMPLETED, mockItem.getState());
		assertEquals(checkDate, mockItem.getEndDate());
		assertEquals(checkDate, mockConversion.getLastUpdate());
		verify(transaction).begin();
		verify(em).merge(mockConversion);
		verify(em).close();
	}
	
	@Test
	public void testConversionKO() throws Exception {
		String url = "http://openjpa.apache.org/builds/1.2.3/apache-openjpa/docs/ref_guide_dbsetup_isolation.html";
		String destDir = "target/test-output";
		String prefix = "apache.org";
		
		PdfConversionJob job = new PdfConversionJob();
		JobExecutionContext context = mock(JobExecutionContext.class);
		JobDetail jobDetail = mock(JobDetail.class);
		JobKey jobKey = new JobKey("job1key", "job1group");
		when(context.getJobDetail()).thenReturn(jobDetail);
		when(jobDetail.getKey()).thenReturn(jobKey);
		
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		
		PdfConversionItem mockItem = new PdfConversionItem();
		PdfConversion mockConversion = new PdfConversion();
		mockItem.setConversion(mockConversion);
		when(em.find(eq(PdfConversionItem.class),  anyLong(), eq(LockModeType.OPTIMISTIC_FORCE_INCREMENT))).thenReturn(mockItem);
		
		PdfConversionService service = mock(PdfConversionService.class);
		when(service.convertToPdf(destDir, url, prefix)).thenReturn(null);
		
		Date checkDate = DateUtils.parseDate("30/05/15 19.16", "dd/MM/yy HH.mm");
		whenNew(Date.class).withNoArguments().thenReturn(checkDate);
		
		job.setUrl(url);
		job.setDestDir(destDir);
		job.setEntityManagerFactory(emf);
		job.setPdfFileNamePrefix(prefix);
		job.setService(service);
		job.execute(context);
		
		assertEquals(ExecutionState.ERROR, mockItem.getState());
		assertEquals(checkDate, mockItem.getEndDate());
		assertEquals(checkDate, mockConversion.getLastUpdate());
		verify(transaction).begin();
		verify(transaction).commit();
		verify(em).merge(mockConversion);
		verify(em).close();
	}
	
}

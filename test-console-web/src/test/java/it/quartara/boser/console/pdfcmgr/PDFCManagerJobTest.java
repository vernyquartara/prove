package it.quartara.boser.console.pdfcmgr;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.StopInstancesRequest;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PDFCManagerJob.class, AWSHelper.class, PDFCManagerHelper.class})
@MockPolicy(Slf4jMockPolicy.class)
public class PDFCManagerJobTest {
	
	@Mock JobExecutionContext context;
	@Mock DataSource ds;
	@Mock Connection conn;
	@Mock Statement stat;
	@Mock ResultSet rs;
	@Mock AmazonEC2 ec2;
	@Mock Instance instance;
	
	private static final String DATE_PATTERN = "dd/MM/yy HH:mm:ss";
	
	@Before
	public void setup() throws SQLException {
		when(ds.getConnection()).thenReturn(conn);
	}

	@Test
	public void shouldDoNothingWhenConversionIsRunning() throws Exception {
		Date instanceDate = DateUtils.parseDate("05/07/15 11:49:30", DATE_PATTERN);
		AmazonEC2 ec2 = mock(AmazonEC2.class);
		Instance instance = mock(Instance.class);
		mockStatic(AWSHelper.class);
		when(AWSHelper.createAmazonEC2Client(anyString())).thenReturn(ec2);
		when(AWSHelper.getInstance(eq(ec2), anyString())).thenReturn(instance);
		InstanceState instanceState = mock(InstanceState.class);
		when(instance.getState()).thenReturn(instanceState);
		when(instanceState.getName()).thenReturn("running");
		
		PDFCManagerJob job = spy(new PDFCManagerJob());
		job.setInstanceDate(instanceDate);
		job.setDs(ds);
		doReturn(true).when(job, "isCurrentlyConverting", any(Connection.class));
		
		job.execute(context);
		
		verifyPrivate(job).invoke("isCurrentlyConverting", any(Connection.class));
	}
	@Test
	public void shouldStopInstanceAfterInterval() throws Exception {
		Date instanceDate = DateUtils.parseDate("05/07/15 11:49:30", DATE_PATTERN);
		Date lastConvDate = DateUtils.parseDate("05/07/15 11:52:12", DATE_PATTERN);
		Date now = DateUtils.parseDate("05/07/15 12:47:33", DATE_PATTERN);
		short standbyInterval = 50;
		
		Scheduler scheduler = mock(Scheduler.class);
		Trigger trigger = mock(Trigger.class);
		when(context.getScheduler()).thenReturn(scheduler);
		when(context.getTrigger()).thenReturn(trigger);
		
		when(conn.createStatement()).thenReturn(stat);
		when(stat.executeQuery(anyString())).thenReturn(rs);
		when(rs.getDate(1)).thenReturn(new java.sql.Date(lastConvDate.getTime()));
		whenNew(Date.class).withNoArguments().thenReturn(now);
		
		PDFCManagerJob job = spy(new PDFCManagerJob());
		job.setInstanceDate(instanceDate);
		job.setDs(ds);
		doReturn(false).when(job, "isCurrentlyConverting", any(Connection.class));
		doReturn(lastConvDate).when(job, "getLastConversionDate", any(Connection.class));
		doReturn(true).when(job, "isTimeToStandby", instanceDate, lastConvDate, standbyInterval);

		mockStatic(AWSHelper.class);
		when(AWSHelper.createAmazonEC2Client(anyString())).thenReturn(ec2);
		when(AWSHelper.getInstance(eq(ec2), anyString())).thenReturn(instance);
		InstanceState instanceState = mock(InstanceState.class);
		when(instance.getState()).thenReturn(instanceState);
		when(instanceState.getName()).thenReturn("running");
		mockStatic(PDFCManagerHelper.class);
		when(PDFCManagerHelper.getStandbyInterval(any(Connection.class))).thenReturn(standbyInterval);
		
		job.execute(context);
		
		verifyPrivate(job).invoke("isCurrentlyConverting", any(Connection.class));
		verify(ec2).stopInstances(any(StopInstancesRequest.class));
		verify(scheduler).unscheduleJob(any(TriggerKey.class));
	}
	
	@Test
	public void shouldntStopInstanceBeforeInterval() throws Exception {
		Date instanceDate = DateUtils.parseDate("05/07/15 11:49:30", DATE_PATTERN);
		Date lastConvDate = DateUtils.parseDate("05/07/15 11:52:12", DATE_PATTERN);
		Date now = DateUtils.parseDate("05/07/15 12:47:33", DATE_PATTERN);
		short standbyInterval = 50;
		
		mockStatic(AWSHelper.class);
		when(AWSHelper.createAmazonEC2Client(anyString())).thenReturn(ec2);
		when(AWSHelper.getInstance(eq(ec2), anyString())).thenReturn(instance);
		InstanceState instanceState = mock(InstanceState.class);
		when(instance.getState()).thenReturn(instanceState);
		when(instanceState.getName()).thenReturn("running");
		
		when(conn.createStatement()).thenReturn(stat);
		when(stat.executeQuery(anyString())).thenReturn(rs);
		when(rs.getDate(1)).thenReturn(new java.sql.Date(lastConvDate.getTime()));
		whenNew(Date.class).withNoArguments().thenReturn(now);
		
		mockStatic(PDFCManagerHelper.class);
		when(PDFCManagerHelper.getStandbyInterval(any(Connection.class))).thenReturn(standbyInterval);
		
		PDFCManagerJob job = spy(new PDFCManagerJob());
		job.setInstanceDate(instanceDate);
		job.setDs(ds);
		doReturn(false).when(job, "isCurrentlyConverting", any(Connection.class));
		doReturn(lastConvDate).when(job, "getLastConversionDate", any(Connection.class));
		doReturn(false).when(job, "isTimeToStandby", instanceDate, lastConvDate, standbyInterval);

		job.execute(context);
		
		verifyPrivate(job).invoke("isCurrentlyConverting", any(Connection.class));
	}
	
	@Test
	public void shouldNotUpdateInstanceDateIfLessThanOneHourHasPassed() throws Exception {
		Date instanceDate = DateUtils.parseDate("05/07/15 11:49:30", DATE_PATTERN);
		Date now = DateUtils.parseDate("05/07/15 12:47:33", DATE_PATTERN);
		whenNew(Date.class).withNoArguments().thenReturn(now);
		
		PDFCManagerJob job = new PDFCManagerJob();
		job.setInstanceDate(instanceDate);
		Whitebox.invokeMethod(job, "updateInstanceDate");
		assertEquals(instanceDate, Whitebox.getInternalState(job, "instanceDate"));
	}
	
	@Test
	public void shoulUpdateInstanceDateIfMoreThanOneHourHasPassed() throws Exception {
		Date instanceDate = DateUtils.parseDate("05/07/15 11:49:30", DATE_PATTERN);
		Date now = DateUtils.parseDate("05/07/15 12:51:33", DATE_PATTERN);
		whenNew(Date.class).withNoArguments().thenReturn(now);
		
		PDFCManagerJob job = new PDFCManagerJob();
		job.setInstanceDate(instanceDate);
		Whitebox.invokeMethod(job, "updateInstanceDate");
		assertEquals(DateUtils.addHours(instanceDate, 1), Whitebox.getInternalState(job, "instanceDate"));
	}
}

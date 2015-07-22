package it.quartara.boser.console.pdfcmgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.ee.servlet.QuartzInitializerListener;
import org.quartz.impl.StdSchedulerFactory;

public class PDFCManagerHelperTest {
	

	@Test
	public void testsScheduleStandbyJob() throws Exception {
		Date instanceDate = DateUtils.parseDate("05/07/15 15:09:30", "dd/MM/yy HH:mm:ss");
		
		DataSource ds = mock(DataSource.class);
		Connection conn = mock(Connection.class);
		when(ds.getConnection()).thenReturn(conn);
		Statement st = mock(Statement.class);
		when(conn.createStatement()).thenReturn(st);
		ResultSet rs = mock(ResultSet.class);
		when(st.executeQuery(anyString())).thenReturn(rs);
		when(rs.next()).thenReturn(Boolean.TRUE);
		when(rs.getShort(0)).thenReturn((short) 50);
		StdSchedulerFactory schedulerFactory = mock(StdSchedulerFactory.class);
		Scheduler scheduler = mock(Scheduler.class);
		ServletContext context = mock(ServletContext.class);
		when(context.getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY)).thenReturn(schedulerFactory);
		when(schedulerFactory.getScheduler()).thenReturn(scheduler);
		
		PDFCManagerHelper.scheduleStandbyJob(ds, context, instanceDate, true);
		
		ArgumentCaptor<JobDetail> jobDetail = ArgumentCaptor.forClass(JobDetail.class);
		ArgumentCaptor<Trigger> trigger = ArgumentCaptor.forClass(Trigger.class);
		verify(scheduler).scheduleJob(jobDetail.capture(), trigger.capture());
		
		assertEquals(jobDetail.getValue().getJobClass(), PDFCManagerJob.class);
		assertTrue(jobDetail.getValue().getJobDataMap().containsKey(PDFCManagerJob.INSTANCE_DATE_KEY));
		assertEquals(jobDetail.getValue().getJobDataMap().get(PDFCManagerJob.INSTANCE_DATE_KEY), instanceDate);
	}
}

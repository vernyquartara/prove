package it.quartara.boser.console.pdfcmgr;

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.ee.servlet.QuartzInitializerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDFCManagerHelper {
	
	private static final Logger log = LoggerFactory.getLogger(PDFCManagerHelper.class);
	
	private static final String SELECT_STANDBY_INTERVAL = "select VALUE from PARAMETERS where id = 'STANDBY_INTERVAL'";
	
	public static short getStandbyInterval(Connection conn) throws SQLException {
		short standbyInterval = -1;
		Statement stat = conn.createStatement();
		ResultSet rs = stat.executeQuery(SELECT_STANDBY_INTERVAL);
		if (rs.next()) {
			standbyInterval = rs.getShort(1);
			log.debug("intervallo di standby: {}", standbyInterval);
		} else {
			log.error("parametro 'STANDBY_INTERVAL' non presente in base dati");
		}
		return standbyInterval;
	}
	
	public static void scheduleStandbyJob(DataSource ds, ServletContext servletContext, Date instanceDate, boolean startNow) {
		short standbyInterval = -1;
		try {
			standbyInterval = getStandbyInterval(ds.getConnection());
			if (standbyInterval == -1) {
				log.error("impossibile schedulare il job di standby automatico");
				return;
			}
		} catch (SQLException e) {
			log.error("errore di lettura dal db, il job di standby non può essere schedulato", e);
			return;
		}
		if (startNow) {
			/*
			 * se richiesto l'avvio immediato, si imposta a 1 l'intervallo
			 * in modo che 1-1=0.
			 */
			standbyInterval = 1;
		}
		
		SchedulerFactory schedulerFactory = (StdSchedulerFactory) servletContext
                .getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY);
		Scheduler scheduler;
		try {
			scheduler = schedulerFactory.getScheduler();
			
			JobDataMap jobDataMap = new JobDataMap();
			jobDataMap.put(PDFCManagerJob.INSTANCE_DATE_KEY, instanceDate);
			jobDataMap.put("ds", ds);
			
			JobDetail jobDetail =  newJob(PDFCManagerJob.class)
									.withIdentity("PDFCMGRJOB", "BOSERCONSOLE")
									.usingJobData(jobDataMap)
									.build();
			Trigger trigger = newTrigger()
								.withIdentity("PDFCMGRTRG", "BOSERCONSOLE")
								.withSchedule(simpleSchedule()
											.withIntervalInSeconds(60)
											.withMisfireHandlingInstructionNextWithRemainingCount()
											.repeatForever())
								.startAt(futureDate(standbyInterval-1, IntervalUnit.MINUTE))
								.build();
		
			scheduler.scheduleJob(jobDetail, trigger);
			log.info("job schedulato");
		} catch (SchedulerException e) {
			log.error("scheduler non trovato!!", e);
		}
	}

}

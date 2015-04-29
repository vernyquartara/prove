package it.quartara.boser.listener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.ee.servlet.QuartzInitializerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Effettua la pulizia delle tabelle di Quartz all'avvio,
 * quindi lancia lo scheduler.
 * @author webny
 *
 */
@WebListener
public class JobStoreCleanerListener implements ServletContextListener {
	
	private static final Logger log = LoggerFactory.getLogger(JobStoreCleanerListener.class);
	
	@Resource(name = "jdbc/QuartzDS")
    DataSource ds;
	

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		log.info("cleanup tabelle quartz");
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			log.error("datasource non trovato", e);
			return;
		}
		List<String> tablesNames = new ArrayList<>();
		tablesNames.add("QRTZ_FIRED_TRIGGERS");
		tablesNames.add("QRTZ_PAUSED_TRIGGER_GRPS");
		tablesNames.add("QRTZ_SCHEDULER_STATE");
		tablesNames.add("QRTZ_LOCKS");
		tablesNames.add("QRTZ_SIMPLE_TRIGGERS");
		tablesNames.add("QRTZ_SIMPROP_TRIGGERS");
		tablesNames.add("QRTZ_BLOB_TRIGGERS");
		tablesNames.add("QRTZ_CRON_TRIGGERS");
		tablesNames.add("QRTZ_TRIGGERS");
		tablesNames.add("QRTZ_JOB_DETAILS");
		tablesNames.add("QRTZ_CALENDARS");
		try {
			for (String tableName : tablesNames) {
				Statement statement = conn.createStatement();
				statement.executeUpdate("DELETE FROM "+tableName);
				statement.close();
			}
			conn.close();
		} catch (SQLException e) {
			log.error("problema di esecuzione sql", e);
		}
		log.info("tabelle ripulite, avvio scheduler");
		SchedulerFactory schedulerFactory = (StdSchedulerFactory) sce.getServletContext()
                .getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY);
		Scheduler scheduler;
		try {
			scheduler = schedulerFactory.getScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
			log.error("scheduler non trovato!!", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

}

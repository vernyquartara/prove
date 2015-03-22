package it.quartara.boser.servlet;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import it.quartara.boser.jobs.PdfConversionControllerJob;
import it.quartara.boser.jobs.PdfConversionJob;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.ee.servlet.QuartzInitializerListener;
import org.quartz.impl.StdSchedulerFactory;

@WebServlet("/quartz")
public class QuartzTestServlet extends BoserServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		EntityManagerFactory emf =
		           (EntityManagerFactory)getServletContext().getAttribute("emf");
		try {
			SchedulerFactory factory = (StdSchedulerFactory) req.getServletContext()
	                .getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY);
			
			Map<String, String> map = new HashMap<String, String>();
			map.put("url1", "STARTED");
			map.put("url2", "STARTED");
			
			Scheduler sched = factory.getScheduler(); 
			// define the job and tie it to our HelloJob class
			JobDetail job = newJob(PdfConversionJob.class)
							.withIdentity("myJob", "group1")
							.usingJobData("url", "url1")
							.usingJobData(new JobDataMap(map))
							.build();
			
			// Trigger the job to run now, and then every 40 seconds
			Trigger trigger = newTrigger()
							.withIdentity("myTrigger", "group1")
							.startNow()
							.build();
			
			// Tell quartz to schedule the job using our trigger
			sched.scheduleJob(job, trigger);
			
			/*
			 * 2
			 * 
			 */
			// define the job and tie it to our HelloJob class
			job = newJob(PdfConversionJob.class)
							.withIdentity("myJob2", "group1")
							.usingJobData("url", "url2")
							.usingJobData(new JobDataMap(map))
							.build();
			
			// Trigger the job to run now, and then every 40 seconds
			trigger = newTrigger()
							.withIdentity("myTrigger2", "group1")
							.startNow()
							.build();
			
			// Tell quartz to schedule the job using our trigger
			sched.scheduleJob(job, trigger);
			
			
			/*
			 * 3
			 * 
			 */
			// define the job and tie it to our HelloJob class
			job = newJob(PdfConversionControllerJob.class)
					.withIdentity("myJob3", "group1")
					.usingJobData(new JobDataMap(map))
					.build();
			
			// Trigger the job to run now, and then every 40 seconds
			trigger = newTrigger()
					.withIdentity("myTrigger3", "group1")
					.startNow()
					.withSchedule(simpleSchedule()
							.withIntervalInSeconds(40)
							.repeatForever())
					.build();
			
			// Tell quartz to schedule the job using our trigger
			sched.scheduleJob(job, trigger);
		} catch (SchedulerException e) {
			throw new ServletException(e);
		}
		resp.getOutputStream().println("ok");
	}

	
}

package it.test.web;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class AppContextListener implements ServletContextListener {
	
	private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);
	
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		// create the thread pool
		ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 200, 50000L,
				TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));
		servletContextEvent.getServletContext().setAttribute("executor", executor);
		log.info("ThreadPoolExecutor initialised");
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		ThreadPoolExecutor executor = (ThreadPoolExecutor) servletContextEvent
				.getServletContext().getAttribute("executor");
		executor.shutdown();
	}
}

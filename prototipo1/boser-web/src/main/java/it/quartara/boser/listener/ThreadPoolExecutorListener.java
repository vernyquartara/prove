package it.quartara.boser.listener;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class ThreadPoolExecutorListener implements ServletContextListener {
	
	private static final Logger log = LoggerFactory.getLogger(ThreadPoolExecutorListener.class);
	
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		/*
		 * TODO It may be more convenient to use one of the 
		 * Executors factory methods instead of this general purpose constructor.
		 */
		ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 100, 10L,
				TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100));
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

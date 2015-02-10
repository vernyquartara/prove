package it.test.web;

import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletResponse;

public class Work implements ServletContextListener {
	private static final BlockingQueue queue = new LinkedBlockingQueue();

	private volatile Thread thread;

	public static void add(AsyncContext c) {
		queue.add(c);
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						System.out.println("sleeping for 2 seconds...");
						Thread.sleep(2000);
						AsyncContext context;
						while ((context = (AsyncContext) queue.poll()) != null) {
							System.out.println("sending response for context: "+context);
							try {
								ServletResponse response = context.getResponse();
								response.setContentType("text/plain");
								PrintWriter out = response.getWriter();
								out.printf("Thread %s completed the task", Thread.currentThread().getName());
								out.flush();
							} catch (Exception e) {
								throw new RuntimeException(e.getMessage(), e);
							} finally {
								context.complete();
							}
						}
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		});
		thread.start();
		System.out.println("context initialized");
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		thread.interrupt();
	}
}

package it.test.web;

import java.io.IOException;

import javax.servlet.AsyncContext;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkerThread implements Runnable {
	
	private static final Logger log = LoggerFactory.getLogger(WorkerThread.class);
	
	private AsyncContext asyncContext;

	public WorkerThread(AsyncContext asyncContext) {
		this.asyncContext = asyncContext;
	}

	@Override
	public void run() {
		StringBuilder cmd = new StringBuilder("/home/webny/work/apache-nutch-1.7/bin/nutch");
		cmd.append(" crawl");
		cmd.append(" /home/webny/work/apache-nutch-1.7/urls/seed.txt");
		cmd.append(" -dir /home/webny/work/apache-nutch-1.7/crawl.test");
		cmd.append(" -depth 3");
		cmd.append(" -topN 100");
		cmd.append(" -threads 32");
		CommandLine cmdLine = CommandLine.parse(cmd.toString());
		log.info("avvio di "+cmdLine.getExecutable());
		
		DefaultExecutor executor = new DefaultExecutor();
		executor.setExitValue(1);
		try {
			log.info("avvio del crawl");
			executor.execute(cmdLine);
			log.info("crawl terminato");
		} catch (IOException e) {
			log.error("errore di esecuzione di nutch", e);
		} finally {
			asyncContext.complete();
		}
	}

}

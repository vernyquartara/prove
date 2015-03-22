package it.quartara.boser.servlet;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import it.quartara.boser.jobs.PdfConversionControllerJob;
import it.quartara.boser.jobs.PdfConversionJob;
import it.quartara.boser.model.Crawler;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.PdfConversion;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.ee.servlet.QuartzInitializerListener;
import org.quartz.impl.StdSchedulerFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConversionServlet.class, WorkbookFactory.class})
public class ConversionServletTest {

	/*
	 * testa lo scenario di successo in cui si devono convertire due pdf
	 */
	@Test
	public void testHappyPath() throws Exception {
		Long crawlerId = 3L;
		String repo = "target/test-output/repo";
		Parameter param = new Parameter();
		param.setValue(repo);
		Crawler crawler = new Crawler();
		crawler.setId(crawlerId);
		
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("crawlerId")).thenReturn(crawlerId.toString());
		
		RequestDispatcher rd = mock(RequestDispatcher.class);
		when(request.getRequestDispatcher("/conversionHome")).thenReturn(rd);
		
		HttpServletResponse response = mock(HttpServletResponse.class);
		
		StdSchedulerFactory schedFactory = mock(StdSchedulerFactory.class);
		ServletContext context = mock(ServletContext.class);
		when(request.getServletContext()).thenReturn(context);
		when(context.getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY)).thenReturn(schedFactory);
		when(context.getAttribute("javax.servlet.context.tempdir")).thenReturn(new File("target/test-output"));
		
		ServletConfig config = mock(ServletConfig.class);
		when(config.getServletContext()).thenReturn(context);
		
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = mock(EntityManager.class);
		when (emf.createEntityManager()).thenReturn(em);
		when(em.find(Parameter.class, "SEARCH_REPO")).thenReturn(param);
		when(em.find(Crawler.class, 3L)).thenReturn(crawler);
		when(context.getAttribute("emf")).thenReturn(emf);

		EntityTransaction transaction = mock(EntityTransaction.class);
		when(em.getTransaction()).thenReturn(transaction);
		doNothing().when(transaction).begin();
		doNothing().when(transaction).commit();

		List<FileItem> items = new ArrayList<FileItem>();
		FileItem item = mock(FileItem.class);
		when(item.getFieldName()).thenReturn("file");
		items.add(item);
		FileItem reqParam = mock(FileItem.class);
		when(reqParam.getFieldName()).thenReturn("crawlerId");
		when(reqParam.getString()).thenReturn(crawlerId.toString());
		items.add(reqParam);
		when(item.getName()).thenReturn("C:\\dir\\file.xls");
		
		Workbook wb = WorkbookFactory.create(new File("src/test/resources/land_rover.xls"));
		mockStatic(WorkbookFactory.class);
		when(WorkbookFactory.create(any(File.class))).thenReturn(wb);

		ServletFileUpload upload = mock(ServletFileUpload.class);
		when(upload.parseRequest(request)).thenReturn(items);

		PdfConversion conv = new PdfConversion();
		whenNew(PdfConversion.class).withNoArguments().thenReturn(conv);

		Date startDate = new GregorianCalendar(2015, 4, 12, 15, 11).getTime();
		whenNew(Date.class).withNoArguments().thenReturn(startDate);
		
		Scheduler scheduler = mock(Scheduler.class);
		when(schedFactory.getScheduler()).thenReturn(scheduler);
		
		/*
		 * attenzione questi sono i link nel file xls in test/resources
		 */
		String url1 = "http://www.omniauto.it/magazine/28983/schumacher-marzo-persi-altri-10-kg";
		String url2 = "http://www.omniauto.it/magazine/28323/pedaggi-autostradali-tutti-i-rincari-2015";
		
		JobDetail job1 = mock(JobDetail.class);
		when(job1.getKey()).thenReturn(new JobKey(""));
		JobDetail job2 = mock(JobDetail.class);
		when(job2.getKey()).thenReturn(new JobKey(""));
		JobDetail ctrl = mock(JobDetail.class);
		Trigger trigger1 = mock(Trigger.class);
		Trigger trigger2 = mock(Trigger.class);
		Trigger trigger3 = mock(Trigger.class);
		
		ConversionServlet servlet = spy(new ConversionServlet());
		
		when(servlet.getServletConfig()).thenReturn(config);
		when(servlet.getServletContext()).thenReturn(context);
		doReturn(em).when(servlet).getEntityManager();
		whenNew(ServletFileUpload.class).withAnyArguments().thenReturn(upload);
		String groupId = Integer.valueOf(Math.abs(Integer.valueOf("file.xls".hashCode()))).toString();
		doReturn(job1).when(servlet, "createJob", eq(PdfConversionJob.class), contains(Integer.valueOf(url1.hashCode()).toString()), eq(groupId), any(JobDataMap.class)); //TODO usare un captor??
		doReturn(job2).when(servlet, "createJob", eq(PdfConversionJob.class), contains(Integer.valueOf(url2.hashCode()).toString()), eq(groupId), any(JobDataMap.class));
		doReturn(ctrl).when(servlet, "createJob", eq(PdfConversionControllerJob.class), eq("ctrl"), eq(groupId), any(JobDataMap.class));
		doReturn(trigger1).when(servlet, "createTrigger", contains(Integer.valueOf(url1.hashCode()).toString()), eq(groupId));
		doReturn(trigger2).when(servlet, "createTrigger", contains(Integer.valueOf(url2.hashCode()).toString()), eq(groupId));
		doReturn(trigger3).when(servlet, "createControllerTrigger", eq("ctrlTrg"), eq(groupId));
		
		servlet.doPost(request, response);
		
		verify(scheduler).scheduleJob(job1, trigger1);
		verify(scheduler).scheduleJob(job2, trigger2);
		verify(scheduler).scheduleJob(ctrl, trigger3);
		verify(item).write((File) any(File.class));
		verify(em).persist(conv);
		verify(em).close();
		verify(response).sendRedirect("/conversionHome");
	}
	
}

package it.quartara.boser.servlet;

import it.quartara.boser.model.PdfConversion;

import java.io.File;
import java.io.IOException;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/delConvs")
public class ConversionDeleteServlet extends BoserServlet {

	private static final long serialVersionUID = -2977049294995216216L;
	
	private static final Logger log = LoggerFactory.getLogger(ConversionDeleteServlet.class);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String[] ids = request.getParameterValues("convId");
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		for (String idString : ids) {
			long id = Long.valueOf(idString);
			log.info("cancellazione conversione ID={}", id);
			PdfConversion conversion = em.find(PdfConversion.class, id);
			if (conversion.getZipFilePath()!=null) {
				File zipFile = new File(conversion.getZipFilePath());
				log.debug("cancellazione zip file: {}, esito: {}", zipFile.getAbsolutePath(), zipFile.delete());
			}
			File workDir = new File(conversion.getDestDir());
			if (workDir.exists()) {
				File xlsFile = new File(workDir.getParent()+File.separator+conversion.getXlsFileName());
				if (xlsFile.exists()) {
					log.debug("cancellazione file xls: {}, esito: {}", xlsFile.getAbsolutePath(), xlsFile.delete());
				}
				log.debug("cancellazione work directory: {}", workDir.getAbsolutePath());
				for (File file : workDir.listFiles()) {
					log.debug("cancellazione file: {}, esito: {}", file.getName(), file.delete() );
				}
				log.debug("cancellazione directory: {}", workDir.delete());
			}
			em.remove(conversion);
		}
		em.getTransaction().commit();
		em.close();
		response.sendRedirect("/conversionHome");
	}

	/*
	 * richiamato nel caso in cui questa servlet venga invocata
	 * a partire da un RequestDispatcher.
	 * (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

}

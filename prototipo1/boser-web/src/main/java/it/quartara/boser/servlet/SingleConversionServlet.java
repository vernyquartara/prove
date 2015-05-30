package it.quartara.boser.servlet;

import it.quartara.boser.model.Parameter;
import it.quartara.boser.service.PdfConversionFactory;
import it.quartara.boser.service.PdfConversionService;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.persistence.EntityManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Permette di convertire un singolo articolo a partire da un URL.
 * @author webny
 *
 */
@WebServlet(loadOnStartup=-1,urlPatterns={"/urlToPdf"})
public class SingleConversionServlet extends BoserServlet {

	private static final long serialVersionUID = -4620438421704863715L;
	
	private static final Logger log = LoggerFactory.getLogger(SingleConversionServlet.class);

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String urlString = request.getParameter("url");
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			String msg = "ATTENZIONE Il link inserito non è un URL valido";
			log.error(msg, e);
			request.setAttribute("errorMsg", msg);
			RequestDispatcher rd = request.getRequestDispatcher("/singleConversion.jsp");
			rd.forward(request, response);
			return;
		}
		
		String crawlerId = request.getParameter("crawlerId");
		log.debug("crawler id: {}", crawlerId);
		EntityManager em = getEntityManager();
		Parameter param = em.find(Parameter.class, "SEARCH_REPO");
		String repo = param.getValue();
		log.debug("SEARCH_REPO: {}", repo);
		if (param.getValue().startsWith("$")) {
			repo = System.getenv(param.getValue().substring(1));
			log.debug("converting variable {} into {}",  param.getValue().substring(1), repo);
		}
		String destDirPath = repo+"/"+crawlerId+"/pdfs/temp";
		log.debug("destDirPath: {}", destDirPath);
		File destDir = new File(destDirPath);
		destDir.mkdirs();
		destDir.deleteOnExit();
		
		PdfConversionService service = PdfConversionFactory.create();
		File pdfFile = service.convertToPdf(destDirPath, urlString, url.getHost());
		handleDownload(response, pdfFile, pdfFile.getName());
		response.getOutputStream().close();
		
		em.close();
	}
	
	

}

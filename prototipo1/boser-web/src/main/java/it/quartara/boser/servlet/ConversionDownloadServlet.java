package it.quartara.boser.servlet;

import it.quartara.boser.model.PdfConversion;

import java.io.File;
import java.io.IOException;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Effettua il download di una conversione in PDF.
 * @author webny
 *
 */
@WebServlet("/conversionDownload")
public class ConversionDownloadServlet extends BoserServlet {

	private static final long serialVersionUID = 1360752692724438178L;
	
	private static final Logger log = LoggerFactory.getLogger(ConversionDownloadServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String conversionId = request.getParameter("conversionId");
		log.debug("ricerca conversionId per id {}", conversionId);
		
		EntityManager em = getEntityManager();
		PdfConversion conversion = em.find(PdfConversion.class, Long.valueOf(conversionId));
		String filePath = conversion.getFilePath();
		log.debug("path: {}", filePath);
		File file = new File(filePath);
		String attachmentName = filePath.substring(filePath.lastIndexOf("/")+1);
		log.debug("attachmentName: {}", attachmentName);
		
		/*
		 * download
		 */
	    ServletOutputStream out = response.getOutputStream();
	    handleDownload(response, file, attachmentName);
        out.close();
        em.close();
	}

}

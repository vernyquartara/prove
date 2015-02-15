package it.quartara.boser.servlet;

import it.quartara.boser.model.SearchResult;

import java.io.File;
import java.io.FileInputStream;
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
 * Effettua il download di una ricerca.
 * @author webny
 *
 */
@WebServlet("/searchDownload")
public class SearchDownloadServlet extends BoserServlet {

	/** */
	private static final long serialVersionUID = 1037242602501373936L;
	
	private static final Logger log = LoggerFactory.getLogger(SearchDownloadServlet.class);
	
	/*
	 * 1) ricava l'id della ricerca dalla request
	 * 2) carica la ricerca da db
	 * 3) scrive lo zip sullo stream di output della response
	 * 
	 * (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String searchId = request.getParameter("searchResultId");
		log.debug("ricerca searchResult per id {}", searchId);
		
		EntityManager em = getEntityManager();
		SearchResult searchResult = em.find(SearchResult.class, Long.valueOf(searchId));
		String zipFilePath = searchResult.getZipFilePath();
		File zipFile = new File(zipFilePath);
		String attachmentName = zipFilePath.substring(zipFilePath.lastIndexOf("/")+1);
		
		/*
		 * download
		 */
		response.setContentType("application/octet-stream");
	    response.setHeader("Content-Disposition","attachment;filename="+attachmentName);
	    response.setContentLengthLong(zipFile.length());
	    ServletOutputStream out = response.getOutputStream();
	    FileInputStream input = new FileInputStream(zipFile);
	    byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = input.read(buffer)) != -1) {
        	out.write(buffer, 0, bytesRead);
        }
        input.close();
        out.flush();
        out.close();
        em.close();
	}

	
}

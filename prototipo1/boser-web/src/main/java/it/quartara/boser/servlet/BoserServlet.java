package it.quartara.boser.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BoserServlet extends HttpServlet {

	/** */
	private static final long serialVersionUID = -1859454805296449374L;
	
	private static final Logger log = LoggerFactory.getLogger(BoserServlet.class);

	protected EntityManager getEntityManager() {
		EntityManagerFactory emf =
		           (EntityManagerFactory)getServletContext().getAttribute("emf");
		EntityManager em = emf.createEntityManager();
		return em;
	}
	
	/**
	 * ATTENZIONE effettua flush() di response.getOutputStream() <strong>ma non close()</strong>
	 * @param response
	 * @param file
	 * @param attachmentName
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected void handleDownload(HttpServletResponse response, File file, String attachmentName)
			throws FileNotFoundException, IOException {
		log.debug("setting headers");
		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition","attachment;filename="+attachmentName);
		ServletOutputStream out = response.getOutputStream();
		log.debug("opening inputstream, buffer is 4096");
		FileInputStream input = new FileInputStream(file);
		try {
			log.debug("getting file lenght");
			long length = file.length();
			log.debug("file lenght is {}", length);
			response.setContentLengthLong(length);
			byte[] buffer = new byte[4096];
			int bytesRead = -1;
			while ((bytesRead = input.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
			log.debug("zip written, closing stream");
		} catch (Exception e) {
			log.error("errore di download", e);
			throw e;
		} finally {
			input.close();
			out.flush();
		}
	}
}

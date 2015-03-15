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

public abstract class BoserServlet extends HttpServlet {

	/** */
	private static final long serialVersionUID = -1859454805296449374L;

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
		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition","attachment;filename="+attachmentName);
		response.setContentLengthLong(file.length());
		ServletOutputStream out = response.getOutputStream();
	    FileInputStream input = new FileInputStream(file);
	    byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = input.read(buffer)) != -1) {
        	out.write(buffer, 0, bytesRead);
        }
        input.close();
        out.flush();
	}
}

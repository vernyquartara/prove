package it.test.web;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class EntityManagerListener implements ServletContextListener {

	// Prepare the EntityManagerFactory & Enhance:
	@Override
    public void contextInitialized(ServletContextEvent e) {
        EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("BoserPU");
        e.getServletContext().setAttribute("emf", emf);
    }
 
    // Release the EntityManagerFactory:
	@Override
    public void contextDestroyed(ServletContextEvent e) {
        EntityManagerFactory emf =
            (EntityManagerFactory)e.getServletContext().getAttribute("emf");
        emf.close();
    }

}

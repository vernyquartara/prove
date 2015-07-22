package it.quartara.boser.console.pdfcmgr;

import javax.annotation.Resource;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;

/**
 * Se l'istanza è attiva al momento dell'avvio del contesto,
 * il job di standby deve essere schedulato.
 * @author webny
 *
 */
@WebListener
public class PDFCManagerContextListener implements ServletContextListener {
	
	@Resource(name="jdbc/BoserDS")
	private DataSource ds;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		AmazonEC2 ec2 = AWSHelper.createAmazonEC2Client(AWSHelper.CREDENTIALS_PROFILE);
		Instance instance = AWSHelper.getInstance(ec2, AWSHelper.INSTANCE_ID);
		if (instance.getState().getName().equalsIgnoreCase(InstanceStateName.Running.toString())) {
    		PDFCManagerHelper.scheduleStandbyJob(ds, sce.getServletContext(), instance.getLaunchTime(), true);
    	}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

}

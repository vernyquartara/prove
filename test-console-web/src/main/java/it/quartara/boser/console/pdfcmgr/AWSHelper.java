package it.quartara.boser.console.pdfcmgr;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;

public class AWSHelper {
	
	public static final String INSTANCE_ID = "i-108733d1";
	public static final String CREDENTIALS_PROFILE = "boser-console";
	
	public static AmazonEC2 createAmazonEC2Client(String profileName) {
		AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider(profileName).getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (/home/webny/.aws/credentials), and is in valid format.",
                    e);
        }
        AmazonEC2 ec2 = new AmazonEC2Client(credentials);
        ec2.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
        return ec2;
	}
	

	public static Instance getInstance(AmazonEC2 ec2, String instanceId) {
		DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
		List<String> instanceIds = new ArrayList<>();
		instanceIds.add(instanceId);
		describeInstancesRequest.setInstanceIds(instanceIds);
		DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);

		Instance instance = describeInstancesResult.getReservations().get(0).getInstances().get(0);
		return instance;
	}

}

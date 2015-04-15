package it.quartara.boser.model;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="PDF_CONVERTIONS")
public class PdfConversion extends PersistentEntity {

	private short countCompleted;
	private short countFailed;
	private long fileSize;
	private String filePath;
	@Enumerated(EnumType.STRING)
	private ExecutionState state;
	private Date startDate;
	private short countTotal;
	@OneToOne(targetEntity=AsyncRequest.class)
	private AsyncRequest asyncRequest;
	
	public int getCountWorking() {
		int count = 0;
		if (asyncRequest == null) {
			return count;
		}
		Map<String, String> rp = asyncRequest.getParameters();
		for (Entry<String, String> entry : rp.entrySet()) {
			if (entry.getKey().endsWith("state")) {
				ExecutionState state = ExecutionState.valueOf(entry.getValue());
				switch (state) {
				case STARTED:
					count++;
					break;
				default:
					break;
				}
			}
		}
		return count;
	}
	
	public Date getLastUpdate() {
		if (asyncRequest==null) {
			return null;
		}
		return asyncRequest.getLastUpdate();
	}
	
	public long getFileSize() {
		return fileSize;
	}
	public void setFileSize(long size) {
		this.fileSize = size;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public ExecutionState getState() {
		return state;
	}
	public void setState(ExecutionState state) {
		this.state = state;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public String getLabel() {
		if (filePath != null) {
			int start = filePath.lastIndexOf("/")+1; //FIXME se usato File.separator in inserimento, potrebbe non essere /
			int end = filePath.lastIndexOf(".");
			return filePath.substring(start, end);
		}
		return null;
	}
	public short getCountCompleted() {
		return countCompleted;
	}
	public void setCountCompleted(short countCompleted) {
		this.countCompleted = countCompleted;
	}
	public short getCountFailed() {
		return countFailed;
	}
	public void setCountFailed(short countFailed) {
		this.countFailed = countFailed;
	}
	public short getCountTotal() {
		return countTotal;
	}
	public void setCountTotal(short countTotal) {
		this.countTotal = countTotal;
	}
	public AsyncRequest getAsyncRequest() {
		return asyncRequest;
	}
	public void setAsyncRequest(AsyncRequest asyncRequest) {
		this.asyncRequest = asyncRequest;
	}
}

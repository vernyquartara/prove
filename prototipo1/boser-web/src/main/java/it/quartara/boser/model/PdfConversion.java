package it.quartara.boser.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Entity
@Table(name="PDF_CONVERTIONS")
public class PdfConversion extends PersistentEntity {

	private short numberOfLinks;
	private long size;
	private String filePath;
	@Enumerated(EnumType.STRING)
	private ExecutionState state;
	private Date startDate;
	
	public short getNumberOfLinks() {
		return numberOfLinks;
	}
	public void setNumberOfLinks(short numberOfLinks) {
		this.numberOfLinks = numberOfLinks;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
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
}

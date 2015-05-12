package it.quartara.boser.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name="PDF_CONVERTIONS_ITEMS")
public class PdfConversionItem extends PersistentEntity {

	private String url;
	@Enumerated(EnumType.STRING)
	private ExecutionState state;
	private Date startDate;
	private Date endDate;
	private String stackTrace;
	private String pdfFileNamePrefix;
	@ManyToOne
	private PdfConversion conversion;
	
	@Version
	private long version;
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
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
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public String getStackTrace() {
		return stackTrace;
	}
	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}
	public String getPdfFileNamePrefix() {
		return pdfFileNamePrefix;
	}
	public void setPdfFileNamePrefix(String pdfFileNamePrefix) {
		this.pdfFileNamePrefix = pdfFileNamePrefix;
	}
	public PdfConversion getConversion() {
		return conversion;
	}
	public void setConversion(PdfConversion conversion) {
		this.conversion = conversion;
	}
	public long getVersion() {
		return version;
	}
	public void setVersion(long version) {
		this.version = version;
	}
}

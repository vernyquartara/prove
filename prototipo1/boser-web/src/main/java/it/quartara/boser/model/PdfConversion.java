package it.quartara.boser.model;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name="PDF_CONVERTIONS")
public class PdfConversion extends PersistentEntity {

	private short countCompleted;
	private short countFailed;
	private long fileSize;
	private String zipFilePath;
	@Enumerated(EnumType.STRING)
	private ExecutionState state;
	private Date creationDate;
	private Date startDate;
	private Date endDate;
	private Date lastUpdate;
	@OneToMany(mappedBy="conversion", cascade=CascadeType.ALL)
	private List<PdfConversionItem> items;
	private String xlsFileName;
	private float scaleFactor;
	private String destDir;
	
	@Version
	private long version;
	
	public int getCountWorking() {
		int count = 0;
		if (items == null) {
			return count;
		}
		for (PdfConversionItem item : items) {
			if (item.getState()==ExecutionState.STARTED) {
				count++;
			}
		}
		return count;
	}
	public int getCountRemaining() {
		int count = 0;
		if (items == null) {
			return count;
		}
		for (PdfConversionItem item : items) {
			if (item.getState()==ExecutionState.STARTED
					|| item.getState()==ExecutionState.READY) {
				count++;
			}
		}
		return count;
	}
	public int getCountReady() {
		int count = 0;
		if (items == null) {
			return count;
		}
		for (PdfConversionItem item : items) {
			if (item.getState()==ExecutionState.READY) {
				count++;
			}
		}
		return count;
	}
	
	public long getFileSize() {
		return fileSize;
	}
	public void setFileSize(long size) {
		this.fileSize = size;
	}
	public String getZipFilePath() {
		return zipFilePath;
	}
	public void setZipFilePath(String filePath) {
		this.zipFilePath = filePath;
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
		if (zipFilePath != null) {
			int start = zipFilePath.lastIndexOf("/")+1; //FIXME se usato File.separator in inserimento, potrebbe non essere /
			int end = zipFilePath.lastIndexOf(".");
			return zipFilePath.substring(start, end);
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
		if (items != null) {
			return (short) items.size();
		}
		return 0;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public List<PdfConversionItem> getItems() {
		return items;
	}

	public void setItems(List<PdfConversionItem> items) {
		this.items = items;
	}

	/**
	 * restituisce il nome del file xls originale, senza percorso.
	 * @return
	 */
	public String getXlsFileName() {
		return xlsFileName;
	}

	public void setXlsFileName(String xlsFileName) {
		this.xlsFileName = xlsFileName;
	}

	public float getScaleFactor() {
		return scaleFactor;
	}

	public void setScaleFactor(float scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * Restituisce il percorso della directory di destinazione della conversione.
	 * @return
	 */
	public String getDestDir() {
		return destDir;
	}

	public void setDestDir(String destDir) {
		this.destDir = destDir;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}
}

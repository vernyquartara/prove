package it.quartara.boser.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="SEARCH")
public class Search extends PersistentEntity {

	private String zipFilePath;
	private Date timestamp;
	@ManyToOne @JoinColumn(name = "config_id", nullable = false)
	private SearchConfig config;
	@ManyToOne @JoinColumn(name = "index_id", nullable = false)
	private Index index;
	
	public String getZipFilePath() {
		return zipFilePath;
	}
	public void setZipFilePath(String zipFilePath) {
		this.zipFilePath = zipFilePath;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public SearchConfig getConfig() {
		return config;
	}
	public void setConfig(SearchConfig config) {
		this.config = config;
	}
	public Index getIndex() {
		return index;
	}
	public void setIndex(Index index) {
		this.index = index;
	}
	public String getZipLabel() {
		if (zipFilePath != null) {
			int start = zipFilePath.lastIndexOf("/")+1; //FIXME se usato File.separator in inserimento, potrebbe non essere /
			int end = zipFilePath.lastIndexOf(".");
			return zipFilePath.substring(start, end);
		}
		return null;
	}
}

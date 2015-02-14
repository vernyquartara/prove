package it.test.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="SEARCH_RESULTS",  
	   uniqueConstraints=@UniqueConstraint(columnNames={"link","key_id"}))
public class SearchResult extends PersistentEntity {
	
	private Date timestamp;
	@ManyToOne
	private SearchConfig config;
	private String link;
	@ManyToOne
	private SearchKey key;
	
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
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public SearchKey getKey() {
		return key;
	}
	public void setKey(SearchKey key) {
		this.key = key;
	}
}

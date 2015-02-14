package it.test.model;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="SITES")
public class Site extends PersistentEntity {

	private String url;
	private String regexUrlFilter;
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getRegexUrlFilter() {
		return regexUrlFilter;
	}
	public void setRegexUrlFilter(String regexUrlFilter) {
		this.regexUrlFilter = regexUrlFilter;
	}
}

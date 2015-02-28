package it.quartara.boser.model;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class SearchResultPK implements Serializable {
	
	/** */
	private static final long serialVersionUID = -1691492284034411L;
	
	private String url;
	private Long key;
	private Long search;
	
	public Long getKey() {
		return key;
	}
	public void setKey(Long key) {
		this.key = key;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public Long getSearch() {
		return search;
	}
	public void setSearch(Long search) {
		this.search = search;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((search == null) ? 0 : search.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SearchResultPK other = (SearchResultPK) obj;
		if (key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!key.equals(other.key)) {
			return false;
		}
		if (search == null) {
			if (other.search != null) {
				return false;
			}
		} else if (!search.equals(other.search)) {
			return false;
		}
		if (url == null) {
			if (other.url != null) {
				return false;
			}
		} else if (!url.equals(other.url)) {
			return false;
		}
		return true;
	}
	
}

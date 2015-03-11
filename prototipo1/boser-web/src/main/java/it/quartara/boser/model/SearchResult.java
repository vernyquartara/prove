package it.quartara.boser.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="SEARCH_RESULTS")
@IdClass(SearchResultPK.class)
public class SearchResult {
	
	@Id
	private String url;
	@Id
	@ManyToOne
	private SearchKey key;
	@Id
	@ManyToOne
	private Search search;
	
	private String title;
	
	public SearchKey getKey() {
		return key;
	}
	public void setKey(SearchKey key) {
		this.key = key;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public Search getSearch() {
		return search;
	}
	public void setSearch(Search search) {
		this.search = search;
	}
	
}

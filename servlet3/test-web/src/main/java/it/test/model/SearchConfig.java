package it.test.model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="SEARCH_CONFIGS")
public class SearchConfig extends PersistentEntity {

	@ManyToOne
	private Crawler crawler;
	@OneToMany
	private Set<SearchAction> actions;
	@OneToMany
	private Set<SearchKey> keys;

	public Crawler getCrawler() {
		return crawler;
	}

	public void setCrawler(Crawler crawler) {
		this.crawler = crawler;
	}

	public Set<SearchAction> getActions() {
		return actions;
	}

	public void setActions(Set<SearchAction> actions) {
		this.actions = actions;
	}

	public Set<SearchKey> getKeys() {
		return keys;
	}

	public void setKeys(Set<SearchKey> keys) {
		this.keys = keys;
	}

}

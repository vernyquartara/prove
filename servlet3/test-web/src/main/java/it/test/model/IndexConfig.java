package it.test.model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="INDEX_CONFIGS")
public class IndexConfig extends PersistentEntity {

	@ManyToOne
	private Crawler crawler;
	private short depth;
	private short topN;
	@OneToMany
	private Set<Site> sites;
	
	public short getDepth() {
		return depth;
	}
	public void setDepth(short depth) {
		this.depth = depth;
	}
	public short getTopN() {
		return topN;
	}
	public void setTopN(short topN) {
		this.topN = topN;
	}
	public Set<Site> getSites() {
		return sites;
	}
	public void setSites(Set<Site> sites) {
		this.sites = sites;
	}
	public Crawler getCrawler() {
		return crawler;
	}
	public void setCrawler(Crawler crawler) {
		this.crawler = crawler;
	}
}

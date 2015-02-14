package it.test.model;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="SEARCH_KEYS")
public class SearchKey extends PersistentEntity {

	private String text;
	@OneToOne
	private SearchKey parent;
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public SearchKey getParent() {
		return parent;
	}
	public void setParent(SearchKey parent) {
		this.parent = parent;
	}
}

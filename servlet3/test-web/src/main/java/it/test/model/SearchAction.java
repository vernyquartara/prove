package it.test.model;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="SEARCH_ACTIONS")
public class SearchAction extends PersistentEntity {

	private String impl;

	public String getImpl() {
		return impl;
	}

	public void setImpl(String impl) {
		this.impl = impl;
	}
	
}

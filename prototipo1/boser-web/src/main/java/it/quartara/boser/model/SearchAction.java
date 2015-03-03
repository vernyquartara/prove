package it.quartara.boser.model;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="SEARCH_ACTIONS")
public class SearchAction extends PersistentEntity {

	private String description;
	private String handlerClass;
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getHandlerClass() {
		return handlerClass;
	}
	public void setHandlerClass(String handlerClass) {
		this.handlerClass = handlerClass;
	}
	
}

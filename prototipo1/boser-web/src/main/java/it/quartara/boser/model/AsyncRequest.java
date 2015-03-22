package it.quartara.boser.model;

import java.util.Date;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name="ASYNC_REQUESTS")
public class AsyncRequest extends PersistentEntity {
	
	@Enumerated(EnumType.STRING)
	private ExecutionState state;
	private Date creationDate;
	private Date lastUpdate;
	
	@ElementCollection
	@MapKeyColumn(name="param_name")
	@Column(name="param_value")
	@CollectionTable(name="ASYNC_REQUESTS_PARAMETERS", joinColumns=@JoinColumn(name="request_id"))
	private Map<String, String> parameters;
	
	@Version
	private long version;
	
	public ExecutionState getState() {
		return state;
	}
	public void setState(ExecutionState state) {
		this.state = state;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public Date getLastUpdate() {
		return lastUpdate;
	}
	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	public Map<String, String> getParameters() {
		return parameters;
	}
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	public long getVersion() {
		return version;
	}
	

}

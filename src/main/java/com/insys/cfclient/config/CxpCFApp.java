package com.insys.cfclient.config;

import java.io.Serializable;

public class CxpCFApp implements Serializable, JsonMessage{
	
private static final long serialVersionUID = -7227223977435098259L;
	
	private String id;
	private String name;
	private Integer instanceCount;
	
	public CxpCFApp() {
		
	}
	
	public CxpCFApp(String id, String name, Integer instanceCount) {
		this.id=id;
		this.name=name;
		this.instanceCount=instanceCount;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Integer getInstanceCount() {
		return instanceCount;
	}
	
	public void setInstanceCount(Integer instanceCount) {
		this.instanceCount = instanceCount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CxpCFApp other = (CxpCFApp) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CFApplication [id=" + id + ", name=" + name + ", instanceCount=" + instanceCount + "]";
	}

}

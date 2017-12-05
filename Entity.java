package com.maxoflondon.ossutils.multipathxcheck;

import java.util.Map;
import java.util.LinkedHashMap;


public class Entity {
	private String entity;
	private  String instId;
	private String status;
	private String type;
	private String data1;
	private String data2;
	
	public Entity(String entity, String instId, String status, String type, String data1, String data2) {
		this.entity = entity;
		this.instId = instId;
		this.status = status;
		this.type = type;
		this.data1 = data1;
		this.data2 = data2;
	}
	
	public Entity(Entity other) {
		this.entity = other.getEntity();
		this.instId = other.getInstId();
		this.status = other.getStatus();
		this.type = other.getType();
		this.data1 = other.getData1();
		this.data2 = other.getData2();
	}
	
	public Entity(Map<String, String> hm) {
		for(Map.Entry<String, String> ent : hm.entrySet()) {
			String key = ent.getKey();
			String value = ent.getValue();
			if (key.equals("ENTITY")) {
				this.entity = value;
			}else if (key.equals("INST")) {
				this.instId = value;
			} else if (key.equals("STATUS")) {
				this.status = value;
			} else if (key.equals("TYPE")) {
				this.type = value;
			} else if (key.equals("DATA1")) {
				this.data1 = value;
			} else if (key.equals("DATA2")) {
				this.data2 = value;
			}
		}
	}
	
	public String getEntity() {
		return entity;
	}
	
	public String getInstId() {
		return instId; 
	}
	
	public String getStatus() {
		return status;
	}
	
	public String getType() {
		return type;
	}
	
	public String getData1() {
		return data1;
	}
	
	public String getData2() {
		return data2;
	}
	
	public String[] toArray() {
		return new String[] {
			entity,
			instId,
			status,
			type,
			data1,
			data2
		};
	}
}
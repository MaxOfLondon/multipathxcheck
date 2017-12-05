package com.maxoflondon.ossutils.multipathxcheck;

import java.util.ArrayList;
import java.util.Map;

public class EntityCounter extends Entity {
	private int count = 0;
	private ArrayList<String> parentId = new ArrayList<String>();
	
	public EntityCounter(Entity entity, String parentId) {
		super(entity);
		this.add(parentId);
	}
	
	public EntityCounter(String parentId, Map<String, String> map) {
		super(map);
		this.add(parentId);
	}
	
	public void add(String parentId) {
		this.parentId.add(parentId);
		this.count++;
	}
	
	public int getCount() {
		return count;
	}
	
	public ArrayList<String> getParentList() {
		return parentId;
	}
}
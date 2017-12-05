package com.maxoflondon.ossutils.multipathxcheck;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class MultiPathXCheck {
	private MpxcDAO dao = null;
	private Map<String, ArrayList<LinkedHashMap<String, String>>> entityList; 
	
	public MultiPathXCheck() {
		dao = new MpxcDAO();
		entityList = new LinkedHashMap<String, ArrayList<LinkedHashMap<String, String>>>();
	}
	
	
	/** Returns circ_path_inst_id from XNG cicruit reference
	* uses varargs
	*/
	public ArrayList<Triplet<String, String, String>> getCircPathIdFromCircRef(String... refs) {
		if (refs ==null)
			return null;
		
		ArrayList<Triplet<String, String, String>> result = new ArrayList<Triplet<String, String, String>>();
		for (int i=0; i<refs.length; i+=2) {
			String instId = dao.getCircPathIdFromCircRef(refs[i], refs[i+1]);
			Triplet t = new Triplet(refs[i], refs[i+1], instId);
			result.add(t);
		}
		
		return result;
	}
	
	/** Adds multiple circuits and populates cache with CIs related to them
	* @return: list of Triplets (circ_path_inst_id, [Success, Fail], error message if fail)
	*/
	public ArrayList<Triplet<String, String, String>> addPathsToCheck(ArrayList<String> listOfCircPathInstId) {
		ArrayList<Triplet<String, String, String>> result = new ArrayList<Triplet<String, String, String>>();
		
		for (String inst : listOfCircPathInstId) {
				result.add(addPathToCheck(inst));
		}
		
		return result;
	}
	
	/** Adds circuit and populates cache with all CIs related to that circuit.
	* if error was encountered the circuit is not added to the cache
	* @return: Triplet circ_path_inst_id, [Success, Fail], error message if fail)
	*/
	public Triplet<String, String, String> addPathToCheck(String circPathInstId) {
		Triplet<String, String, String> result;
		ArrayList<LinkedHashMap<String, String>> en = dao.fetchEntityList(circPathInstId);
		if (en != null && en.size() > 0) {
			// add to the entityList
			entityList.put(circPathInstId, en);
			result = new Triplet<String, String, String>(circPathInstId, "Ok", "");
		} else {
			result = new Triplet<String, String, String>(circPathInstId, "Fail", "Service not found");
		}
		return result;
	}
	
	public ArrayList<Entity> getPathComponents(String circPathInstId) {
		if (entityList.containsKey(circPathInstId)) {
			ArrayList<Entity> result = new ArrayList<Entity>();
			ArrayList<LinkedHashMap<String, String>> root = entityList.get(circPathInstId);
			
			for (LinkedHashMap<String, String> row : root) {
				Entity e = new Entity(row);
				result.add(new Entity(e.getEntity(), e.getInstId().substring(1), e.getStatus(), e.getType(), e.getData1(), e.getData2()));
			}
			return result;
		}
		return null;
	}
	
	public ArrayList<Entity> getIntersect() {

		// iterate over all paths all components and create reference-counting index HashMap
		Map<String, EntityCounter> counter = new HashMap<String, EntityCounter>();

		for (Map.Entry<String, ArrayList<LinkedHashMap<String, String>>> root : entityList.entrySet()) {
			for (LinkedHashMap<String, String> row : root.getValue()) {
				if (!counter.containsKey(row.get("INST"))) {
					counter.put(row.get("INST"), new EntityCounter(root.getKey(), row));
				} else {
					counter.get(row.get("INST")).add(root.getKey());
				}
			}
		}
		
		// Add Entities that have count equal to all paths intersected
		ArrayList<Entity> result = new ArrayList<Entity>();
		for (Map.Entry<String, EntityCounter> cnt : counter.entrySet()) {
			EntityCounter e = cnt.getValue();
			if (e.getCount() == entityList.size()) {
				result.add(new Entity(e.getEntity(), e.getInstId().substring(1), e.getStatus(), e.getType(), e.getData1(), e.getData2()));
			}
		}
		
		return result;
	}

}
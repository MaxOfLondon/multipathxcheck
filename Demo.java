package com.maxoflondon.ossutils.multipathxcheck;

import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Arrays;


public class Demo {
	/*
	* 
	* If two or more paths are given it will display only shared components by all the paths
	*  example command line: java -jar multipathxcheck.jar circuit-id-1 Live circuit-id-2 Live circuit-id-3 Live
	*/
	public static void main(String [] args) {
		// if argument list is not divisable by 2 display error and exit
		if ((args.length & 1) != 0) {
			System.out.println("ERROR: Argument list must be of format: circuit1 status1 circuit2 status2 ...");
			System.exit(1);
		}

		MultiPathXCheck mpc = new MultiPathXCheck();
		
		String savedInstIdForLater = null;
		for (int i=0; i<args.length; i+=2) {
			// resolve circuit_id and status to circ_path_inst_id and adds them one by one but it is also possible to resolve and add them in array
			ArrayList<Triplet<String, String, String>> ref = mpc.getCircPathIdFromCircRef(new String[] {args[i], args[i+1]});
			// add path to checker
			Triplet<String, String, String> resp = mpc.addPathToCheck(ref.get(0).getThird());
			savedInstIdForLater = ref.get(0).getThird();
			System.out.println("Added: "+ args[i] + " " + args[i+1] + "  -> InstId:" + resp.getFirst() + " Result:"+resp.getSecond() + " Error:"+resp.getThird());
		}
		System.out.println();
		
		System.out.println("*** Displaying all common entities ***");
		// call getInterset to get list of common entities
		ArrayList<Entity> intersect = mpc.getIntersect();
		
		// this is for display only - no logic here
		TableBuilder tb = new TableBuilder();
		String[] headers = { "Entity", "Inst", "Status", "Type", "Data1", "Data2" };
		tb.addRow(headers);
		String[] unders = Arrays.copyOf(headers, headers.length);
		for (int i=0; i<unders.length; i++) {
			unders[i] = unders[i].replaceAll(".","-");
		}
		tb.addRow(unders);
		
		for (Entity e : intersect) {
			tb.addRow(e.toArray());
		}
		
		System.out.println(tb.toString());
		System.out.println();
		
	System.out.println(savedInstIdForLater);	
		// To get all entities for path that was added already call getPathComponents
		// the other sneaky way would be to add only one path to MultiPathXCheck and call getIntersect
		System.out.println("*** Displaying all components for one of already added paths ***");
		ArrayList<Entity> allComponents = mpc.getPathComponents(savedInstIdForLater);
		// pretty print
		tb = new TableBuilder();
		tb.addRow(headers);
		tb.addRow(unders);
		
		for (Entity e : allComponents) {
			tb.addRow(e.toArray());
		}
		System.out.println(tb.toString());
	}
	
}
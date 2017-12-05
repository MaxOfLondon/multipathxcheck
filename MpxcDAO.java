package com.maxoflondon.ossutils.multipathxcheck;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.ArrayList;
import java.util.LinkedHashMap;


public class MpxcDAO /*extends DAO */ {
	
	private static java.sql.Connection conn = null; // database connection

	public MpxcDAO() { }
	
	public static String getCircPathIdFromCircRef(String xngCircRef, String status) {
		Statement stmt = null;
		
		String sql = "select circ_path_inst_id from ie_circ_path_inst where circ_path_hum_id='" + xngCircRef + "' and status='" + status + "'";
		String result = null;
		
		if (!connect()) return null;
		
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			
			if (rs.next()) {		
				result = rs.getString("CIRC_PATH_INST_ID");
			}
			
			if (rs != null) { rs.close(); }
			if (stmt != null) { stmt.close(); }
		} catch ( SQLException ex ) {
			ex.printStackTrace();
		}
		return result;
	}
	
	public ArrayList<LinkedHashMap<String, String>> fetchEntityList(String circPathInstId) {
		Statement stmt = null;
		ArrayList<LinkedHashMap<String, String>> list = new ArrayList<LinkedHashMap<String, String>>();
		
		String sql = "with tmp2 (parent_path_inst_id, elem_type, circ_path_hum_id, port_inst_id, path_inst_id, segm_inst_id, child_path_inst_id, cable_inst_id) as (\n" +
		"	select \n" +
		"		e.circ_path_inst_id parent_path_inst_id,\n" +
		"		e.element_type elem_type,\n" +
		"		c.circ_path_hum_id circ_path_hum_id,\n" +
		"		e.port_inst_id port_inst_id,\n" +
		"		e.path_inst_id path_inst_id,\n" +
		"		e.segment_inst_id segm_inst_id,\n" +
		"		e.path_inst_id child_path_inst_id,\n" +
		"		e.cable_inst_id cable_inst_id\n" +
		"	from\n" +
		"		ie_circ_path_element e,\n" +
		"		ie_circ_path_inst c\n" +
		"	where\n" +
		"		c.circ_path_inst_id=e.circ_path_inst_id\n" +
		"		and c.status=c.status\n" +
		"	START WITH\n" +
		"		e.circ_path_inst_id = " + circPathInstId + "\n" +
		"	CONNECT BY NOCYCLE\n" +
		"		 e.circ_path_inst_id = PRIOR e.path_inst_id\n" +
		"	/*ORDER SIBLINGS BY e.circ_path_inst_id*/\n" +
		"),\n" +
		"tmp (port_inst_id, path_inst_id, segm_inst_id, cable_inst_id) as (\n" +
		"select distinct\n" +
		"	port_inst_id,\n" +
		"	path_inst_id,\n" +
		"	segm_inst_id,\n" +
		"	cable_inst_id\n" +
		"from\n" +
		"	tmp2)\n" +
		"-- add paths\n" +
		"select\n" +
		"	'path' entity,\n" +
		"	'A'||tmp.path_inst_id inst,\n" +
		"	cpi.status status,\n" +
		"	cpi.type,\n" +
		"	cpi.circ_path_hum_id data1,\n" +
		"	cpi.customer_id data2\n" +
		"from\n" +
		"	tmp,\n" +
		"	ie_circ_path_inst cpi\n" +
		"where\n" +
		"	cpi.circ_path_inst_id=tmp.path_inst_id\n" +
		"-- add sites for equipment		\n" +
		"union\n" +
		"select\n" +
		"	'site' entity,\n" +
		"	'B'||ei.site_inst_id inst,\n" +
		"	si.status status,\n" +
		"    si.num,\n" +
		"    si.base_num data1,\n" +
		"    si.site_hum_id data2\n" +
		"from\n" +
		"	tmp,\n" +
		"	ie_epa epa,\n" +
		"	ie_equip_inst ei,\n" +
		"	ie_site_inst si\n" +
		"where\n" +
		"	epa.port_inst_id=tmp.port_inst_id\n" +
		"	and ei.equip_inst_id=epa.equip_inst_id\n" +
		"	and si.site_inst_id=ei.site_inst_id\n" +
		"-- add sites for segments - a-site\n" +
		"union\n" +
		"select\n" +
		"	'site' entity,\n" +
		"	'B'||si.site_inst_id inst,\n" +
		"	si.status status,\n" +
		"    si.num,\n" +
		"    si.base_num data1,\n" +
		"    si.site_hum_id data2\n" +
		"from\n" +
		"	tmp,\n" +
		"	ie_circ_inst seg,\n" +
		"	ie_site_inst si\n" +
		"where\n" +
		"	seg.circ_inst_id=tmp.segm_inst_id\n" +
		"	and si.site_inst_id=seg.a_site_id\n" +
		"-- add sites for segments - z-site\n" +
		"union\n" +
		"select\n" +
		"	'site' entity,\n" +
		"	'B'||si.site_inst_id inst,\n" +
		"	si.status status,\n" +
		"    si.num,\n" +
		"    si.base_num data1,\n" +
		"    si.site_hum_id data2\n" +
		"from\n" +
		"	tmp,\n" +
		"	ie_circ_inst seg,\n" +
		"	ie_site_inst si\n" +
		"where\n" +
		"	seg.circ_inst_id=tmp.segm_inst_id\n" +
		"	and si.site_inst_id=seg.z_site_id\n" +
		"-- add devices\n" +
		"-- TODO: check if SHELF is in use in XNG as it seems empty in cache\n" +
		"union\n" +
		"select\n" +
		"	'device' entity,\n" +
		"	'C'||ei.equip_inst_id inst,\n" +
		"	ei.status status,\n" +
		"	ei.type type,\n" +
		"	ei.descr data1,\n" +
		"	ei.vendor||' '||ei.model||' '||ei.shelf data2\n" +
		"from\n" +
		"	tmp,\n" +
		"	ie_epa epa,\n" +
		"	ie_equip_inst ei\n" +
		"where\n" +
		"	epa.port_inst_id=tmp.port_inst_id\n" +
		"	and ei.equip_inst_id=epa.equip_inst_id\n" +
		"/*\n" +
		"-- add parent device (container)\n" +
		"-- TODO: import equip_inst.parent_eq_inst_id then uncomment\n" +
		"union\n" +
		"select\n" +
		"	'container' entity,\n" +
		"	'D'||p.equip_inst_id inst,\n" +
		"	p.status status,\n" +
		"	p.type type,\n" +
		"	p.descr data1,\n" +
		"	p.vendor||' '||ei.model||' '||ei.shelf data2\n" +
		"from\n" +
		"	tmp,\n" +
		"	ie_epa epa,\n" +
		"	ie_equip_inst ei,\n" +
		"	ie_equip_inst p\n" +
		"where\n" +
		"	epa.port_inst_id=tmp.port_inst_id\n" +
		"	and ei.equip_inst_id=epa.equip_inst_id\n" +
		"	and p.equp_inst_id=ie.parent_eq_inst_id\n" +
		"*/\n" +
		"-- add segments\n" +
		"union\n" +
		"select\n" +
		"	'segment' entity,\n" +
		"	'E'||tmp.segm_inst_id inst,\n" +
		"	seg.status status,\n" +
		"	seg.type type,\n" +
		"	seg.circ_hum_id data1,\n" +
		"	seg.vendor data2\n" +
		"from\n" +
		"	tmp,\n" +
		"	ie_circ_inst seg\n" +
		"where\n" +
		"	seg.circ_inst_id=tmp.segm_inst_id\n" +
		"/*\n" +
		"-- add slots\n" +
		"-- TODO: import slot_inst table\n" +
		"union\n" +
		"select\n" +
		"	'slot' entity,\n" +
		"	'F'||si.slot_inst_id inst,\n" +
		"	'Ok' status,\n" +
		"	'SLOT' slot,\n" +
		"	si.slot data1,\n" +
		"	si.description data2\n" +
		"from\n" +
		"	tmp,\n" +
		"	ie_epa epa,\n" +
		"	ie_slot_inst si\n" +
		"where\n" +
		"	epa.port_inst_id=tmp.port_inst_id\n" +
		"	and si.equip_inst_id=epa.equip_inst_id\n" +
		"*/\n" +
		"-- add cards\n" +
		"-- TODO: check ci.descr missing values in many cases, is there a better label?\n" +
		"-- TODO: check ci.slot missing values\n" +
		"-- TODO: add PARENT_CARD_INST_ID and handle it\n" +
		"-- TODO: check ci.SLOT_INST_ID missing\n" +
		"-- TODO: add \n" +
		"union\n" +
		"select\n" +
		"	'card' entity,\n" +
		"	'G'||epa.card_inst_id inst,\n" +
		"	ci.status status,\n" +
		"	ci.type type,\n" +
		"	ci.descr data1,\n" +
		"	ei.descr||' '||ci.slot data2\n" +
		"from\n" +
		"	tmp,\n" +
		"	ie_epa epa,\n" +
		"	ie_equip_inst ei,\n" +
		"	ie_card_inst ci\n" +
		"where\n" +
		"	epa.circ_path_inst_id=tmp.path_inst_id\n" +
		"	and ci.card_inst_id=epa.card_inst_id\n" +
		"	and ei.equip_inst_id=epa.equip_inst_id\n" +
		"-- add ports for paths\n" +
		"-- TODO the ie_epa.CIRC_INST_ID is missing, needs to be added to cache\n" +
		"union\n" +
		"select\n" +
		"	'port' entity,\n" +
		"	'H'||epa.port_inst_id inst,\n" +
		"	epa.status status,\n" +
		"	epa.connector_type type,\n" +
		"	epa.port_hum_id data1,\n" +
		"	ei.descr||' '||ci.slot||' '||ci.descr data2\n" +
		"from\n" +
		"	tmp,\n" +
		"	ie_epa epa,\n" +
		"	ie_equip_inst ei,\n" +
		"	ie_card_inst ci\n" +
		"where\n" +
		"	epa.circ_path_inst_id=tmp.path_inst_id\n" +
		"	and ci.card_inst_id=epa.card_inst_id\n" +
		"	and ei.equip_inst_id=epa.equip_inst_id\n" +
		"/*\n" +
		"-- add ports for segments - a-side\n" +
		"-- TODO: add ie_circ_inst.a_port and z_port to cache then uncomment this\n" +
		"union\n" +
		"select\n" +
		"	'port' entity,\n" +
		"	'H'||epa.port_inst_id inst,\n" +
		"	epa.status status,\n" +
		"	epa.connector_type type,\n" +
		"	epa.port_hum_id data1,\n" +
		"	ei.descr||' '||ci.slot||' '||ci.descr data2\n" +
		"from\n" +
		"	tmp,\n" +
		"	ie_circ_inst seg,\n" +
		"	ie_epa epa,\n" +
		"	ie_equip_inst ei,\n" +
		"	ie_card_inst ci\n" +
		"where\n" +
		"	seg.circ_inst_id=tmp.segm_inst_id\n" +
		"	and epa.port_inst_id=seg.a_port\n" +
		"	and ci.card_inst_id=epa.card_inst_id\n" +
		"	and ei.equip_inst_id=epa.equip_inst_id\n" +
		"-- add ports for segments - z-side\n" +
		"union\n" +
		"select\n" +
		"	'port' entity,\n" +
		"	'H'||epa.port_inst_id inst,\n" +
		"	epa.status status,\n" +
		"	epa.connector_type type,\n" +
		"	epa.port_hum_id data1,\n" +
		"	ei.descr||' '||ci.slot||' '||ci.descr data2\n" +
		"from\n" +
		"	tmp,\n" +
		"	ie_circ_inst seg,\n" +
		"	ie_epa epa,\n" +
		"	ie_equip_inst ei,\n" +
		"	ie_card_inst ci\n" +
		"where\n" +
		"	seg.circ_inst_id=tmp.segm_inst_id\n" +
		"	and epa.port_inst_id=seg.z_port\n" +
		"	and ci.card_inst_id=epa.card_inst_id\n" +
		"	and ei.equip_inst_id=epa.equip_inst_id\n" +
		"*/\n";
		
		if (!connect()) return null;
		
		try {
			
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			
			ResultSetMetaData rsMetaData = rs.getMetaData();
			String[] columns = {"entity","inst","status","type","data1","data2"};
			
			while (rs.next()) {
				LinkedHashMap<String, String> row = new LinkedHashMap<String, String>(columns.length);
				
				for (int i=0; i < columns.length; i++){
					row.put(columns[i].toUpperCase(), rs.getString(columns[i].toUpperCase()));
				}
				list.add(row);
			}
			if (rs != null) { rs.close(); }
			if (stmt != null) { stmt.close(); }
		} catch ( SQLException ex ) {
			ex.printStackTrace();
		}

		return list;
	}

	

	public static boolean connect() {
		
		if (conn != null) return true;
		
		try {
			DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
		} catch(SQLException ex) {
			System.out.println("Error: unable to load driver class!");
			ex.printStackTrace();
			System.exit(1);
		}

		String URL = "jdbc:oracle:thin:@xxxx:xxx/xxx_APP"; // rfs
		Properties info = new Properties() {{ put( "user", "xxxxxxxx" ); put( "password", "xxxxxxxx"); }};
		
		
		try {
			conn = DriverManager.getConnection(URL, info);
		} catch(SQLException ex) {
			System.out.println("Error: unable to get connection!");
			ex.printStackTrace();
		}
		return conn != null;
	}

	public static void closeConnection() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException ex) {
				System.out.println("Error: unable to close connection!");
				ex.printStackTrace();
			}
		}
		conn = null;
	}
	
}
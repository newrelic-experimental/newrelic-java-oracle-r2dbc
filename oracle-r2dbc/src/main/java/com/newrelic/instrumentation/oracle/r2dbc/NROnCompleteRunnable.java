package com.newrelic.instrumentation.oracle.r2dbc;

import java.util.HashMap;
import java.util.Map;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.DatastoreParameters.DatabaseParameter;
import com.newrelic.api.agent.DatastoreParameters.InstanceParameter;
import com.newrelic.api.agent.DatastoreParameters.SlowQueryParameter;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.QueryConverter;
import com.newrelic.api.agent.Segment;

public class NROnCompleteRunnable implements Runnable {
	
	private Segment segment = null;
	private String operation = null;
	private String collection = null;
	private String sql = null;
	Map<String, Object> dbAttributes = null;
	
	public NROnCompleteRunnable(String s,String op, String coll, Map<String, Object> attributes) {
		segment = NewRelic.getAgent().getTransaction().startSegment("OracleR2DBC");
		sql = s;
		operation = op;
		collection = coll;
		dbAttributes = attributes;
	}

	@Override
	public void run() {
		if(segment != null) {
			InstanceParameter instanceParams = DatastoreParameters.product(OracleDatabaseVendor.INSTANCE.getName()).collection(collection).operation(operation);
			
			String host = (String) dbAttributes.get(Utils.DATABASE_HOST);
			Integer port = (Integer) dbAttributes.get(Utils.DATABASE_PORT);
			HashMap<String, Object> attributes = new HashMap<String, Object>();
			
			Utils.addAttribute(attributes,"DatabaseHost",host);
			Utils.addAttribute(attributes,"DatabasePort",port);
			Utils.addAttribute(attributes,"SQL",sql);

			DatabaseParameter db_params;
			if(host != null) {
				db_params = instanceParams.instance(host, port);
			} else {
				db_params = instanceParams.noInstance();
			}
			
			String dbName = (String) dbAttributes.get(Utils.DATABASE_NAME);
			Utils.addAttribute(attributes,"DatabaseName",dbName);
			SlowQueryParameter slowQuery;
			if(dbName != null) {
				slowQuery = db_params.databaseName(dbName);
			} else {
				slowQuery = db_params.noDatabaseName();
			}

			QueryConverter<String> converter = Utils.getQueryConverter();			
			Utils.addAttribute(attributes,"QueryConverter",converter);
 			DatastoreParameters params = slowQuery.slowQuery(sql, converter).build();
 			
			segment.reportAsExternal(params);
			segment.addCustomAttributes(attributes);
			segment.end();
			segment = null;
		}
	}

}

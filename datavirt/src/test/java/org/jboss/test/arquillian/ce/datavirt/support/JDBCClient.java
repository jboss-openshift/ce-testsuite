/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.jboss.test.arquillian.ce.datavirt.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.teiid.jdbc.TeiidDataSource;
import org.teiid.jdbc.TeiidStatement;

@SuppressWarnings("nls")
public class JDBCClient {
	
	public String username = "teiidUser";
	public String password = "Password1-";
	
	public JDBCClient() {
		
	}
	
	public JDBCClient(String username, String password) {
		this.username=username;
		this.password=password;
	}	
	
	public String call(String host, String port, String protocol, String vdb, String sql) throws Exception {
		StringBuffer buffer = new StringBuffer();
		
		boolean isSelect = execute(getDriverConnection(host, port, protocol, vdb), sql, buffer);

        // only if its a Select will the SQL be performed again using TeiidDataSource
		if (isSelect) {
			System.out.println("-----------------------------------");
			System.out.println("Executing using the TeiidDataSource");
			// this is showing how to make a Data Source connection. 
			execute(getDataSourceConnection(host, port, protocol, vdb), sql, buffer);
		}
		
		return buffer.toString();
	}
	
	protected Connection getDriverConnection(String host, String port, String protocol, String vdb) throws Exception {
		String url = "jdbc:teiid:"+vdb+"@" + protocol + "://"+host+":"+port+";showplan=on"; //note showplan setting
		Class.forName("org.teiid.jdbc.TeiidDriver");
		
		return DriverManager.getConnection(url,username, password);		
	}
	
	protected Connection getDataSourceConnection(String host, String port, String protocol, String vdb) throws Exception {
		TeiidDataSource ds = new TeiidDataSource();
		ds.setDatabaseName(vdb);
		ds.setUser(username);
		ds.setPassword(password);
		ds.setServerName(host);
		ds.setPortNumber(Integer.valueOf(port));
		
		ds.setShowPlan("on"); //turn show plan on
		
		if (protocol.equals("mms"))
			ds.setSecure(true);
		
		return ds.getConnection();
	}
	
	public boolean execute(Connection connection, String sql, StringBuffer buffer) throws Exception {
		    
        boolean hasRs = true;
		try {
			Statement statement = connection.createStatement();
			
			hasRs = statement.execute(sql);
			
			if (!hasRs) {
				int cnt = statement.getUpdateCount();
				
				buffer.append("----------------\n");
				buffer.append("Updated #rows: " + cnt + "\n");
				buffer.append("----------------\n");
			} else {
				ResultSet results = statement.getResultSet();
				ResultSetMetaData metadata = results.getMetaData();
				int columns = metadata.getColumnCount();
				buffer.append("Results\n");
				for (int row = 1; results.next(); row++) {
					buffer.append(row + ": ");
					for (int i = 0; i < columns; i++) {
						if (i > 0) {
							buffer.append(",");
						}
						buffer.append(results.getString(i+1));
					}
					buffer.append("\n");
				}
				results.close();
			}
			buffer.append("Query Plan\n");
			buffer.append(statement.unwrap(TeiidStatement.class).getPlanDescription());
			
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
		return hasRs;
	}

}

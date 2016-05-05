/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.test.arquillian.ce.sso;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.test.arquillian.ce.sso.support.Client;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

public class SsoAllInOneTestBase extends SsoEapTestBase {
			
    public SsoAllInOneTestBase() {
    }

    @RouteURL("helloworld")
    private URL routeURL;

    @RouteURL("secure-helloworld")
    private URL secureRouteURL;

    @Override
    protected URL getRouteURL() {
        return routeURL;
    }

    @Override
    protected URL getSecureRouteURL() {
        return secureRouteURL;
    }

    @Test
    @RunAsClient
    public void testLogin() throws Exception {
        testLogin(getRouteURL().toString());
    }

    @Test
    @RunAsClient
    public void testSecureLogin() throws Exception {
        testLogin(getSecureRouteURL().toString());
    }

    protected void testLogin(String host) throws Exception {
        Client client = new Client(host);

        String result = client.get("app-profile-jee/profile.jsp");
        assertTrue(result.contains("kc-form-login"));
    }

    @Test
    @RunAsClient
    public void testOidcLogin() throws Exception {
        Client client = login(getRouteURL().toString(), "app-profile-jee/profile.jsp");
        
        String result = client.get();
        
        assertTrue(result.contains("First name"));
        assertTrue(result.contains("Last name"));
        assertTrue(result.contains("Username"));
        assertTrue(result.contains("demouser"));
    }

    @Test
    @RunAsClient
    public void testSecureOidcLogin() throws Exception {
    	Client client = login(getSecureRouteURL().toString(), "app-profile-jee/profile.jsp");
    	
    	String result = client.get();
    	
    	assertTrue(result.contains("First name"));
        assertTrue(result.contains("Last name"));
        assertTrue(result.contains("Username"));
        assertTrue(result.contains("demouser"));
    }

    protected Client login(String host, String key) throws Exception {
    	Client client = new Client(host);
        String result = client.get(key);
        
        assertTrue(result.contains("kc-form-login"));
        
        int index = result.indexOf("action");
        String action=result.substring(index + "action=\"".length());
        index = action.indexOf("\"");
        action=action.substring(0,index);
         
        client.setBasicUrl(action);
        
        Map<String, String> params = new HashMap<>();
        params.put("username", "demouser");
        params.put("password", "demopass");
        params.put("login", "submit");
        client.setParams(params);
        
        result = client.post();
        
        assertTrue(result.contains(Client.trimPort(host)));
        
        client.setBasicUrl(result);
        params = new HashMap<>();
        client.setParams(params);
        
        return client;
    }

    @Test
    @RunAsClient
    public void testAccessType() throws Exception {
        String host = "sso" + System.getProperty("openshift.domain");

        Map<String, String> params = new HashMap<>();
        params.put("username", "admin");
        params.put("password", "admin");
        params.put("grant_type", "password");
        params.put("client_id", "admin-cli");

        Client client = new Client("http://" + host + "/auth");
        client.setParams(params);
        String result = client.post("realms/master/protocol/openid-connect/token");

        assertFalse(result.contains("error_description"));
        assertTrue(result.contains("access_token"));

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(result);
        String accessToken = (String) jsonObject.get("access_token");

        params = new HashMap<>();
        params.put("Accept", "application/json");
        params.put("Authorization", "Bearer " + accessToken);

        result = client.get("admin/realms/demo/clients", params);
        Iterator clients = ((JSONArray) jsonParser.parse(result)).iterator();
        while (clients.hasNext()) {
            jsonObject = (JSONObject) clients.next();
            if ((jsonObject.get("clientId")).equals("app-jee")) {
                assertEquals(jsonObject.get("publicClient"), Boolean.FALSE);
                assertEquals(jsonObject.get("bearerOnly"), Boolean.FALSE);
                return;
            }
        }

        fail("ClientId app-jee not found");
    }
    
    @Test
    @RunAsClient
    public void testSecureAppJeeButtonsNoLogin() throws Exception {
    	Client client = new Client(getSecureRouteURL().toString());
        String result = client.get("app-jee/index.jsp");
         
        assertTrue(result.contains("Public"));
        assertTrue(result.contains("Secured"));
        assertTrue(result.contains("Admin"));
        
        Map<String, String> params = new HashMap<>();
        params.put("action", "public");
        client.setParams(params);
        
        result = client.post("app-jee/index.jsp");
      
        assertTrue(result.contains("MESSAGE: PUBLIC"));
        
        params.put("action", "secured");
        client.setParams(params);
        
        result = client.post("app-jee/index.jsp");
        
        assertTrue(result.contains("500 Internal Server Error"));
    }
    
    @Test
    @RunAsClient
    public void testSecureAppJeeButtonsLogin() throws Exception {
    	Client client = login(getSecureRouteURL().toString(), "app-jee/protected.jsp");
       
        String result = client.get();
        
        assertTrue(result.contains("Public"));
        assertTrue(result.contains("Secured"));
        assertTrue(result.contains("Admin"));
        assertTrue(result.contains("Logout"));
        assertTrue(result.contains("Account"));
        
        Map<String, String> params = new HashMap<>();
        params.put("action", "public");
        client.setParams(params);
        client.setBasicUrl(getSecureRouteURL().toString());
        
        result = client.post("app-jee/index.jsp");
        
        assertTrue(result.contains("MESSAGE: PUBLIC"));
        
        params.put("action", "secured");
        client.setParams(params);
        
        result = client.post("app-jee/index.jsp");
        
        assertTrue(result.contains("MESSAGE: SECURED"));
        
        printCookieStore(client);
    }
    
    private void printCookieStore(Client client) throws Exception {
    	List<Cookie> cookies = client.getCookieStore().getCookies();
    	System.out.println("Cookies " + cookies.size());
    	for (Cookie cookie : cookies){
    		System.out.println("      " + cookie);
    	}
    	
    }

}

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

import static junit.framework.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.ce.api.ConfigurationHandle;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.test.arquillian.ce.sso.support.Client;
import org.junit.Test;

public abstract class SsoBaseTest {
    public static final String HTTP = "http";
    public static final String HTTPS = "https";

    @ArquillianResource
    ConfigurationHandle configuration;
    
    protected String route;
    protected String secureRoute;
    
    @Test
    @RunAsClient
    public void testConsoleRoute() throws Exception {
        String host = route + System.getProperty("openshift.domain");
        
        consoleRoute(HTTP, host);
    }

    @Test
    @RunAsClient
    public void testSecureConsoleRoute() throws Exception { 	
    	String host = secureRoute + System.getProperty("openshift.domain");
    	
    	consoleRoute(HTTPS, host);
    }
        
    protected void consoleRoute(String protocol, String host) {
        Client client = new Client(protocol + "://" + host + "/auth");
        String result = client.get("admin/master/console/#/realms/master");
        assertTrue(result.contains("realm.js"));
    }
    
    @Test
    @RunAsClient
    public void testRestRoute() throws Exception {
    	String host = route + System.getProperty("openshift.domain");
    	restRoute(HTTP, host);
    }
    
    @Test
    @RunAsClient
    public void testSecureRestRoute() throws Exception {
    	String host = secureRoute + System.getProperty("openshift.domain");
    	
    	restRoute(HTTPS, host);
    }
               
    protected void restRoute(String protocol, String host) {
        List<NameValuePair> params = new ArrayList<NameValuePair>(4);
        params.add(new BasicNameValuePair("username", "admin"));
        params.add(new BasicNameValuePair("password", "admin"));
        params.add(new BasicNameValuePair("grant_type", "password"));
        params.add(new BasicNameValuePair("client_id", "admin-cli"));
        
        Client client = new Client(protocol + "://" + host + "/auth");
        String result = client.post("realms/master/protocol/openid-connect/token", params);
        
        assertFalse(result.contains("error_description"));
        assertTrue(result.contains("access_token"));
    }
    
    @Test
    @RunAsClient
    public void testLogin() throws Exception {
		login(HTTP, route + System.getProperty("openshift.domain"));
	}
	
	@Test
    @RunAsClient
    public void testSecureOidcLogin() throws Exception {
		login(HTTPS, secureRoute + System.getProperty("openshift.domain"));
	}
        
    protected void login(String protocol, String host) throws Exception {
        Client client = new Client(protocol + "://" + host);
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("username", "admin"));
        params.add(new BasicNameValuePair("password", "admin"));
        params.add(new BasicNameValuePair("login", "submit"));
        
        String result = client.post("auth",params);
        System.out.println("!!!!!! result " + result);
        assertTrue(result.contains("Welcome to Red Hat Single Sign-On"));
    }

}

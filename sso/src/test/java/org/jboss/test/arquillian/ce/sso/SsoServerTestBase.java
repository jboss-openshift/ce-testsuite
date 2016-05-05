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

import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.test.arquillian.ce.sso.support.Client;
import org.junit.Test;

public abstract class SsoServerTestBase extends SsoTestBase {

    @Test
    @RunAsClient
    public void testConsoleRoute() throws Exception {
        consoleRoute(getRouteURL().toString());
    }

    @Test
    @RunAsClient
    public void testSecureConsoleRoute() throws Exception { 	 	
    	consoleRoute(getSecureRouteURL().toString());
    }
        
    protected void consoleRoute(String host) throws Exception {
        Client client = new Client(host);
        String result = client.get("auth/admin/master/console/#/realms/master");
        assertTrue(result.contains("realm.js"));
    }
    
    @Test
    @RunAsClient
    public void testRestRoute() throws Exception {
    	restRoute(getRouteURL().toString());
    }
    
    @Test
    @RunAsClient
    public void testSecureRestRoute() throws Exception {
    	restRoute(getSecureRouteURL().toString());
    }
               
    protected void restRoute(String host) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("username", "admin");
        params.put("password", "admin");
        params.put("grant_type", "password");
        params.put("client_id", "admin-cli");
        
        Client client = new Client(host);
        client.setParams(params);
        String result = client.post("auth/realms/master/protocol/openid-connect/token");
        
        assertFalse(result.contains("error_description"));
        assertTrue(result.contains("access_token"));
    }
    
    @Test
    @RunAsClient
    public void testLogin() throws Exception {
		login(getRouteURL().toString());
	}
	
	@Test
    @RunAsClient
    public void testSecureOidcLogin() throws Exception {
		login(getSecureRouteURL().toString());
	}
        
    protected void login(String host) throws Exception {
        Client client = new Client(host);

        Map<String, String> params = new HashMap<>();
        params.put("username", "admin");
        params.put("password", "admin");
        params.put("login", "submit");
        client.setParams(params);
        
        String result = client.post("auth");
        assertTrue(result.contains("Welcome to Red Hat Single Sign-On"));
    }

}

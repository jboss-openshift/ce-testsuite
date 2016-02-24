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

import static junit.framework.Assert.assertTrue;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.test.arquillian.ce.sso.support.Client;
import org.junit.Test;

public abstract class SsoEapTestBase {

    public static final String HTTP = "http";
    public static final String HTTPS = "https";

    protected String route;
    protected String secureRoute;
    
    @Test
    @RunAsClient
    public void testAppRoute() throws Exception {
        String host = route + System.getProperty("openshift.domain");
        
        appRoute(HTTP, host);
    }

    @Test
    @RunAsClient
    public void testSecureAppRoute() throws Exception { 	
    	String host = secureRoute + System.getProperty("openshift.domain");
    	
    	appRoute(HTTPS, host);
    }
        
    protected void appRoute(String protocol, String host) {
        Client client = new Client(protocol + "://" + host );
        String result = client.get("app-profile-jee");
        System.out.println("!!!!!! result " + result);
        assertTrue(result.contains("profile.jsp"));
    }

}

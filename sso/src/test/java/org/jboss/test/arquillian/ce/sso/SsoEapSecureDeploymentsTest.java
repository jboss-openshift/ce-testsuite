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
import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.Map;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.httpclient.HttpClient;
import org.jboss.arquillian.ce.httpclient.HttpClientBuilder;
import org.jboss.arquillian.ce.httpclient.HttpClientExecuteOptions;
import org.jboss.arquillian.ce.httpclient.HttpRequest;
import org.jboss.arquillian.ce.httpclient.HttpResponse;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;

public abstract class SsoEapSecureDeploymentsTest extends SsoTestBase
{
	@ArquillianResource
	OpenShiftHandle adapter;
	
	private final HttpClientExecuteOptions execOptions = new HttpClientExecuteOptions.Builder().tries(3)
            .desiredStatusCode(200).delay(10).build();
    
    @Test
    @RunAsClient
    public void testAppProfileJeeRoute() throws Exception {
        appRoute(getRouteURL().toString(), "app-profile-jee", "profile.jsp", "Please login");
    }

    @Test
    @RunAsClient
    public void testSecureAppProfileJeeRoute() throws Exception { 	
    	appRoute(getSecureRouteURL().toString(), "app-profile-jee", "profile.jsp", "Please login");
    }
    
    @Test
    @RunAsClient
    public void testAppProfileJeeSamlRoute() throws Exception {
        appRoute(getRouteURL().toString(), "app-profile-jee-saml", "profile.jsp", "Please login");
    }

    @Test
    @RunAsClient
    public void testSecureAppProfileJeeSamlRoute() throws Exception { 	
    	appRoute(getSecureRouteURL().toString(), "app-profile-jee-saml", "profile.jsp", "Please login");
    }
        
    protected void appRoute(String host, String app, String... expecteds) throws Exception {
    	HttpClient client = HttpClientBuilder.untrustedConnectionClient();
        HttpRequest request = HttpClientBuilder.doGET(host + app);
        HttpResponse response = client.execute(request, execOptions);
        
        String result = response.getResponseBodyAsString();
        
        System.out.println("!!!!! result " + host + app + " " + result);
        
        for (String expected: expecteds)
        	assertTrue(result.contains(expected));
    }
    
    @Test
    @RunAsClient
    public void testLogs() throws Exception {
        Map<String, String> labels = Collections.singletonMap("application", "eap-app");
        String result = adapter.getLog(null, labels);
        
        assertFalse(result.contains("Failure"));
        assertTrue(result.contains("Deployed \"app-profile-jee.war\""));
        assertTrue(result.contains("Deployed \"app-profile-jee-saml.war\""));
    }

}

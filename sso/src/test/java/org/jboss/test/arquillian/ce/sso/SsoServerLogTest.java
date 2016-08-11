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

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.test.arquillian.ce.sso.support.Client;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template( url = "https://raw.githubusercontent.com/bdecoste/application-templates/adminUser/sso/sso70-basic.json",
		labels = "application=sso")
@OpenShiftResources({
        @OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/sso-app-secret.json"),
        @OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/eap-app-secret.json")
})
public class SsoServerLogTest extends SsoTestBase
{
	
	@RouteURL("sso")
    private URL routeURL;
	
	@RouteURL("secure-sso")
    private URL secureRouteURL;
	
	@ArquillianResource
	OpenShiftHandle adapter;
	
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
    public void testLogs() throws Exception {
        Map<String, String> labels = Collections.singletonMap("application", "sso");
        adapter.exec(labels, 10, "curl", "-s", "https://raw.githubusercontent.com/bdecoste/log-access/master/logaccess-jaxrs/logaccess-jaxrs.war", "-o", "/opt/eap/standalone/deployments/logaccess-jaxrs.war");

        Client client = new Client(getRouteURL().toString());
        String result = client.get("logging/podlog");
    
        assertTrue(result.contains("Deployed \"keycloak-server.war\""));
        assertTrue(result.contains("Deployed \"logaccess-jaxrs.war\""));
	}
	
	

}

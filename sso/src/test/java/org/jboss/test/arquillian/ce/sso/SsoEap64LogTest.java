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

import java.net.URL;

import org.jboss.arquillian.ce.api.ExternalDeployment;
import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.test.arquillian.ce.sso.support.Client;
import org.junit.Test;
import org.junit.runner.RunWith;

// Tests disabled due to WLFY-2634 not backported to EAP6.4
//@RunWith(Arquillian.class)
//@Template(url = "https://raw.githubusercontent.com/bdecoste/application-templates/adminUser/eap/eap64-sso-s2i.json",
//		labels = "application=eap-app",
//		parameters = {
//			@TemplateParameter(name = "SSO_PUBLIC_KEY", value="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiLezsNQtZSaJvNZXTmjhlpIJnnwgGL5R1vkPLdt7odMgDzLHQ1h4DlfJPuPI4aI8uo8VkSGYQXWaOGUh3YJXtdO1vcym1SuP8ep6YnDy9vbUibA/o8RW6Wnj3Y4tqShIfuWf3MEsiH+KizoIJm6Av7DTGZSGFQnZWxBEZ2WUyFt297aLWuVM0k9vHMWSraXQo78XuU3pxrYzkI+A4QpeShg8xE7mNrs8g3uTmc53KR45+wW1icclzdix/JcT6YaSgLEVrIR9WkkYfEGj3vSrOzYA46pQe6WQoenLKtIDFmFDPjhcPoi989px9f+1HCIYP0txBS/hnJZaPdn5/lEUKQIDAQAB")
//    	})
//@OpenShiftResources({
//        @OpenShiftResource("classpath:sso-service-account.json"),
//        @OpenShiftResource("classpath:sso-app-secret.json"),
//        @OpenShiftResource("classpath:eap-app-secret.json")
//})
public class SsoEap64LogTest //extends SsoTestBase
{
	
	@RouteURL("eap-app")
    private URL routeURL;
	
	@RouteURL("secure-eap-app")
    private URL secureRouteURL;
	
	@ArquillianResource
	OpenShiftHandle adapter;
	
//	@Override
    protected URL getRouteURL() {
        return routeURL;
    }
	
//	@Override
    protected URL getSecureRouteURL() {
        return secureRouteURL;
    }
	
//	@Test
//    @RunAsClient
    public void testLogs() throws Exception {
		adapter.exec("application", "eap-app", 10, "curl", "-s", "https://raw.githubusercontent.com/bdecoste/log-access/master/logaccess-jaxrs/logaccess-jaxrs.war", "-o", "/opt/eap/standalone/deployments/logaccess-jaxrs.war");
		
		Client client = new Client(getRouteURL().toString());
        String result = client.get("logging/podlog");
        
        System.out.println("!!!! result " + result);
        
        assertFalse(result.contains("Failure"));
        assertTrue(result.contains("Deployed \"app-profile-jee-saml.war\""));
        assertTrue(result.contains("Deployed \"app-profile-jee.war\""));
        assertTrue(result.contains("Deployed \"app-jee.war\""));
        assertTrue(result.contains("Deployed \"service-jaxrs.war\""));
    }

}

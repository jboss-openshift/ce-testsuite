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

import org.jboss.arquillian.ce.api.ExternalDeployment;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.test.arquillian.ce.sso.support.Client;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/bdecoste/application-templates/adminUser/eap/eap64-sso-s2i.json",
		labels = "application=eap-app",
		parameters = {
				@TemplateParameter(name = "SOURCE_REPOSITORY_URL", value="https://github.com/bdecoste/keycloak-examples"),
				@TemplateParameter(name = "SOURCE_REPOSITORY_REF", value="securedeployments"),
                @TemplateParameter(name = "ARTIFACT_DIR", value="app-profile-jee/target,app-profile-jee-saml/target"),
                @TemplateParameter(name = "SSO_PUBLIC_KEY", value="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiLezsNQtZSaJvNZXTmjhlpIJnnwgGL5R1vkPLdt7odMgDzLHQ1h4DlfJPuPI4aI8uo8VkSGYQXWaOGUh3YJXtdO1vcym1SuP8ep6YnDy9vbUibA/o8RW6Wnj3Y4tqShIfuWf3MEsiH+KizoIJm6Av7DTGZSGFQnZWxBEZ2WUyFt297aLWuVM0k9vHMWSraXQo78XuU3pxrYzkI+A4QpeShg8xE7mNrs8g3uTmc53KR45+wW1icclzdix/JcT6YaSgLEVrIR9WkkYfEGj3vSrOzYA46pQe6WQoenLKtIDFmFDPjhcPoi989px9f+1HCIYP0txBS/hnJZaPdn5/lEUKQIDAQAB")
                })
@OpenShiftResources({
        @OpenShiftResource("classpath:eap-app-secret.json")
})
public class SsoEap64SecureDeploymentsTest extends SsoEapTestBase
{
	@RouteURL("eap-app")
    private URL routeURL;
	
	@RouteURL("secure-eap-app")
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
    public void testSamlAppRoute() throws Exception {
        appRoute(getRouteURL().toString());
    }

    @Test
    @RunAsClient
    public void testSecureSamlAppRoute() throws Exception { 	
    	appRoute(getSecureRouteURL().toString());
    }
        
    protected void appRoute(String host) throws Exception {
        Client client = new Client(host);
        String result = client.get("app-profile-jee-saml");
        assertTrue(result.contains("profile.jsp"));
    }

}

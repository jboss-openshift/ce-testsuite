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

import java.net.URL;

import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/sso/sso70-postgresql-persistent.json",
		labels = "application=sso,component=server",
		parameters = {
                        @TemplateParameter(name = "SSO_ADMIN_USERNAME", value="admin"),
                        @TemplateParameter(name = "SSO_ADMIN_PASSWORD", value="admin"),
		        @TemplateParameter(name = "HTTPS_NAME", value="jboss"),
		        @TemplateParameter(name = "HTTPS_PASSWORD", value="mykeystorepass")
		        })
@OpenShiftResources({
    @OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/sso-app-secret.json"),
    @OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/eap-app-secret.json")
})
public class Sso70ServerPostgresqlPersistentTest extends SsoServerTestBase
{
	
	@RouteURL("sso")
    private URL routeURL;
	
	@RouteURL("secure-sso")
    private URL secureRouteURL;
	
	@Override
    protected URL getRouteURL() {
        return routeURL;
    }
	
	@Override
    protected URL getSecureRouteURL() {
        return secureRouteURL;
    }
}

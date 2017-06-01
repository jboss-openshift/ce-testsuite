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

package org.jboss.test.arquillian.ce.datavirt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.api.TemplateResources;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.ce.httpclient.HttpClient;
import org.jboss.arquillian.ce.httpclient.HttpClientBuilder;
import org.jboss.arquillian.ce.httpclient.HttpClientExecuteOptions;
import org.jboss.arquillian.ce.httpclient.HttpRequest;
import org.jboss.arquillian.ce.httpclient.HttpResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.datavirt.support.JDBCClient;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@TemplateResources(syncInstantiation = true, templates = {
	@Template(url = "https://raw.githubusercontent.com/bdecoste/jdv-jdg-test/master/datavirt63-extensions-support-s2i.json",
		parameters = {
			@TemplateParameter(name = "SOURCE_REPOSITORY_URL", value = "https://github.com/bdecoste/openshift-examples"),
			@TemplateParameter(name = "SOURCE_REPOSITORY_REF", value = "cloud-1303"),
			@TemplateParameter(name = "CONTEXT_DIR", value = "datavirt/jdv-jdg-integration"),
			@TemplateParameter(name = "VDB_DIRS", value = "dynamicvdb-datafederation/src/vdb,jdg-remote-cache-materialization/src/vdb"),
			@TemplateParameter(name = "TEIID_USERNAME", value = "teiidUser"),
			@TemplateParameter(name = "TEIID_PASSWORD", value = "Password1-"),
			@TemplateParameter(name = "EXTENSIONS_REPOSITORY_URL", value = "http://github.com/bdecoste/openshift-examples"),
			@TemplateParameter(name = "EXTENSIONS_REPOSITORY_REF", value = "cloud-1303"),
			@TemplateParameter(name = "EXTENSIONS_DIR", value = "datavirt/jdv-jdg-integration-ext"),
			@TemplateParameter(name = "EXTENSIONS_DOCKERFILE", value = "Dockerfile"),
			@TemplateParameter(name = "ENV_FILES", value = "/etc/datavirt-environment/*,/home/jboss/source/extensions/extras/resourceadapters.env")
		}),
	@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/datagrid/datagrid65-basic.json",
        parameters = {
        	@TemplateParameter(name = "DATAVIRT_CACHE_NAMES", value="ADDRESSBOOK") })
		})
@OpenShiftResources({
	@OpenShiftResource("classpath:datavirt-app-secret.yaml"),
	@OpenShiftResource("classpath:datavirt-app-config-secret.json")
})
public class DatavirtDatagridTest extends DatavirtTestBase
{
	private final HttpClientExecuteOptions execOptions = new HttpClientExecuteOptions.Builder().tries(3)
            .desiredStatusCode(200).delay(10).build();
	
	@RouteURL("datavirt-app")
    private URL routeURL;
	
    protected URL getRouteURL() {
        return routeURL;
    }
    
    @Deployment
    public static WebArchive getDeployment() throws IOException {
        return getDeploymentBase();
    }
    
    @Test
    public void testJdbc() throws Exception {
    	JDBCClient client = createJdbcClient();
    	String host = System.getenv("DATAVIRT_APP_SERVICE_HOST");
		String result = client.call(host, "31000", "mm", "portfolio", "select * from product");
		
		assertTrue(result.contains("14: 1016,DOW,Dow Chemical Company"));
	}
    
    @Test
    public void testMatJdbc() throws Exception {
    	JDBCClient client = createJdbcClient();
    	String host = System.getenv("DATAVIRT_APP_SERVICE_HOST");
		String result = client.call(host, "31000", "mm", "PeopleMat", "select name, id, email from PersonMatModel.PersonMatView");
		
		assertTrue(result.contains("14: Jack Corby,20000015,Jack.Corby@email.com"));
	}
    
    @Test
    @RunAsClient
    public void testODataSecured() throws Exception {
    	HttpClient client = HttpClientBuilder.untrustedConnectionClient();
        HttpRequest request = HttpClientBuilder.doGET(getRouteURL() + "odata");
        HttpResponse response = client.execute(request, execOptions);
        
        assertEquals(401, response.getResponseCode());
    }
    
    @Test
    @RunAsClient
    public void testODataLogin() throws Exception {
    	HttpClient client = HttpClientBuilder.untrustedConnectionClient();
    	
    	String url = getRouteURL().toString();
    	url = url.replaceAll("//", "//teiidUser:Password1-@");

        HttpRequest request = HttpClientBuilder.doGET(url + "odata");
      
        HttpResponse response = client.execute(request, execOptions);
        
        assertEquals(404, response.getResponseCode());
        assertTrue(response.getResponseBodyAsString().contains("VDB name not defined on the URL"));
    }
    
    @Test
    @RunAsClient
    public void testODataVdb() throws Exception {
    	HttpClient client = HttpClientBuilder.untrustedConnectionClient();
    	
    	String url = getRouteURL().toString();
    	url = url.replaceAll("//", "//teiidUser:Password1-@");
    
        HttpRequest request = HttpClientBuilder.doGET(url + "odata/portfolio.1");
      
        HttpResponse response = client.execute(request, execOptions);
        
        assertEquals(200, response.getResponseCode());
        
        String result = response.getResponseBodyAsString();
        assertTrue(result.contains("CUSTOMER"));
    }
    
    @Test
    @RunAsClient
    public void testODataMatVdb() throws Exception {
    	HttpClient client = HttpClientBuilder.untrustedConnectionClient();
    	
    	String url = getRouteURL().toString();
    	url = url.replaceAll("//", "//teiidUser:Password1-@");
    
        HttpRequest request = HttpClientBuilder.doGET(url + "odata/peoplemat.1");
      
        HttpResponse response = client.execute(request, execOptions);
        
        assertEquals(200, response.getResponseCode());
        String result = response.getResponseBodyAsString();
        assertTrue(result.contains("Person"));
        assertTrue(result.contains("ST_Person"));
    }
 
}

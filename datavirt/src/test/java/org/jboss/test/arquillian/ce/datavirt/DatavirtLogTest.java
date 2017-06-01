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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.jboss.arquillian.ce.api.ExternalDeployment;
import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.datavirt.support.JDBCClient;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/datavirt/datavirt63-basic-s2i.json",
		labels = "application=datavirt-app")
@OpenShiftResources({
       @OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/datavirt-app-secret.yaml")
})
public class DatavirtLogTest extends DatavirtTestBase
{
	@RouteURL("datavirt-app")
    private URL routeURL;
	
    protected URL getRouteURL() {
        return routeURL;
    }
    
	@ArquillianResource
	OpenShiftHandle adapter;
    
    @Test
    @RunAsClient
    public void testLogs() throws Exception {
		try {
	        Map<String, String> labels = Collections.singletonMap("application", "datavirt-app");
	        String result = adapter.getLog(null, labels);
	    
	        assertFalse(result.contains("Failure"));
	        assertTrue(result.contains("JBoss Red Hat JBoss Data Virtualization 6.3.5 (AS 7.5.15.Final-redhat-3) started in"));
	        assertTrue(result.contains("Deployed \"portfolio-vdb.xml\""));
	        assertTrue(result.contains("Deployed \"hibernate-portfolio-vdb.xml\""));
	        assertTrue(result.contains("Deployed \"teiid-olingo-odata4.war\""));
	        assertTrue(result.contains("Deployed \"teiid-odata.war\""));
	        assertTrue(result.contains("Deployed \"ModeShape.vdb\""));
	        assertTrue(result.contains("Deployed \"modeshape-rest.war\""));
	        assertTrue(result.contains("TEIID50030 VDB Portfolio.1 model \"MarketData\" metadata loaded."));
	        assertTrue(result.contains("TEIID50030 VDB ModeShape.1 model \"VDB_Lineage\" metadata loaded."));
	        assertTrue(result.contains("TEIID50030 VDB ModeShape.1 model \"Relational_Model_View\" metadata loaded."));
	        assertTrue(result.contains("TEIID50030 VDB ModeShape.1 model \"ModeShape\" metadata loaded."));
	        assertTrue(result.contains("TEIID50030 VDB Portfolio.1 model \"PersonalValuations\" metadata loaded."));
	        assertTrue(result.contains("TEIID50030 VDB Portfolio.1 model \"Stocks\" metadata loaded."));
	        assertTrue(result.contains("TEIID50030 VDB Portfolio.1 model \"OtherHoldings\" metadata loaded."));
	        assertTrue(result.contains("TEIID50030 VDB Portfolio.1 model \"Accounts\" metadata loaded."));
	        assertTrue(result.contains("TEIID50030 VDB Hibernate_Portfolio.1 model \"ProductPricingModel\" metadata loaded."));
		} catch (Exception e){
			e.printStackTrace();
			throw e;
		}
    }

}

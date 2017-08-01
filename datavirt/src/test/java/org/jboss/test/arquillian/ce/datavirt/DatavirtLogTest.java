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

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/datavirt/datavirt63-basic-s2i.json",
        labels = "application=datavirt-app")
@OpenShiftResources({
        @OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/datavirt-app-secret.yaml")
})
public class DatavirtLogTest extends DatavirtTestBase {
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
            assertTrue(result.contains("JBoss Red Hat JBoss Data Virtualization 6.3.4 (AS 7.5.13.Final-redhat-2) started in"));
            assertTrue(result.contains("Deployed \"portfolio-vdb.xml\""));
            assertTrue(result.contains("Deployed \"hibernate-portfolio-vdb.xml\""));
            assertTrue(result.contains("Deployed \"teiid-olingo-odata4.war\""));
            assertTrue(result.contains("Deployed \"teiid-odata.war\""));
            assertTrue(result.contains("Deployed \"ModeShape.vdb\""));
            assertTrue(result.contains("Deployed \"modeshape-cmis.war\""));
            assertTrue(result.contains("Deployed \"modeshape-webdav.war\""));
            assertTrue(result.contains("Deployed \"modeshape-rest.war\""));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
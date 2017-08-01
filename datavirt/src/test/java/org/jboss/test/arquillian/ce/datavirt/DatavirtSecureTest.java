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

import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/datavirt/datavirt63-secure-s2i.json",
        labels = "application=datavirt-app",
        parameters = {
                @TemplateParameter(name = "TEIID_USERNAME", value = "teiidUser"),
                @TemplateParameter(name = "TEIID_PASSWORD", value = "Password1-"),
                @TemplateParameter(name = "HTTPS_NAME", value = "jboss"),
                @TemplateParameter(name = "HTTPS_PASSWORD", value = "mykeystorepass")
        })
@OpenShiftResources({
        @OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/datavirt-app-secret.yaml")
})
public class DatavirtSecureTest extends DatavirtTestBase {
    protected void configureTrustStore() throws IOException {
        InputStream stream = null;
        FileOutputStream file = null;

        try {
            stream = this.getClass().getClassLoader().getResourceAsStream("keystore.jks");
            file = new FileOutputStream("/opt/eap/standalone/configuration/keystore.jks");
            byte[] buffer = new byte[stream.available()];
            stream.read(buffer);

            file.write(buffer);

        } finally {
            stream.close();
            file.close();
        }

        System.setProperty("javax.net.ssl.trustStore", "/opt/eap/standalone/configuration/keystore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "mykeystorepass");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");

    }

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
    public void testSecureJdbc() throws Exception {
        configureTrustStore();

        JDBCClient client = createJdbcClient();
        String host = System.getenv("DATAVIRT_APP_SERVICE_HOST");
        String result = client.call(host, "31443", "mms", "portfolio", "select * from product");

        assertTrue(result.contains("14: 1016,DOW,Dow Chemical Company"));
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
        assertTrue(response.getResponseBodyAsString().contains("CUSTOMER"));
    }

}

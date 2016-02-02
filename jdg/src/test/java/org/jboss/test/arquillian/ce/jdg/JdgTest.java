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

package org.jboss.test.arquillian.ce.jdg;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.jboss.arquillian.ce.api.ConfigurationHandle;
import org.jboss.arquillian.ce.api.ExternalDeployment;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.RunInPod;
import org.jboss.arquillian.ce.api.RunInPodDeployment;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.test.arquillian.ce.jdg.support.MemcachedCache;
import org.jboss.test.arquillian.ce.jdg.support.RESTCache;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

/**
 * @author Marko Luksa
 */
@RunWith(Arquillian.class)
@RunInPod
@ExternalDeployment
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/datagrid/datagrid65-https.json",
        labels = "application=datagrid-app",
        parameters = {
                @TemplateParameter(name = "HOSTNAME_HTTP", value="jdg-http-route.openshift"),
                @TemplateParameter(name = "HOSTNAME_HTTPS", value="jdg-http-route.openshift"),
                @TemplateParameter(name = "HTTPS_NAME", value="jboss"),
                @TemplateParameter(name = "HTTPS_PASSWORD", value="mykeystorepass"),
                @TemplateParameter(name = "IMAGE_STREAM_NAMESPACE", value="${kubernetes.namespace}")})
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:jdg-service-account")
@OpenShiftResources({
        @OpenShiftResource("classpath:jdg-internal-imagestream.json"),
        @OpenShiftResource("classpath:datagrid-service-account.json"),
        @OpenShiftResource("classpath:datagrid-app-secret.json")
})
public class JdgTest {
    private static final boolean USE_SASL = true;

//    public static final String ROUTE_SUFFIX = ".router.default.svc.cluster.local";
    public static final String HTTP_ROUTE_HOST = "jdg-http-route.openshift";
    public static final String HTTPS_ROUTE_HOST = "jdg-http-route.openshift";

    @ArquillianResource
    ConfigurationHandle configuration;

    @Deployment
    @RunInPodDeployment
    public static WebArchive getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML(new StringAsset("<web-app/>"));
        war.addPackage(RESTCache.class.getPackage());

        war.addAsLibraries(
                Maven.resolver()
                        .resolve("com.google.code.simple-spring-memcached:spymemcached:2.8.1")
                        .withTransitivity()
                        .asFile());
        war.addAsLibraries(
                Maven.resolver()
                        .resolve("org.infinispan:infinispan-client-hotrod:6.3.1.Final-redhat-1")
                        .withTransitivity()
                        .asFile());
        return war;
    }

    private String getNamespace() {
        return configuration.getNamespace();
    }

    @Test
    public void testRestService() throws Exception {
        String host = System.getenv("DATAGRID_APP_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("DATAGRID_APP_SERVICE_PORT"));
        
        System.out.println("Host: " + host + " Port: " + port);
        RESTCache<String, Object> cache = new RESTCache<>("default", "http://" + host + ":" + port + "/rest/");
        cache.put("foo1", "bar1");
        assertEquals("bar1", cache.get("foo1"));
    }

    @Test
    public void testSecureRestService() throws Exception {
        trustAllCertificates();

        String host = System.getenv("SECURE_DATAGRID_APP_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("SECURE_DATAGRID_APP_SERVICE_PORT"));
        RESTCache<String, Object> cache = new RESTCache<>("default", "https://" + host + ":" + port + "/rest/");
        cache.put("foo1", "bar1");
        assertEquals("bar1", cache.get("foo1"));
    }

    @Test
    @RunAsClient
    public void testRestRoute() throws Exception {
        String host = HTTP_ROUTE_HOST;
        int port = 80;
        RESTCache<String, Object> cache = new RESTCache<>("default", "http://" + host + ":" + port + "/rest/");
        cache.put("foo1", "bar1");
        assertEquals("bar1", cache.get("foo1"));
    }

    @Test
//    @Ignore("Fails with IOException: Invalid Http response, but works with curl")
    @RunAsClient
    public void testSecureRestRoute() throws Exception {
        trustAllCertificates();

        String host = HTTP_ROUTE_HOST;
        int port = 443;
        RESTCache<String, Object> cache = new RESTCache<>("default", "https://" + host + ":" + port + "/rest/");
        cache.put("foo1", "bar1");
        assertEquals("bar1", cache.get("foo1"));
    }

    @Test
    public void testMemcachedService() throws Exception {
        String host = System.getenv("DATAGRID_APP_MEMCACHED_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("DATAGRID_APP_MEMCACHED_SERVICE_PORT"));
        MemcachedCache<String, Object> cache = new MemcachedCache<>(host, port);
        cache.put("foo2", "bar2");
        assertEquals("bar2", cache.get("foo2"));
    }

    @Test
    @Ignore("Currently there is no memcached route")
    @RunAsClient
    public void testMemcachedRoute() throws Exception {
        MemcachedCache<String, Object> cache = new MemcachedCache<>(HTTP_ROUTE_HOST, 443);
        cache.put("foo2", "bar2");
        assertEquals("bar2", cache.get("foo2"));
    }

    @Test
    @Ignore("Currently there is no memcached route")
    @RunAsClient
    public void testMemcachedRouteWithSasl() throws Exception {
        MemcachedCache<String, Object> cache = new MemcachedCache<>(HTTP_ROUTE_HOST, 443, USE_SASL);
        cache.put("foo2", "bar2");
        assertEquals("bar2", cache.get("foo2"));
    }

    @Test
    public void testHotRodService() throws Exception {
        String host = System.getenv("DATAGRID_APP_HOTROD_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("DATAGRID_APP_HOTROD_SERVICE_PORT"));

        RemoteCacheManager cacheManager = new RemoteCacheManager(
                new ConfigurationBuilder()
                        .addServer()
                        .host(host).port(port)
                        .build()
        );
        RemoteCache<Object, Object> cache = cacheManager.getCache("default");

        cache.put("foo3", "bar3");
        assertEquals("bar3", cache.get("foo3"));
    }

    private static void trustAllCertificates() throws NoSuchAlgorithmException, KeyManagementException {
    	
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        // Install the all-trusting trust manager
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }
}
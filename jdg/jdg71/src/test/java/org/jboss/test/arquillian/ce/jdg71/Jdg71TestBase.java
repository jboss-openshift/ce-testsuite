/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other
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

package org.jboss.test.arquillian.ce.jdg71;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.jboss.arquillian.ce.api.ConfigurationHandle;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.jdg71.support.MemcachedCache;
import org.jboss.test.arquillian.ce.jdg71.support.RESTCache;
import org.junit.Test;

public abstract class Jdg71TestBase {
    public static final String HTTP_ROUTE_HOST = "jdg-http-route.openshift";

    @ArquillianResource
    ConfigurationHandle configuration;

    protected static WebArchive getDeploymentInternal() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML(new StringAsset("<web-app/>"));
        war.addPackage(RESTCache.class.getPackage());
        war.addClass(Jdg71TestBase.class);

        war.addAsLibraries(Libraries.transitive("com.google.code.simple-spring-memcached", "spymemcached"));
        war.addAsLibraries(Libraries.transitive("org.infinispan", "infinispan-client-hotrod"));

        return war;
    }

    @Test
    public void testRestService() throws Exception {
        String host = System.getenv("DATAGRID_APP_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("DATAGRID_APP_SERVICE_PORT"));
        RESTCache<String, Object> cache = new RESTCache<>("default", new URL("http://" + host + ":" + port), "rest");
        cache.put("foo1", "bar1");
        assertEquals("bar1", cache.get("foo1"));
    }

    @Test
    @RunAsClient
    public void testRestRoute(@RouteURL("datagrid-app") URL url) throws Exception {
        RESTCache<String, Object> cache = new RESTCache<>("default", url, "rest");
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
}

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

import org.apache.commons.collections.list.CursorableLinkedList;
import org.jboss.arquillian.ce.api.Tools;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.jdg71.support.MemcachedCache;
import org.jboss.test.arquillian.ce.jdg71.support.RESTCache;
import org.junit.Ignore;
import org.junit.Test;

public abstract class Jdg71TestSecureBase extends Jdg71TestBase {
    protected static WebArchive getDeploymentInternal() {
        WebArchive war = Jdg71TestBase.getDeploymentInternal();
        war.addClass(Jdg71TestSecureBase.class);
        war.addClass(CursorableLinkedList.class);
        return war;
    }
    @Test
    // @Ignore("Fails with IOException: Invalid Http response, but works with curl")
    @RunAsClient
    public void testSecureRestRoute(@RouteURL("secure-datagrid-app") URL url) throws Exception {
        Tools.trustAllCertificates();

        RESTCache<String, Object> cache = new RESTCache<>("default", url, "rest");
        cache.put("foo1", "bar1");
        assertEquals("bar1", cache.get("foo1"));
    }

    @Test
    public void testSecureRestService() throws Exception {
        Tools.trustAllCertificates();

        String host = System.getenv("SECURE_DATAGRID_APP_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("SECURE_DATAGRID_APP_SERVICE_PORT"));
        RESTCache<String, Object> cache = new RESTCache<>("default", new URL("https://" + host + ":" + port), "rest");
        cache.put("foo1", "bar1");
        assertEquals("bar1", cache.get("foo1"));
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
        MemcachedCache<String, Object> cache = new MemcachedCache<>(HTTP_ROUTE_HOST, 443, true);
        cache.put("foo2", "bar2");
        assertEquals("bar2", cache.get("foo2"));
    }
}
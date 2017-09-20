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

package org.jboss.test.arquillian.ce.jdg.common;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.logging.Logger;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.jdg.common.support.RESTCache;
import org.junit.Test;

public abstract class JdgDBPersistentTestBase extends JdgTestSecureBase {
    private static final Logger log = Logger.getLogger(JdgDBPersistentTestBase.class.getName());

    protected abstract String getDriver();

    public static WebArchive getDeployment() {
        WebArchive war = JdgTestSecureBase.getDeployment();
        war.addClass(JdgDBPersistentTestBase.class);
        return war;
    }

    @Test
    @InSequence(1)
    public void testRestService() throws Exception {
        String host = System.getenv("DATAGRID_APP_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("DATAGRID_APP_SERVICE_PORT"));
        RESTCache<String, Object> cache = new RESTCache<>("default", new URL("http://" + host + ":" + port), "rest");
        cache.put("beforeShutdown", "shouldWork");
        assertEquals("shouldWork", cache.get("beforeShutdown"));
    }

    private void restartPod(OpenShiftHandle adapter, String name) throws Exception {
        log.info("Scaling down " + name);
        adapter.scaleDeployment(name, 0);
        log.info("Scaling up " + name);
        adapter.scaleDeployment(name, 1);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void restartDB(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        restartPod(adapter, "datagrid-app-" + getDriver());
        restartPod(adapter, "datagrid-app");
    }

    @Test
    @InSequence(3)
    public void testRestAfterRestartingDB() throws Exception {
        String host = System.getenv("DATAGRID_APP_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("DATAGRID_APP_SERVICE_PORT"));
        RESTCache<String, Object> cache = new RESTCache<>("default", new URL("http://" + host + ":" + port), "rest");
        assertEquals("shouldWork", cache.get("beforeShutdown"));
    }
}
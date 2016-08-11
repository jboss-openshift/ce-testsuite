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

package org.jboss.test.arquillian.ce.webserver;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fspolti
 */

@RunWith(Arquillian.class)
@Template(url = "https://github.com/jboss-openshift/application-templates/raw/master/webserver/jws30-tomcat7-postgresql-persistent-s2i.json",
        labels = "application=jws-app"
)
@OpenShiftResources({
        @OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/jws-app-secret.json")
})
public class WebServerTomcat7PostgresqlDbPVTest extends WebserverTestBase {

    private final String summary = "Testing Persistent PostgreSQL Todo list";
    private final String summaryHttps = "Testing Persistent PostgreSQL Todo list HTTPS";
    private final String description = "This todo was added by Arquillian Test using HTTP using Persistent storage.";
    private final String descriptionHttps = "This todo was added by Arquillian Test using HTTPS using Persistent storage.";

    @ArquillianResource
    private OpenShiftHandle adapter;

    @Deployment
    public static WebArchive getDeployment() throws Exception {
        return getDeploymentInternal();
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void testPostgreSQLDBTodoListAddItems(@RouteURL("jws-app") URL url) throws Exception {
        checkTodoListAddItems(url.toString(),summary, description);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void testPostgreSQLDBTodoListAddItemsSecure(@RouteURL("secure-jws-app") URL url) throws Exception {
        checkTodoListAddItems(url.toString(),summaryHttps, descriptionHttps);
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void testPostgreSQLDBTodoListAddedItems(@RouteURL("jws-app") URL url) throws Exception {
        checkTodoListAddedItems(url.toString(),summary, description);
    }

    @Test
    @RunAsClient
    @InSequence(4)
    public void testPostgreSQLDBTodoListAddedItemsSecure(@RouteURL("secure-jws-app") URL url) throws Exception {
        checkTodoListAddedItems(url.toString(),summaryHttps, descriptionHttps);
    }

    @Test
    @RunAsClient
    @InSequence(5)
    public void restartDatabasePods() throws Exception {
        List<String> pods = new ArrayList<>();
        pods.add("jws-app-postgresql");
        pods.add("jws-app");
        restartPods(adapter, pods);
    }

    /*
    * After restart the pods we have to test it again to make sure the data stored before still there
    */
    @Test
    @RunAsClient
    @InSequence(6)
    public void testPostgreSQLDBTodoListAddedItemsAfterPodsRestart(@RouteURL("jws-app") URL url) throws Exception {
        checkTodoListAddedItems(url.toString(),summary, description);
    }

    @Test
    @RunAsClient
    @InSequence(7)
    public void testPostgreSQLDBTodoListAddedItemsSecureAfterPodsRestart(@RouteURL("secure-jws-app") URL url) throws Exception {
        checkTodoListAddedItems(url.toString(),summaryHttps, descriptionHttps);
    }
}
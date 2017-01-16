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

import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

/**
 * @author fspolti
 */

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/webserver/jws30-tomcat7-mongodb-s2i.json",
        labels = "application=jws-app"
)
@OpenShiftResources({
        @OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/jws-app-secret.json")
})
public class WebServerTomcat7MongoDbBasicTest extends WebserverTestBase {

    private final String summary = "Testing MongoDB Todo list";
    private final String summaryHttps = "Testing MongoDB Todo list HTTPS";
    private final String description = "This todo was added by Arquillian Test using HTTP.";
    private final String descriptionHttps = "This todo was added by Arquillian Test using HTTPS.";

    @Deployment
    public static WebArchive getDeployment() throws Exception {
        return getDeploymentInternal();
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void testMongoDBTodoListAddItems(@RouteURL("jws-app") URL url) throws Exception {
        checkTodoListAddItems(url.toString(),summary, description);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void testMongoDBTodoListAddItemsSecure(@RouteURL("secure-jws-app") URL url) throws Exception {
        checkTodoListAddItems(url.toString(),summaryHttps, descriptionHttps);
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void testMongoDBTodoListAddedItems(@RouteURL("jws-app") URL url) throws Exception {
        checkTodoListAddedItems(url.toString(),summary, description);
    }

    @Test
    @RunAsClient
    @InSequence(4)
    public void testMongoDBTodoListAddedItemsSecure(@RouteURL("secure-jws-app") URL url) throws Exception {
        checkTodoListAddedItems(url.toString(),summaryHttps, descriptionHttps);
    }
}
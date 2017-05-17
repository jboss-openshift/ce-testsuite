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

package org.jboss.test.arquillian.ce.amq.support;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.Tools;
import org.jboss.arquillian.ce.shrinkwrap.Files;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class AmqBase {
    public static final String FILENAME = "amq.properties";
    public static final String USERNAME = System.getProperty("amq.username", "amq-test");
    public static final String PASSWORD = System.getProperty("amq.password", "redhat");

    protected static WebArchive getDeploymentBase(Class... classes) throws IOException {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML(new StringAsset("<web-app/>"));
        war.addClass(AmqBase.class);
        war.addClass(AmqMeshTestBase.class);
        war.addClass(AmqClient.class);
        war.addClass(AmqPersistentTestBase.class);
        war.addClass(AmqTestBase.class);
        war.addClasses(classes);

        war.addAsLibraries(Libraries.transitive("org.apache.activemq", "activemq-client"));
        war.addAsLibraries(Libraries.transitive("org.apache.qpid", "qpid-jms-client"));
        war.addAsLibraries(Libraries.transitive("org.fusesource.mqtt-client", "mqtt-client"));
        war.addAsLibraries(Libraries.transitive("org.fusesource.stompjms", "stompjms-client"));

        Files.PropertiesHandle handle = Files.createPropertiesHandle(FILENAME);
        handle.addProperty("amq.username", USERNAME);
        handle.addProperty("amq.password", PASSWORD);
        handle.store(war);

        return war;
    }

    protected static void restartAmq(OpenShiftHandle handler) throws Exception {
        handler.scaleDeployment("amq-test-amq", 0);
        handler.scaleDeployment("amq-test-amq", 1);
    }

    protected AmqClient createAmqClient(String url) throws Exception {
        Properties properties = Tools.loadProperties(getClass(), FILENAME);
        String username = properties.getProperty("amq.username");
        String password = properties.getProperty("amq.password");
        return new AmqClient(url, username, password);
    }

    protected String getRouteUrl(URL routeUrl, String scheme) {
        StringBuilder routeString = new StringBuilder();
        routeString.append(scheme);
        routeString.append("://");
        routeString.append(routeUrl.getHost());
        routeString.append(":");
        routeString.append(routeUrl.getPort());

        return routeString.toString();
    }
}
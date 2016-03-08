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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author fspolti
 */
@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/webserver/jws30-tomcat7-https-s2i.json",
        labels = "application=jws-app"
)
@OpenShiftResources({
        @OpenShiftResource("classpath:webserver-service-account.json"),
        @OpenShiftResource("classpath:webserver-app-secret.json")
})
@ClientEndpoint
public class WebServerSecureTest extends WebserverTestBase {

    @Deployment
    public static WebArchive getDeployment() throws Exception {
        return getDeploymentInternal();
    }

    @Test
    @RunAsClient
    public void testWebchat(@RouteURL("secure-jws-app") URL url) throws InterruptedException, IOException, DeploymentException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        checkWebChat(url.toURI(), WebServerSecureTest.class);
    }
}
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

package org.jboss.test.arquillian.ce.eap64;

import java.net.URL;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.ce.httpclient.HttpClientBuilder;
import org.jboss.arquillian.ce.httpclient.HttpRequest;
import org.jboss.arquillian.ce.httpclient.HttpResponse;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jonh Wendell
 */

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/eap/eap64-amq-s2i.json", parameters = {
        @TemplateParameter(name = "HTTPS_NAME", value = "jboss"),
        @TemplateParameter(name = "HTTPS_PASSWORD", value = "mykeystorepass") })
@OpenShiftResource("classpath:eap-app-secret.json")
public class Eap64AmqTest {
    @RouteURL("eap-app")
    private URL url;

    @RouteURL("secure-eap-app")
    private URL secureUrl;

    @ArquillianResource
    OpenShiftHandle adapter;

    @Test
    @RunAsClient
    @InSequence(1)
    public void testPageRendering() throws Exception {
        actualTestPageRendering(url.toString());
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void testSecurePageRendering() throws Exception {
        actualTestPageRendering(secureUrl.toString());
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void testLogMessages() throws Exception {
        testLogContains("Received Message from queue: This is message ");
        testLogContains("Received Message from topic: This is message ");
    }

    private void testLogContains(String prefix) throws Exception {
        final int TRIES = 5;
        int i = 0;
        while (i < TRIES) {
            String podLog = adapter.getLog("eap-app-1");
            if (messagesExist(podLog, prefix))
                break;
            Thread.sleep(5 * 1000);
            i++;
        }
        if (i >= TRIES)
            throw new Exception(String.format("Message '%s' not found in log", prefix));
    }

    private boolean messagesExist(String podLog, String prefix) {
        for (int i = 1; i <= 5; i++) {
            if (!podLog.contains(prefix + i))
                return false;
        }
        return true;
    }

    private void actualTestPageRendering(String baseUrl) throws Exception {
        testPageContains(baseUrl, "URL=HelloWorldMDBServletClient");
        testPageContains(baseUrl + "/HelloWorldMDBServletClient", "<em>queue://queue/HELLOWORLDMDBQueue</em>");
        testPageContains(baseUrl + "/HelloWorldMDBServletClient?topic", "<em>topic://topic/HELLOWORLDMDBTopic</em>");
    }

    private void testPageContains(String url, String text) throws Exception {
        HttpRequest request = HttpClientBuilder.doGET(url);
        HttpResponse response = HttpClientBuilder.untrustedConnectionClient().execute(request);

        Assert.assertEquals(200, response.getResponseCode());

        String responseString = response.getResponseBodyAsString();
        Assert.assertTrue(responseString.contains(text));
    }
}

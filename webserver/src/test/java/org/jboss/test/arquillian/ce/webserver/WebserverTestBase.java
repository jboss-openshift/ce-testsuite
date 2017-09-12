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

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.websocket.ContainerProvider;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.Tools;
import org.jboss.arquillian.ce.httpclient.HttpClientBuilder;
import org.jboss.arquillian.ce.httpclient.HttpRequest;
import org.jboss.arquillian.ce.httpclient.HttpResponse;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;

/**
 * @author fspolti
 */
public abstract class WebserverTestBase {

    private Logger log = Logger.getLogger(getClass().getName());
    protected final String URI = "websocket-chat/websocket/chat";
    private boolean ssl;

    /*
    * Returns the correct websocket URI
    */
    public String prepareWsUrl(URI uri) {
        //setting ssl to true if SSL is being used
        ssl = uri.toString().startsWith("https");
        return uri.toString().startsWith("http") ? uri.toString().replace("http", "ws") + "" + URI
            : uri.toString().replace("https", "wss") + "" + URI;
    }

    /*
    * Set the undertow's custom sslContext to ignore untrusted connections.
    *   ** Do not use it in production **
    */
    private void setDefaultWebSocketClientSslProvider() throws Exception {
        log.info("Setting DefaultWebSocketClientSslProvider");
        io.undertow.websockets.jsr.DefaultWebSocketClientSslProvider.setSslContext(Tools.getTlsSslContext());
    }

    /*
    * Check if the websocket-chat is working as expected.
    * websocket-chat sources: https://github.com/jboss-openshift/openshift-quickstarts/tree/master/tomcat-websocket-chat
    * It starts 2 sessions, sender and receiver
    */
    public void checkWebChat(URI url, Class clazz) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        //The response, in this case can be only 1 message.
        final String[] responseMessage = new String[1];
        String prepareUrl = prepareWsUrl(url);
        log.info("ROUTE: " + prepareUrl);
        log.info("Using class endpoint: " + clazz);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        //setting default timeout to avoid this issue
        //java.io.IOException: UT003035: Connection timed out
        container.setDefaultMaxSessionIdleTimeout(120000); //2 minutes

        if (ssl) {
            setDefaultWebSocketClientSslProvider();
        }
        //This session will receive the message sent by the sender session
        Session receiver = container.connectToServer(clazz, new URI(prepareUrl));

        //Reading message from websocket
        synchronized (receiver) {
            receiver.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    //The onMessage method will print when a client is connected or disconnected,
                    //we should avoid these kind of messages
                    if (!message.contains("has joined") && !message.contains("disconnected")) {
                        log.info("Message received -> " + message);
                        responseMessage[0] = message;
                        latch.countDown();
                    }
                }
            });
        }

        if (ssl) {
            setDefaultWebSocketClientSslProvider();
        }
        //This session will send a message to the receiver session
        Session sender = container.connectToServer(clazz, new URI(prepareUrl));
        sender.getBasicRemote().sendText("Hello world!!");

        latch.await(1, TimeUnit.SECONDS);
        //closing the sessions
        sender.close();
        receiver.close();

        // When more than 1 test is performed there will exist more than 2 users, this number
        // will be incremented, we have to accpet Guest*: Hello world!!
        String pattern = "Guest(\\d+): Hello world!!";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(responseMessage[0]);
        Assert.assertTrue(m.matches());
    }

    /*
    * Return a ArrayList<NameValuePair> with the parameters to be inserted in the todo list.
    */
    public Map<String, String> getParams(String summary, String description) {
        Map<String, String> params = new HashMap<>();
        params.put("summary", summary);
        params.put("description", description);
        return params;
    }

    /*
    * Test the todolist, this test will add a new todo on list.
    */
    public void checkTodoListAddItems(String URL, String summary, String description) throws Exception {

        log.info("ROUTE: " + URL);
        log.info("Adding the following item in the todo list [Summary]: " + summary + " - [Description]: " + description);
        HttpRequest request = HttpClientBuilder.doPOST(URL);

        request.setEntity(getParams(summary, description));

        HttpResponse response = HttpClientBuilder.untrustedConnectionClient().execute(request);
        //there is a redirect
        Assert.assertEquals(302, response.getResponseCode());
    }

    /*
    * Test the if the todo were successfully added by checkTodoListAddItems()
    */
    public void checkTodoListAddedItems(String URL, String summary, String description) throws Exception {

        log.info("Cheking if the summary [" + summary + "] and description [" + description + "] was successfully added.");
        HttpRequest request = HttpClientBuilder.doGET(URL);
        HttpResponse response = HttpClientBuilder.untrustedConnectionClient().execute(request);
        String responseString = response.getResponseBodyAsString();

        //responseString cannot be null
        Assert.assertNotNull(responseString);

        //The response is in html format, we need to check if the response contains the items added before
        Assert.assertTrue(responseString.contains(summary));
        Assert.assertTrue(responseString.contains(description));
    }

    /*
    * Restart the running database pods to make sure the data saved before will not get lost
    * @throws Exception for any issue
    */
    public void restartPods(OpenShiftHandle adapter, List<String> pods) throws Exception {
        for (String p: pods) {
            log.info(String.format("Scalling down pod [%s]", p));
            adapter.scaleDeployment(p,0);
            log.info(String.format("Scalling up pod [%s]", p));
            adapter.scaleDeployment(p,1);
        }
    }
}

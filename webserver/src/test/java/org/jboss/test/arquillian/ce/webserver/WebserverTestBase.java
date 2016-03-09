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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.websocket.ContainerProvider;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import junit.framework.Assert;
import org.jboss.arquillian.ce.api.Tools;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author fspolti
 */
public abstract class WebserverTestBase {

    private Logger log = Logger.getLogger(getClass().getName());
    protected final String URI = "websocket-chat/websocket/chat";
    private boolean ssl;

    protected static WebArchive getDeploymentInternal() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML("web.xml");
        war.addAsLibraries(Libraries.transitive("io.undertow", "undertow-websockets-jsr"));
        return war;
    }

    /*
    * Returns the correct websocket URI
    */
    public String prepareUrl(URI uri) {
        //setting ssl to true if SSL is being used
        ssl = uri.toString().startsWith("https");
        return uri.toString().startsWith("http") ? uri.toString().replace("http","ws") + "" + URI
                : uri.toString().replace("https","wss") + "" + URI;
    }

    private void setDefaultWebSocketClientSslProvider() throws Exception {
        log.info("Setting DefaultWebSocketClientSslProvider");
        io.undertow.websockets.jsr.DefaultWebSocketClientSslProvider.setSslContext(Tools.getTlsSslContext());
    }

    /*
    * Check if the websocket-chat is working as expected.
    * It starts 2 sessions, sender and receiver
    */
    public void checkWebChat(URI url, Class clazz) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        //The response, in this case can be only 1 message.
        final String[] responseMessage = new String[1];
        String prepareUrl = prepareUrl(url);
        log.info("ROUTE: " + prepareUrl);
        log.info("Using class endpoint: " + clazz);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

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
                        log.info("Message sent by Guest 1 -> " + message);
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

        Assert.assertEquals("Guest1: Hello world!!", responseMessage[0]);
    }
}
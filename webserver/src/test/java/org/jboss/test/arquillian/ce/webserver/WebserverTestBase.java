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

import io.undertow.websockets.jsr.DefaultWebSocketClientSslProvider;
import junit.framework.Assert;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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
    * Return the container provider to create the UndertowSession
    */
    private WebSocketContainer container() {
        return ContainerProvider.getWebSocketContainer();
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

    /*
    * Configure the SSLContext to accept untrusted connections
    */
    public SSLContext getSslContext() throws KeyManagementException, NoSuchAlgorithmException {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        }};
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init( null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }

    private void setDefaultWebSocketClientSslProvider() throws NoSuchAlgorithmException, KeyManagementException {
        log.info("Setting DefaultWebSocketClientSslProvider");
        io.undertow.websockets.jsr.DefaultWebSocketClientSslProvider.setSslContext(getSslContext());
    }

    /*
    * Check if the websocket-chat is working as expected.
    * It starts 2 sessions, sender and receiver
    */
    public void checkWebChat(URI url, Class clazz) throws URISyntaxException, IOException, DeploymentException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        final CountDownLatch latch = new CountDownLatch(1);
        //The response, in this case can be only 1 message.
        final String[] responseMessage = new String[1];
        log.info("ROUTE: " + prepareUrl(url));

        if (ssl) {
            setDefaultWebSocketClientSslProvider();
        }
        //This session will receive the message sent by the sender session
        Session receiver = container().connectToServer(clazz, new URI(prepareUrl(url)));

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
        Session sender = container().connectToServer(clazz, new URI(prepareUrl(url)));
        sender.getBasicRemote().sendText("Hello world!!");

        latch.await(1, TimeUnit.SECONDS);
        //closing the sessions
        sender.close();
        receiver.close();

        Assert.assertEquals("Guest1: Hello world!!", responseMessage[0]);
    }
}
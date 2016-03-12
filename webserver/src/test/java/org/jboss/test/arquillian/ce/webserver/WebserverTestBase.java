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
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.websocket.ContainerProvider;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import junit.framework.Assert;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
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
    * Allow httpClient to use untrusted connections
    * The code below was tested with httpclient 4.3.6-redhat-1
    * code example from; http://literatejava.com/networks/ignore-ssl-certificate-errors-apache-httpclient-4-4/
    */
    public static HttpClient acceptUntrustedConnClient() throws Exception {
        HttpClientBuilder b = HttpClientBuilder.create();

        // setup a Trust Strategy that allows all certificates.
        //
        SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
            public boolean isTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {
                return true;
            }

        }).build();
        b.setSslcontext(sslContext);

        // don't check Hostnames, either.
        //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
        HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

        // here's the special part:
        //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
        //      -- and create a Registry, to register it.
        //
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, (X509HostnameVerifier) hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();

        // now, we create connection-manager using our Registry.
        //      -- allows multi-threaded use
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        b.setConnectionManager(connMgr);

        // finally, build the HttpClient;
        //      -- done!
        return b.build();
    }

    /*
    * Returns the correct websocket URI
    */
    public String prepareWsUrl(URI uri) {
        //setting ssl to true if SSL is being used
        ssl = uri.toString().startsWith("https");
        return uri.toString().startsWith("http") ? uri.toString().replace("http","ws") + "" + URI
                : uri.toString().replace("https","wss") + "" + URI;
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
    * Return an ArrayList<NameValuePair> with the parameters to be inserted in the todo list.
    */
    public ArrayList<NameValuePair> getParams(String summary, String description) {
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("summary",summary));
        params.add(new BasicNameValuePair("description",description));
        return params;
    }

    /*
    * Test the todolist, this test will add a new todo on list.
    */
    public void checkTodoListAddItems(String URL, String summary, String description) throws Exception {

        log.info("ROUTE: " + URL);
        log.info("Adding the following item in the todo list [Summary]: " + summary + " - [Description]: " + description);
        HttpPost request = new HttpPost(URL);

        request.setEntity(new UrlEncodedFormEntity(getParams(summary,description)));
        HttpResponse response = acceptUntrustedConnClient().execute(request);
        //there is a redirect
        Assert.assertEquals(302, response.getStatusLine().getStatusCode());
    }

    /*
    * Test the if the todos were successfully addded by checkMongoDBTodoListAddItems()
    */
    public void checkTodoListAddedItems(String URL, String summary, String description) throws Exception {

        HttpGet request = new HttpGet(URL);
        HttpResponse response = acceptUntrustedConnClient().execute(request);
        String responseString = new BasicResponseHandler().handleResponse(response);

        //The response is in html format, we need to check if the response contains the itens added before
        Assert.assertTrue(responseString.contains(summary) && responseString.contains(description));

    }

}
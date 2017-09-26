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

import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.fusesource.mqtt.client.*;
import org.jboss.arquillian.ce.api.Tools;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.Test;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by fspolti on 5/12/17.
 */
public class AmqExternalAccessTestBase extends AmqSslTestBase {

    @RouteURL("amq-test-stomp")
    private URL stompUrl;

    @RouteURL("amq-test-mqtt")
    private URL mqttUrl;

    @RouteURL("amq-test-amqp")
    private URL amqpUrl;

    @RouteURL("amq-test-tcp")
    private URL openwireUrl;

    private String openWireMessage = "Arquillian test - OpenWire";
    private String amqpMessage = "Arquillian Test - AMQP";
    private String mqttMessage = "Arquillian test - MQTT";
    private String stompMessage = "Arquillian test - STOMP";

    @Test
    @RunAsClient
    public void testOpenWireConnection() throws Exception {
        Tools.trustAllCertificates();
        AmqClient client = new AmqClient(getRouteUrl(openwireUrl, "ssl"), USERNAME, PASSWORD);

        client.produceOpenWireJms(openWireMessage, true);
        String received = client.consumeOpenWireJms(true);

        assertEquals(openWireMessage, received);
    }

    @Test
    @RunAsClient
    public void testAmqpConnection() throws Exception {
        StringBuilder connectionUrl = new StringBuilder();
        connectionUrl.append(getRouteUrl(amqpUrl, "amqps"));
        connectionUrl.append("?transport.trustStoreLocation=");
        connectionUrl.append(System.getProperty("javax.net.ssl.trustStore"));
        connectionUrl.append("&transport.trustStorePassword=");
        connectionUrl.append(System.getProperty("javax.net.ssl.trustStorePassword"));
        connectionUrl.append("&transport.verifyHost=false");
        AmqClient client = new AmqClient(connectionUrl.toString(), USERNAME, PASSWORD);

        client.produceAmqp(amqpMessage);
        String received = client.consumeAmqp();

        assertEquals(amqpMessage, received);
    }

    @Test
    @RunAsClient
    public void testMqttConnection() throws Exception {
        MQTT mqtt = new MQTT();
        mqtt.setHost(getRouteUrl(mqttUrl, "ssl"));
        mqtt.setUserName(USERNAME);
        mqtt.setPassword(PASSWORD);

        BlockingConnection connection = mqtt.blockingConnection();
        connection.connect();

        Topic[] topics = {new Topic("topics/foo", QoS.EXACTLY_ONCE)};
        connection.subscribe(topics);

        connection.publish("topics/foo", mqttMessage.getBytes(), QoS.EXACTLY_ONCE, false);

        Message msg = connection.receive(5, TimeUnit.SECONDS);

        String received = new String(msg.getPayload());
        assertEquals(mqttMessage, received);
    }

    @Test
    @RunAsClient
    public void testStompConnection() throws Exception {
        AmqClient client = new AmqClient(getRouteUrl(stompUrl, "ssl"), USERNAME, PASSWORD);

        client.produceStomp(stompMessage);
        String received = client.consumeStomp();

        assertEquals(stompMessage, received);
    }

}

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

import org.fusesource.mqtt.client.*;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by fspolti on 5/12/17.
 */
public class AmqSecuredTestBase extends AmqSslTestBase{

    static {
        System.setProperty("javax.net.ssl.trustStore", "/opt/eap/certs/broker.ts");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
    }

    private String openWireMessage = "Arquillian test - Secured OpenWire";
    private String amqpMessage = "Arquillian Test - Secured AMQP";
    private String mqttMessage = "Arquillian test - Secured MQTT";
    private String stompMessage = "Arquillian test - Secured STOMP";

    @Test
    public void testSecuredOpenwire() throws Exception {
        AmqClient client = createAmqClient("ssl://" + System.getenv("AMQ_TEST_AMQ_TCP_SSL_SERVICE_HOST") + ":61617");

        client.produceOpenWireJms(openWireMessage, true);
        String received = client.consumeOpenWireJms(true);

        assertEquals(openWireMessage, received);
    }

    @Test
    @SuppressWarnings("unused")
    public void testSecuredAmqp() throws Exception {
        StringBuilder connectionUrl = new StringBuilder();
        connectionUrl.append("amqps://");
        connectionUrl.append(System.getenv("AMQ_TEST_AMQ_AMQP_SSL_SERVICE_HOST"));
        connectionUrl.append(":5671?transport.trustStoreLocation=");
        connectionUrl.append(System.getProperty("javax.net.ssl.trustStore"));
        connectionUrl.append("&transport.trustStorePassword=");
        connectionUrl.append(System.getProperty("javax.net.ssl.trustStorePassword"));
        connectionUrl.append("&transport.verifyHost=false");
        AmqClient client = createAmqClient(connectionUrl.toString());

        client.produceAmqp(amqpMessage);

        String received = client.consumeAmqp();
        assertEquals(amqpMessage, received);
    }

    @Test
    public void testSecuredMqtt() throws Exception {
        MQTT mqtt = new MQTT();
        mqtt.setHost("ssl://" + System.getenv("AMQ_TEST_AMQ_MQTT_SSL_SERVICE_HOST") + ":8883");
        mqtt.setUserName(USERNAME);
        mqtt.setPassword(PASSWORD);
        mqtt.setSslContext(getSSLContext());

        BlockingConnection connection = mqtt.blockingConnection();
        connection.connect();

        Topic[] topics = {new Topic("topics/foo", QoS.AT_LEAST_ONCE)};
        connection.subscribe(topics);

        connection.publish("topics/foo", mqttMessage.getBytes(), QoS.AT_LEAST_ONCE, false);

        Message msg = connection.receive(5, TimeUnit.SECONDS);

        String received = new String(msg.getPayload());
        msg.ack();

        connection.disconnect();

        assertEquals(mqttMessage, received);
    }

    @Test
    public void testSecuredStomp() throws Exception {
        AmqClient client = createAmqClient("ssl://" + System.getenv("AMQ_TEST_AMQ_STOMP_SSL_SERVICE_HOST") + ":61612");

        client.produceStomp(stompMessage);
        String received = client.consumeStomp();

        assertEquals(stompMessage, received);
    }

}
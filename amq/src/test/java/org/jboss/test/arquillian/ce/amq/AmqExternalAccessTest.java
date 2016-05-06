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

package org.jboss.test.arquillian.ce.amq;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.test.arquillian.ce.amq.support.AmqClient;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/amq/amq62-ssl.json",
	parameters = {
		@TemplateParameter(name = "MQ_QUEUES", value = "QUEUES.FOO,QUEUES.BAR"),
		@TemplateParameter(name = "APPLICATION_NAME", value = "amq-test"),
		@TemplateParameter(name = "MQ_USERNAME", value = "${amq.username:amq-test}"),
		@TemplateParameter(name = "MQ_PASSWORD", value = "${amq.password:redhat}"),
		@TemplateParameter(name = "MQ_PROTOCOL", value = "openwire,amqp,mqtt,stomp"),
		@TemplateParameter(name = "AMQ_TRUSTSTORE_PASSWORD", value = "password"),
		@TemplateParameter(name = "AMQ_KEYSTORE_PASSWORD", value = "password")})
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default")
@OpenShiftResources({
@OpenShiftResource("classpath:amq-routes.json"),
@OpenShiftResource("classpath:amq-app-secret.json"),
@OpenShiftResource("classpath:testrunner-secret.json")
})
public class AmqExternalAccessTest extends AmqSslTestBase {

    static final String STOMP_URL = "ssl://stomp-amq.router.default.svc.cluster.local:443";
	static final String MQTT_URL = "ssl://mqtt-amq.router.default.svc.cluster.local:443";
	static final String AMQP_URL = "amqps://amqp-amq.router.default.svc.cluster.local:443";
	static final String OPENWIRE_URL = "ssl://tcp-amq.router.default.svc.cluster.local:443";

    private String openWireMessage = "Arquillian test - OpenWire";
    private String amqpMessage = "Arquillian Test - AMQP";
    private String mqttMessage = "Arquillian test - MQTT";
    private String stompMessage = "Arquillian test - STOMP";
    
	@Test
	@RunAsClient
    public void testOpenWireConnection() throws Exception {
        AmqClient client = new AmqClient(OPENWIRE_URL, USERNAME, PASSWORD);

        client.produceOpenWireJms(openWireMessage, true);
        String received = client.consumeOpenWireJms(true);

        assertEquals(openWireMessage, received);
    }

    @Test
    @RunAsClient
    public void testAmqpConnection() throws Exception {
    	StringBuilder connectionUrl = new StringBuilder();
        connectionUrl.append(AMQP_URL);
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
        mqtt.setHost(MQTT_URL);
        mqtt.setUserName(USERNAME);
        mqtt.setPassword(PASSWORD);

        BlockingConnection connection = mqtt.blockingConnection();
        connection.connect();

        Topic[] topics = {new Topic("topics/foo", QoS.AT_LEAST_ONCE)};
        connection.subscribe(topics);

        connection.publish("topics/foo", mqttMessage.getBytes(), QoS.AT_LEAST_ONCE, false);

        Message msg = connection.receive(5, TimeUnit.SECONDS);

        String received = new String(msg.getPayload());
        assertEquals(mqttMessage, received);
    }

    @Test
    @RunAsClient
    public void testStompConnection() throws Exception {
        AmqClient client = new AmqClient(STOMP_URL, USERNAME, PASSWORD);

        client.produceStomp(stompMessage);
        String received = client.consumeStomp();

        assertEquals(stompMessage, received);
    }
	
}

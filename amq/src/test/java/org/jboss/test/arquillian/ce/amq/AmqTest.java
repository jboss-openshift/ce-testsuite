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

import java.io.IOException;
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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.amq.support.AmqClient;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ricardo Martinelli
 */
@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/amq/amq62-basic.json",
    parameters = {
        @TemplateParameter(name = "MQ_QUEUES", value = "QUEUES.FOO,QUEUES.BAR"),
        @TemplateParameter(name = "APPLICATION_NAME", value = "amq-test"),
        @TemplateParameter(name = "MQ_USERNAME", value = "${amq.username:amq-test}"),
        @TemplateParameter(name = "MQ_PASSWORD", value = "${amq.password:redhat}"),
        @TemplateParameter(name = "MQ_PROTOCOL", value = "openwire,amqp,mqtt,stomp")})
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default")
@OpenShiftResources({
	@OpenShiftResource("classpath:testrunner-secret.json")
})
public class AmqTest extends AmqTestBase {

    private String openWireMessage = "Arquillian test - OpenWire";
    private String amqpMessage = "Arquillian Test - AMQP";
    private String mqttMessage = "Arquillian test - MQTT";
    private String stompMessage = "Arquillian test - STOMP";

    @Deployment
    public static WebArchive getDeployment() throws IOException {
        return getDeploymentBase();
    }

    @Test
    public void testOpenWireConnection() throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");

        client.produceOpenWireJms(openWireMessage, false);
        String received = client.consumeOpenWireJms(false);

        assertEquals(openWireMessage, received);
    }

    @Test
    public void testAmqpConnection() throws Exception {
        AmqClient client = createAmqClient("amqp://" + System.getenv("AMQ_TEST_AMQ_AMQP_SERVICE_HOST") + ":5672");

        client.produceAmqp(amqpMessage);
        String received = client.consumeAmqp();

        assertEquals(amqpMessage, received);
    }

    @Test
    public void testMqttConnection() throws Exception {
        MQTT mqtt = new MQTT();
        mqtt.setHost("tcp://" + System.getenv("AMQ_TEST_AMQ_MQTT_SERVICE_HOST") + ":1883");
        mqtt.setUserName(USERNAME);
        mqtt.setPassword(PASSWORD);

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
    public void testStompConnection() throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_STOMP_SERVICE_HOST") + ":61613");

        client.produceStomp(stompMessage);
        String received = client.consumeStomp();

        assertEquals(stompMessage, received);
    }

}

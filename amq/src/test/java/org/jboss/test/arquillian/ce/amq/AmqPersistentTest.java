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
import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.amq.support.AmqClient;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ricardo Martinelli
 */
@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/amq/amq62-persistent.json",
    parameters = {
        @TemplateParameter(name = "MQ_QUEUES", value = "QUEUES.FOO,QUEUES.BAR"),
        @TemplateParameter(name = "MQ_TOPICS", value = "topics.mqtt"),
        @TemplateParameter(name = "APPLICATION_NAME", value = "amq-test"),
        @TemplateParameter(name = "MQ_USERNAME", value = "${amq.username:amq-test}"),
        @TemplateParameter(name = "MQ_PASSWORD", value = "${amq.password:redhat}"),
        @TemplateParameter(name = "MQ_PROTOCOL", value = "openwire,amqp,mqtt,stomp")})
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default")
@OpenShiftResources({
	@OpenShiftResource("classpath:testrunner-secret.json")
})
public class AmqPersistentTest extends AmqTestBase {

    private String openWireMessage = "Arquillian test - Persistent OpenWire";
    private String amqpMessage = "Arquillian Test - Persistent AMQP";
    private String stompMessage = "Arquillian Test - Persistent STOMP";
    private String mqttMessage = "Arquillian test - Persistent MQTT";

    private static BlockingConnection receiveConnection;

    @Deployment
    public static WebArchive getDeployment() throws IOException {
        return getDeploymentBase();
    }

    @Test
    @InSequence(1)
    public void testOpenWireProduceConnection() throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");

        client.produceOpenWireJms(openWireMessage, false);
    }

    @Test
    @InSequence(2)
    public void testAmqpProduceConnection() throws Exception {
        AmqClient client = createAmqClient("amqp://" + System.getenv("AMQ_TEST_AMQ_AMQP_SERVICE_HOST") + ":5672");

        client.produceAmqp(amqpMessage);
    }

    @Test
    @InSequence(3)
    public void testStompProduceConnection() throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_STOMP_SERVICE_HOST") + ":61613");

        client.produceStomp(stompMessage);
    }

    @Test
    @InSequence(4)
    public void testMqttProduceConnection() throws Exception {
        MQTT mqtt = new MQTT();
        mqtt.setHost("tcp://" + System.getenv("AMQ_TEST_AMQ_MQTT_SERVICE_HOST") + ":1883");
        mqtt.setUserName(USERNAME);
        mqtt.setPassword(PASSWORD);

        BlockingConnection connection = mqtt.blockingConnection();
        connection.connect();

        receiveConnection = mqtt.blockingConnection();
        receiveConnection.connect();

        Topic[] topics = {new Topic("topics/mqtt", QoS.AT_LEAST_ONCE)};
        receiveConnection.subscribe(topics);

        connection.publish("topics/mqtt", mqttMessage.getBytes(), QoS.AT_LEAST_ONCE, true);

        connection.disconnect();
    }

    @Test
    @RunAsClient
    @InSequence(5)
    public void testRestartAmq(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        restartAmq(adapter);
    }

    @Test
    @InSequence(6)
    public void testOpenWireConsumeConnection() throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");

        String received = client.consumeOpenWireJms(false);

        assertEquals(openWireMessage, received);
    }

    @Test
    @InSequence(7)
    public void testAmqpConsumeConnection() throws Exception {
        AmqClient client = createAmqClient("amqp://" + System.getenv("AMQ_TEST_AMQ_AMQP_SERVICE_HOST") + ":5672");

        String received = client.consumeAmqp();

        assertEquals(amqpMessage, received);
    }

    @Test
    @InSequence(8)
    public void testStompConsumeConnection() throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_STOMP_SERVICE_HOST") + ":61613");

        String received = client.consumeStomp();

        assertEquals(stompMessage, received);
    }

    @Test
    @InSequence(9)
    @Ignore("https://github.com/jboss-openshift/ce-testsuite/issues/121")
    public void testMqttConnection() throws Exception {
        Message msg = receiveConnection.receive(5, TimeUnit.SECONDS);

        String received = new String(msg.getPayload());
        receiveConnection.disconnect();

        assertEquals(mqttMessage, received);
    }

}

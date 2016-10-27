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

package org.jboss.test.arquillian.ce.amq;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Replicas;
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ales Justin
 */
@RunWith(Arquillian.class)
// TODO -- change this once multi repl image is in prod
@Template(url = "https://raw.githubusercontent.com/alesj/application-templates/amq1/amq/amq62-repl.json",
    parameters = {
        @TemplateParameter(name = "MQ_QUEUES", value = "QUEUES.FOO,QUEUES.BAR"),
        @TemplateParameter(name = "MQ_TOPICS", value = "topics.mqtt,TOPICS.FOO"),
        @TemplateParameter(name = "APPLICATION_NAME", value = "amq-test"),
        @TemplateParameter(name = "MQ_USERNAME", value = "${amq.username:amq-test}"),
        @TemplateParameter(name = "MQ_PASSWORD", value = "${amq.password:redhat}"),
        @TemplateParameter(name = "MQ_PROTOCOL", value = "openwire,amqp,mqtt,stomp"),
        @TemplateParameter(name = "IMAGE_STREAM_NAMESPACE", value = "${kubernetes.namespace}")})
// remove when amq-internal-is is removed
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default")
@OpenShiftResources({
    @OpenShiftResource("classpath:testrunner-secret.json")
})
@Replicas(1)
public class AmqDurableTopicSubscriberMigrationTest extends AmqMigrationTestBase {

    private static final int N = 10;
//    private static final String TOPIC_OBJECT_NAME = "org.apache.activemq:brokerName=%s,clientId=tmp123,consumerId=Durable(tmp123_%s),destinationName=TOPICS.FOO,destinationType=Topic,endpoint=Consumer,type=Broker";
//    private static final String HANDLED_MSGS = "Handled %s messages for topic subscriber 'TOPICS.FOO'";

    @Deployment
    public static WebArchive getDeployment() throws IOException {
        return getDeploymentBase(AmqMigrationTestBase.class);
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void testScaleUp(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        adapter.scaleDeployment("amq-test-amq", 2); // scale up
    }

    @Test
    @InSequence(2)
    public void testSendMsgs() throws Exception {
        // hopfully msgs get distributed
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");
        for (int i = 1; i <= N; i++) {
            client.createTopicSubscriber("Sub" + i);
        }
        for (int i = 1; i <= N; i++) {
            client.produceTopic("Text" + i);
        }
    }

    //    @Test
//    @RunAsClient
//    @InSequence(3)
    public void testScaleDown(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        List<String> pods = adapter.getPods("amq-test-amq");
        Assert.assertEquals(2, pods.size());

//        boolean distributed = true;
//        int[] sizes = new int[2];
//        int i = 0;
//        for (String podName : pods) {
//            for (int j = 1; j <= N; j++) {
//                ObjectName objectName = new ObjectName(String.format(TOPIC_OBJECT_NAME, podName, "Sub" + j));
//                int size = queryMessages(adapter, podName, objectName, "PendingQueueSize");
//                distributed = (distributed && (size > 0));
//                sizes[i++] = size;
//            }
//        }

        adapter.scaleDeployment("amq-test-amq", 1); // scale down

//        if (distributed) {
//            // msgs were distributed, hence should be migrated
//            waitForDrain(adapter, 0, String.format(HANDLED_MSGS, sizes[0]), String.format(HANDLED_MSGS, sizes[1]));
//        }

        // drain should kick-in in any case
        waitForDrain(adapter, 0);
    }

    @Test
    @InSequence(4)
    public void testSubscriberConsume() throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");
        for (int i = 1; i <= N; i++) {
            Set<Integer> msgNumbers = new TreeSet<>();
            for (String msg : client.consumeTopic(N, "Sub" + i)) {
                msgNumbers.add(Integer.parseInt(msg.substring(4)));
            }
            Assert.assertEquals(N, msgNumbers.size());
        }
    }
}

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
import java.util.LinkedHashSet;
import java.util.Set;

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
        @TemplateParameter(name = "MQ_TOPICS", value = "topics.mqtt"),
        @TemplateParameter(name = "APPLICATION_NAME", value = "amq-test"),
        @TemplateParameter(name = "MQ_USERNAME", value = "${amq.username:amq-test}"),
        @TemplateParameter(name = "MQ_PASSWORD", value = "${amq.password:redhat}"),
        @TemplateParameter(name = "MQ_PROTOCOL", value = "openwire,amqp,mqtt,stomp"),
        @TemplateParameter(name = "IMAGE_STREAM_NAMESPACE", value = "${kubernetes.namespace}")}) // remove when amq-internal-is is removed
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default")
@OpenShiftResources({
    @OpenShiftResource("classpath:testrunner-secret.json"),
    @OpenShiftResource("classpath:amq-internal-imagestream.json") // custom dev imagestream; remove when multi repl image is in prod
})
@Replicas(3)
public class AmqMultiReplicasPVTest extends AmqTestBase {

    @Deployment
    public static WebArchive getDeployment() throws IOException {
        return getDeploymentBase();
    }

    private void sendNMessages(int from, int to) throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");
        Set<String> msgs = new LinkedHashSet<>();
        for (int i = from; i < to; i++) {
            msgs.add("msg" + i);
        }
        client.produceOpenWireJms(msgs, false);
    }

    @Test
    @InSequence(1)
    public void testSend1() throws Exception {
        sendNMessages(1, 4);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void testRestartAmq(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        adapter.scaleDeployment("amq-test-amq", 2);
        adapter.scaleDeployment("amq-test-amq", 3);
    }

    @Test
    @InSequence(3)
    public void testSend2() throws Exception {
        sendNMessages(4, 7);
    }

    @Test
    @RunAsClient
    @InSequence(4)
    public void testDeletePodAndWaitForRecreation1(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        adapter.replacePods("amq-test-amq", 1, 3);
    }

    @Test
    @InSequence(5)
    public void testSend3() throws Exception {
        sendNMessages(7, 10);
    }

    @Test
    @RunAsClient
    @InSequence(6)
    public void testDeletePodAndWaitForRecreation2(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        adapter.replacePods("amq-test-amq", 1, 3);
    }

    @Test
    @InSequence(7)
    public void testOpenWireConsumeConnection() throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");
        Set<String> msgs = new LinkedHashSet<>();
        client.consumeOpenWireJms(msgs, 9, false);
        while (client.consumeOpenWireJms(2000, false) != null) ;
    }
}

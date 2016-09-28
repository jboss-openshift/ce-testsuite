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
import java.util.Collection;

import javax.management.ObjectName;

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
        @TemplateParameter(name = "MQ_TOPICS", value = "topics.mqtt"),
        @TemplateParameter(name = "APPLICATION_NAME", value = "amq-test"),
        @TemplateParameter(name = "MQ_USERNAME", value = "${amq.username:amq-test}"),
        @TemplateParameter(name = "MQ_PASSWORD", value = "${amq.password:redhat}"),
        @TemplateParameter(name = "MQ_PROTOCOL", value = "openwire,amqp,mqtt,stomp"),
        @TemplateParameter(name = "IMAGE_STREAM_NAMESPACE", value = "${kubernetes.namespace}")})
// remove when amq-internal-is is removed
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default")
@OpenShiftResources({
    @OpenShiftResource("classpath:testrunner-secret.json"),
    @OpenShiftResource("classpath:amq-internal-imagestream.json") // custom dev imagestream; remove when multi repl image is in prod
})
@Replicas(1)
public class AmqFillDataDirTest extends AmqMigrationTestBase {

    private static final String QUEUE_OBJECT_NAME = "org.apache.activemq:brokerName=%s,destinationName=QUEUES.FOO,destinationType=Queue,type=Broker";
    private static final String LOCK_LOG = "Successfully locked directory:";
    private static final String SPLIT_LOG = "split-";

    private static int parseCount(String log) {
        int p = log.indexOf(LOCK_LOG);
        if (p == -1) {
            throw new IllegalStateException("Missing lock log: " + log);
        }
        p = log.indexOf(SPLIT_LOG, p) + SPLIT_LOG.length();
        String number = "";
        while (p < log.length() && Character.isDigit(log.charAt(p))) {
            number += log.charAt(p);
            p++;
        }
        return Integer.parseInt(number);
    }

    @Deployment
    public static WebArchive getDeployment() throws IOException {
        return getDeploymentBase(AmqMigrationTestBase.class);
    }

    @Test
    @InSequence(1)
    public void testSendMsgs() throws Exception {
        sendNMessages(1, 4);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void testScale(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        Collection<String> pods = adapter.getPods("amq-test-amq");
        Assert.assertEquals(1, pods.size()); // there should be only one
        String firstPod = pods.iterator().next(); // we put the msgs here

        ObjectName objectName = new ObjectName(String.format(QUEUE_OBJECT_NAME, firstPod));
        Assert.assertEquals(3, queryMessages(adapter, firstPod, objectName, "QueueSize")); // smoke test for 3 msgs

        String log = adapter.getLog(firstPod);
        int count = parseCount(log);

        adapter.scaleDeployment("amq-test-amq", 2); // scale up

        String sndPod = null;
        pods = adapter.getReadyPods("amq-test-amq");
        for (String podName : pods) {
            if (podName.equals(firstPod) == false) {
                sndPod = podName;
                break;
            }
        }
        Assert.assertNotNull(sndPod);

        adapter.deletePod(firstPod, -1); // kill first, RC should re-use data dir

        adapter.waitForReadyPods("amq-test-amq", 2);

        pods = adapter.getReadyPods("amq-test-amq");
        for (String podName : pods) {
            if (podName.equals(sndPod) == false) {
                int count2 = parseCount(adapter.getLog(podName));
                Assert.assertEquals(count, count2); // should have used the same split- data dir
            }
        }
    }

    @Test
    @InSequence(3)
    public void testConsumeMsgs() throws Exception {
        consumeMsgs(3);
    }
}
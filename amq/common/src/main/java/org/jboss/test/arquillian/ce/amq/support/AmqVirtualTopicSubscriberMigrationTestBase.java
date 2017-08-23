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

import java.util.Set;
import java.util.TreeSet;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by fspolti on 5/12/17.
 */
public class AmqVirtualTopicSubscriberMigrationTestBase extends AmqMigrationTestBase {

    private static final int N = 10;

    @Test
    @RunAsClient
    @InSequence(1)
    public void testScaleUp(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        adapter.scaleDeployment("amq-test-amq", 2); // scale up
        adapter.waitForReadyPods("amq-test-amq", 2);
    }

    @Test
    @InSequence(2)
    public void testSendMsgs() throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");
        for (int i = 1; i <= N; i++) {
            client.createVirtualTopicSubscriber("Sub" + i);
        }
        for (int i = 1; i <= N; i++) {
            client.produceVirtualTopic("Text" + i);
        }
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void testScaleDown(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        adapter.scaleDeployment("amq-test-amq", 1); // scale down
        adapter.waitForReadyPods("amq-test-amq", 1);

        Assert.assertNotEquals("Migration should have finished", -1, waitForDrain(adapter, 0));
    }

    @Test
    @InSequence(4)
    public void testSubscriberConsume() throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");
        for (int i = 1; i <= N; i++) {
            Set<Integer> msgNumbers = new TreeSet<>();
            for (String msg : client.consumeVirtualTopic(N, 2000, "Sub" + i)) {
                msgNumbers.add(Integer.parseInt(msg.substring(4)));
            }
            Assert.assertEquals(N, msgNumbers.size());
        }
    }
}
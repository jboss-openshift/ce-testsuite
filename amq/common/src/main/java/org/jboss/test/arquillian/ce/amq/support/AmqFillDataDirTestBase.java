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

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Test;

import javax.management.ObjectName;
import java.util.Collection;

/**
 * Created by fspolti on 5/12/17.
 */
public class AmqFillDataDirTestBase extends AmqMigrationTestBase {

    private static final String QUEUE_OBJECT_NAME = "org.apache.activemq:brokerName=%s,destinationName=QUEUES.FOO,destinationType=Queue,type=Broker";
    private static final String LOCK_LOG = "Successfully locked directory:";
    private static final String SPLIT_LOG = "split-";

    private static final int NUM_MSGS = 4;

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

    @Test
    @RunAsClient
    @InSequence(1)
    public void testWaitForPods(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        adapter.waitForReadyPods("amq-test", 1);
    }

    @Test
    @InSequence(2)
    public void testSendMsgs() throws Exception {
        sendNMessages(0, NUM_MSGS);
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void testScale(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        Collection<String> pods = adapter.getPods("amq-test");
        Assert.assertEquals(1, pods.size()); // there should be only one
        String firstPod = pods.iterator().next(); // we put the msgs here

        ObjectName objectName = new ObjectName(String.format(QUEUE_OBJECT_NAME, firstPod));
        Assert.assertEquals(NUM_MSGS, queryMessages(adapter, firstPod, objectName, "QueueSize"));

        String log = adapter.getLog(firstPod);
        int count = parseCount(log);

        adapter.scaleDeployment("amq-test", 2); // scale up

        String sndPod = null;
        pods = adapter.getReadyPods("amq-test");
        for (String podName : pods) {
            if (podName.equals(firstPod) == false) {
                sndPod = podName;
                break;
            }
        }
        Assert.assertNotNull(sndPod);

        adapter.deletePod(firstPod, -1); // kill first, RC should re-use data dir

        adapter.waitForReadyPods("amq-test", 2);

        pods = adapter.getReadyPods("amq-test");
        for (String podName : pods) {
            if (podName.equals(sndPod) == false) {
                int count2 = parseCount(adapter.getLog(podName));
                Assert.assertEquals(count, count2); // should have used the same split- data dir
            }
        }
    }

    @Test
    @InSequence(4)
    public void testConsumeMsgs() throws Exception {
        consumeMsgs(NUM_MSGS);
    }

}

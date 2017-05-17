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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by fspolti on 5/12/17.
 */
public class AmqNMigrationsTestBase extends AmqMigrationTestBase {

    private static final Logger log = Logger.getLogger(AmqNMigrationsTestBase.class.getName());

    private static final int REPLICAS = 5;
    private static final String QUEUE_OBJECT_NAME = "org.apache.activemq:brokerName=%s,destinationName=QUEUES.FOO,destinationType=Queue,type=Broker";


    @Test
    @RunAsClient
    @InSequence(1)
    public void testScaleUp(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        adapter.scaleDeployment("amq-test-amq", REPLICAS);
    }

    @Test
    @InSequence(2)
    public void testSendMsgs() throws Exception {
        sendNMessages(1, 11); // 10 msgs
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void testMigrate(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        final int N = 10;

        for (int i = 0; i < N; i++) {
            List<String> pods = new ArrayList<>(adapter.getReadyPods("amq-test-amq"));
            int pi;
            for (pi = 0; pi < pods.size(); pi++) {
                String pod = pods.get(pi);
                ObjectName objectName = new ObjectName(String.format(QUEUE_OBJECT_NAME, pod));
                Number msgCount = queryMessages(adapter, pod, objectName, "QueueSize");
                if (msgCount.intValue() > 0) {
                    break; // find first with some msgs
                }
            }
            Assert.assertTrue("No pod with msgs!?!", pi < pods.size()); // such pod should exist

            log.info(String.format("Deleting pod: %s", pods.get(pi)));
            adapter.deletePod(pods.get(pi), -1);
        }

        adapter.waitForReadyPods("amq-test-amq", REPLICAS);
    }

    @Test
    @InSequence(4)
    public void testConsumeMsgs() throws Exception {
        consumeMsgs(10);
    }
}
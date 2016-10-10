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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.management.ObjectName;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.test.arquillian.ce.amq.support.AmqClient;
import org.jolokia.client.request.J4pReadRequest;
import org.junit.Assert;

/**
 * @author Ales Justin
 */
public class AmqMigrationTestBase extends AmqTestBase {

    protected static final String END = "A-MQ migration finished";

    protected void sendNMessages(int from, int to) throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");
        Set<String> msgs = new LinkedHashSet<>();
        for (int i = from; i < to; i++) {
            msgs.add("msg" + i);
        }
        client.produceOpenWireJms(msgs, false);
    }

    protected Set<String> consumeMsgs(int msgsSize) throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");

        Set<String> msgs = new LinkedHashSet<>();
        client.consumeOpenWireJms(msgs, msgsSize, false);

        Set<Integer> msgNumbers = new TreeSet<>();
        for (String msg : msgs) {
            msgNumbers.add(Integer.parseInt(msg.substring(3)));
        }
        Assert.assertEquals("Missing msgs: " + msgNumbers, msgsSize, msgNumbers.size()); // make sure we have all msgs

        while (client.consumeOpenWireJms(2000, false) != null) ; // test we don't have more msgs

        return msgs;
    }

    protected static int queryMessages(OpenShiftHandle adapter, String podName, ObjectName objectName, String attributeName) throws Exception {
        J4pReadRequest request = new J4pReadRequest(objectName, attributeName);
        return adapter.jolokia(Number.class, podName, request).intValue();
    }

    protected static int waitForDrain(OpenShiftHandle adapter, int p) throws Exception {
        return waitForDrain(adapter, p, END);
    }

    protected static int waitForDrain(OpenShiftHandle adapter, int p, String... parts) throws Exception {
        return waitForDrain(adapter, p, false, parts);
    }

    protected static int waitForDrain(OpenShiftHandle adapter, int p, boolean checkIfReady, String... parts) throws Exception {
        int repeat = 20;
        while (repeat > 0) {
            String drainLog = null;
            if (checkIfReady) {
                Collection<String> drainers = adapter.getReadyPods("amq-test-drainer");
                if (drainers.size() > 0) {
                    drainLog = adapter.getLog(drainers.iterator().next());
                }
            } else {
                drainLog = adapter.getLog("amq-test-drainer", null);
            }

            if (drainLog != null) {
                for (String content : parts) {
                    int pp = drainLog.indexOf(content, p);
                    if (pp != -1) {
                        return pp + content.length();
                    }
                }
            }

            repeat--;
            Thread.sleep(6000);
        }
        throw new IllegalStateException("Migration not finished?!");
    }

}

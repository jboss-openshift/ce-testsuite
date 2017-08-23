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

package org.jboss.test.arquillian.ce.amq.support;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jolokia.client.request.J4pReadRequest;
import org.junit.Assert;

import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * @author Ales Justin
 */
public class AmqMigrationTestBase extends AmqBase {

    protected static final String END = "A-MQ migration finished";
    protected static final long TIMEOUT = 60000;

    protected static int queryMessages(OpenShiftHandle adapter, String podName, ObjectName objectName, String attributeName) throws Exception {
        J4pReadRequest request = new J4pReadRequest(objectName, attributeName);
        Number number = adapter.jolokia(Number.class, podName, request);
        return (number != null) ? number.intValue() : 0;
    }

    protected static int waitForDrain(final OpenShiftHandle adapter, int startIndex) throws Exception {
        return waitForDrain(adapter, startIndex, END);
    }

    protected static int waitForDrain(final OpenShiftHandle adapter, final int startIndex, final String... parts) throws Exception {
        return waitForDrain(adapter, startIndex, false, parts);
    }

    protected static int waitForDrain(final OpenShiftHandle adapter, final int startIndex, final boolean checkIfReady, final String... parts) throws Exception {
        return waitForDrain(adapter, startIndex, false, TIMEOUT, parts);
    }

    protected static int waitForDrain(final OpenShiftHandle adapter, final int startIndex, final boolean checkIfReady, final long timeout, String... parts) throws Exception {
        return waitForPodMessage(adapter, "amq-test-drainer", startIndex, checkIfReady, timeout, parts);
    }

    protected static int waitForPodMessage(final OpenShiftHandle adapter, final String podPrefix, int startIndex, final boolean checkIfReady, final long timeout, String... parts) throws Exception {
        final String pod ;
        final long endTime = System.currentTimeMillis() + timeout;
        if (checkIfReady) {
            while(true) {
                final Set<String> readyPods = adapter.getReadyPods(podPrefix);
                if ((readyPods != null) && (readyPods.size() > 0)) {
                    pod = readyPods.iterator().next();
                    break ;
                }
                if (System.currentTimeMillis() > endTime) {
                    return -1;
                } else {
                    TimeUnit.SECONDS.sleep(1);
                }
            }
        } else {
            final List<String> pods = adapter.getPods(podPrefix);
            pod = ((pods != null && !pods.isEmpty())) ? pods.get(0) : null;
        }

        if (pod == null) {
            return -1;
        }
        final int maxPartLength = maxLength(parts);

        while(true) {
            // It would be more efficient to stream the log however that doesn't work, no information is returned.
            final String drainLog = adapter.getLog("amq-test-drainer", null);

            if (drainLog != null) {
                final int drainLogLength = drainLog.length();
                if (drainLogLength > startIndex) {
                    for (String content : parts) {
                        final int logIndex = drainLog.indexOf(content, startIndex);
                        if (logIndex != -1) {
                            return logIndex + content.length();
                        }
                    }
                    startIndex = drainLogLength-maxPartLength-1;
                    if (startIndex < 0) {
                        startIndex = 0;
                    }
                }
            }
            if (System.currentTimeMillis() > endTime) {
                return -1;
            } else {
                TimeUnit.SECONDS.sleep(1);
            }
        }
    }

    private static int maxLength(final String ... parts) {
        int maxLength = 0;
        for (int index = parts.length -1 ; index >= 0 ; index--) {
            final int length = (parts[index] == null ? 0 : parts[index].length());
            if (length > maxLength) {
                maxLength = length;
            }
        }
        return maxLength;
    }

    protected void sendNMessages(int from, int to) throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");
        List<String> msgs = new ArrayList<>();
        for (int i = from; i < to; i++) {
            msgs.add("msg" + i);
        }
        client.produceOpenWireJms(msgs, false);
    }

    protected List<String> consumeMsgs(int msgsSize) throws Exception {
        AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");

        List<String> msgs = new ArrayList<>();
        client.consumeOpenWireJms(msgs, msgsSize, false);

        Set<Integer> msgNumbers = new TreeSet<>();
        for (String msg : msgs) {
            msgNumbers.add(Integer.parseInt(msg.substring(3)));
        }
        Assert.assertEquals("Missing msgs: " + msgNumbers, msgsSize, msgNumbers.size()); // make sure we have all msgs

        Assert.assertEquals("Extra msgs found", client.consumeOpenWireJms(2000, false), null); // make sure we have all msgs

        return msgs;
    }
}

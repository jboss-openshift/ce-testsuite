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

import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fspolti on 5/12/17.
 */
public class AmqDrainerTestBase extends AmqMigrationTestBase{


    private static final Logger log = Logger.getLogger(AmqDrainerTestBase.class.getName());
    private static final int MSGS_SIZE = 2_000;
    private static final String MIGRATING = "Processing queue: 'QUEUES.FOO'";
    private static final String STATS = "Processing stats: 'QUEUES.FOO' -> \\[([0-9]+) / ([0-9]+)\\]";
    private static final Pattern STATS_PATTERN = Pattern.compile(STATS);


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
        for (int i = 1; i <= MSGS_SIZE; i++) {
            sendNMessages(i, i + 1);
        }
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void testDrainer(@ArquillianResource final OpenShiftHandle adapter) throws Exception {
        List<String> pods = adapter.getPods("amq-test-amq");
        Assert.assertEquals(2, pods.size());

        List<String> drainer = adapter.getPods("amq-test-drainer");
        Assert.assertEquals(1, drainer.size());
        final String drainerPod = drainer.get(0);
        new Thread(new Runnable() {
            public void run() {
                boolean deleted = false;
                try (InputStream stream = adapter.streamLog(drainerPod)) {
                    StringBuilder buffer = new StringBuilder();
                    int ch;
                    while ((ch = stream.read()) != -1) {
                        buffer.append((char) (ch & 0xFF));

                        if (!deleted && buffer.indexOf(MIGRATING) >= 0) {
                            adapter.deletePod(drainerPod, -1);
                            deleted = true;
                        }
                        Matcher matcher = STATS_PATTERN.matcher(buffer);
                        if (matcher.find()) {
                            log.info(String.format("Processing stats -> %s / %s.", matcher.group(1), matcher.group(2)));
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }).start();

        adapter.scaleDeployment("amq-test-amq", 1); // scale down

        // drain should kick-in in any case
        waitForDrain(adapter, 0, true, END);
    }

    @Test
    @InSequence(4)
    public void testConsumeMsgs() throws Exception {
        consumeMsgs(MSGS_SIZE);
    }
}
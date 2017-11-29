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

import org.arquillian.cube.openshift.api.OpenShiftHandle;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by fspolti on 5/12/17.
 */
public class AmqQueueMigrationTestBase extends AmqMigrationTestBase {
    private static final int MSGS_SIZE = 2000;

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
        for (int i = 1; i <= MSGS_SIZE; i++) {
            sendNMessages(i, i + 1);
        }
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void testScaleDown(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        adapter.scaleDeployment("amq-test-amq", 1); // scale down
        adapter.waitForReadyPods("amq-test-amq", 1);

        // drain should kick-in in any case
        Assert.assertNotEquals("Migration should have finished", -1, waitForDrain(adapter, 0, true, END));
    }

    @Test
    @InSequence(4)
    public void testConsumeMsgs() throws Exception {
        consumeMsgs(MSGS_SIZE);
    }
}
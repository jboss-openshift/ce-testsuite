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

import java.util.List;

import org.arquillian.cube.openshift.api.OpenShiftHandle;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by fspolti on 5/12/17.
 */
public class AmqRollingUpgradeTestBase extends AmqMigrationTestBase {

    private static final int N = 5;
    private static final int TIMEOUT = 60000;

    @Test
    @RunAsClient
    @InSequence(1)
    public void testScaleUp(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        adapter.scaleDeployment("amq-test-amq", N);
        adapter.waitForReadyPods("amq-test-amq", N);
    }

    @Test
    @InSequence(2)
    public void testSendMsgs() throws Exception {
        sendNMessages(1, 11); // 10 msgs
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void testRollingUpdate(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        final List<String> origPods = adapter.getPods("amq-test-amq");

        adapter.triggerDeploymentConfigUpdate("amq-test-amq", true);

        final long endTime = System.currentTimeMillis() + TIMEOUT;

        boolean upgradeFinished = false;
        while(!upgradeFinished && (System.currentTimeMillis() < endTime)) {
            final List<String> pods = adapter.getPods("amq-test-amq");
            pods.removeAll(origPods);
            if (pods.size() == N) {
                upgradeFinished = true;
            } else {
                Thread.sleep(1000);
            }
        }

        Assert.assertTrue("Rolling Upgrade did not finish", upgradeFinished);
    }

    @Test
    @InSequence(4)
    public void testConsumeMsgs() throws Exception {
        consumeMsgs(10);
    }
}

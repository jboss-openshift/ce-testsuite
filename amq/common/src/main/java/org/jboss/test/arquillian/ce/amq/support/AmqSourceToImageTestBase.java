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

import org.arquillian.cube.openshift.api.OpenShiftHandle;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

/**
 * Created by fspolti on 5/12/17.
 */
public class AmqSourceToImageTestBase extends AmqBase {

    private static final Pattern NAME_REGEXP = Pattern.compile("(broker-amq-)[0-9]+(-build)");

    @Test
    @RunAsClient
    public void testCustomConfiguration(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        // wait for the broker-amq pod get ready, sometimes the tests finish before the s2i build gets ready, leading the
        // test to fail.
        adapter.waitForReadyPods("broker-amq", 1);
        Assert.assertEquals("There is no enough replicas of broker-amq.",1, adapter.getReadyPods("broker-amq").size());

        String amqPod = "";
        for (String podName : adapter.getPods()) {
            if (NAME_REGEXP.matcher(podName).matches()) {
                amqPod = podName;
                break;
            }
        }

        boolean added = adapter.getLog(amqPod, null).contains("hello.xml");
        Assert.assertTrue("File hello.xml was not added in the A-MQ pod", added);
    }
}

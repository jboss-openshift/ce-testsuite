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

package org.jboss.test.ce.testsuite.cluster.http;

import java.io.InputStream;

import org.jboss.arquillian.ce.api.Client;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.ce.testsuite.cluster.http.support.ResponseFilter;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
public class HttpSessionTest {

    @ArquillianResource
    Client client;

    @Deployment
    public static WebArchive getDeployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        war.setWebXML("web.xml");
        war.addClass(ResponseFilter.class);
        return war;
    }

    @Test
    @TargetsContainer("node1")
    public void configure2nodes() {
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void testFirstNode() throws Exception {
        InputStream response = client.execute(0, "test/foo");
        response.close();
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void testSecondNode() throws Exception {
        InputStream response = client.execute(1, "test/foo");
        response.close();
    }

}

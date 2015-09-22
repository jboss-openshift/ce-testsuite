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

package org.jboss.test.ce.testsuite.common;

import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SimpleUrlTest {
    private static Logger log = Logger.getLogger(SimpleUrlTest.class.getName());

    @Deployment
    public static WebArchive getDeployment() throws Exception {
        return ShrinkWrap.create(WebArchive.class, "test.war");
    }

    @Test
    public void testBasic(@ArquillianResource URL baseURL) throws Exception {
        log.info("Injected URL: " + baseURL);
        String response = "";
        try (InputStream stream = new URL(baseURL + "/_poke").openStream()) {
            int b;
            while ((b = stream.read()) != -1) {
                response += ((char) b);
            }
        }
        log.info("Poke response: " + response);
        Assert.assertEquals("OK", response);
    }
}

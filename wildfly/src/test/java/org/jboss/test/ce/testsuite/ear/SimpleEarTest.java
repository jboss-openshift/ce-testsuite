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

package org.jboss.test.ce.testsuite.ear;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.ce.testsuite.ear.support.HelloServlet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
public class SimpleEarTest {

    @Deployment
    public static Archive getDeployment() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear");
        ear.setApplicationXML("application.xml");

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "web1.war");
        ear.addAsModule(war1);
        war1.addClass(HelloServlet.class);
        war1.setWebXML("web.xml");


        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "web2.war");
        ear.addAsModule(war2);
        war2.addClass(HelloServlet.class);
        war2.setWebXML("web.xml");

        return ear;
    }

    @Test
    @RunAsClient
    public void testModules(@ArquillianResource URL baseUrl) throws Exception {
        Assert.assertEquals("Hello JBoss!", getResponse(new URL(baseUrl + "web1/hello?user=JBoss")));
        Assert.assertEquals("Hello RedHat!", getResponse(new URL(baseUrl + "web2/hello?user=RedHat")));
    }

    private static String getResponse(URL url) throws IOException {
        String response = "";
        try (InputStream stream = url.openStream()) {
            int b;
            while ((b = stream.read()) != -1) {
                response += ((char) b);
            }
        }
        return response;
    }
}

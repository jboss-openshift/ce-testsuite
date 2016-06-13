package org.jboss.test.arquillian.ce.spark;
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

import org.jboss.arquillian.ce.api.*;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

import java.net.URL;
import java.util.logging.Logger;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template(
        url="https://gist.githubusercontent.com/jayunit100/3fad23e42a22e473fff7a45947bd3102/raw/ccedbb3020d98267753ba758c9f739d587b3aae5/xpass-spark",
        labels = "app=spark",
        parameters = {
                @TemplateParameter(name = "MASTER_NAME", value="jspark"),
        }
)
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:jdg-service-account")
public class SparkTest {
    private static final Logger log = Logger.getLogger("spark-smoke");

    // New arquillian developer note...
    // To confirm that this test is working as expected, modify this to "201" and a failure will result in test failure.
    // Thus, you can confirm for yourself that assertions in the container are cascading into the overall build result.
    public static int WEB_UI_EXPECT=200;
    
    static{
        System.out.println("Web ui == " + WEB_UI_EXPECT);
    }
    @ArquillianResource
    ConfigurationHandle configuration;

    @Deployment
    public static WebArchive getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML(new StringAsset("<web-app/>"));
	war.addPackage(org.jboss.arquillian.test.impl.EventTestRunnerAdaptor.class.getPackage());
	 war.addPackage(Arquillian.class.getPackage());     
   war.addPackage(SparkTest.class.getPackage());
        return war;
    }

    @Test
    @InSequence(1)
    public void testSparkWebUI() throws Exception {
        URL u = new URL("http://jspark-webui:8080");
        HttpURLConnection huc =  (HttpURLConnection)  u.openConnection();
        System.out.println(huc.getResponseCode());
        assertEquals(WEB_UI_EXPECT,huc.getResponseCode());
    }
}

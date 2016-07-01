package org.jboss.test.arquillian.ce.jdg;
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

import junit.framework.Assert;
import org.apache.hadoop.security.SaslRpcServer;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.spark.SparkConf;
import org.apache.spark.ThrowableSerializationWrapper;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.util.ThreadStackTrace;
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
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
	url = "https://gist.githubusercontent.com/jayunit100/c2e29f30432d346bb8a9fddb6e8d2012/raw/35952fad20ac34b8ee8a1aac80c81ba6c91b2eb7/xpass-spark.yaml",
	labels = "app=spark",
        parameters = {
                @TemplateParameter(name = "MASTER_NAME", value="jspark")
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
        war.setWebXML(new StringAsset("<web-app><context-param><param-name>resteasy.scan.providers</param-name><param-value>false</param-value></context-param></web-app>"));
        
	
	war.addPackage(org.jboss.arquillian.test.impl.EventTestRunnerAdaptor.class.getPackage());
        war.addPackage(Arquillian.class.getPackage());
        

	/**    Uncomment this if attempting to run spark arquillian unit test w/ JavaSparkContext.
	       these deps were carefully pruned, and are mostly what are required to get things working.

		war.addPackage(org.apache.spark.internal.Logging.class.getPackage());
		war.addPackage(org.apache.spark.api.java.function.Function.class.getPackage());
		war.addPackage(org.apache.spark.SparkConf.class.getPackage());
		war.addPackage(scala.Cloneable.class.getPackage());
		war.addPackage(SparkTest.class.getPackage());

		//catch all because im bored of finding new ones...
		war.addPackages(true,"org.apache.*");
		war.addPackages(true,"org.apache.spark");
		war.addPackages(true,"org.apache.commons");
		war.addPackages(true,"org.apache.hadoop");
		war.addPackages(true,"scala");
		war.addPackages(true,"org.slf4j");
		war.addPackages(true,"org.spark_project.guava");
		war.addPackages(true,"com.google.common");
		war.addPackages(true,"io.netty");
		war.addPackages(true,"com.esotericsoftware");
		war.addPackages(true,"com.twitter");
		war.addPackages(true,"com.codahale");
		war.addPackages(true,"org.json4s");
		war.addPackages(true,"org.spark_project.jetty");
	war.addPackages(true,"org.apache.spark.static");

		war.addAsResource(org.apache.spark.ui.WebUI.class.getPackage(),"static/additional-metrics.js");
		
		// leads to resteasy conflict: alternative, disable spark webui.
		// war.addPackages(true, "org.glassfish.jersey.server");
	
	**/ 
	return war;
    }
 
    @Test
    @InSequence(1)
    public void testSmoke() throws Exception {
	// In addition to the containers spinning up, asser that the web ui spins up.
	URL u = new URL("http://jspark-webui:8080");
	HttpURLConnection huc =  (HttpURLConnection)  u.openConnection();
 	assertEquals(WEB_UI_EXPECT,huc.getResponseCode());
    }

    /**
    *  This test doesnt completely work.
    *  However, it represents a "customer like" workflow for jboss, in that it natively calls spark from java.
    *  So, we should refine it over time and get it working (requires jboss dependency wrangling).
     */
    @org.junit.Ignore
    @Test
    public void testSparkInJBossContainer() throws Exception {

         // TODO put this in a separate test.
         URL u = new URL("http://jspark-webui:8080");
         HttpURLConnection huc =  (HttpURLConnection)  u.openConnection();
         System.out.println(huc.getResponseCode());
         assertEquals(WEB_UI_EXPECT,huc.getResponseCode());

        // Setup nss stuffs. otherwise spark wont have  a user name and will throw a null exception .
        String[] cmd = new String[]{"/bin/bash",
                "export SPARK_USER=jayunit100",
        };
        Process pr = Runtime.getRuntime().exec(cmd);

        for(String key : System.getenv().keySet()){
            System.out.println("EMV =---------- " + key);
        }
        System.out.println("Done w nss setup!");


        // now run the test.
        SparkConf conf = new SparkConf().setMaster("spark://jspark:9090").setAppName("spark-test");

	conf.set("spark.ui.enabled","false");

	JavaSparkContext sc = null;

	try { 
        	sc = new JavaSparkContext(conf);
	} catch (Throwable t) {
		throw new Exception("Got here, creating context, threw "+t.getMessage());
	}	

	List<String> uniqueHosts = null;
	try{
        		uniqueHosts = sc.parallelize(new ArrayList<Integer>(100)).map(
                	new Function<Integer, String>() {
             	       		@Override
             	       		public String call(Integer v1) throws Exception {
                	        	return "hostname is ......"; 
					// return InetAddress.getLocalHost().getHostName();
                		}
                	}).distinct().collect();
			try { 
				for (String s : uniqueHosts){
				    System.out.println("HOST !!! "+s);
		    		    Thread.sleep(1);
				}
			}
 			catch(Throwable t){
				throw new Exception("dies while printing hosts and sleeping!");
			}
       } 
	catch(Throwable t){
		throw new Exception("dies due to a weird exception : "+t);
	}
        // Assert at least 2 unique hosts collected.
        Assert.assertTrue("Asserting that unique hosts (" +uniqueHosts +") size "+uniqueHosts.size() +" Is greater than 2",uniqueHosts.size() > 2);
    }

}

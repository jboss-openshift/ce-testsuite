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

package org.jboss.test.arquillian.ce.amq;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.jboss.arquillian.ce.api.ConfigurationHandle;
import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.api.Tools;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.amq.support.AmqClient;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/amq/amq62-basic.json", parameters = {
		@TemplateParameter(name = "APPLICATION_NAME", value = "amq-test"),
		@TemplateParameter(name = "MQ_QUEUES", value = "QUEUES.FOO"),
		@TemplateParameter(name = "MQ_USERNAME", value = "${amq.username:amq-test}"),
		@TemplateParameter(name = "MQ_PASSWORD", value = "${amq.password:redhat}"), })
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default")
@OpenShiftResources({
	@OpenShiftResource("classpath:testrunner-secret.json")
})
public class AmqMeshTest extends AmqTestBase {

	private static final Logger log = Logger.getLogger(AmqMeshTest.class.getName());

	static final String FILENAME = "amq.properties";
	static final String USERNAME = System.getProperty("amq.username", "amq-test");
	static final String PASSWORD = System.getProperty("amq.password", "redhat");

	static final String BASE_REST_URL = "https://ce-os-rhel-master.usersys.redhat.com:8443/api/v1";

	static List<String> pods = new ArrayList<>();

	@ArquillianResource
	ConfigurationHandle handler;
	
	@Deployment
	public static WebArchive getDeployment() throws IOException {
		WebArchive war = getDeploymentBase();
		
		return war;
	}

	@Test
	@RunAsClient
	@InSequence(1)
	public void scaleUpResources(@ArquillianResource OpenShiftHandle handler) throws Exception {
		handler.scaleDeployment("amq-test", 2);
		pods.addAll(handler.getPods());
		
		log.info("Pods used for test: " + pods.toString());
	}

	@Test
	@InSequence(2)
	public void sendMessages() throws Exception {
		for (int i = 0; i < 40; i++) {
			AmqClient client = createAmqClient("tcp://" + System.getenv("AMQ_TEST_AMQ_TCP_SERVICE_HOST") + ":61616");

			client.produceOpenWireJms("Hello! I sent this message " + i + " times.", false);
		}
	}

	@Test
	@RunAsClient
	@InSequence(3)
	public void checkMessages() throws Exception {
		Tools.trustAllCertificates();

		for (String podName : pods) {
			if(!podName.equals("testrunner")) {
				final String queueSizeQuery = "org.apache.activemq:type=Broker,brokerName=" + podName
						+ ",destinationType=Queue,destinationName=QUEUES.FOO/QueueSize";
				
				URL url = new URL(BASE_REST_URL + "/namespaces/rmartinelli/pods/https:" + podName
						+ ":8778/proxy/jolokia/read/" + queueSizeQuery);
						
				HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
	
				String basicAuth = "Bearer " + handler.getToken();
				log.info("Header >>" + basicAuth);
				con.setHostnameVerifier(new HostnameVerifier() {
					@Override
		            public boolean verify(String s, SSLSession sslSession) {
		                return true;
		            }
		        });
				con.setRequestProperty ("Authorization", basicAuth);
				
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String jsonString = br.readLine();
				log.info(jsonString);
				assertEquals(20, new JSONObject(jsonString).get("value"));
			}
		}
	}

}

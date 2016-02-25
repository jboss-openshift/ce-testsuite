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

import java.io.IOException;

import org.jboss.arquillian.ce.api.ConfigurationHandle;
import org.jboss.arquillian.ce.api.ExternalDeployment;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.RunInPod;
import org.jboss.arquillian.ce.api.RunInPodDeployment;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.shrinkwrap.Files;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.amq.support.AmqClient;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunInPod
@ExternalDeployment
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/amq/amq62-basic.json",
labels = "application=amq-test",
parameters = {
		@TemplateParameter(name = "MQ_USERNAME", value = "ce"),
		@TemplateParameter(name = "MQ_PASSWORD", value = "ce"),
		@TemplateParameter(name = "MQ_QUEUES", value = "QUEUES.FOO,QUEUES.BAR"),
		@TemplateParameter(name = "MQ_TOPICS", value = "TOPICS.FOO,TOPICS.BAR"),
		@TemplateParameter(name = "APPLICATION_NAME", value = "amq-test"),
		@TemplateParameter(name = "MQ_USERNAME", value = "${amq.username:amq-test}"),
		@TemplateParameter(name = "MQ_PASSWORD", value = "${amq.password:redhat}"),
		@TemplateParameter(name = "MQ_PROTOCOL", value = "openwire,amqp,mqtt,stomp"),
		@TemplateParameter(name = "IMAGE_STREAM_NAMESPACE", value="${kubernetes.namespace}")})
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default")
@OpenShiftResources({
@OpenShiftResource("classpath:amq-internal-imagestream.json")
})
public class AmqSecuredTest {
	
static final String FILENAME = "amq.properties";
	
	static final String USERNAME = System.getProperty("amq.username", "amq-test");
	static final String PASSWORD = System.getProperty("amq.password", "redhat");
			
	@ArquillianResource
	ConfigurationHandle configuration;
	
	@Deployment
	@RunInPodDeployment
	public static WebArchive getDeployment() throws IOException {
		WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
		war.setWebXML(new StringAsset("<web-app/>"));
		war.addPackage(AmqClient.class.getPackage());
		
		war.addAsLibraries(Libraries.transitive("org.apache.activemq", "activemq-client"));
		war.addAsLibraries(Libraries.transitive("org.fusesource.mqtt-client", "mqtt-client"));
		war.addAsLibraries(Libraries.transitive("org.fusesource.stompjms", "stompjms-client"));
		
		Files.PropertiesHandle handle = Files.createPropertiesHandle(FILENAME);
        handle.addProperty("amq.username", USERNAME);
        handle.addProperty("amq.password", PASSWORD);
        handle.store(war);
		
		return war;
	}
	
	public void testSecuredOpenwire() {
		
	}
	
	public void testSecuredMqtt() {
		
	}
	
	public void testSecuredStomp() {
		
	}
	
	public void testSecuredAmqp() {
		
	}

}

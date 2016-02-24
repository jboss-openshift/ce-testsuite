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

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.FutureConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.jboss.arquillian.ce.api.Tools;
import org.jboss.test.arquillian.ce.amq.AmqTest;

public class AmqClient {

	String connectionUrl;
	private String username;
	private String password;

	public AmqClient(String connectionUrl, String propertiesFile) throws IOException {
		this.connectionUrl = connectionUrl;
		Properties properties = Tools.loadProperties(AmqTest.class, propertiesFile);
		username = properties.getProperty("amq.username");
		password = properties.getProperty("amq.password");
	}

	public String consumeOpenWireJms() throws JMSException {
		Connection conn = null;
		try {
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(username, password, this.connectionUrl);
			conn = cf.createConnection();
			Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination qFoo = session.createQueue("QUEUES.FOO");

			MessageConsumer consumer = session.createConsumer(qFoo);

			conn.start();

			Message msg = consumer.receive();
			return ((TextMessage) msg).getText();
		} finally {
			conn.close();
		}
	}

	public void produceOpenWireJms(String message) throws JMSException {
		Connection conn = null;
		try {
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(username, password, this.connectionUrl);
			conn = cf.createConnection();
			Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination qFoo = session.createQueue("QUEUES.FOO");

			MessageProducer producer = session.createProducer(qFoo);

			conn.start();

			Message msg = session.createTextMessage(message);
			producer.send(msg);
		} finally {
			conn.close();
		}
	}

	public String consumeAmqp() throws JMSException, NamingException {
		Connection connection = null;

		Properties props = new Properties();
		props.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
		props.put("connectionfactory.arquillianConnectionFactory", connectionUrl);
		props.put("queue.foo", "foo");

		Context context = new InitialContext(props);
		ConnectionFactory factory = (ConnectionFactory) context.lookup("arquillianConnectionFactory");
		Destination destination = (Destination) context.lookup("foo");

		connection = factory.createConnection(username, password);
		connection.start();

		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		
		MessageConsumer consumer = session.createConsumer(destination);

		connection.start();

		Message msg = consumer.receive();
		return ((TextMessage) msg).getText();
	}

	public void produceAmqp(String message) throws JMSException, NamingException {
		Connection connection = null;

		Properties props = new Properties();
		props.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
		props.put("connectionfactory.arquillianConnectionFactory", connectionUrl);
		props.put("queue.foo", "foo");

		Context context = new InitialContext(props);
		ConnectionFactory factory = (ConnectionFactory) context.lookup("arquillianConnectionFactory");
		Destination destination = (Destination) context.lookup("foo");

		connection = factory.createConnection(username, password);
		connection.start();

		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		MessageProducer producer = session.createProducer(destination);

		TextMessage msg = session.createTextMessage(message);
		producer.send(msg);

		producer.close();
		session.close();
		connection.close();
	}

	public String consumeStomp() throws Exception {
		StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
		factory.setBrokerURI(connectionUrl);
		Connection conn = factory.createConnection(username, password);
		conn.start();

		Session session = conn.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
		Queue q = session.createQueue("QUEUES.FOO");
		MessageConsumer consumer = session.createConsumer(q);
		String received = ((TextMessage) consumer.receive()).getText();
		conn.close();
		
		return received;
	}

	public void produceStomp(String message) throws Exception {
		StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
		factory.setBrokerURI(connectionUrl);
		Connection conn = factory.createConnection(username, password);
		conn.start();

		Session session = conn.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

		Queue q = session.createQueue("QUEUES.FOO");
		MessageProducer producer = session.createProducer(q);

		producer.send(session.createTextMessage(message));

		conn.close();
	}

}

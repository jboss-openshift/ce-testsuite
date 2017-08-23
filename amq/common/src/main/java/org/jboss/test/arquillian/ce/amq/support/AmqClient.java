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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
import javax.jms.Topic;
import javax.naming.NamingException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.transports.TransportSslOptions;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;

/**
 * @author Ricardo Martinelli
 */
public class AmqClient {
    private static final Logger log = Logger.getLogger(AmqClient.class.getName());

    private final String connectionUrl;
    private final String username;
    private final String password;

    private static final long MSG_TIMEOUT = 60000;

    public AmqClient(String connectionUrl, String username, String password) throws IOException {
        this.connectionUrl = connectionUrl;
        this.username = username;
        this.password = password;
    }

    private ConnectionFactory getAMQConnectionFactory(boolean secured) throws Exception {
        if (secured) {
            ActiveMQSslConnectionFactory cf = new ActiveMQSslConnectionFactory(this.connectionUrl);
            cf.setTrustStore(System.getProperty("javax.net.ssl.trustStore"));
            cf.setTrustStorePassword(System.getProperty("javax.net.ssl.trustStorePassword"));

            return cf;
        }
        return new ActiveMQConnectionFactory(this.connectionUrl);
    }

    private void close(Connection connection) throws JMSException {
        if (connection != null) {
            connection.close();
        }
    }

    public String consumeOpenWireJms(boolean isSecured) throws Exception {
        return consumeOpenWireJms(0, isSecured);
    }

    public String consumeOpenWireJms(long timeout, boolean isSecured) throws Exception {
        Connection conn = null;
        try {
            ConnectionFactory cf = getAMQConnectionFactory(isSecured);
            conn = cf.createConnection(username, password);
            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination qFoo = session.createQueue("QUEUES.FOO");

            MessageConsumer consumer = session.createConsumer(qFoo);

            conn.start();

            Message msg = consumer.receive(timeout);
            if (msg != null) {
                String text = ((TextMessage) msg).getText();
                log.warning("Msg: " + text);
                return text;
            } else {
                log.warning("No msgs ...");
                return null;
            }
        } finally {
            close(conn);
        }
    }

    public void consumeOpenWireJms(List<String> msgs, int size, boolean isSecured) throws Exception {
        Connection conn = null;
        try {
            ConnectionFactory cf = getAMQConnectionFactory(isSecured);
            conn = cf.createConnection(username, password);
            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination qFoo = session.createQueue("QUEUES.FOO");

            MessageConsumer consumer = session.createConsumer(qFoo);

            conn.start();

            log.warning("Starting open-wire-consume ...");
            while (size > 0) {
                Message msg = consumer.receive(MSG_TIMEOUT);
                if (msg == null) {
                    throw new IllegalStateException("Missing " + size + " messages.");
                }
                msgs.add(((TextMessage) msg).getText());
                size--;
            }
        } finally {
            close(conn);
        }
    }

    public void produceOpenWireJms(String message, boolean isSecured) throws Exception {
        Connection conn = null;
        try {
            ConnectionFactory cf = getAMQConnectionFactory(isSecured);
            conn = cf.createConnection(username, password);
            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination qFoo = session.createQueue("QUEUES.FOO");

            MessageProducer producer = session.createProducer(qFoo);

            conn.start();

            Message msg = session.createTextMessage(message);
            producer.send(msg);
        } finally {
            close(conn);
        }
    }

    public void produceOpenWireJms(List<String> msgs, boolean isSecured) throws Exception {
        Connection conn = null;
        try {
            ConnectionFactory cf = getAMQConnectionFactory(isSecured);
            conn = cf.createConnection(username, password);
            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination qFoo = session.createQueue("QUEUES.FOO");

            MessageProducer producer = session.createProducer(qFoo);

            conn.start();

            for (String message : msgs) {
                Message msg = session.createTextMessage(message);
                producer.send(msg);
            }
        } finally {
            close(conn);
        }
    }

    public String consumeAmqp() throws JMSException, NamingException {
        TransportSslOptions options = new TransportSslOptions();
        options.setTrustStoreLocation(System.getProperty("javax.net.ssl.trustStore"));
        options.setTrustStorePassword("password");
        options.setVerifyHost(false);
        ConnectionFactory factory = new JmsConnectionFactory(connectionUrl);

        Connection connection = factory.createConnection(username, password);
        try {
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Queue q = session.createQueue("QUEUES.BAR");
            MessageConsumer consumer = session.createConsumer(q);

            Message msg = consumer.receive(MSG_TIMEOUT);
            if (msg == null) {
                throw new IllegalStateException("No messages.");
            }

            return ((TextMessage) msg).getText();
        } finally {
            close(connection);
        }
    }

    public void produceAmqp(String message) throws JMSException, NamingException {
        ConnectionFactory factory = new JmsConnectionFactory(connectionUrl);

        Connection conn = factory.createConnection(username, password);
        try {
            conn.start();

            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Queue q = session.createQueue("QUEUES.BAR");
            MessageProducer producer = session.createProducer(q);

            TextMessage msg = session.createTextMessage(message);
            producer.send(msg);

            producer.close();
            session.close();
        } finally {
            close(conn);
        }
    }

    public String consumeStomp() throws Exception {
        StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
        factory.setBrokerURI(connectionUrl);
        Connection conn = factory.createConnection(username, password);
        try {
            conn.start();

            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue q = session.createQueue("QUEUES.FOO");

            MessageConsumer consumer = session.createConsumer(q);

            TextMessage msg = (TextMessage)consumer.receive(MSG_TIMEOUT);
            if (msg == null) {
                throw new IllegalStateException("No messages.");
            }
            return msg.getText();
        } finally {
            close(conn);
        }
    }

    public void produceStomp(String message) throws Exception {
        StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
        factory.setBrokerURI(connectionUrl);
        Connection conn = factory.createConnection(username, password);
        try {
            conn.start();

            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue q = session.createQueue("QUEUES.FOO");

            MessageProducer producer = session.createProducer(q);

            producer.send(session.createTextMessage(message));
        } finally {
            close(conn);
        }
    }

    public void createVirtualTopicSubscriber(String subscriptionName) throws Exception {
        Connection conn = null;
        try {
            ConnectionFactory cf = getAMQConnectionFactory(false);
            conn = cf.createConnection(username, password);
            conn.setClientID("tmp123");

            conn.start();

            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination tFoo = session.createQueue("Consumer." + subscriptionName + ".VirtualTopic.FOO");

            // This may need to remain open
            MessageConsumer consumer = session.createConsumer(tFoo);
            consumer.close();
        } finally {
            close(conn);
        }
    }

    public void produceVirtualTopic(String message) throws Exception {
        Connection conn = null;
        try {
            ConnectionFactory cf = getAMQConnectionFactory(false);
            conn = cf.createConnection(username, password);

            conn.start();

            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic tFoo = session.createTopic("VirtualTopic.FOO");
            MessageProducer producer = session.createProducer(tFoo);
            producer.send(session.createTextMessage(message));
        } finally {
            close(conn);
        }
    }

    public List<String> consumeVirtualTopic(int N, long timeout, String subscriptionName) throws Exception {
        Connection conn = null;
        try {
            ConnectionFactory cf = getAMQConnectionFactory(false);
            conn = cf.createConnection(username, password);
            conn.setClientID("tmp123");

            conn.start();

            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination tFoo = session.createQueue("Consumer." + subscriptionName + ".VirtualTopic.FOO");

            MessageConsumer consumer = session.createConsumer(tFoo);
            List<String> msgs = new ArrayList<>();
            while (N > 0) {
                TextMessage msg = (TextMessage)consumer.receive(timeout);
                if (msg == null) {
                    throw new IllegalStateException("Missing " + N + " messages.");
                }
                msgs.add(msg.getText());
                N--;
            }
            return msgs;
        } finally {
            close(conn);
        }
    }
}

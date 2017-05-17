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

import org.apache.activemq.broker.SslContext;
import org.apache.qpid.jms.transports.TransportSslOptions;
import org.apache.qpid.jms.transports.TransportSupport;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class AmqSslTestBase extends AmqBase {

    static Logger LOG = Logger.getLogger(AmqSslTestBase.class.getName());

    private SslContext brokerContext;

    protected static WebArchive getSslDeploymentBase() throws IOException {
        WebArchive war = getDeploymentBase(AmqSslTestBase.class);
        war.addClass(AmqPersistentSecuredTestBase.class);
        war.addClass(AmqSecuredTestBase.class);
        war.addAsLibraries(Libraries.transitive("org.apache.activemq", "activemq-broker"));
        return war;
    }

    @Before
    public void setUp() throws Exception {
        TransportSslOptions sslOptions = createTransportSslOptions();

        SSLContext ssl = TransportSupport.createSslContext(sslOptions);
        SSLContext.setDefault(ssl);
        createBrokerContext(ssl);
    }

    public TransportSslOptions createTransportSslOptions() {
        TransportSslOptions sslOptions = new TransportSslOptions();
        sslOptions.setTrustAll(true);
        sslOptions.setVerifyHost(false);
        return sslOptions;
    }

    public SslContext createBrokerContext(SSLContext ssl) {
        brokerContext = new SslContext();
        brokerContext.setSSLContext(ssl);
        return brokerContext;
    }

    public SSLContext getSSLContext() throws Exception {
        return brokerContext.getSSLContext();
    }
}
package org.jboss.test.arquillian.ce.amq;

import java.io.IOException;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.activemq.broker.SslContext;
import org.apache.qpid.jms.transports.TransportSslOptions;
import org.apache.qpid.jms.transports.TransportSupport;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;

public class AmqSslTestBase extends AmqTestBase {
	
	static Logger LOG = Logger.getLogger(AmqSslTestBase.class.getName());

    private SslContext brokerContext;

    protected static WebArchive getSslDeploymentBase() throws IOException {
        WebArchive war = getDeploymentBase(AmqSslTestBase.class);
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

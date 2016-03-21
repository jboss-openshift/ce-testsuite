package org.jboss.test.arquillian.ce.amq.support;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.net.ssl.SSLContext;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.SslContext;
import org.apache.qpid.jms.transports.TransportSslOptions;

public class AmqSslTestSupport {
	
	static {
		System.setProperty("javax.net.ssl.trustStore", "/opt/eap/certs/broker.ts");
		System.setProperty("javax.net.ssl.trustStorePassword", "password");
		System.setProperty("javax.net.ssl.keyStore", "/opt/eap/certs/broker.ks");
		System.setProperty("javax.net.ssl.keyStorePassword", "password");
	}
	
	private SslContext brokerContext;
	
	public BrokerService createBroker() {
		BrokerService brokerService = new BrokerService();
		brokerService.setPersistent(false);
		brokerService.setAdvisorySupport(false);
		brokerService.setDeleteAllMessagesOnStartup(true);
		brokerService.setUseJmx(false);
		return brokerService;
	}

	public TransportSslOptions createTransportSslOptions() {
		TransportSslOptions sslOptions = new TransportSslOptions();
		sslOptions.setVerifyHost(false);
		return sslOptions;
	}

	public SslContext createBrokerContext(SSLContext ssl) {
		brokerContext = new SslContext();
		brokerContext.setSSLContext(ssl);
		return brokerContext;
	}
	
	public SSLContext getSSLContext() throws KeyManagementException, NoSuchProviderException, NoSuchAlgorithmException {
		return brokerContext.getSSLContext();
	}

}

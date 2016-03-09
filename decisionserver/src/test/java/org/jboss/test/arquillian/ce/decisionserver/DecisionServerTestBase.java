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

package org.jboss.test.arquillian.ce.decisionserver;

import io.fabric8.utils.Base64Encoder;

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.ce.api.ConfigurationHandle;
import org.jboss.arquillian.ce.shrinkwrap.Files;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.kie.internal.command.CommandFactory;
import org.kie.internal.runtime.helper.BatchExecutionHelper;
import org.kie.server.api.marshalling.Marshaller;
import org.kie.server.api.marshalling.MarshallerFactory;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;
import org.openshift.quickstarts.decisionserver.hellorules.Greeting;
import org.openshift.quickstarts.decisionserver.hellorules.Person;


/**
 * @author Filippe Spolti
 * @author Ales justin
 */
public abstract class DecisionServerTestBase {
    protected final Logger log = Logger.getLogger(getClass().getName());

    protected static final String FILENAME = "kie.properties";

    //kie-server credentials
    protected static final String KIE_USERNAME = System.getProperty("kie.username", "kieserver");
    protected static final String KIE_PASSWORD = System.getProperty("kie.password", "Redhat@123");
    // AMQ credentials
    public static final String MQ_USERNAME = System.getProperty("mq.username", "kieserver");
    public static final String MQ_PASSWORD = System.getProperty("mq.password", "Redhat@123");

    public String AMQ_HOST = "tcp://kie-app-amq-tcp:61616";
    public Person person = new Person();

    @ArquillianResource
    protected ConfigurationHandle configuration;

    protected static WebArchive getDeploymentInternal() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");

        war.setWebXML("web.xml");
        war.addClass(DecisionServerTestBase.class);
        war.addPackage(Person.class.getPackage());
        war.addAsLibraries(Libraries.transitive("org.kie.server", "kie-server-client"));

        Files.PropertiesHandle handle = Files.createPropertiesHandle(FILENAME);
        handle.addProperty("kie.username", KIE_USERNAME);
        handle.addProperty("kie.password", KIE_PASSWORD);
        handle.addProperty("mq.username", MQ_USERNAME);
        handle.addProperty("mq.password", MQ_PASSWORD);
        handle.store(war);

        return war;
    }

    protected abstract URL getRouteURL();

    /*
    * Returns the kieService client
    */
    protected KieServicesClient getKieRestServiceClient(URL baseURL) throws Exception {
        KieServicesConfiguration kieServicesConfiguration = KieServicesFactory.newRestConfiguration(new URL(baseURL,
                "/kie-server/services/rest/server").toString(), KIE_USERNAME, KIE_PASSWORD);
        kieServicesConfiguration.setMarshallingFormat(MarshallingFormat.XSTREAM);
        return KieServicesFactory.newKieServicesClient(kieServicesConfiguration);
    }

    /*
    * Returns the JMS kieService client
    */
    public KieServicesClient getKieJmsServiceClient() throws NamingException {
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        props.setProperty(Context.PROVIDER_URL, AMQ_HOST);
        props.setProperty(Context.SECURITY_PRINCIPAL, KIE_USERNAME);
        props.setProperty(Context.SECURITY_CREDENTIALS, KIE_PASSWORD);
        InitialContext context = new InitialContext(props);
        ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("ConnectionFactory");
        javax.jms.Queue requestQueue = (javax.jms.Queue) context.lookup("dynamicQueues/queue/KIE.SERVER.REQUEST");
        javax.jms.Queue responseQueue = (javax.jms.Queue) context.lookup("dynamicQueues/queue/KIE.SERVER.RESPONSE");
        KieServicesConfiguration config = KieServicesFactory.newJMSConfiguration(connectionFactory, requestQueue, responseQueue, MQ_USERNAME, MQ_PASSWORD);
        config.setMarshallingFormat(MarshallingFormat.XSTREAM);
        return KieServicesFactory.newKieServicesClient(config);
    }

    /*
    * Return the RuleServicesClient
    */
    public RuleServicesClient getRuleServicesClient(KieServicesClient client) {
        return client.getServicesClient(RuleServicesClient.class);
    }

    /*
     * Return the classes used in the MarshallerFactory
     */
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(Person.class);
        classes.add(Greeting.class);
        return classes;
    }

    /*
     * Return the batch command used to fire rules
     */
    public BatchExecutionCommand batchCommand() {
        person.setName("Filippe Spolti");
        List<Command<?>> commands = new ArrayList<>();
        commands.add((Command<?>) CommandFactory.newInsert(person));
        commands.add((Command<?>) CommandFactory.newFireAllRules());
        commands.add((Command<?>) CommandFactory.newQuery("greetings", "get greeting"));
        return CommandFactory.newBatchExecution(commands, "HelloRulesSession");
    }

    protected void prepareClientInvocation() throws Exception {
        // do nothing in basic
    }

    /*
     * Verifies the server capabilities, for decisionserver-openshift:6.2 it
     * should be KieServer BRM
     */
    public void checkDecisionServerCapabilities(URL url) throws Exception {
        log.info("Running test checkDecisionServerCapabilities");
        // for untrusted connections
        prepareClientInvocation();

        // Where the result will be stored
        String serverCapabilitiesResult = "";

        // Getting the KieServiceClient
        KieServicesClient kieServicesClient = getKieRestServiceClient(url);
        KieServerInfo serverInfo = kieServicesClient.getServerInfo().getResult();

        // Reading Server capabilities
        for (String capability : serverInfo.getCapabilities()) {
            serverCapabilitiesResult += capability;
        }

        // Sometimes the getCapabilities returns "KieServer BRM" and another time "BRM KieServer"
        // We have to make sure the result will be the same always
        Assert.assertTrue(serverCapabilitiesResult.equals("KieServerBRM") || serverCapabilitiesResult.equals("BRMKieServer"));
    }

    /*
    * Verifies the KieContainer ID, it should be HelloRulesContainer
    * Verifies the KieContainer Status, it should be org.kie.server.api.model.KieContainerStatus.STARTED
    */
    public void checkDecisionServerContainer() throws Exception {
        log.info("Running test checkDecisionServerContainer");
        // for untrusted connections
        prepareClientInvocation();

        List<KieContainerResource> kieContainers = getKieRestServiceClient(getRouteURL()).listContainers().getResult().getContainers();

        // verify the KieContainer Name
        Assert.assertEquals("HelloRulesContainer", kieContainers.get(0).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(0).getStatus());
    }

    /*
    * Test the rule deployed on Openshift, the template used to register the HelloRules container with the Kie jar:
    * https://github.com/jboss-openshift/openshift-quickstarts/tree/master/decisionserver
    */
    public void checkFireAllRules() throws Exception {
        log.info("Running test checkFireAllRules");
        // for untrusted connections
        prepareClientInvocation();

        KieServicesClient client = getKieRestServiceClient(getRouteURL());

        ServiceResponse<String> response = getRuleServicesClient(client).executeCommands("HelloRulesContainer", batchCommand());

        Marshaller marshaller = MarshallerFactory.getMarshaller(getClasses(), MarshallingFormat.XSTREAM, Person.class.getClassLoader());
        ExecutionResults results = marshaller.unmarshall(response.getResult(), ExecutionResults.class);

        // results cannot be null
        Assert.assertNotNull(results);

        QueryResults queryResults = (QueryResults) results.getValue("greetings");
        Greeting greeting = new Greeting();
        for (QueryResultsRow queryResult : queryResults) {
            greeting = (Greeting) queryResult.get("greeting");
            System.out.println("Result: " + greeting.getSalutation());
        }

        Assert.assertEquals("Hello " + person.getName() + "!", greeting.getSalutation());
    }

    /*
    * Test the rule deployed on Openshift using AMQ client, the template used to register the HelloRules container with the Kie jar:
    * https://github.com/jboss-openshift/openshift-quickstarts/tree/master/decisionserver
    */
    public void checkFireAllRulesAMQ() throws Exception {
        log.info("Running test checkFireAllRulesAMQ");
        log.info("Trying to connect to AMQ HOST: " + AMQ_HOST);

        KieServicesClient client = getKieJmsServiceClient();

        ServiceResponse<String> response = getRuleServicesClient(client).executeCommands("HelloRulesContainer", batchCommand());
        Marshaller marshaller = MarshallerFactory.getMarshaller(getClasses(), MarshallingFormat.XSTREAM, Person.class.getClassLoader());
        ExecutionResults results = marshaller.unmarshall(response.getResult(), ExecutionResults.class);

        // results cannot be null
        Assert.assertNotNull(results);

        QueryResults queryResults = (QueryResults) results.getValue("greetings");
        Greeting greeting = new Greeting();
        for (QueryResultsRow queryResult : queryResults) {
            greeting = (Greeting) queryResult.get("greeting");
            System.out.println("Result AMQ: " + greeting.getSalutation());
        }

        Assert.assertEquals("Hello " + person.getName() + "!", greeting.getSalutation());
    }

    /*
    * Verifies the server capabilities using AMQ client, for decisionserver-openshift:6.2 it
    * should be KieServer BRM
    */
    public void checkDecisionServerCapabilitiesAMQ() throws NamingException {
        log.info("Running test checkDecisionServerCapabilitiesAMQ");
        log.info("Trying to connect to AMQ HOST: " + AMQ_HOST);
        // Where the result will be stored
        String serverCapabilitiesResult = "";

        // Getting the KieServiceClient JMS
        KieServicesClient kieServicesClient = getKieJmsServiceClient();
        KieServerInfo serverInfo = kieServicesClient.getServerInfo().getResult();

        // Reading Server capabilities
        for (String capability : serverInfo.getCapabilities()) {
            serverCapabilitiesResult += (capability);
        }

        // Sometimes the getCapabilities returns "KieServer BRM" and another time "BRM KieServer"
        // We have to make sure the result will be the same always
        Assert.assertTrue(serverCapabilitiesResult.equals("KieServerBRM") || serverCapabilitiesResult.equals("BRMKieServer"));
    }

    /*
    * Verifies the KieContainer ID, it should be HelloRulesContainer
    * Verifies the KieContainer Status, it should be org.kie.server.api.model.KieContainerStatus.STARTED
    */
    public void checkDecisionServerContainerAMQ() throws NamingException {
        log.info("Running test checkDecisionServerContainerAMQ");
        log.info("Trying to connect to AMQ HOST: " + AMQ_HOST);
        List<KieContainerResource> kieContainers = getKieJmsServiceClient().listContainers().getResult().getContainers();

        // verify the KieContainer Name
        Assert.assertEquals("HelloRulesContainer", kieContainers.get(0).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(0).getStatus());
    }

    //Multiple Container Tests

    /*
    * Tests a decision server with 2 containers:
    * Verifies the KieContainer ID, it should be AnotherContainer
    * Verifies the KieContainer Status, it should be org.kie.server.api.model.KieContainerStatus.STARTED
    */
    public void checkSecondDecisionServerContainer() throws Exception {
        log.info("Running test checkSecondDecisionServerContainer");
        // for untrusted connections
        prepareClientInvocation();
        List<KieContainerResource> kieContainers = getKieRestServiceClient(getRouteURL()).listContainers().getResult().getContainers();

        //kieContainers size should be 2
        Assert.assertEquals(2, kieContainers.size());

        // verify the KieContainer Name
        Assert.assertEquals("AnotherContainer", kieContainers.get(1).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(1).getStatus());
    }

    /*
    * Test the rule deployed on Openshift, the template used to register the HelloRules container with the Kie jar:
    * https://github.com/jboss-openshift/openshift-quickstarts/tree/master/decisionserver
    */
    public void checkFireAllRulesInSecondContainer() throws Exception {
        log.info("Running test checkFireAllRulesInSecondContainer");
        // for untrusted connections
        prepareClientInvocation();
        KieServicesClient client = getKieRestServiceClient(getRouteURL());

        ServiceResponse<String> response = getRuleServicesClient(client).executeCommands("AnotherContainer", batchCommand());

        Marshaller marshaller = MarshallerFactory.getMarshaller(getClasses(), MarshallingFormat.XSTREAM, Person.class.getClassLoader());
        ExecutionResults results = marshaller.unmarshall(response.getResult(), ExecutionResults.class);

        // results cannot be null
        Assert.assertNotNull(results);

        QueryResults queryResults = (QueryResults) results.getValue("greetings");
        Greeting greeting = new Greeting();
        for (QueryResultsRow queryResult : queryResults) {
            greeting = (Greeting) queryResult.get("greeting");
            System.out.println("Result: " + greeting.getSalutation());
        }

        Assert.assertEquals("Hello " + person.getName() + "!", greeting.getSalutation());
    }

    /*
    * Verifies the Second KieContainer ID using AMQ client, it should be AnotherContainer
    * Verifies the KieContainer Status, it should be org.kie.server.api.model.KieContainerStatus.STARTED
    */
    public void checkDecisionServerSecondContainerAMQ() throws NamingException {
        log.info("Running test checkDecisionServerSecondContainerAMQ");
        List<KieContainerResource> kieContainers = getKieJmsServiceClient().listContainers().getResult().getContainers();

        //kieContainers size should be 2
        Assert.assertEquals(2, kieContainers.size());

        // verify the KieContainer Name
        Assert.assertEquals("AnotherContainer", kieContainers.get(1).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(1).getStatus());
    }

    /*
    * Test the rule deployed on Openshift using AMQ client, this test case register a new template called AnotherContainer with the Kie jar:
    * https://github.com/jboss-openshift/openshift-quickstarts/tree/master/decisionserver
    */
    public void checkFireAllRulesInSecondContainerAMQ() throws Exception {
        log.info("Running test checkFireAllRulesInSecondContainerAMQ");
        log.info("Trying to connect to AMQ HOST: " + AMQ_HOST);

        KieServicesClient client = getKieJmsServiceClient();

        ServiceResponse<String> response = getRuleServicesClient(client).executeCommands("AnotherContainer", batchCommand());
        Marshaller marshaller = MarshallerFactory.getMarshaller(getClasses(), MarshallingFormat.XSTREAM, Person.class.getClassLoader());
        ExecutionResults results = marshaller.unmarshall(response.getResult(), ExecutionResults.class);

        // results cannot be null
        Assert.assertNotNull(results);

        QueryResults queryResults = (QueryResults) results.getValue("greetings");
        Greeting greeting = new Greeting();
        for (QueryResultsRow queryResult : queryResults) {
            greeting = (Greeting) queryResult.get("greeting");
            System.out.println("Result AMQ: " + greeting.getSalutation());
        }

        Assert.assertEquals("Hello " + person.getName() + "!", greeting.getSalutation());
    }

    //HttpClient tests
    /*
    * Allow httpClient to use untrusted connections
    * The code below was tested with httpclient 4.3.6-redhat-1
    * code example from; http://literatejava.com/networks/ignore-ssl-certificate-errors-apache-httpclient-4-4/
    */
    public static HttpClient acceptUntrustedConnClient() throws Exception {
        HttpClientBuilder b = HttpClientBuilder.create();

        // setup a Trust Strategy that allows all certificates.
        //
        SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
            public boolean isTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {
                return true;
            }

        }).build();
        b.setSslcontext(sslContext);

        // don't check Hostnames, either.
        //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
        HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

        // here's the special part:
        //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
        //      -- and create a Registry, to register it.
        //
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, (X509HostnameVerifier) hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", sslSocketFactory)
            .build();

        // now, we create connection-manager using our Registry.
        //      -- allows multi-threaded use
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        b.setConnectionManager(connMgr);

        // finally, build the HttpClient;
        //      -- done!
        return b.build();
    }

    /*
     * Returns httpClient client
     */
    private HttpClient getClient() throws Exception {
        return acceptUntrustedConnClient();
    }

    /*
    * Returns httpClient client Authorization Base64 encoded
    */
    private String authEncoding() {
        return Base64Encoder.encode(KIE_USERNAME + ":" + KIE_PASSWORD);
    }

    /*
     *Return the formatted xml to perform the fire-all-rules using httpClient
     */
    private String streamXML() {
        return BatchExecutionHelper.newXStreamMarshaller().toXML(batchCommand());
    }

    /*
    * Return the HttpPost response for fire-all-rules using httpClient
    */
    private HttpResponse responseFireAllRules(String host, String containerId) throws Exception {

        //request
        HttpPost request = new HttpPost(host + "/kie-server/services/rest/server/containers/instances/" + containerId);
        //setting headers
        request.setHeader("accept", "application/xml");
        request.setHeader("X-KIE-ContentType", "XSTREAM");
        request.setHeader("Content-Type", "application/xml");
        request.setHeader("X-KIE-ClassType", "org.drools.core.command.runtime.BatchExecutionCommandImpl");

        //setting authorization
        request.setHeader("Authorization", "Basic " + authEncoding());

        //Set the request post body
        StringEntity userEntity = new StringEntity(streamXML());
        request.setEntity(userEntity);

        //performing request
        HttpClient client = acceptUntrustedConnClient();
        return client.execute(request);
    }


    /*
    * Return the HttpGet response for generic requests using httpClient
    */
    private HttpResponse genericResponse(String host, String uri) throws Exception {
        //request
        HttpGet request = new HttpGet(host + uri);
        try {
            //setting header
            request.setHeader("accept", "application/xml");
            //setting authorization
            request.setHeader("Authorization", "Basic " + authEncoding());
            return getClient().execute(request);
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Error executing request: %s", request), e);
        }
    }

    /* Verifies the server capabilities using httpClient, for decisionserver-openshift:6.2 it
    * should be KieServer BRM
    */
    public void checkDecisionServerCapabilitiesHttpClient() throws Exception {

        String HOST = "https://" + System.getenv("SECURE_KIE_APP_SERVICE_HOST") + ":" + System.getenv("SECURE_KIE_APP_SERVICE_PORT");
        String URI = "/kie-server/services/rest/server";

        //performing request
        HttpResponse response = genericResponse(HOST, URI);

        //response code should be 200
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        // retrieving output response
        HttpEntity httpEntity = response.getEntity();
        String output = EntityUtils.toString(httpEntity);
        System.out.println(output);

        //converting response in a object (org.kie.server.api.model.ServiceResponse)
        JAXBContext jaxbContent = JAXBContext.newInstance(ServiceResponse.class);
        Unmarshaller unmarshaller = jaxbContent.createUnmarshaller();

        ServiceResponse serviceResponse = (ServiceResponse) unmarshaller.unmarshal(new StringReader(output));
        KieServerInfo serverInfo = (KieServerInfo) serviceResponse.getResult();

        // Reading Server capabilities
        String serverCapabilitiesResult = "";
        for (String capability : serverInfo.getCapabilities()) {
            serverCapabilitiesResult += (capability);
        }

        Assert.assertTrue(serverCapabilitiesResult.equals("KieServerBRM") || serverCapabilitiesResult.equals("BRMKieServer"));
    }

    /*
    * Verifies the KieContainer ID
    * Verifies the KieContainer Status, it should be org.kie.server.api.model.KieContainerStatus.STARTED
    * test using httpClient
    */
    public void checkDecisionServerSecureMultiContainerHttpClient() throws Exception {

        String HOST = "https://" + System.getenv("SECURE_KIE_APP_SERVICE_HOST") + ":" + System.getenv("SECURE_KIE_APP_SERVICE_PORT");
        String URI = "/kie-server/services/rest/server/containers";

        //Retrieving response
        HttpResponse response = genericResponse(HOST, URI);

        //response code should be 200
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        // retrieving output response
        HttpEntity httpEntity = response.getEntity();
        String output = EntityUtils.toString(httpEntity);

        //converting response in a object (org.kie.server.api.model.ServiceResponse)
        JAXBContext jaxbContent = JAXBContext.newInstance(ServiceResponse.class);
        Unmarshaller unmarshaller = jaxbContent.createUnmarshaller();

        ServiceResponse serviceResponse = (ServiceResponse) unmarshaller.unmarshal(new StringReader(output));
        KieContainerResourceList kieContainers = (KieContainerResourceList) serviceResponse.getResult();

        //kieContainers's should be 2
        Assert.assertEquals(2, kieContainers.getContainers().size());

        // verify the first KieContainer Name
        Assert.assertEquals("HelloRulesContainer", kieContainers.getContainers().get(0).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.getContainers().get(0).getStatus());

        // verify the second KieContainer Name
        Assert.assertEquals("AnotherContainer", kieContainers.getContainers().get(1).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.getContainers().get(1).getStatus());
    }

    /*
    * Test the rule deployed on Openshift using httpClient, the template used register the HelloRulesContainer with the Kie jar:
    * https://github.com/jboss-openshift/openshift-quickstarts/tree/master/decisionserver
    */
    public void checkFireAllRulesSecureHttpClient() throws Exception {

        String HOST = "https://" + System.getenv("SECURE_KIE_APP_SERVICE_HOST") + ":" + System.getenv("SECURE_KIE_APP_SERVICE_PORT");

        HttpResponse response = responseFireAllRules(HOST, "HelloRulesContainer");

        //response code should be 200
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        // retrieving output response
        HttpEntity httpEntity = response.getEntity();
        String output = EntityUtils.toString(httpEntity);

        Marshaller marshaller = MarshallerFactory.getMarshaller(getClasses(), MarshallingFormat.XSTREAM, Person.class.getClassLoader());
        ServiceResponse serviceResponse = marshaller.unmarshall(output, ServiceResponse.class);

        ExecutionResults execResults = marshaller.unmarshall(String.valueOf(serviceResponse.getResult()), ExecutionResults.class);

        QueryResults queryResults = (QueryResults) execResults.getValue("greetings");
        Greeting greeting = new Greeting();
        for (QueryResultsRow queryResult : queryResults) {
            greeting = (Greeting) queryResult.get("greeting");
            System.out.println("Result: " + greeting.getSalutation());
        }

        Assert.assertEquals("Hello " + person.getName() + "!", greeting.getSalutation());
    }

    /*
    * Test the rule deployed on Openshift using httpClient and multicontainer feature, the template used register the
     * AnotherContainer with the Kie jar:
    * https://github.com/jboss-openshift/openshift-quickstarts/tree/master/decisionserver
    */
    public void checkFireAllRulesSecureSecondContainerHttpClient() throws Exception {

        String HOST = "https://" + System.getenv("SECURE_KIE_APP_SERVICE_HOST") + ":" + System.getenv("SECURE_KIE_APP_SERVICE_PORT");

        HttpResponse response = responseFireAllRules(HOST, "AnotherContainer");

        //response code should be 200
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        // retrieving output response
        HttpEntity httpEntity = response.getEntity();
        String output = EntityUtils.toString(httpEntity);

        Marshaller marshaller = MarshallerFactory.getMarshaller(getClasses(), MarshallingFormat.XSTREAM, Person.class.getClassLoader());
        ServiceResponse serviceResponse = marshaller.unmarshall(output, ServiceResponse.class);

        ExecutionResults execResults = marshaller.unmarshall(String.valueOf(serviceResponse.getResult()), ExecutionResults.class);

        QueryResults queryResults = (QueryResults) execResults.getValue("greetings");

        Greeting greeting = new Greeting();
        for (QueryResultsRow queryResult : queryResults) {
            greeting = (Greeting) queryResult.get("greeting");
            System.out.println("Result: " + greeting.getSalutation());
        }

        Assert.assertEquals("Hello " + person.getName() + "!", greeting.getSalutation());
    }
}
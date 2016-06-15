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
import org.jboss.arquillian.ce.api.ConfigurationHandle;
import org.jboss.arquillian.ce.httpclient.HttpClient;
import org.jboss.arquillian.ce.httpclient.HttpClientBuilder;
import org.jboss.arquillian.ce.httpclient.HttpRequest;
import org.jboss.arquillian.ce.httpclient.HttpResponse;
import org.jboss.arquillian.ce.shrinkwrap.Files;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.common.KieServerCommon;
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

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;


/**
 * @author Filippe Spolti
 * @author Ales justin
 */
public abstract class DecisionServerTestBase extends KieServerCommon {
    protected final Logger log = Logger.getLogger(getClass().getName());

    protected static final String FILENAME = "kie.properties";


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
        war.addAsLibraries(Libraries.transitive("org.jboss.arquillian.container", "arquillian-ce-httpclient"));
        Files.PropertiesHandle handle = Files.createPropertiesHandle(FILENAME);
        handle.addProperty("kie.username", KIE_USERNAME);
        handle.addProperty("kie.password", KIE_PASSWORD);
        handle.addProperty("mq.username", MQ_USERNAME);
        handle.addProperty("mq.password", MQ_PASSWORD);
        handle.store(war);

        return war;
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

    /*
    * Verifies the KieContainer ID, it should be decisionserver-hellorules
    * Verifies the KieContainer Status, it should be org.kie.server.api.model.KieContainerStatus.STARTED
    */
    public void checkDecisionServerContainer() throws Exception {
        log.info("Running test checkDecisionServerContainer");
        // for untrusted connections
        prepareClientInvocation();

        List<KieContainerResource> kieContainers = getKieRestServiceClient(getRouteURL()).listContainers().getResult().getContainers();

        // Sorting kieContainerList
        Collections.sort(kieContainers, ALPHABETICAL_ORDER);
        // verify the KieContainer Name

        //When the multicontainer test is running, the decisionserver-hellorules will not be the first anymore
        int pos = 0;
        if (kieContainers.size() > 1) {
            pos = 1;
        }

        Assert.assertTrue(kieContainers.get(pos).getContainerId().startsWith("decisionserver-hellorules_"));
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(pos).getStatus());
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

        ServiceResponse<String> response = getRuleServicesClient(client).executeCommands("decisionserver-hellorules", batchCommand());

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

        ServiceResponse<String> response = getRuleServicesClient(client).executeCommands("decisionserver-hellorules", batchCommand());
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
    * Verifies the KieContainer ID, it should be decisionserver-hellorules
    * Verifies the KieContainer Status, it should be org.kie.server.api.model.KieContainerStatus.STARTED
    */
    public void checkDecisionServerContainerAMQ() throws NamingException {
        log.info("Running test checkDecisionServerContainerAMQ");
        log.info("Trying to connect to AMQ HOST: " + AMQ_HOST);
        List<KieContainerResource> kieContainers = getKieJmsServiceClient().listContainers().getResult().getContainers();

        //When the multicontainer test is running, the decisionserver-hellorules will not be the first anymore
        int pos = 0;
        if (kieContainers.size() > 1) {
            pos = 1;
        }

        // Sorting kieContainerList
        Collections.sort(kieContainers, ALPHABETICAL_ORDER);
        // verify the KieContainer Name
        Assert.assertTrue(kieContainers.get(pos).getContainerId().startsWith("decisionserver-hellorules_"));
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(pos).getStatus());
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

        // Sorting kieContainerList
        Collections.sort(kieContainers, ALPHABETICAL_ORDER);

        //kieContainers size should be 2
        Assert.assertEquals(2, kieContainers.size());

        // verify the KieContainer Name
        Assert.assertEquals("AnotherContainer", kieContainers.get(0).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(0).getStatus());
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
        
        Assert.assertTrue(response.getResult() != null && response.getResult().length() > 0);

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

        // Sorting kieContainerList
        Collections.sort(kieContainers, ALPHABETICAL_ORDER);

        //kieContainers size should be 2
        Assert.assertEquals(2, kieContainers.size());

        // verify the KieContainer Name
        Assert.assertEquals("AnotherContainer", kieContainers.get(0).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(0).getStatus());
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

        Assert.assertTrue(response.getResult() != null && response.getResult().length() > 0);

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
     * Returns httpClient client
     */
    private HttpClient getClient() throws Exception {
        return HttpClientBuilder.untrustedConnectionClient();
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
        HttpRequest request = HttpClientBuilder.doPOST(host + "/kie-server/services/rest/server/containers/instances/" + containerId);
        //setting headers
        request.setHeader("accept", "application/xml");
        request.setHeader("X-KIE-ContentType", "XSTREAM");
        request.setHeader("Content-Type", "application/xml");
        request.setHeader("X-KIE-ClassType", "org.drools.core.command.runtime.BatchExecutionCommandImpl");

        //setting authorization
        request.setHeader("Authorization", "Basic " + authEncoding());

        //Set the request post body
        request.setEntity(streamXML());

        //performing request
        HttpClient client = getClient();
        return client.execute(request);
    }


    /*
    * Return the HttpGet response for generic requests using httpClient
    */
    private HttpResponse genericResponse(String host, String uri) throws Exception {
        //request
        HttpRequest request = HttpClientBuilder.doGET(host + uri);
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
        Assert.assertEquals(200, response.getResponseCode());

        // retrieving output response
        String output = response.getResponseBodyAsString();
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
        Assert.assertEquals(200, response.getResponseCode());

        // retrieving output response
        String output = response.getResponseBodyAsString();

        //converting response in a object (org.kie.server.api.model.ServiceResponse)
        JAXBContext jaxbContent = JAXBContext.newInstance(ServiceResponse.class);
        Unmarshaller unmarshaller = jaxbContent.createUnmarshaller();

        ServiceResponse serviceResponse = (ServiceResponse) unmarshaller.unmarshal(new StringReader(output));
        KieContainerResourceList kieContainers = (KieContainerResourceList) serviceResponse.getResult();

        List<KieContainerResource> containers = kieContainers.getContainers();

        // Sorting kieContainerList
        Collections.sort(containers, ALPHABETICAL_ORDER);

        //kieContainers's should be 2
        Assert.assertEquals(2, kieContainers.getContainers().size());


        // verify the first KieContainer Name
        Assert.assertEquals("AnotherContainer", kieContainers.getContainers().get(0).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.getContainers().get(0).getStatus());
        // verify the second KieContainer Name
        Assert.assertEquals("decisionserver-hellorules", kieContainers.getContainers().get(1).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.getContainers().get(1).getStatus());
    }

    /*
    * Test the rule deployed on Openshift using httpClient, the template used register the decisionserver-hellorules with the Kie jar:
    * https://github.com/jboss-openshift/openshift-quickstarts/tree/master/decisionserver
    */
    public void checkFireAllRulesSecureHttpClient() throws Exception {

        String HOST = "https://" + System.getenv("SECURE_KIE_APP_SERVICE_HOST") + ":" + System.getenv("SECURE_KIE_APP_SERVICE_PORT");

        HttpResponse response = responseFireAllRules(HOST, "decisionserver-hellorules");

        //response code should be 200
        Assert.assertEquals(200, response.getResponseCode());

        // retrieving output response
        String output = response.getResponseBodyAsString();
        Assert.assertTrue(output != null && output.length() > 0);

        Marshaller marshaller = MarshallerFactory.getMarshaller(getClasses(), MarshallingFormat.XSTREAM, Person.class.getClassLoader());
        ServiceResponse<ExecutionResults> serviceResponse = marshaller.unmarshall(output, ServiceResponse.class);

        Assert.assertTrue(serviceResponse.getType() == ServiceResponse.ResponseType.SUCCESS);

        ExecutionResults execResults = serviceResponse.getResult();

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
        Assert.assertEquals(200, response.getResponseCode());

        // retrieving output response
        String output = response.getResponseBodyAsString();
        Assert.assertTrue(output != null && output.length() > 0);

        Marshaller marshaller = MarshallerFactory.getMarshaller(getClasses(), MarshallingFormat.XSTREAM, Person.class.getClassLoader());
        ServiceResponse serviceResponse = marshaller.unmarshall(output, ServiceResponse.class);

        Assert.assertTrue(serviceResponse.getType() == ServiceResponse.ResponseType.SUCCESS);

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
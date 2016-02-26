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

import java.util.*;
import java.util.logging.Logger;

import org.jboss.test.arquillian.ce.decisionserver.support.Support;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.kie.server.api.marshalling.Marshaller;
import org.kie.server.api.marshalling.MarshallerFactory;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.openshift.quickstarts.decisionserver.hellorules.Greeting;
import org.openshift.quickstarts.decisionserver.hellorules.Person;

import javax.naming.NamingException;


/**
 * @author Filippe Spolti
 * @author Ales justin
 */
public abstract class DecisionServerTestBase extends Support{

    private static final Logger log = Logger.getLogger(DecisionServerTestBase.class.getCanonicalName());

    /*
     * Verifies the server capabilities, for decisionserver-openshift:6.2 it
     * should be KieServer BRM
     */
    public void testDecisionServerCapabilities() throws Exception {
        log.info("Running test testDecisionServerCapabilities");
        // for untrusted connections
        prepareClientInvocation();

        // Where the result will be stored
        String serverCapabilitiesResult = "";

        // Getting the KieServiceClient
        KieServicesClient kieServicesClient = getKieRestServiceClient();
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
    public void testDecisionServerContainer() throws Exception {
        log.info("Running test testDecisionServerContainer");
        // for untrusted connections
        prepareClientInvocation();

        List<KieContainerResource> kieContainers = getKieRestServiceClient().listContainers().getResult().getContainers();

        // verify the KieContainer Name
        Assert.assertEquals("HelloRulesContainer", kieContainers.get(0).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(0).getStatus());
    }

    /*
    * Test the rule deployed on Openshift, the template used register the HelloRules container with the Kie jar:
    * https://github.com/jboss-openshift/openshift-quickstarts/tree/master/decisionserver
    */
    public void testFireAllRules() throws Exception {
        log.info("Running test testFireAllRules");
        // for untrusted connections
        prepareClientInvocation();

        KieServicesClient client = getKieRestServiceClient();

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
    * Test the rule deployed on Openshift, the template used register the HelloRules container with the Kie jar:
    * https://github.com/jboss-openshift/openshift-quickstarts/tree/master/decisionserver
    */
    public void testFireAllRulesAMQ() throws Exception {
        log.info("Running test amqCommandExecFiraAllRules");
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
    * Verifies the server capabilities, for decisionserver-openshift:6.2 it
    * should be KieServer BRM
    */
    public void testDecisionServerCapabilitiesAMQ () throws NamingException {
        log.info("Running test testDecisionServerCapabilitiesAMQ");
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
    public void testDecisionServerContainerAMQ() throws NamingException {
        log.info("Running test testDecisionServerContainerAMQ");
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
    public void testSecondDecisionServerContainer() throws Exception {
        log.info("Running test testSecondDecisionServerContainer");
        // for untrusted connections
        prepareClientInvocation();
        List<KieContainerResource> kieContainers = getKieRestServiceClient().listContainers().getResult().getContainers();

        //kieContainers size should be 2
        Assert.assertEquals(2, kieContainers.size());

        // verify the KieContainer Name
        Assert.assertEquals("AnotherContainer", kieContainers.get(1).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(1).getStatus());
    }

    /*
    * Test the rule deployed on Openshift, the template used register the HelloRules container with the Kie jar:
    * https://github.com/jboss-openshift/openshift-quickstarts/tree/master/decisionserver
    */
    public void testFireAllRulesInSecondContainer() throws Exception {
        log.info("Running test testFireAllRulesInSecondContainer");
        // for untrusted connections
        prepareClientInvocation();
        KieServicesClient client = getKieRestServiceClient();

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
    * Verifies the Second KieContainer ID, it should be AnotherContainer
    * Verifies the KieContainer Status, it should be org.kie.server.api.model.KieContainerStatus.STARTED
    */
    public void testDecisionServerSecondContainerAMQ() throws NamingException {
        log.info("Running test testDecisionServerSecondContainerAMQ");
        List<KieContainerResource> kieContainers = getKieJmsServiceClient().listContainers().getResult().getContainers();

        //kieContainers size should be 2
        Assert.assertEquals(2, kieContainers.size());

        // verify the KieContainer Name
        Assert.assertEquals("AnotherContainer", kieContainers.get(1).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(1).getStatus());
    }

    /*
    * Test the rule deployed on Openshift, this test case register a new template called NewHelloRulesContainer with the Kie jar:
    * https://github.com/jboss-openshift/openshift-quickstarts/tree/master/decisionserver
    */
    public void testFireAllRulesInSecondContainerAMQ() throws Exception {
        log.info("Running test testFireAllRulesInSecondContainerAMQ");
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

}
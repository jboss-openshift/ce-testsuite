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

import org.jboss.arquillian.ce.api.*;
import org.jboss.arquillian.ce.shrinkwrap.Files;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.kie.internal.command.CommandFactory;
import org.kie.server.api.marshalling.Marshaller;
import org.kie.server.api.marshalling.MarshallerFactory;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;
import org.openshift.quickstarts.decisionserver.hellorules.Greeting;
import org.openshift.quickstarts.decisionserver.hellorules.Person;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.*;


/**
 * @author Filippe Spolti
 */

@RunWith(Arquillian.class)
@RunInPod
@ExternalDeployment
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/decisionserver/decisionserver62-https-s2i.json",
        labels = "application=kie-app",
        parameters = {
                @TemplateParameter(name = "KIE_SERVER_USER", value = "${kie.username:kieserver}"),
                @TemplateParameter(name = "KIE_SERVER_PASSWORD", value = "${kie.password:Redhat@123}")
        }
)
@OpenShiftResources({
        @OpenShiftResource("https://raw.githubusercontent.com/jboss-openshift/application-templates/master/jboss-image-streams.json"),
        @OpenShiftResource("classpath:decisionserver-service-account.json"),
        @OpenShiftResource("classpath:decisionserver-app-secret.json")
})
public class DecisionServerBasicSecureTest {

    private static final String FILENAME = "kie.properties";
    private static final String SECURE_DECISIONSERVER_ROUTE_HOST = "https://secure-kie-app-%s.router.default.svc.cluster.local/kie-server/services/rest/server";

    //kie-server credentials
    private static final String USERNAME = System.getProperty("kie.username", "kieserver");
    private static final String PASSWORD = System.getProperty("kie.password", "Redhat@123");

    @ArquillianResource
    private ConfigurationHandle configuration;

    @Deployment
    @RunInPodDeployment
    public static WebArchive getDeployment() throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML("web.xml");
        war.addPackage(Person.class.getPackage());
        war.addAsLibraries(Libraries.transitive("org.kie.server", "kie-server-client"));

        Files.PropertiesHandle handle = Files.createPropertiesHandle(FILENAME);
        handle.addProperty("kie.username", USERNAME);
        handle.addProperty("kie.password", PASSWORD);
        handle.store(war);

        return war;
    }

    /*
    * Returns the kieService client
    */
    private KieServicesClient getKieServiceClient() throws Exception {
        Properties properties = Tools.loadProperties(DecisionServerBasicSecureTest.class, FILENAME);
        String username = properties.getProperty("kie.username");
        String password = properties.getProperty("kie.password");

        KieServicesConfiguration kieServicesConfiguration = KieServicesFactory.newRestConfiguration(resolveHost(), username, password);
        kieServicesConfiguration.setMarshallingFormat(MarshallingFormat.XSTREAM);
        return KieServicesFactory.newKieServicesClient(kieServicesConfiguration);
    }

    /*
    * Return the resolved endpoint's host/uri
    */
    private String resolveHost() {
        return String.format(SECURE_DECISIONSERVER_ROUTE_HOST, configuration.getNamespace());
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
    public static Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(Person.class);
        classes.add(Greeting.class);
        return classes;
    }

    /*
     * Verifies the server capabilities, for decisionserver-openshift:6.2 it
     * should be KieServer BRM
     */
    @Test
    public void testDecisionServerStatus() throws Exception {

        // Where the result will be stored
        String serverCapabilitiesResult = "";

        // Getting the KieServiceClient
        KieServicesClient kieServicesClient = getKieServiceClient();

        //forgiveUnknownCert();

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
    @Test
    public void testDecisionServerContainer() throws Exception {
       // forgiveUnknownCert();
        List<KieContainerResource> kieContainers = getKieServiceClient().listContainers().getResult().getContainers();

        // verify the KieContainer Name
        Assert.assertEquals("HelloRulesContainer", kieContainers.get(0).getContainerId());
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(0).getStatus());
    }

    /*
    * Test the rule deployed on Openshift, the template used register the HelloRules container with the Kie jar:
    * https://github.com/jboss-openshift/openshift-quickstarts/tree/master/decisionserver
    */
    @Test
    public void testFireAllRules() throws Exception {
        ignoreUnkonwCerts();
        KieServicesClient client = getKieServiceClient();

        Person person = new Person();
        person.setName("Filippe Spolti");
        List<Command<?>> commands = new ArrayList<>();
        commands.add((Command<?>) CommandFactory.newInsert(person));
        commands.add((Command<?>) CommandFactory.newFireAllRules());
        commands.add((Command<?>) CommandFactory.newQuery("greetings", "get greeting"));
        BatchExecutionCommand command = CommandFactory.newBatchExecution(commands, "HelloRulesSession");
        ServiceResponse<String> response = getRuleServicesClient(client).executeCommands("HelloRulesContainer", command);

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

    // only needed for non-production test scenarios
    private void ignoreUnkonwCerts() throws Exception {

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        // Install the all-trusting trust manager
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

    }
}
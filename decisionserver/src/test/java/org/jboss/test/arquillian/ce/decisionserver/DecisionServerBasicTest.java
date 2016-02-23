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

import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.ce.api.ExternalDeployment;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.RunInPod;
import org.jboss.arquillian.ce.api.RunInPodDeployment;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.shrinkwrap.Files;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
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
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.openshift.quickstarts.decisionserver.hellorules.Greeting;
import org.openshift.quickstarts.decisionserver.hellorules.Person;


/**
 * @author Filippe Spolti
 */

@RunWith(Arquillian.class)
@RunInPod
@ExternalDeployment
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/decisionserver/decisionserver62-basic-s2i.json",
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
public class DecisionServerBasicTest extends DecisionServerTestBase {

    @Deployment
    @RunInPodDeployment
    public static WebArchive getDeployment() throws Exception {
        WebArchive war = getDeploymentInternal();

        Files.PropertiesHandle handle = Files.createPropertiesHandle(FILENAME);
        handle.addProperty("kie.username", KIE_USERNAME);
        handle.addProperty("kie.password", KIE_PASSWORD);
        handle.store(war);

        return war;
    }

    /*
    * Verifies the KieContainer ID, it should be HelloRulesContainer
    * Verifies the KieContainer Status, it should be org.kie.server.api.model.KieContainerStatus.STARTED
    */
    @Test
    public void testDecisionServerContainer() throws Exception {

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
    @Test
    public void testFireAllRules() throws Exception {

        KieServicesClient client = getKieRestServiceClient();

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
}
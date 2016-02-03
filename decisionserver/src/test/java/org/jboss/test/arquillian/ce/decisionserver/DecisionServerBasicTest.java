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

import static org.junit.Assert.assertEquals;

import org.jboss.arquillian.ce.api.ConfigurationHandle;
import org.jboss.arquillian.ce.api.ExternalDeployment;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.RunInPod;
import org.jboss.arquillian.ce.api.RunInPodDeployment;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;

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
        @TemplateParameter(name = "KIE_SERVER_PASSWORD", value = "${kie.password:Redhat@123}")}
)
@OpenShiftResources({
    @OpenShiftResource("classpath:decisionserver-internal-imagestream.json"),
    @OpenShiftResource("classpath:decisionserver-service-account.json"),
    @OpenShiftResource("classpath:decisionserver-app-secret.json")
})
public class DecisionServerBasicTest {

    public static final String DECISIONSERVER_ROUTE_HOST = "http://kie-app-%s.router.default.svc.cluster.local/kie-server/services/rest/server/";
    private static final MarshallingFormat FORMAT = MarshallingFormat.JSON;

    //kie-server credentials
    private static final String USERNAME = System.getProperty("kie.username", "kieserver");
    private static final String PASSWORD = System.getProperty("kie.password", "Redhat@123");

    @ArquillianResource
    ConfigurationHandle configuration;

    @Deployment
    @RunInPodDeployment
    public static WebArchive getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML("web.xml");

        war.addAsLibraries(
            Maven.resolver()
                .resolve("org.kie:kie-server-client")
                .withTransitivity()
                .asFile());

        return war;
    }

    /*
     * Verifies the server capabilities, for decisionserver-openshift:6.2 it should be KieServer BRM 
     */
    @Test
    @RunAsClient
    public void testDecisionServerStatus() throws Exception {

        //POD public address
        final String HOST = String.format(DECISIONSERVER_ROUTE_HOST, configuration.getNamespace());

        //Where the result will be stored
        String serverCapabilitiesResult = "";

        // Getting the KieServiceClient
        KieServicesClient kieServicesClient = getKieServiceClient(HOST);
        KieServerInfo serverInfo = kieServicesClient.getServerInfo().getResult();

        for (String capability : serverInfo.getCapabilities()) {
            serverCapabilitiesResult += (capability);
        }
        assertEquals("KieServerBRM", serverCapabilitiesResult);

    }

    /*
     * Returns the kieService client
     */
    public static KieServicesClient getKieServiceClient(String host) {
        System.out.println("URL:" + host);
        System.out.println("USERNAME:" + USERNAME);
        System.out.println("PASSWORD:" + PASSWORD);
        KieServicesConfiguration kieServicesConfiguration = KieServicesFactory.newRestConfiguration(host, USERNAME, PASSWORD);
        kieServicesConfiguration.setMarshallingFormat(FORMAT);
        return KieServicesFactory.newKieServicesClient(kieServicesConfiguration);
    }

}
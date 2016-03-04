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
import org.jboss.arquillian.ce.api.*;
import org.jboss.arquillian.ce.shrinkwrap.Files;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author fspolti
 */

@RunWith(Arquillian.class)
@RunInPod
@ExternalDeployment
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/decisionserver/decisionserver62-https-s2i.json",
        labels = "application=kie-app",
        parameters = {
                //the container with the bigger name will always get deployed first
                @TemplateParameter(name = "KIE_CONTAINER_DEPLOYMENT", value = "HelloRulesContainer=org.openshift.quickstarts:decisionserver-hellorules:1.3.0-SNAPSHOT|" +
                        "AnotherContainer=org.openshift.quickstarts:decisionserver-hellorules:1.3.0-SNAPSHOT"),
                @TemplateParameter(name = "KIE_SERVER_USER", value = "${kie.username:kieserver}"),
                @TemplateParameter(name = "KIE_SERVER_PASSWORD", value = "${kie.password:Redhat@123}")
        }
)
@OpenShiftResources({
        @OpenShiftResource("https://raw.githubusercontent.com/jboss-openshift/application-templates/master/jboss-image-streams.json"),
        @OpenShiftResource("classpath:decisionserver-service-account.json"),
        @OpenShiftResource("classpath:decisionserver-app-secret.json")
})
public class DecisionServerHttpClientSecureAllInOneTest extends DecisionServerTestBase {

    @Deployment
    @RunInPodDeployment
    public static WebArchive getDeployment() throws Exception {
        WebArchive war = getDeploymentInternal();
        war.addAsLibraries(Libraries.transitive("org.apache.httpcomponents", "httpclient"));
        war.addClass(Base64Encoder.class);
        Files.PropertiesHandle handle = Files.createPropertiesHandle(FILENAME);
        handle.addProperty("kie.username", KIE_USERNAME);
        handle.addProperty("kie.password", KIE_PASSWORD);
        handle.store(war);
        return war;
    }

    @Test
    public void testDecisionServerCapabilitiesHttpClient() throws Exception {
        checkDecisionServerCapabilitiesHttpClient();
    }

    @Test
    public void testDecisionServerSecureMultiContainerHttpClient () throws Exception {
        checkDecisionServerSecureMultiContainerHttpClient();
    }

    @Test
    public void testFireAllRulesSecureHttpClient() throws Exception {
        checkFireAllRulesSecureHttpClient();
    }

    @Test
    public void testFireAllRulesSecureSecondContainerHttpClient () throws Exception {
        checkFireAllRulesSecureSecondContainerHttpClient();
    }
}
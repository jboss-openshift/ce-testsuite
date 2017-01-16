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

import static org.jboss.arquillian.ce.api.Tools.trustAllCertificates;

import java.net.URL;

import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;

/**
 * @author Filippe Spolti
 */

@RunWith(Arquillian.class)
//The rest of template's parameters are coming from DecisionServerBasicTest class
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/decisionserver/decisionserver62-https-s2i.json",
        parameters = {
                //the Containers list will be sorted in alphabetical order
                @TemplateParameter(name = "KIE_CONTAINER_DEPLOYMENT", value = "HelloRulesContainer=org.openshift.quickstarts:decisionserver-hellorules:1.2.0.Final|" +
                        "AnotherContainer=org.openshift.quickstarts:decisionserver-hellorules:1.2.0.Final"),
                @TemplateParameter(name = "KIE_SERVER_USER", value = "${kie.username:kieserver}"),
                @TemplateParameter(name = "KIE_SERVER_PASSWORD", value = "${kie.password:Redhat@123}")
        }
)
public class DecisionServerBasicSecureMultiContainerTest extends DecisionServerBasicMulltiContainerTest {

    @RouteURL("secure-kie-app")
    private URL routeURL;

    @Override
    protected URL getRouteURL() {
        return routeURL;
    }

    // only needed for non-production test scenarios
    @Override
    protected void prepareClientInvocation() throws Exception {
        trustAllCertificates();
        log.info("Trusting all certs");
    }
}
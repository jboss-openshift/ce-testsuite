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

package org.jboss.test.arquillian.ce.common;

import org.junit.Assert;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;

import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Filippe Spolti
 *         <p>
 *         Place here all code/methods in common for process and decision server
 */
public abstract class KieServerCommon {

    /*
    * Sort the kieContainers list in alphabetical order
    * To sort the list just add the following in the desired method:
    * Collections.sort(KieContainersList, ALPHABETICAL_ORDER);
    */
    public static final Comparator<KieContainerResource> ALPHABETICAL_ORDER =
            new Comparator<KieContainerResource>() {
                @Override
                public int compare(KieContainerResource o1, KieContainerResource o2) {
                    return o1.getContainerId().compareTo(o2.getContainerId());
                }
            };
    //kie-server credentials
    protected static final String KIE_USERNAME = System.getProperty("kie.username", "kieserver");
    protected static final String KIE_PASSWORD = System.getProperty("kie.password", "Redhat@123");
    protected final Logger log = Logger.getLogger(KieServerCommon.class.getName());

    protected abstract URL getRouteURL();

    protected void prepareClientInvocation() throws Exception {
        // do nothing in basic
    }

    /*
    * @returns the kieService client
    * @param URL url - KieServer address
    * @throwns Exception for any issue
    */
    protected KieServicesClient getKieRestServiceClient(URL baseURL) throws Exception {
        KieServicesConfiguration kieServicesConfiguration = KieServicesFactory.newRestConfiguration(new URL(baseURL,
                "/kie-server/services/rest/server").toString(), KIE_USERNAME, KIE_PASSWORD);
        kieServicesConfiguration.setMarshallingFormat(MarshallingFormat.XSTREAM);
        return KieServicesFactory.newKieServicesClient(kieServicesConfiguration);
    }

    /*
    * Verifies the server capabilities, for:
    * decisionserver-openshift:6.3: KieServer BRM
    * processserver-openshift:6.3: KieServer BPM
    * @params URL url - kieserver address
    *         String cap - capability to test
    * @throws Exception for all issues
    */
    public void checkKieServerCapabilities(URL url, String cap) throws Exception {
        log.info("Running test checkKieServerCapabilities");
        // for untrusted connections
        prepareClientInvocation();

        // Getting the KieServiceClient
        KieServicesClient kieServicesClient = getKieRestServiceClient(url);
        KieServerInfo serverInfo = kieServicesClient.getServerInfo().getResult();

        // Where the result will be stored
        String serverCapabilitiesResult = "";

        // Reading Server capabilities
        for (String capability : serverInfo.getCapabilities()) {
            if (capability.toUpperCase().equals("KIESERVER")) {
                //do nothing
            }
            serverCapabilitiesResult += capability;
        }

        // Sometimes the getCapabilities returns "KieServer BRM" and another time "BRM KieServer"
        // We have to make sure the result will be the same always
        if ("BRM".equals(cap)) {
            Assert.assertTrue(serverCapabilitiesResult.contains("BRM"));
        } else if ("BPM".equals(cap)) {
            //for process server both BPM and BRM capabilities should be enabled
            Assert.assertTrue(serverCapabilitiesResult.contains("BPM") || serverCapabilitiesResult.contains("BRM"));
        }
    }

    /*
    * Verifies the KieContainer ID, it should be decisionserver-hellorules
    * Verifies the KieContainer Status, it should be org.kie.server.api.model.KieContainerStatus.STARTED
    */
    public void checkKieServerContainer(String containerId) throws Exception {
        log.info("Running test checkKieServerContainer");
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

        log.info("Container ID: " + kieContainers.get(pos).getContainerId());
        log.info("Container Status: " + kieContainers.get(pos).getStatus());

        Assert.assertTrue(kieContainers.get(pos).getContainerId().startsWith(containerId));
        // verify the KieContainer Status
        Assert.assertEquals(org.kie.server.api.model.KieContainerStatus.STARTED, kieContainers.get(pos).getStatus());

    }

}
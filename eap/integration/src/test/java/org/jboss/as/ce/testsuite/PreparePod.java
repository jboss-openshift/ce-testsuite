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

package org.jboss.as.ce.testsuite;

import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author spolti
 */
public class PreparePod {

    private static final Logger log = Logger.getLogger(PreparePod.class.getName());

    private final String DEFAULT_USERNAME = "admin";
    private final String DEFAULT_PASSWORD = "Admin#70365";
    private final String DEFAULT_POD_JBOSS_HOME = "/opt/eap";
    private final String DEFAULT_CUBE_ID = "testrunner";

    //This is only for the tests running successfully using portforwarder. DO NOT DO IT IN PRODUCTION
    private final String CHANGE_REMOTING_INTERFACE = "/socket-binding-group=standard-sockets/socket-binding=remoting:write-attribute(name=interface,value=management)";
    private final String CHANGE_HTTP_INTERFACE = "/socket-binding-group=standard-sockets/socket-binding=http:write-attribute(name=interface,value=management)";
    private final String CHANGE_MESSAGING_INTERFACE = "/socket-binding-group=standard-sockets/socket-binding=messaging:write-attribute(name=interface,value=management)";
    private final String RELOAD_COMMAND = "reload";

    private CommandContext ctx;

    /*
    * To run the EAP integration tests against a EAP instance running on Opensfhift we have to do some changes in the testrunner pod:
    *  -> Change the remoting port bind address.
    *  -> Change the http port bind address.
    *  -> set the jboss.dist property, needed by the CLI tests
    *  -> set the CLI credentials, needed by CLI tests
    *  -> add the additional users and groups needed by the integration tests.
    */
    public void preparePod(@Observes AfterStart aStart, ArquillianDescriptor descriptor, OpenShiftClient client) throws InterruptedException, CommandLineException, IOException {

        // Loading properties from arquillian.xml and setting the CommandContext
        final Map<String, String> props = descriptor.extension("prepare-pod").getExtensionProperties();
        final String username = getProperty(props, "username", DEFAULT_USERNAME);
        final String password = getProperty(props, "password", DEFAULT_PASSWORD);
        final String JBOSS_HOME = getProperty(props, "jbossHome", DEFAULT_POD_JBOSS_HOME);
        final String cubeId = getProperty(props, "cubeId", DEFAULT_CUBE_ID);

        // Setting Command Context
        ctx = getCtx(username, password);

        log.info("Preparing pod...." + descriptor.extension("prepare-pod") + ", " + aStart.getDeployableContainer());

        //adding the jboss.home and jboss.dist properties
        System.setProperty("jboss.home", JBOSS_HOME);
        System.setProperty("jboss.dist", JBOSS_HOME);

        // Setting the CLI credentials used in the org.jboss.as.test.integration.management.util.CLITestUtil
        System.setProperty("jboss.cli.username", username);
        System.setProperty("jboss.cli.password", password);

        // Changing the remoting, messaging and http port bind address so we can use it through port-forwarder
        if (executeCliCommand(CHANGE_REMOTING_INTERFACE) && executeCliCommand(CHANGE_HTTP_INTERFACE) && executeCliCommand(CHANGE_MESSAGING_INTERFACE)) {
            log.info("Command Successfully executed [ " + CHANGE_REMOTING_INTERFACE + "].");
            log.info("Command Successfully executed [ " + CHANGE_HTTP_INTERFACE + "].");
            log.info("Command Successfully executed [ " + CHANGE_MESSAGING_INTERFACE + "]. Reloading.");
            executeCliCommand(RELOAD_COMMAND);
            //wait 15 seconds before continue to make sure EAP is fully app and serving requests.
            Thread.sleep(15000);
        } else {
            log.info("Command execution failed.");
        }
    }

    /*
    * Execute CLI commands
    * @return true for success and false for fail
    * @throws CommandFormatException for malformed commands
    * @throws IOExeption for all other exceptions
    */
    private boolean executeCliCommand(String command) throws CommandFormatException, IOException {
        ModelControllerClient client = ctx.getModelControllerClient();
        ModelNode request = ctx.buildRequest(command);
        request.get("blocking").set(true);
        String commandOutcome = client.execute(request).get("outcome").toString();
        return true ? commandOutcome.contains("success") : false;
    }

    /*
    * Return the CLI connection
    * @param username
    * @param password
    * @throws CommandLineException
    */
    private CommandContext getCtx(String username, String password) throws CommandLineException {
        ctx = CommandContextFactory.getInstance().newCommandContext(username, password.toCharArray());
        ctx.connectController(TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort());
        return ctx;
    }

    /*
    * Return property from ArquillianExtension propsMap
    * @param props
    * @param key
    * @param defaultValue
    * If no configuration value is founf for the given key, its default value will be returned
    */
    private String getProperty(Map<String, String> props, String key, String defaultValue) {
        final String value = props.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
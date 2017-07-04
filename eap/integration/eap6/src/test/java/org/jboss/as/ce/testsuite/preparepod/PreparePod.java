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

package org.jboss.as.ce.testsuite.preparepod;

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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author spolti
 */
public class PreparePod {

    private static final Logger log = Logger.getLogger(PreparePod.class.getName());

    //list of commands
    private final List<String> commands = Arrays.asList(
            "/interface=public:undefine-attribute(name=nic)",
            "/interface=public:write-attribute(name=inet-address, value=127.0.0.1)",
            "/subsystem=remoting:write-attribute(name=worker-read-threads, value=8)",
            "/subsystem=remoting:write-attribute(name=worker-task-core-threads, value=8)",
            "/subsystem=remoting:write-attribute(name=worker-task-max-threads, value=64)",
            "/subsystem=remoting:write-attribute(name=worker-write-threads, value=8)",
            "/subsystem=remoting:write-attribute(name=worker-task-limit, value=2000)",
            "reload"
    );

    private CommandContext ctx;
    private String JBOSS_HOME;
    private String MANAGEMENT_USERNAME;
    private String MANAGEMENT_PASSWORD;
    private ModelControllerClient client;

    /*
    * To run the EAP integration tests against a EAP instance running on Opensfhift we have to do some changes in the testrunner pod:
    *  -> Change the remoting port bind address.
    *  -> Change the http port bind address.
    *  -> set the jboss.dist property, needed by the CLI tests
    *  -> set the CLI credentials, needed by CLI tests
    *  -> add the additional users and groups needed by the integration tests.
    */
    public void preparePod(@Observes AfterStart aStart, ArquillianDescriptor descriptor) throws InterruptedException, CommandLineException, IOException {
        //load properties from arquillian.xml
        loadProperties(descriptor);
        // set needed system properties
        setNeededSystemProps();
        // Setting Command Context
        ctx = getCtx(MANAGEMENT_USERNAME, MANAGEMENT_PASSWORD);
        // Execute all defined commands
        commands.stream().forEach(this::execute);

        // Make sure server is running before proceed
        boolean isServerReady = false;
        client = ctx.getModelControllerClient();
        while (!isServerReady) {
            try {
                Thread.sleep(1000);
                ModelNode request = ctx.buildRequest(":read-attribute(name=server-state)");
                String result = client.execute(request).get("result").toString();
                if (result.equals("\"running\"")) {
                    log.info("Server is ready.");
                    isServerReady = true;
                }
            } catch (final Exception e) {
                log.info("Server not ready yet, waiting 1 second, reason: " + e.getCause());
            }
        }
    }


    /*
    * Execute CLI commands
    * @returns true for success
    * @returns false for fail
    * @throws Exception for all exceptions
    */
    public void execute(String command) {
        client = ctx.getModelControllerClient();
        try {
            log.info("Trying to execute command [" + command + "]");
            ModelNode request = ctx.buildRequest(command);
            String result = client.execute(request).get("outcome").toString();
            if (!result.equals("\"success\"")) {
                throw new CommandLineException("Failed to execute the command " + command + ".");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    * Load properties properties from arquillian.xml configuration file
    * @param ArquillianDescriptor
    */
    private void loadProperties(ArquillianDescriptor descriptor) {
        // Loading properties from arquillian.xml and setting the CommandContext
        final Map<String, String> props = descriptor.extension("prepare-pod").getExtensionProperties();
        MANAGEMENT_USERNAME = getProperty(props, "username", "admin");
        MANAGEMENT_PASSWORD = getProperty(props, "password", "Admin#70365");
        JBOSS_HOME = getProperty(props, "jbossHome", "/opt/eap");
    }

    /*
    * Set the needed System Properties
    */
    private void setNeededSystemProps() {
        System.setProperty("jboss.home", JBOSS_HOME);
        System.setProperty("jboss.dist", JBOSS_HOME);
        //System.setProperty("jboss.inst", JBOSS_HOME);
        System.setProperty("jboss.cli.username", MANAGEMENT_USERNAME);
        System.setProperty("jboss.cli.password", MANAGEMENT_PASSWORD);
        //System.setProperty("jbossws.undefined.host", MANAGEMENT_ADDRESS);
        System.setProperty("module.path", JBOSS_HOME + "/modules");
        System.setProperty("jbossas.ts.submodule.dir", JBOSS_HOME + "/modules");
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
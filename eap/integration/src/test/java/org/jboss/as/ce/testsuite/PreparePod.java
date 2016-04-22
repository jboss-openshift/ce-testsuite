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

import com.squareup.okhttp.Response;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
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

    private final Logger log = Logger.getLogger(PreparePod.class.getName());

    private final String DEFAULT_USERNAME = "admin";
    private final String DEFAULT_PASSWORD = "Admin#70365";
    private final String DEFAULT_POD_JBOSS_HOME = "/opt/eap";
    private final String DEFAULT_CUBE_ID = "testrunner";

    private final String[] app_users = {"appuser=a2bd9ae9a89bfecaa3633d4bd49d327a","guest=b5d048a237bfd2874b6928e1f37ee15e",
            "user1=23624d2f74dfcb9688651a066d90b97e", "user2=ab3f9e12039435236d89de9023a304b7", "admin=779bbcdbf82f3da3990e94b29bceefe6"};
    private final String[] app_roles = {"appuser=appuser", "guest=guest","user1=Users,Role1", "user2=Users,Role2"};
    private final String[] mgt_user = {"testSuite=29a64f8524f32269aa9b681efc347f96"};
    private final String[] mgt_role = {"testSuite=SuperUser", "admin=admin"};

    private final String APP_USER_PROPERTIES_LOCATION = "/standalone/configuration/application-users.properties";
    private final String APP_ROLES_PROPERTIES_LOCATION = "/standalone/configuration/application-roles.properties";
    private final String MGMT_USER_PROPERTIES_LOCATION = "/standalone/configuration/mgmt-users.properties";
    private final String MGMT_ROLES_PROPERTIES_LOCATION = "/standalone/configuration/mgmt-groups.properties";

    //This is only for the tests running successfully using portforwarder. DO NOT DO IT IN PRODUCTION
    private final String CHANGE_REMOTING_INTERFACE = "/socket-binding-group=standard-sockets/socket-binding=remoting:write-attribute(name=interface,value=management)";
    private final String CHANGE_HTTP_INTERFACE = "/socket-binding-group=standard-sockets/socket-binding=http:write-attribute(name=interface,value=management)";
    private final String CHANGE_MESSAGING_INTERFACE = "/socket-binding-group=standard-sockets/socket-binding=messaging:write-attribute(name=interface,value=management)";
    private final String RELOAD_COMMAND = "reload";

    private CommandContext ctx;

    /*
    * To run the EAP integrationt tests against a we have to do some changes in the testrunner pod:
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

        ctx = getCtx(username, password);

        log.info("Preparing pod...." + descriptor.extension("prepare-pod") + ", " + aStart.getDeployableContainer());

        //adding the jboss.home and jboss.dist properties
        System.setProperty("jboss.home", JBOSS_HOME);
        System.setProperty("jboss.dist", JBOSS_HOME);

        // Setting the CLI credentials used in the org.jboss.as.test.integration.management.util.CLITestUtil
        System.setProperty("jboss.cli.username", username);
        System.setProperty("jboss.cli.password", password);

        // Changing the remoting and http port bind address
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

        //Adding the needed users to run the EAP integration tests
        log.info("Trying to add the needed users...");
        for( int i = 0; i< app_users.length; i++) {
            log.info("Executing the command [/bin/echo " + app_users[i] + " >> " + JBOSS_HOME + "/" + APP_USER_PROPERTIES_LOCATION + "] in the NameSpace [" + client.getClient().getNamespace() + "] " +
                    " against the pod [" + cubeId + "]");
            executeCommand(client, cubeId, "/bin/echo " + app_users[i] + " >> " + JBOSS_HOME + "/" + APP_USER_PROPERTIES_LOCATION);
        }

        for( int i = 0; i< app_roles.length; i++) {
            log.info("Executing the command [/bin/echo " + app_roles[i] + " >> " + JBOSS_HOME + "/" + APP_ROLES_PROPERTIES_LOCATION + "] in the NameSpace [" + client.getClient().getNamespace() + "] " +
                    " against the pod [" + cubeId + "]");
            executeCommand(client, cubeId, "/bin/echo " + app_roles[i] + " >> " + JBOSS_HOME + "/" + APP_ROLES_PROPERTIES_LOCATION);
        }

        for (int i = 0; i < mgt_user.length; i++) {
            log.info("Executing the command [/bin/echo " + mgt_user[i] + " >> " + JBOSS_HOME + "/" + MGMT_USER_PROPERTIES_LOCATION + "] in the NameSpace [" + client.getClient().getNamespace() + "] " +
                    " against the pod [" + cubeId + "]");
            executeCommand(client, cubeId, "/bin/echo " + mgt_user[i] + " >> " + JBOSS_HOME + "/" + MGMT_USER_PROPERTIES_LOCATION);
        }

        for (int i = 0; i < mgt_role.length; i++) {
            log.info("Executing the command [/bin/echo " + mgt_role[i] + " >> " + JBOSS_HOME + "/" + MGMT_ROLES_PROPERTIES_LOCATION + "] in the NameSpace [" + client.getClient().getNamespace() + "] " +
                    " against the pod [" + cubeId + "]");
            executeCommand(client, cubeId, "/bin/echo " + mgt_role[i] + " >> " + JBOSS_HOME + "/" + MGMT_ROLES_PROPERTIES_LOCATION);
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

    /*
    * Try to create the user in the target container using the kubernetes client to perform the commands
    * @param OpenshiftClient
    * @param cubeId
    * @param command
    */
    private void executeCommand(OpenShiftClient client, String cubeId, String command) throws InterruptedException {

        final String bash = "/bin/bash";
        final String bashC = "-c";

        //build the kubernetes config using the OpenshiftClient to get the required parameters
        Config config = new ConfigBuilder().withMasterUrl(String.valueOf(client.getClient().getMasterUrl()))
                .withNamespace(client.getClient().getNamespace())
                .withOauthToken(client.getClient().getConfiguration().getOauthToken())
                .build();

        //Execute the desired command
        try {
            try (final KubernetesClient kubeClient = new DefaultKubernetesClient(config);
                 ExecWatch watch = kubeClient.pods().withName(cubeId)
                         .readingInput(System.in)
                         .writingOutput(System.out)
                         .writingError(System.err)
                         .withTTY()
                         .usingListener(new SimpleListener())
                         .exec(bash, bashC, command)) {
                Thread.sleep(2 * 1000);
            }
        } catch (Exception e) {
            //do nothing
        }

    }

    private static class SimpleListener implements ExecListener {

        @Override
        public void onOpen(Response response) {
            System.out.println("The shell will remain open for 2 seconds.");
        }

        @Override
        public void onFailure(IOException e, Response response) {
            System.err.println("shell barfed");
        }

        @Override
        public void onClose(int code, String reason) {
            System.out.println("The shell will now close.");
        }
    }
}
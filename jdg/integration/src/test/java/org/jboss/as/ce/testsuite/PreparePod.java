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

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author dpalmer
 */
public class PreparePod {

    private static final Logger log = Logger.getLogger(PreparePod.class.getName());

    private final String DEFAULT_USERNAME = "admin";
    private final String DEFAULT_PASSWORD = "Admin#70365";
    private final String DEFAULT_HOTROD_PORT = "11222";

    //This is only for the tests running successfully using portforwarder. DO NOT DO IT IN PRODUCTION
    //private final String ADD_TRANSPORT_EXECUTOR = "/subsystem=infinispan/cache-container=clustered/transport=TRANSPORT:write-attribute(name=executor, value=infinispan-transport)";
    //private final String ADD_TRANSPORT_STACK = "/subsystem=infinispan/cache-container=clustered/transport=TRANSPORT:write-attribute(name=stack, value=udp)";
    //private final String ADD_TOPOLOGY_STATE_TRANSFER = "/subsystem=endpoint/hotrod-connector=hotrod-internal/topology-state-transfer=TOPOLOGY_STATE_TRANSFER:add(lazy-retrieval=false, lock-timeout=1000, replication-timeout=5000)";
    //private final String ADD_TESTCACHE_STATE_TRANSFER = "/subsystem=infinispan/cache-container=clustered/distributed-cache=testcache/state-transfer=STATE_TRANSFER:add(enabled=true, timeout=600000)";
    //private final String ADD_TESTCACHE_TRANSACTION = "/subsystem=infinispan/cache-container=clustered/distributed-cache=testcache/transaction=TRANSACTION:add(mode=NONE)";
    //private final String ADD_MEMCACHEDCACHE_STATE_TRANSFER = "/subsystem=infinispan/cache-container=clustered/distributed-cache=memcachedCache/state-transfer=STATE_TRANSFER:add(enabled=true, timeout=600000)";
    //private final String ADD_MEMCACHEDCACHE_TRANSACTION = "/subsystem=infinispan/cache-container=clustered/distributed-cache=memcachedCache/transaction=TRANSACTION:add(mode=NONE)";
    private final String CHANGE_HTTP_INTERFACE = "/socket-binding-group=standard-sockets/socket-binding=http:write-attribute(name=interface,value=management)";
    private final String CHANGE_HOTROD_INTERFACE = "/socket-binding-group=standard-sockets/socket-binding=hotrod-internal:write-attribute(name=interface,value=management)";
    private final String CHANGE_MEMCACHED_INTERFACE = "/socket-binding-group=standard-sockets/socket-binding=memcached:write-attribute(name=interface,value=management)";
    private final String REMOVE_MANAGEMENT_SECURITY = "/core-service=management/management-interface=native-interface:undefine-attribute(name=security-realm)";
    private final String RENAME_HOTROD_CONNECTOR = "/subsystem=endpoint/hotrod-connector=hotrod-internal:undefine-attribute(name=name)";
    private final String RELOAD_COMMAND = "reload";

    private CommandContext ctx;

    /*
    * To run the JDG integration tests against a JDG instance running on OpenShift we have to do some changes in the pod:
    *  -> Change the http port bind address.
    *  -> Remove the name from the hotrod connector.
    *  -> Remove the security from the management interface.
    */
    public void preparePod(@Observes AfterStart aStart, ArquillianDescriptor descriptor, ManagementClient client, Container container) throws InterruptedException, CommandLineException, IOException {
        // Loading properties from arquillian.xml and setting the CommandContext
        final Map<String, String> props = descriptor.extension("prepare-pod").getExtensionProperties();
        final String username = getProperty(props, "username", DEFAULT_USERNAME);
        final String password = getProperty(props, "password", DEFAULT_PASSWORD);
        final Map<String, String> containerProps = container.getContainerConfiguration().getContainerProperties();
        final String hotrodPort = getProperty(containerProps, "hotrodPort", DEFAULT_HOTROD_PORT);
        final String CHANGE_HOTROD_PORT = "/socket-binding-group=standard-sockets/socket-binding=hotrod-internal:write-attribute(name=port,value=" + hotrodPort + ")";

        final String HOST = client.getMgmtAddress();
        final int PORT = client.getMgmtPort();
        
        // Setting Command Context
        ctx = getCtx(username, password, HOST, PORT);

        log.info("Preparing pod...." + descriptor.extension("prepare-pod") + ", " + aStart.getDeployableContainer());

        if(!DEFAULT_HOTROD_PORT.equals(hotrodPort)) {
            executeCliCommand(CHANGE_HOTROD_PORT);
        }

        if (executeCliCommand(CHANGE_HTTP_INTERFACE) && executeCliCommand(CHANGE_HOTROD_INTERFACE) && executeCliCommand(CHANGE_MEMCACHED_INTERFACE) &&
            executeCliCommand(RENAME_HOTROD_CONNECTOR) && executeCliCommand(REMOVE_MANAGEMENT_SECURITY)) {
            log.info("Commands Successfully executed. Reloading...");
            executeCliCommand(RELOAD_COMMAND);
            //wait server gets ready
            while(!client.isServerInRunningState())
                Thread.sleep(1000);
        } else {
            log.info("Command execution failed.");
        }
    }

    /*
    * Execute CLI commands
    * @returns true for success
    * @returns false for fail
    * @throws CommandFormatException for malformed commands
    * @throws IOExeption for all other exceptions
    */
    private boolean executeCliCommand(String command) throws CommandFormatException, IOException, InterruptedException {
        log.info("Executing CLI command: " + command);
        ModelControllerClient client = ctx.getModelControllerClient();
        ModelNode request = ctx.buildRequest(command);
        String commandOutcome = client.execute(request).get("outcome").toString();
        log.info("Outcome: " + commandOutcome);
        return commandOutcome.contains("success");
    }

    /*
    * Return the CLI connection
    * @param username
    * @param password
    * @throws CommandLineException
    */
    private CommandContext getCtx(String username, String password, String host, int port) throws CommandLineException {
        ctx = CommandContextFactory.getInstance().newCommandContext(username, password.toCharArray());
        ctx.connectController(host, port);
        return ctx;
    }

    /*
    * Return property from ArquillianExtension propsMap
    * @param props
    * @param key
    * @param defaultValue
    * If no configuration value is found for the given key, its default value will be returned
    */
    private String getProperty(Map<String, String> props, String key, String defaultValue) {
        final String value = props.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
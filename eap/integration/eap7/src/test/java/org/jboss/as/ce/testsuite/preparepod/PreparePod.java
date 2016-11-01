package org.jboss.as.ce.testsuite.preparepod;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.CommandContextConfiguration;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author spolti
 */
public class PreparePod {

    private final Logger log = Logger.getLogger(PreparePod.class.getName());

    //list of commands
    private final List<String> commands = Arrays.asList(
            // make all interfaces public to allow connection through port forwarder
            "/interface=public:write-attribute(name=inet-address, value=127.0.0.1)",

            //disable the logging to do not overload the pod with I/O operations. Enable for debugging purposes
            //"/subsystem=logging/root-logger=ROOT:write-attribute(name=level,value=OFF)",

            // create a new io-thread-pool for remoting connections
            "/subsystem=io/worker=remoting:add(io-threads=800, stack-size=64,task-max-threads=2000)",
            "/subsystem=remoting/configuration=endpoint:write-attribute(name=worker, value=remoting)",

            // create a new io-thread-pool for ejb connections
            "/subsystem=io/worker=ejb-worker:add(io-threads=800, stack-size=64, task-max-threads=2000)",
            "/subsystem=ejb3/service=remote:write-attribute(name=thread-pool-name, value=ejb-worker)",

            // increase the ejb thread pool, the default is to small
            "/subsystem=ejb3/thread-pool=default:write-attribute(name=max-threads, value=2000)",

            // increase the undertow's buffers values.
            "/subsystem=undertow/buffer-cache=default:write-attribute(name=buffer-size, value=1001920)",
            "/subsystem=undertow/buffer-cache=default:write-attribute(name=buffers-per-region, value=10024)",
            "/subsystem=undertow/buffer-cache=default:write-attribute(name=max-regions, value=256)",

            // no retries on auth to decrease the overhead
            "/subsystem=remoting/configuration=endpoint:write-attribute(name=authentication-retries, value=0)",

            ":reload"
    );

    private CommandContext ctx;
    private String JBOSS_HOME;
    private String MANAGEMENT_USERNAME;
    private String MANAGEMENT_PASSWORD;
    private String MANAGEMENT_ADDRESS;
    private String MANAGEMENT_PORT;
    private int CLI_CONNECTION_TIMEOUT;

    /*
    * Configure the pod before run the tests.
    */
    private void preparePod(@Observes AfterStart aStart, ArquillianDescriptor descriptor) throws InterruptedException {
        //load properties from arquillian.xml
        loadProperties(descriptor);
        // set needed system properties
        setNeededSystemProps();
        // Setting Command Context
        ctx = getCtx();
        // Execute all defined commands
        commands.stream().forEach(this::execute);
        //wait server gets ready
        // TODO maybe run the probes or something to do a real liveness check?
        Thread.sleep(20000);
    }

    /*
    * Execute CLI commands
    * @returns true for success
    * @returns false for fail
    * @throws Exception for all exceptions
    */
    public void execute(String command) {
        ModelControllerClient client = ctx.getModelControllerClient();
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
    * @throws Exception
    */
    private CommandContext getCtx() {
        CommandContextConfiguration config = new CommandContextConfiguration.Builder().
                setController("http-remoting://"
                        + MANAGEMENT_ADDRESS + ":"
                        + MANAGEMENT_PORT)
                .setUsername(MANAGEMENT_USERNAME)
                .setPassword(MANAGEMENT_PASSWORD.toCharArray())
                .setConnectionTimeout(CLI_CONNECTION_TIMEOUT)
                .build();
        try {
            ctx = CommandContextFactory.getInstance().newCommandContext(config);
            ctx.connectController();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ctx;
    }

    /*
    * Set the needed System Properties
    */
    private void setNeededSystemProps() {
        System.setProperty("jboss.home", JBOSS_HOME);
        System.setProperty("jboss.dist", JBOSS_HOME);
        System.setProperty("jboss.inst", JBOSS_HOME);
        System.setProperty("jboss.management.user", MANAGEMENT_USERNAME);
        System.setProperty("jboss.management.password", MANAGEMENT_PASSWORD);
        System.setProperty("jbossws.undefined.host", MANAGEMENT_ADDRESS);
        System.setProperty("module.path", JBOSS_HOME + "/modules");
    }

    /*
    * Load properties properties from arquillian.xml configuration file
    * @param ArquillianDescriptor
    */
    private void loadProperties(ArquillianDescriptor descriptor) {
        final Map<String, String> props = descriptor.extension("prepare-pod").getExtensionProperties();
        JBOSS_HOME = getProperty(props, "jbossHome", "/opt/eap/");
        MANAGEMENT_USERNAME = getProperty(props, "username", "admin");
        MANAGEMENT_PASSWORD = getProperty(props, "password", "Admin#70365");
        MANAGEMENT_ADDRESS = getProperty(props, "managementAddress", "localhost");
        MANAGEMENT_PORT = getProperty(props, "managementPort", "9990");
        CLI_CONNECTION_TIMEOUT = Integer.parseInt(getProperty(props,"cliConnectionTimeout", "10000")); //default 10 seconds
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
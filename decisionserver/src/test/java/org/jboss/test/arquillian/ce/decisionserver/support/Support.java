package org.jboss.test.arquillian.ce.decisionserver.support;

import org.jboss.arquillian.ce.api.ConfigurationHandle;
import org.jboss.arquillian.ce.api.Tools;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.decisionserver.DecisionServerTestBase;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.internal.command.CommandFactory;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;
import org.openshift.quickstarts.decisionserver.hellorules.Greeting;
import org.openshift.quickstarts.decisionserver.hellorules.Person;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Filippe Spolti
 */
public abstract class Support {

    private static final Logger log = Logger.getLogger(Support.class.getCanonicalName());
    protected static final String FILENAME = "kie.properties";

    //kie-server credentials
    protected static final String KIE_USERNAME = System.getProperty("kie.username", "kieserver");
    protected static final String KIE_PASSWORD = System.getProperty("kie.password", "Redhat@123");
    // AMQ credentials
    public static final String MQ_USERNAME = System.getProperty("mq.username", "kieserver");
    public static final String MQ_PASSWORD = System.getProperty("mq.password", "Redhat@123");

    public String AMQ_HOST = "tcp://kie-app-amq-tcp:61616";
    public Person person = new Person();

    @ArquillianResource
    protected ConfigurationHandle configuration;

    protected static WebArchive getDeploymentInternal() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML("web.xml");
        war.addClass(Support.class);
        war.addPackage(Person.class.getPackage());
        war.addAsLibraries(Libraries.transitive("org.kie.server", "kie-server-client"));
        return war;
    }

    /*
    * Returns the kieService client
    */
    protected KieServicesClient getKieRestServiceClient() throws Exception {
        Properties properties = Tools.loadProperties(DecisionServerTestBase.class, FILENAME);
        String username = properties.getProperty("kie.username");
        String password = properties.getProperty("kie.password");
        KieServicesConfiguration kieServicesConfiguration = KieServicesFactory.newRestConfiguration(resolveHost(), username, password);
        kieServicesConfiguration.setMarshallingFormat(MarshallingFormat.XSTREAM);
        return KieServicesFactory.newKieServicesClient(kieServicesConfiguration);
    }

    /*
    * Returns the JMS kieService client
    */
    public KieServicesClient getKieJmsServiceClient() throws NamingException {
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        props.setProperty(Context.PROVIDER_URL, AMQ_HOST);
        props.setProperty(Context.SECURITY_PRINCIPAL, KIE_USERNAME );
        props.setProperty(Context.SECURITY_CREDENTIALS, KIE_PASSWORD);
        InitialContext context = new InitialContext(props);
        ConnectionFactory connectionFactory = (ConnectionFactory)context.lookup("ConnectionFactory");
        javax.jms.Queue requestQueue = (javax.jms.Queue)context.lookup("dynamicQueues/queue/KIE.SERVER.REQUEST");
        javax.jms.Queue responseQueue = (javax.jms.Queue)context.lookup("dynamicQueues/queue/KIE.SERVER.RESPONSE");
        KieServicesConfiguration config = KieServicesFactory.newJMSConfiguration(connectionFactory, requestQueue, responseQueue, MQ_USERNAME, MQ_PASSWORD);
        config.setMarshallingFormat(MarshallingFormat.XSTREAM);
        return KieServicesFactory.newKieServicesClient(config);
    }

    protected String getDecisionserverRouteHost() {
        return "http://kie-app-%s.router.default.svc.cluster.local/kie-server/services/rest/server";
    }

    /*
    * Return the resolved endpoint's host/uri
    */
    protected String resolveHost() {
        String resolvedHost = String.format(getDecisionserverRouteHost(), configuration.getNamespace());
        log.info("Testing against URL: " + resolvedHost);
        return resolvedHost;
    }

    /*
    * Return the RuleServicesClient
    */
    public RuleServicesClient getRuleServicesClient(KieServicesClient client) {
        return client.getServicesClient(RuleServicesClient.class);
    }

    /*
     * Return the classes used in the MarshallerFactory
     */
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(Person.class);
        classes.add(Greeting.class);
        return classes;
    }

    /*
     * Return the batch command used to fire rules
     */
    public BatchExecutionCommand batchCommand() {
        person.setName("Filippe Spolti");
        List<Command<?>> commands = new ArrayList<>();
        commands.add((Command<?>) CommandFactory.newInsert(person));
        commands.add((Command<?>) CommandFactory.newFireAllRules());
        commands.add((Command<?>) CommandFactory.newQuery("greetings", "get greeting"));
        return CommandFactory.newBatchExecution(commands, "HelloRulesSession");
    }

    protected void prepareClientInvocation() throws Exception {
        // do nothing in basic
    }
}
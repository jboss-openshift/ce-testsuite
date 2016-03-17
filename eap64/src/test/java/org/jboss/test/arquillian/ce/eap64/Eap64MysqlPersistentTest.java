package org.jboss.test.arquillian.ce.eap64;

import java.net.URL;
import java.util.UUID;
import java.util.logging.Logger;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.common.TodoList;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jonh Wendell
 */

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/eap/eap64-mysql-persistent-s2i.json", parameters = {
        @TemplateParameter(name = "HTTPS_NAME", value = "jboss"),
        @TemplateParameter(name = "HTTPS_PASSWORD", value = "mykeystorepass") })
@OpenShiftResource("classpath:eap-app-secret.json")
public class Eap64MysqlPersistentTest {
    private final static Logger log = Logger.getLogger(Eap64MysqlPersistentTest.class.getName());

    @RouteURL("eap-app")
    private URL url;

    @RouteURL("secure-eap-app")
    private URL secureUrl;

    private final static String summary1 = UUID.randomUUID().toString();
    private final static String summary2 = UUID.randomUUID().toString();
    private final static String description1 = UUID.randomUUID().toString();
    private final static String description2 = UUID.randomUUID().toString();

    @Test
    @RunAsClient
    @InSequence(1)
    public void InsertItems() throws Exception {
        log.info("INSERTING SUMMARY1 " + summary1);
        TodoList.insertItem(url.toString(), summary1, description1);
        TodoList.insertItem(secureUrl.toString(), summary2, description2);
    }

    private void restartPod(OpenShiftHandle adapter, String name) throws Exception {
        log.info("Scaling down " + name);
        adapter.scaleDeployment(name, 0);
        log.info("Scaling up " + name);
        adapter.scaleDeployment(name, 1);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void restartPods(@ArquillianResource OpenShiftHandle adapter) throws Exception {
        restartPod(adapter, "eap-app-mysql");
        restartPod(adapter, "eap-app");
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void verifyItems() throws Exception {
        log.info("Checking summary " + summary1);
        TodoList.checkItem(url.toString(), summary1, description1);
        log.info("Checking summary " + summary2);
        TodoList.checkItem(secureUrl.toString(), summary2, description2);
    }
}

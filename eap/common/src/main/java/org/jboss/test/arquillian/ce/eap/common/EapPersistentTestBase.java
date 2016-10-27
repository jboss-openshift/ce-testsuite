package org.jboss.test.arquillian.ce.eap.common;

import java.net.URL;
import java.util.UUID;
import java.util.logging.Logger;

import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;

/**
 * @author Jonh Wendell
 * @author Marko Luksa
 */
public abstract class EapPersistentTestBase {

    private final static Logger log = Logger.getLogger(org.jboss.test.arquillian.ce.eap.common.EapPersistentTestBase.class.getName());

    protected abstract String[] getRCNames();

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
        for (String rc : getRCNames()) {
            restartPod(adapter, rc);
        }
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void verifyItems() throws Exception {
        TodoList.checkItem(url.toString(), summary1, description1);
        TodoList.checkItem(secureUrl.toString(), summary2, description2);
    }
}

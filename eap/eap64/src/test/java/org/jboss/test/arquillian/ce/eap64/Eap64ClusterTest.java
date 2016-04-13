package org.jboss.test.arquillian.ce.eap64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.jboss.arquillian.ce.api.ConfigurationHandle;
import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.RoleBindings;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.ce.httpclient.HttpClient;
import org.jboss.arquillian.ce.httpclient.HttpClientBuilder;
import org.jboss.arquillian.ce.httpclient.HttpRequest;
import org.jboss.arquillian.ce.httpclient.HttpResponse;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jonh Wendell
 */

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/eap/eap64-basic-s2i.json", parameters = {
        @TemplateParameter(name = "SOURCE_REPOSITORY_URL", value = "https://github.com/jboss-openshift/openshift-examples"),
        @TemplateParameter(name = "SOURCE_REPOSITORY_REF", value = "master"),
        @TemplateParameter(name = "CONTEXT_DIR", value = "eap-tests/cluster1") })
@OpenShiftResource("classpath:eap-app-secret.json")
@RoleBindings({ @RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default"),
        @RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:eap-service-account") })
public class Eap64ClusterTest {
    private static final Logger log = Logger.getLogger(Eap64ClusterTest.class.getName());

    @ArquillianResource
    OpenShiftHandle adapter;

    @ArquillianResource
    ConfigurationHandle config;

    private String token;
    private List<String> pods;
    private HttpClient client;

    @Before
    public void Setup() throws Exception {
        token = config.getToken();
        assertFalse("Auth token must be provided", token.isEmpty());

        client = HttpClientBuilder.untrustedConnectionClient();
    }

    /**
     * This test starts two pods; insert a value into the first pod's session.
     * Then it retrieves this value from the second pod.
     * 
     * After that it starts a third pod and try to get the value from it, to see
     * if session replication is working at pod's startup as well.
     * 
     * @throws Exception
     */
    @Test
    @RunAsClient
    public void testSession() throws Exception {
        // Start with 2 pods
        scale(2);

        final String valueToCheck = UUID.randomUUID().toString();

        // Insert a session value into the first pod
        String servletUrl = buildURL(pods.get(0));
        HttpRequest request = HttpClientBuilder.doPOST(servletUrl);
        request.setHeader("Authorization", "Bearer " + token);
        Map<String, String> params = new HashMap<>();
        params.put("key", valueToCheck);
        request.setEntity(params);
        HttpResponse response = client.execute(request);
        assertEquals("OK", response.getResponseBodyAsString());

        String cookie = response.getHeader("Set-Cookie");

        // Retrieve the value from the second pod
        assertEquals(valueToCheck, retrieveKey(1, cookie));

        // Now start a third shiny new pod and retrieve the value from it
        scale(3);
        assertEquals(valueToCheck, retrieveKey(2, cookie));
    }

    private void scale(int replicas) throws Exception {
        adapter.scaleDeployment("eap-app", replicas);
        pods = getPods();
    }

    private String retrieveKey(int podIndex, String cookie) throws Exception {
        String servletUrl = buildURL(pods.get(podIndex));
        HttpRequest request = HttpClientBuilder.doGET(servletUrl);
        request.setHeader("Authorization", "Bearer " + token);
        request.setHeader("Cookie", cookie);
        return client.execute(request).getResponseBodyAsString();
    }

    private List<String> getPods() throws Exception {
        List<String> pods = adapter.getPods();
        pods.removeIf(p -> p.endsWith("-build") || !p.startsWith("eap-app-"));

        return pods;
    }

    private String buildURL(String podName) {
        final String PROXY_URL = "%s/api/%s/namespaces/%s/pods/%s:%s/proxy%s";
        return String.format(PROXY_URL, config.getKubernetesMaster(), config.getApiVersion(), config.getNamespace(),
                podName, 8080, "/cluster1/StoreInSession");
    }

    /**
     * This test starts with a high number of pods. We do lots of HTTP requests
     * sequentially, with a delay of 1 second between them.
     * 
     * In paralel, after every N requests we scale down the cluster by 1 pod.
     * This happens in another thread and continues until we reach only one pod
     * in activity.
     * 
     * The HTTP requests must continue to work correctly, as the openshift router
     * should redirect them to any working pod.
     * 
     * @param url
     * @throws Exception
     */
    @Test
    @RunAsClient
    @Ignore
    public void testAppStillWorksWhenScalingDown(@RouteURL("eap-app") URL url) throws Exception {
        // Number of HTTP requests we are going to do
        final int REQUESTS = 100;
        // Initial number of replicas, will decrease over time until 1
        final int REPLICAS = 5;
        // Decrement the number of replicas on each STEP requests
        final int STEP = REQUESTS / REPLICAS;

        // Setup initial state
        int replicas = REPLICAS;
        adapter.scaleDeployment("eap-app", replicas);

        HttpRequest request = HttpClientBuilder.doGET(url.toString() + "/cluster1/Hi");

        // Do the requests
        for (int i = 1; i <= REQUESTS; i++) {
            HttpResponse response = client.execute(request);
            assertEquals(200, response.getResponseCode());

            String body = response.getResponseBodyAsString();
            log.info(String.format("Try %d -  GOT: %s", i, body));
            assertTrue(body.startsWith("Served from node: "));

            if (i % STEP == 0 && replicas > 1) {
                replicas--;
                (new ScaleTo(replicas)).start();
            }

            Thread.sleep(1000);
        }
    }

    private class ScaleTo extends Thread {
        int r;

        public ScaleTo(int r) {
            log.info(String.format("ScaleTo created with %d replicas\n", r));
            this.r = r;
        }

        @Override
        public void run() {
            try {
                adapter.scaleDeployment("eap-app", r);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

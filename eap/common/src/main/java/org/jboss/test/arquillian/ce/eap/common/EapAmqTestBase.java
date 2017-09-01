package org.jboss.test.arquillian.ce.eap.common;

import java.net.URL;
import java.util.HashMap;

import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.ce.api.OpenShiftHandle;
import org.jboss.arquillian.ce.httpclient.HttpClientBuilder;
import org.jboss.arquillian.ce.httpclient.HttpClientExecuteOptions;
import org.jboss.arquillian.ce.httpclient.HttpRequest;
import org.jboss.arquillian.ce.httpclient.HttpResponse;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jonh Wendell
 * @author Marko Luksa
 */
public class EapAmqTestBase {
    private final HttpClientExecuteOptions execOptions = new HttpClientExecuteOptions.Builder().tries(3)
            .desiredStatusCode(200).delay(10).build();

    @RouteURL("eap-app")
    private URL url;

    @RouteURL("secure-eap-app")
    private URL secureUrl;

    @ArquillianResource
    OpenShiftHandle adapter;

    @Test
    @RunAsClient
    @InSequence(1)
    public void testPageRendering() throws Exception {
        actualTestPageRendering(url.toString());
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void testSecurePageRendering() throws Exception {
        actualTestPageRendering(secureUrl.toString());
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void testLogMessages() throws Exception {
        testLogContains("Received Message from queue: This is message ");
        testLogContains("Received Message from topic: This is message ");
    }

    private void testLogContains(String prefix) throws Exception {
        final int TRIES = 5;

        HashMap<String, String> labels = new HashMap<String, String>();
        labels.put("deploymentconfig", "eap-app");

        int i = 0;
        while (i < TRIES) {
            String podLog = adapter.getLog("eap-app", labels);
            if (messagesExist(podLog, prefix))
                break;
            Thread.sleep(5 * 1000);
            i++;
        }
        if (i >= TRIES)
            throw new Exception(String.format("Message '%s' not found in log", prefix));
    }

    private boolean messagesExist(String podLog, String prefix) {
        for (int i = 1; i <= 5; i++) {
            if (!podLog.contains(prefix + i))
                return false;
        }
        return true;
    }

    private void actualTestPageRendering(String baseUrl) throws Exception {
        testPageContains(baseUrl, "URL=HelloWorldMDBServletClient");
        testPageContains(baseUrl + "/HelloWorldMDBServletClient", "<em>queue://queue/HELLOWORLDMDBQueue</em>");
        testPageContains(baseUrl + "/HelloWorldMDBServletClient?topic", "<em>topic://topic/HELLOWORLDMDBTopic</em>");
    }

    private void testPageContains(String url, String text) throws Exception {
        HttpRequest request = HttpClientBuilder.doGET(url);
        HttpResponse response = HttpClientBuilder.untrustedConnectionClient().execute(request, execOptions);

        Assert.assertEquals(200, response.getResponseCode());

        String responseString = response.getResponseBodyAsString();
        Assert.assertTrue(responseString.contains(text));
    }
}

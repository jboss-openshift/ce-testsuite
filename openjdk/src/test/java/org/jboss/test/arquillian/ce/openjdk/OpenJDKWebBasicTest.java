package org.jboss.test.arquillian.ce.openjdk;

import org.jboss.arquillian.junit.Arquillian;

import java.net.URL;

import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.runner.RunWith;
import org.junit.Test;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/openjdk/openjdk18-web-basic-s2i.json")

public class OpenJDKWebBasicTest extends OpenJDKTest {
    private final String MSG = "Hello World";

    @Test
    @RunAsClient
    public void testRoute(@RouteURL("openjdk-app") URL url) throws Exception {
        checkResponse(url.toString(), MSG);
    }
    
    @Test
    public void testService() throws Exception {
        String host = System.getenv("OPENJDK_APP_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("OPENJDK_APP_SERVICE_PORT"));
        String url = "http://" + host + ":" + port;
        checkResponse(url, MSG);
    }
}

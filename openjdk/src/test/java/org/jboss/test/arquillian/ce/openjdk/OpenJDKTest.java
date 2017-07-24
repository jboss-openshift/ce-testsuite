package org.jboss.test.arquillian.ce.openjdk;

import java.util.logging.Logger;

import org.jboss.arquillian.ce.httpclient.HttpClientBuilder;
import org.jboss.arquillian.ce.httpclient.HttpRequest;
import org.jboss.arquillian.ce.httpclient.HttpResponse;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.logmanager.Level;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;

public class OpenJDKTest {

    private Logger log = Logger.getLogger(getClass().getName());

    @Deployment
    public static WebArchive getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML(new StringAsset("<web-app/>"));
        war.addClass(OpenJDKTest.class);
        war.addClass(OpenJDKWebBasicTest.class);
        war.addPackage(HttpClientBuilder.class.getPackage());
        war.addAsLibraries(Libraries.transitive("org.apache.httpcomponents", "httpclient"));
        return war;
    }

    protected void checkResponse(String url, String expected) throws Exception {
        HttpRequest request = HttpClientBuilder.doGET(url);
        HttpResponse response = HttpClientBuilder.untrustedConnectionClient().execute(request);
        String responseString = response.getResponseBodyAsString();
    
        log.log(Level.INFO, responseString);
        Assert.assertNotNull(responseString);
        Assert.assertEquals(expected, responseString);        
    }

}

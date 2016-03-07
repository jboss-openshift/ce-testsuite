package org.jboss.test.arquillian.ce.webserver;

import junit.framework.Assert;
import org.jboss.arquillian.ce.api.*;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Created by fspolti on 3/7/16.
 */
@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/webserver/jws30-tomcat7-basic-s2i.json",
        labels = "application=jws-app"
)
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:jws-service-account")
@OpenShiftResources({
        @OpenShiftResource("classpath:webserver-service-account.json"),
        @OpenShiftResource("classpath:webserver-app-secret.json")
})
public class WebServerTest {


    @ArquillianResource
    ConfigurationHandle configuration;

    @RouteURL
    private String ROUTE_URL;

    @Deployment
    public static WebArchive getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        return war;
    }

    @Test
    public void firstTest() {


        System.out.println("ROUTE: " + ROUTE_URL);
        Assert.assertTrue(true);
    }

}
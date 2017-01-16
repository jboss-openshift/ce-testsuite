package org.jboss.test.arquillian.ce.eap64;

import java.net.URL;

import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.RoleBindings;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.test.arquillian.ce.eap.common.EapClusteringTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Jonh Wendell
 */
@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/eap/eap64-basic-s2i.json", parameters = {
        @TemplateParameter(name = "SOURCE_REPOSITORY_URL", value = "https://github.com/jboss-openshift/openshift-examples"),
        @TemplateParameter(name = "SOURCE_REPOSITORY_REF", value = "master"),
        @TemplateParameter(name = "CONTEXT_DIR", value = "eap-tests/cluster1")})
@OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/eap-app-secret.json")
@RoleBindings({
        @RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default"),
        @RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:eap-service-account")})
public class Eap64ClusteringTest extends EapClusteringTestBase {


    /**
     * This one tests the behavior when a client is doing a long HTTP request
     * and the pod which is serving it dies (or, in this case it's killed).
     * <p/>
     * We have two expected results:
     * <p/>
     * (1) - If the request is shorter than 60 seconds it should be OK, because
     * openshift has a graceful timeout of 60 seconds by default.
     * <p/>
     * (2) - If the request is longer than 60 seconds we should expect a
     * disconnect in our HTTP connection.
     *
     * @param url route url
     * @throws Exception
     */
    @Test
    @RunAsClient
    @InSequence(2)
    public void testLongRequest(@RouteURL("eap-app") URL url) throws Exception {
        final int DELAY_BETWEEN_REQUESTS = 5;
        final String serviceUrl = url.toString();

        scale(2);

        int stars = doDelayRequest(serviceUrl, 20);
        assertEquals("Number of stars should match number of seconds", 20, stars);

        log.info(String.format("Waiting %d seconds before doing the second long request", DELAY_BETWEEN_REQUESTS));
        Thread.sleep(DELAY_BETWEEN_REQUESTS * 1000);

        stars = doDelayRequest(serviceUrl, 200);
        assertFalse(String.format("Number of stars (%d) should not match number of seconds (%d)", stars, 200), 200 == stars);
    }


}

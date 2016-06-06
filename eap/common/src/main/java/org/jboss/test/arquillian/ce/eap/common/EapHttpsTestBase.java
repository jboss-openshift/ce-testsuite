package org.jboss.test.arquillian.ce.eap.common;

import java.net.URL;

import org.jboss.arquillian.ce.cube.RouteURL;

/**
 * @author Jonh Wendell
 * @author Marko Luksa
 */
public class EapHttpsTestBase extends EapBasicTestBase {

    @RouteURL("secure-eap-app")
    private URL url;

    protected URL getUrl() {
        return url;
    }
}

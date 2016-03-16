/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.test.arquillian.ce.sso;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.test.arquillian.ce.sso.support.Client;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/jboss-openshift/application-templates/master/sso/demo/sso70-all-in-one-demo.json",
    labels = "application=helloworld,component=eap",
    parameters = {
        @TemplateParameter(name = "HOSTNAME_HTTP", value = "helloworld${openshift.domain}"),
        @TemplateParameter(name = "HOSTNAME_HTTPS", value = "secure-helloworld${openshift.domain}"),
        @TemplateParameter(name = "SSO_HOSTNAME_HTTP", value = "sso${openshift.domain}"),
        @TemplateParameter(name = "SSO_HOSTNAME_HTTPS", value = "secure-sso${openshift.domain}"),
        @TemplateParameter(name = "SSO_URI", value = "https://secure-sso${openshift.domain}/auth"),
        @TemplateParameter(name = "ARTIFACT_DIR", value = "app-jee/target,app-profile-jee/target"),
        @TemplateParameter(name = "APPLICATION_ROUTES", value = "http://helloworld${openshift.domain};https://secure-helloworld${openshift.domain}")
    })
@OpenShiftResources({
    @OpenShiftResource("classpath:sso-service-account.json"),
    @OpenShiftResource("classpath:sso-app-secret.json"),
    @OpenShiftResource("classpath:sso-demo-secret.json"),
    @OpenShiftResource("classpath:eap-app-secret.json")
})
public class SsoAllInOneTest extends SsoEapTestBase {

    public SsoAllInOneTest() {
    }

    @RouteURL("helloworld")
    private URL routeURL;

    @RouteURL("secure-helloworld")
    private URL secureRouteURL;

    @Override
    protected URL getRouteURL() {
        return routeURL;
    }

    @Override
    protected URL getSecureRouteURL() {
        return secureRouteURL;
    }

    @Test
    @RunAsClient
    public void testLogin() throws Exception {
        login(getRouteURL().toString());
    }

    @Test
    @RunAsClient
    public void testSecureLogin() throws Exception {
        login(getRouteURL().toString());
    }

    protected void login(String host) throws Exception {
        Client client = new Client(host);

        String result = client.get("app-profile-jee/profile.jsp");
        assertTrue(result.contains("kc-form-login"));
    }

    @Test
    @RunAsClient
    public void testOidcLogin() throws Exception {
        oidcLogin(getRouteURL().toString(), "redirect_uri=http%3A%2F%2Fhelloworld" + System.getProperty("openshift.domain") + "%2Fapp-profile-jee%2Fprofile.jsp");
    }

    @Test
    @RunAsClient
    public void testSecureOidcLogin() throws Exception {
        oidcLogin(getSecureRouteURL().toString(), "redirect_uri=https%3A%2F%2Fsecure-helloworld" + System.getProperty("openshift.domain") + "%2Fapp-profile-jee%2Fprofile.jsp");
    }

    protected void oidcLogin(String host, String expected) throws Exception {
        Client client = new Client(host);

        Map<String, String> params = new HashMap<>();
        params.put("username", "demouser");
        params.put("password", "demopass");
        params.put("login", "submit");

        String result = client.post("app-profile-jee/profile.jsp", params);
        assertTrue(result.contains(expected));
    }

    @Test
    @RunAsClient
    public void testAccessType() throws Exception {
        String host = "sso" + System.getProperty("openshift.domain");

        Map<String, String> params = new HashMap<>();
        params.put("username", "admin");
        params.put("password", "admin");
        params.put("grant_type", "password");
        params.put("client_id", "admin-cli");

        Client client = new Client("http://" + host + "/auth");
        String result = client.post("realms/master/protocol/openid-connect/token", params);

        assertFalse(result.contains("error_description"));
        assertTrue(result.contains("access_token"));

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(result);
        String accessToken = (String) jsonObject.get("access_token");

        params = new HashMap<>();
        params.put("Accept", "application/json");
        params.put("Authorization", "Bearer " + accessToken);

        result = client.get("admin/realms/demo/clients", params);
        Iterator clients = ((JSONArray) jsonParser.parse(result)).iterator();
        while (clients.hasNext()) {
            jsonObject = (JSONObject) clients.next();
            if ((jsonObject.get("clientId")).equals("app-jee")) {
                assertEquals(jsonObject.get("publicClient"), Boolean.FALSE);
                assertEquals(jsonObject.get("bearerOnly"), Boolean.FALSE);
                return;
            }
        }

        fail("ClientId app-jee not found");
    }

}

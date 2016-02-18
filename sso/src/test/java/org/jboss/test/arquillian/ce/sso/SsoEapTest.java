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

import org.jboss.arquillian.ce.api.ExternalDeployment;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ExternalDeployment
@Template(url = "https://raw.githubusercontent.com/bdecoste/application-templates/ssoTP/eap/eap64-sso-s2i.json",
		labels = "application=ssoeap",
		parameters = {
                @TemplateParameter(name = "APPLICATION_NAME", value="ssoeap"),
                @TemplateParameter(name = "HOSTNAME_HTTP", value="ssoeap${openshift.domain}"),
                @TemplateParameter(name = "HOSTNAME_HTTPS", value="secure-ssoeap${openshift.domain}"),
                @TemplateParameter(name = "ARTIFACT_DIR", value="app-jee/target,app-profile-jee/target"),
                @TemplateParameter(name = "SSO_REALM", value="demo"),
                @TemplateParameter(name = "SSO_URI", value="http://localhost:8080/auth"),
                @TemplateParameter(name = "SSO_PUBLIC_KEY", value="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiLezsNQtZSaJvNZXTmjhlpIJnnwgGL5R1vkPLdt7odMgDzLHQ1h4DlfJPuPI4aI8uo8VkSGYQXWaOGUh3YJXtdO1vcym1SuP8ep6YnDy9vbUibA/o8RW6Wnj3Y4tqShIfuWf3MEsiH+KizoIJm6Av7DTGZSGFQnZWxBEZ2WUyFt297aLWuVM0k9vHMWSraXQo78XuU3pxrYzkI+A4QpeShg8xE7mNrs8g3uTmc53KR45+wW1icclzdix/JcT6YaSgLEVrIR9WkkYfEGj3vSrOzYA46pQe6WQoenLKtIDFmFDPjhcPoi989px9f+1HCIYP0txBS/hnJZaPdn5/lEUKQIDAQAB"),
                @TemplateParameter(name = "IMAGE_STREAM_NAMESPACE", value="${kubernetes.namespace}")})
@OpenShiftResources({
        @OpenShiftResource("classpath:eap-app-secret.json"),
        @OpenShiftResource("classpath:ssoeap-image-stream.json")
}) 
public class SsoEapTest extends SsoEapBaseTest 
{
	
	public SsoEapTest() {
		route = "ssoeap";
		secureRoute = "secure-ssoeap";
	}

}

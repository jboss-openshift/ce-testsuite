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
@Template(url = "https://raw.githubusercontent.com/bdecoste/application-templates/ssoTP/sso/sso70-mysql.json",
		labels = "application=ssomysql,component=server",
        parameters = {
                @TemplateParameter(name = "APPLICATION_NAME", value="ssomysql"),
                @TemplateParameter(name = "HOSTNAME_HTTP", value="ssomysql${openshift.domain}"),
                @TemplateParameter(name = "HOSTNAME_HTTPS", value="secure-ssomysql${openshift.domain}"),
                @TemplateParameter(name = "HTTPS_NAME", value="jboss"),
                @TemplateParameter(name = "HTTPS_PASSWORD", value="mykeystorepass"),
                @TemplateParameter(name = "IMAGE_STREAM_NAMESPACE", value="${kubernetes.namespace}")})
@OpenShiftResources({
    @OpenShiftResource("classpath:sso-service-account.json"),
    @OpenShiftResource("classpath:sso-app-secret.json"),
    @OpenShiftResource("classpath:eap-app-secret.json"),
    @OpenShiftResource("classpath:sso-image-stream.json"),
    @OpenShiftResource("classpath:mysql-image-stream.json"),
})
public class SsoMysqlTest extends SsoTestBase
{

	public SsoMysqlTest() {
		route = "ssomysql";
		secureRoute = "secure-ssomysql";
	}
}

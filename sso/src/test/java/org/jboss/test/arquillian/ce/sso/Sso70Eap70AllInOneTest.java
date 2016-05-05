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

import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/bdecoste/application-templates/adminUser/demos/sso70-eap70-all-in-one-demo.json",
    labels = "application=helloworld,component=eap",
	parameters = {
		@TemplateParameter(name = "SOURCE_REPOSITORY_REF", value = "master")
    }	
)
@OpenShiftResources({
    @OpenShiftResource("classpath:sso-service-account.json"),
    @OpenShiftResource("classpath:sso-app-secret.json"),
    @OpenShiftResource("classpath:sso-demo-secret.json"),
    @OpenShiftResource("classpath:eap-app-secret.json")
})
public class Sso70Eap70AllInOneTest extends SsoAllInOneTestBase {
			
    public Sso70Eap70AllInOneTest() {
    }

}

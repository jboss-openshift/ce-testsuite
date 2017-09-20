/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other
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
package org.jboss.test.arquillian.ce.jdg;

import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.api.TemplateResources;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.test.arquillian.ce.jdg.common.JdgMultTempTestBase;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@TemplateResources(syncInstantiation = true, templates = {
		@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/datagrid/datagrid71-basic.json", parameters = {
				@TemplateParameter(name = "APPLICATION_NAME", value = "carcache"),
				@TemplateParameter(name = "INFINISPAN_CONNECTORS", value = "hotrod"),
				@TemplateParameter(name = "CACHE_NAMES", value = "carcache") }),
		@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/eap/eap64-basic-s2i.json", parameters = {
				@TemplateParameter(name = "SOURCE_REPOSITORY_URL", value = "https://github.com/jboss-openshift/openshift-quickstarts"),
				@TemplateParameter(name = "SOURCE_REPOSITORY_REF", value = "1.2"),
				@TemplateParameter(name = "CONTEXT_DIR", value = "datagrid/carmart") }) })
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:jdg-service-account")
@OpenShiftResources({
		@OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/datagrid-app-secret.json") })
public class JdgMultTempTest extends JdgMultTempTestBase {

}
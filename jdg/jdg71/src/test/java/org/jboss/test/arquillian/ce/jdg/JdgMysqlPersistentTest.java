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

import org.arquillian.cube.openshift.api.OpenShiftResource;
import org.arquillian.cube.openshift.api.OpenShiftResources;
import org.arquillian.cube.openshift.api.RoleBinding;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.TemplateParameter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.jdg.common.JdgDBPersistentTestBase;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/datagrid/datagrid71-mysql-persistent.json", parameters = {
        @TemplateParameter(name = "HTTPS_NAME", value = "jboss"),
        @TemplateParameter(name = "HTTPS_PASSWORD", value = "mykeystorepass") })
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:jdg-service-account")
@OpenShiftResources({
        @OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/datagrid-app-secret.json") })
public class JdgMysqlPersistentTest extends JdgDBPersistentTestBase {

    @Deployment
    public static WebArchive getDeployment() {
        return JdgDBPersistentTestBase.getDeployment();
    }

    @Override
    protected String getDriver() {
        return "mysql";
    }
}
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

package org.jboss.test.arquillian.ce.jdg;

import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.test.arquillian.ce.jdg.common.query.JdgQueryTestBase;
import org.junit.runner.RunWith;

/**
 * @author Marko Luksa
 */
@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/datagrid/datagrid71-https.json",
        labels = "application=datagrid-app",
        parameters = {
                @TemplateParameter(name="HTTPS_NAME", value="jboss"),
                @TemplateParameter(name="HTTPS_PASSWORD", value="mykeystorepass"),
                @TemplateParameter(name="CACHE_NAMES", value="addressbook_indexed,addressbook"),
                @TemplateParameter(name="addressbook_indexed_CACHE_START", value="EAGER"),
                @TemplateParameter(name="addressbook_indexed_CACHE_INDEX", value="ALL"),
                @TemplateParameter(name="addressbook_indexed_INDEXING_PROPERTIES", value="\"default.directory_provider=ram\""),
                @TemplateParameter(name="addressbook_indexed_JDBC_STORE_TYPE", value="string"),
                @TemplateParameter(name="addressbook_indexed_JDBC_STORE_PASSIVATION", value="false"),
                @TemplateParameter(name="addressbook_indexed_JDBC_STORE_PRELOAD", value="false"),
                @TemplateParameter(name="addressbook_indexed_JDBC_STORE_PURGE", value="false"),
                @TemplateParameter(name="addressbook_indexed_JDBC_STORE_DATASOURCE", value="\"java:jboss/datasources/ExampleDS\""),
                @TemplateParameter(name="addressbook_indexed_KEYED_TABLE_PREFIX", value="JDG"),
                @TemplateParameter(name="addressbook_indexed_ID_COLUMN_NAME", value="id"),
                @TemplateParameter(name="addressbook_indexed_ID_COLUMN_TYPE", value="VARCHAR"),
                @TemplateParameter(name="addressbook_indexed_DATA_COLUMN_NAME", value="datum"),
                @TemplateParameter(name="addressbook_indexed_DATA_COLUMN_TYPE", value="BINARY"),
                @TemplateParameter(name="addressbook_indexed_TIMESTAMP_COLUMN_NAME", value="version"),
                @TemplateParameter(name="addressbook_indexed_TIMESTAMP_COLUMN_TYPE", value="BIGINT"),
                @TemplateParameter(name="addressbook_CACHE_START", value="EAGER"),
                @TemplateParameter(name="addressbook_JDBC_STORE_TYPE", value="string"),
                @TemplateParameter(name="addressbook_JDBC_STORE_PASSIVATION", value="false"),
                @TemplateParameter(name="addressbook_JDBC_STORE_PRELOAD", value="false"),
                @TemplateParameter(name="addressbook_JDBC_STORE_PURGE", value="false"),
                @TemplateParameter(name="addressbook_JDBC_STORE_DATASOURCE", value="\"java:jboss/datasources/ExampleDS\""),
                @TemplateParameter(name="addressbook_KEYED_TABLE_PREFIX", value="JDG"),
                @TemplateParameter(name="addressbook_ID_COLUMN_NAME", value="id"),
                @TemplateParameter(name="addressbook_ID_COLUMN_TYPE", value="VARCHAR"),
                @TemplateParameter(name="addressbook_DATA_COLUMN_NAME", value="datum"),
                @TemplateParameter(name="addressbook_DATA_COLUMN_TYPE", value="BINARY"),
                @TemplateParameter(name="addressbook_TIMESTAMP_COLUMN_NAME", value="version"),
                @TemplateParameter(name="addressbook_TIMESTAMP_COLUMN_TYPE", value="BIGINT")})
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:jdg-service-account")
@OpenShiftResources({
        @OpenShiftResource("https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/secrets/datagrid-app-secret.json")
})
public class JdgQueryTest extends JdgQueryTestBase {

}
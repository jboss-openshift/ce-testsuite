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

package org.jboss.test.arquillian.ce.amq;

import java.io.IOException;

import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.Replicas;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.amq.support.AmqMigrationTestBase;
import org.jboss.test.arquillian.ce.amq.support.AmqNMigrationsTestBase;
import org.junit.runner.RunWith;

/**
 * @author Ales Justin
 */
@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/amq/amq63-persistent.json",
        parameters = {
                @TemplateParameter(name = "MQ_QUEUES", value = "QUEUES.FOO,QUEUES.BAR"),
                @TemplateParameter(name = "MQ_TOPICS", value = "topics.mqtt"),
                @TemplateParameter(name = "APPLICATION_NAME", value = "amq-test"),
                @TemplateParameter(name = "MQ_USERNAME", value = "${amq.username:amq-test}"),
                @TemplateParameter(name = "MQ_PASSWORD", value = "${amq.password:redhat}"),
                @TemplateParameter(name = "MQ_PROTOCOL", value = "openwire,amqp,mqtt,stomp")})
@RoleBinding(roleRefName = "view", userName = "system:serviceaccount:${kubernetes.namespace}:default")
@OpenShiftResources({
        @OpenShiftResource("classpath:testrunner-secret.json")
})
@Replicas(1)
public class Amq63NMigrationsTest extends AmqNMigrationsTestBase {

    @Deployment
    public static WebArchive getDeployment() throws IOException {
        return getDeploymentBase(AmqMigrationTestBase.class, AmqNMigrationsTestBase.class);
    }

}
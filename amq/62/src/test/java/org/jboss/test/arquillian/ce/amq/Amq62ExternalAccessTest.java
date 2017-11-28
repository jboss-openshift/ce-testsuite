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

package org.jboss.test.arquillian.ce.amq;

import org.jboss.arquillian.ce.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.test.arquillian.ce.amq.support.AmqExternalAccessTestBase;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/amq/amq62-ssl.json",
        parameters = {
                @TemplateParameter(name = "MQ_QUEUES", value = "QUEUES.FOO,QUEUES.BAR"),
                @TemplateParameter(name = "APPLICATION_NAME", value = "amq-test"),
                @TemplateParameter(name = "MQ_USERNAME", value = "${amq.username:amq-test}"),
                @TemplateParameter(name = "MQ_PASSWORD", value = "${amq.password:redhat}"),
                @TemplateParameter(name = "MQ_PROTOCOL", value = "openwire,amqp,mqtt,stomp"),
                @TemplateParameter(name = "AMQ_TRUSTSTORE", value = "amq-test.ts"),
                @TemplateParameter(name = "AMQ_TRUSTSTORE_PASSWORD", value = "amq-test"),
                @TemplateParameter(name = "AMQ_KEYSTORE", value = "amq-test.ks"),
                @TemplateParameter(name = "AMQ_KEYSTORE_PASSWORD", value = "amq-test")})
@OpenShiftResources({
        @OpenShiftResource("classpath:amq-routes.json"),
        @OpenShiftResource("classpath:amq-app-secret.json"),
        @OpenShiftResource("classpath:testrunner-secret.json")
})
public class Amq62ExternalAccessTest extends AmqExternalAccessTestBase {

    static {
        System.setProperty("javax.net.ssl.trustStore", Amq62ExternalAccessTest.class.getClassLoader().getResource("").getPath() + "/amq-test.ts");
        System.setProperty("javax.net.ssl.trustStorePassword", "amq-test");
    }
}
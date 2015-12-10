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

package org.jboss.arquillian.ce.testsuite.eap;

import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.jboss.as.arquillian.container.CommonContainerExtension;
import org.jboss.as.arquillian.container.ManagementClient;
import org.kohsuke.MetaInfServices;

/**
 * Enable container utils injection.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@MetaInfServices(LoadableExtension.class)
public class CEContainerExtension extends CommonContainerExtension {
    @Override
    public void register(ExtensionBuilder builder) {
        super.register(builder);
        builder.observer(InitialObserver.class);
    }

    private static class InitialObserver {
        @Inject
        @ContainerScoped
        private InstanceProducer<ManagementClient> managementClient;

        public void observe(@Observes BeforeSuite event) {
            System.out.println("result = " + event);
            // TODO -- create mgmt client
        }
    }
}

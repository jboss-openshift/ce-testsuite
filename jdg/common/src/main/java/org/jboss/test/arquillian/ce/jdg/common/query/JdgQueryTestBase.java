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

package org.jboss.test.arquillian.ce.jdg.common.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.jdg.common.query.support.Person;
import org.jboss.test.arquillian.ce.jdg.common.query.support.PersonMarshaller;
import org.jboss.test.arquillian.ce.jdg.common.query.support.PhoneNumberMarshaller;
import org.jboss.test.arquillian.ce.jdg.common.query.support.PhoneTypeMarshaller;
import org.junit.Test;

/**
 * @author Marko Luksa
 */
public class JdgQueryTestBase {
    private static final String PROTOBUF_DEFINITION_RESOURCE = "/jdgquerytest/addressbook.proto";

    @Deployment
    public static WebArchive getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
        war.setWebXML(new StringAsset("<web-app/>"));
        war.addPackage(Person.class.getPackage());
        war.addClass(JdgQueryTestBase.class);
        war.addAsResource("jdgquerytest/addressbook.proto");

        war.addAsLibraries(Libraries.transitive("org.infinispan", "infinispan-client-hotrod"));
        war.addAsLibraries(Libraries.transitive("org.infinispan", "infinispan-query-dsl"));
        war.addAsLibraries(Libraries.transitive("org.infinispan", "infinispan-remote-query-client"));

        return war;
    }


    @Test
    public void testQueryThroughHotRodService() throws Exception {
        String host = System.getenv("DATAGRID_APP_HOTROD_SERVICE_HOST");
        int port = Integer.parseInt(System.getenv("DATAGRID_APP_HOTROD_SERVICE_PORT"));

        RemoteCacheManager cacheManager = new RemoteCacheManager(
                new ConfigurationBuilder()
                        .addServer()
                        .host(host).port(port)
                        .marshaller(new ProtoStreamMarshaller())  // The Protobuf based marshaller is required for query capabilities
                        .build()
        );
        RemoteCache<Object, Object> cache = cacheManager.getCache();
        assertNotNull(cache);

        registerSchemasAndMarshallers(cacheManager);

        Person person = new Person();
        person.setId(123);
        person.setName("John Doe");
        person.setEmail("john.doe@internet.com");
        cache.put(person.getId(), person);

        QueryFactory<Query> queryFactory = Search.getQueryFactory(cache);
        QueryFactory qf = (QueryFactory) queryFactory;
        Query query = qf.from(Person.class)
                .having("name").like("%John%").toBuilder()
                .build();

        List<Person> results = query.list();
        assertEquals(1, results.size());
        assertEquals("John Doe", results.iterator().next().getName());
    }

    /**
     * Register the Protobuf schemas and marshallers with the client and then register the schemas with the server too.
     * @param cacheManager cache manager
     */
    private void registerSchemasAndMarshallers(RemoteCacheManager cacheManager) throws IOException {
        // Register entity marshallers on the client side ProtoStreamMarshaller instance associated with the remote cache manager.
        SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(cacheManager);
        ctx.registerProtoFiles(FileDescriptorSource.fromResources(PROTOBUF_DEFINITION_RESOURCE));
        ctx.registerMarshaller(new PersonMarshaller());
        ctx.registerMarshaller(new PhoneNumberMarshaller());
        ctx.registerMarshaller(new PhoneTypeMarshaller());

        // register the schemas with the server too
        RemoteCache<String, String> metadataCache = cacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
        metadataCache.put(PROTOBUF_DEFINITION_RESOURCE, readResource(PROTOBUF_DEFINITION_RESOURCE));
        String errors = metadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
        if (errors != null) {
            throw new IllegalStateException("Some Protobuf schema files contain errors:\n" + errors);
        }
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            final Reader reader = new InputStreamReader(is, "UTF-8");
            StringWriter writer = new StringWriter();
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                writer.write(buf, 0, len);
            }
            return writer.toString();
        }
    }

}

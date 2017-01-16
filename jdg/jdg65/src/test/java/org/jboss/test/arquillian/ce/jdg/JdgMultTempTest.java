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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.arquillian.ce.api.OpenShiftResource;
import org.jboss.arquillian.ce.api.OpenShiftResources;
import org.jboss.arquillian.ce.api.RoleBinding;
import org.jboss.arquillian.ce.api.Template;
import org.jboss.arquillian.ce.api.TemplateParameter;
import org.jboss.arquillian.ce.api.TemplateResources;
import org.jboss.arquillian.ce.cube.RouteURL;
import org.jboss.arquillian.ce.httpclient.HttpClient;
import org.jboss.arquillian.ce.httpclient.HttpClientBuilder;
import org.jboss.arquillian.ce.httpclient.HttpClientExecuteOptions;
import org.jboss.arquillian.ce.httpclient.HttpRequest;
import org.jboss.arquillian.ce.httpclient.HttpResponse;
import org.jboss.arquillian.ce.shrinkwrap.Libraries;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.arquillian.ce.jdg.query.support.Car;
import org.jboss.test.arquillian.ce.jdg.query.support.CarMarshaller;
import org.jboss.test.arquillian.ce.jdg.query.support.CarType;
import org.jboss.test.arquillian.ce.jdg.query.support.CarTypeMarshaller;
import org.jboss.test.arquillian.ce.jdg.query.support.Country;
import org.jboss.test.arquillian.ce.jdg.query.support.CountryMarshaller;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests Multiple Template Instantiation using CarMart application quickstart.
 *
 * Pings EAP to store a car within CarMart app and confirms that car was added
 * to the application. Then checks the JDG cache to confirm that car entry was
 * cached.
 *
 * This test confirms whether we can instantiate more than one template using
 * multiple @Template annotations within @TemplateResources.
 *
 * Note that the "syncInstantiation" parameter of @TemplateResources determines
 * whether the templates are instantiated at the same time(synchronously) or in
 * order(asynchronously e.g. the second template will deploy after the first
 * template finished deploying). The templates are instantiated synchronously
 * by default.
 *
 * @author kliberti@redhat.com
 */
@RunWith(Arquillian.class)
@TemplateResources(syncInstantiation = true, templates = {
		@Template(url = "https://raw.githubusercontent.com/${template.repository:jboss-openshift}/application-templates/${template.branch:master}/datagrid/datagrid65-basic.json", parameters = {
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
public class JdgMultTempTest {

	private static final String PROTOBUF_DEFINITION_RESOURCE = "/jdgquerytest/carcache.proto";
	private static final HttpClientExecuteOptions execOptions = new HttpClientExecuteOptions.Builder().tries(3)
			.desiredStatusCode(200).delay(10).build();
	private static final Car CAR = new Car("test", 0.0, CarType.SEDAN, "test", "test", Country.USA);

	@RouteURL("eap-app")
	private URL url;

	 protected URL getUrl() {
	        return url;
	}

	@Deployment
	public static WebArchive getDeployment() {
		WebArchive war = ShrinkWrap.create(WebArchive.class, "run-in-pod.war");
		war.setWebXML(new StringAsset("<web-app/>"));
		war.addPackage(Car.class.getPackage());
		war.addAsResource("jdgquerytest/carcache.proto");

		war.addClass(HttpClientExecuteOptions.class);
		war.addClass(HttpClient.class);

		war.addAsLibraries(Libraries.transitive("org.infinispan", "infinispan-client-hotrod"));
		war.addAsLibraries(Libraries.transitive("org.infinispan", "infinispan-query-dsl"));
		war.addAsLibraries(Libraries.transitive("org.infinispan", "infinispan-remote-query-client"));

		return war;
	}

	/**
	 * Adds new car to the CarMart application running on EAP, retrieves
	 * it, and then compares the retrieved car to the original car stored.
	 *
	 * @throws Exception
	 */
	@Test
	@RunAsClient
	@InSequence(1)
	public void testCarMartApp() throws Exception {
		HttpClient client = HttpClientBuilder.untrustedConnectionClient();
		addCar(client, CAR);
		assertCarsAreSame(CAR, getCar(client, CAR));
	}


	/**
	 * Confirms that the car that was added to the CarMart application
	 * in the previous test was properly cached
	 *
	 * @throws Exception
	 */
	@Test
	@InSequence(2)
	public void testInfinispanCache() throws Exception {
		String host = System.getenv("CARCACHE_HOTROD_SERVICE_HOST");
		int port = Integer.parseInt(System.getenv("CARCACHE_HOTROD_SERVICE_PORT"));

		RemoteCacheManager cacheManager = new RemoteCacheManager(
				new ConfigurationBuilder()
				.addServer()
				.host(host)
				.port(port)
				// Needed to convert cache entries back to Java objects
				.marshaller(new ProtoStreamMarshaller())
				.build()
		);
		RemoteCache<Object, Object> cache = cacheManager.getCache("carcache");
		assertNotNull(cache);

		registerSchemasAndMarshallers(cacheManager);

		cache.put(encode(CAR.getNumberPlate()), CAR);
		Car car = (Car) cache.get(encode(CAR.getNumberPlate()));

		assertCarsAreSame(CAR, car);
	}

	/**
	 * Register the Protobuf schemas and marshallers with the client and then
	 * register the schemas with the server too.
	 *
	 * @param cacheManager cache manager
	 */
	private void registerSchemasAndMarshallers(RemoteCacheManager cacheManager) throws IOException {
		// Register entity marshallers on the client side ProtoStreamMarshaller instance associated with the remote cache manager.
		SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(cacheManager);
		ctx.registerProtoFiles(FileDescriptorSource.fromResources(PROTOBUF_DEFINITION_RESOURCE));
		ctx.registerMarshaller(new CarMarshaller());
		ctx.registerMarshaller(new CarTypeMarshaller());
		ctx.registerMarshaller(new CountryMarshaller());

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

	private String makeGetRequest(HttpClient client, String url) throws Exception {
		HttpRequest request = HttpClientBuilder.doGET(url);
		HttpResponse response = client.execute(request, execOptions);

		return response.getResponseBodyAsString();
	}

	/**
	 * Retrieves cookie from CarMart application by extracting
	 * javax.faces.ViewState from HTTP response. Returns null on failure.
	 *
	 * NOTE: This method uses regex to parse a HTTP response in order extract
	 * the ViewState. This is NOT recommended practice. This regex is tailored
	 * to CarMart application and may fail if used on other jsf applications.
	 */
	private String getCookie(HttpClient client, String page) throws Exception {
		String response = makeGetRequest(client, getUrl() + page);

		String vsRegex = "-*\\d{5,20}:-*\\d{5,20}";
		Pattern vsPatt = Pattern.compile(vsRegex);
		Matcher vsMatch = vsPatt.matcher(response);
		if (vsMatch.find()) {
			return vsMatch.group();
		} else {
			return null;
		}
	}

	/**
	 * Stores car within CarMart application via HTTP GET request
	 */
	private void addCar(HttpClient client, Car car) throws Exception {

		/* Mapping of car attributes and values */
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		map.put("j_idt6", "j_idt6");
		map.put("j_idt6:j_idt9", "j_idt6:j_idt9");
		map.put("j_idt6:j_idt9:numberPlate", car.getNumberPlate());
		map.put("j_idt6:j_idt9:brand", car.getBrand());
		map.put("j_idt6:j_idt9:color", car.getColor());
		map.put("j_idt6:j_idt9:displacement", "" + car.getDisplacement());
		map.put("j_idt6:j_idt9:carType", "" + car.getType());
		map.put("j_idt6:j_idt9:country", "" + car.getCountry());
		map.put("j_idt6:j_idt9:j_idt20", "Add car");

		/* Formats attribute information for GET request */
		StringBuilder connectionUrl = new StringBuilder();
		connectionUrl.append(getUrl() + "addcar.jsf?");
		for (String s : map.keySet()) {
			connectionUrl.append(encode(s) + "=" + encode(map.get(s)) + "&");
		}
		connectionUrl.append("javax.faces.ViewState=" + getCookie(client, "addcar.jsf"));
		makeGetRequest(client, connectionUrl.toString());
	}

	/**
	 * Returns car retrieved from CarMart application which matches the
	 * attributes of the parameter, car1. Returns null if there is not
	 * match store within application
	 *
	 * @throws Exception
	 */
	public Car getCar(HttpClient client, Car car1) throws Exception {
		String response = makeGetRequest(client, getUrl() + "home.jsf");
		Car car2;

		LinkedHashSet<String> carIds = new LinkedHashSet<String>();
		Matcher idMatch = Pattern.compile("(j_idt6:j_idt9:)[0-9](:j_idt13)").matcher(response);
		while (idMatch.find()) {
			// Collects all the car ids on page using regex.
			carIds.add(idMatch.group());
		}

		for (String id : carIds) {
			// Uses id to gather the attribute information of car
			car2 = extractCarFromApp(client, id);
			if (cmpCar(CAR, car2)) {
				return car2;
			}
		}
		return null;
	}

	/**
	 * Takes a car element id from the CarMart application as an argument and
	 * uses it to extract the car's specifications(numberPlate, Brand, etc.).
	 * Then uses these extracted specifications to create a new car representing
	 * the car stored in the CarMart app.
	 *
	 * @throws Exception
	 */
	private Car extractCarFromApp(HttpClient client, String carId) throws Exception {
		StringBuilder connectionUrl = new StringBuilder();
		connectionUrl.append(getUrl());
		connectionUrl.append("home.jsf");
		connectionUrl.append("?j_idt6=j_idt6");
		connectionUrl.append("&javax.faces.ViewState=" + getCookie(client, "home.jsf"));
		connectionUrl.append("&" + carId);
		String response = makeGetRequest(client, connectionUrl.toString());

		Car car = new Car();

		// Attributes are stored in this order within CarMart application
		String[] attrs = { "numberPlate", "country", "brand", "color", "displacement", "type" };

		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		Matcher value = Pattern.compile("<td>[0.0-9|a-z|A-Z]+").matcher(response);

		// Search and Store attributes and corresponding values
		for (String attr : attrs) {
			value.find();
			map.put(attr, value.group().replace("<td>", ""));
		}

		car.setNumberPlate(map.get("numberPlate"));
		car.setCountry(Country.toEnum(map.get("country")));
		car.setBrand(map.get("brand"));
		car.setColor(map.get("color"));
		car.setDisplacement(Double.parseDouble(map.get("displacement")));
		car.setType(CarType.toEnum(map.get("type")));

		return car;
	}

	private void assertCarsAreSame(Car car1, Car car2) {
		assertEquals(car1.getNumberPlate(), car2.getNumberPlate());
		assertEquals(car1.getBrand(), car2.getBrand());
		assertEquals(car1.getColor(), car2.getColor());
		assertEquals(car1.getDisplacement(), car2.getDisplacement(), 0.01);
		assertEquals(car1.getType(), car2.getType());
	}

	private boolean cmpCar(Car car1, Car car2) {
		return car1.getNumberPlate().equals(car2.getNumberPlate())
			&& car1.getCountry() == car2.getCountry()
			&& car1.getBrand().equals(car2.getBrand())
			&& car1.getColor().equals(car2.getColor())
			&& car1.getDisplacement() == car2.getDisplacement()
			&& car1.getType() == car2.getType();
	}

	public static String encode(String key) {
		try {
			return URLEncoder.encode(key, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
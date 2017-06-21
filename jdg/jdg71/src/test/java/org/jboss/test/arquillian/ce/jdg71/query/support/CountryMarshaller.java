/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.test.arquillian.ce.jdg71.query.support;

import org.infinispan.protostream.EnumMarshaller;

/**
 * @author Adrian Nistor
 */
public class CountryMarshaller implements EnumMarshaller<Country> {

	@Override
	public Class<Country> getJavaClass() {
		return Country.class;
	}

	@Override
	public String getTypeName() {
		return "jdgquerytest.Car.Country";
	}

	@Override
	public Country decode(int enumValue) {
		switch (enumValue) {
		case 0:
			return Country.CZECH_REPUBLIC;
		case 1:
			return Country.USA;
		case 2:
			return Country.GERMANY;
		}
		return null;
	}

	@Override
	public int encode(Country Country) {
		switch (Country) {
		case CZECH_REPUBLIC:
			return 0;
		case USA:
			return 1;
		case GERMANY:
			return 2;
		}

		throw new IllegalArgumentException("Unexpected Country value : " + Country);
	}
}
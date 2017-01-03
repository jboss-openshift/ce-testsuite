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
package org.jboss.test.arquillian.ce.jdg.query.support;

import org.infinispan.protostream.EnumMarshaller;

/**
 * @author Adrian Nistor
 */
public class CarTypeMarshaller implements EnumMarshaller<CarType> {

	@Override
	public Class<CarType> getJavaClass() {
		return CarType.class;
	}

	@Override
	public String getTypeName() {
		return "jdgquerytest.Car.CarType";
	}

	@Override
	public CarType decode(int enumValue) {
		switch (enumValue) {
		case 0:
			return CarType.SEDAN;
		case 1:
			return CarType.HATCHBACK;
		case 2:
			return CarType.COMBI;
		case 3:
			return CarType.CABRIO;
		case 4:
			return CarType.ROADSTER;

		}
		return null;
	}

	@Override
	public int encode(CarType carType) {
		switch (carType) {
		case SEDAN:
			return 0;
		case HATCHBACK:
			return 1;
		case COMBI:
			return 2;
		case CABRIO:
			return 3;
		case ROADSTER:
			return 4;
		}

		throw new IllegalArgumentException("Unexpected CarType value : " + carType);
	}
}
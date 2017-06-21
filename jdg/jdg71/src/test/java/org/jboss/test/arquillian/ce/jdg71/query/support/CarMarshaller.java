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

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class CarMarshaller implements MessageMarshaller<Car> {

	@Override
	public String getTypeName() {
		return "jdgquerytest.Car";
	}

	@Override
	public Class<Car> getJavaClass() {
		return Car.class;
	}

	@Override
	public Car readFrom(ProtoStreamReader reader) throws IOException {

		String brand = reader.readString("brand");
		double displacement = reader.readDouble("displacement");
		CarType type = reader.readObject("type", CarType.class);
		String color = reader.readString("color");
		String numberPlate = reader.readString("numberPlate");
		Country country = reader.readObject("country", Country.class);

		Car car = new Car();
		car.setBrand(brand);
		car.setDisplacement(displacement);
		car.setType(type);
		car.setColor(color);
		car.setNumberPlate(numberPlate);
		car.setCountry(country);

		return car;
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, Car car) throws IOException {
		writer.writeString("brand", car.getBrand());
		writer.writeDouble("displacement", car.getDisplacement());
		writer.writeObject("type", car.getType(), CarType.class);
		writer.writeString("color", car.getColor());
		writer.writeString("numberPlate", car.getNumberPlate());
		writer.writeObject("country", car.getCountry(), Country.class);

	}

}

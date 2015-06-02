/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
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
package org.jboss.datagrid.demo.marshallers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.infinispan.protostream.MessageMarshaller;
import org.jboss.datagrid.demo.model.Person;
import org.jboss.datagrid.demo.model.PhoneNumber;

/**
 * @author Thomas Qvarnstrom
 */
public class PersonMarshaller implements MessageMarshaller<Person> {

   @Override
   public String getTypeName() {
      return "phonebook.Person";
   }

   @Override
   public Class<Person> getJavaClass() {
      return Person.class;
   }

   @Override
   public Person readFrom(ProtoStreamReader reader) throws IOException {
      String id = reader.readString("id");
      String firstname = reader.readString("firstname");
      String surname = reader.readString("surname");
      String email = reader.readString("email");
      List<PhoneNumber> phones = reader.readCollection("phone", new ArrayList<PhoneNumber>(), PhoneNumber.class);

      Person person = new Person();
      person.setFirstname(firstname);
      person.setSurname(surname);
      person.setId(id);
      person.setEmail(email);
      person.setPhones(phones);
      return person;
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, Person person) throws IOException {
	  writer.writeString("id", person.getId());
      writer.writeString("firstname", person.getFirstname());
      writer.writeString("surname", person.getSurname());
      writer.writeString("email", person.getEmail());
      writer.writeCollection("phone", person.getPhones(), PhoneNumber.class);
   }
}

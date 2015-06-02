package org.jboss.datagrid.demo.rest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.jboss.datagrid.demo.PhoneBookManager;
import org.jboss.datagrid.demo.model.ConnectionDetails;
import org.jboss.datagrid.demo.model.Person;

@Path("/")
@Stateless
public class PhoneBookRestService {

	Logger log = Logger.getLogger(PhoneBookRestService.class.getName());

	public static final int PHONEBOOK_SIZE = 10;

	@Inject
	PhoneBookManager manager;

	@GET
	@Path("/persons")
	@Produces({ "application/json" })
	public Person[] getAllPersons() {		

		Person[] persons = manager.getAll().toArray(new Person[0]);

		Arrays.sort(persons, new Comparator<Person>() {
			public int compare(Person p1, Person p2) {
				String pname1 = p1.getSurname() + "," + p1.getFirstname();
				String pname2 = p2.getSurname() + "," + p2.getFirstname();
				return pname1.compareTo(pname2);
			}
		});

		return persons;
	}

	@GET
	@Path("/generate-data")
	@Produces({ "application/json" })
	public void generate() {
		manager.populateWithGenerateEntries(PHONEBOOK_SIZE);
	}
	
	@GET
	@Path("/clear")
	@Produces({ "application/json" })
	public boolean clear() {
		manager.removeAll();
		return true;
	}

	@GET
	@Produces("application/json")
	@Path("/filter/{value}")
	public Person[] filter(@PathParam("value") String value) {
		Person[] persons = manager.searchByName(value+"*").toArray(new Person[0]);
		Arrays.sort(persons, new Comparator<Person>() {
			public int compare(Person p1, Person p2) {
				String pname1 = p1.getSurname() + "," + p1.getFirstname();
				String pname2 = p2.getSurname() + "," + p2.getFirstname();
				return pname1.compareTo(pname2);
			}
		});
		return persons;
	}

	public static Comparator<Person> FruitNameComparator = new Comparator<Person>() {

		public int compare(Person p1, Person p2) {

			String pname1 = p1.getSurname() + "," + p1.getFirstname();
			String pname2 = p2.getSurname() + "," + p2.getFirstname();
			// ascending order
			return pname1.compareTo(pname2);
		}

	};
	
	@GET
	@Path("/connectiondetails")
	@Produces({ "application/json" })
	public ConnectionDetails getConnectionDetails() {		
		return manager.getConnectionDetails();
	}
	
	@POST
	@Path("/connect")
	public void connect(@QueryParam("profile") String profile, 
						@QueryParam("username") String username, 
						@QueryParam("password") String password) {		
		manager.connect(profile,username,password);
	}
	
	@POST
	@Path("/disconnect")
	public void connect() {		
		manager.disconnect();
	}
	
}

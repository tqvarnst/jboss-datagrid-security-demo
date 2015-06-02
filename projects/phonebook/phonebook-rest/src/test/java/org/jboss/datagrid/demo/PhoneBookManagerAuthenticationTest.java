package org.jboss.datagrid.demo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Config;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.datagrid.demo.marshallers.PersonMarshaller;
import org.jboss.datagrid.demo.marshallers.PhoneNumberMarshaller;
import org.jboss.datagrid.demo.marshallers.PhoneTypeMarshaller;
import org.jboss.datagrid.demo.model.ConnectionDetails;
import org.jboss.datagrid.demo.model.Person;
import org.jboss.datagrid.demo.model.PhoneNumber;
import org.jboss.datagrid.demo.model.PhoneType;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class PhoneBookManagerAuthenticationTest {

	public static final int PHONEBOOK_SIZE = 50;
	
	Logger log = Logger.getLogger(this.getClass().getName());
	
	@Deployment
	public static WebArchive createDeployment() {
		File[] jars = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve().withTransitivity().asFile();
		
		return ShrinkWrap
				.create(WebArchive.class, "phonebook-test.war")
				.addClass(PhoneBookManager.class)
				.addClass(LoginHandler.class)
				.addClass(PersonMarshaller.class)
				.addClass(PhoneNumberMarshaller.class)
				.addClass(PhoneTypeMarshaller.class)
				.addAsResource("identity.jks")
				.addAsResource("keystore.jks")
				.addAsResource("phonebook.properties")
				.addAsResource("phonebook/phonebook.proto","phonebook/phonebook.proto")
				.addAsLibraries(jars)
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
	}
	
	@Inject
	PhoneBookManager manager;

	
	
	@Before
	public void setUp() throws Exception {
		this.manager = new PhoneBookManager();
		manager.connect("plain","writer","writer-12345");
		manager.removeAll();
		assertTrue(manager.getAll().size()==0);
	}

	@After
	public void tearDown() throws Exception {
		manager.disconnect();
	}
	
	@Test 
	public void testConnectionDetails() {
		ConnectionDetails connectionDetails = manager.getConnectionDetails();
		Map<String, String> supportedConfigurations = connectionDetails.getSupportedConfigurations();
		
		log.info("Number of supported configs are " + supportedConfigurations.size());
		assertTrue(supportedConfigurations.size()==6);
		
		for(String config : supportedConfigurations.keySet()) {
			log.info(String.format("Friendly name of %s is %s",config,supportedConfigurations.get(config)));
			assertNotNull(supportedConfigurations.get(config));
		}
	}

	@Test
	public void testGetAll() {
		Person p = new Person();
		p.setFirstname("Thomas");
		p.setSurname("Qvarnstrom");
		String uid = manager.put(p);
		assertNotNull(uid);
		assertTrue(uid.length()>0);
		
		p=null;
		
		List<Person> r1 = manager.getAll();
		assertNotNull(r1);
		assertTrue(r1.size()==1);
		p = r1.get(0);
		assertTrue("Thomas".equals(p.getFirstname()));
		
		manager.remove(p);
		assertTrue(manager.getAll().size()==0);
		
	}
	
	@Test
	public void testGetByName() {
		Person p = new Person();
		p.setFirstname("Thomas");
		p.setSurname("Qvarnstrom");
		String uid = manager.put(p);
		assertNotNull(uid);
		assertTrue(uid.length()>0);
		
		p = null;
		
		List<Person> r = manager.getByFirstname("Thomas");
		assertNotNull(r);
		assertTrue(r.size()==1);
		p = r.get(0);
		assertTrue("Thomas".equals(p.getFirstname()));
		assertTrue("Qvarnstrom".equals(p.getSurname()));
		
		p = null;
		r = null;
		
		r = manager.getBySurname("Qvarnstrom");
		assertNotNull(r);
		assertTrue(r.size()==1);
		p = r.get(0);
		assertTrue("Thomas".equals(p.getFirstname()));
		assertTrue("Qvarnstrom".equals(p.getSurname()));
		
		p = null;
		r = null;

		p = manager.getByName("Thomas", "Qvarnstrom");
		assertTrue("Thomas".equals(p.getFirstname()));
		assertTrue("Qvarnstrom".equals(p.getSurname()));
		
		manager.removeAll();
		assertTrue(manager.getAll().size()==0);
	}
	
	@Test
	public void testSearchByName() {
		Person p = new Person();
		p.setFirstname("Thomas");
		p.setSurname("Qvarnstrom");
		String uid = manager.put(p);
		assertNotNull(uid);
		assertTrue(uid.length()>0);
		
		p = null;
		
		List<Person> r = manager.searchByName("Tho*");
		assertNotNull(r);
		assertTrue(r.size()==1);
		p = r.get(0);
		assertTrue("Thomas".equals(p.getFirstname()));
		assertTrue("Qvarnstrom".equals(p.getSurname()));
		
		p = null;
		r = null;
		
		r = manager.searchByName("Qva*");
		assertNotNull(r);
		assertTrue(r.size()==1);
		p = r.get(0);
		assertTrue("Thomas".equals(p.getFirstname()));
		assertTrue("Qvarnstrom".equals(p.getSurname()));
		
		manager.removeAll();
		assertTrue(manager.getAll().size()==0);
	}
	
	@Test
	public void testGetById() {
		Person p = new Person();
		p.setFirstname("Thomas");
		p.setSurname("Qvarnstrom");
		String uid = manager.put(p);
		assertNotNull(uid);
		assertTrue(uid.length()>0);
		
		p = null;
		
		p = manager.getById(uid);
		assertNotNull(p);
		assertTrue("Thomas".equals(p.getFirstname()));
	}
	
	@Test
	public void testLoadJks() {
		log.info("jboss.server.config.dir=" + System.getProperty("jboss.server.config.dir"));
		assertTrue(true);
	}
		
	
	@Test
	public void testWithGeneratedEntries() {
		
		this.populateWithGenerateEntries();
		assertTrue(manager.getAll().size()==PHONEBOOK_SIZE);
		
		
	}
	
	
	
	
	
	public void populateWithGenerateEntries() {
		Random r = new Random();
		String[] firstNames = new String[] { "Keira", "Sandee", "Roslyn",
				"Leland", "Joella", "Anneliese", "Janita", "Marcella",
				"Adella", "Iraida", "Laquanda", "Jonie", "Juliean", "Paige",
				"Berta", "Denver", "Francis", "Ardella", "Alfonzo", "Velvet" };
		String[] lastNames = new String[] { "Ladwig", "Rains", "Kappel",
				"Kulikowski", "Fink", "Morein", "Griffieth", "Hannahs",
				"Peacock", "Motter", "Pfaff", "Zieman", "Nutt", "Hart",
				"Seiden", "Weeks", "Richert", "Rivero", "Zale", "Ecklund" };
		String[] emailDomains = new String[] { "google.com", "redhat.com", "hotmail.com" };
		for (int i = 0; i < PHONEBOOK_SIZE; i++) {
			Person p = new Person();
			while(true) {
				p.setFirstname(firstNames[r.nextInt(20)]);
				p.setSurname(lastNames[r.nextInt(20)]);
				if(!manager.isDuplicate(p)) {
					break;
				} else {
					log.info(String.format("%s %s is a duplicate, generating new name", p.getFirstname(), p.getSurname()));
				}
			}
			PhoneNumber pn1 = new PhoneNumber();
			pn1.setNumber("555-" + Integer.toString(r.nextInt(10000)));
			pn1.setType(PhoneType.HOME);
			PhoneNumber pn2 = new PhoneNumber();
			pn2.setNumber("555-" + Integer.toString(r.nextInt(10000)));
			pn2.setType(PhoneType.MOBILE);
			PhoneNumber pn3 = new PhoneNumber();
			pn3.setNumber("555-" + Integer.toString(r.nextInt(10000)));
			pn3.setType(PhoneType.WORK);
			p.setPhones(Arrays.asList(pn1, pn2, pn3));
			p.setEmail(p.getFirstname().toLowerCase() + "." + p.getSurname().toLowerCase() + "@" + emailDomains[r.nextInt(3)]);
			manager.put(p);
		}
	}
	
	private boolean checkIfDuplicate(Person p) {
		return false;
	}

}

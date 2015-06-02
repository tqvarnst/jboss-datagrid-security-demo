package org.jboss.datagrid.demo;
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


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.PrivilegedActionException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import javax.ejb.PrePassivate;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.datagrid.demo.marshallers.PersonMarshaller;
import org.jboss.datagrid.demo.marshallers.PhoneNumberMarshaller;
import org.jboss.datagrid.demo.marshallers.PhoneTypeMarshaller;
import org.jboss.datagrid.demo.model.ConnectionDetails;
import org.jboss.datagrid.demo.model.Person;
import org.jboss.datagrid.demo.model.PhoneNumber;
import org.jboss.datagrid.demo.model.PhoneType;

@Named
@SessionScoped
public class PhoneBookManager implements Serializable {

	private static final long serialVersionUID = 5105865150111362402L;

	Logger log = Logger.getLogger(PhoneBookManager.class.getName());
	
	public static final String DEFAULT_CACHE_NAME = "phonebook";

	public static final String DEFAULT_REALM = "LdapRealm";
	public static final String DEFAULT_SERVER_NAME = "node0";

	public static final String LOADER_USER = "writer";
	public static final String LOADER_USER_PWD = "writer-12345";

	public static final String READER_USER = "reader";
	public static final String READER_USER_PWD = "reader-12345";

	private static final String PROTOBUF_DEFINITION_RESOURCE = "/phonebook/phonebook.proto";
	private static final String PHONEBOOK_JDG_PROPERTIES_FILE = "phonebook.properties";
	
	private static final String DEFAULT_JDG_HOST = "localhost";
	private static final String DEFAULT_JDG_PORT = "11222";
	
	protected Map<String,String> supportedConfigurations = new LinkedHashMap<String,String>();
	protected String host;
	protected int port;
	protected String user;
	protected String password;
	protected String type;
	protected String cacheName;
	protected String serverRealm;
	protected String serverName;
	protected String saslMechanism;
	protected String keystoreFile;
	protected String keystorePasswd;
	protected String truststoreFile;
	protected String truststorePasswd;
	
	private boolean connected = false;
	
	private RemoteCacheManager cacheManager;
	private RemoteCache<String, Person> cache;

	
	private Properties props = new Properties();


	public PhoneBookManager() {
		InputStream res = null;
		try {
			res = getClass().getClassLoader().getResourceAsStream(PHONEBOOK_JDG_PROPERTIES_FILE);
		 	props.load(res);
		 	String configs = props.getProperty("common.configs","plain,auth");
		 	for(String config : configs.split(",")) {
		 		supportedConfigurations.put(config, props.getProperty(config + ".friendlyName",config));
		 	}
		} catch (IOException ioe) {
			log.info("Exception: " + ioe.getMessage());
			throw new RuntimeException(ioe);
		} finally {
			if (res != null) {
				try {
					res.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	
	
	
	public void connect(String config, String user, String password) {
		log.info(String.format("Connecting to JDG with profile=%s, user=%s, password=%s",config,user,password));
		
		if(!supportedConfigurations.containsKey(config)) {
			throw new UnsupportedConfigurationException();
		}
	 	this.host = getPrefixeProperty(config,"jdg.hotrod.host", DEFAULT_JDG_HOST);
	 	this.port = Integer.parseInt(getPrefixeProperty(config,"jdg.hotrod.port", DEFAULT_JDG_PORT));
	 	this.user = user;
	 	this.password = password;
	 	this.type = config;
	 	this.cacheName = props.getProperty("common.jdg.cache.name", DEFAULT_CACHE_NAME);
	 	this.serverRealm = getPrefixeProperty(config,"jdg.server.realm", DEFAULT_REALM);
	 	this.serverName = getPrefixeProperty(config,"jdg.server.name", DEFAULT_SERVER_NAME);
	 	this.saslMechanism = getPrefixeProperty(config,"jdg.cache.authentication.sasl.method","PLAIN");
	 	this.keystoreFile = System.getProperty("jboss.server.config.dir") + "/" + getPrefixeProperty(config,"jdg.client.keystore.file");
	 	this.keystorePasswd = getPrefixeProperty(config,"jdg.client.keystore.password");
	 	this.truststoreFile = System.getProperty("jboss.server.config.dir") + "/" + getPrefixeProperty(config,"jdg.client.truststore.file");
	 	this.truststorePasswd = getPrefixeProperty(config,"jdg.client.truststore.password");
         
	 	log.info(String.format("Connecting to JDG with profile=%s, host=%s, port=%s, user=%s, password=%s, realm=%s, cachename=%s, servername=%s",config,host,port,user,password,serverRealm,cacheName,serverName));
	 	
		this.cacheManager = getRemoteCacheManager();
		registerSchemasAndMarshallers();
		this.cache = cacheManager.getCache(this.cacheName);
		this.connected = true;
		
		log.info("Successfully connected to JDG");

//		if (cache.isEmpty()) {
//			try {
//				populateWithGenerateEntries(10);
//			} catch (HotRodClientException e) {
//				if (e.getCause() instanceof PrivilegedActionException) {
//					log.info(String
//							.format("User %s does not have persmission to write to the cache",
//									user));
//				}
//			}
//		}

		
	}
	
	public void disconnect() {
		if(cache!=null)
			cache.stop();
		if(cacheManager!=null)
			cacheManager.stop();
		this.connected=false;
		
	}
	
	public List<Person> getAll() {
		if(!connected) {
			throw new NotConnectedException();
		}
		Query query = Search.getQueryFactory(cache).from(Person.class).build();
		return query.list();
	}
	
	public List<Person> getByFirstname(String name) {
		if(!connected) {
			throw new NotConnectedException();
		}
		Query query = Search.getQueryFactory(cache).from(Person.class)
				.having("firstname").like(name).toBuilder().build();
		return query.list();
	}
	
	public List<Person> getBySurname(String surname) {
		if(!connected) {
			throw new NotConnectedException();
		}
		Query query = Search.getQueryFactory(cache).from(Person.class)
				.having("surname").like(surname).toBuilder().build();
		return query.list();
	}	
	
	public Person getByName(String firstname, String surname) {
		if(!connected) {
			throw new NotConnectedException();
		}
		Query query = Search.getQueryFactory(cache).from(Person.class)
				.having("firstname").eq(firstname).and()
				.having("surname").eq(surname).toBuilder().build();
		if(query.list().size()==0) {
			throw new NoSuchPersonException();
		} else if(query.list().size()>1) {
			throw new MultipleSearchHitsException();
		}
		return (Person) query.list().get(0);
	}
	
	public List<Person> searchByName(String name) {
		if(!connected) {
			throw new NotConnectedException();
		}
		Query query = Search.getQueryFactory(cache).from(Person.class)
				.having("firstname").like(name).or()
				.having("surname").like(name)
				.toBuilder().build();
		return query.list();
	}	

	
	public Person getById(String id) {
		if(!connected) {
			throw new NotConnectedException();
		}
		return cache.get(id);
	}
	
	public void remove(Person p) {
		if(!connected) {
			throw new NotConnectedException();
		}
		cache.remove(p.getId());
	}
	
	public void removeAll() {
		if(!connected) {
			throw new NotConnectedException();
		}
		List<Person> all = getAll();
		for(Person p : all) {
			cache.remove(p.getId());
		}
	}
	
	public String put(Person p) {
		if(!connected) {
			throw new NotConnectedException();
		}
		if(p.getId()==null || p.getId().isEmpty()) {
			String id = UUID.randomUUID().toString();
			p.setId(id);
		}
		cache.put(p.getId(), p);
		return p.getId();
	}
	
	
	public boolean isDuplicate(Person p) {
		if(!connected) {
			throw new NotConnectedException();
		}
		try {
			this.getByName(p.getFirstname(), p.getSurname());
		} catch (RuntimeException rte) {
			if(rte instanceof NoSuchPersonException) {
				return false;
			}
		}
		return true;
	}
	
	private RemoteCacheManager getRemoteCacheManager() {
		RemoteCacheManager cacheManager;
		if("auth-ldap".equals(this.type) || "auth".equals(this.type) || "auth-digest".equals(this.type)) {
			cacheManager = getAuthenticatedRemoteCacheManager();
		} else if("auth-ldap-ssl".equals(type) || "auth-ssl".equals(this.type)) {
			cacheManager = getAuthenticatedSecureRemoteCacheManager();
		} else if("plain".equals(type)) {
			ConfigurationBuilder config = new ConfigurationBuilder();
			config.addServer()
					.host(host)
					.port(port)
				.maxRetries(0)
				.marshaller(new ProtoStreamMarshaller());
			cacheManager =  new RemoteCacheManager(config.build(), true);
		} else {
			throw new UnsupportedConfigurationException();
		}
		
		return cacheManager;
	}
	
	
	private RemoteCacheManager getAuthenticatedSecureRemoteCacheManager() {
		System.out.println("Get authenticated secure remote cache manager");
		ConfigurationBuilder config = new ConfigurationBuilder();
		config.addServer()
				.host(host)
				.port(port)
			.maxRetries(0)
			.marshaller(new ProtoStreamMarshaller())
			.security()
				.authentication()
					.serverName(this.serverName)
					.saslMechanism(saslMechanism)
					.callbackHandler(
						new LoginHandler(user, password.toCharArray(),
								this.serverRealm)).enable();
		config.security().ssl()
				.keyStoreFileName(this.keystoreFile)
				.keyStorePassword(this.keystorePasswd.toCharArray())
				.trustStoreFileName(this.truststoreFile)
				.trustStorePassword(this.truststorePasswd.toCharArray()).enable();
		return new RemoteCacheManager(config.build(), true);

	}
	
	private RemoteCacheManager getAuthenticatedRemoteCacheManager() {
		System.out.println("Get authenticated remote cache manager");
		ConfigurationBuilder config = new ConfigurationBuilder();
		config.addServer()
				.host(host)
				.port(port)
				.marshaller(new ProtoStreamMarshaller())
				.maxRetries(0)
				.security()
				.authentication()
				.serverName(this.serverName)
				.saslMechanism(saslMechanism)
				.callbackHandler(
						new LoginHandler(user, password.toCharArray(),
								this.serverRealm)).enable();
		RemoteCacheManager cacheManager = new RemoteCacheManager(
				config.build(), true);
		return cacheManager;
	}
	
	private void registerSchemasAndMarshallers() {
		// Register entity marshallers on the client side ProtoStreamMarshaller
		// instance associated with the remote cache manager.
		SerializationContext ctx = ProtoStreamMarshaller
				.getSerializationContext(cacheManager);
		try {
			ctx.registerProtoFiles(FileDescriptorSource
					.fromResources(PROTOBUF_DEFINITION_RESOURCE));
		} catch (DescriptorParserException | IOException e) {
			e.printStackTrace();
		}
		ctx.registerMarshaller(new PersonMarshaller());
		ctx.registerMarshaller(new PhoneNumberMarshaller());
		ctx.registerMarshaller(new PhoneTypeMarshaller());

		// register the schemas with the server too
		RemoteCache<String, String> metadataCache = cacheManager
				.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
		metadataCache.put(PROTOBUF_DEFINITION_RESOURCE,
				readResource(PROTOBUF_DEFINITION_RESOURCE));
		String errors = metadataCache
				.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
		if (errors != null) {
			throw new IllegalStateException(
					"Some Protobuf schema files contain errors:\n" + errors);
		}
	}
	
	private String getPrefixeProperty(String prefix,String propName) {
		return props.getProperty(String.format("%s.%s", prefix,propName));
	}
	private String getPrefixeProperty(String prefix,String propName,String defaultValue) {
		return props.getProperty(String.format("%s.%s", prefix,propName),defaultValue);
	}
	
	
	
	private String readResource(String resourcePath) {
		InputStream is = getClass().getResourceAsStream(resourcePath);
		try {
			final Reader reader = new InputStreamReader(is, "UTF-8");
			StringWriter writer = new StringWriter();
			char[] buf = new char[1024];
			int len;
			while ((len = reader.read(buf)) != -1) {
				writer.write(buf, 0, len);
			}
			return writer.toString();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try { is.close(); } catch(IOException ioe) {}
		}
	}
	
	@PrePassivate
	public void destroy() {
		if(this.cacheManager!=null) {
			cacheManager.stop();
		}
	}
	
	public ConnectionDetails getConnectionDetails() {
		ConnectionDetails conn = new ConnectionDetails();
		conn.setSupportedConfigurations(supportedConfigurations);
		if(cacheManager!=null && cacheManager.isStarted() && cache != null) {
			conn.setConnected(true);
			conn.setSize(cache.size());
			conn.setHost(host);
			conn.setPort(port);
			conn.setSecurity(type);
			conn.setUser(user);
			
		} else {
			conn.setConnected(false);
		}
		return conn;
	}


	public void populateWithGenerateEntries(int numberofentires) {

		Random r = new Random();
		String[] firstNames = new String[] { "Keira", "Sandee", "Roslyn",
				"Leland", "Joella", "Anneliese", "Janita", "Marcella",
				"Adella", "Iraida", "Laquanda", "Jonie", "Juliean", "Paige",
				"Berta", "Denver", "Francis", "Ardella", "Alfonzo", "Velvet" };
		String[] lastNames = new String[] { "Ladwig", "Rains", "Kappel",
				"Kulikowski", "Fink", "Morein", "Griffieth", "Hannahs",
				"Peacock", "Motter", "Pfaff", "Zieman", "Nutt", "Hart",
				"Seiden", "Weeks", "Richert", "Rivero", "Zale", "Ecklund" };
		String[] emailDomains = new String[] { "company-one.com",
				"company-two.com", "company-three.com" };
		for (int i = 0; i < numberofentires ; i++) {
			Person p = new Person();
			while (true) {
				p.setFirstname(firstNames[r.nextInt(20)]);
				p.setSurname(lastNames[r.nextInt(20)]);
				if (!isDuplicate(p)) {
					break;
				} else {
					log.info(String.format(
							"%s %s is a duplicate, generating new name",
							p.getFirstname(), p.getSurname()));
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
			p.setEmail(p.getFirstname().toLowerCase() + "."
					+ p.getSurname().toLowerCase() + "@"
					+ emailDomains[r.nextInt(3)]);
			put(p);
		}
	}


	@SuppressWarnings("serial")
	public class NoSuchPersonException extends RuntimeException {}

	@SuppressWarnings("serial")
	public class MultipleSearchHitsException extends RuntimeException {}
	
	@SuppressWarnings("serial")
	public class UnsupportedConfigurationException extends RuntimeException {}
	
	@SuppressWarnings("serial")
	public class NotConnectedException extends RuntimeException {}


}

package org.jboss.datagrid.demo.model;

import java.util.Map;

public class ConnectionDetails {
	private boolean connected;
	private String host;
	private int port;
	private String user;
	private String security;
	private int size;
	protected Map<String,String> supportedConfigurations;
	
	public boolean isConnected() {
		return connected;
	}
	public void setConnected(boolean connected) {
		this.connected = connected;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getSecurity() {
		return security;
	}
	public void setSecurity(String security) {
		this.security = security;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public int getSize() {
		return size;
	}
	public Map<String, String> getSupportedConfigurations() {
		return supportedConfigurations;
	}
	public void setSupportedConfigurations(Map<String, String> supportedConfigurations) {
		this.supportedConfigurations = supportedConfigurations;
	}
	
	
	
}

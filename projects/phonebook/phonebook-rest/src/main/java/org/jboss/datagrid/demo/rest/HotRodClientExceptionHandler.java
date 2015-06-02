package org.jboss.datagrid.demo.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.infinispan.client.hotrod.exceptions.HotRodClientException;

@Provider
public class HotRodClientExceptionHandler implements ExceptionMapper<HotRodClientException> {
	
	 @Override
	    public Response toResponse(HotRodClientException ex)
	    {
	        return Response.status(Status.FORBIDDEN).entity(ex.getMessage()).build(); 
	    }
}

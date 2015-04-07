package org.bch.i2me2.core.rest;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Just a demo Class to show how rest methods are implemented
 * @author CH176656
 *
 */
@Path("/echo")
@RequestScoped
@Stateful
public class Echo {
    @GET
    @Path("/getEcho/{var}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEcho(@PathParam("var") String var, @Context SecurityContext sc) {
        try {
            String ret = "Echo: " + var.trim();
            ReturnDTO retDTO = new ReturnDTO();
            retDTO.setVar(ret);
            return Response.status(Response.Status.OK).entity(retDTO).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/echo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response postEcho(@QueryParam("var") String var, @Context SecurityContext sc) {
        try {
            String ret = "Echo: " + var.trim();
            ReturnDTO retDTO = new ReturnDTO();
            retDTO.setVar(ret);
            return Response.status(Response.Status.OK).entity(retDTO).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/echoPost")
    @Produces("text/plain")
    @Consumes("text/plain")
    public Response postEchoExtended(String content, @Context SecurityContext sc) {
        return Response.status(Response.Status.OK).entity(content).build();
    }

    static public class ReturnDTO {
        private String var;

        public String getVar() {
            return var;
        }
    
        public void setVar(String var) {
            this.var = var;
        }
       
    }

}

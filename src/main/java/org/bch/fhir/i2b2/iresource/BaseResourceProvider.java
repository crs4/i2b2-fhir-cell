package org.bch.fhir.i2b2.iresource;


import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;

import javax.servlet.http.HttpServletRequest;

public class BaseResourceProvider {
    protected String [] getCredentials(HttpServletRequest theRequest) throws AuthenticationException {
        String authHeader = theRequest.getHeader("Authentication");
        if (authHeader == null || authHeader.isEmpty()) {
            throw new AuthenticationException("Missing or invalid Authentication header");
        }
        String [] credentials = authHeader.split(":");
        if (credentials.length != 2) {
            throw new AuthenticationException("invalid Authentication header");
        }
        return credentials;
    }
}

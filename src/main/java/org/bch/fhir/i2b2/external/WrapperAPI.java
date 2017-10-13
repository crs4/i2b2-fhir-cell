package org.bch.fhir.i2b2.external;

import org.bch.fhir.i2b2.config.AppConfig;
import org.bch.fhir.i2b2.exception.FHIRI2B2Exception;
import org.bch.fhir.i2b2.util.HttpRequest;
import org.bch.fhir.i2b2.util.SoapRequest;

import javax.inject.Inject;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract Wwapper class to provide comming methods for external systems interactions through http
 * @author CHIP-IHL
 */
public class WrapperAPI {
    protected String user;
    protected String pwd;

    @Inject
    private HttpRequest httpRequest;

    @Inject
    private SoapRequest soapRequest;

    private Logger LOG;

    protected void log(Level level, String message) {
        if (this.LOG == null) {
            this.LOG = Logger.getAnonymousLogger();
        }
        this.LOG.log(level, message);
    }

    /**
     * Returns the http Request
     * @return The request
     */
    public HttpRequest getHttpRequest() {
        if (this.httpRequest==null) {
            this.httpRequest=new HttpRequest();
        }
        return this.httpRequest;
    }

    /**
     * Returns the soap request
     * @return The request
     */
    public SoapRequest getSoapRequest() {
        if (this.soapRequest==null) {
            this.soapRequest = new SoapRequest();
        }
        return this.soapRequest;
    }
    //**************************
    // For testing purposes only
    //**************************
    public void setHttpRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }
    public void setSoapRequest(SoapRequest soapRequest) { this.soapRequest = soapRequest;}
    public void setCredentials(String user, String pwd) {
        this.user = user;
        this.pwd = pwd;
    }

    public String [] getCredentials() {
        return new String []{user, pwd};
    }

    public String getUser(){
        return user;
    }

    public String getPwd(){
        return pwd;
    }

    public void loadDefaultCredentials() throws FHIRI2B2Exception {
        String credentials=null;
        try {
            credentials = AppConfig.getAuthCredentials(AppConfig.CREDENTIALS_FILE_I2B2);
        } catch (IOException e) {
            // It means the file does not exists
        }

        if (credentials!=null) {
            String[] usrpwd = credentials.split(":");
            user = usrpwd[0];
            if (usrpwd.length > 1) {
                pwd = usrpwd[1];
            }
        }
    }


}

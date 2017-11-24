package org.bch.fhir.i2b2.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.bch.fhir.i2b2.iresource.DiagnosticReportResourceProvider;
import org.bch.fhir.i2b2.iresource.ObservationResourceProvider;
import org.bch.fhir.i2b2.iresource.PatientResourceProvider;
import org.bch.fhir.i2b2.iresource.QuestionnaireResponseResourceProvider;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;

/**
 * The FHIR servlet
 * @author CHIP-IHL
 */
public class FHIRServlet extends RestfulServer {
    private static final long serialVersionUID = 1L;

    @Override
    protected void initialize() throws ServletException {
      /*
       * The servlet defines any number of resource providers, and
       * configures itself to use them by calling
       * setResourceProviders()
       */
        setFhirContext(FhirContext.forDstu2());

        List<IResourceProvider> resourceProviders = new ArrayList<>();
	    resourceProviders.add(new QuestionnaireResponseResourceProvider());
        resourceProviders.add(new ObservationResourceProvider());
        resourceProviders.add(new PatientResourceProvider());
        resourceProviders.add(new DiagnosticReportResourceProvider());
        setResourceProviders(resourceProviders);
        setUseBrowserFriendlyContentTypes(true);
    }
}

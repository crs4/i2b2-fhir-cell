package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import org.bch.fhir.i2b2.exception.FHIRI2B2Exception;
import org.bch.fhir.i2b2.pdomodel.PDOModel;

/**
 * Created by mauro on 29/11/17.
 */
public class EncounterToI2B2 extends FHIRToPDO  {

    @Override
    public String getPDOXML(BaseResource resource) throws FHIRI2B2Exception {
        Encounter enc = (Encounter) resource;

        this.eventIde = this.getEventId(enc);
        this.patientIde = getPatientId(enc);
        this.eventIdeSource = enc.getServiceProvider().getReference().getIdPart();

        PDOModel pdo = new PDOModel();
        pdo.addElementSet(generateEventSet(enc));
        return pdo.generatePDOXML();
    }

    protected  String getPatientId(Encounter enc) throws FHIRI2B2Exception {
        ResourceReferenceDt refPatient = enc.getPatient();
        if (refPatient.isEmpty()) throw new FHIRI2B2Exception("Subject reference is not informed");
        return refPatient.getReference().getIdPart();
    }
}

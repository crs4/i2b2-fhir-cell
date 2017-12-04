package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import org.bch.fhir.i2b2.exception.FHIRI2B2Exception;
import org.bch.fhir.i2b2.pdomodel.Element;
import org.bch.fhir.i2b2.pdomodel.ElementSet;
import org.bch.fhir.i2b2.pdomodel.PDOModel;

/**
 * Created by mauro on 29/11/17.
 */
public class EncounterToI2B2 extends FHIRToPDO  {
    private ObservationToI2B2 observationToI2B2 = new ObservationToI2B2();


    @Override
    public String getPDOXML(BaseResource resource) throws FHIRI2B2Exception {
        return getPDO(resource).generatePDOXML();
    }
    @Override
    protected PDOModel getPDO(BaseResource resource) throws FHIRI2B2Exception {
        Encounter enc = (Encounter) resource;

        this.eventIde = this.getEventId(enc);
        this.patientIde = getPatientId(enc);
        System.out.println("this.patientIde " + this.patientIde);
//        this.eventIdeSource = enc.getServiceProvider().getReference().getIdPart();
        this.eventIdeSource = "HIVE";
        this.patientIdeSource = this.eventIdeSource;
        PDOModel pdo = new PDOModel();
        pdo.addElementSet(generateEventSet(enc));
        pdo.addElementSet(generateEIDSet());
        return pdo;
    }

    protected  String getPatientId(Encounter enc) throws FHIRI2B2Exception {
        ResourceReferenceDt refPatient = enc.getPatient();
        if (refPatient.isEmpty()) throw new FHIRI2B2Exception("Subject reference is not informed");
        return refPatient.getReference().getIdPart();
    }
}

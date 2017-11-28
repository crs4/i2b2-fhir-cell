package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import org.apache.axis2.databinding.types.soapencoding.DateTime;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bch.fhir.i2b2.config.AppConfig;
import org.bch.fhir.i2b2.exception.FHIRI2B2Exception;
import org.bch.fhir.i2b2.pdomodel.Element;
import org.bch.fhir.i2b2.pdomodel.ElementSet;
import org.bch.fhir.i2b2.pdomodel.PDOModel;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by mauro on 20/11/17.
 */
public class DiagnosticReportToI2B2 extends FHIRToPDO {
    Log log = LogFactory.getLog(ObservationToI2B2.class);
    private DiagnosticReport report;
    private ObservationToI2B2 observationToI2B2 = new ObservationToI2B2();

    @Override
    public String getPDOXML(BaseResource resource) throws FHIRI2B2Exception {
        this.report = (DiagnosticReport) resource;
        Observation obs = new Observation();

        obs.setEncounter(report.getEncounter());
        obs.setSubject(report.getSubject());
        obs.setCode(report.getCodedDiagnosis().get(0));
        obs.setValue(report.getCodedDiagnosis().get(0).getCoding().get(0).getDisplayElement());
        obs.setEffective(report.getEffective());
        List<ResourceReferenceDt> performerList = new ArrayList<>();
        performerList.add(report.getPerformer());
        obs.setPerformer(performerList);

        PDOModel pdoModel = observationToI2B2.getPDO(obs);

        System.out.println("this.report.getContained().getContainedResources().size() " + this.report.getContained().getContainedResources().size());
        for (IResource ir : this.report.getContained().getContainedResources()){
            PDOModel containedPDO = observationToI2B2.getPDO((BaseResource) ir);

            String [] pdo_set_array = new String[]{
                    ElementSet.PDO_OBSERVATION_SET,
                    ElementSet.PDO_EVENT_SET,
                    ElementSet.PDO_EID_SET,
//                    ElementSet.PDO_PATIENT_SET,
                    ElementSet.PDO_PID_SET,
                    ElementSet.PDO_CONCEPT_SET,
            };

            for (String pdo_set: pdo_set_array) {
                for(Element el: containedPDO.getElementSet(pdo_set).getElements()) {
                    pdoModel.getElementSet(pdo_set).addElement(el);
                }
            }
        }

        return pdoModel.generatePDOXML();
    }
}

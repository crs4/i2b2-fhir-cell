package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.*;
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
        return getPDO(resource).generatePDOXML();
    }



    @Override
    protected PDOModel getPDO(BaseResource resource) throws FHIRI2B2Exception {
        this.report = (DiagnosticReport) resource;
        PDOModel pdoModel = new PDOModel();

        this.patientIde = report.getSubject().getReference().getIdPart();
        Patient patient = (Patient) findResourceById(report.getContained().getContainedResources(), this.patientIde);
        if (patient != null) {
//            this.patientIdeSource = patient.getManagingOrganization().getReference().getIdPart();
            PatientToI2B2 patientToI2B2 = new PatientToI2B2();
            patientToI2B2.patientIde = this.patientIde;
            patientToI2B2.patientIdeSource = this.patientIdeSource;
            patientToI2B2.eventIdeSource = this.eventIdeSource;
            pdoModel.addElementSet(patientToI2B2.generatePatientSet(patient));
        }
        else {
            pdoModel.addElementSet(generatePatientSet());
        }

        this.eventIde = report.getEncounter().getReference().getIdPart();
        Encounter enc = (Encounter) findResourceById(report.getContained().getContainedResources(), this.eventIde);
        if (enc != null) {
//            this.eventIdeSource = enc.getServiceProvider().getReference().getIdPart();

        }
        else {
            enc = new Encounter();
            enc.setId(this.eventIde);
        }


        pdoModel.addElementSet(generateEIDSet());
        pdoModel.addElementSet(generatePIDSet());
        pdoModel.addElementSet(generateEventSet(enc));
        pdoModel.addElementSet(generateObservationSet(report));






//        Observation obs = new Observation();
//        Encounter enc = new Encounter();
//        enc.setId(report.getEncounter().getReference().getIdPart());
//
//        PeriodDt period;
//        try {
//            period = (PeriodDt) report.getEffective();
//
//        }
//        catch (ClassCastException castException) {
//            period = new PeriodDt();
//            DateTimeDt datetime = (DateTimeDt) report.getEffective();
//            period.setStart(datetime);
//            period.setEnd(datetime);
//
//        }
//        enc.setPeriod(period);
//        obs.setEffective(period.getStartElement());
//
//        obs.setEncounter(report.getEncounter());
//        obs.setSubject(report.getSubject());
//        if (!report.getCodedDiagnosis().isEmpty()) {
//            obs.setCode(report.getCodedDiagnosis().get(0));
//            obs.setValue(report.getCodedDiagnosis().get(0).getCoding().get(0).getDisplayElement());
//        }
////        obs.setEffective(report.getEffective());
//        List<ResourceReferenceDt> performerList = new ArrayList<>();
//        performerList.add(report.getPerformer());
//        obs.setPerformer(performerList);
//        ContainedDt contained = new ContainedDt();
//        List<IResource> containedList = report.getContained().getContainedResources();
//        containedList.add(enc);
//        contained.setContainedResources(containedList);
//        obs.setContained(contained);
//        PDOModel pdoModel = observationToI2B2.getPDO(obs);
//
//
//        System.out.println("this.report.getContained().getContainedResources().size() " + this.report.getContained().getContainedResources().size());
//        for (IResource ir : this.report.getContained().getContainedResources()){
//            if (ir instanceof Observation) {
//                PDOModel containedPDO = observationToI2B2.getPDO((BaseResource) ir);
//
//                String [] pdo_set_array = new String[]{
//                        ElementSet.PDO_OBSERVATION_SET,
//                        ElementSet.PDO_EVENT_SET,
//                        ElementSet.PDO_EID_SET,
////                    ElementSet.PDO_PATIENT_SET,
//                        ElementSet.PDO_PID_SET,
//                        ElementSet.PDO_CONCEPT_SET,
//                };
//
//                for (String pdo_set: pdo_set_array) {
//                    for(Element el: containedPDO.getElementSet(pdo_set).getElements()) {
//                        pdoModel.getElementSet(pdo_set).addElement(el);
//                    }
//                }
//            }
//        }
//
//
////        this.eventIde = this.getEventId(enc);
////        this.patientIde = this.eventIde;
////        pdoModel.addElementSet(generateEventSet(enc));

        return pdoModel;
    }

    protected ElementSet generateObservationSet(DiagnosticReport report) throws FHIRI2B2Exception {
        ElementSet observationSet = new ElementSet();
        observationSet.setTypePDOSet(ElementSet.PDO_OBSERVATION_SET);
        Observation obs;

        ObservationToI2B2 observationToI2B2 = new ObservationToI2B2();
        observationToI2B2.patientIdeSource = this.patientIdeSource;
        observationToI2B2.patientIde = this.patientIde;
        observationToI2B2.eventIde = this.eventIde;
        observationToI2B2.eventIde = this.eventIde;

        for (CodeableConceptDt diagnosis: report.getCodedDiagnosis()) {
            obs = new Observation();
            PeriodDt period;
            try {
                period = (PeriodDt) report.getEffective();

            }
            catch (ClassCastException castException) {
                period = new PeriodDt();
                DateTimeDt datetime = (DateTimeDt) report.getEffective();
                period.setStart(datetime);
                period.setEnd(datetime);

            }
            obs.setEffective(period.getStartElement());
            obs.setEncounter(report.getEncounter());
            obs.setSubject(report.getSubject());

            obs.setCode(diagnosis);
            obs.setValue(diagnosis.getCoding().get(0).getDisplayElement());

    //        obs.setEffective(report.getEffective());
            List<ResourceReferenceDt> performerList = new ArrayList<>();
            performerList.add(report.getPerformer());
            obs.setPerformer(performerList);
            ElementSet obsSet = observationToI2B2.generateObservationSet(obs);
            observationSet.addElement(obsSet.getElements().get(0));

        }
        return observationSet;
    }
}

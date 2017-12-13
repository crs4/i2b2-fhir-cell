package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.*;
import ca.uhn.fhir.model.primitive.CodeDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.apache.axis2.databinding.types.soapencoding.DateTime;
import org.apache.bcel.classfile.Code;
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
    private Patient patient;
    private Encounter enc;
    private ObservationToI2B2 observationToI2B2 = new ObservationToI2B2();
    private PatientToI2B2 patientToI2B2 = new PatientToI2B2();


    @Override
    public String getPDOXML(BaseResource resource) throws FHIRI2B2Exception {
        return getPDO(resource).generatePDOXML();
    }


    @Override
    protected PDOModel getPDO(BaseResource resource) throws FHIRI2B2Exception {
        report = (DiagnosticReport) resource;
        PDOModel pdoModel = new PDOModel();

        this.patientIde = report.getSubject().getReference().getIdPart();
        this.patient = (Patient) findResourceById(report.getContained().getContainedResources(), this.patientIde);
        if (patient != null) {
            this.patientIdeSource = patient.getManagingOrganization().getReference().getIdPart();
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
            this.eventIdeSource = enc.getServiceProvider().getReference().getIdPart();

        }
        else {
            enc = new Encounter();
            enc.setId(this.eventIde);
        }

        pdoModel.addElementSet(generateEIDSet());
        pdoModel.addElementSet(generatePIDSet());
        pdoModel.addElementSet(generateEventSet(enc));
        pdoModel.addElementSet(generateObservationSet(report));
        pdoModel.addElementSet(generateConceptSet(report));

        return pdoModel;
    }

    protected ElementSet generateConceptSet(DiagnosticReport report) {
        ElementSet conceptSet = new ElementSet();
        conceptSet.setTypePDOSet(ElementSet.PDO_CONCEPT_SET);

        for (CodeableConceptDt diagnosis: report.getCodedDiagnosis()) {
            Element concept = new Element();
            concept.setTypePDO(Element.PDO_CONCEPT);
            CodingDt codingDt = diagnosis.getCoding().get(0);
            String code = codingDt.getCode();
            concept.addRow(generateRow(PDOModel.PDO_CONCEPT_PATH, String.format("Diagnoses\\%s", code)));
            concept.addRow(generateRow(PDOModel.PDO_CONCEPT_CD, code));
            concept.addRow(generateRow(PDOModel.PDO_NAME_CHAR, codingDt.getDisplay()));
            conceptSet.addElement(concept);
        }
        return conceptSet;
    }

    private ElementSet generateObservationSet(DiagnosticReport report) throws FHIRI2B2Exception {
        ElementSet observationSet = new ElementSet();
        observationSet.setTypePDOSet(ElementSet.PDO_OBSERVATION_SET);
        Observation obs;
        ObservationToI2B2 observationToI2B2 = new ObservationToI2B2();
        observationToI2B2.patientIdeSource = this.patientIdeSource;
        observationToI2B2.patientIde = this.patientIde;
        observationToI2B2.eventIde = this.eventIde;
        observationToI2B2.eventIdeSource = this.eventIdeSource;

        List<ResourceReferenceDt> performerList = new ArrayList<>();
        performerList.add(report.getPerformer());

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

        for (CodeableConceptDt diagnosis: report.getCodedDiagnosis()) {
            obs = new Observation();

            obs.setEffective(period.getStartElement());
            obs.setEncounter(report.getEncounter());
            obs.setSubject(report.getSubject());

            obs.setCode(diagnosis);
            obs.setValue(diagnosis.getCoding().get(0).getDisplayElement());

    //        obs.setEffective(report.getEffective());
            obs.setPerformer(performerList);
            ElementSet obsSet = observationToI2B2.generateObservationSet(obs);
            observationSet.addElement(obsSet.getElements().get(0));

        }

        String gender = patient.getGender();
        if (gender != null) {
            Observation genderObs = new Observation();
            String system = "DEM|SEX:", code, value;
            switch (gender.toLowerCase()){
                case "male": system += "m"; code = "\\i2b2\\Demographics\\Gender\\Male\\"; value = "male"; break;
                case "female": system += "f"; code = "\\i2b2\\Demographics\\Gender\\Female\\"; value = "female"; break;
                default: system += "u"  ; code = "\\i2b2\\Demographics\\Gender\\Unknown\\Unknown-U\\"; value = "unknown"; break;
            }
            genderObs.setCode(new CodeableConceptDt(system, code));
            genderObs.setValue(new StringDt(value));
//            genderObs.setPerformer(performerList);

            genderObs.setEffective(period.getStartElement());
            genderObs.setEncounter(report.getEncounter());
            genderObs.setSubject(report.getSubject());

            ElementSet obsSet = observationToI2B2.generateObservationSet(genderObs);
            observationSet.addElement(obsSet.getElements().get(0));

        }


        return observationSet;
    }

}

package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Observation;
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
    private CodingDt codedDiagnosis;

    @Override
    public String getPDOXML(BaseResource resource) throws FHIRI2B2Exception {
        this.report = (DiagnosticReport) resource;
        PDOModel pdo = new PDOModel();

        if (report != null) {
            String uri = report.getSubject().getReference().getBaseUrl();
            System.out.println("uri " + uri);
            if (uri != null) {
                this.patientIdeSource = uri;
            }
            else {
                this.patientIdeSource = "@";
            }

        }
        this.codedDiagnosis = this.report.getCodedDiagnosis().get(0).getCoding().get(0);
        this.patientIde = this.getPatientId();
        pdo.addElementSet(generatePIDSet());

        pdo.addElementSet(generateConceptSet());
        pdo.addElementSet(generateObservationSet());
        pdo.addElementSet(generateObserverSet());

        System.out.println("performer " + report.getPerformer().getReference().getIdPart());

        return pdo.generatePDOXML();
    }

    private String getPatientId() throws FHIRI2B2Exception {
        ResourceReferenceDt refPatient = report.getSubject();
        if (refPatient.isEmpty()) throw new FHIRI2B2Exception("Subject reference is not informed");
        String idPat = refPatient.getReference().getIdPart();
        return idPat;
    }

    protected ElementSet generateConceptSet() throws FHIRI2B2Exception {
        ElementSet conceptSet = new ElementSet();
        conceptSet.setTypePDOSet(ElementSet.PDO_CONCEPT_SET);
        Element concept = new Element();
        concept.setTypePDO(Element.PDO_CONCEPT);

        String conceptCd = this.generateRow(PDOModel.PDO_CONCEPT_CD, "/Diagnoses/" + codedDiagnosis.getCode());
        String nameChar = this.generateRow(PDOModel.PDO_NAME_CHAR, codedDiagnosis.getDisplay());
        concept.addRow(conceptCd);
        concept.addRow(nameChar);
        conceptSet.addElement(concept);
        return conceptSet;
    }

    private String generatePatientID(){
        return this.generateRow(
                PDOModel.PDO_PATIENT_ID, this.patientIde, genParamStr(PDOModel.PDO_SOURCE, this.patientIdeSource)
        );
    }

    private ElementSet generateObservationSet() throws FHIRI2B2Exception {
        ElementSet observationSet = new ElementSet();
        observationSet.setTypePDOSet(ElementSet.PDO_OBSERVATION_SET);

        Element observation = new Element();
        observation.setTypePDO(Element.PDO_OBSERVATION);
        observationSet.addElement(observation);
        observation.addRow(generatePatientID());
        observation.addRow(this.generateRow(PDOModel.PDO_CONCEPT_CD, codedDiagnosis.getCode()));
        observation.addRow(this.generateRow(PDOModel.PDO_OBSERVER_CD, report.getPerformer().getReference().getIdPart()));

        for (IResource obs : this.report.getContained().getContainedResources()){
            Element observationContained = new Element();
            observationContained.setTypePDO(Element.PDO_OBSERVATION);
            observationContained.addRow(generatePatientID());
            observationContained.addRow(
                    this.generateRow(PDOModel.PDO_CONCEPT_CD, ((Observation) obs).getCode().getText())
            );
            observationContained.addRow(
                    this.generateRow(PDOModel.PDO_OBSERVER_CD, ((Observation) obs).getPerformer().get(0).getReference().getIdPart())
            );
            observationSet.addElement(observationContained);



        }
        return observationSet;

    }

    private ElementSet generateObserverSet() throws FHIRI2B2Exception {
        ElementSet observerSet = new ElementSet();
        observerSet.setTypePDOSet(ElementSet.PDO_OBSERVER_SET);



//        observers from contained observation
        List<ResourceReferenceDt> performersList = new ArrayList<>();
        performersList.add(report.getPerformer());
        for (IResource obs : this.report.getContained().getContainedResources()) {
            performersList.addAll(((Observation) obs).getPerformer());
        }

        for (ResourceReferenceDt performer: performersList){
            Element observer = new Element();
            observer.setTypePDO(Element.PDO_OBSERVER);

            String [] observerPathArray = performer.getReference().getValue().split("/");
            String observerPath = StringUtils.join(
                    Arrays.copyOfRange(observerPathArray, 1, observerPathArray.length - 1), "\\"
            );

            observer.addRow(this.generateRow(PDOModel.PDO_OBSERVER_PATH, observerPath));
            observer.addRow(this.generateRow(PDOModel.PDO_OBSERVER_CD, performer.getReference().getIdPart()));
            observerSet.addElement(observer);
        }

        return observerSet;

    }

}

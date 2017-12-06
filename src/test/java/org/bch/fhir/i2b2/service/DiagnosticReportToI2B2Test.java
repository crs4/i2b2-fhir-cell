package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.parser.IParser;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.print.Doc;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by mauro on 20/11/17.
 */
public class DiagnosticReportToI2B2Test extends BaseTest {

    @Test
    public void basicTest() throws Exception {
        DiagnosticReportToI2B2 diagnosticReportToI2B2 = new DiagnosticReportToI2B2();
        DiagnosticReport report = (DiagnosticReport) parseFile(DiagnosticReport.class, "DiagnosticReport.json");
        String xml = diagnosticReportToI2B2.getPDOXML(report);
        String source = "http://fake_fse.it";

        System.out.println(xml);
        Document doc = parseXMLString(xml);
        XPath xpath = XPathFactory.newInstance().newXPath();

        NodeList conceptList = (NodeList) xpath.evaluate("/patient_data/concept_set/concept", doc, XPathConstants.NODESET);
        assertEquals(1, conceptList.getLength());

        Node conceptCd = (Node) xpath.evaluate("/patient_data/concept_set/*[1]/concept_cd[text()='Sarcoma, NAS']", doc, XPathConstants.NODE);
        assertNotNull(conceptCd);

        Node conceptPath = (Node) xpath.evaluate("/patient_data/concept_set/*[1]/concept_path[text()='Diagnoses\\Sarcoma, NAS']", doc, XPathConstants.NODE);
        assertNotNull(conceptPath);

        Node conceptNameChar = (Node) xpath.evaluate("/patient_data/concept_set/*[1]/name_char[text()='Sarcoma, NAS']", doc, XPathConstants.NODE);
        assertNotNull(conceptNameChar);



        Node patientSetPatientID = (Node) xpath.evaluate("/patient_data/pid_set/pid/patient_id", doc, XPathConstants.NODE);
        String patientID = patientSetPatientID.getTextContent();
        assertEquals("7996923", patientID);

//        NodeList observationList = doc.getElementsByTagName("observation");
//        Assert.assertEquals(1, observationList.getLength());
//
////        NodeList conceptList = doc.getElementsByTagName("concept");
////        Assert.assertEquals(1, conceptList.getLength());
//
//
////        Node conceptPath = (Node) xpath.evaluate("/patient_data/concept_set/*[1]/concept_path[text()='Sarcoma, NAS']", doc, XPathConstants.NODE);
////        assertNotNull(conceptPath);
////        Node conceptCD = (Node) xpath.evaluate("/patient_data/concept_set/concept/concept_cd[text()='Sarcoma, NAS']", doc, XPathConstants.NODE);
////        assertNotNull(conceptCD);
////        Node  nameChar = (Node) xpath.evaluate("/patient_data/concept_set/concept/name_char[text()='Sarcoma, NAS']", doc, XPathConstants.NODE);
////        assertNotNull(nameChar);
//
//        Node observationDiagnosisPatientID1 = (Node) xpath.evaluate("/patient_data/observation_set/*[1]/patient_id[text()='7996923']", doc, XPathConstants.NODE);
//        assertNotNull(observationDiagnosisPatientID1);
//
//        Node observationDiagnosisEventID1 = (Node) xpath.evaluate("/patient_data/observation_set/*[1]/event_id[text()='483572']", doc, XPathConstants.NODE);
//        assertNotNull(observationDiagnosisEventID1);
//
//        Node observationModifier1 = (Node) xpath.evaluate("/patient_data/observation_set/*[1]/modifier_cd[text()='@']", doc, XPathConstants.NODE);
//        assertNotNull(observationModifier1);
//
//
//        Node observationDiagnosisConceptCD1 = (Node) xpath.evaluate("/patient_data/observation_set/*[1]/concept_cd[text()='Sarcoma, NAS']", doc, XPathConstants.NODE);
//        assertNotNull(observationDiagnosisConceptCD1);
////
//        Node observationDiagnosisStartDate1 = (Node) xpath.evaluate(
//                "/patient_data/observation_set/*[1]/start_date[text()='1850-06-12T00:00:00.00']", doc, XPathConstants.NODE);
//        assertNotNull(observationDiagnosisStartDate1);
////
//
//

    }


}

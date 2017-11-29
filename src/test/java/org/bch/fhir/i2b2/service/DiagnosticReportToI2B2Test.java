package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.context.FhirContext;
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
public class DiagnosticReportToI2B2Test {
    protected FhirContext ctx = FhirContext.forDstu2();

    private  DiagnosticReport parseFile(String fileName) throws Exception {
        IParser parser = ctx.newJsonParser();
        InputStream in = DiagnosticReportToI2B2Test.class.getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        return parser.parseResource(DiagnosticReport.class, br);

    }

    private static Document parseXMLString(String xml) throws ParserConfigurationException, IOException,  org.xml.sax.SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    @Test
    public void basicTest() throws Exception {
        DiagnosticReport report = parseFile("DiagnosticReport.json");
        DiagnosticReportToI2B2 diagnosticReportToI2B2 = new DiagnosticReportToI2B2();
        String xml = diagnosticReportToI2B2.getPDOXML(report);
        String source = "http://fake_fse.it";

        System.out.println(xml);
        Document doc = parseXMLString(xml);
        XPath xpath = XPathFactory.newInstance().newXPath();

        Node patientSetPatientID = (Node) xpath.evaluate("/patient_data/pid_set/pid/patient_id", doc, XPathConstants.NODE);
        String patientID = patientSetPatientID.getTextContent();
        assertEquals(patientID, "7996923");

        NodeList observationList = doc.getElementsByTagName("observation");
        Assert.assertTrue(observationList.getLength() == 2);

        NodeList conceptList = doc.getElementsByTagName("concept");
        Assert.assertTrue(conceptList.getLength() == 2);


        Node conceptPath = (Node) xpath.evaluate("/patient_data/concept_set/*[1]/concept_path[text()='Sarcoma, NAS']", doc, XPathConstants.NODE);
        assertNotNull(conceptPath);
        Node conceptCD = (Node) xpath.evaluate("/patient_data/concept_set/concept/concept_cd[text()='Sarcoma, NAS']", doc, XPathConstants.NODE);
        assertNotNull(conceptCD);
        Node  nameChar = (Node) xpath.evaluate("/patient_data/concept_set/concept/name_char[text()='Sarcoma, NAS']", doc, XPathConstants.NODE);
        assertNotNull(nameChar);

        Node observationDiagnosisPatientID1 = (Node) xpath.evaluate("/patient_data/observation_set/*[1]/patient_id[text()='7996923']", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisPatientID1);

        Node observationDiagnosisPatientID2 = (Node) xpath.evaluate("/patient_data/observation_set/*[2]/patient_id[text()='7996923']", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisPatientID2);

        Node observationDiagnosisEventID1 = (Node) xpath.evaluate("/patient_data/observation_set/*[1]/event_id[text()='483572']", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisEventID1);

        Node observationModifier1 = (Node) xpath.evaluate("/patient_data/observation_set/*[1]/modifier_cd[text()='@']", doc, XPathConstants.NODE);
        assertNotNull(observationModifier1);

        Node observationModifier2 = (Node) xpath.evaluate("/patient_data/observation_set/*[2]/modifier_cd[text()='@']", doc, XPathConstants.NODE);
        assertNotNull(observationModifier2);
//
        Node observationDiagnosisConceptCD1 = (Node) xpath.evaluate("/patient_data/observation_set/*[1]/concept_cd[text()='Sarcoma, NAS']", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisConceptCD1);
//
        Node observationDiagnosisStartDate1 = (Node) xpath.evaluate(
                "/patient_data/observation_set/*[1]/start_date[text()='1850-06-12T00:00:00.00']", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisStartDate1);
//
//        Node observationDiagnosisEndDate1 = (Node) xpath.evaluate(
//                "/patient_data/observation_set/*[1]/end_date[text()='2012-12-01T11:00:00.00']", doc, XPathConstants.NODE);
//        assertNotNull(observationDiagnosisEndDate1);
        Node observationDiagnosisConceptCD2 = (Node) xpath.evaluate("/patient_data/observation_set/*[2]/concept_cd[text()='185055488771']", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisConceptCD2);


        Node observationStartDate2 = (Node) xpath.evaluate(
                "/patient_data/observation_set/*[2]/start_date[text()='1850-06-12T00:00:00.00']", doc, XPathConstants.NODE);
        assertNotNull(observationStartDate2);
//
//        Node observationEndDate2 = (Node) xpath.evaluate(
//                "/patient_data/observation_set/*[2]/end_date[text()='2012-12-01T12:00:00.00']", doc, XPathConstants.NODE);
//        assertNotNull(observationEndDate2);
//
        Node observationDiagnosisEventID2 = (Node) xpath.evaluate("/patient_data/observation_set/*[2]/event_id[text()='483572']", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisEventID2);
//
        Node observationDiagnosisObserverCD1 = (Node) xpath.evaluate("/patient_data/observation_set/*[1]/observer_cd", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisObserverCD1);
        assertEquals(observationDiagnosisObserverCD1.getTextContent(), "I.R.C.C.S. CENTRO RIFERIMENTO ONCOLOGICO");

        Node observationObserverCD2 = (Node) xpath.evaluate("/patient_data/observation_set/*[2]/observer_cd", doc, XPathConstants.NODE);
        assertNotNull(observationObserverCD2);
        assertEquals(observationObserverCD2.getTextContent(), "I.R.C.C.S. CENTRO RIFERIMENTO ONCOLOGICO");

//
//        NodeList observers = (NodeList) xpath.evaluate("/patient_data/observer_set/observer", doc, XPathConstants.NODESET);
//        assertEquals(2, observers.getLength());
//
//
//        Node observerDiagnosisPath1 = (Node) xpath.evaluate(
//                "/patient_data/observer_set/*[1]/observer_path[text()='Friuli Venezia Giulia']",
//                doc,
//                XPathConstants.NODE
//        );
//        assertNotNull(observerDiagnosisPath1);
//
//        Node observerDiagnosisCD1 = (Node) xpath.evaluate(
//                "/patient_data/observer_set/*[1]/observer_cd[text()='I.R.C.C.S. CENTRO RIFERIMENTO ONCOLOGICO']",
//                doc,
//                XPathConstants.NODE
//        );
//        assertNotNull(observerDiagnosisCD1);
//
//        Node observerDiagnosisPath2 = (Node) xpath.evaluate(
//                "/patient_data/observer_set/*[2]/observer_path[text()='Acme\\test']",
//                doc,
//                XPathConstants.NODE
//        );
//        assertNotNull(observerDiagnosisPath2);
//
//        Node observerDiagnosisCD2 = (Node) xpath.evaluate(
//                "/patient_data/observer_set/*[2]/observer_cd[text()='1832473e-2fe0-452d-abe9-3cdb9879522f']",
//                doc,
//                XPathConstants.NODE
//        );
//        assertNotNull(observerDiagnosisCD2);


    }


}

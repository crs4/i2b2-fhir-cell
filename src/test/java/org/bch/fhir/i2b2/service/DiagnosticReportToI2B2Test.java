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
        InputStream in = QAnswerToI2B2Test.class.getResourceAsStream(fileName);
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
        assertEquals(patientID, "SNNSNN56M25B354O");

        assertEquals(
                patientSetPatientID.getAttributes().getNamedItem("source").getNodeValue(),
                source
        );

        Node conceptCD = (Node) xpath.evaluate("/patient_data/concept_set/concept/concept_cd[text()='/Diagnoses/188340000']", doc, XPathConstants.NODE);
        assertNotNull(conceptCD);
        Node  nameChar = (Node) xpath.evaluate("/patient_data/concept_set/concept/name_char[../concept_cd/text()='/Diagnoses/188340000']", doc, XPathConstants.NODE);
        assertNotNull(nameChar);
        assertEquals("Malignant tumor of craniopharyngeal duct", nameChar.getTextContent());

        Assert.assertTrue(doc.getElementsByTagName("observation_set").getLength() == 1);
        NodeList observationList = doc.getElementsByTagName("observation");
        Assert.assertTrue(observationList.getLength() == 2);

        Node observationDiagnosisPatientID1 = (Node) xpath.evaluate("/patient_data/observation_set/*[1]/patient_id", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisPatientID1);

        assertEquals(observationDiagnosisPatientID1.getAttributes().getNamedItem("source").getNodeValue(), source);
        assertEquals(observationDiagnosisPatientID1.getTextContent(), "SNNSNN56M25B354O");

        Node observationDiagnosisPatientID2 = (Node) xpath.evaluate("/patient_data/observation_set/*[2]/patient_id", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisPatientID2);

        assertEquals(observationDiagnosisPatientID2.getAttributes().getNamedItem("source").getNodeValue(), source);
        assertEquals(observationDiagnosisPatientID2.getTextContent(), "SNNSNN56M25B354O");


        Node observationDiagnosisConceptCD1 = (Node) xpath.evaluate("/patient_data/observation_set/*[1]/concept_cd", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisConceptCD1);

        assertEquals(observationDiagnosisConceptCD1.getTextContent(), "188340000");

        Node observationDiagnosisConceptCD2 = (Node) xpath.evaluate("/patient_data/observation_set/*[2]/concept_cd", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisConceptCD2);

        assertEquals(observationDiagnosisConceptCD2.getTextContent(), "Haemoglobin");


        Node observationDiagnosisObserverCD1 = (Node) xpath.evaluate("/patient_data/observation_set/*[1]/observer_cd", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisObserverCD1);
        assertEquals(observationDiagnosisObserverCD1.getTextContent(), "1832473e-2fe0-452d-abe9-3cdb9879522f");

        Node observationDiagnosisObserverCD2 = (Node) xpath.evaluate("/patient_data/observation_set/*[2]/observer_cd", doc, XPathConstants.NODE);
        assertNotNull(observationDiagnosisObserverCD2);
        assertEquals(observationDiagnosisObserverCD2.getTextContent(), "1832473e-2fe0-452d-abe9-3cdb9879522f");



    }

}

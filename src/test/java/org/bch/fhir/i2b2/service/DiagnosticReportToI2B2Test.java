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

        Node patientSetPatientID = (Node) xpath.evaluate("//pid_set/pid/patient_id", doc, XPathConstants.NODE);
        String patientID = patientSetPatientID.getTextContent();
        Assert.assertEquals(patientID, "SNNSNN56M25B354O");

        Assert.assertEquals(
                doc.getElementsByTagName("patient_id").item(0).getAttributes().getNamedItem("source").getNodeValue(),
                source
        );
        String expectedConceptCD = "188340000";
        String conceptCD = doc.getElementsByTagName("concept_cd").item(0).getTextContent();
        Assert.assertEquals(conceptCD, "/Diagnoses/" + expectedConceptCD);

        String nameChar = doc.getElementsByTagName("name_char").item(0).getTextContent();
        Assert.assertEquals(nameChar, "Malignant tumor of craniopharyngeal duct");


        Assert.assertTrue(doc.getElementsByTagName("observation_set").getLength() == 1);
        NodeList observationList = doc.getElementsByTagName("observation");
        Assert.assertTrue(observationList.getLength() == 1);

        NodeList patientList = (NodeList) xpath.evaluate("//observation_set/observation/patient_id", doc, XPathConstants.NODESET);
        Assert.assertTrue(patientList.getLength() == 1);

        Node patient = patientList.item(0);
        Assert.assertEquals(patient.getAttributes().getNamedItem("source").getNodeValue(), source);

        Node obsConceptCD = (Node) xpath.evaluate("//observation_set/observation/concept_cd", doc, XPathConstants.NODE);
        Assert.assertEquals(obsConceptCD.getTextContent(), expectedConceptCD);



    }

}

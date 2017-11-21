package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.parser.IParser;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.print.Doc;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
        System.out.println(xml);
        Document doc = parseXMLString(xml);

        String patientID = doc.getElementsByTagName("patient_id").item(0).getTextContent();
        Assert.assertEquals(patientID, "SNNSNN56M25B354O");

        String source = doc.getElementsByTagName("patient_id").item(0).getAttributes().getNamedItem("source").getNodeValue();
        Assert.assertEquals(source, "http://fake_fse.it");

        String concept_cd = doc.getElementsByTagName("concept_cd").item(0).getTextContent();
        Assert.assertEquals(concept_cd, "188340000");

        String name_char = doc.getElementsByTagName("name_char").item(0).getTextContent();
        Assert.assertEquals(name_char, "Malignant tumor of craniopharyngeal duct");

    }

}

package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.composite.ElementDefinitionDt;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.parser.IParser;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

/**
 * Created by mauro on 29/11/17.
 */
public class BaseTest {
    protected FhirContext ctx = FhirContext.forDstu2();

    protected BaseResource parseFile(Class cls, String fileName) throws Exception {
        IParser parser = ctx.newJsonParser();
        InputStream in = BaseTest.class.getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        return (BaseResource) parser.parseResource(cls, br);

    }

    protected static Document parseXMLString(String xml) throws ParserConfigurationException, IOException,  org.xml.sax.SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    @Test
    public void basicTest() throws Exception {

    }
}

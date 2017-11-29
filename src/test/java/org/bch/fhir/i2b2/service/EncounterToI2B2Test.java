package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.parser.IParser;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by mauro on 29/11/17.
 */
public class EncounterToI2B2Test extends BaseTest {
    protected FhirContext ctx = FhirContext.forDstu2();

    private  EncounterToI2B2 parseFile(String fileName) throws Exception {
        IParser parser = ctx.newJsonParser();
        InputStream in = DiagnosticReportToI2B2Test.class.getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        return (EncounterToI2B2) parser.parseResource((Class)EncounterToI2B2.class, br);

    }

    @Test
    public void basicTest() throws Exception {
        Encounter enc = (Encounter) parseFile(Encounter.class, "Encounter.json");
        EncounterToI2B2 encounterToI2B2 = new EncounterToI2B2();
        String xml = encounterToI2B2.getPDOXML(enc);

        System.out.println(xml);
        Document doc = parseXMLString(xml);
        XPath xpath = XPathFactory.newInstance().newXPath();


        NodeList eventList = (NodeList) xpath.evaluate("/patient_data/event_set/event", doc, XPathConstants.NODESET);
        assertEquals(eventList.getLength(), 1);
        Node eventId = (Node) xpath.evaluate(
                String.format(
                        "/patient_data/event_set/event/event_id[@source='%s'][text()='%s']",
                        enc.getServiceProvider().getReference().getIdPart(),
                        enc.getId().getIdPart()
                ),
                doc,
                XPathConstants.NODE
        );
        assertNotNull(eventId);

        Node patientId = (Node) xpath.evaluate(
                String.format("/patient_data/event_set/event/patient_id[text()='%s']", enc.getPatient().getReference().getIdPart()),
                doc,
                XPathConstants.NODE
        );
        assertNotNull(patientId);

        Node startDate = (Node) xpath.evaluate(
                String.format("/patient_data/event_set/event/start_date[text()='%s']", "1850-06-11T00:00:00.00"),
                doc,
                XPathConstants.NODE
        );
        assertNotNull(startDate);

        Node endDate = (Node) xpath.evaluate(
                String.format("/patient_data/event_set/event/end_date[text()='%s']", "1850-06-19T00:00:00.00"),
                doc,
                XPathConstants.NODE
        );
        assertNotNull(endDate);


    }
}

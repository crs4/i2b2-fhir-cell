package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import org.bch.fhir.i2b2.config.AppConfig;
import org.bch.fhir.i2b2.exception.FHIRI2B2Exception;
import org.bch.fhir.i2b2.pdomodel.Element;
import org.bch.fhir.i2b2.pdomodel.ElementSet;
import org.bch.fhir.i2b2.pdomodel.PDOModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Abstract class that provides common methods for mapping fhir into i2b2
 * @author CHIP-IHL
 */
public abstract class FHIRToPDO {

    /**
     * Method to the implemented that returns the pdo xml equivalent of the fhir resource
     * @param resource  The fhir resource
     * @return The pdo xml as String
     * @throws FHIRI2B2Exception In case some error occurs
     */
    public abstract String getPDOXML(BaseResource resource) throws FHIRI2B2Exception;

    protected abstract PDOModel getPDO(BaseResource resource) throws FHIRI2B2Exception;


    public static final String METADATA_CONCEPT_CD = "META";

    public static final String FHIR_TAG_VALUE_QUANTITY = "valueQuantity";
    public static final String FHIR_TAG_VALUE_STRING = "valueString";
    public static final String FHIR_TAG_VALUE_INTEGER = "valueInteger";
    public static final String FHIR_TAG_VALUE_DECIMAL = "valueDecimal";
    public static final String FHIR_TAG_VALUE_CODING = "valueCoding";
    public static final String FHIR_TAG_VALUE_BOOLEAN = "valueBoolean";

    public static final String DEFAULT_PATIENT_SOURCE = "HIVE";
    public static final String DEFAULT_EVENT_SOURCE = "HIVE";

    protected String patientIdeSource=DEFAULT_PATIENT_SOURCE;
    protected String patientIde=null;
    protected String eventIdeSource=DEFAULT_EVENT_SOURCE;
    protected String eventIde=null;

    protected String getEventId(Encounter enc) throws FHIRI2B2Exception {
        String eventId = null;
        if (enc == null || enc.getId().isEmpty()) {
            eventId = "" + new Date().getTime() / 1000;

        } else {

            eventId = enc.getId().getIdPart();
        }
        return eventId;
    }

    protected ElementSet generateEventSet(Encounter enc) throws FHIRI2B2Exception {
        ElementSet eventSet = new ElementSet();
        eventSet.setTypePDOSet(ElementSet.PDO_EVENT_SET);
        Element event = new Element();
        event.setTypePDO(Element.PDO_EVENT);

        String pdoEventId = generateRow(PDOModel.PDO_EVENT_ID, this.eventIde,genParamStr(PDOModel.PDO_SOURCE, this.eventIdeSource));
        String pdoPatientId = generateRow(PDOModel.PDO_PATIENT_ID, this.patientIde,
                genParamStr("source", this.patientIdeSource));
        event.addRow(pdoEventId);
        event.addRow(pdoPatientId);

        Date startDate = null;
        Date endDate = null;

        if (enc!= null) {
            if (!enc.getPeriod().isEmpty()) {
                PeriodDt period = enc.getPeriod();
                startDate = period.getStart();
                endDate = period.getEnd();
            }
        }

        if (startDate!=null) {
            String outputDataFormat = AppConfig.getProp(AppConfig.FORMAT_DATE_I2B2);
            SimpleDateFormat dateFormatOutput = new SimpleDateFormat(outputDataFormat);
            String startDateStr = dateFormatOutput.format(startDate);

            String pdoStartDate = generateRow(PDOModel.PDO_START_DATE, startDateStr);
            event.addRow(pdoStartDate);
        }
        if (endDate!=null) {
            String outputDataFormat = AppConfig.getProp(AppConfig.FORMAT_DATE_I2B2);
            SimpleDateFormat dateFormatOutput = new SimpleDateFormat(outputDataFormat);
            String endDateStr = dateFormatOutput.format(endDate);

            String pdoEndDate = generateRow(PDOModel.PDO_END_DATE, endDateStr);
            event.addRow(pdoEndDate);
        }

        eventSet.addElement(event);
        return eventSet;
    }

    protected ElementSet generateEIDSet() throws FHIRI2B2Exception {
        ElementSet eidSet = new ElementSet();
        eidSet.setTypePDOSet(ElementSet.PDO_EID_SET);
        Element eid = new Element();
        eid.setTypePDO(Element.PDO_EID);

        //<event_id patient_id="1234" patient_id_source="BCH" source="SCR">1423742400000</event_id>
        String pdoEventId = this.generateRow(PDOModel.PDO_EVENT_ID, this.eventIde,
                genParamStr(PDOModel.PDO_PATIENT_ID, this.patientIde),
                genParamStr(PDOModel.PDO_PATIENT_ID_SOURCE, this.patientIdeSource),
                genParamStr(PDOModel.PDO_SOURCE, this.eventIdeSource));

        eid.addRow(pdoEventId);
        eidSet.addElement(eid);
        return eidSet;
    }

    protected ElementSet generatePIDSet() throws FHIRI2B2Exception {
        ElementSet pidSet = new ElementSet();
        pidSet.setTypePDOSet(ElementSet.PDO_PID_SET);
        Element pid = new Element();
        pid.setTypePDO(Element.PDO_PID);

        //<patient_id source="BCH">1234</patient_id>
        String pdoPatientId = this.generateRow(PDOModel.PDO_PATIENT_ID, this.patientIde,
                genParamStr(PDOModel.PDO_SOURCE, this.patientIdeSource));

        pid.addRow(pdoPatientId);
        pidSet.addElement(pid);
        return pidSet;
    }

    protected ElementSet generatePatientSet() throws FHIRI2B2Exception {
        ElementSet patientSet = new ElementSet();
        patientSet.setTypePDOSet(ElementSet.PDO_PATIENT_SET);
        Element patient = new Element();
        patient.setTypePDO(Element.PDO_PATIENT);

        String pdoPatientId = this.generateRow(PDOModel.PDO_PATIENT_ID, this.patientIde,
                genParamStr(PDOModel.PDO_SOURCE, this.patientIdeSource));
        patient.addRow(pdoPatientId);
        patientSet.addElement(patient);
        return patientSet;
    }

    protected String finishRow(StringBuffer in, String tag, String value) {
        in.append(value);
        in.append("</").append(tag).append(">");
        return in.toString();
    }

    protected String generateRow(String tag, String value) {
        StringBuffer out = new StringBuffer();
        out.append("<").append(tag).append(">");
        return finishRow(out,tag, value);
    }

    protected String generateRow(String tag, String value, String param) {
        StringBuffer out = new StringBuffer();
        out.append("<").append(tag).append(" ").append(param).append(">");
        return finishRow(out,tag, value);
    }

    protected String generateRow(String tag, String value, String param1, String param2) {
        StringBuffer out = new StringBuffer();
        out.append("<").append(tag).append(" ").append(param1).append(" ").append(param2).append(">");
        return finishRow(out,tag, value);
    }

    protected String generateRow(String tag, String value, String param1, String param2, String param3) {
        StringBuffer out = new StringBuffer();
        out.append("<").append(tag).append(" ").append(param1).append(" ").
                append(param2).append(" ").append(param3).append(">");
        return finishRow(out,tag, value);
    }

    protected String genParamStr(String paramName, String valueStr) {
        return paramName + "=\"" + valueStr + "\"";
    }

    protected String genParamNum(String paramName, String valueNum) {
        return paramName + "=" + valueNum;
    }

    protected boolean isRawConceptCD(String type) {
        if (type.equals(FHIR_TAG_VALUE_CODING)) return false;
        if (type.equals(FHIR_TAG_VALUE_BOOLEAN)) return false;
        return true;
    }

    protected boolean isNumericType(String type) {
        if (type == null) return false;
        if (type.equals(FHIR_TAG_VALUE_QUANTITY)) return true;
        if (type.equals(FHIR_TAG_VALUE_INTEGER)) return true;
        if (type.equals(FHIR_TAG_VALUE_DECIMAL)) return true;
        return false;
    }

    protected IResource findResourceById(List<IResource> resources, String id){
        int i=0;
        IResource out = null;
        while (i<resources.size() && out==null) {
            IResource res = resources.get(i);
            if (res.getId().getIdPart().equals(id)) {
                out = res;
            }
            i++;
        }
        return out;
    }

    /**
     * Adds a metadata element associated to the current fhir resource. It does it by adding a new observation in an
     * ElementSet of type ObservationSet
     * @param info The metadata that will be stored in TVAL_CHAR
     * @param conceptCdMetadada The desired ConceptCd
     * @param observationSet The observation set
     */
    protected void addMetadataInObservationSet(String info, String conceptCdMetadada, ElementSet observationSet)
            throws FHIRI2B2Exception {

        Element out = new Element();
        out.setTypePDO(Element.PDO_OBSERVATION);

        String pdoEventId = this.generateRow(PDOModel.PDO_EVENT_ID, this.eventIde,
                this.genParamStr(PDOModel.PDO_SOURCE, this.eventIdeSource));
        out.addRow(pdoEventId);

        String pdoPatientId = this.generateRow(PDOModel.PDO_PATIENT_ID, this.patientIde,
                this.genParamStr(PDOModel.PDO_SOURCE, this.patientIdeSource));
        out.addRow(pdoPatientId);

        String outputDataFormat = AppConfig.getProp(AppConfig.FORMAT_DATE_I2B2);
        SimpleDateFormat dateFormatOutput = new SimpleDateFormat(outputDataFormat);
        String pdoStartDate = this.generateRow(PDOModel.PDO_START_DATE, dateFormatOutput.format(new Date()));
        out.addRow(pdoStartDate);

        String pdoObserverCd = generateRow(PDOModel.PDO_OBSERVER_CD, "@");
        out.addRow(pdoObserverCd);

        String pdoInstanceNum = generateRow(PDOModel.PDO_INSTANCE_NUM, ""+1);
        out.addRow(pdoInstanceNum);

        String pdoModifierCd = generateRow(PDOModel.PDO_MODIFIER_CD, "@");
        out.addRow(pdoModifierCd);

        String pdoValueTypeCd = generateRow(PDOModel.PDO_VALUETYPE_CD, "T");
        out.addRow(pdoValueTypeCd);

        String pdoTValChar = generateRow(PDOModel.PDO_TVAL_CHAR, info);
        out.addRow(pdoTValChar);

        String pdoConceptCd = generateRow(PDOModel.PDO_CONCEPT_CD, conceptCdMetadada);
        out.addRow(pdoConceptCd);

        observationSet.addElement(out);
    }

    protected void addDate(Element element, DateTimeDt datetime) throws FHIRI2B2Exception {
        if (datetime != null) {
            String outputDataFormat = AppConfig.getProp(AppConfig.FORMAT_DATE_I2B2);
            SimpleDateFormat dateFormatOutput = new SimpleDateFormat(outputDataFormat);
            String startDateStr = dateFormatOutput.format( datetime.getValue());
            element.addRow(this.generateRow(PDOModel.PDO_START_DATE, startDateStr));
            element.addRow(this.generateRow(PDOModel.PDO_END_DATE, startDateStr));
        }
    }

    protected Patient getPatient(IResource resource){
        for (IResource containedRes: resource.getContained().getContainedResources()) {
            if (containedRes instanceof Patient) {
                return (Patient) containedRes;
            }
        }
        return null;
    }

}

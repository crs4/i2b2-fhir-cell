package org.bch.fhir.i2b2.service;

import ca.uhn.fhir.model.api.BasePrimitive;
import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.base.resource.ResourceMetadataMap;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.composite.BoundCodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.MaritalStatusCodesEnum;
import ca.uhn.fhir.model.primitive.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bch.fhir.i2b2.config.AppConfig;
import org.bch.fhir.i2b2.exception.FHIRI2B2Exception;
import org.bch.fhir.i2b2.pdomodel.Element;
import org.bch.fhir.i2b2.pdomodel.ElementSet;
import org.bch.fhir.i2b2.pdomodel.PDOModel;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Generates the i2b2 pdo xml equivalent for fhir Patient resource
 * @author CHIP-IHL
 */
public class PatientToI2B2 extends FHIRToPDO {
    Log log = LogFactory.getLog(PatientToI2B2.class);
    private ResourceReferenceDt org;

    /**
     * Override method that implements {@link FHIRToPDO#getPDOXML(BaseResource)}
     * @param resource  The fhir resource. It must be a Patient resource
     * @return  The pdo xml as String
     * @throws FHIRI2B2Exception In case some error occurs
     */
    @Override
    public String getPDOXML(BaseResource resource) throws FHIRI2B2Exception {
        return getPDO(resource).generatePDOXML();
    }
    @Override
    protected PDOModel getPDO(BaseResource resource) throws FHIRI2B2Exception {

        PDOModel pdo = new PDOModel();
        Patient patient = (Patient) resource;

        if (patient!=null) {
            this.patientIde = this.getPatiendIde(patient);
            if (this.patientIde == null) {
                //FIXME must return something different from 500 http code
                throw new FHIRI2B2Exception("Patient id cannot be empty");
            }
            org = patient.getManagingOrganization();
            if ( org != null || !org.isEmpty()) {
                this.patientIdeSource = org.getReference().getIdPart();
            }
            else {
                this.patientIdeSource = "@";
            }
            ElementSet patientSet = this.generatePatientSet(patient);
            pdo.addElementSet(patientSet);
            // If patientSet is null, nothing to update
            if (patientSet == null) return null;
            ElementSet pidSet = this.generatePIDSet();
            pdo.addElementSet(pidSet);
        }

        return pdo;
    }

    protected void addColumnParam(Element patientElement, String columnName, String type, Object value) {
        if (value == null) return;

        if (type.equals("date")) {
            value = getTimeStampStringFromDate((java.util.Date) value);
        }

        if (!(value instanceof String)) {
            value = ((BasePrimitive) value).getValue();
        }

        patientElement.addRow(
                this.generateRow(PDOModel.PDO_PARAM, (String) value,
                    genParamStr(PDOModel.PDO_COLUMN, columnName),
                    genParamStr(PDOModel.PDO_TYPE, type)
                )
        );

    }

    // return null if bot zip code and state are not present
    protected ElementSet generatePatientSet(Patient patient) throws FHIRI2B2Exception {
        //<param column="sex_cd">F__PatientSegments.patientGender__F</param>
        ElementSet patientSet = new ElementSet();
        patientSet.setTypePDOSet(ElementSet.PDO_PATIENT_SET);
        Element patientElement = new Element();
        patientElement.setTypePDO(Element.PDO_PATIENT);
        String pdoPatientId = this.generateRow(PDOModel.PDO_PATIENT_ID, this.patientIde,
                genParamStr(PDOModel.PDO_SOURCE, this.patientIdeSource));
        patientElement.addRow(pdoPatientId);

        Map<String, String> addressInfo = this.getAddressInfo(patient);

        String zip = addressInfo.get("zip");
        String state = addressInfo.get("state");
        String city = addressInfo.get("city");

        if (zip != null) {
            addColumnParam(patientElement, PDOModel.PDO_COLUMN_ZIP_CD, "string", zip);

            if (state != null && city != null) {
                addColumnParam(patientElement, PDOModel.PDO_COLUMN_STATE_PATH,
                        "string", String.format("Zip codes\\%s\\%s\\%s", state, city, zip));
                }
        }

        String vitalStatusCDDeath = "";
        String vitalStatusCDBirth = "";

        Date birthDate = patient.getBirthDate();
        if (birthDate != null) {
            addColumnParam(patientElement, PDOModel.PDO_BIRTH_DATE, "date", patient.getBirthDate());
            vitalStatusCDBirth = getPrecisionCode(new DateTimeDt(birthDate), "birth");
        }

        IDatatype deceased = patient.getDeceased();
        System.out.println("deceased " + deceased);
        if (deceased != null) {
            if (deceased instanceof BooleanDt) {
                vitalStatusCDDeath = ((BooleanDt) deceased).getValue() ? "Z": "N";
            }
            else {
                vitalStatusCDDeath = getPrecisionCode((DateTimeDt) deceased, "death");
                addColumnParam(patientElement, PDOModel.PDO_DEATH_DATE, "date", (((DateTimeDt) deceased).getValue()));
            }
        }

        addColumnParam(patientElement, PDOModel.PDO_VITAL_STATUS_CD, "string", vitalStatusCDDeath + vitalStatusCDBirth);

        String gender = patient.getGender();
        if (gender != null) {
            addColumnParam(patientElement, PDOModel.PDO_GENDER, "string", ("" + patient.getGender().charAt(0)).toUpperCase());
        }

        List<Patient.Communication> communication = patient.getCommunication();
        if (communication!= null && !communication.isEmpty())
            addColumnParam(patientElement, PDOModel.PDO_LANGUAGE, "string", communication.get(0).getLanguage().getText());

        if (org != null && !org.isEmpty())
            addColumnParam(patientElement, PDOModel.PDO_SOURCESYSTEM_CD, "string", org.getReference().getIdPart());

        BoundCodeableConceptDt<MaritalStatusCodesEnum> maritalStatus = patient.getMaritalStatus();
        if (!maritalStatus.isEmpty()) {
            addColumnParam(patientElement, PDOModel.PDO_MARITAL_STATUS_CD, "string", maritalStatus.getText());
        }


//        boolean deceasedBoolean = patient.getDeceased();

        ResourceMetadataMap meta = patient.getResourceMetadata();

        if (!meta.isEmpty()) {
            InstantDt updateDate = (InstantDt) meta.get(ResourceMetadataKeyEnum.UPDATED);
            if (updateDate != null) {

                String updateDateElement = this.generateRow(
                        PDOModel.PDO_PARAM,
                        getTimeStampStringFromDate(updateDate.getValue()),
                        genParamStr(PDOModel.PDO_COLUMN, PDOModel.PDO_UPDATE_DATE),
                        genParamStr(PDOModel.PDO_TYPE, "date"));
                patientElement.addRow(updateDateElement);

            }

        }


        // Provisional until CRCLoader works properly with patients that already exists
//        boolean b=false;
//        try {
//            b = updateI2B2DB(zip, state, this.patientIde, this.patientIdeSource);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        // No need to bother i2b2, since the upload as been done directly into i2b2
//        if (b) return null;

        patientSet.addElement(patientElement);
        return patientSet;
    }


    private String getPrecisionCode (DateTimeDt dateTimeDt, String type) {
        String result = "";
        TemporalPrecisionEnum precisionEnum = dateTimeDt.getPrecision();
        switch (precisionEnum) {
            case DAY:
                result = type.equals("death") ? "Y" : "D";
                break;
            case MONTH:
                result = type.equals("death") ? "M" : "B";
                break;
            case YEAR:
                result = type.equals("death") ? "X" : "F";
                break;
            case SECOND:
                result= type.equals("death") ? "S" : "C";
                break;
            case MILLI:
                result = type.equals("death") ? "S" : "C";
                break;

        }
        return result;
    }

    private String getTimeStampStringFromDate(java.util.Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MILLISECOND, 0);
        return (new java.sql.Timestamp(date.getTime()).toString());

    }

    // PRE: patient is not null
    private Map<String, String> getAddressInfo(Patient patient) {
        List<AddressDt> addrs = patient.getAddress();
        Map res = new HashMap<String, String>();
        if (addrs.isEmpty()) {
            log.warn("No address information is provided for the Patient resource");
            return res;
        }
        AddressDt addressDt = addrs.get(0);
        StringDt zip = addressDt.getPostalCodeElement();
        StringDt city = addressDt.getCityElement();

        if (!zip.isEmpty())
            res.put("zip", zip.isEmpty()? null: zip.getValue());
        StringDt state = addressDt.getStateElement();
        res.put("state", state.isEmpty()? null: state.getValue());
        res.put("city", city.isEmpty()? null: city.getValue());

        return res;
    }

    // PRE: patient is not null
    private String getPatiendIde(Patient patient) {
        return patient.getId().isEmpty()? null: patient.getId().getIdPart();
    }

    /**
     * TODO: Refactor when I2B2 CRCLoader works properly with patients that already exists
     * This is a provisional method. Currently, the CRCLoader does not work when uploading patient information if the
     * patient is already there. So, we do it directly on DB.
     * @param zip
     * @param state
     * @param subjectId
     * @return
     * @throws Exception
     */

    // Return true if updated was done
    private boolean updateI2B2DB(String zip, String state, String subjectId, String source) throws Exception {
        Class.forName("oracle.jdbc.driver.OracleDriver");
        //String jdbcCon = AppConfig.getProp(AppConfig.I2B2_JDBC);
        //String auth = AppConfig.getAuthCredentials(AppConfig.CREDENTIALS_FILE_DB_I2B2);
        //String[] auths = auth.split(":");

        Connection con = null;
        Statement stmt = null;
        boolean b = true;
        try {
            //con = DriverManager.getConnection(jdbcCon, auths[0], auths[1]);
            InitialContext context = new InitialContext();
            DataSource dataSource = (DataSource) context.lookup(AppConfig.getProp(AppConfig.I2B2_DATASOURCE));
            con = dataSource.getConnection();
            String numPatientSql = "select patient_num from patient_mapping where patient_ide = '" + subjectId +
                    "' and patient_ide_source='" + source + "'";

            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(numPatientSql);
            // It must be one entry
            if (!rs.next()) {
                b=false;
                rs.close();
            } else {
                String patientNum = rs.getString("patient_num");
                rs.close();

                String updateSql = "update patient_dimension set " + PDOModel.PDO_COLUMN_ZIP_CD + "='" + zip + "', " +
                        PDOModel.PDO_COLUMN_STATE_PATH + "='" + state + "' where patient_num=" + patientNum;
                stmt.executeUpdate(updateSql);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try{
                if(stmt!=null) {
                    stmt.close();
                }
            } catch(SQLException se){
                se.printStackTrace();
            }// do nothing
            try{
                if(con!=null) {
                    con.close();
                }
            }catch(SQLException se){
                se.printStackTrace();
            }
        }
        return b;
    }
}

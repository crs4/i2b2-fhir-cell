package it.crs4.scripts;

import org.apache.commons.io.IOUtils;
import org.bch.fhir.i2b2.exception.FHIRI2B2Exception;
import org.bch.fhir.i2b2.external.I2B2CellFR;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PublishData {
    public static void main(String[] args) throws IOException {
        String xmlpdo;
        System.out.println("********reading file " + args[0]);
        FileInputStream inputStream = new FileInputStream(args[0]);
        try {
            xmlpdo = IOUtils.toString(inputStream);
        } finally {
            inputStream.close();
        }

        I2B2CellFR i2b2 = new I2B2CellFR();
        I2B2CellFR.UploadI2B2Response response = null;
        try {
            response = i2b2.pushPDOXML(xmlpdo);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        int nObservations = response.getTotalRecords(I2B2CellFR.XmlPdoTag.TAG_OBSERVATIONS);
        System.out.println("total records inserted: " + nObservations);

    }
}

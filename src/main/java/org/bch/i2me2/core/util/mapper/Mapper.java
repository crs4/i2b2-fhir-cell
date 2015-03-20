package org.bch.i2me2.core.util.mapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bch.i2me2.core.exception.I2ME2Exception;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that implements a generic mapping between JSON-like and XML PDO
 * JSON structure is {a1:object, a2:object .... b:array[ {c1:object, c2:object ...}]}
 * @author CH176656
 *
 */
public abstract class Mapper {
	
	// Name of the xml file that acts as template
	//public static String XML_MAP_FILE_NAME="xmlpdoTemplate.xml";

	private String xmlMapFileTemplate="xmlpdoTemplate.xml";
	
	// delimiter between fields
	private String delPre = "F__";
    private String delPost = "__F";

    // The header
    private String tagRepositoryValue="";

    // The order in which the xml tags will appear in the xml pdo
    private final List <XmlPdoTag> orderXmlTags = new ArrayList<>();

	// Tab: 4 blank spaces
	private static final String TAB = "    ";
	/**
	 * The enum class for the xml pdo set tags 
	 * @author CH176656
	 *
	 */
	public static enum XmlPdoTag {
		TAG_OBSERVATIONS ("observation_set", "observation"),
		TAG_EVENTS ("event_set", "event"),
		TAG_CONCEPTS ("concept_set", "concept"),
		TAG_EIDS ("eid_set", "eid"),
		TAG_PIDS ("pid_set", "pid"),
		TAG_MODIFIERS ("modifier_set", "modifier"),
		TAG_PATIENTS ("patient_set", "patient"),
        TAG_REPOSITORY ("repository:patient_data", "");
		
		private final String tagValue;
        private final String tagValueIn;
		XmlPdoTag(String tagValue, String tagValueIn) {
            this.tagValueIn = tagValueIn;
            this.tagValue = tagValue;
		}
		public String toString() {
            return this.tagValue;
		}
        public String getTagValueIn() {
            return this.tagValueIn;
        }
	}

    public static enum XmlPdoObservationTag {
        TAG_TVAL ("tval_char"),
        TAG_NVAL ("nval_num"),
        TAG_CONCEPT_CD ("concept_cd"),
        TAG_MODIFIER_CD ("modifier_cd"),
        TAG_START_DATE ("start_date");

        private final String tagValue;
        XmlPdoObservationTag(String tagValue) {
            this.tagValue = tagValue;
        }

        public String toString() {
            return this.tagValue;
        }
    }

    public static enum XmlPdoPatientTag {
        TAG_PARAM_SEX_CD ("param", "sex_cd"),
        TAG_PARAM_DOB ("param", "birth_date"),
        TAG_PARAM_ZIP ("param", "zip_cd");

        private final String tagValue;
        private final String column;
        XmlPdoPatientTag(String tagValue, String column) {
            this.tagValue = tagValue;
            this.column = column;
        }
        public String getTagValue() {
            return this.tagValue;
        }
        public String toString() {
            return this.tagValue + " column=\"" + this.column + "\"";
        }
    }

    /*
	// Keys of RXConnect JSON
	public static String RX_PATIENTSEGMENT= "PatientSegments";
	public static String RX_HISTORYSEGMENT = "RxHistorySegments";
	public static String RX_ORDERS = "orders";
	public static String RX_RXD = "rxd";
	public static String RX_ORC = "orc";
	*/
	
	private final Map< XmlPdoTag, String> mapTemplate = new HashMap<>();
	private final Map<XmlPdoTag, String> mapResult = new HashMap<>();

    // Will contains the key for which the abstract method 'format' will be called
    private List<String> keysToFormat = new ArrayList<>();

	protected Mapper() {
		initialize();
	}

    protected void addKeyToFormat(String key) {
        this.keysToFormat.add(key);
    }

    /**
     * Perform the map
     * @param jsonInput			The JSON-formatted string
     * @param extraJsonKey      Extra json key
     * @param extraJsonInput    Extra json object that will be included in the root of the element
     * @return					The entire XML PDO as String
     * @throws IOException		If there is a problem reading the xml template
     * @throws JSONException	If there is a problem parsing the json
     * @throws I2ME2Exception   If XML PDO template is malformed
     */
    protected String doMap(String jsonInput, String extraJsonKey, String extraJsonInput)
            throws IOException, JSONException, I2ME2Exception {
        initialize();
        String xmlTemplate = loadXMLTemplate();

        if (jsonInput==null) {
            throw new JSONException("Input cannot be null");
        }
        JSONObject jsonRoot = new JSONObject(jsonInput);
        if (extraJsonKey!=null && extraJsonInput!=null) {
            JSONObject jsonExtra = new JSONObject(extraJsonInput);
            jsonRoot.put(extraJsonKey, jsonExtra);
        }
        List<String> keys = findJSONKeys(xmlTemplate);
        Map<String, String> jsonDataMap = buildDataMap(jsonRoot, keys);
        //List<JSONObject> jsonObjects = getJSONObjects(jsonRoot);
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = getJSONArray(jsonRoot);
        } catch (JSONException e) {
            // it's ok. It means orders are not present
        }

        Map<String,String> emptyMap = new HashMap<>();
        performElementMap(jsonDataMap, emptyMap, XmlPdoTag.TAG_PATIENTS);
        performElementMap(jsonDataMap, emptyMap, XmlPdoTag.TAG_EVENTS);
        performElementMap(jsonDataMap, emptyMap, XmlPdoTag.TAG_PIDS);
        performElementMap(jsonDataMap, emptyMap, XmlPdoTag.TAG_EIDS);
        performElementMap(jsonDataMap, emptyMap, XmlPdoTag.TAG_OBSERVATIONS);
        for (int i = 0; i < jsonArray.length(); i++) {
            Map<String, String> jsonDataMapInArray = buildDataMap(jsonArray.getJSONObject(i), keys);
            performElementMap(jsonDataMap, jsonDataMapInArray, XmlPdoTag.TAG_OBSERVATIONS, true);
        }
        return buildXMLOutput();
    }

    /**
     * Returns the entire sub xml element of a given xmlElement String
     * @param tag the tag to find
     * @return The xml element. empty string if the tag is not found or any other problem occurs
     */
    protected String getTagValueLine(String xmlElement, String tag) {
        return getTagValueLine(xmlElement, tag, tag);
    }

    protected String getTagValueLine(String xmlElement, String tag, String tail) {
        String tagInit = "<"+ tag;
        String tagEnd = "</"+tail+">";
        int init = xmlElement.indexOf(tagInit);
        if (init<0) return "";
        int end = xmlElement.indexOf(tagEnd, init);
        if (end <0) return "";
        return xmlElement.substring(init,end+tagEnd.length());
    }

    private void initialize() {
        this.mapResult.clear();
        this.mapTemplate.clear();
        this.orderXmlTags.clear();

        this.mapResult.put(XmlPdoTag.TAG_PATIENTS, null);
        this.mapResult.put(XmlPdoTag.TAG_EIDS, null);
        this.mapResult.put(XmlPdoTag.TAG_PIDS, null);
        this.mapResult.put(XmlPdoTag.TAG_EVENTS, null);
        this.mapResult.put(XmlPdoTag.TAG_OBSERVATIONS, null);

        this.mapTemplate.put(XmlPdoTag.TAG_PATIENTS, null);
        this.mapTemplate.put(XmlPdoTag.TAG_EIDS, null);
        this.mapTemplate.put(XmlPdoTag.TAG_PIDS, null);
        this.mapTemplate.put(XmlPdoTag.TAG_EVENTS, null);
        this.mapTemplate.put(XmlPdoTag.TAG_OBSERVATIONS, null);

        this.orderXmlTags.add(XmlPdoTag.TAG_PIDS);
        this.orderXmlTags.add(XmlPdoTag.TAG_EIDS);
        this.orderXmlTags.add(XmlPdoTag.TAG_EVENTS);
        this.orderXmlTags.add(XmlPdoTag.TAG_PATIENTS);
        this.orderXmlTags.add(XmlPdoTag.TAG_OBSERVATIONS);

        this.tagRepositoryValue="";
    }

    private Map<String, String> buildDataMap(JSONObject jsonRoot, List<String> keys) throws JSONException {
        Map<String, String> dataMap = new HashMap<>();
        for(String key:keys) {
            try {
                String value = getValueFromJSONObject(jsonRoot, key);
                dataMap.put(key,value);
            } catch (Exception e) {
                // It's ok
            }
        }
        return dataMap;
    }

    private String getValueFromJSONObject(JSONObject jsonRoot, String key) throws JSONException {
        String [] subKeys = key.split("\\.");
        JSONObject aux = jsonRoot;

        // We follow the entire path assuming they are objects
        for(int i=0; i<subKeys.length-1;i++) {
            aux = aux.getJSONObject(subKeys[i]);
        }
        // Here we are at the root element, so, we can access the element directly
        String value = null;
        try {
            // We try if it's a String
            value = aux.getString(subKeys[subKeys.length-1]);
            return value;
        } catch (JSONException e) {
            // We try if it's a numerical value
            try {
                Long longValue = aux.getLong(subKeys[subKeys.length - 1]);
                value = longValue.toString();
            } catch (Exception ee) {
                // Don't do anything. We assume the value is not there
            }
        }
        return value;
    }

    private List<String> findJSONKeys(String xmlTemplate) {
        // Build the pattern
        Pattern pattern = Pattern.compile(this.delPre + "(.*?)" + this.delPost);
        Matcher m = pattern.matcher(xmlTemplate);
        List<String> keys = new ArrayList<>();
        while (m.find()) {
            String key = m.group();
            String realKey = key.substring(this.delPre.length(), key.length()-this.delPost.length());
            if (!keys.contains(realKey)) {
                keys.add(realKey);
            }
        }
        return keys;
    }

    /**
     * Returns the jsonArray
     * It will be called only once
     * @return The JSONArray instance
     */
    protected abstract JSONArray getJSONArray(JSONObject root) throws JSONException;

    /**
     * Replace internal modifiers codes with the real ones. It is called only once after the mapping is done
     * @param text The entire xml pdo with the final results containing the internal modifier codes
     * @return The final mapping with the real modifier codes.
     */
    public String placeRealModifiersCodes(String text) {
        return text;
    }
    /**
     * Provides a way to format the actual value that will be place in the final XML
     * It will be called every time a value from a key included in keysToFormat list is found.
     * It must be override if special formatting is needed for some keys.
     *
     * @param key               The key as it is in the xmlTemplate
     * @param value             The actual value found in the JSON
     * @param dataMap           The current mapping of the root object
     * @param dataMapInArray    The current mapping of the element in the array
     * @return                  The formatted value
     */
    protected String format(String key, String value, Map<String, String> dataMap, Map<String, String> dataMapInArray) {
        return value;
    }

	private String buildXMLOutput() {
		StringBuilder out = new StringBuilder();
        // We append the header
        out.append(this.tagRepositoryValue);
		for(XmlPdoTag keyTag:this.orderXmlTags) {
            String key = keyTag.toString();
            String tagInit = "<" + key + ">";
            String tagEnd = "</" + key + ">";
            out.append(TAB).append(tagInit).append('\n');
            out.append(mapResult.get(keyTag)).append('\n');
            out.append(TAB).append(tagEnd).append('\n');
		}
        out.append("</").append(XmlPdoTag.TAG_REPOSITORY.toString()).append(">");
		return placeRealModifiersCodes(out.toString());
	}

    private void performElementMap(Map<String, String> jsonDataMap, Map<String, String> jsonDataMapInArray,
                                   XmlPdoTag tag) {
        performElementMap(jsonDataMap, jsonDataMapInArray, tag, false);
    }

    private void performElementMap(Map<String, String> jsonDataMap, Map<String, String> jsonDataMapInArray,
                                   XmlPdoTag tag, boolean doFilter) {
        String out = mapTemplate.get(tag);
        Set<String> keys = jsonDataMap.keySet();
        for(String key: keys) {
            String value = jsonDataMap.get(key);
            if (this.keysToFormat.contains(key)) {
                value = this.format(key, value, jsonDataMap, jsonDataMapInArray);
            }
            if (value!=null) {
                out = out.replaceAll(delPre + key + delPost, value);
            }
        }

        keys = jsonDataMapInArray.keySet();
        for(String key: keys) {
            String value = jsonDataMapInArray.get(key);
            if (this.keysToFormat.contains(key)) {
                value = this.format(key, value, jsonDataMap, jsonDataMapInArray);
            }
            if (value!=null) {
                out = out.replaceAll(delPre + key + delPost, value);
            }
        }

        out = out + '\n';
        out = filter(out, jsonDataMap, jsonDataMapInArray, tag, doFilter);
        String rem = mapResult.get(tag);
        if (rem!=null) {
            out = rem+out;
        }
        mapResult.put(tag, out);

    }

	private String loadXMLTemplate() throws IOException, I2ME2Exception{
        InputStream in = Mapper.class.getResourceAsStream(xmlMapFileTemplate);
        if (in==null) {
            throw new IOException("Template File not found:" + xmlMapFileTemplate);
        }
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuilder sBuffer = new StringBuilder();
		String line;
		try {
			while ((line = br.readLine()) != null) {
				sBuffer.append(line).append('\n');
			}
		} catch(Exception e) {
			e.printStackTrace();
			
		} finally {
			in.close();
		}
		String xmlTemplate = sBuffer.toString();
        this.tagRepositoryValue = getHeader(XmlPdoTag.TAG_REPOSITORY, xmlTemplate);
		Set<XmlPdoTag> keys = mapTemplate.keySet();
		for(XmlPdoTag keyTag:keys) {
            String key = keyTag.toString();
            String aux = getPart(key, xmlTemplate);
            mapTemplate.put(keyTag, aux);
		}
        return xmlTemplate;
	}

    private String getHeader(XmlPdoTag tag, String xmlTemplate) {
        String tagStr = "<" + tag.toString();
        int init = xmlTemplate.indexOf(tagStr);
        if (init<0) return "";
        int initEnd = xmlTemplate.indexOf(">", init);
        if (initEnd<0) return "";
        return xmlTemplate.substring(0, initEnd+1);
    }

	private String getPart(String xmlTag, String xmlString) throws I2ME2Exception {
		String tagInit ="<" + xmlTag +">";
		String tagEnd = "</" + xmlTag +">";
		int init = xmlString.indexOf(tagInit);
		int end = xmlString.indexOf(tagEnd);
        if (init>=0 && end <0) {
            // Malformed XM. Missing tagEnd
            throw new I2ME2Exception("Malformed XML template. Missing " + tagEnd);
        }

        if (init<0 && end >= 0) {
            // Malformed XM. Missing tagInit
            throw new I2ME2Exception("Malformed XML template. Missing " + tagInit);
        }
        if (init<0 || end <0) {
            // Tag does not exists, which is fine
            return "";
        }
		return xmlString.substring(init+tagInit.length(), end);
	}

    /**
     * Can be override if special filter is needed after the mapping is completed.
     * The function will be called for each xml individual element of the tag <observation><patiend><eid> etc...
     * @param xmlElem               The current xmlElement
     * @param jsonDataMap           The json Data Map
     * @param jsonDataMapInArray    The jsonDataMapIn Array
     * @param tag                   The XmlPdoTag
     * @return                      Return a transformation of the new element
     */
    protected String filterExtra(String xmlElem, Map<String, String> jsonDataMap,
                                 Map<String, String> jsonDataMapInArray, XmlPdoTag tag) {
        return xmlElem;
    }
    // Filter elements
    private String filter(String out, Map<String, String> jsonDataMap, Map<String, String> jsonDataMapInArray,
                          XmlPdoTag tag, boolean doFilter) {
        String newOut = "";
        String tagValueIn = tag.getTagValueIn();

        String tagInit ="<" + tagValueIn +">";
        String tagEnd = "</" + tagValueIn +">";
        int start = out.indexOf(tagInit);
        int end = out.indexOf(tagEnd);
        while (start > 0 && end > 0) {
            String elem = TAB+TAB+out.substring(start, end+tagEnd.length());
            String newElem = elem;
            // we eliminate observations that have not been updated
            if (tag.toString().equals(XmlPdoTag.TAG_OBSERVATIONS.toString())) {
                String tval = this.getTagValueLine(elem, XmlPdoObservationTag.TAG_TVAL.toString());
                String nval = this.getTagValueLine(elem, XmlPdoObservationTag.TAG_NVAL.toString());
                String startDate = this.getTagValueLine(elem, XmlPdoObservationTag.TAG_START_DATE.toString());

                // We eliminate any observation that has not been updated
                if (isNotSet(tval) || isNotSet(nval) || isNotSet(startDate)) {
                    newElem = "";
                } else if (doFilter) {
                    newElem = filterExtra(elem, jsonDataMap, jsonDataMapInArray, tag);
                }
            } else if (tag.toString().equals(XmlPdoTag.TAG_PATIENTS.toString())) {

                // We eliminate any param from patient tag that has not been updated because they are optional
                String sexcd = this.getTagValueLine(elem, XmlPdoPatientTag.TAG_PARAM_SEX_CD.toString(),
                        XmlPdoPatientTag.TAG_PARAM_DOB.getTagValue());
                String dob = this.getTagValueLine(elem, XmlPdoPatientTag.TAG_PARAM_DOB.toString(),
                        XmlPdoPatientTag.TAG_PARAM_DOB.getTagValue());
                String zip = this.getTagValueLine(elem, XmlPdoPatientTag.TAG_PARAM_ZIP.toString(),
                        XmlPdoPatientTag.TAG_PARAM_ZIP.getTagValue());
                if (isNotSet(sexcd)) {
                    newElem = elem.replaceAll(sexcd, "");
                }
                if (isNotSet(dob)) {
                    newElem = newElem.replaceAll(dob, "");
                }
                if (isNotSet(zip)) {
                    newElem = newElem.replaceAll(zip, "");
                }
            }
            // If the element already exists, we do not add it.
            if (!newOut.contains(newElem.trim())) {
                newOut = newOut + newElem;
                if (!newElem.trim().equals("")) {
                    newOut = newOut + "\n";
                }
            }
            start = out.indexOf(tagInit, end+1);
            end = out.indexOf(tagEnd, end+1);
        }

        return newOut;
    }

    /**
     * return true if the value has not been set, so, if there is still the template information
     * @param value The field to check
     * @return True if the value has not been set. i.e, if F__ and __F are the delimiters, returns true is found both
     * of them.
     */
    protected boolean isNotSet(String value) {
        return value!=null && (value.indexOf(this.getDelPre())>0 && value.indexOf(this.getDepPost())>0);
    }

    public void setXmlMapFileTemplate(String xmlMapFileTemplate) {
        this.xmlMapFileTemplate = xmlMapFileTemplate;
    }

    public String getDelPre(){
        return this.delPre;
    }

    public String getDepPost() {
        return this.delPost;
    }
}
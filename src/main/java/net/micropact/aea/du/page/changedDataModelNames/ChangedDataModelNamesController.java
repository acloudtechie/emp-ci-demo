package net.micropact.aea.du.page.changedDataModelNames;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.query.Coersion;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This serves as the Controller Code for a Page which displays Data Objects and Data Elements which appear to have had
 * their names changed after being initially created.
 *
 * @author zachary.miller
 */
public class ChangedDataModelNamesController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            response.put("changedObjects", JsonUtilities.encode(getChangedDataObjects(etk)));
            response.put("changedElements", JsonUtilities.encode(getChangedDataElements(etk)));

            return response;
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Retrieves a list of data elements which appear to have had their name changed after creation.
     *
     * @param etk entellitrak execution context
     * @return The list of Data Elements
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static List<Map<String, Object>> getChangedDataElements(final PageExecutionContext etk)
            throws IncorrectResultSizeDataAccessException {
        final List<Map<String, Object>> changedElements = new LinkedList<>();

        final List<Map<String, Object>> allElements = etk.createSQL("SELECT do.DATA_OBJECT_ID, do.NAME OBJECT_NAME, de.DATA_ELEMENT_ID, de.NAME ELE_NAME, de.ELEMENT_NAME FROM etk_data_object DO JOIN etk_data_element de ON de.data_object_id = do.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.system_field = 0 ORDER BY OBJECT_NAME, DATA_OBJECT_ID, ELE_NAME, ELEMENT_NAME, DATA_ELEMENT_ID")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                .fetchList();

        for(final Map<String, Object> element : allElements){
            final String eleName = (String) element.get("ELE_NAME");
            final String elementName = (String) element.get("ELEMENT_NAME");

            final boolean nameMatchesElementName = doesNameMatchElementName(eleName, elementName);

            if(!nameMatchesElementName){
                changedElements.add(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                    {"DATA_OBJECT_ID", Coersion.toLong(element.get("DATA_OBJECT_ID"))},
                    {"OBJECT_NAME", element.get("OBJECT_NAME")},
                    {"DATA_ELEMENT_ID", Coersion.toLong(element.get("DATA_ELEMENT_ID"))},
                    {"ELE_NAME", element.get("ELE_NAME")},
                    {"ELEMENT_NAME", element.get("ELEMENT_NAME")},
                    {"nameMatchesElementName", nameMatchesElementName},
                }));
            }
        }

        return changedElements;
    }

    /**
     * This method determines whether elementName is the internal name that entellitrak would have assigned to a
     * Data Element whose Name was eleName.
     *
     * @param eleName ETK_DATA_ELEMENT.NAME
     * @param elementName ETK_DATA_ELEMENT.element_name
     * @return whether the names appear to match
     */
    private static boolean doesNameMatchElementName(final String eleName, final String elementName) {
        return elementName.equals(getValidPropertyName(eleName));
    }

    /**
     * Retrieves a list of Data Objects which appear to have had their name changed after creation.
     *
     * @param etk entellitrak execution context
     * @return The list of Data Objects
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static List<Map<String, Object>> getChangedDataObjects(final PageExecutionContext etk)
            throws IncorrectResultSizeDataAccessException {
        final List<Map<String, Object>> changedObjects = new LinkedList<>();

        final List<Map<String, Object>> dataObjects = etk.createSQL("SELECT DATA_OBJECT_ID, NAME, LABEL, OBJECT_NAME FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId ORDER BY NAME, OBJECT_NAME, DATA_OBJECT_ID")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                .fetchList();

        for(final Map<String, Object> dataObject : dataObjects){
            final String name = (String) dataObject.get("NAME");
            final String label = (String) dataObject.get("LABEL");
            final String objectName = (String) dataObject.get("OBJECT_NAME");

            final boolean nameMatchesLabel = doesNameMatchesLabel(name, label);
            final boolean nameMatchesObjectName = doesNameMatchObjectName(name, objectName);

            if(!(nameMatchesLabel && nameMatchesObjectName)){
                changedObjects.add(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                    {"DATA_OBJECT_ID", Coersion.toLong(dataObject.get("DATA_OBJECT_ID"))},
                    {"NAME", name},
                    {"LABEL", label},
                    {"OBJECT_NAME", objectName},
                    {"nameMatchesLabel", nameMatchesLabel},
                    {"nameMatchesObjectName", nameMatchesObjectName}
                }));
            }
        }

        return changedObjects;
    }

    /**
     * This method determines whether objectName is the name that entellitrak would have created for a Data Object
     * which was created with the Name name.
     *
     * @param name ETK_DATA_OBJECT.NAME
     * @param objectName ETK_DATA_OBJECT.OBJECT_NAME
     * @return whether the names appear to match
     */
    private static boolean doesNameMatchObjectName(final String name, final String objectName) {
        return objectName.equals(getValidObjectName(name));
    }

    /**
     * This method determines whether label is what entellitrak would have provided as the default label for an object
     * with Name name.
     *
     * @param name ETK_DATA_OBJECT.NAME
     * @param label ETK_DATA_OBJECT.LABEL
     * @return whether the values match
     */
    private static boolean doesNameMatchesLabel(final String name, final String label) {
        return name.equals(label);
    }

    /**
     * <strong>
     *  This method is copied directly from
     *  {@link com.micropact.entellitrak.cfg.database.NamingUtil#getValidObjectName(String)}
     * </strong>
     *
     * Generates a valid object name based on the passed name.
     *
     * @param name an entelliTrak object name
     * @return a valid, camel case object name
     */
    private static String getValidObjectName(final String name) {
        if (name == null) {
            return null;
        }

        final StringBuffer rc = new StringBuffer();
        final String[] words = name.trim().split("\\W+");

        for (int i = 0; i < words.length; i++) {
            rc.append(words[i].substring(0, 1).toUpperCase())
                .append(words[i].substring(1).toLowerCase());
        }

        return rc.toString();
    }

    /**
     * <strong>
     *  This method is copied directly from
     *  {@link com.micropact.entellitrak.cfg.database.NamingUtil#getValidPropertyName(String)}
     * </strong>
     *
     * Generates a valid property name based on the passed name.
     * @param name an entelliTrak property name
     * @return a valid, camel case property name
     */
    private static String getValidPropertyName(final String name) {

        if (StringUtility.isBlank(name)) {
            return name;
        }

        /* This method throws error if property name has non-alhpanum
         * characters as a prefix
        // One solution: apply WordUtil capitalizeFully on name, then
        // remove all white spaces and return after uncapitalizing
        // the whole string (end result in camelCase)
         * Code:
        name = removeAllSpaces(WordUtils.capitalizeFully(name));
        return WordUtils.uncapitalize(name);
        */

        final StringBuffer rc = new StringBuffer();
        final String[] words = name.trim().split("\\W+");

        for (int i = 0; i < words.length; i++) {
            if (i == 0) {
                rc.append(words[i].substring(0, 1).toLowerCase());
            } else {
                rc.append(words[i].substring(0, 1).toUpperCase());
            }
            rc.append(words[i].substring(1).toLowerCase());
        }

        return rc.toString();

    }

}

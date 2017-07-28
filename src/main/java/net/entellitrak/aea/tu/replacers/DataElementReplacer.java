package net.entellitrak.aea.tu.replacers;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.entellitrak.ExecutionContext;
import com.entellitrak.localization.Localizations;

import net.entellitrak.aea.exception.TemplateException;
import net.entellitrak.aea.tu.IReplacer;
import net.entellitrak.aea.tu.ITemplater;
import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.Utility;

/**
 * <p>
 *  A DataElement replacer will replace a value based on a DataElement on a DataObject.
 *  The Syntax is DataObjectName_DataElementName.
 *  The DataObjectName is from etk_data_object.object_name and DataElementName
 *  is from etk_data_element.element_name.
 *  The replacementVariables Map must pass in the trackingId as DataObjectNameId
 *  (Note that the data object name is capitalized and Id is added at the end).
 *  For instance if you have a DataObject called Event with a DataElement called eventDate you can call
 *  the DataElementReplacer with Event_eventDate passing in EventId through the replacementVariables Map.
 *  Currently the DataElement replacer cannot handle values which come from Lookups.
 *  When core introduces lookup services this may or may not change depending on whether
 *  it can be efficiently implemented.
 * </p>
 * <p>
 *  TimeStamp fields will be localized to the server time because the user may not be available.
 *  If this choice becomes a problem we can introduce a new replacer for TimeStamps which requires
 *  additional replacementVariables such as a {@link com.entellitrak.localization.TimeZonePreferenceInfo}.
 * </p>
 *
 * @author zmiller
 */
public final class DataElementReplacer implements IReplacer {

    private final ExecutionContext etk;

    /**
     * This generates a new DataElementReplacer.
     *
     * @param executionContext The etk variable in entellitrak
     */
    public DataElementReplacer(final ExecutionContext executionContext){
        etk = executionContext;
    }

    @Override
    public String replace(final ITemplater templater, final String variableName,
            final Map<String, Object> replacementVariables) throws TemplateException {
        try {

            final String[] split = variableName.split("_");
            if(split.length != 2){
                throw new TemplateException(
                        String.format("Error encountered while trying to split replacement variable %s. The variable should have the form DataObjectName_dataElementName but instead of the expected 1 _ character there are %d",
                                variableName, split.length - 1));
            }

            final String dataObjectName = split[0];
            final String dataElementName = split[1];
            final String expectedTrackingIdFormat = dataObjectName + "Id";

            final List<Map<String, Object>> matchingDataElements = etk.createSQL("SELECT do.TABLE_NAME, de.COLUMN_NAME, de.DATA_TYPE, de.BUSINESS_KEY FROM etk_data_object DO JOIN etk_data_element de ON de.data_object_id = do.data_object_id WHERE do.tracking_config_id = ( SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) AND do.object_name = :dataObjectName AND de.element_name = :dataElementName")
                    .setParameter("dataObjectName", dataObjectName)
                    .setParameter("dataElementName", dataElementName)
                    .fetchList();

            if(matchingDataElements.size() == 0){
                throw new TemplateException(String.format("Error encountered while trying to perform replacement on %s. The query to find the dataObject/dataElement information from etk_data_object and etk_data_element returned 0 rows"
                        , variableName));
            }

            if(!replacementVariables.containsKey(expectedTrackingIdFormat)){
                throw new TemplateException(String.format("Could not perform the DataElementReplacement of \"%s\" because the key \"%s\" was not found",
                        variableName, expectedTrackingIdFormat));
            }

            final Map<String, Object> matchingDataElement = matchingDataElements.get(0);
            final List<Map<String, Object>> results = etk.createSQL("SELECT "+matchingDataElement.get("COLUMN_NAME")+" VALUE FROM "+matchingDataElement.get("TABLE_NAME")+" WHERE id = :trackingId")
                    .setParameter("trackingId", replacementVariables.get(expectedTrackingIdFormat))
                    .fetchList();

            final String result;
            if(results.size() == 0){
                result = "";
            }else{
                final Object value = results.get(0).get("VALUE");

                switch(DataElementType.getDataElementType(((Number) matchingDataElement.get("DATA_TYPE")).longValue())){
                    case TEXT:
                        result = Utility.nvl((String) value, "");
                        break;
                    case NUMBER:
                        result = value == null ? "" : ((Number) value).longValue() + "";
                        break;
                    case LONG:
                        result = value == null ? "" : ((Number) value).longValue() + "";
                        break;
                    case DATE:
                        result = value == null
                            ? ""
                            : Localizations.toLocalTimestamp(etk.getLocalization().getDefaultTimeZonePreference(),
                                    (java.sql.Timestamp) value)
                                    .getDateString();
                        break;
                    case CURRENCY:
                        result = value == null ? "" : new DecimalFormat("0.00").format(value);
                        break;
                    case YES_NO:
                        result = value == null ? ""
                                : ((Number) value).intValue() == 0 ? "No"
                                        : ((Number) value).intValue() == 1 ? "Yes"
                                                : value + "";
                        break;
                    case FILE:
                        result = etk.createSQL("SELECT file_name FROM etk_file WHERE id = :fileId")
                        .setParameter("fileId", value)
                        .returnEmptyResultSetAs("")
                        .fetchString();
                        break;
                    case STATE:
                        result = etk.createSQL("SELECT node.name_  FROM jbpm_processinstance processInstance JOIN jbpm_token token ON token.id_ = processInstance.roottoken_ JOIN jbpm_node node ON node.id_ = token.node_ WHERE processInstance.id_ = :processInstanceId")
                        .setParameter("processInstanceId", value)
                        .returnEmptyResultSetAs("")
                        .fetchString();
                        break;
                    case PASSWORD:
                        if(value == null || "".equals(value)){
                            result = "";
                        }else{
                            final Map<String, Object> userInfo = etk.createSQL("SELECT p.last_name LASTNAME, p.first_name FIRSTANME, u.username USERNAME FROM "+matchingDataElement.get("TABLE_NAME")+" obj LEFT JOIN etk_user u ON u.user_id = obj."+matchingDataElement.get("COLUMN_NAME")+"_uid LEFT JOIN etk_person p ON p.person_id = u.person_id WHERE obj.id = :trackingId")
                                    .setParameter("trackingId", replacementVariables.get(dataObjectName+"Id"))
                                    .returnEmptyResultSetAs(new HashMap<String, Object>())
                                    .fetchMap();

                            result = Utility.nvl(userInfo.get("LASTNAME"), "")
                                    + ", "
                                    + Utility.nvl(userInfo.get("FIRSTANME"), "")
                                    + " ("
                                    + Utility.nvl(userInfo.get("USERNAME"), "")
                                    + ")";
                        }
                        break;
                    case LONG_TEXT:
                        result = Utility.nvl((String) value, "");
                        break;
                    case TIMESTAMP:
                        result = value == null
                        ? ""
                        : Localizations.toLocalTimestamp(etk.getLocalization().getDefaultTimeZonePreference(),
                                (java.sql.Timestamp) value)
                                .getTimestampString();
                        break;
//                    case NONE:
//                        result = value == null
//                        ? ""
//                          : DataTypePluginClassUtility.getDataTypePluginDisplayFromStringValue(etk,
//                                  matchingDataElement.get("BUSINESS_KEY").toString(), value.toString());
//                        break;
                    default:
                        throw new TemplateException(
                                String.format("Unsupported data type \"%s\" encountered while trying to replace variable \"%s\"",
                                        matchingDataElement.get("DATA_TYPE"), variableName));
                }
            }
            return result;
        } catch (final Exception e) {
            throw new TemplateException(
                    String.format("Error performing Data Element replacement for %s", variableName), e);
        }
    }
}

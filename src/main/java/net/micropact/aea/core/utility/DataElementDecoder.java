package net.micropact.aea.core.utility;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.configuration.DataElement;
import com.entellitrak.localization.Localizations;
import com.entellitrak.lookup.For;
import com.entellitrak.lookup.LookupResult;

import net.entellitrak.aea.exception.TemplateException;
//import net.micropact.aea.core.dataTypePlugin.DataTypePluginClassUtility;
import net.micropact.aea.utility.Utility;

/* TODO: This class is a work in progress and should not be used anywhere*/
/**
 * This class is meant to determine values of data element for use in tools such as template utilities, audit log,
 * exporting tools. It should be able to get the values of fields, including things such as value and display of
 * lookups.
 *
 * @author Zachary.Miller
 */
public final class DataElementDecoder {

    /**
     * Utility classes do not need public constructors.
     */
    private DataElementDecoder(){}

    /**
     * Retrieve the information about a particular data element on a particular record. The type of the returned object
     * will depend on the type of the underlying data element. For instance, text fields would just return a String,
     * while lookups will return an object holding both their Value and Display attributes.
     *
     * @param etk entellitrak execution context
     * @param dataElement the data element to retrieve data for
     * @param trackingId the trackingId of the object to retrieve the data for
     * @return An object representing the data. The type of object returned will be determined by the type of the
     *      data element.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static Object getDataElementInformation(final ExecutionContext etk,
            final DataElement dataElement,
            final long trackingId) throws ApplicationException, IncorrectResultSizeDataAccessException{
        /* Have to return something which indicates whether it is a multiselect or not.
         * Lookups have to have both a display and a value. */

        if(dataElement.isMultiValued()){
            // Handle multi-select
            // TODO: handle multiselects
            return "TODO: Multiselects";
            // throw new ApplicationException("Not handling multiselects yet");
        }else{
            final Object databaseValue = etk.createSQL(String.format("SELECT %s FROM %s WHERE id = :trackingId",
                    dataElement.getColumnName(),
                    dataElement.getDataObject().getTableName()))
                    .setParameter("trackingId", trackingId)
                    .fetchObject();

            if(isLookup(etk, dataElement)){
                // Hande lookups
                return Utility.arrayToMap(String.class, Object.class, new Object[][]{
                    {"value", databaseValue},
                    {"display", getLookupDisplay(etk, dataElement, databaseValue, trackingId)}
                });
            }else{
                // Handle non-lookups
                // TODO: Do we actually want the display or do we want to keep things like null?
                return getSimpleDataElementDisplay(etk, dataElement, databaseValue, trackingId);
            }
        }
    }

    /**
     * This helper method is used to get the display value of "simple" fields.
     * This includes fields which do not involve complications that would be involved with lookups or multiselects.
     *
     * @param etk entellitrak execution context
     * @param dataElement data element to get the value for
     * @param value the database value of the element. This is only passed in to save the database calls which would be
     *      needed to retrieve it since the calling code already has access to this value.
     * @param trackingId the trackingId of the object which the element belongs to
     * @return The string representation of the object
     * @throws ApplicationException
     *      If there was an underlying {@link ApplicationException}
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static String getSimpleDataElementDisplay(final ExecutionContext etk,
            final DataElement dataElement,
            final Object value,
            final long trackingId) throws ApplicationException, IncorrectResultSizeDataAccessException{
        final String result;
        switch(dataElement.getDataType()){
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
                // TODO: Should files include a way to get the actual file content?
                result = etk.createSQL("SELECT file_name FROM etk_file WHERE id = :fileId")
                .setParameter("fileId", value)
                .returnEmptyResultSetAs("")
                .fetchString();
                break;
            case STATE:
                result = etk.createSQL(String.format("SELECT STATE_LABEL FROM %s WHERE id = :trackingId", dataElement.getDataObject().getTableName()))
                .setParameter("trackingId", trackingId)
                .fetchString();
                break;
            case PASSWORD:
                if(value == null || "".equals(value)){
                    result = "";
                }else{
                    final Map<String, Object> userInfo = etk.createSQL(String.format("SELECT p.last_name LASTNAME, p.first_name FIRSTANME, u.username USERNAME FROM %s obj LEFT JOIN etk_user u ON u.user_id = obj.%s_uid LEFT JOIN etk_person p ON p.person_id = u.person_id WHERE obj.id = :trackingId",
                            dataElement.getDataObject().getTableName(),
                            dataElement.getColumnName()))
                            .setParameter("trackingId", trackingId)
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
            case NONE:
                result = value == null
                ? ""
                  : null/*DataTypePluginClassUtility.getDataTypePluginDisplayFromStringValue(etk,
                          dataElement.getBusinessKey(), value.toString())*/;
                break;
            default:
                throw new TemplateException(
                        String.format("Unsupported data type \"%s\" encountered while trying to replace value %s",
                                dataElement.getDataType(),
                                value));
        }
        return result;
    }

    /**
     * Get the display value for a lookup given a particular data object and element.
     * The implementation is still up in the air due to questions such as:
     *  - do we just execute FOR_SINGLE or a different context?
     *  - what parameters get set (just the trackingId and/or the element's database value)?
     *
     * @param etk entellitrak execution context
     * @param dataElement data element
     * @param databaseValue the value which was stored in the database for this object
     * @param trackingId the tracking id
     * @return the display value of the lookup
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static String getLookupDisplay(final ExecutionContext etk, final DataElement dataElement, final Object databaseValue, final long trackingId) throws IncorrectResultSizeDataAccessException {
        final String lookupInfo = etk.createSQL("SELECT lookupDefinition.BUSINESS_KEY FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id JOIN etk_lookup_definition lookupDefinition ON lookupDefinition.lookup_definition_id = dataElement.lookup_definition_id WHERE dataObject.tracking_config_id = :trackingConfigId AND dataElement.business_key = :dataElementBusinessKey")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
                .setParameter("dataElementBusinessKey", dataElement.getBusinessKey())
                .fetchString();

        final List<LookupResult> lookupResults = etk.getLookupService().getLookup(lookupInfo)
                /* TODO: Have to set more parameters here */
                .set("trackingId", trackingId)
                .execute(For.SINGLE);

        String display;

        if(lookupResults.isEmpty()){
            display = null;
        }else{
            display = lookupResults.get(0).getDisplay();
        }

        return display;
    }

    /**
     * This method determines whether or not a data element is a lookup.
     *
     * @param etk entellitrak execution context
     * @param dataElement the data element to test
     * @return whether the data element is a lookup
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static boolean isLookup(final ExecutionContext etk, final DataElement dataElement) throws IncorrectResultSizeDataAccessException {
        return null != etk.createSQL("SELECT dataElement.LOOKUP_DEFINITION_ID FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId AND dataElement.business_key = :dataElementBusinessKey")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
                .setParameter("dataElementBusinessKey", dataElement.getBusinessKey())
                .fetchInt();
    }
}

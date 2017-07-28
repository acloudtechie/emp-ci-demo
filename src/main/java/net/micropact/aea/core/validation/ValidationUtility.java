package net.micropact.aea.core.validation;

import java.util.Map;

import com.entellitrak.BaseObjectEventContext;
import com.entellitrak.DataEventType;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.ReferenceObjectEventContext;
import com.entellitrak.configuration.DataObject;
import com.entellitrak.dynamic.DataObjectInstance;
import com.entellitrak.dynamic.DataObjectProperties;
import com.entellitrak.query.FileInfo;

import net.micropact.aea.core.utility.StringEscapeUtils;
import net.micropact.aea.utility.Utility;

/**
 * This class contains various methods for performing common validation actions,
 * mainly around data object event handlers.
 *
 * @author zmiller
 */
public final class ValidationUtility {

    /**
     * Utility classes do not need constructors.
     */
    private ValidationUtility(){}

    /**
     * This method ensures that the C_CODE column of the object being saved is unique for that type of object.
     * The validation is only done on Create or Update events.
     *
     * @param etk entellitrak execution context
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static void validateUniqueCode(final BaseObjectEventContext etk) throws IncorrectResultSizeDataAccessException {
        final DataObjectInstance newObject = etk.getNewObject();
        final DataObjectProperties properties = newObject.properties();
        final DataObject configuration = newObject.configuration();

        final String tableName = configuration.getTableName();

        final Map<String, Object> duplicateInfo = etk.createSQL(String.format("SELECT C_CODE, COUNT(*) COUNT FROM %s WHERE c_code = (SELECT c_code FROM %s WHERE id = :trackingId) GROUP BY c_code",
                tableName, tableName))
                .setParameter("trackingId", properties.getId())
                .fetchMap();
        final long matchingCodes = ((Number) duplicateInfo.get("COUNT")).longValue();

        if(1 < matchingCodes){
            Utility.cancelTransactionMessage(etk,
                    StringEscapeUtils.escapeHtml(
                            String.format("Could not save the record because there is already a %s with Code \"%s\"",
                                    configuration.getLabel(),
                                    duplicateInfo.get("C_CODE"))));
        }
    }

    /**
     * Determine whether a data object has a code element.
     *
     * @param etk entellitrak execution context
     * @param dataObject the data object
     * @return whether the data object has a code element
     */
    private static boolean hasCodeElement(final ExecutionContext etk, final DataObject dataObject){
        return etk.getDataElementService().getDataElements(dataObject)
                .stream()
                .anyMatch(dataElement -> dataElement.getPropertyName().equals("code"));
    }

    /**
     * Ensure that an object's code is either globally unique (if it has no parent), or unique amongst its siblings
     * (if it has a parent).
     *
     * @param etk entellitrak execution context
     * @throws IncorrectResultSizeDataAccessException
     *          If there is an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static void ensureUniqueCodePerParent(final BaseObjectEventContext etk)
            throws IncorrectResultSizeDataAccessException{

        final DataEventType dataEventType = etk.getDataEventType();
        final DataObject dataObject = etk.getNewObject().configuration();

        if(DataEventType.CREATE == dataEventType
                || DataEventType.UPDATE == dataEventType){
            if(hasCodeElement(etk, dataObject)){
                if(etk.getDataObjectService().getParent(dataObject) == null){
                    validateUniqueCode(etk);
                }else{
                    validateUniqueCodePerParent(etk);
                }
            }
        }

    }

    /**
     * This method ensures that the C_CODE column for the object being saved is unique for all of its siblings.
     * This method only works if it is called on a child object (since only child objects have siblings).
     * The validation is only done on Create or Update events.
     *
     * @param etk entellitrak execution context
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static void validateUniqueCodePerParent(final BaseObjectEventContext etk)
            throws IncorrectResultSizeDataAccessException{

        final String tableName = etk.getNewObject().configuration().getTableName();

        final Map<String, Object> duplicateInfo = etk.createSQL(
                String.format("SELECT thisObject.c_code C_CODE, (SELECT COUNT(*) FROM %s otherObject WHERE otherObject.id != thisObject.id AND otherObject.c_code = thisObject.c_code AND otherObject.id_parent = thisObject.id_parent) DUPLICATES FROM %s thisObject WHERE thisObject.id = :trackingId",
                        tableName, tableName))
                .setParameter("trackingId", etk.getNewObject().properties().getId())
                .fetchMap();

        if(((Number) duplicateInfo.get("DUPLICATES")).longValue() > 0){
            Utility.cancelTransactionMessage(etk,
                    StringEscapeUtils.escapeHtml(
                            String.format("Could not save the record because there is already a %s with Code \"%s\" under this %s",
                                    etk.getNewObject().configuration().getLabel(),
                                    duplicateInfo.get("C_CODE"),
                                    etk.getDataObjectService().getParent(etk.getNewObject().configuration())
                                    .getLabel())));
        }
    }

    /**
     * This method ensures that a particular file element has a particular file extension.
     *
     * @param etk entellitrak execution context
     * @param elementName The name of the element to check
     * @param expectedExtension The expected file extension (does not contain a leading ".")
     */
    public static void ensureFileIsCorrectType(final ReferenceObjectEventContext etk,
            final String elementName,
            final String expectedExtension) {

        final DataEventType dataEventType = etk.getDataEventType();
        if(DataEventType.CREATE == dataEventType
                || DataEventType.UPDATE == dataEventType){
            final DataObjectInstance newObject = etk.getNewObject();

            final FileInfo fileInfo =
                    etk.getFromObject(newObject.configuration().getBusinessKey(), newObject.properties().getId())
                    .element(elementName)
                    .fetch()
                    .getFileInfo(elementName);

            if(fileInfo != null){
                final String actualExtension = fileInfo.getFileExtension();
                if(!expectedExtension.equals(actualExtension)){
                    Utility.cancelTransactionMessage(etk,
                            String.format("Expected a \"%s\" file but instead received \"%s\" file",
                                    expectedExtension,
                                    actualExtension));
                }
            }
        }
    }
}

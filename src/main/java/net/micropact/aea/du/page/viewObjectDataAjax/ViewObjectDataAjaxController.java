package net.micropact.aea.du.page.viewObjectDataAjax;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.configuration.DataElement;
import com.entellitrak.configuration.DataObject;
import com.entellitrak.exception.IncorrectResultSizeException;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.query.QueryUtility;
import net.micropact.aea.utility.IJson;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This class serves as the controller code for a JSON page which can fetch information about a BTO and all of its
 * descendants.
 *
 * @author zachary.miller
 */
public class ViewObjectDataAjaxController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();
            response.setContentType(ContentType.JSON);

            final String objectBusinessKey = etk.getParameters().getSingle("dataObjectKey");
            final long trackingId = Long.parseLong(etk.getParameters().getSingle("trackingId"));

//             final String objectBusinessKey = "object.rfWorkflow";
//             final long trackingId = 50;

            response.put("out", JsonUtilities.encode(fetchBTO(etk, objectBusinessKey, trackingId)));

            return response;
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Retrieves information about a single BTO and all of its descendants.
     *
     * @param etk entellitrak execution context
     * @param businessKey business key of the object
     * @param trackingId trackingId of the object
     * @return A representation of the object and its descendants.
     * @throws IncorrectResultSizeException If there was an underlying {@link IncorrectResultSizeException}
     * @throws IncorrectResultSizeDataAccessException
     *      If there twas an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private static List<Map<String, Object>> fetchBTO(
            final ExecutionContext etk,
            final String businessKey,
            final long trackingId)
            throws IncorrectResultSizeException, IncorrectResultSizeDataAccessException, ApplicationException{
        final List<Map<String, Object>> objectList = new LinkedList<>();

        final List<TreeNode> objectNodes = new LinkedList<>();
        objectNodes.add(fetchTreeNode(etk, businessKey, trackingId));


        objectList.add(Utility.arrayToMap(String.class, Object.class, new Object[][]{
            {"businessKey", businessKey},
            {"name", etk.getDataObjectService().getDataObjectByBusinessKey(businessKey).getName()},
            {"objects", objectNodes}
        }));

        return objectList;
    }

    /**
     * Gets information about a particular data object and all of its descendants.
     *
     * @param etk entellitrak execution context
     * @param objectBusinessKey business key of the object
     * @param trackingId trackingId of the object
     * @return An object containing information about the object and all of its descendants.
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws IncorrectResultSizeException If there was an underlying {@link IncorrectResultSizeException}
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private static TreeNode fetchTreeNode(
            final ExecutionContext etk,
            final String objectBusinessKey,
            final long trackingId)
            throws IncorrectResultSizeDataAccessException, IncorrectResultSizeException, ApplicationException{
        return new TreeNode(etk,
                objectBusinessKey,
                trackingId,
                fetchElements(etk, objectBusinessKey, trackingId),
                fetchChildren(etk, objectBusinessKey, trackingId));
    }

    /**
     * Gets the elements and their data for a particular data object.
     *
     * @param etk entellitrak execution context
     * @param objectBusinessKey business key of the object
     * @param trackingId trackingId of the object
     * @return An object representing the elements and their data
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static List<Map<String, Object>> fetchElements(
            final ExecutionContext etk,
            final String objectBusinessKey,
            final long trackingId)
            throws IncorrectResultSizeDataAccessException{
        final List<Map<String, Object>> returnElements = new LinkedList<>();

        final List<String> elementBusinessKeys = QueryUtility.toSimpleList(etk.createSQL("SELECT dataElement.business_key FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id WHERE tracking_config_id = :trackingConfigId AND dataObject.business_key = :dataObjectKey ORDER BY dataElement.name, dataElement.business_key")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
                .setParameter("dataObjectKey", objectBusinessKey)
                .fetchList());

        for(final String elementBusinessKey : elementBusinessKeys){
            final DataElement dataElement = etk.getDataElementService().getDataElementByBusinessKey(elementBusinessKey);

            returnElements.add(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"elementBusinessKey", dataElement.getBusinessKey()},
                {"name", dataElement.getName()},
                {"value", //DataElementDecoder.getDataElementInformation(etk, dataElement, trackingId)
                    getDataElementValue(etk, dataElement, trackingId)}
            }));
        }

        return returnElements;
    }

    /**
     * This method gets all of the descendants and their data for a particular parent object.
     *
     * @param etk entellitrak execution context
     * @param objectBusinessKey business key of the object
     * @param trackingId trackingId of the object
     * @return An object representing all of the descendants of an object and their data
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws IncorrectResultSizeException If there was an underlying {@link IncorrectResultSizeException}
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private static List<Map<String, Object>> fetchChildren(
            final ExecutionContext etk,
            final String objectBusinessKey,
            final long trackingId)
            throws IncorrectResultSizeDataAccessException, IncorrectResultSizeException, ApplicationException {
        final List<Map<String, Object>> returnList = new LinkedList<>();

        final List<String> childBusinessKeys = QueryUtility.toSimpleList(etk.createSQL("SELECT childObject.business_key FROM etk_data_object parentObject JOIN etk_data_object childObject ON childObject.parent_object_id = parentObject.data_object_id WHERE parentObject.tracking_config_id = :trackingConfigId AND parentObject.business_key = :dataObjectKey ORDER BY childObject.list_order, childObject.name")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
                .setParameter("dataObjectKey", objectBusinessKey)
                .fetchList());

        for(final String childBusinessKey : childBusinessKeys){
            final DataObject childObject = etk.getDataObjectService().getDataObjectByBusinessKey(childBusinessKey);
            final List<TreeNode> childNodes = new LinkedList<>();

            for(final long childTrackingId : QueryUtility.mapsToLongs(etk.createSQL(String.format("SELECT ID FROM %s WHERE id_parent = :trackingId",
                    childObject.getTableName()))
                    .setParameter("trackingId", trackingId)
                    .fetchList())){
                childNodes.add(fetchTreeNode(etk, childObject.getBusinessKey(), childTrackingId));
            }
            returnList.add(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"businessKey", childBusinessKey},
                {"name", childObject.getName()},
                {"objects", childNodes}
            }));
        }

        return returnList;
    }

    /**
     * This method gets the value of a particular element for a particular object.
     *
     * @param etk entellitrak execution context
     * @param dataElement data element the data is to be retrieved for
     * @param trackingId trackingId of the object
     * @return The value of the element
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static Object getDataElementValue(
            final ExecutionContext etk,
            final DataElement dataElement,
            final long trackingId)
            throws IncorrectResultSizeDataAccessException {
        final Object returnValue;
        if(dataElement.isMultiValued()){
            returnValue = QueryUtility.toSimpleList(etk.createSQL(String.format("SELECT %s FROM %s WHERE id_owner = :idOwner ORDER BY  list_order, id",
                    dataElement.getColumnName(),
                    dataElement.getTableName()))
            .setParameter("idOwner", trackingId)
            .fetchList());
        }else{
            returnValue = etk.createSQL(String.format("SELECT %s FROM %s WHERE id = :trackingId",
                    dataElement.getColumnName(),
                    dataElement.getDataObject().getTableName()))
                    .setParameter("trackingId", trackingId)
                    .fetchObject();
        }
        return returnValue;
    }

    /**
     * This class represents a data object's data and the data of all of its descendants.
     *
     * @author zachary.miller
     */
    static class TreeNode implements IJson{
        private final ExecutionContext etk;
        private final String businessKey;
        private final long trackingId;
        private final List<Map<String, Object>> elements;
        private final List<Map<String, Object>> children;

        /**
         * Constructs a new TreeNode.
         *
         * @param executionContext entellitrak execution context
         * @param theBusinessKey business key of the object
         * @param theTrackingId trackingId of the object
         * @param theElements the elements of this object
         * @param theChildren object representing the descendants of the object.
         */
        TreeNode(final ExecutionContext executionContext,
                final String theBusinessKey,
                final long theTrackingId,
                final List<Map<String, Object>> theElements,
                final List<Map<String, Object>> theChildren) {
            etk = executionContext;
            businessKey = theBusinessKey;
            trackingId = theTrackingId;
            elements = theElements;
            children = theChildren;
        }

        @Override
        public String encode() {
            return JsonUtilities.encode(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"dataObjectKey", businessKey},
                {"name", etk.getDataObjectService().getDataObjectByBusinessKey(businessKey).getName()},
                {"trackingId", trackingId},
                {"elements", elements},
                {"children", children}
            }));
        }
    }
}

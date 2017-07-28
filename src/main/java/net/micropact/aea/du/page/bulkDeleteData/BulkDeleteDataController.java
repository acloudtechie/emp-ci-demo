package net.micropact.aea.du.page.bulkDeleteData;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.InputValidationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.WorkExecutionException;
import com.entellitrak.configuration.DataObject;
import com.entellitrak.dynamic.DataObjectInstance;
import com.entellitrak.dynamic.DynamicObjectService;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.query.QueryUtility;
import net.micropact.aea.core.utility.DynamicObjectConfigurationUtils;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This is a {@link PageController} for a page which deletes all BTOs of a particular type.
 * It uses the underlying core commands (deletWorkflow) and therefore has any limitations of those methods.
 *
 * @author zmiller
 */
public class BulkDeleteDataController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            final Set<String> keysToDelete = new HashSet<>(Optional.ofNullable(etk.getParameters().getField("dataObjects"))
                    .orElse(new LinkedList<>()));

            final List<Map<String, Object>> dataObjects = etk.createSQL("SELECT dataObject.OBJECT_TYPE, dataObject.BUSINESS_KEY, dataObject.LABEL, dataObject.TABLE_NAME FROM etk_data_object dataObject WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive) AND dataObject.parent_object_id IS NULL ORDER BY OBJECT_TYPE, LABEL, BUSINESS_KEY")
                    .fetchList();

            /* We will delete any objects we were requested to */
            dataObjects
            .stream()
            .filter(dataObject -> keysToDelete.contains(dataObject.get("BUSINESS_KEY")))
            .forEachOrdered(dataObject -> {
                try {
                    deleteObjectType(etk, (String) dataObject.get("BUSINESS_KEY"));
                } catch (final ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });

            /* We add the counts. This has to be done after the deletions happen so that the counts are 0
             * for ones just deleted */
            for(final Map<String, Object> dataObject : dataObjects){
                final String tableName = (String) dataObject.get("TABLE_NAME");
                dataObject.put("count", etk.createSQL("SELECT COUNT(*) FROM "+ tableName)
                        .fetchInt());
            }

            response.put("dataObjects", JsonUtilities.encode(dataObjects));

            return response;
        }catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Deletes all data objects of a particular type.
     *
     * @param etk entellitrak execution context
     * @param objectBusinessKey business key of the object which is to be deleted
     * @throws ClassNotFoundException If there was an underlying {@link ClassNotFoundException}
     */
    private static void deleteObjectType(final ExecutionContext etk,
            final String objectBusinessKey) throws ClassNotFoundException{

        final Class<? extends DataObjectInstance> objectClass = DynamicObjectConfigurationUtils.getDynamicClass(etk, objectBusinessKey);

        getIdsForDataObject(etk, etk.getDataObjectService().getDataObjectByBusinessKey(objectBusinessKey))
        .forEachOrdered(trackingId -> {
            try {
                etk.doWork(workEtk -> {
                    final DynamicObjectService objectService = workEtk.getDynamicObjectService();
                    try {
                        objectService.createDeleteOperation(objectService.get(objectClass, trackingId))
                        .delete();
                    } catch (final InputValidationException e) {
                        throw new ApplicationException(e);
                    }
                });
            } catch (final WorkExecutionException e) {
                Utility.aeaLog(etk, String.format("Error attempting to delete object of type %s with trackingId %s",
                        objectBusinessKey,
                        trackingId), e);
            }
        });
    }

    /**
     * Retrieves the trackingIds of all objects of a particular type.
     *
     * @param etk entellitrak execution
     * @param dataObjectType the type of object to get ids for
     * @return the tracking ids
     */
    private static Stream<Long> getIdsForDataObject(final ExecutionContext etk, final DataObject dataObjectType){
        return QueryUtility.mapsToLongs(etk.createSQL(String.format("SELECT id FROM %s ORDER BY id",
                dataObjectType.getTableName()))
                .fetchList())
                .stream();
    }
}

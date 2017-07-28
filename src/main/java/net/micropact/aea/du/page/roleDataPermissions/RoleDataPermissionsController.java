package net.micropact.aea.du.page.roleDataPermissions;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.DataObjectType;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * <p>
 *  This page serves as the controller code for a page which can be used to display the Role Data Permissions for
 *  Data Objects in the system.
 * </p>
 *
 * <p>
 *  Note: Before entellitrak 3.21.0.0.0 (Role-base data element permissions) this page used to be able to update existing
 *  permissions. With that release the page was temporarily disabled, and then later it was changed to just display
 *  Data Object permissions (and not support update).
 * </p>
 *
 * @author zachary.miller
 */
public class RoleDataPermissionsController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {

            /* We need to know whether this is the first time they are viewing this page, or whether they have hit the
             * refresh button. This is because we want to select al of the permissions on new, but want to only take
             * the submitted ones on updated. */
            final boolean isCreate = etk.getParameters().getSingle("update") == null;
            final List<String> selectedRoles = Optional.ofNullable(etk.getParameters().getField("roles")).orElse(new LinkedList<>());
            final List<String> selectedDataObjects = Optional.ofNullable(etk.getParameters().getField("dataObjects")).orElse(new LinkedList<>());
            final List<String> selectedPermissions = Optional.ofNullable(etk.getParameters().getField("permissionTypes")).orElse(new LinkedList<>());

            final TextResponse response = etk.createTextResponse();

            // Roles
            final List<Map<String, Object>> allRoles = etk.createSQL("SELECT ROLE_ID, NAME FROM etk_role ORDER BY NAME")
                    .fetchList();
            addEnabled(allRoles, "ROLE_ID", selectedRoles);

            // Data Objects
            final List<Map<String, Object>> allDataObjects = etk.createSQL("SELECT DATA_OBJECT_ID, PARENT_OBJECT_ID, TABLE_NAME, NULL COLUMN_NAME, LABEL, LIST_ORDER FROM etk_data_object WHERE tracking_config_id = :trackingConfigId AND object_type = :objectType ORDER BY LIST_ORDER, LABEL")
                    .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
                    .setParameter("objectType", DataObjectType.TRACKING.getEntellitrakNumber())
                    .fetchList();
            addEnabled(allDataObjects, "DATA_OBJECT_ID", selectedDataObjects);

            response.put("roles", JsonUtilities.encode(allRoles));
            response.put("dataObjects", JsonUtilities.encode(allDataObjects));
            response.put("selectedPermissions", JsonUtilities.encode(selectedPermissions));
            response.put("dataObjectPermissions", etk.createSQL("SELECT dataPermission.DATA_OBJECT_TYPE, dataPermission.ROLE_ID, dataPermission.ASSIGN_ACCESS_LEVEL, dataPermission.CREATE_ACCESS_LEVEL, dataPermission.READ_ACCESS_LEVEL, dataPermission.UPDATE_ACCESS_LEVEL, dataPermission.DELETE_ACCESS_LEVEL, dataPermission.SEARCHING_ACCESS_LEVEL FROM etk_data_permission dataPermission WHERE data_element_type IS NULL")
                    .fetchJSON());
            response.put("isCreate", JsonUtilities.encode(isCreate));

            return response;
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * This method adds the enabled property to multiselects so that they can be persisted across page refreshes.
     * Each map in allValues will be given an enabled property. The property will be true, if the identifier's value
     * (found via the idPoperty) matches a value in selectedValues (after converting it to a String).
     *
     * @param allValues the raw database maps to add the enabled property to
     * @param idProperty the key which is used as the identifier for the values
     * @param selectedValues The String representations of the selected values
     */
    private static void addEnabled(final List<Map<String, Object>> allValues,
            final String idProperty,
            final List<String> selectedValues){
        allValues.stream().forEachOrdered(allValue ->
            allValue.put("enabled", selectedValues.contains(allValue.get(idProperty).toString())));
    }
}

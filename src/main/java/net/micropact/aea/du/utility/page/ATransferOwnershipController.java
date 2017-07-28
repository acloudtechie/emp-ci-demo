package net.micropact.aea.du.utility.page;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.query.QueryUtility;
import net.micropact.aea.utility.JsonUtilities;

/**
 * Abstract class for more easily implementing the controller code for the pages which transfer items from one user
 * to another.
 *
 * @author Zachary.Miller
 */
public abstract class ATransferOwnershipController implements PageController {

    @Override
    public final Response execute(final PageExecutionContext etk) throws ApplicationException {

        final TextResponse response = etk.createTextResponse();

        final String action = etk.getParameters().getSingle("action");
        final String toUser = etk.getParameters().getSingle("toUser");
        final List<String> selectedFromUsers = Optional.ofNullable(etk.getParameters().getField("fromUsers")).orElse(new LinkedList<>());
        final List<String> selectedItems = Optional.ofNullable(etk.getParameters().getField("items")).orElse(new LinkedList<>());

        boolean transferred = false;
        final List<String> errors = new LinkedList<>();

        if("transfer".equals(action)){
            if(StringUtility.isBlank(toUser)){
                errors.add("Transfer To is Required");
            }

            if(selectedItems.isEmpty()){
                errors.add(String.format("%ss are Required", getItemName()));
            }

            if(errors.isEmpty()){
                etk.createSQL(getUpdateQuery())
                .setParameter("userId", toUser)
                .setParameter("itemIds", selectedItems)
                .execute();

                transferred = true;
            }
        }

        response.put("transferred", transferred);
        response.put("errors", JsonUtilities.encode(errors));
        response.put("toUser", JsonUtilities.encode(toUser));
        response.put("selectedFromUsers", JsonUtilities.encode(selectedFromUsers));

        /* We will refresh the list of selected items to be only ones that are owned by the selectedUsers */
        final List<Long> selectedItemsOuput = QueryUtility.mapsToLongs(etk.createSQL(getSelectedItemsQuery())
                .setParameter("itemIds", QueryUtility.toNonEmptyParameterList(selectedItems))
                .setParameter("userIds", QueryUtility.toNonEmptyParameterList(selectedFromUsers))
                .fetchList());
        response.put("selectedItems", JsonUtilities.encode(selectedItemsOuput));

        response.put("users", etk.createSQL(getUsersWithItemsOrPermissionsQuery())
                .setParameter("systemPermissions", getGrantablePermissions(getPermission()))
                .fetchJSON());

        response.put("items", etk.createSQL(getItemsQuery())
                .fetchJSON());

        return response;
    }

    /**
     * Gets all permissions which would grant a particular permission. This is needed because of the way entellitrak
     * stores permissions in the database. There is an implied tree structure to permissions where being granted
     * to a parent permission gives you implicit access to all of the descendent permissions.
     *
     * @param permission permission
     * @return all permissions which would grant that permission
     */
    private static List<String> getGrantablePermissions(final String permission){
        List<String> returnValue;

        final int lastPeriodIndex = permission.lastIndexOf('.');
        if(lastPeriodIndex == -1){
            returnValue = new LinkedList<>();
        }else{
            returnValue = getGrantablePermissions(permission.substring(0, lastPeriodIndex));
        }

        returnValue.add(permission);

        return returnValue;
    }

    /**
     * The name of the item being transfered.
     *
     * @return the item name
     */
    protected abstract String getItemName();

    /**
     * The query to transfer the items to their new owner.
     * The following parameters are available:
     * <ul>
     *  <li>:userId</li>
     *  <li>:itemIds</li>
     * </ul>
     *
     * @return the update query
     */
    protected abstract String getUpdateQuery();

    /**
     * Get the selected items to be passed back to the front-end. This is needed to give the front-end user a good user
     * experience in both the case of canceled transactions and successful transfers.
     *
     * The following parameters are available:
     * <ul>
     *  <li>:userIds</li>
     *  <li>:itemIds</li>
     * </ul>
     *
     * @return the selected items query
     */
    protected abstract String getSelectedItemsQuery();

    /**
     * Get the query to select all users which have items.
     * The following parameters are available:
     * <ul>
     *  <li>:systemPermissions</li>
     * </ul>
     *
     * Query must select the following columns:
     * <ul>
     *  <li>USER_ID</li>
     *  <li>USERNAME</li>
     * </ul>
     *
     * @return the users with items query
     */
    protected abstract String getUsersWithItemsOrPermissionsQuery();

    /**
     * Get the system permission which is required to own the item.
     *
     * @return the permission
     */
    protected abstract String getPermission();

    /**
     * Get a query to fetch all items.
     *
     * Query must select the following columns:
     * <ul>
     *  <li>ITEM_ID</li>
     *  <li>NAME</li>
     *  <li>BUSINESS_KEY</li>
     *  <li>USER_ID</li>
     * </ul>
     *
     * @return the items query
     */
    protected abstract String getItemsQuery();
}

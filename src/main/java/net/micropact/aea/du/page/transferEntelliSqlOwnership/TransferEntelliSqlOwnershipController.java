package net.micropact.aea.du.page.transferEntelliSqlOwnership;

import net.micropact.aea.du.utility.page.ATransferOwnershipController;

/**
 * This class is the controller code for a page which can transfer ownership of entelliSQL queries.
 *
 * @author zmiller
 */
public class TransferEntelliSqlOwnershipController extends ATransferOwnershipController {

    @Override
    protected String getItemName() {
        return "entelliSQL";
    }

    @Override
    protected String getUpdateQuery() {
        return "UPDATE etk_query SET user_id = :userId WHERE query_id IN (:itemIds)";
    }

    @Override
    protected String getSelectedItemsQuery() {
        return "SELECT query_id ITEM_ID FROM etk_query WHERE query_id IN(:itemIds) AND user_id IN(:userIds) ORDER BY ITEM_ID";
    }

    @Override
    protected String getUsersWithItemsOrPermissionsQuery() {
        return "WITH pageCreateRoles AS ( SELECT role_id FROM etk_role_permission WHERE permission_key IN(:systemPermissions)) SELECT USER_ID, USERNAME, CASE WHEN EXISTS(SELECT * FROM etk_subject_role sr WHERE sr.subject_id = u.user_id AND sr.role_id IN (SELECT role_id FROM pageCreateRoles)) THEN 1 ELSE 0 END HASCREATE FROM etk_user u WHERE EXISTS(SELECT * FROM etk_page p WHERE p.user_id = u.user_id) OR EXISTS(SELECT * FROM etk_subject_role sr WHERE sr.subject_id = u.user_id AND sr.role_id IN (SELECT role_id FROM pageCreateRoles)) ORDER BY username";
    }

    @Override
    protected String getPermission() {
        return "permission.entellisql.create";
    }

    @Override
    protected String getItemsQuery() {
        return "SELECT query_id ITEM_ID, NAME, query_id BUSINESS_KEY, USER_ID FROM etk_query ORDER BY ITEM_ID";
    }
}

package net.micropact.aea.du.page.transferPageOwnership;

import net.micropact.aea.du.utility.page.ATransferOwnershipController;

/**
 * Controller code for a page which allows the user to transfer ownership of Pages from one user to another.
 *
 * @author zmiller
 */
public class TransferPageOwnershipController extends ATransferOwnershipController {

    @Override
    protected String getItemName() {
        return "Page";
    }

    @Override
    protected String getUpdateQuery() {
        return "UPDATE etk_page SET user_id = :userId WHERE page_id IN(:itemIds)";
    }

    @Override
    protected String getSelectedItemsQuery() {
        return "SELECT page_id ITEM_ID FROM etk_page WHERE page_id IN(:itemIds) AND user_id IN(:userIds) ORDER BY ITEM_ID";
    }

    @Override
    protected String getUsersWithItemsOrPermissionsQuery() {
        return "WITH pageCreateRoles AS ( SELECT role_id FROM etk_role_permission WHERE permission_key IN(:systemPermissions)) SELECT USER_ID, USERNAME, CASE WHEN EXISTS(SELECT * FROM etk_subject_role sr WHERE sr.subject_id = u.user_id AND sr.role_id IN (SELECT role_id FROM pageCreateRoles)) THEN 1 ELSE 0 END HASCREATE FROM etk_user u WHERE EXISTS(SELECT * FROM etk_page p WHERE p.user_id = u.user_id) OR EXISTS(SELECT * FROM etk_subject_role sr WHERE sr.subject_id = u.user_id AND sr.role_id IN (SELECT role_id FROM pageCreateRoles)) ORDER BY username";
    }

    @Override
    protected String getPermission() {
        return "permission.page.manage.create";
    }

    @Override
    protected String getItemsQuery() {
        return "SELECT page_id ITEM_ID, NAME, BUSINESS_KEY, USER_ID FROM etk_page p ORDER BY name, business_key, ITEM_ID";
    }
}

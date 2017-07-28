package net.micropact.aea.du.page.transferReportOwnership;

import net.micropact.aea.du.utility.page.ATransferOwnershipController;

/**
 * Controller code for a page which allows the user to transfer ownership of a Reports from one user to another.
 *
 * @author zmiller
 */
public class TransferReportOwnershipController extends ATransferOwnershipController {

    @Override
    protected String getItemName() {
        return "Report";
    }

    @Override
    protected String getUpdateQuery() {
        return "UPDATE etk_saved_report SET user_id = :userId, created_by = (SELECT username FROM etk_user WHERE user_id = :userId) WHERE saved_report_id IN (:itemIds)";
    }

    @Override
    protected String getSelectedItemsQuery() {
        return "SELECT saved_report_id ITEM_ID FROM etk_saved_report WHERE saved_report_id IN(:itemIds) AND user_id IN(:userIds) ORDER BY ITEM_ID";
    }

    @Override
    protected String getUsersWithItemsOrPermissionsQuery() {
        return "WITH reportCreateRoles AS (SELECT role_id FROM etk_role_permission WHERE permission_key IN(:systemPermissions) ) SELECT USER_ID, USERNAME, CASE WHEN EXISTS (SELECT * FROM etk_subject_role sr WHERE sr.subject_id = u.user_id AND sr.role_id IN (SELECT role_id FROM reportCreateRoles ) ) THEN 1 ELSE 0 END HASCREATE FROM etk_user u WHERE EXISTS (SELECT * FROM etk_saved_report p WHERE p.user_id = u.user_id ) OR EXISTS (SELECT * FROM etk_subject_role sr WHERE sr.subject_id = u.user_id AND sr.role_id IN (SELECT role_id FROM reportCreateRoles ) ) ORDER BY username";
    }

    @Override
    protected String getPermission() {
        return "permission.reporting.create";
    }

    @Override
    protected String getItemsQuery() {
        return "SELECT saved_report_id ITEM_ID, NAME, BUSINESS_KEY, USER_ID FROM etk_saved_report ORDER BY BUSINESS_KEY, ITEM_ID";
    }
}

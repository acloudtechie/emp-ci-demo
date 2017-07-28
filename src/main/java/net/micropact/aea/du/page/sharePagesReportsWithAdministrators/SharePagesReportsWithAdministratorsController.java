package net.micropact.aea.du.page.sharePagesReportsWithAdministrators;

import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.Utility;

/**
 * This page updates the page and report permissions so that the Administration role will have access to edit
 * all pages and reports.
 * @author zmiller
 */
public class SharePagesReportsWithAdministratorsController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

        try {
            final TextResponse response = etk.createTextResponse();

            response.setContentType(ContentType.HTML);

            final String administrationId = etk
                    .createSQL(
                            "SELECT role.role_id FROM etk_role role WHERE role.business_key = 'role.administration'")
                            .fetchString();

            // Update any existing administrator page/report permissions
            etk.createSQL("UPDATE etk_shared_object_permission SET is_edit = 1, is_execute = 1, is_display = 1 WHERE role_id = :roleId")
            .setParameter("roleId", administrationId).execute();

            // Handle pages which don't have an administrator permission
            final List<Map<String, Object>> pages = etk.createSQL("SELECT p.PAGE_ID FROM etk_page p WHERE NOT EXISTS(SELECT * FROM etk_page_permission pp JOIN etk_shared_object_permission sop ON sop.shared_object_permission_id = pp.page_permission_id WHERE pp.page_id = p.page_id AND sop.role_id = :roleId)")
                    .setParameter("roleId", administrationId).fetchList();
            for (final Map<String, Object> page : pages) {

                String sharedObjectPermissionId;

                if (Utility.isSqlServer(etk)) { // We have to do this separately
                    // because for some reason
                    // Oracle cannot handle
                    // executeForKey in this context
                    sharedObjectPermissionId = String
                            .valueOf(etk.createSQL("INSERT INTO etk_shared_object_permission(is_edit, is_execute, is_display, subject_id, role_id, is_all_users) VALUES (1, 1, 1, NULL, :roleId, 0)")
                                    .setParameter("roleId", administrationId)
                                    .executeForKey(
                                            "shared_object_permission_id"));
                } else {
                    sharedObjectPermissionId = etk.createSQL("SELECT HIBERNATE_SEQUENCE.NEXTVAL FROM DUAL")
                            .fetchString();
                    etk.createSQL("INSERT INTO etk_shared_object_permission(shared_object_permission_id, is_edit, is_execute, is_display, subject_id, role_id, is_all_users) VALUES (:sharedObjectPermissionId, 1, 1, 1, NULL, :roleId, 0)")
                    .setParameter("roleId", administrationId)
                    .setParameter("sharedObjectPermissionId",
                            sharedObjectPermissionId).execute();
                }

                etk.createSQL("INSERT INTO etk_page_permission(page_permission_id, page_id) VALUES(:pagePermissionId, :pageId)")
                .setParameter("pagePermissionId",
                        sharedObjectPermissionId)
                        .setParameter("pageId", page.get("PAGE_ID")).execute();
            }

            // Handle reports which don't have an administrator permission
            final List<Map<String, Object>> reports = etk.createSQL("SELECT R.SAVED_REPORT_ID FROM etk_saved_report r WHERE NOT EXISTS ( SELECT * FROM etk_report_permission rp JOIN etk_shared_object_permission sop ON sop.shared_object_permission_id = rp.report_permission_id WHERE rp.saved_report_id = r.saved_report_id AND sop.role_id = :roleId )")
                    .setParameter("roleId", administrationId).fetchList();
            for (final Map<String, Object> report : reports) {

                String sharedObjectPermissionId;

                if (Utility.isSqlServer(etk)) { // We have to do this separately
                    // because for some reason
                    // Oracle cannot handle
                    // executeForKey in this context
                    sharedObjectPermissionId = String
                            .valueOf(etk.createSQL("INSERT INTO etk_shared_object_permission(is_edit, is_execute, is_display, subject_id, role_id, is_all_users) VALUES (1, 1, 1, NULL, :roleId, 0)")
                                    .setParameter("roleId", administrationId)
                                    .executeForKey(
                                            "shared_object_permission_id"));
                } else {
                    sharedObjectPermissionId = etk.createSQL(
                            "SELECT HIBERNATE_SEQUENCE.NEXTVAL FROM DUAL")
                            .fetchString();
                    etk.createSQL("INSERT INTO etk_shared_object_permission(shared_object_permission_id, is_edit, is_execute, is_display, subject_id, role_id, is_all_users) VALUES (:sharedObjectPermissionId, 1, 1, 1, NULL, :roleId, 0)")
                    .setParameter("roleId", administrationId)
                    .setParameter("sharedObjectPermissionId",
                            sharedObjectPermissionId).execute();
                }

                etk.createSQL("INSERT INTO etk_report_permission(report_permission_id, saved_report_id) VALUES(:reportPermissionId, :savedReportId)")
                .setParameter("reportPermissionId",
                        sharedObjectPermissionId)
                        .setParameter("savedReportId",
                                report.get("SAVED_REPORT_ID")).execute();
            }

            return response;
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }

}

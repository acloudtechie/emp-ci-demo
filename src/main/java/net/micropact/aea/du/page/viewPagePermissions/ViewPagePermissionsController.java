package net.micropact.aea.du.page.viewPagePermissions;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * This controller is for viewing the page permissions in ways which are currently inconvenient within entellitrak.
 * @author zmiller
 */
public class ViewPagePermissionsController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

        final TextResponse response = etk.createTextResponse();

        response.put("roles", etk.createSQL("SELECT role.ROLE_ID, role.NAME FROM etk_role role ORDER BY NAME, ROLE_ID")
                .fetchJSON());

        response.put("users", etk.createSQL("SELECT u.USER_ID, u.USERNAME FROM etk_user u ORDER BY USERNAME, USER_ID")
                .fetchJSON());

        response.put("groups", etk.createSQL("SELECT g.GROUP_ID, g.GROUP_NAME FROM etk_group g ORDER BY GROUP_NAME, GROUP_ID")
                .fetchJSON());

        response.put("pages", etk.createSQL("SELECT PAGE_ID, NAME FROM etk_page ORDER BY NAME, PAGE_ID")
                .fetchJSON());

        response.put("pagePermissions", etk.createSQL("SELECT pp.PAGE_ID, sop.IS_EDIT, sop.IS_EXECUTE, sop.IS_DISPLAY, sop.ROLE_ID, sop.SUBJECT_ID, sop.IS_ALL_USERS FROM etk_page_permission pp JOIN etk_shared_object_permission sop ON sop.shared_object_permission_id = pp.page_permission_id")
                .fetchJSON());

        return response;
    }
}

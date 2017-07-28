package net.micropact.aea.rf.page.ajaxGetScriptObject;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * Controller code for a page which fetches information about a particular script object given its fully qualified name.
 *
 * @author zachary.miller
 */
public class AjaxGetScriptObjectController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            final String fullyQualifiedName = etk.getParameters().getSingle("fullyQualifiedName");

            response.setContentType(ContentType.JSON);

            final long workspaceId = Utility.getWorkspaceId(etk, etk.getCurrentUser().getId());

            response.put("out", JsonUtilities.encode(etk.createSQL("SELECT SCRIPT_ID, FULLY_QUALIFIED_SCRIPT_NAME FROM aea_script_pkg_view WHERE fully_qualified_script_name = :fullyQualifiedName AND ( tracking_config_id = :trackingConfigId OR tracking_config_id IS NULL ) AND workspace_id = :workspaceId")
                    .setParameter("workspaceId", workspaceId)
                    .setParameter("fullyQualifiedName", fullyQualifiedName)
                    .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                    .fetchMap()));

            return response;
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}

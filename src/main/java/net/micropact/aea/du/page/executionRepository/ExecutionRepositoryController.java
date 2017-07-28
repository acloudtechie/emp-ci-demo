package net.micropact.aea.du.page.executionRepository;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.JsonUtilities;

/**
 * This is for a {@link PageController} which displays information about users who are not using the System Repository
 * as their Execution Repository.
 *
 * @author zmiller
 */
public class ExecutionRepositoryController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        final TextResponse response = etk.createTextResponse();
        try {
            response.put("users", etk.createSQL("SELECT u.USER_ID, u.USERNAME, u.TYPE_OF_USER, workspace.WORKSPACE_REVISION FROM etk_user u JOIN etk_workspace workspace ON workspace.user_id = u.user_id JOIN etk_development_preferences developmentPreferences ON developmentPreferences.development_preferences_id = u.development_preferences_id WHERE developmentPreferences.use_system_workspace = 0 ORDER BY username, user_id")
                    .fetchJSON());

            response.put("systemWorkspace", JsonUtilities.encode(etk.createSQL("SELECT WORKSPACE_REVISION FROM etk_workspace WHERE user_id IS NULL")
                    .fetchMap()));

            return response;
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}

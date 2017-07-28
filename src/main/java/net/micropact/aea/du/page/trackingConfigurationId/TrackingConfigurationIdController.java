package net.micropact.aea.du.page.trackingConfigurationId;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.JsonUtilities;

/**
 * This page is for displaying the queries for the current and next tracking configuration ids.
 * When developing frameworks it is very common to need one of these ids because you need them to be able to interact
 * with most ETK_ tables.
 * The currently deployed id is the id that users experience and is what is reflect in all tabs except the
 * configuration tab.
 * The next deployed id is the id that is shown in the configuration tab and will become the currently deployed id
 * once somebody Applies Changes
 *
 * @author zmiller
 */
public class TrackingConfigurationIdController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            final String currentlyDeployedQuery = "SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive";
            final String nextDeployedQuery = "SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = (SELECT MAX(config_version) FROM etk_tracking_config)";

            final String currentlyDeployedVersion = etk.createSQL(currentlyDeployedQuery).fetchString();
            final String nextDeployedVersion = etk.createSQL(nextDeployedQuery).fetchString();

            response.put("currentlyDeployedQuery", JsonUtilities.encode(currentlyDeployedQuery));
            response.put("nextDeployedQuery", JsonUtilities.encode(nextDeployedQuery));
            response.put("currentlyDeployedVersion", JsonUtilities.encode(currentlyDeployedVersion));
            response.put("nextDeployedVersion", JsonUtilities.encode(nextDeployedVersion));

            return response;
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }
}

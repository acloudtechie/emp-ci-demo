package net.micropact.aea.du.page.duplicatePageDashboardOptions;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * This serves as the Controller Code for a Page which displays duplicate Page Dashboard Options.
 *
 * @author zachary.miller
 */
public class DuplicatePageDashboardOptionsController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        response.put("duplicateRecords", etk.createSQL("SELECT u.username USERNAME, page.BUSINESS_KEY, COUNT(*) COUNT FROM etk_page_dashboard_option dashboardOption LEFT JOIN etk_user u ON u.user_id = dashboardOption.user_id LEFT JOIN etk_page page ON page.page_id = dashboardOption.page_id GROUP BY USERNAME, BUSINESS_KEY HAVING COUNT(*) > 1 ORDER BY COUNT, USERNAME, BUSINESS_KEY")
                .fetchJSON());

        return response;
    }
}

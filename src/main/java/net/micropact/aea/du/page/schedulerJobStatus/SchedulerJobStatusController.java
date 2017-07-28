package net.micropact.aea.du.page.schedulerJobStatus;

import java.util.List;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.SchedulerJobUtility;
import net.micropact.aea.utility.SchedulerJobUtility.SchedulerJobError;

/**
 * This controller is to be used in the page which indicates whether there appear to be any kind
 * Scheduler Job errors or oddities.
 *
 * @author zmiller
 */
public class SchedulerJobStatusController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        final List<SchedulerJobError> schedulerJobErrors = SchedulerJobUtility.getLikelySchedulerJobErrors(etk);

        response.put("errorsExist", schedulerJobErrors.size() > 0);
        response.put("schedulerJobErrorsTable", SchedulerJobUtility.convertErrorsToHTML(schedulerJobErrors, true));

        return response;
    }
}

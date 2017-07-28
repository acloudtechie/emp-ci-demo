package net.micropact.aea.rf.page.cleanWorkflow;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.rf.utility.RfUtility;

/**
 * This page is simply a wrapper around the {@link RfUtility#cleanRfWorkflow(com.entellitrak.ExecutionContext)}
 * which cleans foreign keys related to RF Workflow objects.
 *
 * @author zmiller
 * @see RfUtility#cleanRfWorkflow(com.entellitrak.ExecutionContext)
 */
public class CleanWorkflowController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

        try {
            final TextResponse response = etk.createTextResponse();

            RfUtility.cleanRfWorkflow(etk);
            return response;
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }
}

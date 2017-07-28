package net.micropact.aea.rf.page.viewWorkflowGraph;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * This page should be embedded within the rf.page.viewWorkflow page and is responsible for actually
 * displaying the graph.
 *
 * @author zmiller
 * @see net.micropact.aea.rf.page.viewWorkflow.ViewWorkflowController
 */
public class ViewWorkflowGraphController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        final String rfWorkflowId = etk.getParameters().getSingle("rfWorkflowId");

        response.put("rfWorkflowId", rfWorkflowId);

        return response;
    }
}

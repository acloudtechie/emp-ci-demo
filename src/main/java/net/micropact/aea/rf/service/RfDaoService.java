package net.micropact.aea.rf.service;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

import net.entellitrak.aea.rf.dao.IRfWorkflow;
import net.entellitrak.aea.rf.service.IRfDaoService;

/**
 * Implementation of {@link IRfDaoService} of the public API.
 *
 * @author zachary.miller
 */
public class RfDaoService implements IRfDaoService{

    private final ExecutionContext etk;

    /**
     * {@link RfDaoService} execution context.
     *
     * @param executionContext entellitrak execution context
     */
    public RfDaoService(final ExecutionContext executionContext) {
        etk = executionContext;
    }

    @Override
    public IRfWorkflow loadRfWorkflowByCode(final String workflowCode) throws IncorrectResultSizeDataAccessException {
        return new RfDaoServicePrivate(etk).loadRfRfWorkflowByCode(workflowCode);
    }
}

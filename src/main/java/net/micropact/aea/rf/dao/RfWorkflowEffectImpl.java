package net.micropact.aea.rf.dao;

import java.util.List;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.dynamic.RfWorkflowEffect;

import net.entellitrak.aea.rf.ICustomParameters;
import net.entellitrak.aea.rf.dao.IRfScript;
import net.entellitrak.aea.rf.dao.IRfTransition;
import net.entellitrak.aea.rf.dao.IRfWorkflowEffect;
import net.micropact.aea.rf.service.RfDaoServicePrivate;

/**
 * Simple implementation of {@link IRfWorkflowEffect}.
 *
 * @author zachary.miller
 */
public class RfWorkflowEffectImpl implements IRfWorkflowEffect {

    private final RfDaoServicePrivate daoService;
    private final long trackingId;

    private final String name;
    private final String code;
    private final long executionOrder;
    private IRfScript rfScript;
    private final long rfScriptId;
    private List<IRfTransition> transitions;

    /**
     * A simple constructor.
     *
     * @param etk entellitrak execution context
     * @param rfDaoService service for lazily loading related objects
     * @param workflowEffectId tracking id of the RF Workflow Effect
     */
    public RfWorkflowEffectImpl(final ExecutionContext etk, final RfDaoServicePrivate rfDaoService, final long workflowEffectId) {
        daoService = rfDaoService;
        trackingId = workflowEffectId;

        final RfWorkflowEffect rfWorkflowEffect = etk.getDynamicObjectService().get(RfWorkflowEffect.class, workflowEffectId);

        name = rfWorkflowEffect.getName();
        code = rfWorkflowEffect.getCode();
        executionOrder = rfWorkflowEffect.getExecutionOrder();
        rfScriptId = rfWorkflowEffect.getScript();
    }

    @Override
    public long getId(){
        return trackingId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public long getExecutionOrder() {
        return executionOrder;
    }

    @Override
    public IRfScript getScript() throws IncorrectResultSizeDataAccessException {
        if(rfScript == null){
            rfScript = daoService.loadRfScriptById(rfScriptId);
        }
        return rfScript;
    }

    @Override
    public List<IRfTransition> getTransitions() throws IncorrectResultSizeDataAccessException {
        if(transitions == null){
            transitions = daoService.loadTransitionsByRfWorkflowEffect(trackingId);
        }
        return transitions;
    }

    @Override
    public ICustomParameters getParameters() {
        return daoService.loadCustomParametersByRfWorkflowEffect(trackingId);
    }
}

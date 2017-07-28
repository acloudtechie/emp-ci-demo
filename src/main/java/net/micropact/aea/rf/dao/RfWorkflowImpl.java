package net.micropact.aea.rf.dao;

import java.util.List;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.configuration.DataElement;
import com.entellitrak.configuration.DataElementService;
import com.entellitrak.dynamic.RfWorkflow;

import net.entellitrak.aea.rf.dao.IRfState;
import net.entellitrak.aea.rf.dao.IRfTransition;
import net.entellitrak.aea.rf.dao.IRfWorkflow;
import net.entellitrak.aea.rf.dao.IRfWorkflowEffect;
import net.entellitrak.aea.rf.dao.IRfWorkflowParameter;
import net.micropact.aea.rf.service.RfDaoServicePrivate;

/**
 * Simple implementation of {@link IRfWorkflow}.
 *
 * @author zachary.miller
 */
public class RfWorkflowImpl implements IRfWorkflow {

    private final RfDaoServicePrivate daoService;

    private final long trackingId;
    private final String name;
    private final String code;
    private final DataElement childTransitionElement;
    private final DataElement parentStateElement;
    private final String description;

    private List<IRfState> states;
    private List<IRfTransition> transitions;
    private List<IRfWorkflowEffect> workflowEffects;
    private List<IRfWorkflowParameter> workflowParameters;

    /**
     * A Simple Constructor.
     *
     * @param etk entellitrak execution context
     * @param rfDaoService service for lazily loading related objects
     * @param rfWorkflowId tracking id of the RF Workflow
     */
    public RfWorkflowImpl(final ExecutionContext etk, final RfDaoServicePrivate rfDaoService, final long rfWorkflowId) {
        final DataElementService dataElementService = etk.getDataElementService();

        daoService = rfDaoService;
        trackingId = rfWorkflowId;

        final RfWorkflow rfWorkflow = etk.getDynamicObjectService().get(RfWorkflow.class, rfWorkflowId);

        name = rfWorkflow.getName();
        code = rfWorkflow.getCode();
        description = rfWorkflow.getDescription();
        childTransitionElement = dataElementService.getDataElementByBusinessKey(rfWorkflow.getChildTransitionElement());
        parentStateElement = dataElementService.getDataElementByBusinessKey(rfWorkflow.getParentStateElement());
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
    public DataElement getChildTransitionElement() {
        return childTransitionElement;
    }

    @Override
    public DataElement getParentStateElement() {
        return parentStateElement;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<IRfState> getStates() throws IncorrectResultSizeDataAccessException {
        if(states == null){
            states = daoService.loadRfStatesByRfWorkflowId(trackingId);
        }
        return states;
    }

    @Override
    public List<IRfTransition> getTransitions() throws IncorrectResultSizeDataAccessException {
        if(transitions == null){
            transitions = daoService.loadRfTransitionsByRfWorkflow(trackingId);
        }
        return transitions;
    }

    @Override
    public List<IRfWorkflowEffect> getWorkflowEffects() throws IncorrectResultSizeDataAccessException {
        if(workflowEffects == null){
            workflowEffects = daoService.loadWorkflowEffectsByRfWorkflow(trackingId);
        }
        return workflowEffects;
    }

    @Override
    public List<IRfWorkflowParameter> getWorkflowParameters() throws IncorrectResultSizeDataAccessException {
        if(workflowParameters == null){
            workflowParameters = daoService.loadRfWorkflowParametersByRfWorkflow(trackingId);
        }
        return workflowParameters;
    }
}

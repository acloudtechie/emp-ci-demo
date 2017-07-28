package net.micropact.aea.rf;

import java.util.Map;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.rf.IDefaultParameters;
import net.entellitrak.aea.rf.RfServiceFactory;
import net.entellitrak.aea.rf.dao.IRfState;
import net.entellitrak.aea.rf.dao.IRfTransition;
import net.entellitrak.aea.rf.dao.IRfWorkflow;
import net.micropact.aea.rf.service.RfDaoServicePrivate;
import net.micropact.aea.utility.Utility;

/**
 * This class contains the private implementation of the public {@link IDefaultParameters} interface.
 * @author zmiller
 */
public final class DefaultParameters implements IDefaultParameters {

    private final String workflowCode;
    private final long childTrackingId;
    private final long parentTrackingId;
    private final Long toStateId;
    private final String toStateCode;
    private final Long fromStateId;
    private final String fromStateCode;
    private final long transitionId;
    private final String transitionCode;
    private final IRfWorkflow rfWorkflow;
    private final IRfTransition rfTransition;
    private final IRfState fromState;
    private final IRfState toState;
    private final IRfState nextState;

    /**
     * Constructor for default parameters.
     *
     * @param etk entellitrak execution context
     * @param rfWorkflowCode The Code of the RF Workflow that the parameters should be generated for
     * @param childObjectTrackingId The tracking id of the workflow's child object to generate the parameters for
     * @param workflowFromStateId The tracking id of the RF State that the parent object is coming from
     * @param workflowTransitionId The tracking id of the RF Transition which is being taken.
     * @throws Exception If there is any underlying exception
     */
    public DefaultParameters(final ExecutionContext etk,
            final String rfWorkflowCode,
            final long childObjectTrackingId,
            final Long workflowFromStateId,
            final long workflowTransitionId)
                    throws Exception {

        workflowCode = rfWorkflowCode;
        childTrackingId = childObjectTrackingId;

        rfWorkflow = RfServiceFactory.getRfDaoService(etk).loadRfWorkflowByCode(workflowCode);

        final Map<String, Object> info = etk.createSQL(String.format(Utility.isSqlServer(etk) ? "SELECT (SELECT id_parent FROM %s WHERE id = :trackingId) PARENTTRACKINGID, (SELECT toState.c_code FROM t_rf_transition transition JOIN t_rf_state toState ON toState.id = transition.c_to_state WHERE transition.id = :transitionId) TOSTATECODE, (SELECT transition.c_to_state FROM t_rf_transition transition WHERE transition.id = :transitionId) TOSTATEID, (SELECT c_code FROM t_rf_state WHERE id = :fromStateId) FROMSTATECODE, (SELECT c_code FROM t_rf_transition WHERE id = :transitionId) TRANSITIONCODE"
                                                                                                : "SELECT (SELECT id_parent FROM %s WHERE id = :trackingId) PARENTTRACKINGID, (SELECT toState.c_code FROM t_rf_transition transition JOIN t_rf_state toState ON toState.id = transition.c_to_state WHERE transition.id = :transitionId) TOSTATECODE, (SELECT transition.c_to_state FROM t_rf_transition transition WHERE transition.id = :transitionId) TOSTATEID, (SELECT c_code FROM t_rf_state WHERE id = :fromStateId) FROMSTATECODE, (SELECT c_code FROM t_rf_transition WHERE id = :transitionId) TRANSITIONCODE FROM DUAL",
                                                                                                getChildTable()))
                .setParameter("trackingId", childObjectTrackingId)
                .setParameter("fromStateId", workflowFromStateId)
                .setParameter("transitionId", workflowTransitionId)
                .fetchMap();

        parentTrackingId = ((Number) info.get("PARENTTRACKINGID")).longValue();
        toStateId = info.get("TOSTATEID") != null ? ((Number) info.get("TOSTATEID")).longValue() : null ;
        toStateCode = (String) info.get("TOSTATECODE");
        fromStateId = workflowFromStateId;
        fromStateCode = (String) info.get("FROMSTATECODE");
        transitionId = workflowTransitionId;
        transitionCode = (String) info.get("TRANSITIONCODE");

        final RfDaoServicePrivate daoServicePrivate = new RfDaoServicePrivate(etk);
        rfTransition = daoServicePrivate.loadRfTransitionById(transitionId);
        fromState = fromStateId == null ? null : daoServicePrivate.loadRfStateById(fromStateId);
        toState = toStateId == null ? null : daoServicePrivate.loadRfStateById(toStateId);
        nextState = Utility.nvl(rfTransition.getToState(), fromState);
    }

    @Override
    public String getWorkflowCode(){
        return workflowCode;
    }

    @Override
    public long getChildTrackingId(){
        return childTrackingId;
    }

    @Override
    public long getParentTrackingId(){
        return parentTrackingId;
    }

    @Override
    public String getChildTable(){
        return rfWorkflow.getChildTransitionElement().getDataObject().getTableName();
    }

    @Override
    public String getTransitionColumn(){
        return rfWorkflow.getChildTransitionElement().getColumnName();
    }

    @Override
    public String getParentTable(){
        return rfWorkflow.getParentStateElement().getDataObject().getTableName();

    }

    @Override
    public String getParentStateColumn(){
        return rfWorkflow.getParentStateElement().getColumnName();
    }

    @Override
    public String getTransitionCode() {
        return transitionCode;
    }

    @Override
    public long getTransitionId() {
        return transitionId;
    }

    @Override
    public String getFromStateCode() {
        return fromStateCode;
    }

    @Override
    public Long getFromStateId() {
        return fromStateId;
    }

    @Override
    public String getToStateCode() {
        return toStateCode;
    }

    @Override
    public Long getToStateId() {
        return toStateId;
    }

    @Override
    public long getNextStateId() {
        return Utility.nvl(getToStateId(), getFromStateId());
    }

    @Override
    public String getNextStateCode() {
        return Utility.nvl(getToStateCode(), getFromStateCode());
    }

    @Override
    public IRfWorkflow getRfWorkflow() {
        return rfWorkflow;
    }

    @Override
    public IRfTransition getRfTransition() {
        return rfTransition;
    }

    @Override
    public IRfState getFromState() {
        return fromState;
    }

    @Override
    public IRfState getToState() {
        return toState;
    }

    @Override
    public IRfState getNextState() {
        return nextState;
    }
}

package net.micropact.aea.rf.dao;

import java.util.Date;
import java.util.List;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.dynamic.RfTransition;
import com.entellitrak.user.Role;

import net.entellitrak.aea.rf.ITransitionParameters;
import net.entellitrak.aea.rf.dao.IRfState;
import net.entellitrak.aea.rf.dao.IRfTransition;
import net.entellitrak.aea.rf.dao.IRfWorkflowEffect;
import net.micropact.aea.core.query.Coersion;
import net.micropact.aea.rf.service.RfDaoServicePrivate;

/**
 * Simple implementation of {@link IRfTransition}.
 *
 * @author zachary.miller
 */
public class RfTransitionImpl implements IRfTransition {

    private final RfDaoServicePrivate daoService;
    private final long trackingId;
    private final String name;
    private final String code;
    private final String description;
    private final Date endDate;
    private final boolean initialTransition;
    private final Long order;
    private final Date startDate;

    private final Long toStateId;
    private IRfState toState;

    private List<IRfState> fromStates;

    private List<Role> roles;

    private List<IRfWorkflowEffect> workflowEffects;

    /**
     * A simple constructor.
     *
     * @param etk entellitrak execution context
     * @param rfDaoService service for lazily loading related objects
     * @param transitionId tracking id of the RF Transition
     */
    public RfTransitionImpl(final ExecutionContext etk, final RfDaoServicePrivate rfDaoService, final long transitionId) {
        daoService = rfDaoService;
        trackingId = transitionId;

        final RfTransition rfTransition = etk.getDynamicObjectService().get(RfTransition.class, transitionId);

        name = rfTransition.getName();
        code = rfTransition.getCode();
        description = rfTransition.getDescription();
        endDate = rfTransition.getEndDate();
        initialTransition = Coersion.toBooleanNonNull(rfTransition.getInitialTransition());
        order = Coersion.toLong(rfTransition.getOrder());
        startDate = rfTransition.getStartDate();
        toStateId = Coersion.toLong(rfTransition.getToState());
    }

    @Override
    public long getId() {
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
    public String getDescription() {
        return description;
    }

    @Override
    public IRfState getToState() throws IncorrectResultSizeDataAccessException {
        if(toState == null && toStateId != null){
            toState = daoService.loadRfStateById(toStateId);
        }
        return toState;
    }

    @Override
    public List<IRfState> getFromStates() throws IncorrectResultSizeDataAccessException {
        if(fromStates == null){
            fromStates = daoService.loadRfStatesByTransitionFromState(trackingId);
        }
        return fromStates;
    }

    @Override
    public List<Role> getRoles() {
        if(roles == null){
            roles = daoService.loadRolesByTransition(trackingId);
        }
        return roles;
    }

    @Override
    public boolean isInitialTransition() {
        return initialTransition;
    }

    @Override
    public Long getOrder() {
        return order;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public List<IRfWorkflowEffect> getWorkflowEffects() throws IncorrectResultSizeDataAccessException {
        if(workflowEffects == null){
            workflowEffects = daoService.loadRfWorkflowEffectsByTransition(trackingId);
        }
        return workflowEffects;
    }

    @Override
    public ITransitionParameters getTransitionParameters() {
        return daoService.loadTransitionParametersByTransitionId(trackingId);
    }
}

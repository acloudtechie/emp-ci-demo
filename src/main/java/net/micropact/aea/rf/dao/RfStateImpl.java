package net.micropact.aea.rf.dao;

import java.util.Date;
import java.util.List;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.dynamic.RfState;

import net.entellitrak.aea.rf.dao.IRfState;
import net.entellitrak.aea.rf.dao.IRfTransition;
import net.micropact.aea.core.query.Coersion;
import net.micropact.aea.rf.service.RfDaoServicePrivate;

/**
 * Simple implementation of {@link IRfState}.
 *
 * @author zachary.miller
 */
public class RfStateImpl implements IRfState {
    private final RfDaoServicePrivate daoService;
    private final long trackingId;

    private final String name;
    private final String code;
    private final String description;
    private final Date startDate;
    private final Date endDate;
    private final Long order;
    private List<IRfTransition> allowedTransitions;

    /**
     * A simple constructor.
     *
     * @param etk entellitrak execution context
     * @param rfDaoService service for lazily loading related objects
     * @param stateId tracking id of the RF State object
     */
    public RfStateImpl(final ExecutionContext etk, final RfDaoServicePrivate rfDaoService, final long stateId) {
        daoService = rfDaoService;
        trackingId = stateId;

        final RfState rfState = etk.getDynamicObjectService().get(RfState.class, stateId);

        name = rfState.getName();
        code = rfState.getCode();
        description = rfState.getDescription();
        startDate = rfState.getStartDate();
        endDate = rfState.getEndDate();
        order = Coersion.toLong(rfState.getOrder());
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
    public String getDescription() {
        return description;
    }

    @Override
    public Long getOrder() {
        return order;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public List<IRfTransition> getAllowedTransitions() throws IncorrectResultSizeDataAccessException {
        if(allowedTransitions == null){
            allowedTransitions = daoService.loadRfTransitionsByRfStateAllowedId(trackingId);
        }
        return allowedTransitions;
    }
}

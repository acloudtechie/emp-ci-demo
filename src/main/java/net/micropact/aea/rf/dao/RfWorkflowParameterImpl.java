package net.micropact.aea.rf.dao;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.dynamic.RfWorkflowParameter;

import net.entellitrak.aea.rf.dao.IRfLookup;
import net.entellitrak.aea.rf.dao.IRfParameterType;
import net.entellitrak.aea.rf.dao.IRfWorkflowParameter;
import net.micropact.aea.core.query.Coersion;
import net.micropact.aea.rf.service.RfDaoServicePrivate;

/**
 * Simple implementation of {@link IRfWorkflowParameter}.
 *
 * @author zachary.miller
 */
public class RfWorkflowParameterImpl implements IRfWorkflowParameter {
    private final RfDaoServicePrivate daoService;

    private final long trackingId;
    private final String name;
    private IRfParameterType parameterType;
    private final long parameterTypeId;
    private IRfLookup rfLookup;
    private final Long rfLookupId;
    private final boolean required;
    private final boolean allowMultiple;
    private final String code;
    private final Long order;
    private final String description;

    /**
     * A Simple Constructor.
     *
     * @param etk entellitrak execution context
     * @param rfDaoService {@link RfDaoServicePrivate} for lazily loading other objects
     * @param rfWorkflowParameterId tracking id of the RF Workflow Parameter
     */
    public RfWorkflowParameterImpl(final ExecutionContext etk, final RfDaoServicePrivate rfDaoService, final long rfWorkflowParameterId) {
        daoService = rfDaoService;
        final RfWorkflowParameter rfWorkflowParameter = etk.getDynamicObjectService().get(RfWorkflowParameter.class, rfWorkflowParameterId);

        trackingId = rfWorkflowParameterId;
        name = rfWorkflowParameter.getName();
        parameterTypeId = rfWorkflowParameter.getType();
        rfLookupId = Coersion.toLong(rfWorkflowParameter.getLookup());
        required = Coersion.toBooleanNonNull(rfWorkflowParameter.getRequired());
        allowMultiple = Coersion.toBooleanNonNull(rfWorkflowParameter.getAllowMultiple());
        code = rfWorkflowParameter.getCode();
        order = Coersion.toLong(rfWorkflowParameter.getOrder());
        description = rfWorkflowParameter.getDescription();
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
    public IRfParameterType getParameterType() throws IncorrectResultSizeDataAccessException {
        if(parameterType == null){
            parameterType = daoService.loadRfParameterTypeById(parameterTypeId);
        }
        return parameterType;
    }

    @Override
    public IRfLookup getRfLookup() throws IncorrectResultSizeDataAccessException {
        if(rfLookup == null && rfLookupId != null){
            rfLookup = daoService.loadRfLookupById(rfLookupId);
        }
        return rfLookup;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public boolean isAllowMultiple() {
        return allowMultiple;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public Long getOrder() {
        return order;
    }

    @Override
    public String getDescription() {
        return description;
    }
}

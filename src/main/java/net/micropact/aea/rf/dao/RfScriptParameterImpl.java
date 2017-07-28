package net.micropact.aea.rf.dao;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.dynamic.RfScriptParameter;

import net.entellitrak.aea.rf.dao.IRfLookup;
import net.entellitrak.aea.rf.dao.IRfParameterType;
import net.entellitrak.aea.rf.dao.IRfScriptParameter;
import net.micropact.aea.core.query.Coersion;
import net.micropact.aea.rf.service.RfDaoServicePrivate;

/**
 * Simple implementation of {@link IRfScriptParameter}.
 *
 * @author zachary.miller
 */
public class RfScriptParameterImpl implements IRfScriptParameter {
    private final RfDaoServicePrivate rfDaoService;

    private final long trackingId;
    private final String name;
    private final boolean required;
    private final boolean allowMultiple;
    private final String code;
    private final Long order;
    private final String description;

    private final long rfParameterTypeId;
    private IRfParameterType rfParameterType;

    private final Long rfLookupId;
    private IRfLookup rfLookup;

    /**
     * A simple constructor.
     *
     * @param etk entellitrak execution context
     * @param daoService service for lazily loading related objects
     * @param scriptParameterId tracking id of the RF Script Parameter
     */
    public RfScriptParameterImpl(final ExecutionContext etk, final RfDaoServicePrivate daoService, final long scriptParameterId) {
        rfDaoService = daoService;

        final RfScriptParameter rfScriptParameter = etk.getDynamicObjectService().get(RfScriptParameter.class, scriptParameterId);

        trackingId = scriptParameterId;
        name = rfScriptParameter.getName();
        required = Coersion.toBooleanNonNull(rfScriptParameter.getRequired());
        allowMultiple = Coersion.toBooleanNonNull(rfScriptParameter.getAllowMultiple());
        code = rfScriptParameter.getCode();
        order = Coersion.toLong(rfScriptParameter.getOrder());
        description = rfScriptParameter.getDescription();
        rfParameterTypeId = Coersion.toLong(rfScriptParameter.getType());
        rfLookupId = Coersion.toLong(rfScriptParameter.getLookup());
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
    public IRfParameterType getType() throws IncorrectResultSizeDataAccessException {
        if(rfParameterType == null){
            rfParameterType = rfDaoService.loadRfParameterTypeById(rfParameterTypeId);
        }
        return rfParameterType;
    }

    @Override
    public IRfLookup getLookup() throws IncorrectResultSizeDataAccessException {
        if(rfLookup == null && rfLookupId != null){
            rfLookup = rfDaoService.loadRfLookupById(rfLookupId);
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

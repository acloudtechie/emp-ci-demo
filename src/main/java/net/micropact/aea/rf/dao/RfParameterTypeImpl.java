package net.micropact.aea.rf.dao;

import com.entellitrak.ExecutionContext;
import com.entellitrak.dynamic.RfParameterType;

import net.entellitrak.aea.rf.dao.IRfParameterType;

/**
 * Simple implementation of {@link IRfParameterType}.
 *
 * @author zachary.miller
 */
public class RfParameterTypeImpl implements IRfParameterType {

    private final long trackingId;
    private final String name;
    private final String code;
    private final long order;

    /**
     * A simple constructor.
     *
     * @param etk entellitrak execution context
     * @param rfParameterTypeId tracking id of the RF Parameter Type
     */
    public RfParameterTypeImpl(final ExecutionContext etk, final long rfParameterTypeId) {

        final RfParameterType rfParameterType = etk.getDynamicObjectService().get(RfParameterType.class, rfParameterTypeId);

        trackingId = rfParameterTypeId;
        name = rfParameterType.getName();
        code = rfParameterType.getCode();
        order = rfParameterType.getOrder();
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
    public long getOrder() {
        return order;
    }

}

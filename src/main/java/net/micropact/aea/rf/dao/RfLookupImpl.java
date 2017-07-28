package net.micropact.aea.rf.dao;

import java.util.Date;

import com.entellitrak.ExecutionContext;
import com.entellitrak.dynamic.RfLookup;

import net.entellitrak.aea.rf.dao.IRfLookup;

/**
 * Simple Implementation of {@link IRfLookup}.
 *
 * @author zachary.miller
 */
public class RfLookupImpl implements IRfLookup {

    private final long trackingId;
    private final String name;
    private final String code;
    private final Date startDate;
    private final Date endDate;
    private final String sql;

    /**
     * Constructor for an RF Lookup specified by tracking id.
     *
     * @param etk entellitrak execution context
     * @param rfLookupId tracking id of the RF Lookup
     */
    public RfLookupImpl(final ExecutionContext etk, final Long rfLookupId) {
        final RfLookup rfLookup = etk.getDynamicObjectService().get(RfLookup.class, rfLookupId);

        trackingId = rfLookupId;
        name = rfLookup.getName();
        code = rfLookup.getCode();
        startDate = rfLookup.getStartDate();
        endDate = rfLookup.getEndDate();
        sql = rfLookup.getSql();
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
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public String getSQL() {
        return sql;
    }
}

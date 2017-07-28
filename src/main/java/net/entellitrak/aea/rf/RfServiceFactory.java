package net.entellitrak.aea.rf;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.rf.service.IRfDaoService;
import net.entellitrak.aea.rf.service.IRfMigrationService;
import net.micropact.aea.rf.service.RfDaoService;
import net.micropact.aea.rf.service.RfMigrationService;

/**
 * This class is the entry point for getting service classes related to the Rules Framework.
 *
 * @author zachary.miller
 */
public final class RfServiceFactory{

    /**
     * Utility classes do not need constructors.
     */
    private RfServiceFactory() {}

    /**
     * Gets an instance of a Rules Framework DAO Service.
     *
     * @param etk entellitrak execution context
     * @return The RF DAO Service
     */
    public static IRfDaoService getRfDaoService(final ExecutionContext etk){
        return new RfDaoService(etk);
    }

    /**
     * Gets an instance of a Rules Framework Migration Service.
     *
     * @param etk entellitrak execution context
     * @return The RF Migration Service
     */
    public static IRfMigrationService getRfMigrationService(final ExecutionContext etk){
        return new RfMigrationService(etk);
    }
}

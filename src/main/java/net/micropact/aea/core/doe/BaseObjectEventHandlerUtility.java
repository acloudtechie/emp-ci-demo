package net.micropact.aea.core.doe;

import com.entellitrak.ApplicationException;
import com.entellitrak.BaseObjectEventContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

import net.micropact.aea.core.validation.ValidationUtility;

/**
 * Utility class containing common functionality for object event handlers.
 *
 * @author Zachary.Miller
 */
public final class BaseObjectEventHandlerUtility {

    /**
     * Utility classes do not need public constructors.
     */
    private BaseObjectEventHandlerUtility(){}

    /**
     * Executes common code which should fire for all objects.
     *
     * @param etk entellitrak execution context
     * @throws ApplicationException If there is an underlying {@link ApplicationException}
     */
    public static void executeCommonCode(final BaseObjectEventContext etk) throws ApplicationException{
        try {
            ValidationUtility.ensureUniqueCodePerParent(etk);
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}

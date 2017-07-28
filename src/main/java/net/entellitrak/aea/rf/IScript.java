package net.entellitrak.aea.rf;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.exception.RulesFrameworkException;

/**
 * <p>
 *  This interface is for effects which are to be called from the Rules Framework.
 *  When a Rules Framework transition is triggered the Workflow Effects
 *  will be called passing in all necessary parameters.
 *  Once you implement this interface, you must create a new "RF Script"
 *  Base Tracked Object and indicate what parameters that Script takes.
 * </p>
 *
 * @author zmiller
 */
public interface IScript {

    /**
     * This is the method that will be called when the Workflow Effect is triggered.
     *
     * @param etk The context that the effects will occur in
     * @param parameters The parameters which the effect will have access to.
     * It includes both default and custom parameters
     * @throws RulesFrameworkException If any problem occurs while executing the effect
     */
    void doEffect(ExecutionContext etk, IRulesFrameworkParameters parameters) throws RulesFrameworkException;
}

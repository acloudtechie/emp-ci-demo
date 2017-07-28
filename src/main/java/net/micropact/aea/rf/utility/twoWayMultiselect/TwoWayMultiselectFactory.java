package net.micropact.aea.rf.utility.twoWayMultiselect;

import com.entellitrak.ExecutionContext;

/**
 * This class produces the necessary implementations of {@link ITwoWayMultiselect}.
 *
 * @author zmiller
 */
public final class TwoWayMultiselectFactory {

    /**
     * Utility classes do not need constructors.
     */
    private TwoWayMultiselectFactory(){}

    /**
     * Get an {@link ITwoWayMultiselect} which represents the RF State - Allowed Transitions multiselect.
     *
     * @param etk entellitrak execution context
     * @return The information for the RF State - Allowed Transitions multiselect.
     */
    public static ITwoWayMultiselect getRfStateAllowedTransitionsMultiselect(final ExecutionContext etk){
        return new GenericTwoWayMultiselect(etk, "object.rfTransition.element.fromStates", "allowedTransitions");
    }

    /**
     * Get an {@link ITwoWayMultiselect} which represents the RF Transition - Effects multiselect.
     *
     * @param etk entellitrak execution context
     * @return The information for the RF Transition - Effects multiselect.
     */
    public static ITwoWayMultiselect getRfTransitionEffects(final ExecutionContext etk){
        return new GenericTwoWayMultiselect(etk, "object.rfWorkflowEffect.element.transitions", "workflowEffects");
    }
}

package net.entellitrak.aea.rf;

/**
 * <p>
 *  This interface represents the parameters which get passed into RF Workflow Effects when they are triggered.
 *  It includes the 'default' parameters which get passed into every effect, as well as
 *  the 'custom' parameters.
 *  which are specific to that effect.
 * </p>
 *
 * @author zmiller
 */
public interface IRulesFrameworkParameters {

    /**
     * This method returns the default parameters which are available for every effect.
     * It includes information such as the trackingId of the parent and child objects as well as meta-data such as
     * the database table of the child.
     *
     * @return The default parameters common among all effects for a workflow
     */
    IDefaultParameters getDefaultParameters();

    /**
     * This returns the custom parameters for a particular Workflow Effect.
     *
     * @return The custom parameters for this particular effect
     */
    ICustomParameters getCustomParameters();

    /**
     * Gets the RF Transition Parameters which are configured for the RF Transition which is triggering the execution
     * of this Effect.
     *
     * @return The parameters specific to this particular transition.
     * */
    ITransitionParameters getTransitionParameters();
}

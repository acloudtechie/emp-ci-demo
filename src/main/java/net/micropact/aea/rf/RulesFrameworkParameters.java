package net.micropact.aea.rf;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.rf.ICustomParameters;
import net.entellitrak.aea.rf.IDefaultParameters;
import net.entellitrak.aea.rf.IRulesFrameworkParameters;
import net.entellitrak.aea.rf.ITransitionParameters;

/**
 * This class contains the private implementation of the public {@link IRulesFrameworkParameters} interface.
 * @author zmiller
 */
public class RulesFrameworkParameters implements IRulesFrameworkParameters {

    private final IDefaultParameters defaultParameters;
    private final ICustomParameters customParameters;
    private final ITransitionParameters transitionParameters;

    /**
     * A simple constructor.
     *
     * @param etk entellitrak execution context
     * @param workflowDefaultParameters the default parameters to use.
     * @param workflowTransitionParameters The Parameters specific to the particular transition being taken.
     * @param workflowEffectId The tracking id of the RF Workflow Effect to generate the parameters for.
     */
    public RulesFrameworkParameters(final ExecutionContext etk,
            final IDefaultParameters workflowDefaultParameters,
            final ITransitionParameters workflowTransitionParameters,
            final long workflowEffectId) {
        defaultParameters = workflowDefaultParameters;
        customParameters =  new CustomParameters(etk, workflowEffectId);
        transitionParameters = workflowTransitionParameters;
    }

    @Override
    public IDefaultParameters getDefaultParameters(){
        return defaultParameters;
    }

    @Override
    public ICustomParameters getCustomParameters(){
        return customParameters;
    }

    @Override
    public ITransitionParameters getTransitionParameters(){
        return transitionParameters;
    }
}

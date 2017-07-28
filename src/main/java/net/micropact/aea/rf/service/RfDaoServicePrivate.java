package net.micropact.aea.rf.service;

import java.util.LinkedList;
import java.util.List;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.user.Role;

import net.entellitrak.aea.rf.ICustomParameters;
import net.entellitrak.aea.rf.ITransitionParameters;
import net.entellitrak.aea.rf.dao.IRfLookup;
import net.entellitrak.aea.rf.dao.IRfParameterType;
import net.entellitrak.aea.rf.dao.IRfScript;
import net.entellitrak.aea.rf.dao.IRfScriptParameter;
import net.entellitrak.aea.rf.dao.IRfState;
import net.entellitrak.aea.rf.dao.IRfTransition;
import net.entellitrak.aea.rf.dao.IRfWorkflow;
import net.entellitrak.aea.rf.dao.IRfWorkflowEffect;
import net.entellitrak.aea.rf.dao.IRfWorkflowParameter;
import net.entellitrak.aea.rf.dao.IScriptObject;
import net.micropact.aea.core.query.QueryUtility;
import net.micropact.aea.rf.CustomParameters;
import net.micropact.aea.rf.TransitionParameters;
import net.micropact.aea.rf.dao.RfLookupImpl;
import net.micropact.aea.rf.dao.RfParameterTypeImpl;
import net.micropact.aea.rf.dao.RfScriptImpl;
import net.micropact.aea.rf.dao.RfScriptParameterImpl;
import net.micropact.aea.rf.dao.RfStateImpl;
import net.micropact.aea.rf.dao.RfTransitionImpl;
import net.micropact.aea.rf.dao.RfWorkflowEffectImpl;
import net.micropact.aea.rf.dao.RfWorkflowImpl;
import net.micropact.aea.rf.dao.RfWorkflowParameterImpl;
import net.micropact.aea.rf.dao.ScriptObjectImpl;

/**
 * Service class for loading Rules Framework DAOs. This class contains services which are used by the private API.
 * There is a separate class for implementing the public API's service.
 *
 * @author zachary.miller
 * @see RfDaoService
 */
public class RfDaoServicePrivate {

    private final ExecutionContext etk;

    /**
     * Constructor taking the execution context to use for all queries.
     *
     * @param executionContext entellitrak execution context
     */
    public RfDaoServicePrivate(final ExecutionContext executionContext){
        etk = executionContext;
    }

    /**
     * Load a Script Object given its fully qualified path name.
     *
     * @param fullyQualifiedName fully qualified name of the Script Object
     * @return Script Object
     */
    public static IScriptObject loadScriptObject(final String fullyQualifiedName) {
        return new ScriptObjectImpl(fullyQualifiedName);
    }

    /**
     * Loads all RF Script Parameters that are children of a specific RF Script.
     *
     * @param rfScriptIdId tracking id of the RF Script
     * @return The RF Script Parameters
     */
    public List<IRfScriptParameter> loadScriptParametersByRfScriptId(final long rfScriptIdId){
        final List<IRfScriptParameter> scriptParameters = new LinkedList<>();
        for(final long scriptParameterId : QueryUtility.mapsToLongs(etk.createSQL("SELECT ID FROM t_rf_script_parameter WHERE id_parent = :rfScriptId ORDER BY c_order, id")
                .setParameter("rfScriptId", rfScriptIdId)
                .fetchList())){
            scriptParameters.add(new RfScriptParameterImpl(etk, this, scriptParameterId));
        }
        return scriptParameters;
    }

    /**
     * Load an RF Parameter by its tracking id.
     *
     * @param rfParameterTypeId tracking id of the RF Parameter Type
     * @return the RF Parameter Type
     */
    public IRfParameterType loadRfParameterTypeById(final long rfParameterTypeId){
        return new RfParameterTypeImpl(etk, rfParameterTypeId);
    }

    /**
     * Load an RF Lookup given its tracking id.
     *
     * @param rfLookupId tracking id of the RF Lookup
     * @return the RF Lookup
     */
    public IRfLookup loadRfLookupById(final Long rfLookupId) {
        return new RfLookupImpl(etk, rfLookupId);
    }

    /**
     * Load all RF Transitions which can originate from a particular RF State.
     *
     * @param rfStateId tracking id of the RF State
     * @return the RF Transitions
     */
    public List<IRfTransition> loadRfTransitionsByRfStateAllowedId(final long rfStateId){
        final List<IRfTransition> transitions = new LinkedList<>();
        for(final long transitionId : QueryUtility.mapsToLongs(etk.createSQL("SELECT ID_OWNER FROM m_rf_transition_from_state WHERE c_from_state = :rfStateId ORDER BY ID_OWNER")
                .setParameter("rfStateId", rfStateId)
                .fetchList())){
            transitions.add(new RfTransitionImpl(etk, this, transitionId));
        }
        return transitions;
    }

    /**
     * Load an RF State by its tracking id.
     *
     * @param stateId tracking id of the RF State
     * @return the RF State
     */
    public IRfState loadRfStateById(final Long stateId){
        return new RfStateImpl(etk, this, stateId);
    }

    /**
     * Load all RF States that a particular RF Transition may originate from.
     *
     * @param transitionId tracking id of the RF Transition
     * @return The RF States
     */
    public List<IRfState> loadRfStatesByTransitionFromState(final long transitionId){
        final List<IRfState> states = new LinkedList<>();
        for(final long fromStateId : QueryUtility.mapsToLongs(etk.createSQL("SELECT c_from_state FROM m_rf_transition_from_state WHERE id_owner = :rfTransitionId ORDER BY c_from_state")
                .setParameter("rfTransitionId", transitionId)
                .fetchList())){
            states.add(loadRfStateById(fromStateId));
        }
        return states;
    }

    /**
     * Load the Roles which may take a particular RF Transition.
     *
     * @param transitionId tracking id of the RF Transition
     * @return the Roles
     */
    public List<Role> loadRolesByTransition(final long transitionId) {
        final List<Role> roles = new LinkedList<>();
        for(final long roleId : QueryUtility.mapsToLongs(etk.createSQL("SELECT C_ROLE FROM m_rf_transition_role WHERE id_owner = :rfTransitionId ORDER BY c_role")
                .setParameter("rfTransitionId", transitionId)
                .fetchList())){
            roles.add(etk.getUserService().getRole(roleId));
        }
        return roles;
    }

    /**
     * Load the RF Workflow Effects which fire for a given RF Transition.
     *
     * @param transitionId tracking id of the RF Transition
     * @return the RF Workflow Effects
     */
    public List<IRfWorkflowEffect> loadRfWorkflowEffectsByTransition(final long transitionId){
        final List<IRfWorkflowEffect> effects = new LinkedList<>();
        for(final long workflowEffectId : QueryUtility.mapsToLongs(etk.createSQL("SELECT id_owner FROM m_rf_effect_transition WHERE c_transition = :rfTransitionId ORDER BY id_owner")
                .setParameter("rfTransitionId", transitionId)
                .fetchList())){
            effects.add(loadRfWorkflowEffectById(workflowEffectId));
        }
        return effects;
    }

    /**
     * Load an RF Workflow Effect by its tracking id.
     *
     * @param workflowEffectId tracking id of the RF Workflow Effect
     * @return the RF Workflow Effect
     */
    public IRfWorkflowEffect loadRfWorkflowEffectById(final long workflowEffectId){
        return new RfWorkflowEffectImpl(etk, this, workflowEffectId);
    }

    /**
     * Load the RF Transition Parameters for a given RF Transition.
     *
     * @param transitionId tracking id of the RF Transition
     * @return The RF Transition Parameters
     */
    public ITransitionParameters loadTransitionParametersByTransitionId(final long transitionId) {
        return new TransitionParameters(etk, transitionId);
    }

    /**
     * Load an RF Script by tracking id.
     *
     * @param rfScriptId tracking id of the RF Script
     * @return the RF Script
     */
    public IRfScript loadRfScriptById(final long rfScriptId){
        return new RfScriptImpl(etk, this, rfScriptId);
    }

    /**
     * Load all RF Transitions which a particular RF Workflow Effect gets fired for.
     *
     * @param workflowEffectId tracking id of the RF Workflow Effect
     * @return the RF Transitions
     */
    public List<IRfTransition> loadTransitionsByRfWorkflowEffect(final long workflowEffectId){
        final List<IRfTransition> transitions = new LinkedList<>();
        for(final long transitionId : QueryUtility.mapsToLongs(etk.createSQL("SELECT c_transition FROM m_rf_effect_transition WHERE id_owner = :rfWorkflowEffectId ORDER BY c_transition")
                .setParameter("rfWorkflowEffectId", workflowEffectId)
                .fetchList())){
            transitions.add(loadRfTransitionById(transitionId));
        }
        return transitions;
    }

    /**
     * Load an RF Transition by tracking id.
     *
     * @param transitionId tracking id of the RF Transition
     * @return The RF Transition
     */
    public IRfTransition loadRfTransitionById(final long transitionId){
        return new RfTransitionImpl(etk, this, transitionId);
    }

    /**
     * Load the Custom Parameters (Workflow Effect Parameters) for a given RF Workflow Effect.
     *
     * @param workflowEffectId tracking id of the RF Workflow Effect
     * @return the Custom Parameters
     */
    public ICustomParameters loadCustomParametersByRfWorkflowEffect(final long workflowEffectId) {
        return new CustomParameters(etk, workflowEffectId);
    }

    /**
     * Load an RF Workflow by tracking id.
     *
     * @param workflowId tracking id of the RF Workflow
     * @return the RF Workflow
     */
    public IRfWorkflow loadRfWorkflowById(final long workflowId){
        return new RfWorkflowImpl(etk, this, workflowId);
    }

    /**
     * Load an RF Workflow object given the value of its Code element.
     *
     * @param workflowCode value of the Code element of the RF Workflow
     * @return the RF Workflow
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public IRfWorkflow loadRfRfWorkflowByCode(final String workflowCode) throws IncorrectResultSizeDataAccessException{
        final int rfWorkflowId = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :rfWorkflowCode")
                .setParameter("rfWorkflowCode", workflowCode)
                .fetchInt();
        return loadRfWorkflowById(rfWorkflowId);
    }

    /**
     * Loads all RF States which are children of a particular RF Workflow.
     *
     * @param workflowId tracking id of the RF Workflow
     * @return The RF States
     */
    public List<IRfState> loadRfStatesByRfWorkflowId(final long workflowId){
        final List<IRfState> states = new LinkedList<>();
        for(final long stateId : QueryUtility.mapsToLongs(etk.createSQL("SELECT ID FROM t_rf_state WHERE id_parent = :rfWorkflowId ORDER BY c_order, c_name, id")
                .setParameter("rfWorkflowId", workflowId)
                .fetchList())){
            states.add(loadRfStateById(stateId));
        }
        return states;
    }

    /**
     * Loads all RF Transitions which are children of a particular RF Workflow.
     *
     * @param workflowId trackingId of the RF Workflow
     * @return the RF Transitions
     */
    public List<IRfTransition> loadRfTransitionsByRfWorkflow(final long workflowId){
        final List<IRfTransition> transitions = new LinkedList<>();
        for(final long transitionId : QueryUtility.mapsToLongs(etk.createSQL("SELECT ID FROM t_rf_transition WHERE id_parent = :rfWorkflowId ORDER BY c_order, c_name, id")
                .setParameter("rfWorkflowId", workflowId)
                .fetchList())){
            transitions.add(loadRfTransitionById(transitionId));
        }
        return transitions;
    }

    /**
     * Loads all RF Workflow Effects which are children of a particular RF Workflow.
     *
     * @param workflowId tracking id of the RF Workflow
     * @return the RF Workflow Effects
     */
    public List<IRfWorkflowEffect> loadWorkflowEffectsByRfWorkflow(final long workflowId) {
        final List<IRfWorkflowEffect> effects = new LinkedList<>();
        for(final long workflowEffectId : QueryUtility.mapsToLongs(etk.createSQL("SELECT ID FROM t_rf_workflow_effect WHERE id_parent = :rfWorkflowId ORDER BY c_execution_order, c_name, id")
                .setParameter("rfWorkflowId", workflowId)
                .fetchList())){
            effects.add(loadRfWorkflowEffectById(workflowEffectId));
        }
        return effects;
    }

    /**
     * Loads all RF Workflow Parameters which are children of a particular RF Workflow.
     *
     * @param workflowId tracking id of the RF Workflow
     * @return The RF Workflow Parameters
     */
    public List<IRfWorkflowParameter> loadRfWorkflowParametersByRfWorkflow(final long workflowId){
        final List<IRfWorkflowParameter> workflowParameters = new LinkedList<>();
        for(final long workflowParameterId : QueryUtility.mapsToLongs(etk.createSQL("SELECT ID FROM t_rf_workflow_parameter WHERE id_parent = :rfWorkflowId ORDER BY c_order, c_name, id")
                .setParameter("rfWorkflowId", workflowId)
                .fetchList())){
            workflowParameters.add(loadRfWorkflowParameterById(workflowParameterId));
        }
        return workflowParameters;
    }

    /**
     * Load an RF Workflow Parameter given its tracking id.
     *
     * @param workflowParameterId the tracking id of the RF Workflow Parameter
     * @return the RF Workflow Parameter
     */
    public IRfWorkflowParameter loadRfWorkflowParameterById(final long workflowParameterId){
        return new RfWorkflowParameterImpl(etk, this, workflowParameterId);
    }

    /**
     * Loads an RF State given its tracking id.
     *
     * @param stateId tracking id of the RF State
     * @return the RF State
     */
    public IRfState loadRfStateById(final long stateId){
        return new RfStateImpl(etk, this, stateId);
    }
}
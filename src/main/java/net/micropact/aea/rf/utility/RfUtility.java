package net.micropact.aea.rf.utility;

import com.entellitrak.ExecutionContext;

import net.micropact.aea.utility.Utility;

/**
 * This class is intended to contain utility methods related to the Rules Framework.
 * @author zmiller
 */
public final class RfUtility {

    /**
     * There is no reason to instantiate an RfUtility.
     */
    private RfUtility(){}

    /**
     * This method cleans up &quot;foreign keys&quot; in the database related to the RF Workflow tables
     * since entellitrak does not actually enforce their integrity.
     * This method can be called at any time.
     * @param etk entellitrak execution context
     */
    public static void cleanRfWorkflow(final ExecutionContext etk) {
        // Delete T_RF_WORKFLOW_EFFECT where T_RF_SCRIPT has been deleted
        etk.createSQL("DELETE FROM t_rf_workflow_effect WHERE c_script NOT IN(SELECT id FROM t_rf_script)")
            .execute();

        // Delete M_RF_EFFECT_TRANSITION that do not match to an Effect or a Transition
        etk.createSQL("DELETE FROM m_rf_effect_transition WHERE NOT EXISTS(SELECT * FROM t_rf_workflow_effect WHERE t_rf_workflow_effect.id = m_rf_effect_transition.id_owner) OR NOT EXISTS(SELECT * FROM t_rf_transition WHERE t_rf_transition.id = m_rf_effect_transition.c_transition)")
        .execute();

        // Delete M_RF_TRANSITION_FROM_STATE that do not match a From or To State
        etk.createSQL("DELETE FROM m_rf_transition_from_state WHERE NOT EXISTS(SELECT * FROM t_rf_transition WHERE t_rf_transition.id = m_rf_transition_from_state.id_owner) OR NOT EXISTS(SELECT * FROM t_rf_state WHERE t_rf_state.id = m_rf_transition_from_state.c_from_state)")
        .execute();

        // Update the T_RF_TRANSITION.C_TO_STATE field to NULL if it doesn't match a state
        etk.createSQL(Utility.isSqlServer(etk) ? "UPDATE t_rf_transition SET c_to_state = NULL FROM t_rf_transition WHERE NOT EXISTS(SELECT * FROM t_rf_state WHERE t_rf_state.id = t_rf_transition.c_to_state)"
                                                 : "UPDATE t_rf_transition SET c_to_state = NULL WHERE NOT EXISTS (SELECT * FROM t_rf_state WHERE t_rf_state.id = t_rf_transition.c_to_state)")
        .execute();

        // Delete from M_RF_TRANSITION_ROLE if the Transition or Role doesn't exist
        etk.createSQL("DELETE FROM m_rf_transition_role WHERE NOT EXISTS ( SELECT * FROM t_rf_transition WHERE t_rf_transition.id = m_rf_transition_role.id_owner ) OR NOT EXISTS ( SELECT * FROM etk_role WHERE etk_role.role_id = m_rf_transition_role.c_role )")
        .execute();

        // Delete T_RF_SCRIPT_PARAMETER_VALUE where the script parameter or the workflow effect does not exist
        etk.createSQL("DELETE FROM t_rf_script_parameter_value WHERE NOT EXISTS (SELECT * FROM t_rf_script_parameter scriptParameter WHERE scriptParameter.id = t_rf_script_parameter_value.c_script_parameter ) OR NOT EXISTS( SELECT * FROM t_rf_workflow_effect WHERE t_rf_workflow_effect.id = t_rf_script_parameter_value.id_parent )")
        .execute();

        // Delete the T_RF_TRANSITION_PARAMETER_VALU where the rfWorkflowParameter or the transition does not exist
        etk.createSQL("DELETE FROM t_rf_transition_parameter_valu WHERE NOT EXISTS (SELECT * FROM t_rf_workflow_parameter WHERE t_rf_workflow_parameter.id = t_rf_transition_parameter_valu.c_workflow_parameter ) OR NOT EXISTS(SELECT * FROM t_rf_transition WHERE t_rf_transition.id = t_rf_transition_parameter_valu.id_parent )")
        .execute();

        /* Could do one that deletes rf_transition_parmater_valu and t_rf_script_parameter_value
         * that have multiples but are not set as multiples */
    }
}

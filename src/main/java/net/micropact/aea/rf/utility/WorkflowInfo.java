package net.micropact.aea.rf.utility;

import java.util.Map;

import com.entellitrak.ExecutionContext;
import com.entellitrak.configuration.DataObject;

import net.entellitrak.aea.rf.dao.IRfWorkflow;

/**
 * TODO: Replace this class with {@link IRfWorkflow} once {@link DataObject} has a method to determine if it is the BTO.
 *
 * This class stores information related to a particular RF Workflow and is intended to be used in various places.<br/>
 * Eventually this information will be able to be cached.
 * @author zmiller
 */
public class WorkflowInfo {

    private final String childTable;
    private final String childTransitionColumn;
    private final String parentTable;
    private final String parentStateColumn;
    private final boolean parentIsBase;

    /**
     * Construct a new workflow info.
     *
     * @param etk entellitrak execution context
     * @param workflowCode the Code of the RF Workflow to get information about.
     */
    public WorkflowInfo(final ExecutionContext etk, final String workflowCode) {
        final Map<String, Object> result = etk.createSQL("SELECT childObject.table_name CHILDTABLE, transitionElement.column_name TRANSITIONCOLUMN, parentObject.table_name PARENTTABLE, parentObject.base_object PARENTISBASE, parentStateElement.column_name PARENTSTATECOLUMN FROM t_rf_workflow rfWorkflow JOIN etk_data_object childObject ON childObject.business_key = rfWorkflow.c_child_object JOIN etk_data_object parentObject ON parentObject.data_object_id = childObject.parent_object_id JOIN etk_data_element transitionElement ON transitionElement.business_key = rfWorkflow.c_child_transition_element AND transitionElement.data_object_id = childObject.data_object_id JOIN etk_data_element parentStateElement ON parentStateElement.business_key = rfWorkflow.c_parent_state_element AND parentStateElement.data_object_id = parentObject.data_object_id WHERE rfWorkflow.c_code = :workflowCode AND childObject.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive )")
                .setParameter("workflowCode", workflowCode)
                .fetchList().get(0);

        childTable = (String) result.get("CHILDTABLE");
        childTransitionColumn = (String) result.get("TRANSITIONCOLUMN");
        parentTable = (String) result.get("PARENTTABLE");
        parentStateColumn = (String) result.get("PARENTSTATECOLUMN");
        parentIsBase = 1 == ((Number) result.get("PARENTISBASE")).longValue();

    }

    /**
     * Get the child table.
     *
     * @return the name of the database table for the Child Object for this RF Workflow.
     */
    public String getChildTable() {
        return childTable;
    }

    /**
     * Get the child transition column.
     *
     * @return the name of the database column for the Transition element of the child object in this RF Workflow.
     */
    public String getChildTransitionColumn() {
        return childTransitionColumn;
    }

    /**
     * Get the parent table.
     *
     * @return the name of the database table for the Parent Object for this RF Workflow
     */
    public String getParentTable() {
        return parentTable;
    }

    /**
     * Get the parent state column.
     *
     * @return the name of the database column of the Parent State element for this RF Workflow
     */
    public String getParentStateColumn() {
        return parentStateColumn;
    }

    /**
     * Get whether the parent is a BTO.
     *
     * @return true if the Parent object is a Base Object in entellitrak (it does not have any parents)
     */
    public boolean getParentIsBase(){
        return parentIsBase;
    }
}

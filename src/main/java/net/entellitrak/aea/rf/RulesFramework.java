/**
 *
 * RF - Rules Framework
 *
 * zmiller 05/19/2014
 **/

package net.entellitrak.aea.rf;
import java.util.List;
import java.util.Map;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

import net.entellitrak.aea.exception.RulesFrameworkException;
import net.entellitrak.aea.rf.dao.IRfWorkflow;
import net.micropact.aea.rf.DefaultParameters;
import net.micropact.aea.rf.RulesFrameworkParameters;
import net.micropact.aea.rf.TransitionParameters;
import net.micropact.aea.utility.Utility;

/**
 * This class is for triggering Rules Framework Transitions.
 *
 *
 * @author zmiller
 */
public final class RulesFramework{

    /**
     * Since all the methods are static, there is no need to instantiate a RulesFramework.
     */
    private RulesFramework(){}

    /**
     * This should be called from the createEvent of the parent object.
     * It will automatically insert the first child, call all of its effects, and set the RfState of the parent
     *
     * @param etk the context in which the workflow should be started
     * @param workflowCode The Code of the RF Workflow which the object belongs to
     * @param trackingId trackingId of the Parent Object
     * @return trackingId of the newly created child.
     * @throws RulesFrameworkException If there is any problem trying to start the workflow
     */
    public static long startWorkflow(final ExecutionContext etk, final String workflowCode, final long trackingId)
            throws RulesFrameworkException {

        final String errorMessage = String.format("Error starting workflow for workflowCode: %s, trackingId: %s",
                workflowCode, trackingId);

        try {
            final IRfWorkflow rfWorkflow = RfServiceFactory.getRfDaoService(etk).loadRfWorkflowByCode(workflowCode);
            final String parentTable = rfWorkflow.getParentStateElement().getDataObject().getTableName();
            final String parentStateColumn = rfWorkflow.getParentStateElement().getColumnName();
            final String childTable = rfWorkflow.getChildTransitionElement().getDataObject().getTableName();
            final String childTransitionColumn = rfWorkflow.getChildTransitionElement().getColumnName();
            final boolean parentIsBase =
                    etk.getDataObjectService().getParent(rfWorkflow.getParentStateElement().getDataObject()) == null;

            final long initialTransitionId;
            /* Get the Initial Transition, there should only be one. */
            try{
                initialTransitionId = etk.createSQL(Utility.isSqlServer(etk) ? "SELECT rfTransition.id FROM t_rf_workflow rfWorkflow JOIN t_rf_transition rfTransition ON rfTransition.id_parent = rfWorkflow.id WHERE rfWorkflow.c_code = :workflowCode AND rfTransition.c_initial_transition = 1 AND EXISTS ( SELECT * FROM m_rf_transition_role transitionRole WHERE transitionRole.id_owner = rfTransition.id AND transitionRole.c_role = :roleId ) AND ( rfTransition.c_start_date IS NULL OR rfTransition.c_start_date <= CAST(DBO.ETKF_GETSERVERTIME() AS DATE) ) AND ( rfTransition.c_end_date IS NULL OR rfTransition.c_end_date > CAST(DBO.ETKF_GETSERVERTIME() AS DATE) )"
                                                                               : "SELECT rfTransition.id FROM t_rf_workflow rfWorkflow JOIN t_rf_transition rfTransition ON rfTransition.id_parent = rfWorkflow.id WHERE rfWorkflow.c_code = :workflowCode AND rfTransition.c_initial_transition = 1 AND EXISTS( SELECT * FROM m_rf_transition_role transitionRole WHERE transitionRole.id_owner = rfTransition.id AND transitionRole.c_role = :roleId ) AND (rfTransition.c_start_date IS NULL OR rfTransition.c_start_date <= TRUNC(ETKF_GETSERVERTIME())) AND (rfTransition.c_end_date IS NULL OR rfTransition.c_end_date > TRUNC(ETKF_GETSERVERTIME()))")
                        .setParameter("workflowCode", workflowCode)
                        .setParameter("roleId", etk.getCurrentUser().getRole().getId())
                        .fetchInt();
            }catch(final IncorrectResultSizeDataAccessException e){
                throw new RulesFrameworkException("There should only be 1 matching \"Initial Transition\".", e);
            }

            final Map<String, Object> initialTransition =
                    RulesFrameworkUtility.getRfTransitionById(etk, initialTransitionId);

            //Update the parent
            etk.createSQL(String.format("UPDATE %s SET %s = :stateId WHERE id = :parentId",
                    parentTable,
                    parentStateColumn))
            .setParameter("stateId", initialTransition.get("STATEID"))
            .setParameter("parentId", trackingId)
            .execute();

            final long childTrackingId;

            if(Utility.isSqlServer(etk)){
                //Insert a new child
                childTrackingId = ((Number)
                        etk.createSQL(String.format("INSERT INTO %s (id_base, id_parent, %s) VALUES((SELECT %s FROM %s WHERE id = :parentId), :parentId, :transitionId)",
                                childTable,
                                childTransitionColumn,
                                parentIsBase ? "id" : "id_base",
                                             parentTable))
                        .setParameter("parentId", trackingId)
                        .setParameter("transitionId", initialTransitionId)
                        .executeForKey("id")).longValue();
            }else{
                /* Get the ID, we could use executeForKey instead to get it */
                childTrackingId = ((Number) etk.createSQL("SELECT OBJECT_ID.NEXTVAL FROM DUAL")
                        .fetchObject()).longValue();

                //Insert a new child
                etk.createSQL(String.format("INSERT INTO %s (ID, id_base, id_parent, %s) VALUES(:childTrackingId, (SELECT %s FROM %s WHERE id = :parentId), :parentId, :transitionId)",
                        childTable,
                        childTransitionColumn,
                        parentIsBase ? "id" : "id_base",
                                     parentTable))
                .setParameter("childTrackingId", childTrackingId)
                .setParameter("parentId", trackingId)
                .setParameter("transitionId", initialTransitionId)
                .execute();
            }

            final IDefaultParameters defaultParameters = new DefaultParameters(etk,
                    workflowCode,
                    childTrackingId,
                    null,
                    initialTransitionId);

            //Do the transition's effects
            RulesFrameworkUtility.doTransitionEffects(etk, defaultParameters);

            //Return the new child's id
            return childTrackingId;
        } catch (final Exception e) {
            throw new RulesFrameworkException(errorMessage, e);
        }
    }

    /**
     * This should be called from the createEvent of a Child.
     * It will update the RfState of the parent and fire all WorkflowEffects.
     *
     * @param etk The context to update the workflow in
     * @param workflowCode The Code of the RF Workflow
     * @param trackingId the trackingId of the Child Object
     * @throws RulesFrameworkException If there is any problem updating the workflow
     */
    public static void updateWorkflow(final ExecutionContext etk, final String workflowCode, final long trackingId)
            throws RulesFrameworkException{
        try {
            //Get the RF Workflow
            final IRfWorkflow rfWorkflow = RfServiceFactory.getRfDaoService(etk).loadRfWorkflowByCode(workflowCode);
            final String parentTable = rfWorkflow.getParentStateElement().getDataObject().getTableName();
            final String parentStateColumn = rfWorkflow.getParentStateElement().getColumnName();
            final String childTable = rfWorkflow.getChildTransitionElement().getDataObject().getTableName();
            final String childTransitionColumn = rfWorkflow.getChildTransitionElement().getColumnName();

            final Map<String, Object> info = etk.createSQL(String.format(Utility.isSqlServer(etk) ? "SELECT (SELECT childObject.%s  FROM %s childObject WHERE childObject.id = :trackingId) TRANSITIONID, (SELECT parentObject.%s FROM %s parentObject JOIN %s childObject ON childObject.id_parent = parentObject.id WHERE childObject.id = :trackingId) FROMSTATEID"
                                                                                                    : "SELECT (SELECT childObject.%s  FROM %s childObject WHERE childObject.id = :trackingId) TRANSITIONID, (SELECT parentObject.%s FROM %s parentObject JOIN %s childObject ON childObject.id_parent = parentObject.id WHERE childObject.id = :trackingId) FROMSTATEID FROM DUAL",
                                                                                                    childTransitionColumn,
                                                                                                    childTable,
                                                                                                    parentStateColumn,
                                                                                                    parentTable,
                                                                                                    childTable))
                    .setParameter("trackingId", trackingId)
                    .fetchMap(); //TRANSITIONID, FROMSTATEID

            final long rfTransitionId = ((Number) info.get("TRANSITIONID")).longValue();
            final Long fromStateId = info.get("FROMSTATEID") == null
                    ? null
                    : ((Number) info.get("FROMSTATEID")).longValue();

            final Map<String, Object> rfTransition = RulesFrameworkUtility.getRfTransitionById(etk, rfTransitionId);

            //Update the parent
            etk.createSQL(String.format(Utility.isSqlServer(etk) ? "UPDATE %s SET %s = ISNULL(:stateId, %s) WHERE id = (SELECT childObject.id_parent FROM %s childObject WHERE childObject.id = :childId)"
                                                                   : "UPDATE %s parentObject SET parentObject.%s = NVL(:stateId, parentObject.%s) WHERE parentObject.id = (SELECT childObject.id_parent FROM %s childObject WHERE childObject.id = :childId)",
                                                                   parentTable,
                                                                   parentStateColumn,
                                                                   parentStateColumn,
                                                                   childTable))
            .setParameter("stateId", rfTransition.get("STATEID"))
            .setParameter("childId", trackingId)
            .execute();

            final IDefaultParameters defaultParameters = new DefaultParameters(etk,
                    workflowCode,
                    trackingId,
                    fromStateId,
                    rfTransitionId);

            /*Do the Effects*/
            RulesFrameworkUtility.doTransitionEffects(etk, defaultParameters);
        } catch (final Exception e) {
            throw new RulesFrameworkException(String.format("Error updating workflow for workflowCode: %s, trackingId: %s", workflowCode, trackingId),
                    e);
        }
    }

    /**
     * <p>
     *  This is a convenience function which does not truly add new functionality.
     *  It will insert a new Child Object and then call {@link #updateWorkflow(ExecutionContext, String, long)}.
     *  This method is useful if you are triggering the workflow from someplace other than the Child Object,
     *  for instance the Parent Object or Tracking Inbox.
     * </p>
     *
     * @param etk The context to use to insert the child
     * @param workflowCode The Code of the RF Workflow
     * @param parentTrackingId The trackingId of the Parent Object which the child should be inserted under
     * @param rfTransitionCode The Code of the RF Transition which the child represents
     * @return The trackingId of the newly created Child Object
     * @throws RulesFrameworkException If any problem is encountered
     */
    public static long insertChildWorkflow(final ExecutionContext etk,
            final String workflowCode,
            final long parentTrackingId,
            final String rfTransitionCode)
                    throws RulesFrameworkException{

        try {
            final IRfWorkflow rfWorkflow = RfServiceFactory.getRfDaoService(etk).loadRfWorkflowByCode(workflowCode);
            final String parentTable = rfWorkflow.getParentStateElement().getDataObject().getTableName();
            final String childTable = rfWorkflow.getChildTransitionElement().getDataObject().getTableName();
            final String childTransitionColumn = rfWorkflow.getChildTransitionElement().getColumnName();
            final boolean parentIsBase =
                    etk.getDataObjectService().getParent(rfWorkflow.getParentStateElement().getDataObject()) == null;

            final long childTrackingId;

            if(Utility.isSqlServer(etk)){
                childTrackingId = ((Number) etk.createSQL(String.format("INSERT INTO %s (id_base, id_parent, %s) VALUES((SELECT %s  FROM %s WHERE id = :idParent), :idParent, (SELECT rfTransition.id FROM t_rf_workflow workflow JOIN t_rf_transition rfTransition ON rfTransition.id_parent = workflow.id WHERE workflow.c_code = :workflowCode AND rfTransition.c_code = :transitionCode))",
                        childTable,
                        childTransitionColumn,
                        parentIsBase ? "id" : "id_base",
                                     parentTable))
                        .setParameter("transitionCode", rfTransitionCode)
                        .setParameter("workflowCode", workflowCode)
                        .setParameter("idParent", parentTrackingId)
                        .executeForKey("id")).longValue();
            }else{
                childTrackingId = ((Number) etk.createSQL("SELECT OBJECT_ID.NEXTVAL FROM DUAL").fetchObject()).longValue();

                etk.createSQL(String.format("INSERT INTO %s (id, id_base, id_parent, %s) VALUES(:childTrackingId, (SELECT %s  FROM %s WHERE id = :idParent), :idParent, (SELECT rfTransition.id FROM t_rf_workflow workflow JOIN t_rf_transition rfTransition ON rfTransition.id_parent = workflow.id WHERE workflow.c_code = :workflowCode AND rfTransition.c_code = :transitionCode))",
                        childTable,
                        childTransitionColumn,
                        parentIsBase ? "id" : "id_base",
                                     parentTable))
                .setParameter("transitionCode", rfTransitionCode)
                .setParameter("workflowCode", workflowCode)
                .setParameter("idParent", parentTrackingId)
                .setParameter("childTrackingId", childTrackingId)
                .execute();
            }

            updateWorkflow(etk, workflowCode, childTrackingId);
            return childTrackingId;
        } catch (final Exception e) {
            throw new RulesFrameworkException(String.format("Error inserting child workflow for workflowCode: %s, parentTrackinId: %s, transitionCode: %s",
                    workflowCode, parentTrackingId, rfTransitionCode),
                    e);
        }
    }
}

/**
 * This class is for internal use of the Rules Framework. It contains helper methods.
 *
 * @author zmiller
 */
final class RulesFrameworkUtility{

    /**
     * There is no reason to need to make a RulesFrameworkUtility.
     */
    private RulesFrameworkUtility(){}

    /**
     * This method gets all necessary information regarding a particular Transition.
     *
     * @param etk The context to execute queries
     * @param rfTransitionId the trackingId of the RF Transition
     * @return a Map with the following keys: TRANSITIONID, TRANSITIONCODE, STATEID, STATECODE
     */
    public static Map<String, Object> getRfTransitionById(final ExecutionContext etk, final long rfTransitionId){
        return etk.createSQL("SELECT transition.id transitionId, transition.c_code transitionCode, rfState.id stateId, rfState.c_code stateCode FROM t_rf_transition transition LEFT JOIN t_rf_state rfState ON rfState.id = transition.c_to_state WHERE transition.id = :transitionId")
                .setParameter("transitionId", rfTransitionId)
                .fetchList().get(0);
    }

    /**
     * This will cause all the WorkflowEffects for a Transition to be fired off.
     *
     * @param etk The context to use for database queries
     * @param defaultParameters The Default Parameters which will be used for the effects.
     *
     *  If there is an underlying core {@link IncorrectResultSizeDataAccessException}
     * @throws RulesFrameworkException If any other problems occur
     */
    public static void doTransitionEffects(final ExecutionContext etk, final IDefaultParameters defaultParameters)
            throws RulesFrameworkException{
        final String workflowCode = defaultParameters.getRfWorkflow().getCode();

        try {

            final ITransitionParameters transitionParameters =
                    new TransitionParameters(etk, defaultParameters.getRfTransition().getId());

            final IRfWorkflow rfWorkflow = RfServiceFactory.getRfDaoService(etk).loadRfWorkflowByCode(workflowCode);
            final String childTable = rfWorkflow.getChildTransitionElement().getDataObject().getTableName();
            final String childTransitionColumn = rfWorkflow.getChildTransitionElement().getColumnName();

            final List<Map<String, Object>> rfTransitionEffects = etk.createSQL(String.format("SELECT workflowEffect.id workflowEffectId, script.c_script_object SCRIPTOBJECT FROM %s childObject JOIN t_rf_transition transition ON transition.id = childObject.%s JOIN m_rf_effect_transition effectTransition ON effectTransition.c_transition = transition.id JOIN t_rf_workflow_effect workflowEffect ON workflowEffect.id = effectTransition.id_owner JOIN t_rf_script script ON script.id = workflowEffect.c_script WHERE childObject.id = :trackingId ORDER BY workflowEffect.c_execution_order",
                    childTable,
                    childTransitionColumn))
                    .setParameter("trackingId", defaultParameters.getChildTrackingId())
                    .fetchList(); /*WORKFLOWEFFECTID, SCRIPTOBJECT*/

            //Do the transition effects
            for(final Map<String, Object> rfTransitionEffect : rfTransitionEffects){
                final IRulesFrameworkParameters frameworkParameters =
                        new RulesFrameworkParameters(etk,
                                defaultParameters,
                                transitionParameters,
                                ((Number) rfTransitionEffect.get("WORKFLOWEFFECTID")).longValue());

                final IScript script = (IScript) Class.forName((String) rfTransitionEffect.get("SCRIPTOBJECT")).newInstance();

                script.doEffect(etk, frameworkParameters);
            }
        } catch (final Exception e) {
            throw new RulesFrameworkException(String.format("Error performing transition effects for workflowCode: %s, trackingId: %s",
                    workflowCode, defaultParameters.getChildTrackingId()),
                    e);
        }
    }
}

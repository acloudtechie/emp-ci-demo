package net.entellitrak.aea.rf;

import com.entellitrak.ApplicationException;
import com.entellitrak.configuration.DataElement;
import com.entellitrak.lookup.LookupExecutionContext;

import net.entellitrak.aea.rf.dao.IRfWorkflow;
import net.micropact.aea.utility.Utility;

/**
 * <p>
 *  This class contains useful functions which can be used for generating the lookups for data elements
 *  related to the Rules Framework.
 *  Your own RF Workflow related {@link com.entellitrak.lookup.LookupHandler}s
 *  can simply contain a call to one of the methods in this class.
 * </p>
 *
 * @author zmiller
 */
public final class RulesFrameworkLookup {

    /**
     * There is no reason to need to make a new RulesFrameworkLookup.
     */
    private RulesFrameworkLookup(){}

    /**
     * This method will generate the lookup for the Transition data element on the Child Object.
     * It returns the Tracking Id of RF Transition objects.
     *
     * @param etk The lookup context to be used
     * @param rfWorkflowCode The Code of the RF Workflow that the lookup will be executed for
     * @return The lookup String which should be returned to core entellitrak
     * @throws ApplicationException If any problem occurs
     */
    public static String generateChildTransitionLookup(final LookupExecutionContext etk, final String rfWorkflowCode)
            throws ApplicationException{

        final IRfWorkflow rfWorkflow;
        try {
            rfWorkflow = RfServiceFactory.getRfDaoService(etk).loadRfWorkflowByCode(rfWorkflowCode);
        } catch (final Exception e) {
            throw new ApplicationException(String.format("Error attempting to load the workflow information. This is most likely caused by there not being exactly one RF Worklfow object with code: \"%s\"",
                    rfWorkflowCode), e);
        }

        final String parentTable = rfWorkflow.getParentStateElement().getDataObject().getTableName();
        final String parentStateColumn = rfWorkflow.getParentStateElement().getColumnName();
        final String childTable = rfWorkflow.getChildTransitionElement().getDataObject().getTableName();
        final String childTransitionColumn = rfWorkflow.getChildTransitionElement().getColumnName();

        if(etk.isForTracking()){
            return Utility.isSqlServer(etk) ? "SELECT rfTransition.id Value, rfTransition.c_name Display FROM t_rf_transition rfTransition WHERE rfTransition.id = (SELECT "+childTransitionColumn+" FROM "+childTable+" WHERE id = {?trackingId}) OR (EXISTS (SELECT * FROM m_rf_transition_role transitionRole WHERE transitionRole.id_owner = rfTransition.id AND transitionRole.c_role = {?currentUser.roleId} ) AND rfTransition.id IN (SELECT rfTransitionFromState.id_owner FROM m_rf_transition_from_state rfTransitionFromState JOIN "+parentTable+" parentObject ON parentObject."+parentStateColumn+" = rfTransitionFromState.c_from_state WHERE parentObject.id = {?parentId} ) AND (rfTransition.c_start_date IS NULL OR rfTransition.c_start_date <= CAST(DBO.ETKF_GETSERVERTIME() AS DATE)) AND (rfTransition.c_end_date IS NULL OR rfTransition.c_end_date > CAST(DBO.ETKF_GETSERVERTIME() AS DATE))) ORDER BY rfTransition.c_order, Display"
                                            : "SELECT rfTransition.id Value, rfTransition.c_name Display FROM t_rf_transition rfTransition WHERE rfTransition.id = (SELECT "+childTransitionColumn+" FROM "+childTable+" WHERE id = {?trackingId}) OR (EXISTS (SELECT * FROM m_rf_transition_role transitionRole WHERE transitionRole.id_owner = rfTransition.id AND transitionRole.c_role = {?currentUser.roleId} ) AND rfTransition.id IN (SELECT rfTransitionFromState.id_owner FROM m_rf_transition_from_state rfTransitionFromState JOIN "+parentTable+" parentObject ON parentObject."+parentStateColumn+" = rfTransitionFromState.c_from_state WHERE parentObject.id = {?parentId} ) AND (rfTransition.c_start_date IS NULL OR rfTransition.c_start_date <= TRUNC(ETKF_GETSERVERTIME())) AND (rfTransition.c_end_date IS NULL OR rfTransition.c_end_date > TRUNC(ETKF_GETSERVERTIME()))) ORDER BY rfTransition.c_order, Display";
        }else if(etk.isForSearch()){
            return Utility.isSqlServer(etk) ? "SELECT rfTransition.id Value, rfTransition.c_name Display FROM t_rf_transition rfTransition LEFT JOIN t_rf_workflow rfWorkflow ON rfWorkflow.id = rfTransition.id_parent WHERE rfTransition.id IN(SELECT "+childTransitionColumn+" FROM "+childTable+") OR (rfWorkflow.c_code = '"+rfWorkflowCode+"' AND (rfTransition.c_start_date IS NULL OR rfTransition.c_start_date <= CAST(DBO.ETKF_GETSERVERTIME() AS DATE)) AND (rfTransition.c_end_date IS NULL OR rfTransition.c_end_date > CAST(DBO.ETKF_GETSERVERTIME() AS DATE))) ORDER BY c_order, Display, Value"
                                            : "SELECT rfTransition.id Value, rfTransition.c_name Display FROM t_rf_transition rfTransition LEFT JOIN t_rf_workflow rfWorkflow ON rfWorkflow.id = rfTransition.id_parent WHERE rfTransition.id IN(SELECT "+childTransitionColumn+" FROM "+childTable+") OR (rfWorkflow.c_code = '"+rfWorkflowCode+"' AND (rfTransition.c_start_date IS NULL OR rfTransition.c_start_date <= TRUNC(ETKF_GETSERVERTIME())) AND (rfTransition.c_end_date IS NULL OR rfTransition.c_end_date > TRUNC(ETKF_GETSERVERTIME()))) ORDER BY c_order, Display, Value";
        }else if(etk.isForAdvancedSearch()){
            return "SELECT rfTransition.id Value, rfTransition.c_name Display FROM t_rf_transition rfTransition JOIN t_rf_workflow rfWorkflow ON rfWorkflow.id = rfTransition.id_parent WHERE rfWorkflow.c_code = '"+rfWorkflowCode+"' ORDER BY rfTransition.c_order, Display, Value";
        }else if(etk.isForView()){
            return "SELECT rfTransition.id Value, rfTransition.c_name Display FROM t_rf_transition rfTransition JOIN t_rf_workflow rfWorkflow ON rfWorkflow.id = rfTransition.id_parent WHERE rfWorkflow.c_code = '"+rfWorkflowCode+"'";
        }else if(etk.isForSingleResult()){
            return "SELECT rfTransition.id Value, rfTransition.c_name Display FROM "+childTable+" childTable JOIN t_rf_transition rfTransition ON rfTransition.id = childTable."+childTransitionColumn+" WHERE childTable.id = {?trackingId}";
        }else {
            throw new ApplicationException("net.entellitrak.aea.rf.RulesFrameworkLookup.generateChildTransitionLookup(LookupExecutionContext, String) only supports forTracking, forSearch, forView, forAdvancedSearch and forSingleResult");
        }
    }

    /**
     * This method will generate a lookup which can be used for the State column of the Parent Object.
     * The VALUEs will be the Tracking Ids of the RF State objects.
     *
     * @param etk The context which the lookup will be executed in
     * @param rfWorkflowCode The Code of the RF Workflow that the lookup will be executed for
     * @return The String which should be returned to core entellitrak
     * @throws ApplicationException If any problem occurs
     */
    public static String generateParentStateLookup(final LookupExecutionContext etk,
            final String rfWorkflowCode) throws ApplicationException{
        try {
            final IRfWorkflow iRfWorkflow = RfServiceFactory.getRfDaoService(etk).loadRfWorkflowByCode(rfWorkflowCode);
            final DataElement parentRuleStateElement = iRfWorkflow.getParentStateElement();

            final String parentTableName = parentRuleStateElement.getDataObject().getTableName();
            final String parentRuleStateElementName = parentRuleStateElement.getPropertyName();
            final String parentRuleStateColumnName = parentRuleStateElement.getColumnName();

            if(etk.isForTracking()){
                return Utility.isSqlServer(etk) ? "SELECT id Value, c_name Display FROM t_rf_state WHERE id = CASE WHEN {?"+parentRuleStateElementName+"} != '' THEN {?"+parentRuleStateElementName+"} END"
                                                : "SELECT id Value, c_name Display FROM t_rf_state WHERE id = {?"+parentRuleStateElementName+"}";
            }else if(etk.isForSearch()){
                return Utility.isSqlServer(etk) ? "SELECT rfState.id Value, rfState.c_name Display FROM t_rf_state rfState LEFT JOIN t_rf_workflow rfWorkflow ON rfWorkflow.id = rfState.id_parent WHERE rfState.id IN ( SELECT "+parentRuleStateColumnName+" FROM "+parentTableName+") OR ( rfWorkflow.c_code = '"+rfWorkflowCode+"' AND ( rfState.c_start_date IS NULL OR rfState.c_start_date <= CAST(DBO.ETKF_GETSERVERTIME() AS DATE) ) AND ( rfState.c_end_date IS NULL OR rfState.c_end_date > CAST(DBO.ETKF_GETSERVERTIME() AS DATE) ) ) ORDER BY rfState.c_order, Display, Value"
                                                : "SELECT rfState.id Value, rfState.c_name Display FROM t_rf_state rfState LEFT JOIN t_rf_workflow rfWorkflow ON rfWorkflow.id = rfState.id_parent WHERE rfState.id IN (SELECT "+parentRuleStateColumnName+" FROM "+parentTableName+") OR(rfWorkflow.c_code = '"+rfWorkflowCode+"' AND (rfState.c_start_date IS NULL OR rfState.c_start_date <= TRUNC(ETKF_GETSERVERTIME())) AND (rfState.c_end_date IS NULL OR rfState.c_end_date > TRUNC(ETKF_GETSERVERTIME()))) ORDER BY rfState.c_order, Display, Value";
            }else if(etk.isForAdvancedSearch()){
                return "SELECT rfState.id Value, rfState.c_name Display FROM t_rf_state rfState JOIN t_rf_workflow rfWorkflow ON rfWorkflow.id = rfState.id_parent WHERE rfWorkflow.c_code = '"+rfWorkflowCode+"' ORDER BY rfState.c_order, Display, Value";
            }else if(etk.isForView()){
                return "SELECT rfState.id Value, rfState.c_name Display FROM t_rf_state rfState JOIN t_rf_workflow rfWorkflow ON rfWorkflow.id = rfState.id_parent WHERE rfWorkflow.c_code = '"+rfWorkflowCode+"'";
            }else if(etk.isForSingleResult()){
                return "SELECT rfState.id Value, rfState.c_name Display FROM "+parentTableName+" parentObject JOIN t_rf_state rfState ON rfState.id = parentObject."+parentRuleStateColumnName+" WHERE parentObject.id = {?trackingId}";
            }else{
                throw new ApplicationException("net.entellitrak.aea.rf.RulesFrameworkLookup.generateParentStateLookup(LookupExecutionContext, String) only supports forTracking, forSearch, forView, forAdvancedSearch and forSingleResult");
            }
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * <p>
     *  This method is useful if you want to be able to take transitions through a dropdown or button on the
     *  parent object.
     *  This method should return the same values as the
     *  {@link #generateChildTransitionLookup(LookupExecutionContext, String)} except that it expects to be called
     *  on the form of the parent object.
     *  Note: If you do implement this functionality it should be done using an unbound form field and not
     *  an actual data element.
     * </p>
     * <p>
     *   It returns a VALUE of the C_CODE column of RF Transition objects and the Value should therefore be String.
     * </p>
     *
     * @param etk The context which the lookup will be executed in
     * @param rfWorkflowCode The Code of the RF Workflow that the lookup will be executed for
     * @return The String which should be returned to core entellitrak
     * @throws ApplicationException If any problem occurs
     */
    public static String generateParentTransitionLookup(final LookupExecutionContext etk, final String rfWorkflowCode)
            throws ApplicationException{

        final IRfWorkflow rfWorkflow;
        try {
            rfWorkflow = RfServiceFactory.getRfDaoService(etk).loadRfWorkflowByCode(rfWorkflowCode);
        } catch (final Exception e) {
            throw new ApplicationException(String.format("Error attempting to load the workflow information. This is most likely caused by there not being exactly one RF Worklfow object with code: \"%s\"",
                    rfWorkflowCode), e);
        }

        final String parentTable = rfWorkflow.getParentStateElement().getDataObject().getTableName();
        final String parentStateColumn = rfWorkflow.getParentStateElement().getColumnName();

        if(etk.isForTracking()){
            return String.format(Utility.isSqlServer(etk) ? "SELECT rfTransition.c_code Value, rfTransition.c_name Display FROM t_rf_transition rfTransition WHERE EXISTS ( SELECT * FROM m_rf_transition_role transitionRole WHERE transitionRole.id_owner = rfTransition.id AND transitionRole.c_role = {?currentUser.roleId} ) AND rfTransition.id IN ( SELECT rfTransitionFromState.id_owner FROM m_rf_transition_from_state rfTransitionFromState JOIN %s parentObject ON parentObject.%s = rfTransitionFromState.c_from_state WHERE parentObject.id = {?trackingId} ) AND ( rfTransition.c_start_date IS NULL OR rfTransition.c_start_date <= CAST(DBO.ETKF_GETSERVERTIME() AS DATE) ) AND ( rfTransition.c_end_date IS NULL OR rfTransition.c_end_date > CAST(DBO.ETKF_GETSERVERTIME() AS DATE) ) AND rfTransition.c_to_state IS NOT NULL ORDER BY rfTransition.c_order, Display"
                                                            : "SELECT rfTransition.c_code Value, rfTransition.c_name Display FROM t_rf_transition rfTransition WHERE EXISTS ( SELECT * FROM m_rf_transition_role transitionRole WHERE transitionRole.id_owner = rfTransition.id AND transitionRole.c_role = {?currentUser.roleId} ) AND rfTransition.id IN ( SELECT rfTransitionFromState.id_owner FROM m_rf_transition_from_state rfTransitionFromState JOIN %s parentObject ON parentObject.%s = rfTransitionFromState.c_from_state WHERE parentObject.id = {?trackingId} ) AND ( rfTransition.c_start_date IS NULL OR rfTransition.c_start_date <= TRUNC(ETKF_GETSERVERTIME()) ) AND ( rfTransition.c_end_date IS NULL OR rfTransition.c_end_date > TRUNC(ETKF_GETSERVERTIME()) ) AND rfTransition.c_to_state IS NOT NULL ORDER BY rfTransition.c_order, Display",
                                                            parentTable,
                                                            parentStateColumn);
        }else if(etk.isForSingleResult()){
            return "SELECT rfTransition.c_code Value, rfTransition.c_name Display FROM t_rf_transition rfTransition WHERE 1 = 0";
        }else{
            throw new ApplicationException("net.entellitrak.aea.rf.RulesFrameworkLookup.generateParentTransitionLookup(LookupExecutionContext, String) only supports forTracking and forSingleResult");
        }
    }
}

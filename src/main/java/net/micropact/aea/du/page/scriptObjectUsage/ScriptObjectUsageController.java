package net.micropact.aea.du.page.scriptObjectUsage;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.query.InClauseInfo;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * Controller code for a page which shows where different Script Objects are being used within entellitrak.
 *
 * @author zmiller
 */
public class ScriptObjectUsageController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {

        try {
            final TextResponse response = etk.createTextResponse();

            final List<Map<String, Object>> resultList = new LinkedList<>();

            for(final TypeOfReference type : TypeOfReference.values()){
                for(final Map<String, Object> item :
                    etk.createSQL(TypeOfReference.getQuery(etk, type))
                    .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                    .fetchList()){
                    item.put("type", type.toString());
                    resultList.add(item);
                }
            }

            final List<Long> scriptIds = new LinkedList<>();
            scriptIds.addAll(selectScriptId(resultList));

            response.put("items", JsonUtilities.encode(resultList));
            response.put("scriptObjects", getScriptObjectJson(etk, scriptIds));

            return response;

        }  catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * This method selects out the values associated with the SCRIPT_ID key in the Maps.
     *
     * @param list A list of items which have a SCRIPT_ID key.
     * @return A list of the values associated with the SCRIPT_ID keys.
     */
    private static List<Long> selectScriptId(final List<Map<String, Object>> list){
        final List<Long> returnList = new LinkedList<>();

        for(final Map<String, Object> item : list){
            final Object value = item.get("SCRIPT_ID");
            if(value != null && !"".equals(value)){
                returnList.add(((Number) value).longValue());
            }
        }

        return returnList;
    }

    /**
     * Gets a JSON representation for a particular set of Script Objects.
     *
     * @param etk entellitrak execution context
     * @param scriptObjectIds the list of Script Object Ids to get the JSON for
     * @return A JSON representation of the Script Objects
     */
    private static String getScriptObjectJson(final ExecutionContext etk, final List<Long> scriptObjectIds){
        final InClauseInfo inClause = new InClauseInfo("script_id", "scriptIds", scriptObjectIds);
        final Set<Map<String, Object>> uniqueScripts = new HashSet<>(
                etk.createSQL(String.format("SELECT scriptObject.SCRIPT_ID, scriptObject.SCRIPT_BUSINESS_KEY, scriptObject.SCRIPT_LANGUAGE_TYPE, scriptObject.FULLY_QUALIFIED_SCRIPT_NAME, scriptObject.SCRIPT_HANDLER_TYPE FROM aea_script_pkg_view_sys_only scriptObject WHERE %s",
                        inClause.getQueryFragment()))
                .setParameter(inClause.getParameterMap())
                .fetchList());

        final List<Map<String, Object>> finalizedList = new LinkedList<>(uniqueScripts);
        Collections.sort(finalizedList, (so1, so2) -> ((String) so1.get("FULLY_QUALIFIED_SCRIPT_NAME"))
                .compareTo((String) so2.get("FULLY_QUALIFIED_SCRIPT_NAME")));

        return JsonUtilities.encode(finalizedList);
    }

    /**
     * This is an enumeration of all the places within entellitrak which can reference a Script Object.
     *
     * @author zmiller
     */
    private enum TypeOfReference{
        PAGE_CONTROLLER,
        PAGE_VIEW,
        DISPLAY_MAPPING,
        SYSTEM_EVENT_LISTENER,
        FORM_EVENT,
        FORM_ELEMENT_EVENT,
        JOB,
        LOOKUP,
        TRANSITION,
        DATA_EVENT,
        RESPONSIVE_SCRIPT,
        FILTER_HANDLER;

        /**
         * This returns a query which gets all the items of a particular type.
         * The query MUST have the following columns: ID, NAME, SCRIPT_ID.
         * The query MAY return additional columns.
         * The query will have access to :trackingConfigId
         *
         * @param etk entellitrak execution context
         * @param type The place which refers to a Script Object
         * @return An SQL query String to get references of this type to Script Objects
         */
        public static String getQuery(final ExecutionContext etk, final TypeOfReference type){
            switch (type){
                case PAGE_CONTROLLER:
                    return "SELECT p.page_id ID, p.name NAME, p.controller_script_id SCRIPT_ID FROM etk_page p ORDER BY p.NAME";
                case PAGE_VIEW:
                    return "SELECT p.page_id ID, p.name NAME, p.view_script_id SCRIPT_ID FROM etk_page p ORDER BY p.NAME";
                case DISPLAY_MAPPING:
                    return "SELECT dm.form_mapping_id ID, dm.name NAME, dm.evaluation_script_id SCRIPT_ID FROM etk_display_mapping dm JOIN etk_data_object do ON do.data_object_id = dm.data_object_id WHERE do.tracking_config_id = :trackingConfigId ORDER BY dm.NAME, dm.FORM_MAPPING_ID";
                case SYSTEM_EVENT_LISTENER:
                    return "SELECT business_key ID, name NAME, script_object_id SCRIPT_ID FROM etk_system_event_listener ORDER BY NAME, BUSINESS_KEY";
                case FORM_EVENT:
                    return "SELECT DISTINCT formEventHandler.script_object_id SCRIPT_ID, dataForm.name NAME, dataForm.data_form_id ID FROM etk_data_object dataObject JOIN etk_data_form dataForm ON dataForm.data_object_id = dataObject.data_object_id JOIN etk_data_form_event_handler formEventHandler ON formEventHandler.data_form_id = dataForm.data_form_id WHERE dataObject.tracking_config_id = :trackingConfigId ORDER BY NAME, ID";
                case FORM_ELEMENT_EVENT:
                    return Utility.isSqlServer(etk) ? "SELECT dataForm.data_form_id FORM_ID, dataForm.name + ' - ' + formControl.name NAME, formControl.form_control_id ID, formControlEventHandler.script_object_id SCRIPT_ID FROM etk_data_object dataObject JOIN etk_data_form dataForm ON dataForm.data_object_id = dataObject.data_object_id JOIN etk_form_control formControl ON formControl.data_form_id = dataForm.data_form_id JOIN etk_form_control_event_handler formControlEventHandler ON formControlEventHandler.form_control_id = formControl.form_control_id WHERE dataObject.tracking_config_id = :trackingConfigId ORDER BY NAME, ID"
                                                      : "SELECT dataForm.data_form_id FORM_ID, dataForm.name || ' - ' || formControl.name NAME, formControl.form_control_id ID, formControlEventHandler.script_object_id SCRIPT_ID FROM etk_data_object dataObject JOIN etk_data_form dataForm ON dataForm.data_object_id = dataObject.data_object_id JOIN etk_form_control formControl ON formControl.data_form_id = dataForm.data_form_id JOIN etk_form_control_event_handler formControlEventHandler ON formControlEventHandler.form_control_id = formControl.form_control_id WHERE dataObject.tracking_config_id = :trackingConfigId ORDER BY NAME, ID";
                case JOB:
                    return "SELECT job.business_key ID, job.name NAME, jobCustom.script_object_id SCRIPT_ID FROM etk_job job JOIN etk_job_custom jobCustom ON job.job_id = jobCustom.job_custom_id ORDER BY NAME, BUSINESS_KEY";
                case LOOKUP:
                    return "SELECT lookupDefinition.lookup_definition_id ID, lookupDefinition.name NAME, lookupDefinition.sql_script_object_id SCRIPT_ID FROM etk_lookup_definition lookupDefinition WHERE lookupDefinition.tracking_config_id = :trackingConfigId ORDER BY NAME, LOOKUP_DEFINITION_ID";
                case TRANSITION:
                    return "SELECT doTransition.do_transition_id ID, doTransition.name NAME, doTransition.trigger_script_id SCRIPT_ID FROM etk_data_object dataObject JOIN etk_do_state doState ON dataObject.data_object_id = doState.data_object_id JOIN etk_do_transition doTransition ON doTransition.do_previous_state_id = doState.do_state_id WHERE dataObject.tracking_config_id = :trackingConfigId ORDER BY NAME, DO_TRANSITION_ID";
                case DATA_EVENT:
                    return Utility.isSqlServer(etk) ? "SELECT dataObject.data_object_id ID, dataObject.name + ' (' + CAST(COUNT(*) AS VARCHAR) + ')' NAME, dataEventListener.script_object_id SCRIPT_ID FROM etk_data_event_listener dataEventListener JOIN etk_data_object dataObject ON dataObject.data_object_id = dataEventListener.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId GROUP BY dataObject.data_object_id, dataObject.name, dataEventListener.script_object_id ORDER BY NAME, ID"
                                                      : "SELECT dataObject.data_object_id ID, dataObject.name || ' (' || COUNT(*) || ')' NAME, dataEventListener.script_object_id SCRIPT_ID FROM etk_data_event_listener dataEventListener JOIN etk_data_object dataObject ON dataObject.data_object_id = dataEventListener.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId GROUP BY dataObject.data_object_id, dataObject.name, dataEventListener.script_object_id ORDER BY NAME, ID";
                case RESPONSIVE_SCRIPT:
                    return "SELECT dataForm.data_form_id ID, dataForm.name NAME, dataForm.script_object_id SCRIPT_ID FROM etk_data_object dataObject JOIN etk_data_form dataForm ON dataForm.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId ORDER BY NAME, ID";
                case FILTER_HANDLER:
                    return "SELECT dataObject.data_object_id ID, dataObject.name NAME, filterHandler.script_object_id SCRIPT_ID FROM etk_data_object dataObject JOIN etk_filter_handler filterHandler ON filterHandler.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId";
                default:
                    throw new IllegalArgumentException(String.format("Non-exhaustive pattern: TypeOfReference %s not accounted for", type));
            }
        }
    }
}

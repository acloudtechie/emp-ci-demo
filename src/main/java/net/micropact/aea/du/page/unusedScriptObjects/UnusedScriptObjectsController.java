package net.micropact.aea.du.page.unusedScriptObjects;

import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.LookupSourceType;
import net.micropact.aea.utility.ScriptObjectHandlerType;
import net.micropact.aea.utility.ScriptObjectLanguageType;

/**
 * Generates a list of Script Objects which do not appear to be being used in entellitrak. For instance, javascript
 * files which are not selected as form event listeners or form element listeners.
 *
 * @author zmiller
 */
public class UnusedScriptObjectsController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        final TextResponse response = etk.createTextResponse();

        response.setContentType(ContentType.HTML);

        /* This query ignores tracking_config because it should not affect the results.
         * Another way of writing this query would be to union together all the places where we use script objects,
         * and MINUS those from the list of script objects. */
        final List<Map<String, Object>> unusedScriptObjects = etk.createSQL("SELECT scriptObject.SCRIPT_ID, scriptObject.SCRIPT_LANGUAGE_TYPE, scriptObject.FULLY_QUALIFIED_SCRIPT_NAME, scriptObject.SCRIPT_HANDLER_TYPE FROM aea_script_pkg_view_sys_only scriptObject WHERE ( /* Advanced Search, Apply Changes, User Event, Deployment */ scriptObject.script_handler_type IN(:advancedSearchEventHandler, :applyChangesEventHandler, :userEventHandler, :deploymentHandler) AND NOT EXISTS (SELECT * FROM etk_system_event_listener systemEventListener WHERE systemEventListener.script_object_id = scriptObject.script_id )) OR ( /* Display Mapping */ scriptObject.script_handler_type = :displayMappingHandler AND NOT EXISTS (SELECT * FROM etk_display_mapping displayMapping WHERE displayMapping.evaluation_script_id = scriptObject.script_id )) OR ( /* Form Element Event Handler, Change Handler, Click Handler */ scriptObject.script_handler_type IN(:formElementEventHandler, :changeHandler, :clickHandler) AND NOT EXISTS (SELECT * FROM etk_form_control_event_handler formControlEventHandler WHERE formControlEventHandler.script_object_id = scriptObject.script_id )) OR ( /* Form Event Handler, New Handler, Read Handler, Save Handler */ scriptObject.script_handler_type IN(:formEventHandler, :newHandler, :readHandler, :saveHandler) AND NOT EXISTS (SELECT * FROM etk_data_form_event_handler dataFormEventHandler WHERE dataFormEventHandler.script_object_id = scriptObject.script_id )) OR ( /* Job Handler */ scriptObject.script_handler_type = :jobHandler AND NOT EXISTS (SELECT * FROM etk_job_custom jobCustom WHERE jobCustom.script_object_id = scriptObject.script_id )) OR ( /* Lookup Handler */ scriptObject.script_handler_type = :lookupHandler AND NOT EXISTS (SELECT * FROM etk_lookup_definition lookupDefinition WHERE lookupDefinition.lookup_source_type IN(:lookupSourceDataObject, :lookupSourceSql, :lookupSourceScript) AND lookupDefinition.sql_script_object_id = scriptObject.script_id )) OR ( /* Page Controller / Step Based Page */ scriptObject.script_handler_type IN (:pageController, :stepBasedPage) AND NOT EXISTS (SELECT * FROM etk_page page WHERE page.controller_script_id = scriptObject.script_id )) OR ( /* Transition Handler */ scriptObject.script_handler_type = :transitionHandler AND NOT EXISTS (SELECT * FROM etk_do_transition doTransition WHERE doTransition.trigger_script_id = scriptObject.script_id )) OR( /* Scan Event Handler, Data Object Event Handler */ scriptObject.script_handler_type IN(:scanEventHandler, :dataObjectEventHandler, :rdoEventHandler) AND NOT EXISTS (SELECT * FROM etk_data_event_listener dataEventListener WHERE dataEventListener.script_object_id = scriptObject.script_id ) ) OR( /* Form Execution */ scriptObject.script_handler_type = :formExecutionHandler AND NOT EXISTS (SELECT * FROM etk_data_form dataForm WHERE dataForm.script_object_id = scriptObject.script_id ) ) OR( /* Filter Handlers*/ scriptObject.script_handler_type IN(:elementFilterHandler, :recordFilterHandler) AND NOT EXISTS(SELECT * FROM etk_filter_handler filterHandler WHERE filterHandler.script_object_id = scriptObject.script_id ) ) OR( /* HTML */ scriptObject.script_language_type = :htmlLanguageType AND NOT EXISTS (SELECT * FROM etk_page page WHERE page.view_script_id = scriptObject.script_id ) ) OR( /* SQL */ scriptObject.script_language_type = :sqlLanguageType AND NOT EXISTS (SELECT * FROM etk_lookup_definition lookupDefinition WHERE lookupDefinition.sql_script_object_id = scriptObject.script_id ) ) OR( /* JAVASCRIPT */ scriptObject.script_language_type = :javascriptLanguageType AND NOT ( EXISTS (SELECT * FROM etk_data_form_event_handler dataFormEventHandler WHERE dataFormEventHandler.script_object_id = scriptObject.script_id ) OR EXISTS (SELECT * FROM etk_form_control_event_handler formControlEventHandler WHERE formControlEventHandler.script_object_id = scriptObject.script_id ) OR EXISTS (SELECT * FROM etk_page page WHERE page.view_script_id = scriptObject.script_id )) ) ORDER BY scriptObject.script_language_type, scriptObject.script_handler_type, scriptObject.fully_qualified_script_name")
                .setParameter("htmlLanguageType", ScriptObjectLanguageType.HTML.getId())
                .setParameter("javascriptLanguageType", ScriptObjectLanguageType.JAVASCRIPT.getId())
                .setParameter("jobHandler", ScriptObjectHandlerType.JOB_HANDLER.getId())
                .setParameter("advancedSearchEventHandler",
                        ScriptObjectHandlerType.ADV_SEARCH_PROCESSOR.getId())
                .setParameter("transitionHandler", ScriptObjectHandlerType.TRANSITION_HANDLER.getId())
                .setParameter("userEventHandler", ScriptObjectHandlerType.USER_EVENT_HANDLER.getId())
                .setParameter("displayMappingHandler", ScriptObjectHandlerType.DISPLAY_MAPPING_HANDLER.getId())
                .setParameter("formElementEventHandler", ScriptObjectHandlerType.FORM_ELEMENT_EVENT_HANDLER.getId())
                .setParameter("formEventHandler", ScriptObjectHandlerType.FORM_EVENT_HANDLER.getId())
                .setParameter("advancedSearchEventType",
                        ScriptObjectHandlerType.ADV_SEARCH_PROCESSOR.getId())
                .setParameter("lookupHandler", ScriptObjectHandlerType.LOOKUP_HANDLER.getId())
                .setParameter("scanEventHandler", ScriptObjectHandlerType.SCAN_EVENT_HANDLER.getId())
                .setParameter("dataObjectEventHandler", ScriptObjectHandlerType.DATA_OBJECT_EVENT_HANDLER.getId())
                .setParameter("rdoEventHandler", ScriptObjectHandlerType.REFERENCE_OBJECT_EVENT_HANDLER.getId())
                .setParameter("pageController", ScriptObjectHandlerType.PAGE_CONTROLLER.getId())
                .setParameter("stepBasedPage", ScriptObjectHandlerType.STEP_BASED_PAGE.getId())
                .setParameter("sqlLanguageType", ScriptObjectLanguageType.SQL.getId())
                .setParameter("lookupSourceDataObject", LookupSourceType.DATA_OBJECT_LOOKUP.getEntellitrakNumber())
                .setParameter("lookupSourceSql", LookupSourceType.QUERY_LOOKUP.getEntellitrakNumber())
                .setParameter("lookupSourceScript", LookupSourceType.SCRIPT_LOOKUP.getEntellitrakNumber())
                .setParameter("applyChangesEventHandler",
                        ScriptObjectHandlerType.APPLY_CHANGES_EVENT_HANDLER.getId())
                .setParameter("formExecutionHandler", ScriptObjectHandlerType.FORM_EXECUTION_HANDLER.getId())
                .setParameter("changeHandler", ScriptObjectHandlerType.CHANGE_HANDLER.getId())
                .setParameter("clickHandler", ScriptObjectHandlerType.CLICK_HANDLER.getId())
                .setParameter("newHandler", ScriptObjectHandlerType.NEW_HANDLER.getId())
                .setParameter("readHandler", ScriptObjectHandlerType.READ_HANDLER.getId())
                .setParameter("saveHandler", ScriptObjectHandlerType.SAVE_HANDLER.getId())
                .setParameter("deploymentHandler", ScriptObjectHandlerType.DEPLOYMENT_HANDLER.getId())
                .setParameter("elementFilterHandler", ScriptObjectHandlerType.ELEMENT_FILTER_HANDLER.getId())
                .setParameter("recordFilterHandler", ScriptObjectHandlerType.RECORD_FILTER_HANDLER.getId())
                .fetchList(); /* SCRIPT_ID, SCRIPT_LANGUAGE_TYPE, FULLY_QUALIFIED_SCRIPT_NAME, SCRIPT_HANDLER_TYPE*/

        for(final Map<String, Object> scriptObject : unusedScriptObjects){
            scriptObject.put("SCRIPT_HANDLER_TYPE",
                    ScriptObjectHandlerType.getById(
                            ((Number) scriptObject.get("SCRIPT_HANDLER_TYPE")).intValue()).getName());
            scriptObject.put("SCRIPT_LANGUAGE_TYPE",
                    ScriptObjectLanguageType.getById(
                            ((Number) scriptObject.get("SCRIPT_LANGUAGE_TYPE")).intValue()).getName());
        }

        response.put("unusedScriptObjects", JsonUtilities.encode(unusedScriptObjects));

        return response;
    }
}

package net.micropact.aea.rf.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

import net.micropact.aea.core.importExport.IImportLogic;
import net.micropact.aea.rf.utility.RfUtility;
import net.micropact.aea.utility.ImportExportUtility;
import net.micropact.aea.utility.Utility;

/**
 * This class actually contains all of the logic for importing the Rules Framework data.
 *
 * @author zmiller
 */
public class RfImportLogic implements IImportLogic{
    private final ExecutionContext etk;

    /**
     * Construct a new {@link RfImportLogic}.
     *
     * @param executionContext entellitrak execution context
     */
    public RfImportLogic(final ExecutionContext executionContext) {
        etk = executionContext;
    }

    /**
     * Imports data into the RF Parameter Type reference data list.
     *
     * @param etk entellitrak execution context.
     * @param document XML document containing the data to be imported.
     */
    private static void importParameterTypes(final ExecutionContext etk, final Document document){
        final List<Map<String, String>> parameterTypes =
                ImportExportUtility.getTable(document, "T_RF_PARAMETER_TYPE");
        for(final Map<String, String> parameterType : parameterTypes){

            final List<Map<String, Object>> matchingParameters = etk.createSQL("SELECT parameterType.id ID FROM t_rf_parameter_type parameterType WHERE parametertype.c_code = :code")
                    .setParameter("code", parameterType.get("C_CODE"))
                    .fetchList(); /*ID*/
            if(matchingParameters.size() == 0){
                //Insert
                etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO t_rf_parameter_type (c_code, c_name, c_order) VALUES(:c_code, :c_name, :c_order)"
                                                         : "INSERT INTO t_rf_parameter_type (id, c_code, c_name, c_order) VALUES(OBJECT_ID.NEXTVAL, :c_code, :c_name, :c_order)")
                          .setParameter("c_code", parameterType.get("C_CODE"))
                          .setParameter("c_name", parameterType.get("C_NAME"))
                          .setParameter("c_order", parameterType.get("C_ORDER"))
                          .execute();
            }else{
                //Update
                etk.createSQL("UPDATE t_rf_parameter_type SET c_name = :c_name, c_order = :c_order WHERE id = :id")
                .setParameter("id", matchingParameters.get(0).get("ID"))
                .setParameter("c_order", parameterType.get("C_ORDER"))
                .setParameter("c_name", parameterType.get("C_NAME"))
                .execute();
            }
        }
    }

    /**
     * Imports the RF Lookup reference data list.
     *
     * @param etk entellitrak execution context.
     * @param document XML document containing the RF Lookup data.
     */
    private static void importLookups(final ExecutionContext etk, final Document document) {
        final List<Map<String, String>> objects = ImportExportUtility.getTable(document, "T_RF_LOOKUP");
        for(final Map<String, String> object : objects){
            final List<Map<String, Object>> matchingObjects = etk.createSQL("SELECT ID FROM t_rf_lookup WHERE c_code = :c_code")
                    .setParameter("c_code", object.get("C_CODE"))
                    .fetchList();
            if(matchingObjects.size() == 0){
                //Insert
                etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO t_rf_lookup(c_code, etk_end_date, c_name, c_sql, etk_start_date) VALUES(:c_code, CONVERT(DATE, :etk_end_date, 101), :c_name, :c_sql, CONVERT(DATE, :etk_start_date, 101))"
                                                         : "INSERT INTO t_rf_lookup(id, c_code, etk_end_date, c_name, c_sql, etk_start_date) VALUES(OBJECT_ID.NEXTVAL, :c_code, TO_DATE(:etk_end_date, 'MM/DD/YYYY'), :c_name, :c_sql, TO_DATE(:etk_start_date, 'MM/DD/YYYY'))")
                     .setParameter("c_code", object.get("C_CODE"))
                     .setParameter("etk_end_date", object.get("ETK_END_DATE"))
                     .setParameter("c_name", object.get("C_NAME"))
                     .setParameter("c_sql", object.get("C_SQL"))
                     .setParameter("etk_start_date", object.get("ETK_START_DATE"))
                     .execute();
            }else{
                //Update
                etk.createSQL(Utility.isSqlServer(etk) ? "UPDATE t_rf_lookup SET etk_end_date = CONVERT(DATE, :etk_end_date, 101), c_name = :c_name, c_sql = :c_sql, etk_start_date = CONVERT(DATE, :etk_start_date, 101) WHERE id = :id"
                                                         : "UPDATE t_rf_lookup SET etk_end_date = TO_DATE(:etk_end_date, 'MM/DD/YYYY'), c_name = :c_name, c_sql = :c_sql, etk_start_date = TO_DATE(:etk_start_date, 'MM/DD/YYYY') WHERE id = :id")
                     .setParameter("id", matchingObjects.get(0).get("ID"))
                     .setParameter("etk_end_date", object.get("ETK_END_DATE"))
                     .setParameter("c_name", object.get("C_NAME"))
                     .setParameter("c_sql", object.get("C_SQL"))
                     .setParameter("etk_start_date", object.get("ETK_START_DATE"))
                     .execute();
            }
        }
    }

    /**
     * Imports RF Script objects into the system.
     *
     * @param etk entellitrak execution context.
     * @param document XML document containing RF Script data.
     */
    private static void importScripts(final ExecutionContext etk, final Document document) {
        final List<Map<String, String>> objects = ImportExportUtility.getTable(document, "T_RF_SCRIPT");
        for(final Map<String, String> object : objects){
            final List<Map<String, Object>> matchingObjects = etk.createSQL("SELECT ID FROM t_rf_script WHERE c_code = :c_code")
                    .setParameter("c_code", object.get("C_CODE"))
                    .fetchList();
            if(matchingObjects.size() == 0){
                //Insert

                etk.getWorkflowService()
                .startWorkflow("object.rfScript",
                        new HashMap<>(
                                Utility.arrayToMap(String.class,
                                        Object.class,
                                        new Object[][]{{"object.rfScript.element.code", object.get("C_CODE")},
                                    {"object.rfScript.element.description", object.get("C_DESCRIPTION")},
                                    {"object.rfScript.element.name", object.get("C_NAME")},
                                    {"object.rfScript.element.scriptObject", object.get("C_SCRIPT_OBJECT")}})));
            }else{
                //Update
                etk.createSQL("UPDATE t_rf_script SET c_description = :c_description, c_name = :c_name, c_script_object = :c_script_object WHERE id = :id")
                .setParameter("id", matchingObjects.get(0).get("ID"))
                .setParameter("c_description", object.get("C_DESCRIPTION"))
                .setParameter("c_name", object.get("C_NAME"))
                .setParameter("c_script_object", object.get("C_SCRIPT_OBJECT"))
                .execute();
            }
        }
    }

    /**
     * Imports RF Script Parameter objects into the system.
     *
     * @param etk entellitrak execution context.
     * @param document XML Document containing user-configurable Rules Framework data.
     * @throws IncorrectResultSizeDataAccessException
     *  If there was an underlying {@link IncorrectResultSizeDataAccessException}.
     */
    private static void importScriptParameters(final ExecutionContext etk, final Document document)
            throws IncorrectResultSizeDataAccessException{
        final List<Map<String, String>> objects = ImportExportUtility.getTable(document, "T_RF_SCRIPT_PARAMETER");
        final List<Map<String, String>> rfScripts = ImportExportUtility.getTable(document, "T_RF_SCRIPT");
        final List<Map<String, String>> lookups = ImportExportUtility.getTable(document, "T_RF_LOOKUP");
        final List<Map<String, String>> parameterTypes =
                ImportExportUtility.getTable(document, "T_RF_PARAMETER_TYPE");
        for(final Map<String, String> object : objects){
            final String scriptCode =
                    ImportExportUtility.lookupValueInListOfMaps(rfScripts, "ID", object.get("ID_PARENT"), "C_CODE");
            final String scriptIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_script rfScript WHERE rfScript.c_code = :scriptCode")
                    .setParameter("scriptCode", scriptCode)
                    .fetchObject().toString();
            final String lookupCode =
                    ImportExportUtility.lookupValueInListOfMaps(lookups, "ID", object.get("C_LOOKUP"), "C_CODE");
            final String lookupIdInNewSystem = lookupCode == null
                    ? null
                    : etk.createSQL("SELECT ID FROM t_rf_lookup WHERE c_code = :lookupCode")
                    .setParameter("lookupCode", lookupCode)
                    .fetchObject().toString();

            final String parameterTypeCode =
                    ImportExportUtility.lookupValueInListOfMaps(parameterTypes,
                            "ID",
                            object.get("C_TYPE"),
                            "C_CODE");
            final String parameterTypeIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_parameter_type WHERE c_code = :parameterTypeCode")
                    .setParameter("parameterTypeCode", parameterTypeCode)
                    .fetchObject().toString();

            final List<Map<String, Object>> matchingObjects = etk.createSQL("SELECT scriptParameter.ID FROM t_rf_script_parameter scriptParameter WHERE scriptParameter.c_code = :scriptParameterCode AND scriptParameter.id_parent = :scriptId")
                    .setParameter("scriptParameterCode", object.get("C_CODE"))
                    .setParameter("scriptId", scriptIdInNewSystem)
                    .fetchList();
            if(matchingObjects.size() == 0){
                //Insert
                etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO t_rf_script_parameter(id_base, id_parent, c_lookup, c_name, c_required, c_type, c_code, c_allow_multiple, c_description, c_order) VALUES(:scriptIdInNewSystem, :scriptIdInNewSystem, :lookupIdInNewSystem, :c_name, :c_required, :c_type, :c_code, :c_allow_multiple, :c_description, :c_order)"
                                                         : "INSERT INTO t_rf_script_parameter(id, id_base, id_parent, c_lookup, c_name, c_required, c_type, c_code, c_allow_multiple, c_description, c_order) VALUES(OBJECT_ID.NEXTVAL, :scriptIdInNewSystem, :scriptIdInNewSystem, :lookupIdInNewSystem, :c_name, :c_required, :c_type, :c_code, :c_allow_multiple, :c_description, :c_order)")
                     .setParameter("c_type", parameterTypeIdInNewSystem)
                     .setParameter("scriptIdInNewSystem", scriptIdInNewSystem)
                     .setParameter("lookupIdInNewSystem", lookupIdInNewSystem)
                     .setParameter("c_code", object.get("C_CODE"))
                     .setParameter("c_required", object.get("C_REQUIRED"))
                     .setParameter("c_name", object.get("C_NAME"))
                     .setParameter("c_allow_multiple", object.get("C_ALLOW_MULTIPLE"))
                     .setParameter("c_description", object.get("C_DESCRIPTION"))
                     .setParameter("c_order", object.get("C_ORDER"))
                     .execute();
            }else{
                //Update
                etk.createSQL("UPDATE t_rf_script_parameter SET id_base = :scriptIdInNewSystem, id_parent = :scriptIdInNewSystem, c_lookup = :lookupIdInNewSystem, c_name = :c_name, c_required = :c_required, c_type = :parameterTypeIdInNewSystem, c_allow_multiple = :c_allow_multiple, c_description = :c_description, c_order = :c_order WHERE id = :id")
                .setParameter("id", matchingObjects.get(0).get("ID"))
                .setParameter("scriptIdInNewSystem", scriptIdInNewSystem)
                .setParameter("lookupIdInNewSystem", lookupIdInNewSystem)
                .setParameter("parameterTypeIdInNewSystem", parameterTypeIdInNewSystem)
                .setParameter("c_name", object.get("C_NAME"))
                .setParameter("c_required", object.get("C_REQUIRED"))
                .setParameter("c_allow_multiple", object.get("C_ALLOW_MULTIPLE"))
                .setParameter("c_description", object.get("C_DESCRIPTION"))
                .setParameter("c_order", object.get("C_ORDER"))
                .execute();
            }
        }
    }

    /**
     * Imports the RF Workflow objects.
     *
     * @param etk entellitark execution context.
     * @param document XML document containing user-configurable Rules Framework data.
     */
    private static void importWorkflows(final ExecutionContext etk, final Document document) {
        final List<Map<String, String>> objects = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        for(final Map<String, String> object : objects){
            final List<Map<String, Object>> matchingObjects = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :c_code")
                    .setParameter("c_code", object.get("C_CODE"))
                    .fetchList();
            if(matchingObjects.size() == 0){
                //Insert
                etk.getWorkflowService().startWorkflow("object.rfWorkflow",
                        new HashMap<>(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                            {"object.rfWorkflow.element.childObject",
                                object.get("C_CHILD_OBJECT")},
                                {"object.rfWorkflow.element.childTransitionElement",
                                    object.get("C_CHILD_TRANSITION_ELEMENT")},
                                    {"object.rfWorkflow.element.code",
                                        object.get("C_CODE")},
                                        {"object.rfWorkflow.element.name",
                                            object.get("C_NAME")},
                                            {"object.rfWorkflow.element.description",
                                                object.get("C_DESCRIPTION")},
                                                {"object.rfWorkflow.element.parentStateElement",
                                                    object.get("C_PARENT_STATE_ELEMENT")},
                                                    {"object.rfWorkflow.element.startStateXCoordinate",
                                                        object.get("C_START_STATE_X_COORDINATE")},
                                                        {"object.rfWorkflow.element.startStateYCoordinate",
                                                            object.get("C_START_STATE_Y_COORDINATE")}})));
            }else{
                //Update
                etk.createSQL("UPDATE t_rf_workflow SET c_child_object = :childObject, c_child_transition_element = :childTransitionElement, c_parent_state_element = :parentStateElement, c_name = :c_name, c_description = :description, c_start_state_x_coordinate = :startStateXCoordinate, c_start_state_y_coordinate = :startStateYCoordinate WHERE id = :id")
                .setParameter("id", matchingObjects.get(0).get("ID"))
                .setParameter("childObject", object.get("C_CHILD_OBJECT"))
                .setParameter("childTransitionElement", object.get("C_CHILD_TRANSITION_ELEMENT"))
                .setParameter("parentStateElement", object.get("C_PARENT_STATE_ELEMENT"))
                .setParameter("startStateXCoordinate", object.get("C_START_STATE_X_COORDINATE"))
                .setParameter("startStateYCoordinate", object.get("C_START_STATE_Y_COORDINATE"))
                .setParameter("c_name", object.get("C_NAME"))
                .setParameter("description", object.get("C_DESCRIPTION"))
                .execute();
            }
        }
    }

    /**
     * Imports the RF State objects.
     *
     * @param etk entellitrak execution context.
     * @param document XML Document containing user-configurable Rules Framework data.
     * @throws IncorrectResultSizeDataAccessException
     *  If there was an underlying {@link IncorrectResultSizeDataAccessException}.
     */
    private static void importStates(final ExecutionContext etk, final Document document)
            throws IncorrectResultSizeDataAccessException{
        final List<Map<String, String>> objects = ImportExportUtility.getTable(document, "T_RF_STATE");
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");

        for(final Map<String, String> object : objects){
            final String workflowCode =
                    ImportExportUtility.lookupValueInListOfMaps(rfWorkflows,
                            "ID",
                            object.get("ID_PARENT"),
                            "C_CODE");
            final String workflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :workflowCode")
                    .setParameter("workflowCode", workflowCode)
                    .fetchObject().toString();

            final List<Map<String, Object>> matchingObjects = etk.createSQL("SELECT rfState.ID FROM t_rf_state rfState WHERE rfState.c_code = :c_code AND rfState.id_parent = :workflowIdInNewSystem")
                    .setParameter("c_code", object.get("C_CODE"))
                    .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                    .fetchList();
            if(matchingObjects.size() == 0){
                //Insert
                etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO t_rf_state (id_base, id_parent, c_code, c_name, c_order, c_end_date, c_start_date, c_x_coordinate, c_y_coordinate, c_description) VALUES(:workflowId, :workflowId, :c_code, :c_name, :c_order, CONVERT(DATE, :c_end_date, 101), CONVERT(DATE, :c_start_date, 101), :c_x_coordinate, :c_y_coordinate, :c_description)"
                                                         : "INSERT INTO t_rf_state (id, id_base, id_parent, c_code, c_name, c_order, c_end_date, c_start_date, c_x_coordinate, c_y_coordinate, c_description) VALUES(OBJECT_ID.NEXTVAL, :workflowId, :workflowId, :c_code, :c_name, :c_order, TO_DATE(:c_end_date, 'MM/DD/YYYY'), TO_DATE(:c_start_date, 'MM/DD/YYYY'), :c_x_coordinate, :c_y_coordinate, :c_description)")
                     .setParameter("c_y_coordinate", object.get("C_Y_COORDINATE"))
                     .setParameter("c_start_date", object.get("C_START_DATE"))
                     .setParameter("c_order", object.get("C_ORDER"))
                     .setParameter("workflowId", workflowIdInNewSystem)
                     .setParameter("c_end_date", object.get("C_END_DATE"))
                     .setParameter("c_code", object.get("C_CODE"))
                     .setParameter("c_x_coordinate", object.get("C_X_COORDINATE"))
                     .setParameter("c_name", object.get("C_NAME"))
                     .setParameter("c_description", object.get("C_DESCRIPTION"))
                     .execute();
            }else{
                //Update
                etk.createSQL(Utility.isSqlServer(etk) ? "UPDATE t_rf_state SET c_name= :c_name, c_order = :c_order, c_end_date = CONVERT(DATE, :c_end_date, 101), c_start_date = CONVERT(DATE, :c_start_date, 101), c_x_coordinate = :c_x_coordinate, c_y_coordinate = :c_y_coordinate, c_description = :c_description WHERE id = :id"
                                                         : "UPDATE t_rf_state SET c_name= :c_name, c_order = :c_order, c_end_date = TO_DATE(:c_end_date, 'MM/DD/YYYY'), c_start_date = TO_DATE(:c_start_date, 'MM/DD/YYYY'), c_x_coordinate = :c_x_coordinate, c_y_coordinate = :c_y_coordinate, c_description = :c_description WHERE id = :id")
                     .setParameter("id", matchingObjects.get(0).get("ID"))
                     .setParameter("c_y_coordinate", object.get("C_Y_COORDINATE"))
                     .setParameter("c_start_date", object.get("C_START_DATE"))
                     .setParameter("c_order", object.get("C_ORDER"))
                     .setParameter("workflowId", workflowIdInNewSystem)
                     .setParameter("c_end_date", object.get("C_END_DATE"))
                     .setParameter("c_x_coordinate", object.get("C_X_COORDINATE"))
                     .setParameter("c_name", object.get("C_NAME"))
                     .setParameter("c_description", object.get("C_DESCRIPTION"))
                     .execute();
            }
        }
    }

    /**
     * Imports RF Transition objects into the system.
     *
     * @param etk entellitrak execution context.
     * @param document XML document containing user-configurable Rules Framework data.
     * @throws IncorrectResultSizeDataAccessException
     *  If there was an underlying {@link IncorrectResultSizeDataAccessException}.
     */
    private static void importTransitions(final ExecutionContext etk, final Document document)
            throws IncorrectResultSizeDataAccessException{
        final List<Map<String, String>> objects = ImportExportUtility.getTable(document, "T_RF_TRANSITION");
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        final List<Map<String, String>> states = ImportExportUtility.getTable(document, "T_RF_STATE");
        for(final Map<String, String> object : objects){
            final String workflowCode =
                    ImportExportUtility.lookupValueInListOfMaps(rfWorkflows,
                            "ID",
                            object.get("ID_PARENT"),
                            "C_CODE");
            final String workflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :workflowCode")
                    .setParameter("workflowCode", workflowCode)
                    .fetchObject().toString();
            final String toStateCode =
                    ImportExportUtility.lookupValueInListOfMaps(states, "ID", object.get("C_TO_STATE"), "C_CODE");
            final List<Map<String, Object>> toStateIds = etk.createSQL("SELECT ID FROM t_rf_state rfState WHERE c_code = :toStateCode AND id_parent = :workflowIdInNewSystem")
                    .setParameter("toStateCode", toStateCode)
                    .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                    .fetchList();
            final String toStateId = toStateIds.isEmpty() ? null : toStateIds.get(0).get("ID").toString();

            final List<Map<String, Object>> matchingObjects = etk.createSQL("SELECT ID FROM t_rf_transition WHERE c_code = :c_code AND id_parent = :workflowIdInNewSystem")
                    .setParameter("c_code", object.get("C_CODE"))
                    .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                    .fetchList();
            if(matchingObjects.size() == 0){
                //Insert
                etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO t_rf_transition(id_base, id_parent, c_code, c_name, c_order, c_to_state, c_initial_transition, c_end_date, c_start_date, c_description) VALUES(:workflowIdInNewSystem, :workflowIdInNewSystem, :c_code, :c_name, :c_order, :toStateId, :c_initial_transition, CONVERT(DATE, :c_end_date, 101), CONVERT(DATE, :c_start_date, 101), :c_description)"
                                                         : "INSERT INTO t_rf_transition(id, id_base, id_parent, c_code, c_name, c_order, c_to_state, c_initial_transition, c_end_date, c_start_date, c_description) VALUES(OBJECT_ID.NEXTVAL, :workflowIdInNewSystem, :workflowIdInNewSystem, :c_code, :c_name, :c_order, :toStateId, :c_initial_transition, TO_DATE(:c_end_date, 'MM/DD/YYYY'), TO_DATE(:c_start_date, 'MM/DD/YYYY'), :c_description)")
                     .setParameter("c_start_date", object.get("C_START_DATE"))
                     .setParameter("c_order", object.get("C_ORDER"))
                     .setParameter("toStateId", toStateId)
                     .setParameter("c_initial_transition", object.get("C_INITIAL_TRANSITION"))
                     .setParameter("c_end_date", object.get("C_END_DATE"))
                     .setParameter("c_code", object.get("C_CODE"))
                     .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                     .setParameter("c_name", object.get("C_NAME"))
                     .setParameter("c_description", object.get("C_DESCRIPTION"))
                                 .execute();
            }else{
                //Update
                etk.createSQL(Utility.isSqlServer(etk) ? "UPDATE t_rf_transition SET c_name = :c_name, c_order = :c_order, c_to_state = :c_to_state, c_initial_transition = :c_initial_transition, c_end_date = CONVERT(DATE, :c_end_date, 101), c_start_date = CONVERT(DATE, :c_start_date, 101), c_description = :c_description WHERE id = :id"
                                                         : "UPDATE t_rf_transition SET c_name = :c_name, c_order = :c_order, c_to_state = :c_to_state, c_initial_transition = :c_initial_transition, c_end_date = TO_DATE(:c_end_date, 'MM/DD/YYYY'), c_start_date = TO_DATE(:c_start_date, 'MM/DD/YYYY'), c_description = :c_description WHERE id = :id")
                     .setParameter("id", matchingObjects.get(0).get("ID"))
                     .setParameter("c_start_date", object.get("C_START_DATE"))
                     .setParameter("c_order", object.get("C_ORDER"))
                     .setParameter("c_initial_transition", object.get("C_INITIAL_TRANSITION"))
                     .setParameter("c_to_state", toStateId)
                     .setParameter("c_end_date", object.get("C_END_DATE"))
                     .setParameter("c_name", object.get("C_NAME"))
                     .setParameter("c_description", object.get("C_DESCRIPTION"))
                     .execute();
            }
        }
    }

    /**
     * Imports RF Workflow Effect objects into the system.
     *
     * @param etk entellitrak execution context.
     * @param document XML Document containing user-configurable Rules Framework data.
     * @throws IncorrectResultSizeDataAccessException
     *  If there was an underlying {@link IncorrectResultSizeDataAccessException}.
     */
    private static void importWorkflowEffects(final ExecutionContext etk, final Document document)
            throws IncorrectResultSizeDataAccessException {
        final List<Map<String, String>> objects = ImportExportUtility.getTable(document, "T_RF_WORKFLOW_EFFECT");
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        final List<Map<String, String>> rfScripts = ImportExportUtility.getTable(document, "T_RF_SCRIPT");
        for(final Map<String, String> object : objects){
            final String workflowCode =
                    ImportExportUtility.lookupValueInListOfMaps(rfWorkflows,
                            "ID",
                            object.get("ID_PARENT"),
                            "C_CODE");
            final String workflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :workflowCode")
                    .setParameter("workflowCode", workflowCode)
                    .fetchObject().toString();
            final String rfScriptCode =
                    ImportExportUtility.lookupValueInListOfMaps(rfScripts, "ID", object.get("C_SCRIPT"), "C_CODE");
            final String rfScriptIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_script WHERE c_code = :rfScriptCode")
                    .setParameter("rfScriptCode", rfScriptCode)
                    .fetchObject().toString();

            final List<Map<String, Object>> matchingObjects = etk.createSQL("SELECT ID FROM t_rf_workflow_effect WHERE c_code = :c_code AND id_parent = :workflowIdInNewSystem")
                    .setParameter("c_code", object.get("C_CODE"))
                    .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                    .fetchList();
            if(matchingObjects.size() == 0){
                //Insert
                etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO t_rf_workflow_effect(id_base, id_parent, c_script, c_name, c_execution_order, c_code) VALUES(:workflowId, :workflowId, :scriptId, :c_name, :c_execution_order, :c_code)"
                                                         : "INSERT INTO t_rf_workflow_effect(id, id_base, id_parent, c_script, c_name, c_execution_order, c_code) VALUES(OBJECT_ID.NEXTVAL, :workflowId, :workflowId, :scriptId, :c_name, :c_execution_order, :c_code)")
                     .setParameter("c_execution_order", object.get("C_EXECUTION_ORDER"))
                     .setParameter("workflowId", workflowIdInNewSystem)
                     .setParameter("c_code", object.get("C_CODE"))
                     .setParameter("scriptId", rfScriptIdInNewSystem)
                     .setParameter("c_name", object.get("C_NAME"))
                     .execute();
            }else{
                //Update
                etk.createSQL("UPDATE t_rf_workflow_effect SET c_script = :scriptId, c_name = :c_name, c_execution_order= :c_execution_order WHERE id = :id")
                .setParameter("id", matchingObjects.get(0).get("ID"))
                .setParameter("c_execution_order", object.get("C_EXECUTION_ORDER"))
                .setParameter("workflowId", workflowIdInNewSystem)
                .setParameter("scriptId", rfScriptIdInNewSystem)
                .setParameter("c_name", object.get("C_NAME"))
                .execute();
            }
        }
    }

    /**
     * Imports data into the M_RF_TRANSITION_FROM_STATE table. Takes care of deleting any old values.
     *
     * @param etk entellitrak execution context.
     * @param document XML document containing user-configurable Rules Framework data.
     * @throws IncorrectResultSizeDataAccessException
     *  If there was an underlying {@link IncorrectResultSizeDataAccessException}.
     */
    private static void importTransitionFromStates(final ExecutionContext etk, final Document document)
            throws IncorrectResultSizeDataAccessException {
        final List<Map<String, String>> objects =
                ImportExportUtility.getTable(document, "M_RF_TRANSITION_FROM_STATE");
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        final List<Map<String, String>> rfTransitions = ImportExportUtility.getTable(document, "T_RF_TRANSITION");
        final List<Map<String, String>> rfStates = ImportExportUtility.getTable(document, "T_RF_STATE");

        /*Delete any old entries*/
        for(final Map<String, String> rfTransition : rfTransitions){
            final String rfTransitionCode = rfTransition.get("C_CODE");

            final String workflowCode =
                    ImportExportUtility.lookupValueInListOfMaps(
                            rfWorkflows,
                            "ID",
                            rfTransition.get("ID_PARENT"),
                            "C_CODE");
            final String workflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :workflowCode")
                    .setParameter("workflowCode", workflowCode)
                    .fetchObject().toString();

            final String rfTransitionIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_transition WHERE id_base = :workflowIdInNewSystem AND c_code = :transitionCode")
                    .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                    .setParameter("transitionCode", rfTransitionCode)
                    .fetchObject().toString();


            etk.createSQL("DELETE FROM m_rf_transition_from_state WHERE id_owner = :transitionIdInNewSystem")
            .setParameter("transitionIdInNewSystem", rfTransitionIdInNewSystem)
            .execute();
        }

        for(final Map<String, String> object : objects){
            final Map<String, String> rfTransition =
                    ImportExportUtility.lookupMapByKey(rfTransitions, "ID", object.get("ID_OWNER"));
            final String rfTransitionCode = rfTransition.get("C_CODE");
            final String workflowCode =
                    ImportExportUtility.lookupValueInListOfMaps(
                            rfWorkflows,
                            "ID",
                            rfTransition.get("ID_PARENT"),
                            "C_CODE");
            final String workflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :workflowCode")
                    .setParameter("workflowCode", workflowCode)
                    .fetchObject().toString();
            final String rfTransitionIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_transition WHERE id_base = :workflowIdInNewSystem AND c_code = :transitionCode")
                    .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                    .setParameter("transitionCode", rfTransitionCode)
                    .fetchObject().toString();
            final String fromStateCode =
                    ImportExportUtility.lookupValueInListOfMaps(rfStates,
                            "ID",
                            object.get("C_FROM_STATE"),
                            "C_CODE");
            final String fromStateIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_state WHERE id_base = :workflowIdInNewSystem AND c_code = :fromStateCode")
                    .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                    .setParameter("fromStateCode", fromStateCode)
                    .fetchObject().toString();

            //Insert
            etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO m_rf_transition_from_state(id_owner, list_order, c_from_state) VALUES(:rfTransitionIdInNewSystem, :list_order, :fromStateInNewSystem)"
                                                     : "INSERT INTO m_rf_transition_from_state(id, id_owner, list_order, c_from_state) VALUES(OBJECT_ID.NEXTVAL, :rfTransitionIdInNewSystem, :list_order, :fromStateInNewSystem)")
                 .setParameter("rfTransitionIdInNewSystem", rfTransitionIdInNewSystem)
                 .setParameter("list_order", object.get("LIST_ORDER"))
                 .setParameter("fromStateInNewSystem", fromStateIdInNewSystem)
                 .execute();
        }
    }

    /**
     * Imports data for the M_RF_TRANSITION_ROLE table. Takes care of deleting any old values.
     *
     * @param etk entellitrak execution context.
     * @param document XML document containing the user-configurable Rules Framework data.
     * @throws IncorrectResultSizeDataAccessException
     *  If there was an underlying {@link IncorrectResultSizeDataAccessException}.
     */
    private static void importTransitionRoles(final ExecutionContext etk, final Document document)
            throws IncorrectResultSizeDataAccessException{
        final List<Map<String, String>> objects = ImportExportUtility.getTable(document, "M_RF_TRANSITION_ROLE");
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        final List<Map<String, String>> rfTransitions = ImportExportUtility.getTable(document, "T_RF_TRANSITION");
        final List<Map<String, String>> roles = ImportExportUtility.getTable(document, "ETK_ROLE");

        /*Delete any old entries*/
        for(final Map<String, String> rfTransition : rfTransitions){
            final String rfTransitionCode = rfTransition.get("C_CODE");

            final String workflowCode =
                    ImportExportUtility.lookupValueInListOfMaps(
                            rfWorkflows,
                            "ID",
                            rfTransition.get("ID_PARENT"),
                            "C_CODE");
            final String workflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :workflowCode")
                    .setParameter("workflowCode", workflowCode)
                    .fetchObject().toString();

            final String rfTransitionIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_transition WHERE id_base = :workflowIdInNewSystem AND c_code = :transitionCode")
                    .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                    .setParameter("transitionCode", rfTransitionCode)
                    .fetchObject().toString();


            etk.createSQL("DELETE FROM m_rf_transition_role WHERE id_owner = :transitionIdInNewSystem")
            .setParameter("transitionIdInNewSystem", rfTransitionIdInNewSystem)
            .execute();
        }

        for(final Map<String, String> object : objects){
            final Map<String, String> rfTransition =
                    ImportExportUtility.lookupMapByKey(rfTransitions, "ID", object.get("ID_OWNER"));
            final String rfTransitionCode = rfTransition.get("C_CODE");
            final String workflowCode =
                    ImportExportUtility.lookupValueInListOfMaps(
                            rfWorkflows,
                            "ID",
                            rfTransition.get("ID_PARENT"),
                            "C_CODE");
            final String workflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :workflowCode")
                    .setParameter("workflowCode", workflowCode)
                    .fetchObject().toString();
            final String rfTransitionIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_transition WHERE id_base = :workflowIdInNewSystem AND c_code = :transitionCode")
                    .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                    .setParameter("transitionCode", rfTransitionCode)
                    .fetchObject().toString();
            final String roleBusinessKey =
                    ImportExportUtility.lookupValueInListOfMaps(roles,
                            "ROLE_ID",
                            object.get("C_ROLE"),
                            "BUSINESS_KEY");
            final Object roleIdInNewSystem = etk.createSQL("SELECT ROLE_ID FROM etk_role WHERE business_key = :roleBusinessKey")
                    .setParameter("roleBusinessKey", roleBusinessKey)
                    .returnEmptyResultSetAs(null)
                    .fetchObject();

            if(roleIdInNewSystem == null){ // This check could/should be moved up to the top of this loop.
                Utility.aeaLog(etk,
                        String.format("WARNING: Attempting to import Rules Framework Transition Roles. The import file contains a role with business key \"%s\", but that role does not appear to exist in this system. The import will skip this role",
                                roleBusinessKey));
            } else {
                //Insert
                etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO m_rf_transition_role (id_owner, list_order, c_role) VALUES(:rfTransitionIdInNewSystem, :list_order, :roleIdInNewSystem)"
                                                         : "INSERT INTO m_rf_transition_role (id, id_owner, list_order, c_role) VALUES(OBJECT_ID.NEXTVAL, :rfTransitionIdInNewSystem, :list_order, :roleIdInNewSystem)")
                     .setParameter("rfTransitionIdInNewSystem", rfTransitionIdInNewSystem)
                     .setParameter("list_order", object.get("LIST_ORDER"))
                     .setParameter("roleIdInNewSystem", roleIdInNewSystem)
                     .execute();
            }
        }
    }

    /**
     * Imports entries for the M_RF_EFFECT_TRANSITION table.
     *
     * @param etk entellitrak execution context.
     * @param document XML Document containing the user-configurable Rules Framework data.
     * @throws IncorrectResultSizeDataAccessException
     *  If there was an underling {@link IncorrectResultSizeDataAccessException}.
     */
    private static void importEffectTransitions(final ExecutionContext etk, final Document document)
            throws IncorrectResultSizeDataAccessException {
        final List<Map<String, String>> objects = ImportExportUtility.getTable(document, "M_RF_EFFECT_TRANSITION");
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        final List<Map<String, String>> rfTransitions = ImportExportUtility.getTable(document, "T_RF_TRANSITION");
        final List<Map<String, String>> rfEffects = ImportExportUtility.getTable(document, "T_RF_WORKFLOW_EFFECT");

        /*Delete any old entries*/
        for(final Map<String, String> rfEffect : rfEffects){
            final String rfEffectCode = rfEffect.get("C_CODE");

            final String workflowCode =
                    ImportExportUtility.lookupValueInListOfMaps(rfWorkflows,
                            "ID",
                            rfEffect.get("ID_PARENT"),
                            "C_CODE");
            final String workflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :workflowCode")
                    .setParameter("workflowCode", workflowCode)
                    .fetchObject().toString();

            final String rfEffectIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow_effect WHERE id_base = :workflowIdInNewSystem AND c_code = :effectCode")
                    .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                    .setParameter("effectCode", rfEffectCode)
                    .fetchObject().toString();


            etk.createSQL("DELETE FROM m_rf_effect_transition WHERE id_owner = :effectIdInNewSystem")
            .setParameter("effectIdInNewSystem", rfEffectIdInNewSystem)
            .execute();
        }

        for(final Map<String, String> object : objects){
            final Map<String, String> rfEffect =
                    ImportExportUtility.lookupMapByKey(rfEffects, "ID", object.get("ID_OWNER"));
            final String rfEffectCode = rfEffect.get("C_CODE");
            final String workflowCode =
                    ImportExportUtility.lookupValueInListOfMaps(rfWorkflows,
                            "ID",
                            rfEffect.get("ID_PARENT"),
                            "C_CODE");
            final String workflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :workflowCode")
                    .setParameter("workflowCode", workflowCode)
                    .fetchObject().toString();
            final String rfEffectIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow_effect WHERE id_base = :workflowIdInNewSystem AND c_code = :effectCode")
                    .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                    .setParameter("effectCode", rfEffectCode)
                    .fetchObject().toString();
            final String transitionCode =
                    ImportExportUtility.lookupValueInListOfMaps(
                            rfTransitions,
                            "ID",
                            object.get("C_TRANSITION"),
                            "C_CODE");
            final String transitionIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_transition WHERE id_base = :workflowIdInNewSystem AND c_code = :transitionCode")
                    .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                    .setParameter("transitionCode", transitionCode)
                    .fetchObject().toString();

            //Insert
            etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO m_rf_effect_transition(id_owner, list_order, c_transition) VALUES(:effectIdInNewSystem, :list_order, :transitionIdInNewSystem)"
                                                     : "INSERT INTO m_rf_effect_transition(id, id_owner, list_order, c_transition) VALUES(OBJECT_ID.NEXTVAL, :effectIdInNewSystem, :list_order, :transitionIdInNewSystem)")
                 .setParameter("effectIdInNewSystem", rfEffectIdInNewSystem)
                 .setParameter("list_order", object.get("LIST_ORDER"))
                 .setParameter("transitionIdInNewSystem", transitionIdInNewSystem)
                 .execute();
        }
    }

    /**
     * Imports the RF Script Parameter Value objects.
     *
     * @param etk entellitrak execution context.
     * @param document XML document containing the Rules Framework information to be imported
     * @throws IncorrectResultSizeDataAccessException
     *  If there was an underlying {@link IncorrectResultSizeDataAccessException}.
     */
    private static void importScriptParameterValues(final ExecutionContext etk, final Document document)
            throws IncorrectResultSizeDataAccessException {
        final List<Map<String, String>> objects =
                ImportExportUtility.getTable(document, "T_RF_SCRIPT_PARAMETER_VALUE");
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        final List<Map<String, String>> rfWorkflowEffects =
                ImportExportUtility.getTable(document, "T_RF_WORKFLOW_EFFECT");
        final List<Map<String, String>> rfScripts = ImportExportUtility.getTable(document, "T_RF_SCRIPT");
        final List<Map<String, String>> rfScriptParameters =
                ImportExportUtility.getTable(document, "T_RF_SCRIPT_PARAMETER");

        /*Delete any old entries*/
        for(final Map<String, String> rfWorkflow : rfWorkflows){
            final String rfWorkflowCode = rfWorkflow.get("C_CODE");
            final String rfWorkflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :rfWorkflowCode")
                    .setParameter("rfWorkflowCode", rfWorkflowCode)
                    .fetchObject().toString();

            etk.createSQL("DELETE FROM t_rf_script_parameter_value WHERE id_base = :rfWorkflowIdInNewSystem")
            .setParameter("rfWorkflowIdInNewSystem", rfWorkflowIdInNewSystem)
            .execute();
        }

        for(final Map<String, String> object : objects){
            final String rfWorkflowCode =
                    ImportExportUtility.lookupValueInListOfMaps(rfWorkflows, "ID", object.get("ID_BASE"), "C_CODE");
            final String rfWorkflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :rfWorkflowCode")
                    .setParameter("rfWorkflowCode", rfWorkflowCode)
                    .fetchObject().toString();
            final String rfWorkflowEfectCode =
                    ImportExportUtility.lookupValueInListOfMaps(
                            rfWorkflowEffects,
                            "ID",
                            object.get("ID_PARENT"),
                            "C_CODE");
            final String rfWorkflowEffectIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow_effect effect WHERE c_code = :effectCode AND id_parent = :workflowIdInNewSystem")
                    .setParameter("effectCode", rfWorkflowEfectCode)
                    .setParameter("workflowIdInNewSystem", rfWorkflowIdInNewSystem)
                    .fetchObject().toString();
            final Map<String, String> rfScriptParameter =
                    ImportExportUtility.lookupMapByKey(rfScriptParameters, "ID", object.get("C_SCRIPT_PARAMETER"));
            final String rfScriptCode =
                    ImportExportUtility.lookupValueInListOfMaps(rfScripts,
                            "ID",
                            rfScriptParameter.get("ID_PARENT"),
                            "C_CODE");
            final String rfScriptIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_script WHERE c_code = :rfScriptCode")
                    .setParameter("rfScriptCode", rfScriptCode)
                    .fetchObject().toString();
            final String rfScriptParameterIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_script_parameter WHERE c_code = :rfScriptParameterCode AND id_parent = :rfScriptIdInNewSystem")
                    .setParameter("rfScriptParameterCode", rfScriptParameter.get("C_CODE"))
                    .setParameter("rfScriptIdInNewSystem", rfScriptIdInNewSystem)
                    .fetchObject().toString();

            //Insert
            etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO t_rf_script_parameter_value(id_base, id_parent, c_script_parameter, c_value) VALUES(:workflowIdInNewSystem, :rfWorkflowEffectIdInNewSystem, :scriptParameterIdInNewSystem, :c_value)"
                                                     : "INSERT INTO t_rf_script_parameter_value(id, id_base, id_parent, c_script_parameter, c_value) VALUES(OBJECT_ID.NEXTVAL, :workflowIdInNewSystem, :rfWorkflowEffectIdInNewSystem, :scriptParameterIdInNewSystem, :c_value)")
                 .setParameter("rfWorkflowEffectIdInNewSystem", rfWorkflowEffectIdInNewSystem)
                 .setParameter("c_value", object.get("C_VALUE"))
                 .setParameter("scriptParameterIdInNewSystem", rfScriptParameterIdInNewSystem)
                 .setParameter("workflowIdInNewSystem", rfWorkflowIdInNewSystem)
                 .execute();
        }
    }

    /**
     * Imports RF Workflow Parameter objects into the system.
     *
     * @param etk entellitrak execution context.
     * @param document XML Document containing user-configurable Rules Framework data.
     *
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}.
     */
    private static void importWorkflowParameters(final ExecutionContext etk, final Document document)
            throws IncorrectResultSizeDataAccessException {
        final List<Map<String, String>> objects = ImportExportUtility.getTable(document, "T_RF_WORKFLOW_PARAMETER");
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        final List<Map<String, String>> lookups = ImportExportUtility.getTable(document, "T_RF_LOOKUP");
        final List<Map<String, String>> parameterTypes =
                ImportExportUtility.getTable(document, "T_RF_PARAMETER_TYPE");
        for(final Map<String, String> object : objects){
            final String workflowCode =
                    ImportExportUtility.lookupValueInListOfMaps(rfWorkflows,
                            "ID",
                            object.get("ID_PARENT"),
                            "C_CODE");
            final String workflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow rfWorkflow WHERE rfWorkflow.c_code = :workflowCode")
                    .setParameter("workflowCode", workflowCode)
                    .fetchObject().toString();
            final String lookupCode =
                    ImportExportUtility.lookupValueInListOfMaps(lookups, "ID", object.get("C_LOOKUP"), "C_CODE");
            final String lookupIdInNewSystem = lookupCode == null
                    ? null
                    : etk.createSQL("SELECT ID FROM t_rf_lookup WHERE c_code = :lookupCode")
                    .setParameter("lookupCode", lookupCode)
                    .fetchObject().toString();

            final String parameterTypeCode = ImportExportUtility.lookupValueInListOfMaps(parameterTypes,
                    "ID",
                    object.get("C_TYPE"),
                    "C_CODE");
            final String parameterTypeIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_parameter_type WHERE c_code = :parameterTypeCode")
                    .setParameter("parameterTypeCode", parameterTypeCode)
                    .fetchObject().toString();

            final List<Map<String, Object>> matchingObjects = etk.createSQL("SELECT workflowParameter.ID FROM t_rf_workflow_parameter workflowParameter WHERE workflowParameter.c_code  = :workflowParameterCode AND workflowParameter.id_parent = :workflowId")
                    .setParameter("workflowParameterCode", object.get("C_CODE"))
                    .setParameter("workflowId", workflowIdInNewSystem)
                    .fetchList();
            if(matchingObjects.size() == 0){
                //Insert
                etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO t_rf_workflow_parameter(id_base, id_parent, c_lookup, c_name, c_required, c_type, c_code, c_allow_multiple, c_description, c_order) VALUES(:workflowIdInNewSystem, :workflowIdInNewSystem, :lookupIdInNewSystem, :c_name, :c_required, :c_type, :c_code, :c_allow_multiple, :c_description, :c_order)"
                                                         : "INSERT INTO t_rf_workflow_parameter(id, id_base, id_parent, c_lookup, c_name, c_required, c_type, c_code, c_allow_multiple, c_description, c_order) VALUES(OBJECT_ID.NEXTVAL, :workflowIdInNewSystem, :workflowIdInNewSystem, :lookupIdInNewSystem, :c_name, :c_required, :c_type, :c_code, :c_allow_multiple, :c_description, :c_order)")
                                                         .setParameter("c_type", parameterTypeIdInNewSystem)
                     .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                     .setParameter("lookupIdInNewSystem", lookupIdInNewSystem)
                     .setParameter("c_code", object.get("C_CODE"))
                     .setParameter("c_required", object.get("C_REQUIRED"))
                     .setParameter("c_name", object.get("C_NAME"))
                     .setParameter("c_allow_multiple", object.get("C_ALLOW_MULTIPLE"))
                     .setParameter("c_description", object.get("C_DESCRIPTION"))
                     .setParameter("c_order", object.get("C_ORDER"))
                     .execute();
            }else{
                //Update
                etk.createSQL("UPDATE t_rf_workflow_parameter SET id_base = :workflowIdInNewSystem, id_parent = :workflowIdInNewSystem, c_lookup = :lookupIdInNewSystem, c_name = :c_name, c_required = :c_required, c_type = :parameterTypeIdInNewSystem, c_allow_multiple = :c_allow_multiple, c_description = :c_description, c_order = :c_order WHERE id = :id")
                .setParameter("id", matchingObjects.get(0).get("ID"))
                .setParameter("workflowIdInNewSystem", workflowIdInNewSystem)
                .setParameter("lookupIdInNewSystem", lookupIdInNewSystem)
                .setParameter("parameterTypeIdInNewSystem", parameterTypeIdInNewSystem)
                .setParameter("c_name", object.get("C_NAME"))
                .setParameter("c_required", object.get("C_REQUIRED"))
                .setParameter("c_allow_multiple", object.get("C_ALLOW_MULTIPLE"))
                .setParameter("c_description", object.get("C_DESCRIPTION"))
                .setParameter("c_order", object.get("C_ORDER"))
                .execute();
            }
        }
    }

    /**
     * Imports the RF Transition Parameter Value objects.
     *
     * @param etk entellitrak execution context.
     * @param document XML document containing the Rules Framework information to be imported
     *
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}.
     */
    private static void importTransitionParameterValues(final ExecutionContext etk, final Document document)
            throws IncorrectResultSizeDataAccessException {
        final List<Map<String, String>> objects =
                ImportExportUtility.getTable(document, "T_RF_TRANSITION_PARAMETER_VALU");
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        final List<Map<String, String>> rfTransition =
                ImportExportUtility.getTable(document, "T_RF_TRANSITION");
        final List<Map<String, String>> rfWorkflowParameters =
                ImportExportUtility.getTable(document, "T_RF_WORKFLOW_PARAMETER");

        /*Delete any old entries*/
        for(final Map<String, String> rfWorkflow : rfWorkflows){
            final String rfWorkflowCode = rfWorkflow.get("C_CODE");
            final String rfWorkflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :rfWorkflowCode")
                    .setParameter("rfWorkflowCode", rfWorkflowCode)
                    .fetchObject().toString();

            etk.createSQL("DELETE FROM t_rf_transition_parameter_valu WHERE id_base = :rfWorkflowIdInNewSystem")
            .setParameter("rfWorkflowIdInNewSystem", rfWorkflowIdInNewSystem)
            .execute();
        }

        for(final Map<String, String> object : objects){
            final String rfWorkflowCode =
                    ImportExportUtility.lookupValueInListOfMaps(rfWorkflows, "ID", object.get("ID_BASE"), "C_CODE");
            final String rfWorkflowIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow WHERE c_code = :rfWorkflowCode")
                    .setParameter("rfWorkflowCode", rfWorkflowCode)
                    .fetchObject().toString();
            final String rfTransitionCode =
                    ImportExportUtility.lookupValueInListOfMaps(
                            rfTransition,
                            "ID",
                            object.get("ID_PARENT"),
                            "C_CODE");
            final String rfTransitionIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_transition transition WHERE c_code = :transitionCode AND id_parent = :workflowIdInNewSystem")
                    .setParameter("transitionCode", rfTransitionCode)
                    .setParameter("workflowIdInNewSystem", rfWorkflowIdInNewSystem)
                    .fetchObject().toString();
            final Map<String, String> rfWorkflowParameter = ImportExportUtility.lookupMapByKey(rfWorkflowParameters,
                    "ID",
                    object.get("C_WORKFLOW_PARAMETER"));
            final String rfWorkflowParameterIdInNewSystem = etk.createSQL("SELECT ID FROM t_rf_workflow_parameter WHERE c_code = :rfWorkflowParameterCode AND id_parent = :workflowIdInNewSystem")
                    .setParameter("rfWorkflowParameterCode", rfWorkflowParameter.get("C_CODE"))
                    .setParameter("workflowIdInNewSystem", rfWorkflowIdInNewSystem)
                    .fetchObject().toString();

            //Insert
            etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO t_rf_transition_parameter_valu(id_base, id_parent, c_workflow_parameter, c_value) VALUES(:workflowIdInNewSystem, :rfTransitionIdInNewSystem, :workflowParameterIdInNewSystem, :c_value)"
                                                     : "INSERT INTO t_rf_transition_parameter_valu(id, id_base, id_parent, c_workflow_parameter, c_value) VALUES(OBJECT_ID.NEXTVAL, :workflowIdInNewSystem, :rfTransitionIdInNewSystem, :workflowParameterIdInNewSystem, :c_value)")
                 .setParameter("rfTransitionIdInNewSystem", rfTransitionIdInNewSystem)
                 .setParameter("c_value", object.get("C_VALUE"))
                 .setParameter("workflowParameterIdInNewSystem", rfWorkflowParameterIdInNewSystem)
                 .setParameter("workflowIdInNewSystem", rfWorkflowIdInNewSystem)
                 .execute();
        }
    }

    /**
     * Deletes all RF States which need to be removed for this import.
     *
     * @param etk entellitrak execution context.
     * @param document XML document containing information on the Rules Framework objects which are
     *  going to be imported.
     */
    private static void deleteStates(final ExecutionContext etk, final Document document) {
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        final List<Map<String, String>> rfStates = ImportExportUtility.getTable(document, "T_RF_STATE");

        for(final Map<String, String> rfWorkflow : rfWorkflows){
            final List<String> workflowStates = new LinkedList<>();
            workflowStates.add("0");
            for(final Map<String, String> rfState : rfStates){
                if(rfWorkflow.get("ID").equals(rfState.get("ID_PARENT"))){
                    workflowStates.add(rfState.get("C_CODE"));
                }
            }

            etk.createSQL("DELETE FROM t_rf_state WHERE id_parent IN(SELECT id FROM t_rf_workflow WHERE c_code = :rfWorkflowCode) AND c_code NOT IN(:rfStates)")
            .setParameter("rfWorkflowCode", rfWorkflow.get("C_CODE"))
            .setParameter("rfStates", workflowStates)
            .execute();
        }
    }

    /**
     * Deletes all RF Transitions which belong to an RF Workflow that is being imported,
     * but is not in the list of ones in the new XML file.
     *
     * @param etk entellitrak execution context
     * @param document XML document containing user-configurable Rules Framework data.
     */
    private static void deleteTransitions(final ExecutionContext etk, final Document document) {
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        final List<Map<String, String>> rfTransitions = ImportExportUtility.getTable(document, "T_RF_TRANSITION");

        for(final Map<String, String> rfWorkflow : rfWorkflows){
            final List<String> workflowTransitions = new LinkedList<>();
            workflowTransitions.add("0");
            for(final Map<String, String> rfTransition : rfTransitions){
                if(rfWorkflow.get("ID").equals(rfTransition.get("ID_PARENT"))){
                    workflowTransitions.add(rfTransition.get("C_CODE"));
                }
            }

            etk.createSQL("DELETE FROM t_rf_transition WHERE id_parent IN(SELECT id FROM t_rf_workflow WHERE c_code = :rfWorkflowCode) AND c_code NOT IN(:rfTransitions)")
            .setParameter("rfWorkflowCode", rfWorkflow.get("C_CODE"))
            .setParameter("rfTransitions", workflowTransitions)
            .execute();
        }
    }

    /**
     * Deletes RF Workflow Effect objects which are currently in the system but are not in the new file
     * being imported.
     *
     * @param etk entellitrak execution context.
     * @param document XML Document containing user-configurable Rules Framework data.
     */
    private static void deleteWorkflowEffects(final ExecutionContext etk, final Document document) {
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        final List<Map<String, String>> rfWorkflowEffects =
                ImportExportUtility.getTable(document, "T_RF_WORKFLOW_EFFECT");

        for(final Map<String, String> rfWorkflow : rfWorkflows){
            final List<String> workflowWorkflowEffects = new LinkedList<>();
            workflowWorkflowEffects.add("0");
            for(final Map<String, String> rfWorkflowEffect : rfWorkflowEffects){
                if(rfWorkflow.get("ID").equals(rfWorkflowEffect.get("ID_PARENT"))){
                    workflowWorkflowEffects.add(rfWorkflowEffect.get("C_CODE"));
                }
            }

            etk.createSQL("DELETE FROM t_rf_workflow_effect WHERE id_parent IN(SELECT id FROM t_rf_workflow WHERE c_code = :rfWorkflowCode) AND c_code NOT IN(:rfWorkflowEffects)")
            .setParameter("rfWorkflowCode", rfWorkflow.get("C_CODE"))
            .setParameter("rfWorkflowEffects", workflowWorkflowEffects)
            .execute();
        }
    }

    /**
     * Deletes RF Workflow Parameter objects which are currently in the system but are not in the
     * new file being imported.
     *
     * @param etk entellitrak execution context.
     * @param document XML Document containing user-configurable Rules Framework data.
     */
    private static void deleteWorkflowParameters(final ExecutionContext etk, final Document document) {
        final List<Map<String, String>> rfWorkflows = ImportExportUtility.getTable(document, "T_RF_WORKFLOW");
        final List<Map<String, String>> rfWorkflowParameters =
                ImportExportUtility.getTable(document, "T_RF_WORKFLOW_PARAMETER");

        for(final Map<String, String> rfWorkflow : rfWorkflows){
            final List<String> workflowParameters = new LinkedList<>();
            workflowParameters.add("0");
            for(final Map<String, String> rfWorkflowParameter : rfWorkflowParameters){
                if(rfWorkflow.get("ID").equals(rfWorkflowParameter.get("ID_PARENT"))){
                    workflowParameters.add(rfWorkflowParameter.get("C_CODE"));
                }
            }

            etk.createSQL("DELETE FROM t_rf_workflow_parameter WHERE id_parent IN(SELECT id FROM t_rf_workflow WHERE c_code = :rfWorkflowCode) AND c_code NOT IN(:rfWorkflowParameters)")
            .setParameter("rfWorkflowCode", rfWorkflow.get("C_CODE"))
            .setParameter("rfWorkflowParameters", workflowParameters)
            .execute();
        }
    }

    /**
     * Performs the import of all configuration in the new XML file.
     * Takes care of deleting values which are currently in the system but are not in the file.
     *
     * @param inputStream XML document containing user-configurable Rules Framework data.
     * @throws ApplicationException If there was an underlying exception.
     */
    @Override
    public void performImport(final InputStream inputStream)
            throws ApplicationException{

        try {
            final Document document = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new InputStreamReader(inputStream)));

            RfUtility.cleanRfWorkflow(etk);

            /* First we will delete States, Transitions and Effects which used to be in the system
             * but are not in the new import */
            deleteStates(etk, document);
            deleteTransitions(etk, document);
            deleteWorkflowEffects(etk, document);
            deleteWorkflowParameters(etk, document);

            RfUtility.cleanRfWorkflow(etk); //We clean up foreign keys of the things we just deleted.

            /* Now we import the new stuff */
            importParameterTypes(etk, document);
            importLookups(etk, document);
            importScripts(etk, document);
            importScriptParameters(etk, document);
            importWorkflows(etk, document);
            importStates(etk, document);
            importTransitions(etk, document);
            importWorkflowEffects(etk, document);
            importTransitionFromStates(etk, document);
            importTransitionRoles(etk, document);
            importEffectTransitions(etk, document);
            importScriptParameterValues(etk, document);
            importWorkflowParameters(etk, document);
            importTransitionParameterValues(etk, document);

            RfUtility.cleanRfWorkflow(etk); //We clean up foreign keys again
        } catch (final SAXException | IOException | ParserConfigurationException | IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }
}
package net.micropact.aea.rf.service;

import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.entellitrak.ExecutionContext;

import net.micropact.aea.core.query.QueryUtility;
import net.micropact.aea.rf.utility.RfUtility;
import net.micropact.aea.utility.ImportExportUtility;
import net.micropact.aea.utility.Utility;

/**
 * Class containing logic to export the site-specific Rules Framework configuration data.
 *
 * @author Zachary.Miller
 */
public final class RfExportLogic {

    /**
     * Utility classes do not need public constructors.
     */
    private RfExportLogic(){}

    /**
     * This method exports the site-specific Rules Framework configuration data for all workflows in the site.
     *
     * @param etk entellitrak execution context
     * @return an XML Document containing the Rules Framework data
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}
     */
    public static Document generateXml(final ExecutionContext etk) throws ParserConfigurationException{
        return generateXml(etk, QueryUtility.mapsToLongs(etk.createSQL("SELECT ID FROM t_rf_workflow ORDER BY ID")
                .fetchList()));
    }

    /**
     * This method generates an XML representation of all the user-configurable data relating to particular
     * RF Workflow objects. This also includes linked RF Script and reference data.
     *
     * @param etk entellitrak execution context
     * @param rfWorkflowIds The tracking ids of the RF Workflow objects which you would like the XML file to contain
     *     information on.
     * @return An XML file representing all the user-configurable data relating to the Rules Framework
     * @throws ParserConfigurationException If there was an underlying {@link ParserConfigurationException}
     */
    public static Document generateXml(final ExecutionContext etk, final List<Long> rfWorkflowIds)
            throws ParserConfigurationException {

        final List<Long> rfWorkflowIdsQueryList = QueryUtility.toNonEmptyParameterList(rfWorkflowIds);

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        final Document document = documentBuilder.newDocument();

        final Element root = document.createElement("objects");

        document.appendChild(root);

        RfUtility.cleanRfWorkflow(etk);

        /*T_RF_PARAMETER_TYPE*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_RF_PARAMETER_TYPE",
                etk.createSQL("SELECT * FROM t_rf_parameter_type parameterType")
                .fetchList());

        /*T_RF_SCRIPT_PARAMETER*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_RF_SCRIPT_PARAMETER",
                etk.createSQL("SELECT * FROM t_rf_script_parameter scriptParameter WHERE EXISTS(SELECT * FROM t_rf_script script JOIN t_rf_workflow_effect effect ON effect.c_script = script.id WHERE script.id = scriptParameter.id_base AND effect.id_base IN (:rfWorkflowIds))")
                .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                .fetchList());

        /*T_RF_LOOKUP*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_RF_LOOKUP",
                etk.createSQL("SELECT * FROM t_rf_lookup lookup WHERE EXISTS (SELECT * FROM t_rf_script_parameter scriptParameter JOIN t_rf_workflow_effect effect ON effect.c_script = scriptParameter.id_base WHERE effect.id_base IN (:rfWorkflowIds) AND scriptParameter.c_lookup = lookup.id ) OR EXISTS(SELECT * FROM t_rf_workflow_parameter workflowParameter WHERE workflowParameter.id_base IN (:rfWorkflowIds) AND workflowParameter.c_lookup = lookup.id )")
                .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                .fetchList());

        /*T_RF_WORKFLOW*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_RF_WORKFLOW",
                etk.createSQL("SELECT * FROM t_rf_workflow workflow WHERE workflow.id IN (:rfWorkflowIds)")
                .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                .fetchList());

        /*T_RF_STATE*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_RF_STATE",
                etk.createSQL(Utility.isSqlServer(etk) ? "SELECT id, id_base, id_parent, c_code, c_name, c_order, CONVERT(VARCHAR, c_end_date, 101) c_end_date, CONVERT(VARCHAR, c_start_date, 101) c_start_date, c_x_coordinate, c_y_coordinate, c_description FROM t_rf_state state WHERE state.id_base IN (:rfWorkflowIds)"
                        : "SELECT id, id_base, id_parent, c_code, c_name, c_order, TO_CHAR(c_end_date, 'MM/DD/YYYY') c_end_date, TO_CHAR(c_start_date, 'MM/DD/YYYY') c_start_date, c_x_coordinate, c_y_coordinate, c_description FROM t_rf_state state WHERE state.id_base IN (:rfWorkflowIds)")
                        .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                        .fetchList());

        /*T_RF_TRANSITION*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_RF_TRANSITION",
                etk.createSQL(Utility.isSqlServer(etk) ? "SELECT id, id_base, id_parent, c_code, c_name, c_order, c_to_state, c_initial_transition, CONVERT(VARCHAR, c_end_date, 101) c_end_date, CONVERT(VARCHAR, c_start_date, 101) c_start_date, c_description FROM t_rf_transition transition WHERE transition.id_base IN (:rfWorkflowIds)"
                        : "SELECT id, id_base, id_parent, c_code, c_name, c_order, c_to_state, c_initial_transition, TO_CHAR(c_end_date, 'MM/DD/YYYY') c_end_date, TO_CHAR(c_start_date, 'MM/DD/YYYY') c_start_date, c_description FROM t_rf_transition transition WHERE transition.id_base IN (:rfWorkflowIds)")
                        .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                        .fetchList());

        /*T_RF_TRANSITION_PARAMETER_VALU*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_RF_TRANSITION_PARAMETER_VALU",
                etk.createSQL("SELECT * FROM t_rf_transition_parameter_valu WHERE id_base IN (:rfWorkflowIds)")
                        .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                        .fetchList());

        /*T_RF_WORKFLOW_PARAMETER*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_RF_WORKFLOW_PARAMETER",
                etk.createSQL("SELECT * FROM t_rf_workflow_parameter WHERE id_base IN(:rfWorkflowIds)")
                    .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                    .fetchList());

        /*T_RF_WORKFLOW_EFFECT*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_RF_WORKFLOW_EFFECT",
                etk.createSQL("SELECT * FROM t_rf_workflow_effect effect WHERE effect.id_base IN (:rfWorkflowIds)")
                .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                .fetchList());

        /*T_RF_SCRIPT_PARAMETER_VALUE*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_RF_SCRIPT_PARAMETER_VALUE",
                etk.createSQL("SELECT * FROM t_rf_script_parameter_value WHERE id_base IN (:rfWorkflowIds)")
                .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                .fetchList());

        /*T_RF_SCRIPT*/
        ImportExportUtility.addListToXml(document,
                root,
                "T_RF_SCRIPT",
                etk.createSQL("SELECT rfScript.* FROM t_rf_script rfScript WHERE EXISTS(SELECT * FROM t_rf_workflow_effect effect WHERE effect.c_script = rfScript.id AND effect.id_parent IN (:rfWorkflowIds))")
                .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                .fetchList());

        /*M_RF_TRANSITION_FROM_STATE*/
        ImportExportUtility.addListToXml(document,
                root,
                "M_RF_TRANSITION_FROM_STATE",
                etk.createSQL("SELECT transitionState.* FROM m_rf_transition_from_state transitionState JOIN t_rf_state fromState ON transitionState.c_from_state = fromState.id WHERE fromState.id_base IN (:rfWorkflowIds)")
                .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                .fetchList());

        /*M_RF_TRANSITION_ROLE*/
        ImportExportUtility.addListToXml(document,
                root,
                "M_RF_TRANSITION_ROLE",
                etk.createSQL("SELECT * FROM m_rf_transition_role transitionRole WHERE EXISTS(SELECT * FROM t_rf_transition transition WHERE transition.id = transitionRole.id_owner AND transition.id_base IN (:rfWorkflowIds))")
                .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                .fetchList());

        /*M_RF_EFFECT_TRANSITION*/
        ImportExportUtility.addListToXml(document,
                root,
                "M_RF_EFFECT_TRANSITION",
                etk.createSQL("SELECT * FROM m_rf_effect_transition effectTransition WHERE EXISTS(SELECT * FROM t_rf_workflow_effect effect WHERE effect.id = effectTransition.id_owner AND effect.id_base IN (:rfWorkflowIds))")
                .setParameter("rfWorkflowIds", rfWorkflowIdsQueryList)
                .fetchList());

        /*ETK_ROLE*/
        ImportExportUtility.addListToXml(document,
                root,
                "ETK_ROLE",
                etk.createSQL("SELECT * FROM etk_role r")
                .fetchList());

        return document;
    }
}

package net.micropact.aea.core.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.entellitrak.DataAccessException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

import net.entellitrak.aea.core.service.IDeploymentResult;
import net.entellitrak.aea.core.service.IDeploymentService;
import net.micropact.aea.core.exceptionTools.ExceptionUtility;
import net.micropact.aea.core.utility.StringUtils;
import net.micropact.aea.utility.Utility;

/*
 * This class pretty much threads the mutable DeploymentResult through all of the sub-methods.
 * This could all be refactored so that each of the private methods we call to do something implements an interface
 * and returns its result but there's not much point in doing that at this point time.
 */

/**
 * Implementation of {@link IDeploymentService} public API.
 *
 * @author Zachary.Miller
 */
public class DeploymentService implements IDeploymentService {

    private static final int ETK_DEFAULT_TEXT_ELEMENT_LENGTH = 255;
    private static final String NEWLINE = "\n";

    private final ExecutionContext etk;

    /**
     * Simple constructor.
     *
     * @param executionContext entellitrak execution context
     */
    public DeploymentService(final ExecutionContext executionContext){
        etk = executionContext;
    }

    @Override
    public IDeploymentResult runComponentSetup() {
        final DeploymentResult deploymentResult = new DeploymentResult();

        if (Utility.isSqlServer(etk)) {
            deploymentResult.addMessage("SQLServer DB Detected");
        } else {
            deploymentResult.addMessage("Oracle DB Detected");
        }

        /* Begin DB insertion methods */
        generateDatabaseArtifacts(etk, deploymentResult);
        configureComponents(etk, deploymentResult);
        /* End DB insertion methods */

        //Clear caches to ensure new values are picked up.
        clearCache(etk, deploymentResult);

        return deploymentResult;
    }

    /**
     * Clear the cache.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void clearCache(final ExecutionContext etk, final DeploymentResult deploymentResult) {
        etk.getCache().clearCache();
        etk.getDataCacheService().clearDataCaches();

        deploymentResult.addMessage("Cache Cleared.");
    }

    /**
     * Configure specific components.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void configureComponents(final ExecutionContext etk, final DeploymentResult deploymentResult) {
        configureCalculatedFields(etk, deploymentResult);
        configureDbUtils(etk, deploymentResult);
        configureAeaCoreConfiguration(etk, deploymentResult);
        configureAeaAuditLog(etk, deploymentResult);
        configureDashboardTools(etk, deploymentResult);
    }

    /**
     * Generate database objects (functions/procedures/views).
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void generateDatabaseArtifacts(final ExecutionContext etk, final DeploymentResult deploymentResult) {
        createScriptPackageView(etk, deploymentResult);
        createScriptPackageViewSysOnly(etk, deploymentResult);
        createAeaLsDataElementView(etk, deploymentResult);
        createAddJbpmLogEntry(etk, deploymentResult);
        createAddJbpm(etk, deploymentResult);
        createAeaUpdateFileReferenceId(etk, deploymentResult);
    }

    /**
     * Creates AEA_SCRIPT_PKG_VIEW View for both Oracle and SQLServer. This view simplifies retrieval of the fully
     * qualified package associated with a script object.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void createScriptPackageView(final ExecutionContext etk, final DeploymentResult deploymentResult) {
        try {
            if (Utility.isSqlServer(etk)) {
                try {
                    etk.createSQL("DROP VIEW AEA_SCRIPT_PKG_VIEW").execute();
                    deploymentResult.addMessage("Dropped existing AEA_SCRIPT_PKG_VIEW.");
                } catch (final Exception e) {
                    deploymentResult.addMessage("Error dropping existing AEA_SCRIPT_PKG_VIEW, ignore if first time import.");
                }

                etk.createSQL(
                        " CREATE VIEW AEA_SCRIPT_PKG_VIEW " +
                                " (SCRIPT_ID, SCRIPT_NAME, SCRIPT_LANGUAGE_TYPE, SCRIPT_BUSINESS_KEY, WORKSPACE_ID,  " +
                                " TRACKING_CONFIG_ID, PACKAGE_PATH, PACKAGE_NODE_ID, PACKAGE_TYPE, FULLY_QUALIFIED_SCRIPT_NAME, " +
                                " SCRIPT_HANDLER_TYPE) AS " +

                    " WITH etk_packages (package_node_id, workspace_id, tracking_config_id, path, package_type) AS  " +
                    "   (   " +
                    "     SELECT obj.package_node_id,  " +
                    "                            obj.workspace_id,  " +
                    "                            obj.tracking_config_id,  " +
                    "                            cast(obj.name as varchar(4000)) as path,   " +
                    "                            obj.package_type " +
                    "     FROM etk_package_node obj   " +
                    "     WHERE obj.parent_node_id is null " +
                    "     UNION ALL   " +
                    "     SELECT nplus1.package_node_id, " +
                    "           nplus1.workspace_id,  " +
                    "           nplus1.tracking_config_id, " +
                    "           cast(etk_packages.path + '.' + nplus1.name as varchar(4000)) as path,   " +
                    "           nplus1.package_type " +
                    "     FROM etk_package_node nplus1, etk_packages    " +
                    "     WHERE nplus1.parent_node_id= etk_packages.package_node_id   " +
                    "   )                    " +
                    "   select so.SCRIPT_ID as SCRIPT_ID, " +
                    "   so.NAME as SCRIPT_NAME, " +
                    "   so.LANGUAGE_TYPE as SCRIPT_LANGUAGE_TYPE, " +
                    "   so.BUSINESS_KEY as SCRIPT_BUSINESS_KEY, " +
                    "   so.WORKSPACE_ID as WORKSPACE_ID, " +
                    "   so.TRACKING_CONFIG_ID as TRACKING_CONFIG_ID, " +
                    "   ep.path as PACKAGE_PATH,  " +
                    "   ep.package_node_id as PACKAGE_NODE_ID, " +
                    "   ep.package_type as PACKAGE_TYPE, " +
                    "   case when so.package_node_id is null  " +
                    "   then so.NAME else " +
                    "   ep.path + '.' + so.NAME END AS FULLY_QUALIFIED_SCRIPT_NAME, " +
                    "   so.HANDLER_TYPE as SCRIPT_HANDLER_TYPE " +
                    "   from etk_script_object so  " +
                        "   left join etk_packages ep on so.package_node_id = ep.package_node_id ")
                .execute();
            } else {
                etk.createSQL(
                        "   CREATE OR REPLACE VIEW AEA_SCRIPT_PKG_VIEW  " +
                                "   (SCRIPT_ID, SCRIPT_NAME, SCRIPT_LANGUAGE_TYPE, SCRIPT_BUSINESS_KEY, WORKSPACE_ID,  " +
                                "       TRACKING_CONFIG_ID, PACKAGE_PATH, PACKAGE_NODE_ID, PACKAGE_TYPE, FULLY_QUALIFIED_SCRIPT_NAME, " +
                                "       SCRIPT_HANDLER_TYPE) AS " +
                                "   WITH etk_packages (package_node_id, workspace_id, tracking_config_id, path, package_type) AS " +
                                "     (SELECT obj.package_node_id, " +
                                "       obj.workspace_id, " +
                                "       obj.tracking_config_id, " +
                                "       cast(obj.name as varchar(4000)) as path, " +
                                "       obj.package_type " +
                                "     FROM etk_package_node obj " +
                                "     WHERE obj.parent_node_id IS NULL " +
                                "     UNION ALL " +
                                "     SELECT nplus1.package_node_id, " +
                                "       nplus1.workspace_id, " +
                                "       nplus1.tracking_config_id, " +
                                "       cast(etk_packages.path || '.' || nplus1.name as varchar(4000)) as path, " +
                                "       nplus1.package_type " +
                                "     FROM etk_package_node nplus1, " +
                                "       etk_packages " +
                                "     WHERE nplus1.parent_node_id= etk_packages.package_node_id " +
                                "     ) " +
                                "   SELECT so.SCRIPT_ID     AS SCRIPT_ID, " +
                                "     so.NAME               AS SCRIPT_NAME, " +
                                "     so.LANGUAGE_TYPE      AS SCRIPT_LANGUAGE_TYPE, " +
                                "     so.BUSINESS_KEY       AS SCRIPT_BUSINESS_KEY, " +
                                "     so.WORKSPACE_ID       AS WORKSPACE_ID, " +
                                "     so.TRACKING_CONFIG_ID AS TRACKING_CONFIG_ID, " +
                                "     ep.path               AS PACKAGE_PATH, " +
                                "     ep.package_node_id    AS PACKAGE_NODE_ID, " +
                                "         ep.package_type       AS PACKAGE_TYPE, " +
                                "     CASE " +
                                "       WHEN so.package_node_id IS NULL " +
                                "       THEN so.NAME " +
                                "       ELSE ep.path " +
                                "         || '.' " +
                                "         || so.NAME " +
                                "     END AS FULLY_QUALIFIED_SCRIPT_NAME, " +
                                "   so.HANDLER_TYPE as SCRIPT_HANDLER_TYPE " +
                                "   FROM etk_script_object so " +
                                "   LEFT JOIN etk_packages ep " +
                        "   ON so.package_node_id = ep.package_node_id ")
                .execute();
            }

            deploymentResult.addMessage("Successfully created view AEA_SCRIPT_PKG_VIEW");

        } catch (final Exception e) {
            deploymentResult.addMessage("Error creating AEA_SCRIPT_PKG_VIEW.");
            deploymentResult.addMessage(ExceptionUtility.getFullStackTrace(e));
        }
    }

    /**
     * Creates AEA_SCRIPT_PKG_VIEW View for both Oracle and SQLServer. This view simplifies retrieval of the fully
     * qualified package associated with a script object.
     *
     * Filtered by SYSTEM workspace only. Useful because you don't need to filter this one by workspace ID and tracking
     * config ID.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void createScriptPackageViewSysOnly(final ExecutionContext etk, final DeploymentResult deploymentResult) {
        try {

            try {
                etk.createSQL("DROP VIEW AEA_SCRIPT_PKG_VIEW_SYS_ONLY").execute();
                deploymentResult.addMessage("Dropped existing AEA_SCRIPT_PKG_VIEW_SYS_ONLY.");
            } catch (final Exception e) {
                deploymentResult.addMessage("Error dropping existing AEA_SCRIPT_PKG_VIEW_SYS_ONLY, ignore if first time import.");
            }

            etk.createSQL(" CREATE VIEW AEA_SCRIPT_PKG_VIEW_SYS_ONLY " +
                    "  (SCRIPT_ID, SCRIPT_NAME, SCRIPT_LANGUAGE_TYPE, SCRIPT_BUSINESS_KEY,  " +
                    "   WORKSPACE_ID, TRACKING_CONFIG_ID, PACKAGE_PATH, PACKAGE_NODE_ID, PACKAGE_TYPE, " +
                    "   FULLY_QUALIFIED_SCRIPT_NAME, SCRIPT_HANDLER_TYPE) AS  " +
                    "      SELECT SCRIPT_ID, SCRIPT_NAME, SCRIPT_LANGUAGE_TYPE, SCRIPT_BUSINESS_KEY, " +
                    "             WORKSPACE_ID, TRACKING_CONFIG_ID, PACKAGE_PATH, PACKAGE_NODE_ID, PACKAGE_TYPE, " +
                    "             FULLY_QUALIFIED_SCRIPT_NAME, SCRIPT_HANDLER_TYPE " +
                    "      FROM AEA_SCRIPT_PKG_VIEW " +
                    "      WHERE WORKSPACE_ID =  " +
                    "         (SELECT workspace_id FROM etk_workspace  " +
                    "          WHERE workspace_name = 'system' and user_id is null) " +
                    "      AND TRACKING_CONFIG_ID = " +
                    "         (SELECT tracking_config_id FROM etk_tracking_config  " +
                    "          WHERE config_version = " +
                    "             (SELECT MAX(config_version) FROM etk_tracking_config)) ").execute();

            deploymentResult.addMessage("Successfully created view AEA_SCRIPT_PKG_VIEW_SYS_ONLY");
        } catch (final Exception e) {
            deploymentResult.addMessage("Error creating AEA_SCRIPT_PKG_VIEW_SYS_ONLY.");
            deploymentResult.addMessage(ExceptionUtility.getFullStackTrace(e));
        }
    }

    /**
     * Creates helper view AEA_LS_DATA_ELEMENT_VIEW.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void createAeaLsDataElementView(final ExecutionContext etk, final DeploymentResult deploymentResult) {

        String createOrAlter = "CREATE";

        if (Utility.isSqlServer(etk)) {
            int existingViewCount = 0;

            try {
                existingViewCount =
                        etk.createSQL(
                                "SELECT count(*) FROM sys.views WHERE object_id = OBJECT_ID(N'AEA_LS_DATA_ELEMENT_VIEW')")
                        .fetchInt();
            } catch (final Exception e) {
                deploymentResult.addMessage("createAeaLsDataElementView = Error detecting if view already exists, assuming no.");
            }

            if (existingViewCount >= 1) {
                createOrAlter = "ALTER";
            }
        }

        try {
            etk.createSQL(Utility.isSqlServer(etk) ?
                                                    createOrAlter +
                                                    " VIEW AEA_LS_DATA_ELEMENT_VIEW "
                                                    + " AS "
                                                    + " WITH  ETK_PACKAGES (package_node_id, path) AS ( "

                    + "       SELECT obj.package_node_id, cast(obj.name as varchar(4000)) "
                    + "       FROM etk_package_node obj "
                    + "       WHERE obj.parent_node_id IS NULL "
                    + "       AND obj.workspace_id      = "
                    + "         (SELECT workspace_id "
                    + "         FROM etk_workspace "
                    + "         WHERE workspace_name = 'system' "
                    + "         AND user_id         IS NULL "
                    + "         ) "
                    + "       AND obj.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) "
                    + "       UNION ALL "
                    + "       SELECT nplus1.package_node_id, cast(etk_packages.path + '.' + nplus1.name as varchar(4000)) as path "
                    + "       FROM etk_package_node nplus1, "
                    + "         etk_packages "
                    + "       WHERE nplus1.parent_node_id= etk_packages.package_node_id "
                    + "        "
                    + " ),  "
                    + " SYSTEM_SCRIPTS (SCRIPT_NAME, PACKAGE_NODE_ID, SCRIPT_ID, tracking_config_id, workspace_id) AS ( "

                    + "     SELECT SO.NAME, SO.PACKAGE_NODE_ID, SO.SCRIPT_ID, SO.tracking_config_id, so.workspace_id  "
                    + "     FROM ETK_SCRIPT_OBJECT SO "
                    + "     WHERE  ( "
                    + "                 ( "
                    + "                   so.workspace_id         = "
                    + "                     (SELECT workspace_id "
                    + "                      FROM etk_workspace "
                    + "                      WHERE workspace_name = 'system' "
                    + "                      AND user_id         IS NULL) "
                    + "                 ) "
                    + "                 AND  "
                    + "                 ( "
                    + "                    so.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) "
                    + "                 )  "
                    + "           ) "
                    + " ) "
                    + " SELECT LD.LOOKUP_SOURCE_TYPE      AS LOOKUP_TYPE, "
                    + "   (select DATA_OBJECT.TABLE_NAME from ETK_DATA_OBJECT DATA_OBJECT where DATA_OBJECT.DATA_OBJECT_ID = LD.DATA_OBJECT_ID) AS TABLE_NAME, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.VALUE_ELEMENT_ID) AS VALUE_COLUMN, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.DISPLAY_ELEMENT_ID) AS DISPLAY_COLUMN, "
                    + "   LD.SYSTEM_OBJECT_TYPE           AS SYSTEM_OBJECT_TYPE, "
                    + "   LD.SYSTEM_OBJECT_DISPLAY_FORMAT AS SYSTEM_OBJECT_DISPLAY_FORMAT, "
                    + "   LD.ENABLE_CACHING               AS ENABLE_CACHING, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.ORDER_BY_ELEMENT_ID) AS ORDER_COLUMN, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.START_DATE_ELEMENT_ID) AS START_DATE_COLUMN, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.END_DATE_ELEMENT_ID) AS END_DATE_COLUMN, "
                    + "   (select PATH from ETK_PACKAGES ep where so.package_node_id = ep.package_node_id) "
                    + "   + "
                    + "   CASE "
                    + "     WHEN so.package_node_id IS NULL "
                    + "     THEN '' "
                    + "     ELSE '.' "
                    + "   END "
                    + "   + so.SCRIPT_NAME       AS SCRIPT_JAVA_ID, "
                    + "   (select EFC.NAME from ETK_FORM_CONTROL EFC where EFC.FORM_CONTROL_ID = FCB.FORM_CONTROL_ID) AS DATA_ELEMENT_NAME, "
                    + "   (select EFC.DATA_FORM_ID from ETK_FORM_CONTROL EFC where EFC.FORM_CONTROL_ID = FCB.FORM_CONTROL_ID) AS DATA_FORM_ID, "
                    + "   DE.BUSINESS_KEY AS DATA_ELEMENT_BUSINESS_KEY "
                    + " FROM ETK_LOOKUP_DEFINITION LD "
                    + " JOIN ETK_DATA_ELEMENT DE ON DE.LOOKUP_DEFINITION_ID = LD.LOOKUP_DEFINITION_ID "
                    + " LEFT JOIN ETK_FORM_CTL_ELEMENT_BINDING FCB ON FCB.DATA_ELEMENT_ID = DE.DATA_ELEMENT_ID "
                    + " LEFT JOIN SYSTEM_SCRIPTS SO ON LD.SQL_SCRIPT_OBJECT_ID = SO.SCRIPT_ID "
                    + " WHERE LD.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) "


                    + " UNION "


                    + " SELECT LD.LOOKUP_SOURCE_TYPE      AS LOOKUP_TYPE, "
                    + "   (select DATA_OBJECT.TABLE_NAME from ETK_DATA_OBJECT DATA_OBJECT where DATA_OBJECT.DATA_OBJECT_ID = LD.DATA_OBJECT_ID) AS TABLE_NAME, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.VALUE_ELEMENT_ID) AS VALUE_COLUMN, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.DISPLAY_ELEMENT_ID) AS DISPLAY_COLUMN, "
                    + "   LD.SYSTEM_OBJECT_TYPE           AS SYSTEM_OBJECT_TYPE, "
                    + "   LD.SYSTEM_OBJECT_DISPLAY_FORMAT AS SYSTEM_OBJECT_DISPLAY_FORMAT, "
                    + "   LD.ENABLE_CACHING               AS ENABLE_CACHING, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.ORDER_BY_ELEMENT_ID) AS ORDER_COLUMN, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.START_DATE_ELEMENT_ID) AS START_DATE_COLUMN, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.END_DATE_ELEMENT_ID) AS END_DATE_COLUMN, "
                    + "   (select PATH from ETK_PACKAGES ep where so.package_node_id = ep.package_node_id) "
                    + "   + "
                    + "   CASE "
                    + "     WHEN so.package_node_id IS NULL "
                    + "     THEN '' "
                    + "     ELSE '.' "
                    + "   END "
                    + "   + so.SCRIPT_NAME        AS SCRIPT_JAVA_ID, "
                    + "   (select EFC2.NAME from ETK_FORM_CONTROL EFC2 where EFC2.FORM_CONTROL_ID = FCLB.FORM_CONTROL_ID) AS DATA_ELEMENT_NAME, "
                    + "   (select EFC2.DATA_FORM_ID from ETK_FORM_CONTROL EFC2 where EFC2.FORM_CONTROL_ID = FCLB.FORM_CONTROL_ID) AS DATA_FORM_ID, "
                    + "    NULL AS DATA_ELEMENT_BUSINESS_KEY "
                    + " FROM ETK_LOOKUP_DEFINITION LD "
                    + " JOIN ETK_FORM_CTL_LOOKUP_BINDING FCLB ON LD.LOOKUP_DEFINITION_ID = FCLB.LOOKUP_DEFINITION_ID "
                    + " LEFT JOIN SYSTEM_SCRIPTS SO ON LD.SQL_SCRIPT_OBJECT_ID = SO.SCRIPT_ID "
                    + " WHERE LD.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) "

                    :

                        " CREATE OR REPLACE "
                        + " VIEW AEA_LS_DATA_ELEMENT_VIEW "
                        + " AS "
                        + " WITH  ETK_PACKAGES (package_node_id, path) AS ( "

                    + "       SELECT obj.package_node_id, obj.name AS path "
                    + "       FROM etk_package_node obj "
                    + "       WHERE obj.parent_node_id IS NULL "
                    + "       AND obj.workspace_id      = "
                    + "         (SELECT workspace_id "
                    + "         FROM etk_workspace "
                    + "         WHERE workspace_name = 'system' "
                    + "         AND user_id         IS NULL "
                    + "         ) "
                    + "       AND obj.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) "
                    + "       UNION ALL "
                    + "       SELECT nplus1.package_node_id, "
                    + "         etk_packages.path "
                    + "         || '.' "
                    + "         || nplus1.name AS path "
                    + "       FROM etk_package_node nplus1, "
                    + "         etk_packages "
                    + "       WHERE nplus1.parent_node_id= etk_packages.package_node_id "
                    + "        "
                    + " ),  "
                    + " SYSTEM_SCRIPTS (SCRIPT_NAME, PACKAGE_NODE_ID, SCRIPT_ID, tracking_config_id, workspace_id) AS ( "

                    + "     SELECT SO.NAME, SO.PACKAGE_NODE_ID, SO.SCRIPT_ID, SO.tracking_config_id, so.workspace_id  "
                    + "     FROM ETK_SCRIPT_OBJECT SO "
                    + "     WHERE  ( "
                    + "                 ( "
                    + "                   so.workspace_id         = "
                    + "                     (SELECT workspace_id "
                    + "                      FROM etk_workspace "
                    + "                      WHERE workspace_name = 'system' "
                    + "                      AND user_id         IS NULL) "
                    + "                 ) "
                    + "                 AND  "
                    + "                 ( "
                    + "                    so.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) "
                    + "                 )  "
                    + "           ) "
                    + " ) "
                    + " SELECT LD.LOOKUP_SOURCE_TYPE      AS LOOKUP_TYPE, "
                    + "   (select DATA_OBJECT.TABLE_NAME from ETK_DATA_OBJECT DATA_OBJECT where DATA_OBJECT.DATA_OBJECT_ID = LD.DATA_OBJECT_ID) AS TABLE_NAME, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.VALUE_ELEMENT_ID) AS VALUE_COLUMN, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.DISPLAY_ELEMENT_ID) AS DISPLAY_COLUMN, "
                    + "   LD.SYSTEM_OBJECT_TYPE           AS SYSTEM_OBJECT_TYPE, "
                    + "   LD.SYSTEM_OBJECT_DISPLAY_FORMAT AS SYSTEM_OBJECT_DISPLAY_FORMAT, "
                    + "   LD.ENABLE_CACHING               AS ENABLE_CACHING, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.ORDER_BY_ELEMENT_ID) AS ORDER_COLUMN, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.START_DATE_ELEMENT_ID) AS START_DATE_COLUMN, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.END_DATE_ELEMENT_ID) AS END_DATE_COLUMN, "
                    + "   (select PATH from ETK_PACKAGES ep where so.package_node_id = ep.package_node_id) "
                    + "   || "
                    + "   CASE "
                    + "     WHEN so.package_node_id IS NULL "
                    + "     THEN '' "
                    + "     ELSE '.' "
                    + "   END "
                    + "   || so.SCRIPT_NAME       AS SCRIPT_JAVA_ID, "
                    + "   (select EFC.NAME from ETK_FORM_CONTROL EFC where EFC.FORM_CONTROL_ID = FCB.FORM_CONTROL_ID) AS DATA_ELEMENT_NAME, "
                    + "   (select EFC.DATA_FORM_ID from ETK_FORM_CONTROL EFC where EFC.FORM_CONTROL_ID = FCB.FORM_CONTROL_ID) AS DATA_FORM_ID, "
                    + "   DE.BUSINESS_KEY AS DATA_ELEMENT_BUSINESS_KEY "
                    + " FROM ETK_LOOKUP_DEFINITION LD "
                    + " JOIN ETK_DATA_ELEMENT DE ON DE.LOOKUP_DEFINITION_ID = LD.LOOKUP_DEFINITION_ID "
                    + " LEFT JOIN ETK_FORM_CTL_ELEMENT_BINDING FCB ON FCB.DATA_ELEMENT_ID = DE.DATA_ELEMENT_ID "
                    + " LEFT JOIN SYSTEM_SCRIPTS SO ON LD.SQL_SCRIPT_OBJECT_ID = SO.SCRIPT_ID "
                    + " WHERE LD.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) "


                    + " UNION "


                    + " SELECT LD.LOOKUP_SOURCE_TYPE      AS LOOKUP_TYPE, "
                    + "   (select DATA_OBJECT.TABLE_NAME from ETK_DATA_OBJECT DATA_OBJECT where DATA_OBJECT.DATA_OBJECT_ID = LD.DATA_OBJECT_ID) AS TABLE_NAME, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.VALUE_ELEMENT_ID) AS VALUE_COLUMN, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.DISPLAY_ELEMENT_ID) AS DISPLAY_COLUMN, "
                    + "   LD.SYSTEM_OBJECT_TYPE           AS SYSTEM_OBJECT_TYPE, "
                    + "   LD.SYSTEM_OBJECT_DISPLAY_FORMAT AS SYSTEM_OBJECT_DISPLAY_FORMAT, "
                    + "   LD.ENABLE_CACHING               AS ENABLE_CACHING, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.ORDER_BY_ELEMENT_ID) AS ORDER_COLUMN, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.START_DATE_ELEMENT_ID) AS START_DATE_COLUMN, "
                    + "   (select COLUMN_NAME from ETK_DATA_ELEMENT where DATA_ELEMENT_ID = LD.END_DATE_ELEMENT_ID) AS END_DATE_COLUMN, "
                    + "   (select PATH from ETK_PACKAGES ep where so.package_node_id = ep.package_node_id) "
                    + "   || "
                    + "   CASE "
                    + "     WHEN so.package_node_id IS NULL "
                    + "     THEN '' "
                    + "     ELSE '.' "
                    + "   END "
                    + "   || so.SCRIPT_NAME        AS SCRIPT_JAVA_ID, "
                    + "   (select EFC2.NAME from ETK_FORM_CONTROL EFC2 where EFC2.FORM_CONTROL_ID = FCLB.FORM_CONTROL_ID) AS DATA_ELEMENT_NAME, "
                    + "   (select EFC2.DATA_FORM_ID from ETK_FORM_CONTROL EFC2 where EFC2.FORM_CONTROL_ID = FCLB.FORM_CONTROL_ID) AS DATA_FORM_ID, "
                    + "    NULL AS DATA_ELEMENT_BUSINESS_KEY "
                    + " FROM ETK_LOOKUP_DEFINITION LD "
                    + " JOIN ETK_FORM_CTL_LOOKUP_BINDING FCLB ON LD.LOOKUP_DEFINITION_ID = FCLB.LOOKUP_DEFINITION_ID "
                    + " LEFT JOIN SYSTEM_SCRIPTS SO ON LD.SQL_SCRIPT_OBJECT_ID = SO.SCRIPT_ID "
                    + " WHERE LD.tracking_config_id = (SELECT MAX(tracking_config_id) FROM etk_tracking_config_archive) " ).execute();

            deploymentResult.addMessage("AEA_LS_DATA_ELEMENT_VIEW Created or Updated.");
        } catch (final Exception e) {
            deploymentResult.addMessage("Unable to Create or Update AEA_LS_DATA_ELEMENT_VIEW");
        }
    }

    /**
     * Configures the necessary data for the Header/Calculated Fields component. This includes both reference data and
     * ALTER table statements needed for upgrading existing database to newer versions.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void configureCalculatedFields(final ExecutionContext etk, final DeploymentResult deploymentResult) {

        try {
            final int dataElementCount = etk.createSQL(
                    "select count(*) from R_CF_ELEMENT_TYPE where c_code = "
                            + "'code.usr.cf.elementType.dataElement' ").fetchInt();
            final int custSqlCount = etk.createSQL(
                    "select count(*) from R_CF_ELEMENT_TYPE where c_code = "
                            + "'code.usr.cf.elementType.custom.sql' ").fetchInt();
            final int scriptObjCount = etk.createSQL(
                    "select count(*) from R_CF_ELEMENT_TYPE where c_code = "
                            + "'code.usr.cf.elementType.custom.scriptObject' ").fetchInt();
            final int staticTextCount = etk.createSQL(
                    "select count(*) from R_CF_ELEMENT_TYPE where c_code = "
                            + "'code.usr.cf.elementType.custom.staticText' ").fetchInt();

            int cDataFormLen = 0;
            int cDataObjLen = 0;
            int cScriptObjLen = 0;
            int cDataElementLen = 0;
            int cBaseObjLen = 0;
            int cObjectsLen = 0;

            if (Utility.isSqlServer(etk)) {
                cDataFormLen = etk.createSQL("SELECT character_maximum_length FROM information_schema.columns "
                        + "where table_name = 'M_HDR_DATA_FORMS' and column_name = 'C_DATA_FORM'").fetchInt();
                cDataObjLen = etk.createSQL("SELECT character_maximum_length FROM information_schema.columns "
                        + "where table_name = 'M_HDR_DATA_OBJECTS' and column_name = 'C_DATA_OBJECT'").fetchInt();
                cScriptObjLen = etk.createSQL("SELECT character_maximum_length FROM information_schema.columns "
                        + "where table_name = 'R_CF_ELEMENTS' and column_name = 'C_SCRIPT_OBJECT'").fetchInt();
                cDataElementLen = etk.createSQL("SELECT character_maximum_length FROM information_schema.columns "
                        + "where table_name = 'R_CF_ELEMENTS' and column_name = 'C_DATA_ELEMENT'").fetchInt();
                cBaseObjLen = etk.createSQL("SELECT character_maximum_length FROM information_schema.columns "
                        + "where table_name = 'R_CF_ELEMENTS' and column_name = 'C_BASE_OBJECT'").fetchInt();
                cObjectsLen = etk.createSQL("SELECT character_maximum_length FROM information_schema.columns "
                        + "where table_name = 'R_CF_ELEMENTS' and column_name = 'C_OBJECTS'").fetchInt();
            } else {
                cDataFormLen = etk.createSQL("select char_length from user_tab_cols "
                        + "where table_name = 'M_HDR_DATA_FORMS' and column_name = 'C_DATA_FORM'").fetchInt();
                cDataObjLen = etk.createSQL("select char_length from user_tab_cols "
                        + "where table_name = 'M_HDR_DATA_OBJECTS' and column_name = 'C_DATA_OBJECT'").fetchInt();
                cScriptObjLen = etk.createSQL("select char_length from user_tab_cols "
                        + "where table_name = 'R_CF_ELEMENTS' and column_name = 'C_SCRIPT_OBJECT'").fetchInt();
                cDataElementLen = etk.createSQL("select char_length from user_tab_cols "
                        + "where table_name = 'R_CF_ELEMENTS' and column_name = 'C_DATA_ELEMENT'").fetchInt();
                cBaseObjLen = etk.createSQL("select char_length from user_tab_cols "
                        + "where table_name = 'R_CF_ELEMENTS' and column_name = 'C_BASE_OBJECT'").fetchInt();
                cObjectsLen = etk.createSQL("select char_length from user_tab_cols "
                        + "where table_name = 'R_CF_ELEMENTS' and column_name = 'C_OBJECTS'").fetchInt();
            }

            String result = "";

            if (Utility.isSqlServer(etk)) {

                result += NEWLINE + NEWLINE;

                if (dataElementCount == 0) {
                    etk.createSQL("Insert into R_CF_ELEMENT_TYPE (C_CODE,C_NAME,C_ORDER)" +
                            " values ('code.usr.cf.elementType.dataElement','Data Element',1)").execute();

                    result += "Inserted Data Element CF Element Type" + NEWLINE;
                } else {
                    result += "Data Element CF Element Type already exists." + NEWLINE;
                }

                if (custSqlCount == 0) {
                    etk.createSQL("Insert into R_CF_ELEMENT_TYPE (C_CODE,C_NAME,C_ORDER)" +
                            " values ('code.usr.cf.elementType.custom.sql','Custom - SQL',2)").execute();

                    result += "Inserted Custom - SQL CF Element Type" + NEWLINE;
                } else {
                    result += "Custom - SQL CF Element Type already exists." + NEWLINE;
                }

                if (scriptObjCount == 0) {
                    etk.createSQL("Insert into R_CF_ELEMENT_TYPE (C_CODE,C_NAME,C_ORDER)" +
                            " values ('code.usr.cf.elementType.custom.scriptObject','Custom - Script Object',3)")
                    .execute();

                    result += "Inserted Custom - Script Object CF Element Type" + NEWLINE;
                } else {
                    result += "Custom - Script Object CF Element Type already exists." + NEWLINE;
                }

                if (staticTextCount == 0) {
                    etk.createSQL("Insert into R_CF_ELEMENT_TYPE (C_CODE,C_NAME,C_ORDER)" +
                            " values ('code.usr.cf.elementType.custom.staticText','Static Text',4)").execute();

                    result += "Inserted Static Text CF Element Type" + NEWLINE;
                } else {
                    result += "Static Text CF Element Type already exists." + NEWLINE;
                }

                if (cDataFormLen == ETK_DEFAULT_TEXT_ELEMENT_LENGTH) {
                    etk.createSQL("alter table M_HDR_DATA_FORMS alter column C_DATA_FORM VARCHAR(4000) null").execute();
                    result += "Altered M_HDR_DATA_FORMS.C_DATA_FORM to 4000 characters." + NEWLINE;
                } else {
                    result += "M_HDR_DATA_FORMS.C_DATA_FORM size = " + cDataFormLen + " characters, skipping." + NEWLINE;
                }

                if (cDataObjLen == ETK_DEFAULT_TEXT_ELEMENT_LENGTH) {
                    etk.createSQL("alter table M_HDR_DATA_OBJECTS alter column C_DATA_OBJECT VARCHAR(4000) null")
                    .execute();
                    result += "Altered M_HDR_DATA_OBJECTS.C_DATA_OBJECT to 4000 characters." + NEWLINE;
                } else {
                    result += "M_HDR_DATA_OBJECTS.C_DATA_OBJECT size = " + cDataObjLen + " characters, skipping." + NEWLINE;
                }

                if (cScriptObjLen == ETK_DEFAULT_TEXT_ELEMENT_LENGTH) {
                    etk.createSQL("alter table R_CF_ELEMENTS alter column C_SCRIPT_OBJECT VARCHAR(4000) null")
                    .execute();
                    result += "Altered R_CF_ELEMENTS.C_SCRIPT_OBJECT to 4000 characters." + NEWLINE;
                } else {
                    result += "R_CF_ELEMENTS.C_SCRIPT_OBJECT size = " + cScriptObjLen + " characters, skipping." + NEWLINE;
                }

                if (cDataElementLen == ETK_DEFAULT_TEXT_ELEMENT_LENGTH) {
                    etk.createSQL("alter table R_CF_ELEMENTS alter column C_DATA_ELEMENT VARCHAR(4000) null").execute();
                    result += "Altered R_CF_ELEMENTS.C_DATA_ELEMENT to 4000 characters." + NEWLINE;
                } else {
                    result += "R_CF_ELEMENTS.C_DATA_ELEMENT size = " + cDataElementLen + " characters, skipping." + NEWLINE;
                }

                if (cBaseObjLen == ETK_DEFAULT_TEXT_ELEMENT_LENGTH) {
                    etk.createSQL("alter table R_CF_ELEMENTS alter column C_BASE_OBJECT VARCHAR(4000) null").execute();
                    result += "Altered R_CF_ELEMENTS.C_BASE_OBJECT to 4000 characters." + NEWLINE;
                } else {
                    result += "R_CF_ELEMENTS.C_BASE_OBJECT size = " + cBaseObjLen + " characters, skipping." + NEWLINE;
                }

                if (cObjectsLen == ETK_DEFAULT_TEXT_ELEMENT_LENGTH) {
                    etk.createSQL("alter table R_CF_ELEMENTS alter column C_OBJECTS VARCHAR(4000) null").execute();
                    result += "Altered R_CF_ELEMENTS.C_OBJECTS to 4000 characters." + NEWLINE;
                } else {
                    result += "R_CF_ELEMENTS.C_OBJECTS size = " + cObjectsLen + " characters, skipping." + NEWLINE;
                }

            } else {

                result += NEWLINE + NEWLINE;

                if (dataElementCount == 0) {
                    etk.createSQL("Insert into R_CF_ELEMENT_TYPE (ID,C_CODE,C_NAME,C_ORDER)" +
                            " values (object_id.nextval,'code.usr.cf.elementType.dataElement','Data Element',1)")
                    .execute();

                    result += "Inserted Data Element CF Element Type" + NEWLINE;
                } else {
                    result += "Data Element CF Element Type already exists." + NEWLINE;
                }

                if (custSqlCount == 0) {
                    etk.createSQL("Insert into R_CF_ELEMENT_TYPE (ID,C_CODE,C_NAME,C_ORDER)" +
                            " values (object_id.nextval,'code.usr.cf.elementType.custom.sql','Custom - SQL',2)")
                    .execute();

                    result += "Inserted Custom - SQL CF Element Type" + NEWLINE;
                } else {
                    result += "Custom - SQL CF Element Type already exists." + NEWLINE;
                }

                if (scriptObjCount == 0) {
                    etk.createSQL(
                            "Insert into R_CF_ELEMENT_TYPE (ID,C_CODE,C_NAME,C_ORDER)"
                                    +
                            " values (object_id.nextval,'code.usr.cf.elementType.custom.scriptObject','Custom - Script Object',3)")
                    .execute();

                    result += "Inserted Custom - Script Object CF Element Type" + NEWLINE;
                } else {
                    result += "Custom - Script Object CF Element Type already exists." + NEWLINE;
                }

                if (staticTextCount == 0) {
                    etk.createSQL("Insert into R_CF_ELEMENT_TYPE (ID,C_CODE,C_NAME,C_ORDER)" +
                            " values (object_id.nextval,'code.usr.cf.elementType.custom.staticText','Static Text',4)")
                    .execute();

                    result += "Inserted Static Text CF Element Type" + NEWLINE;
                } else {
                    result += "Static Text CF Element Type already exists." + NEWLINE;
                }

                if (cDataFormLen == ETK_DEFAULT_TEXT_ELEMENT_LENGTH) {
                    etk.createSQL("alter table M_HDR_DATA_FORMS MODIFY (C_DATA_FORM VARCHAR2(4000))").execute();
                    result += "Altered M_HDR_DATA_FORMS.C_DATA_FORM to 4000 characters." + NEWLINE;
                } else {
                    result += "M_HDR_DATA_FORMS.C_DATA_FORM size = " + cDataFormLen + " characters, skipping." + NEWLINE;
                }

                if (cDataObjLen == ETK_DEFAULT_TEXT_ELEMENT_LENGTH) {
                    etk.createSQL("alter table M_HDR_DATA_OBJECTS MODIFY (C_DATA_OBJECT VARCHAR2(4000))").execute();
                    result += "Altered M_HDR_DATA_OBJECTS.C_DATA_OBJECT to 4000 characters." + NEWLINE;
                } else {
                    result += "M_HDR_DATA_OBJECTS.C_DATA_OBJECT size = " + cDataObjLen + " characters, skipping." + NEWLINE;
                }

                if (cScriptObjLen == ETK_DEFAULT_TEXT_ELEMENT_LENGTH) {
                    etk.createSQL("alter table R_CF_ELEMENTS MODIFY (C_SCRIPT_OBJECT VARCHAR2(4000))").execute();
                    result += "Altered R_CF_ELEMENTS.C_SCRIPT_OBJECT to 4000 characters." + NEWLINE;
                } else {
                    result += "R_CF_ELEMENTS.C_SCRIPT_OBJECT size = " + cScriptObjLen + " characters, skipping." + NEWLINE;
                }

                if (cDataElementLen == ETK_DEFAULT_TEXT_ELEMENT_LENGTH) {
                    etk.createSQL("alter table R_CF_ELEMENTS MODIFY (C_DATA_ELEMENT VARCHAR2(4000))").execute();
                    result += "Altered R_CF_ELEMENTS.C_DATA_ELEMENT to 4000 characters." + NEWLINE;
                } else {
                    result += "R_CF_ELEMENTS.C_DATA_ELEMENT size = " + cDataElementLen + " characters, skipping." + NEWLINE;
                }

                if (cBaseObjLen == ETK_DEFAULT_TEXT_ELEMENT_LENGTH) {
                    etk.createSQL("alter table R_CF_ELEMENTS MODIFY (C_BASE_OBJECT VARCHAR2(4000))").execute();
                    result += "Altered R_CF_ELEMENTS.C_BASE_OBJECT to 4000 characters." + NEWLINE;
                } else {
                    result += "R_CF_ELEMENTS.C_BASE_OBJECT size = " + cBaseObjLen + " characters, skipping." + NEWLINE;
                }

                if (cObjectsLen == ETK_DEFAULT_TEXT_ELEMENT_LENGTH) {
                    etk.createSQL("alter table R_CF_ELEMENTS MODIFY (C_OBJECTS VARCHAR2(4000))").execute();
                    result += "Altered R_CF_ELEMENTS.C_OBJECTS to 4000 characters." + NEWLINE;
                } else {
                    result += "R_CF_ELEMENTS.C_OBJECTS size = " + cObjectsLen + " characters, skipping." + NEWLINE;
                }
            }

            deploymentResult.addMessage(result);
        } catch (final Exception e) {
            deploymentResult.addMessage("Error configuring Calculated Fields / Header Banners RDO Data Tables. "
                    + "Ignore if CF/HDR Component is not installed.");
        }
    }

    /**
     * Configure the DB Utils component.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void configureDbUtils(final ExecutionContext etk, final DeploymentResult deploymentResult) {
        try {
            //This will blow up if the table doesnt exist.
            etk.createSQL("select count(*) from T_AEA_RDO_FILE_STAGING").fetchString();
            final List<Map<String, Object>> columnsToUpdate;

            if (Utility.isSqlServer(etk)) {
                columnsToUpdate =
                        etk.createSQL(" SELECT COLUMN_NAME " +
                                " FROM INFORMATION_SCHEMA.COLUMNS " +
                                " where table_name = 'T_AEA_RDO_FILE_STAGING' " +
                                " and data_type = 'int' ")
                        .returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
                        .fetchList();

                if (columnsToUpdate.size() == 0) {
                    deploymentResult.addMessage("T_AEA_RDO_FILE_STAGING - all numeric columns already altered to NUMERIC(19,0)");
                } else {
                    for (final Map<String, Object> columnToUpdate : columnsToUpdate) {
                        etk.createSQL("ALTER TABLE T_AEA_RDO_FILE_STAGING alter column "
                                + columnToUpdate.get("COLUMN_NAME") +
                                " NUMERIC(19,0)").execute();

                        deploymentResult.addMessage(
                                String.format("Altered column T_AEA_RDO_FILE_STAGING. %s to NUMERIC(19,0)",
                                        columnToUpdate.get("COLUMN_NAME")));
                    }
                }
            } else {
                columnsToUpdate =
                        etk.createSQL("select COLUMN_NAME from all_tab_columns "
                                + "where table_name = 'T_AEA_RDO_FILE_STAGING' "
                                + "and DATA_TYPE = 'NUMBER' "
                                + "and data_precision <> 19")
                        .returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
                        .fetchList();

                if (columnsToUpdate.size() == 0) {
                    deploymentResult.addMessage("T_AEA_RDO_FILE_STAGING - all numeric columns already altered to NUMBER(19,0)");
                } else {
                    for (final Map<String, Object> columnToUpdate : columnsToUpdate) {
                        etk.createSQL("ALTER TABLE T_AEA_RDO_FILE_STAGING MODIFY("
                                + columnToUpdate.get("COLUMN_NAME") +
                                " NUMBER(19,0))").execute();

                        deploymentResult.addMessage(
                                String.format("Altered column T_AEA_RDO_FILE_STAGING. %s to NUMBER(19,0)",
                                        columnToUpdate.get("COLUMN_NAME")));
                    }
                }
            }
        } catch (final Exception e) {
            deploymentResult.addMessage("Error configuring T_AEA_RDO_FILE_STAGING - ignore if DBUtils component is not installed.");
        }
    }

    /**
     * Configure values for the T_AEA_CORE_CONFIGURATION table.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void configureAeaCoreConfiguration(final ExecutionContext etk, final DeploymentResult deploymentResult) {

        /* format of entries is C_CODE, C_VALUE, C_DESCRIPTION */
        final String[][] entries = new String[][]{
            {"aea.core.cacheStaticContent", "1", "If set to 1, aearchitecture pages such as DefaultJavascriptController and DefaultCssController will instruct browsers to cache the content. This leads to increased performance on production. \r\n\r\nIf set to 0, these pages will not cache which means that developers will not need to clear their cache constantly"},
            {"dt.enhancedInboxEnabled", "true", "Valid values are 'true' and 'false'.\r\n\r\nIf false the page display option record for this feature is removed when the Dashboard Tools scheduler job is run.  If this feature won't be used it should be set to false to improve performance on the Dashboard."},
            {"dt.systemWideBroadcastEnabled", "true", "Valid values are 'true' and 'false'.\r\n\r\nIf false the page display option record for this feature is removed when the Dashboard Tools scheduler job is run.  If this feature won't be used it should be set to false to improve performance on the Dashboard."},
            {"dt.calendarEnabled", "true", "Valid values are 'true' and 'false'.\r\n\r\nIf false the page display option record for this feature is removed when the Dashboard Tools scheduler job is run.  If this feature won't be used (at least on the Dashboard) it should be set to false to improve performance on the Dashboard."},
            {"enhancedInbox.customJavaScript", "", "The business key of the page which returns any custom JavaScript needed to handle any custom html elements added to an inbox via an InboxDecorator."},
            {"enhancedInbox.customCSS", "", "The business key of the page which returns any custom CSS to decorate the Enhanced Inbox."},
            {"enhancedInbox.customCssClasses", "", "Space separated list of classes to add to the class attribute of the table displaying the inbox data (table ID 'inbox_content_table' when not showing all inboxes).  This can be used in conjunction with the 'enhancedInbox.customCSS' configuration in order to customize the look of the Enhanced Inbox datatable."},
            {"enhancedInbox.displayLength", "20", "The number of records to show in an inbox per page."},
            {"enhancedInbox.displayTitle", "Enhanced Inbox", "The title displayed in the header of the inbox accordion."},
            {"enhancedInbox.showCount", "true", "Valid values are 'true' or 'false'.\r\n\r\nWhether to calculate and show the count of rows for each inbox in the Inbox Selection drop-down list.  Not using the count can improve performance if the count display isn't required by the project."},
            {"enhancedInbox.showAllInboxes", "false", "Valid values are 'true' or 'false'.\r\n\r\nWhether to show all inboxes on the dashboard at once.  This will disregard any groups the inboxes may belong to.  Each inbox will be in its own accordion with the inbox name as the title."},
            {"enhancedInbox.usesGroups", "true", "Valid values are 'true' or 'false'.\r\n\r\nIf set to 'false' group selection will be hidden and all inboxes which would have been shown separately per group will all be in a single dropdown list."},
            {"advancedRecursiveDebug", "false", "Enabled advanced, recursive debugging output for aeaLog"},
            {"swb.label", "System Wide Broadcasts", "Label of the fieldset which contains the current broadcasts."},
            {"swb.height", "125", "Height of the iframe displaying the broadcasts.  This can be an integer value in pixels or 'dynamic'.  Setting to dynamic will adjust the height to be the height of the first page of broadcasts (if paginated) or the height of all the broadcasts (if not paginated).  Dynamic is best used when not paginating and you just want all broadcasts visible without scrolling."},
            {"swb.paginate", "true", "Valid values are 'true' or 'false'.\r\n\r\nIf set to 'false' all broadcasts will be displayed at once."},
            {"swb.showDate", "true", "Valid values are 'true' or 'false'.\r\n\r\nIf set to 'true' all broadcasts will have the date they started appearing next to the title."},
            {"swb.broadcastsPerPage", "1", "An integer representing how many broadcasts should be shown per page (if paginating)."},
            {"swb.noBroadcastsMessage", "No System Wide Broadcasts at this time.", "If configured so that the System Wide Broadcast fieldset is shown even when there are no broadcasts, this value will be shown in the fieldset whenever there are no broadcasts."},
            {"swb.showFieldsetWhenEmpty", "true", "Valid values are 'true' or 'false'.\r\n\r\nIf set to 'false', nothing will be shown on the home page when there are no currently active broadcasts to display."},
            {"calendar.displayArchivedEvents", "true", "Valid values are 'true' or 'false'.\r\n\r\nIf false the calendar will not show any events which have been moved to the archive.  You may want this option if you just want to hold on to the data but not slow down the calendar by showing archived events, which could be a large number."},
            {"calendar.monthsNotArchived", "1", "Number of months to keep calendar events in the main table.  Anything older will be moved to an archive table.  This value works in conjunction with the number configured for 'calendar.monthsNotDeleted' in that once in the archive table, any event older than the number of months in 'calendar.monthsNotDeleted' will be removed from the archive table."},
            {"calendar.monthsNotDeleted", "3", "Once an event occurred X months ago it will be deleted from the archive table.  This value works in conjunction with the number configured for 'calendar.monthsNotArchived'.  If this value is smaller than 'calendar.monthsNotArchived' then nothing will ever get deleted."},
            {"calendar.dashboardWidth", "max", "Width of the calendar on the dashboard.  This can be an integer value in pixels or 'max'.  Setting to max will adjust the width of the calendar to fit the width of the dashboard."},
            {"du.mismatchedColumnTypeExclusions",
                StringUtils.join(Arrays.asList(new String[]{
                        "T_AEA_AUDIT_LOG.C_BASE_ID",
                        "T_AEA_AUDIT_LOG.C_ORGANIZATION_ID",
                        "T_AEA_AUDIT_LOG.C_PARENT_ID",
                        "T_AEA_AUDIT_LOG.C_TRACKING_ID",
                        "T_AEA_AUDIT_LOG.C_USER_ID",
                        "T_AEA_AUDIT_LOG.C_USER_ROLE_ID",
                        "T_AEA_RDO_FILE_STAGING.C_ETK_FILE_ID",
                        "T_AEA_RDO_FILE_STAGING.C_ORDER",
                        "T_AEA_RDO_FILE_STAGING.C_SOURCE_SYSTEM_ID",
                        "R_CF_ELEMENTS.C_BASE_OBJECT",
                        "R_CF_ELEMENTS.C_DATA_ELEMENT",
                        "R_CF_ELEMENTS.C_OBJECTS",
                        "R_CF_ELEMENTS.C_SCRIPT_OBJECT",
                        "M_HDR_DATA_FORMS.C_DATA_FORM",
                "M_HDR_DATA_OBJECTS.C_DATA_OBJECT"}),
                        "\r\n"),
            "A newline-separated list of TABLE.COLUMN to exclude from detection by the Mismatched Column Types Developer Utility."},
            {"eu.daysUntilDeleteEmailsFromQueue", "7", "Email Utility - This is the Number of days that an item needs to be in the \"EU Email Queue\" list (by \"Created Time\") before it will be deleted from the system.\r\n\r\nIf you leave it blank, items will never be deleted."},
            {"eu.enableEmail", "1", "Email Utility - If this value is set to 1, then emails are enabled and will be sent to the email server normally. If this value is set to 0, then emails will not be sent to the email server. \r\n\r\nEmails in EU Email Queue will still change their status to sent, however they will not be sent to the Email Server."},
            {"eu.minutesUntilAbortResendingErrors", "20", "Email Utility - The Email Utility will try to resend emails which have encountered an error automatically, however this is a not good if the problem emails are never corrected. This value puts a limit on how long ago an email must have been created before the Email Queue will stop trying to send it.\r\n\r\nIf this value is blank, the system will never give up trying to resend emails."},
            {"dbutils.rdoExport.rdoExportMaxLines", "1000", "Defines the ideal maximum total size (in number of lines) of the database_inserts_X.sql inside of the rdo_export_XXX.zip. If the header / footer / errors exceed the limit, 1 statement will be included per file. A value of 0 will print all statements in a single file reguardless of size. A size of 1 is recommended for debugging."}
        };
        String newResult = NEWLINE;

        try {
            for(final String[] entry : entries){
                final String code = entry[0];
                final String defaultValue = entry[1];
                final String description = entry[2];

                String matchingId;
                try {
                    matchingId = etk.createSQL("SELECT ID FROM t_aea_core_configuration WHERE c_code = :c_code")
                            .setParameter("c_code", code)
                            .returnEmptyResultSetAs(null)
                            .fetchString();

                    if(matchingId == null){
                        etk.createSQL(Utility.isSqlServer(etk)
                                ? "INSERT INTO t_aea_core_configuration(c_code, c_description, c_value) VALUES(:c_code, :description, :value)"
                                  : "INSERT INTO t_aea_core_configuration(id, c_code, c_description, c_value) VALUES(OBJECT_ID.NEXTVAL, :c_code, :description, :value)")
                        .setParameter("c_code", code)
                        .setParameter("description", description)
                        .setParameter("value", defaultValue)
                        .execute();
                        newResult += String.format("Inserted T_AEA_CORE_CONFIGURATION %s = %s%s", code, defaultValue, NEWLINE);
                    }else{
                        etk.createSQL("UPDATE t_aea_core_configuration SET c_description = :description WHERE id = :id")
                        .setParameter("description", description)
                        .setParameter("id", matchingId)
                        .execute();
                        newResult += String.format("Updated Description of T_AEA_CORE_CONFIGURATION %s%s", code, NEWLINE);
                    }
                } catch (final IncorrectResultSizeDataAccessException e) {
                    newResult += String.format("Found multiple values in T_AEA_CORE_CONFIGURATION with C_CODE = %s%s", code, NEWLINE);
                }
            }
            deploymentResult.addMessage(newResult);

        } catch (final DataAccessException e) {
            deploymentResult.addMessage("Error configuring T_AEA_CORE_CONFIGURATION RDO Data - ignore if AEA Configuration is not installed.");
        }

        try {

            final int lsMaxSearchResults = etk.createSQL(
                    "select count(*) from T_AEA_CORE_CONFIGURATION where c_code = "
                            + "'ls.maxSearchResults' ").fetchInt();
            final int writeDebugToLog = etk.createSQL(
                    "select count(*) from T_AEA_CORE_CONFIGURATION where c_code = "
                            + "'writeDebugToLog' ").fetchInt();
            final int lsCharactersNeededForSearch = etk.createSQL(
                    "select count(*) from T_AEA_CORE_CONFIGURATION where c_code = "
                            + "'ls.charactersNeededForSearch' ").fetchInt();
            final int lsDebugMode = etk.createSQL(
                    "select count(*) from T_AEA_CORE_CONFIGURATION where c_code = "
                            + "'ls.debugMode' ").fetchInt();

            String result = "";

            if (Utility.isSqlServer(etk)) {
                result += NEWLINE + NEWLINE;

                if (lsMaxSearchResults == 0) {
                    etk.createSQL("Insert into T_AEA_CORE_CONFIGURATION (C_CODE,C_DESCRIPTION,C_VALUE)" +
                            " values ('ls.maxSearchResults',"
                            + "'The maximum number of search results live search windows will return. "
                            + "Must be a positive integer between 1 and 200, otherwise a default "
                            + "of 20 will be used.', '20')").execute();

                    result += "Inserted T_AEA_CORE_CONFIGURATION ls.maxSearchResults = 20" + NEWLINE;
                } else {
                    result += "T_AEA_CORE_CONFIGURATION ls.maxSearchResults already exists." + NEWLINE;
                }


                if (writeDebugToLog == 0) {
                    etk.createSQL("Insert into T_AEA_CORE_CONFIGURATION (C_CODE,C_DESCRIPTION,C_VALUE)" +
                            " values ('writeDebugToLog',"
                            + "'Enables advanced AEA Debug Logging', 'true')").execute();

                    result += "Inserted T_AEA_CORE_CONFIGURATION writeDebugToLog = true" + NEWLINE;
                } else {
                    result += "T_AEA_CORE_CONFIGURATION writeDebugToLog already exists." + NEWLINE;
                }


                if (lsCharactersNeededForSearch == 0) {
                    etk.createSQL("Insert into T_AEA_CORE_CONFIGURATION (C_CODE,C_DESCRIPTION,C_VALUE)" +
                            " values ('ls.charactersNeededForSearch',"
                            + "'Number of characters a user must enter before live search performs a search. "
                            + "Must be a positive integer between 1 and 10, otherwise the "
                            + "system will default to a value of 3.', '2')").execute();

                    result += "Inserted T_AEA_CORE_CONFIGURATION ls.charactersNeededForSearch = 2" + NEWLINE;
                } else {
                    result += "T_AEA_CORE_CONFIGURATION ls.charactersNeededForSearch already exists." + NEWLINE;
                }



                if (lsDebugMode == 0) {
                    etk.createSQL("Insert into T_AEA_CORE_CONFIGURATION (C_CODE,C_DESCRIPTION,C_VALUE)" +
                            " values ('ls.debugMode',"
                            + "'Valid values are \"true\", \"t\" and \"1\"."
                            + "All others evaluate to false.', 'false')").execute();

                    result += "Inserted T_AEA_CORE_CONFIGURATION ls.debugMode = false" + NEWLINE;
                } else {
                    result += "T_AEA_CORE_CONFIGURATION ls.debugMode already exists." + NEWLINE;
                }

            } else {

                result += NEWLINE + NEWLINE;

                if (lsMaxSearchResults == 0) {
                    etk.createSQL("Insert into T_AEA_CORE_CONFIGURATION (ID,C_CODE,C_DESCRIPTION,C_VALUE)" +
                            " values (object_id.nextval, 'ls.maxSearchResults',"
                            + "'The maximum number of search results live search windows will return. "
                            + "Must be a positive integer between 1 and 200, otherwise a default "
                            + "of 20 will be used.', '20')").execute();

                    result += "Inserted T_AEA_CORE_CONFIGURATION ls.maxSearchResults" + NEWLINE;
                } else {
                    result += "T_AEA_CORE_CONFIGURATION ls.maxSearchResults already exists." + NEWLINE;
                }

                if (writeDebugToLog == 0) {
                    etk.createSQL("Insert into T_AEA_CORE_CONFIGURATION (ID,C_CODE,C_DESCRIPTION,C_VALUE)" +
                            " values (object_id.nextval, 'writeDebugToLog',"
                            + "'Enables advanced AEA Debug Logging', 'true')").execute();

                    result += "Inserted T_AEA_CORE_CONFIGURATION writeDebugToLog = true" + NEWLINE;
                } else {
                    result += "T_AEA_CORE_CONFIGURATION writeDebugToLog already exists." + NEWLINE;
                }


                if (lsCharactersNeededForSearch == 0) {
                    etk.createSQL("Insert into T_AEA_CORE_CONFIGURATION (ID,C_CODE,C_DESCRIPTION,C_VALUE)" +
                            " values (object_id.nextval, 'ls.charactersNeededForSearch',"
                            + "'Number of characters a user must enter before live search performs a search. "
                            + "Must be a positive integer between 1 and 10, otherwise the "
                            + "system will default to a value of 3.', '2')").execute();

                    result += "Inserted T_AEA_CORE_CONFIGURATION ls.charactersNeededForSearch = 2" + NEWLINE;
                } else {
                    result += "T_AEA_CORE_CONFIGURATION ls.charactersNeededForSearch already exists." + NEWLINE;
                }


                if (lsDebugMode == 0) {
                    etk.createSQL("Insert into T_AEA_CORE_CONFIGURATION (ID,C_CODE,C_DESCRIPTION,C_VALUE)" +
                            " values (object_id.nextval, 'ls.debugMode',"
                            + "'Valid values are \"true\", \"t\" and \"1\"."
                            + "All others evaluate to false.', 'false')").execute();

                    result += "Inserted T_AEA_CORE_CONFIGURATION ls.debugMode = false" + NEWLINE;
                } else {
                    result += "T_AEA_CORE_CONFIGURATION ls.debugMode already exists." + NEWLINE;
                }
            }

            deploymentResult.addMessage(result);
        } catch (final Exception e) {
            deploymentResult.addMessage("Error configuring T_AEA_CORE_CONFIGURATION RDO Data - "
                    + "ignore if AEA Configuration is not installed.");
        }
    }

    /**
     * Configure the Audit Log component.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void configureAeaAuditLog(final ExecutionContext etk, final DeploymentResult deploymentResult) {
        try {
            //This will blow up if the table doesnt exist.
            etk.createSQL("select count(*) from t_aea_audit_log").fetchString();
            final List<Map<String, Object>> columnsToUpdate;

            if (Utility.isSqlServer(etk)) {
                columnsToUpdate =
                        etk.createSQL(" SELECT COLUMN_NAME " +
                                " FROM INFORMATION_SCHEMA.COLUMNS " +
                                " where table_name = 'T_AEA_AUDIT_LOG' " +
                                " and data_type = 'int' ")
                        .returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
                        .fetchList();

                if (columnsToUpdate.size() == 0) {
                    deploymentResult.addMessage("T_AEA_AUDIT_LOG - all numeric columns already altered to NUMERIC(19,0)");
                } else {
                    for (final Map<String, Object> columnToUpdate : columnsToUpdate) {
                        etk.createSQL(String.format("ALTER TABLE T_AEA_AUDIT_LOG alter column %s NUMERIC(19,0) ",
                                columnToUpdate.get("COLUMN_NAME")))
                        .execute();

                        deploymentResult.addMessage(String.format("Altered column T_AEA_AUDIT_LOG. %s to NUMERIC(19,0)",
                                columnToUpdate.get("COLUMN_NAME")));
                    }
                }
            } else {
                columnsToUpdate =
                        etk.createSQL("select COLUMN_NAME from all_tab_columns "
                                + "where table_name = 'T_AEA_AUDIT_LOG' "
                                + "and DATA_TYPE = 'NUMBER' "
                                + "and data_precision <> 19")
                        .returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
                        .fetchList();

                if (columnsToUpdate.size() == 0) {
                    deploymentResult.addMessage("T_AEA_AUDIT_LOG - all numeric columns already altered to NUMBER(19,0)");
                } else {
                    for (final Map<String, Object> columnToUpdate : columnsToUpdate) {
                        etk.createSQL("ALTER TABLE T_AEA_AUDIT_LOG MODIFY("
                                + columnToUpdate.get("COLUMN_NAME") +
                                " NUMBER(19,0))").execute();

                        deploymentResult.addMessage(
                                String.format("Altered column T_AEA_AUDIT_LOG. %s to NUMBER(19,0)",
                                        columnToUpdate.get("COLUMN_NAME")));
                    }
                }
            }

            final List<Map<String, Object>> existingIndexes;

            if (Utility.isSqlServer(etk)) {
                existingIndexes =
                        etk.createSQL( " select si.name as INDEX_NAME " +
                                " from sys.indexes si " +
                                " JOIN sys.objects so ON si.object_id = so.object_id " +
                                " where si.name like 'AEA_%' " +
                                " and so.name = 'T_AEA_AUDIT_LOG' "
                                ).returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
                        .fetchList();
            } else {
                existingIndexes =
                        etk.createSQL( "select INDEX_NAME from all_indexes "
                                + "where table_name = 'T_AEA_AUDIT_LOG' and INDEX_NAME LIKE ('AEA_%')"
                                ).returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
                        .fetchList();
            }

            final HashMap<String, String> indexMap = new HashMap<>();

            for (final Map<String, Object> anIndex : existingIndexes) {
                indexMap.put((String) anIndex.get("INDEX_NAME"), null);

                deploymentResult.addMessage(String.format("Index T_AEA_AUDIT_LOG. %s already exists, skipping.",
                        anIndex.get("INDEX_NAME")));
            }

            if (!indexMap.containsKey("AEA_AL_C_BASE_ID")) {
                etk.createSQL("CREATE INDEX AEA_AL_C_BASE_ID ON T_AEA_AUDIT_LOG (C_BASE_ID)").execute();
                deploymentResult.addMessage("Created new index T_AEA_AUDIT_LOG.AEA_AL_C_BASE_ID");
            }
            if (!indexMap.containsKey("AEA_AL_C_PARENT_ID")) {
                etk.createSQL("CREATE INDEX AEA_AL_C_PARENT_ID ON T_AEA_AUDIT_LOG (C_PARENT_ID)").execute();
                deploymentResult.addMessage("Created new index T_AEA_AUDIT_LOG.AEA_AL_C_PARENT_ID");
            }
            if (!indexMap.containsKey("AEA_AL_C_USER_ROLE_ID")) {
                etk.createSQL("CREATE INDEX AEA_AL_C_USER_ROLE_ID ON T_AEA_AUDIT_LOG (C_USER_ROLE_ID)").execute();
                deploymentResult.addMessage("Created new index T_AEA_AUDIT_LOG.AEA_AL_C_USER_ROLE_ID");
            }
            if (!indexMap.containsKey("AEA_AL_C_TRACKING_ID")) {
                etk.createSQL("CREATE INDEX AEA_AL_C_TRACKING_ID ON T_AEA_AUDIT_LOG (C_TRACKING_ID)").execute();
                deploymentResult.addMessage("Created new index T_AEA_AUDIT_LOG.AEA_AL_C_TRACKING_ID");
            }
            if (!indexMap.containsKey("AEA_AL_C_ORGANIZATION_ID")) {
                etk.createSQL("CREATE INDEX AEA_AL_C_ORGANIZATION_ID ON T_AEA_AUDIT_LOG (C_ORGANIZATION_ID)").execute();
                deploymentResult.addMessage("Created new index T_AEA_AUDIT_LOG.AEA_AL_C_ORGANIZATION_ID");
            }
            if (!indexMap.containsKey("AEA_AL_C_USER_ID")) {
                etk.createSQL("CREATE INDEX AEA_AL_C_USER_ID ON T_AEA_AUDIT_LOG (C_USER_ID)").execute();
                deploymentResult.addMessage("Created new index T_AEA_AUDIT_LOG.AEA_AL_C_USER_ID");
            }

        } catch (final Exception e) {
            deploymentResult.addMessage("Error configuring T_AEA_AUDIT_LOG - ignore if Audit Log component is not installed.");
        }
    }

    /**
     * Configure the Dashboard Tools component.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void configureDashboardTools(final ExecutionContext etk, final DeploymentResult deploymentResult){
        Integer tableCount = 0;
        try{
            if (Utility.isSqlServer(etk)) {
                tableCount = etk.createSQL("select count(*) from sys.tables where name = 'R_CALENDAR_EVENT_TYPE'").fetchInt();
            }else{
                tableCount = etk.createSQL("select count(*) from user_tables where table_name = 'R_CALENDAR_EVENT_TYPE'").fetchInt();
            }
        }catch(final Exception e){
            //Nothing! Dashboard Tools is not installed!
        }

        if(tableCount > 0){
            try{
                final List<Map<String, Object>> existingIndexes;

                if (Utility.isSqlServer(etk)) {
                    existingIndexes =
                            etk.createSQL( " select si.name as INDEX_NAME " +
                                    " from sys.indexes si " +
                                    " JOIN sys.objects so ON si.object_id = so.object_id " +
                                    " where si.name like 'AEA_DT_CALENDAR_%' " +
                                    " and (so.name = 'M_CALENDAR_ASSOCIATED_OBJECT' or so.name = 'M_CALENDAR_ASSOC_OBJCT_ARCHIVE') "
                                    ).returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
                            .fetchList();
                } else {
                    existingIndexes =
                            etk.createSQL( "select INDEX_NAME from all_indexes "
                                    + "where (table_name = 'M_CALENDAR_ASSOCIATED_OBJECT' OR table_name = 'M_CALENDAR_ASSOC_OBJCT_ARCHIVE') and INDEX_NAME LIKE ('AEA_DT_CALENDAR_%')"
                                    ).returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
                            .fetchList();
                }

                final HashMap<String, String> indexMap = new HashMap<>();

                for (final Map<String, Object> anIndex : existingIndexes) {
                    indexMap.put((String) anIndex.get("INDEX_NAME"), null);
                    deploymentResult.addMessage(String.format("Index %s already exists, skipping.",
                            anIndex.get("INDEX_NAME")));
                }


                if (!indexMap.containsKey("AEA_DT_CALENDAR_ASSOC_OBJ")) {
                    etk.createSQL("CREATE INDEX AEA_DT_CALENDAR_ASSOC_OBJ ON M_CALENDAR_ASSOCIATED_OBJECT (C_ASSOCIATED_OBJECT_ID, C_ASSOCIATED_OBJECT_TYPE)").execute();
                    deploymentResult.addMessage("Created new index M_CALENDAR_ASSOCIATED_OBJECT.AEA_DT_CALENDAR_ASSOC_OBJ");
                }
                if (!indexMap.containsKey("AEA_DT_CALENDAR_ASSOC_OBJ_ARCV")) {
                    etk.createSQL("CREATE INDEX AEA_DT_CALENDAR_ASSOC_OBJ_ARCV ON M_CALENDAR_ASSOC_OBJCT_ARCHIVE (C_ASSOCIATED_OBJECT_ID, C_ASSOCIATED_OBJECT_TYPE)").execute();
                    deploymentResult.addMessage("Created new index M_CALENDAR_ASSOC_OBJCT_ARCHIVE.AEA_DT_CALENDAR_ASSOC_OBJ_ARCHIVE");
                }
            } catch (final Exception e) {
                deploymentResult.addMessage("Error configuring Dashboard Tools - ignore if Dashboard Tools component is not installed.");
            }

            try {
                final int hasRecord = etk.createSQL("SELECT COUNT(0) FROM R_CALENDAR_EVENT_TYPE ").fetchInt();
                if(hasRecord == 0){
                    String assignedInsertStmt = "INSERT INTO R_CALENDAR_EVENT_TYPE (ID, C_NAME, C_CODE) VALUES (OBJECT_ID.NEXTVAL, 'Assigned', 'calendar.assigned')";
                    String globalInsertStmt = "INSERT INTO R_CALENDAR_EVENT_TYPE (ID, C_NAME, C_CODE) VALUES (OBJECT_ID.NEXTVAL, 'Global', 'calendar.global')";
                    String editableInsertStmt = "INSERT INTO R_CALENDAR_EVENT_TYPE (ID, C_NAME, C_CODE) VALUES (OBJECT_ID.NEXTVAL, 'Editable', 'calendar.editable')";
                    if(Utility.isSqlServer(etk)){
                        assignedInsertStmt = assignedInsertStmt.replace("ID, ", "");
                        assignedInsertStmt = assignedInsertStmt.replace("OBJECT_ID.NEXTVAL, ", "");
                        globalInsertStmt = globalInsertStmt.replace("ID, ", "");
                        globalInsertStmt = globalInsertStmt.replace("OBJECT_ID.NEXTVAL, ", "");
                        editableInsertStmt = editableInsertStmt.replace("ID, ", "");
                        editableInsertStmt = editableInsertStmt.replace("OBJECT_ID.NEXTVAL, ", "");
                    }
                    etk.createSQL(assignedInsertStmt).execute();
                    etk.createSQL(globalInsertStmt).execute();
                    etk.createSQL(editableInsertStmt).execute();
                }

            } catch (final Exception e) {
                deploymentResult.addMessage("Error configuring Dashboard Tools - ignore if Dashboard Tools component is not installed.");
            }

            try {
                final int hasRecord = etk.createSQL("SELECT COUNT(0) FROM R_CALENDAR_ASSOC_OBJECT_TYPE WHERE C_CODE = 'ETK_USER' ").fetchInt();
                if(hasRecord == 0){
                    String insertStmt = "INSERT INTO R_CALENDAR_ASSOC_OBJECT_TYPE (ID, C_CODE) VALUES (OBJECT_ID.NEXTVAL, 'ETK_USER')";
                    if(Utility.isSqlServer(etk)){
                        insertStmt = insertStmt.replace("ID, ", "");
                        insertStmt = insertStmt.replace("OBJECT_ID.NEXTVAL, ", "");
                    }
                    etk.createSQL(insertStmt).execute();
                }

            } catch (final Exception e) {
                deploymentResult.addMessage("Error configuring Dashboard Tools - ignore if Dashboard Tools component is not installed.");
            }
        }
    }

    /**
     * Ensures that a SQL Server procedure with the given name exists. If one does not exist, it is created.
     * This is useful because SQL Server has different syntax for creating a procedure than for updating it, so you can
     * ensure that the procedure exists, then blindly update it.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     *
     * @param procedureName The name of the procedure which should exist.
     */
    private static void ensureSqlServerProcedureExists(final ExecutionContext etk, final DeploymentResult deploymentResult, final String procedureName){
        try {
            etk.createSQL(String.format("IF OBJECT_ID('DBO.%s', 'p') IS NULL\r\n\tEXEC('CREATE PROCEDURE %s AS SELECT 1')",
                    procedureName, procedureName))
            .execute();
        } catch (final DataAccessException e) {
            deploymentResult.addMessage(String.format("Error encountered creating stub for %s", procedureName));
        }
    }

    /**
     * Create/Update a stored procedure.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     * @param procedureName The procedure name
     * @param sqlServerAlterStatement ALTER PROCEDURE statement to be used for SQL Server
     * @param oracleCreateReplaceStatement CREATE OR REPLACE PROCEDURE statement to be used for Oracle
     */
    private static void createProcedure(final ExecutionContext etk, final DeploymentResult deploymentResult, final String procedureName, final String sqlServerAlterStatement, final String oracleCreateReplaceStatement){
        final String query;

        if(Utility.isSqlServer(etk)){
            ensureSqlServerProcedureExists(etk, deploymentResult, procedureName);
            query = sqlServerAlterStatement;
        }else{
            query = oracleCreateReplaceStatement;
        }

        try{
            etk.createSQL(query).execute();
            deploymentResult.addMessage(String.format("%s created", procedureName));
        }catch(final DataAccessException e){
            deploymentResult.addMessage(String.format("Error creating %s", procedureName));
        }
    }

    /**
     * Ensure the AEA_CORE_ADD_JBPM_LOG_ENTRY procedure exists.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void createAddJbpmLogEntry(final ExecutionContext etk, final DeploymentResult deploymentResult) {
        createProcedure(etk,
                deploymentResult,
                "AEA_CORE_ADD_JBPM_LOG_ENTRY", "ALTER PROCEDURE AEA_CORE_ADD_JBPM_LOG_ENTRY(@PP_PROCESS_DEFINITION_ID BIGINT,\r\n                                @PP_LOG_CLASS VARCHAR(MAX),\r\n                                @PP_INDEX BIGINT,\r\n                                @PP_TOKEN_ID BIGINT,\r\n                                @PP_PARENT_LOG_INDEX BIGINT,\r\n                                @PP_TRANSITION_NAME VARCHAR(MAX),\r\n                                @PP_INCLUDE_SOURCE_AND_DEST BIGINT,\r\n                                @PP_VARIABLE_INSTANCE_NAME VARCHAR(MAX),\r\n                                @PP_NEW_LONG_VALUE BIGINT,\r\n                                @PP_NEW_STRING_VALUE VARCHAR(MAX)) AS\r\n  BEGIN\r\n\r\n    INSERT INTO jbpm_log(class_, index_, date_, token_, parent_, message_, exception_, action_, node_, enter_, leave_, duration_, transition_, child_, sourcenode_, destinationnode_, variableinstance_, oldbytearray_, newbytearray_, olddatevalue_, newdatevalue_, olddoublevalue_, newdoublevalue_, oldlongidclass_, oldlongidvalue_, newlongidclass_, newlongidvalue_, oldstringidclass_, oldstringidvalue_, newstringidclass_, newstringidvalue_, oldlongvalue_, newlongvalue_, oldstringvalue_, newstringvalue_, taskinstance_, taskactorid_, taskoldactorid_, swimlaneinstance_)\r\n            VALUES(@PP_LOG_CLASS, @PP_INDEX, DBO.ETKF_GETSERVERTIME(), @PP_TOKEN_ID, \r\n                  (SELECT logSub.id_ FROM jbpm_log logSub WHERE logSub.token_ = @PP_TOKEN_ID AND logSub.index_ = @PP_PARENT_LOG_INDEX), \r\n                  NULL, NULL, NULL, NULL, NULL, NULL, NULL, \r\n                  (SELECT transition.id_ FROM jbpm_transition transition WHERE transition.processdefinition_ = @PP_PROCESS_DEFINITION_ID AND transition.name_ = @PP_TRANSITION_NAME), \r\n                  NULL, \r\n                  CASE WHEN @PP_INCLUDE_SOURCE_AND_DEST = 1 THEN (SELECT transition.from_ FROM jbpm_transition transition WHERE transition.processdefinition_ = @PP_PROCESS_DEFINITION_ID AND transition.name_ = @PP_TRANSITION_NAME) END, \r\n                  CASE WHEN @PP_INCLUDE_SOURCE_AND_DEST = 1 THEN (SELECT transition.to_ FROM jbpm_transition transition WHERE transition.processdefinition_ = @PP_PROCESS_DEFINITION_ID AND transition.name_ = @PP_TRANSITION_NAME) END, \r\n                  (SELECT variableInstance.id_ FROM jbpm_variableinstance variableInstance WHERE variableInstance.token_ = @PP_TOKEN_ID AND variableInstance.name_ = @PP_VARIABLE_INSTANCE_NAME), \r\n                  NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, @PP_NEW_LONG_VALUE, NULL, @PP_NEW_STRING_VALUE, NULL, NULL, NULL, NULL);\r\n  END;", "create or replace \r\nPROCEDURE AEA_CORE_ADD_JBPM_LOG_ENTRY(PP_PROCESS_DEFINITION_ID IN NUMBER,\r\n                                PP_LOG_CLASS IN VARCHAR2,\r\n                                PP_INDEX IN NUMBER,\r\n                                PP_TOKEN_ID IN NUMBER,\r\n                                PP_PARENT_LOG_INDEX IN NUMBER,\r\n                                PP_TRANSITION_NAME IN VARCHAR2,\r\n                                PP_INCLUDE_SOURCE_AND_DEST IN NUMBER,\r\n                                PP_VARIABLE_INSTANCE_NAME IN VARCHAR2,\r\n                                PP_NEW_LONG_VALUE IN NUMBER,\r\n                                PP_NEW_STRING_VALUE IN VARCHAR2) AS\r\n  BEGIN\r\n    INSERT INTO jbpm_log(id_, class_, index_, date_, token_, parent_, message_, exception_, action_, node_, enter_, leave_, duration_, transition_, child_, sourcenode_, destinationnode_, variableinstance_, oldbytearray_, newbytearray_, olddatevalue_, newdatevalue_, olddoublevalue_, newdoublevalue_, oldlongidclass_, oldlongidvalue_, newlongidclass_, newlongidvalue_, oldstringidclass_, oldstringidvalue_, newstringidclass_, newstringidvalue_, oldlongvalue_, newlongvalue_, oldstringvalue_, newstringvalue_, taskinstance_, taskactorid_, taskoldactorid_, swimlaneinstance_)\r\n            VALUES(HIBERNATE_SEQUENCE.NEXTVAL, PP_LOG_CLASS, PP_INDEX, ETKF_GETSERVERTIME(), PP_TOKEN_ID, \r\n                  (SELECT logSub.id_ FROM jbpm_log logSub WHERE logSub.token_ = PP_TOKEN_ID AND logSub.index_ = PP_PARENT_LOG_INDEX), \r\n                  NULL, NULL, NULL, NULL, NULL, NULL, NULL, \r\n                  (SELECT transition.id_ FROM jbpm_transition transition WHERE transition.processdefinition_ = PP_PROCESS_DEFINITION_ID AND transition.name_ = PP_TRANSITION_NAME), \r\n                  NULL, \r\n                  CASE WHEN PP_INCLUDE_SOURCE_AND_DEST = 1 THEN (SELECT transition.from_ FROM jbpm_transition transition WHERE transition.processdefinition_ = PP_PROCESS_DEFINITION_ID AND transition.name_ = PP_TRANSITION_NAME) END, \r\n                  CASE WHEN PP_INCLUDE_SOURCE_AND_DEST = 1 THEN (SELECT transition.to_ FROM jbpm_transition transition WHERE transition.processdefinition_ = PP_PROCESS_DEFINITION_ID AND transition.name_ = PP_TRANSITION_NAME) END, \r\n                  (SELECT variableInstance.id_ FROM jbpm_variableinstance variableInstance WHERE variableInstance.token_ = PP_TOKEN_ID AND variableInstance.name_ = PP_VARIABLE_INSTANCE_NAME), \r\n                  NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, PP_NEW_LONG_VALUE, NULL, PP_NEW_STRING_VALUE, NULL, NULL, NULL, NULL);\r\n  END;");
    }

    /**
     * Ensure the AEA_CORE_ADD_JBPM procedure exists.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void createAddJbpm(final ExecutionContext etk, final DeploymentResult deploymentResult){
        createProcedure(etk,
                deploymentResult,
                "AEA_CORE_ADD_JBPM", "ALTER\r\nPROCEDURE\r\nAEA_CORE_ADD_JBPM (@PP_OBJECT_BUSINESS_KEY VARCHAR(MAX),\r\n        @PP_TRACKING_ID BIGINT,\r\n        @PP_TRANSITION_NAME VARCHAR(MAX)) AS\r\nBEGIN\r\n  DECLARE\r\n\r\n  @VV_TRACKING_CONFIG_ID BIGINT,\r\n  @VV_ARCHIVE_ID BIGINT,\r\n  \r\n  @VV_TABLE_NAME VARCHAR(50),\r\n  \r\n  @VV_PROCESS_DEFINITION_ID BIGINT,\r\n  @VV_T_MODULE_DEFINITION_ID BIGINT,\r\n  @VV_INTERIM_NODE_ID BIGINT,\r\n  @VV_INTERIM_NODE_NAME VARCHAR(MAX),\r\n  \r\n  @VV_TOKEN_ID BIGINT,\r\n  @VV_TOKEN_VARIABLE_MAP_ID BIGINT,\r\n  @VV_PROCESS_INSTANCE_ID BIGINT,\r\n  \r\n  @VV_DYNAMIC_QUERY NVARCHAR(MAX);\r\n\r\n  BEGIN\r\n    SELECT @VV_TRACKING_CONFIG_ID = MAX (tracking_config_id) FROM etk_tracking_config_archive;\r\n    \r\n    SELECT @VV_ARCHIVE_ID  = archive_id FROM etk_tracking_config_archive WHERE tracking_config_id = @VV_TRACKING_CONFIG_ID;\r\n    \r\n    SELECT @VV_TABLE_NAME  = table_name FROM etk_data_object WHERE business_key = @PP_OBJECT_BUSINESS_KEY AND tracking_config_id = @VV_TRACKING_CONFIG_ID;\r\n    \r\n    \r\n    SELECT @VV_PROCESS_DEFINITION_ID = processDefinition.id_, @VV_INTERIM_NODE_ID = interimNode.id_, @VV_INTERIM_NODE_NAME = interimNode.name_, @VV_T_MODULE_DEFINITION_ID = moduleDefinition.id_ FROM jbpm_processdefinition processDefinition JOIN jbpm_node startNode ON startNode.id_ = processDefinition.startstate_ JOIN jbpm_transition transition ON transition.from_ = startNode.id_ AND transition.name_ = @PP_TRANSITION_NAME JOIN jbpm_node interimNode ON interimNode.id_ = transition.to_ JOIN jbpm_moduledefinition moduleDefinition ON moduleDefinition.processdefinition_ = processDefinition.id_ AND moduleDefinition.class_ = 'T' WHERE processDefinition.name_ = (SELECT dataObject.object_name FROM etk_data_object dataObject WHERE dataObject.business_key = @PP_OBJECT_BUSINESS_KEY AND dataObject.tracking_config_id = @VV_TRACKING_CONFIG_ID ) AND processDefinition.version_ = (SELECT MAX(processDefinitionSub.version_) FROM jbpm_processdefinition processDefinitionSub WHERE processDefinitionSub.name_ = (SELECT dataObjectSub.object_name FROM etk_data_object dataObjectSub WHERE dataObjectSub.business_key = @PP_OBJECT_BUSINESS_KEY AND dataObjectSub.tracking_config_id = @VV_TRACKING_CONFIG_ID ) );\r\n    \r\n    INSERT INTO jbpm_processinstance(start_, end_, processdefinition_, roottoken_, superprocesstoken_) \r\n            VALUES(DBO.ETKF_GETSERVERTIME(), NULL, @VV_PROCESS_DEFINITION_ID, NULL, NULL);\r\n\tSELECT @VV_PROCESS_INSTANCE_ID = SCOPE_IDENTITY();\r\n\r\n\tSELECT @VV_DYNAMIC_QUERY = 'UPDATE ' + @VV_TABLE_NAME + ' SET id_workflow = @VV_PROCESS_INSTANCE_ID, id_archive = @VV_ARCHIVE_ID, state_label = @VV_INTERIM_NODE_NAME WHERE id = @PP_TRACKING_ID';\r\n\r\n\tEXEC SP_EXECUTESQL @VV_DYNAMIC_QUERY,\r\n            N'@VV_PROCESS_INSTANCE_ID BIGINT, @VV_ARCHIVE_ID BIGINT, @VV_INTERIM_NODE_NAME VARCHAR, @PP_TRACKING_ID BIGINT', \r\n\t\t\t@VV_PROCESS_INSTANCE_ID, @VV_ARCHIVE_ID, @VV_INTERIM_NODE_NAME, @PP_TRACKING_ID;\r\n\r\n    INSERT INTO jbpm_token(name_, start_, end_, nodeenter_, nextlogindex_, isabletoreactivateparent_, isterminationimplicit_, node_, processinstance_, parent_) \r\n          VALUES(NULL, DBO.ETKF_GETSERVERTIME(), NULL, DBO.ETKF_GETSERVERTIME(), 9, 1, 0, @VV_INTERIM_NODE_ID, @VV_PROCESS_INSTANCE_ID, NULL);\r\n    SELECT @VV_TOKEN_ID = SCOPE_IDENTITY();\r\n\r\n    UPDATE jbpm_processinstance SET roottoken_ = @VV_TOKEN_ID WHERE id_ = @VV_PROCESS_INSTANCE_ID;\r\n    \r\n    INSERT INTO jbpm_moduleinstance(class_, processinstance_, TASKMGMTDEFINITION_, name_)\r\n            VALUES('C', @VV_PROCESS_INSTANCE_ID, NULL, 'org.jbpm.context.exe.ContextInstance');\r\n    INSERT INTO jbpm_moduleinstance(class_, processinstance_, TASKMGMTDEFINITION_, name_)\r\n            VALUES('T', @VV_PROCESS_INSTANCE_ID, @VV_T_MODULE_DEFINITION_ID, 'org.jbpm.taskmgmt.exe.TaskMgmtInstance');\r\n    \r\n    INSERT INTO jbpm_tokenvariablemap(token_, contextinstance_) \r\n            VALUES(@VV_TOKEN_ID, (SELECT moduleInstance.id_ FROM jbpm_moduleinstance moduleInstance WHERE moduleInstance.class_ = 'C' AND moduleInstance.processinstance_ = @VV_PROCESS_INSTANCE_ID));\r\n    SELECT @VV_TOKEN_VARIABLE_MAP_ID = SCOPE_IDENTITY();\r\n\r\n    INSERT INTO jbpm_variableinstance(class_, name_, converter_, token_, tokenvariablemap_, processinstance_, bytearrayvalue_, datevalue_, doublevalue_, longidclass_, longvalue_, stringidclass_, stringvalue_)\r\n            VALUES('L', 'archiveId', NULL, @VV_TOKEN_ID, @VV_TOKEN_VARIABLE_MAP_ID, @VV_PROCESS_INSTANCE_ID, NULL, NULL, NULL, NULL, @VV_ARCHIVE_ID, NULL, NULL);\r\n    INSERT INTO jbpm_variableinstance(class_, name_, converter_, token_, tokenvariablemap_, processinstance_, bytearrayvalue_, datevalue_, doublevalue_, longidclass_, longvalue_, stringidclass_, stringvalue_) \r\n            VALUES('S', 'tableName', NULL, @VV_TOKEN_ID, @VV_TOKEN_VARIABLE_MAP_ID, @VV_PROCESS_INSTANCE_ID, NULL, NULL, NULL, NULL, NULL, NULL, @VV_TABLE_NAME);\r\n    INSERT INTO jbpm_variableinstance(class_, name_, converter_, token_, tokenvariablemap_, processinstance_, bytearrayvalue_, datevalue_, doublevalue_, longidclass_, longvalue_, stringidclass_, stringvalue_) \r\n            VALUES('L', 'trackingId', NULL, @VV_TOKEN_ID, @VV_TOKEN_VARIABLE_MAP_ID, @VV_PROCESS_INSTANCE_ID, NULL, NULL, NULL, NULL, @PP_TRACKING_ID, NULL, NULL);\r\n    \r\n\tEXEC DBO.AEA_CORE_ADD_JBPM_LOG_ENTRY @VV_PROCESS_DEFINITION_ID, 'I', 0, @VV_TOKEN_ID, NULL, NULL, 0, NULL, NULL, NULL;\r\n    EXEC DBO.AEA_CORE_ADD_JBPM_LOG_ENTRY @VV_PROCESS_DEFINITION_ID, 'R', 1, @VV_TOKEN_ID, NULL, NULL, 0, 'archiveId', NULL, NULL;\r\n    EXEC DBO.AEA_CORE_ADD_JBPM_LOG_ENTRY @VV_PROCESS_DEFINITION_ID, 'G', 2, @VV_TOKEN_ID, NULL, NULL, 0, 'archiveId', @VV_ARCHIVE_ID, NULL;\r\n    EXEC DBO.AEA_CORE_ADD_JBPM_LOG_ENTRY @VV_PROCESS_DEFINITION_ID, 'R', 3, @VV_TOKEN_ID, NULL, NULL, 0, 'tableName', NULL, NULL;\r\n    EXEC DBO.AEA_CORE_ADD_JBPM_LOG_ENTRY @VV_PROCESS_DEFINITION_ID, 'U', 4, @VV_TOKEN_ID, NULL, NULL, 0, 'tableName', NULL, @VV_TABLE_NAME;\r\n    EXEC DBO.AEA_CORE_ADD_JBPM_LOG_ENTRY @VV_PROCESS_DEFINITION_ID, 'R', 5, @VV_TOKEN_ID, NULL, NULL, 0, 'trackingId', NULL, NULL;\r\n    EXEC DBO.AEA_CORE_ADD_JBPM_LOG_ENTRY @VV_PROCESS_DEFINITION_ID, 'G', 6, @VV_TOKEN_ID, NULL, NULL, 0, 'trackingId', @PP_TRACKING_ID, NULL;\r\n    EXEC DBO.AEA_CORE_ADD_JBPM_LOG_ENTRY @VV_PROCESS_DEFINITION_ID, 'S', 7, @VV_TOKEN_ID, NULL, @PP_TRANSITION_NAME, 0, NULL, NULL, NULL;\r\n    EXEC DBO.AEA_CORE_ADD_JBPM_LOG_ENTRY @VV_PROCESS_DEFINITION_ID, 'T', 8, @VV_TOKEN_ID, 7, @PP_TRANSITION_NAME, 1, NULL, NULL, NULL;\r\n    \r\n  END;\r\nEND;", "create or replace \r\nPROCEDURE\r\nAEA_CORE_ADD_JBPM (PP_OBJECT_BUSINESS_KEY IN VARCHAR2,\r\n        PP_TRACKING_ID IN NUMBER,\r\n        PP_TRANSITION_NAME IN VARCHAR2) AS\r\nBEGIN\r\nDECLARE\r\n  \r\n  VV_TRACKING_CONFIG_ID NUMBER;\r\n  VV_ARCHIVE_ID NUMBER;\r\n  \r\n  VV_TABLE_NAME VARCHAR2(50);\r\n  \r\n  VV_PROCESS_DEFINITION_ID NUMBER;\r\n  VV_T_MODULE_DEFINITION_ID NUMBER;\r\n  VV_INTERIM_NODE_ID NUMBER;\r\n  VV_INTERIM_NODE_NAME VARCHAR2(4000);\r\n  \r\n  VV_TOKEN_ID NUMBER;\r\n  VV_TOKEN_VARIABLE_MAP_ID NUMBER;\r\n  VV_PROCESS_INSTANCE_ID NUMBER;\r\n  \r\n  BEGIN\r\n    SELECT MAX (tracking_config_id) INTO VV_TRACKING_CONFIG_ID FROM etk_tracking_config_archive;\r\n    \r\n    SELECT archive_id INTO VV_ARCHIVE_ID FROM etk_tracking_config_archive WHERE tracking_config_id = VV_TRACKING_CONFIG_ID;\r\n    \r\n    SELECT table_name INTO VV_TABLE_NAME FROM etk_data_object WHERE business_key = PP_OBJECT_BUSINESS_KEY AND tracking_config_id = VV_TRACKING_CONFIG_ID;\r\n    \r\n    \r\n    SELECT processDefinition.id_, interimNode.id_, interimNode.name_, moduleDefinition.id_ INTO VV_PROCESS_DEFINITION_ID, VV_INTERIM_NODE_ID, VV_INTERIM_NODE_NAME, VV_T_MODULE_DEFINITION_ID FROM jbpm_processdefinition processDefinition JOIN jbpm_node startNode ON startNode.id_ = processDefinition.startstate_ JOIN jbpm_transition transition ON transition.from_ = startNode.id_ AND transition.name_ = PP_TRANSITION_NAME JOIN jbpm_node interimNode ON interimNode.id_ = transition.to_ JOIN jbpm_moduledefinition moduleDefinition ON moduleDefinition.processdefinition_ = processDefinition.id_ AND moduleDefinition.class_ = 'T' WHERE processDefinition.name_ = (SELECT dataObject.object_name FROM etk_data_object dataObject WHERE dataObject.business_key = PP_OBJECT_BUSINESS_KEY AND dataObject.tracking_config_id = VV_TRACKING_CONFIG_ID ) AND processDefinition.version_ = (SELECT MAX(processDefinitionSub.version_) FROM jbpm_processdefinition processDefinitionSub WHERE processDefinitionSub.name_ = (SELECT dataObjectSub.object_name FROM etk_data_object dataObjectSub WHERE dataObjectSub.business_key = PP_OBJECT_BUSINESS_KEY AND dataObjectSub.tracking_config_id = VV_TRACKING_CONFIG_ID ) );\r\n    \r\n    SELECT HIBERNATE_SEQUENCE.NEXTVAL INTO VV_PROCESS_INSTANCE_ID FROM DUAL;\r\n    SELECT HIBERNATE_SEQUENCE.NEXTVAL INTO VV_TOKEN_ID FROM DUAL;\r\n    SELECT HIBERNATE_SEQUENCE.NEXTVAL INTO VV_TOKEN_VARIABLE_MAP_ID FROM DUAL;\r\n    \r\n    EXECUTE IMMEDIATE 'UPDATE ' || VV_TABLE_NAME || ' SET id_workflow = :VV_PROCESS_INSTANCE_ID, id_archive = :VV_ARCHIVE_ID, state_label = :VV_INTERIM_STATE_LABEL WHERE id = :PP_TRACKING_ID'\r\n            USING VV_PROCESS_INSTANCE_ID, VV_ARCHIVE_ID, VV_INTERIM_NODE_NAME, PP_TRACKING_ID;\r\n    \r\n    INSERT INTO jbpm_processinstance(id_, start_, end_, processdefinition_, roottoken_, superprocesstoken_) \r\n            VALUES(VV_PROCESS_INSTANCE_ID, ETKF_GETSERVERTIME(), NULL, VV_PROCESS_DEFINITION_ID, NULL, NULL);\r\n    \r\n    INSERT INTO jbpm_token(id_, name_, start_, end_, nodeenter_, nextlogindex_, isabletoreactivateparent_, isterminationimplicit_, node_, processinstance_, parent_) \r\n          VALUES(VV_TOKEN_ID, NULL, ETKF_GETSERVERTIME(), NULL, ETKF_GETSERVERTIME(), 9, 1, 0, VV_INTERIM_NODE_ID, VV_PROCESS_INSTANCE_ID, NULL);\r\n    \r\n    UPDATE jbpm_processinstance SET roottoken_ = VV_TOKEN_ID WHERE id_ = VV_PROCESS_INSTANCE_ID;\r\n    \r\n    INSERT INTO jbpm_moduleinstance(id_, class_, processinstance_, TASKMGMTDEFINITION_, name_)\r\n            VALUES(HIBERNATE_SEQUENCE.NEXTVAL, 'C', VV_PROCESS_INSTANCE_ID, NULL, 'org.jbpm.context.exe.ContextInstance');\r\n    INSERT INTO jbpm_moduleinstance(id_, class_, processinstance_, TASKMGMTDEFINITION_, name_)\r\n            VALUES(HIBERNATE_SEQUENCE.NEXTVAL, 'T', VV_PROCESS_INSTANCE_ID, VV_T_MODULE_DEFINITION_ID, 'org.jbpm.taskmgmt.exe.TaskMgmtInstance');\r\n    \r\n    INSERT INTO jbpm_tokenvariablemap(id_, token_, contextinstance_) \r\n            VALUES(VV_TOKEN_VARIABLE_MAP_ID, VV_TOKEN_ID, (SELECT moduleInstance.id_ FROM jbpm_moduleinstance moduleInstance WHERE moduleInstance.class_ = 'C' AND moduleInstance.processinstance_ = VV_PROCESS_INSTANCE_ID));\r\n    \r\n    INSERT INTO jbpm_variableinstance(id_, class_, name_, converter_, token_, tokenvariablemap_, processinstance_, bytearrayvalue_, datevalue_, doublevalue_, longidclass_, longvalue_, stringidclass_, stringvalue_)\r\n            VALUES(HIBERNATE_SEQUENCE.NEXTVAL, 'L', 'archiveId', NULL, VV_TOKEN_ID, VV_TOKEN_VARIABLE_MAP_ID, VV_PROCESS_INSTANCE_ID, NULL, NULL, NULL, NULL, VV_ARCHIVE_ID, NULL, NULL);\r\n    INSERT INTO jbpm_variableinstance(id_, class_, name_, converter_, token_, tokenvariablemap_, processinstance_, bytearrayvalue_, datevalue_, doublevalue_, longidclass_, longvalue_, stringidclass_, stringvalue_) \r\n            VALUES(HIBERNATE_SEQUENCE.NEXTVAL, 'S', 'tableName', NULL, VV_TOKEN_ID, VV_TOKEN_VARIABLE_MAP_ID, VV_PROCESS_INSTANCE_ID, NULL, NULL, NULL, NULL, NULL, NULL, VV_TABLE_NAME);\r\n    INSERT INTO jbpm_variableinstance(id_, class_, name_, converter_, token_, tokenvariablemap_, processinstance_, bytearrayvalue_, datevalue_, doublevalue_, longidclass_, longvalue_, stringidclass_, stringvalue_) \r\n            VALUES(HIBERNATE_SEQUENCE.NEXTVAL, 'L', 'trackingId', NULL, VV_TOKEN_ID, VV_TOKEN_VARIABLE_MAP_ID, VV_PROCESS_INSTANCE_ID, NULL, NULL, NULL, NULL, PP_TRACKING_ID, NULL, NULL);\r\n    \r\n    AEA_CORE_ADD_JBPM_LOG_ENTRY(VV_PROCESS_DEFINITION_ID, 'I', 0, VV_TOKEN_ID, NULL, NULL, 0, NULL, NULL, NULL);\r\n    AEA_CORE_ADD_JBPM_LOG_ENTRY(VV_PROCESS_DEFINITION_ID, 'R', 1, VV_TOKEN_ID, NULL, NULL, 0, 'archiveId', NULL, NULL);\r\n    AEA_CORE_ADD_JBPM_LOG_ENTRY(VV_PROCESS_DEFINITION_ID, 'G', 2, VV_TOKEN_ID, NULL, NULL, 0, 'archiveId', VV_ARCHIVE_ID, NULL);\r\n    AEA_CORE_ADD_JBPM_LOG_ENTRY(VV_PROCESS_DEFINITION_ID, 'R', 3, VV_TOKEN_ID, NULL, NULL, 0, 'tableName', NULL, NULL);\r\n    AEA_CORE_ADD_JBPM_LOG_ENTRY(VV_PROCESS_DEFINITION_ID, 'U', 4, VV_TOKEN_ID, NULL, NULL, 0, 'tableName', NULL, VV_TABLE_NAME);\r\n    AEA_CORE_ADD_JBPM_LOG_ENTRY(VV_PROCESS_DEFINITION_ID, 'R', 5, VV_TOKEN_ID, NULL, NULL, 0, 'trackingId', NULL, NULL);\r\n    AEA_CORE_ADD_JBPM_LOG_ENTRY(VV_PROCESS_DEFINITION_ID, 'G', 6, VV_TOKEN_ID, NULL, NULL, 0, 'trackingId', PP_TRACKING_ID, NULL);\r\n    AEA_CORE_ADD_JBPM_LOG_ENTRY(VV_PROCESS_DEFINITION_ID, 'S', 7, VV_TOKEN_ID, NULL, PP_TRANSITION_NAME, 0, NULL, NULL, NULL);\r\n    AEA_CORE_ADD_JBPM_LOG_ENTRY(VV_PROCESS_DEFINITION_ID, 'T', 8, VV_TOKEN_ID, 7, PP_TRANSITION_NAME, 1, NULL, NULL, NULL);\r\n    \r\n  END;\r\nEND;");
    }

    /**
     * Ensure the AEA_UPDATE_FILE_REFERENCE_ID procedure exists.
     *
     * @param etk entellitrak execution context
     * @param deploymentResult deployment result
     */
    private static void createAeaUpdateFileReferenceId(final ExecutionContext etk, final DeploymentResult deploymentResult){
        /* Notes:
         *      - If performance becomes an issue, there are optimizations which should be able to be done. The easiest
         *        of which would be to restrict the etk_file update statement to only the matching OBJECT_TYPE.
         */
        createProcedure(etk,
                deploymentResult,
                "AEA_UPDATE_FILE_REFERENCE_ID", "ALTER PROCEDURE AEA_UPDATE_FILE_REFERENCE_ID AS\r\n  \r\n  DECLARE @updateQuery varchar(4000)\r\n  DECLARE fileCursor CURSOR FOR SELECT 'UPDATE etk_file\r\n                                        SET reference_id = ISNULL((SELECT id\r\n                                        FROM ' + dataObject.TABLE_NAME + '\r\n                                        WHERE ' + dataElement.COLUMN_NAME + ' = etk_file.id), reference_id)'\r\n                                FROM etk_data_object dataObject\r\n                                JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id\r\n                                WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive)\r\n                                      AND dataElement.data_type = 8\r\n                                ORDER BY dataObject.TABLE_NAME, dataElement.COLUMN_NAME\r\n  \r\n  OPEN fileCursor\r\n  FETCH fileCursor INTO @updateQuery\r\n  WHILE @@FETCH_STATUS = 0\r\n    BEGIN\r\n      BEGIN TRY\r\n        EXEC (@updateQuery)\r\n      END TRY\r\n      BEGIN CATCH\r\n        DECLARE @ErrorMessage NVARCHAR(max), @ErrorSeverity INT, @ErrorState INT;\r\n        SELECT @ErrorMessage = 'ETK_FILE data integrity error. '\r\n                               + CHAR(13) + CHAR(10)\r\n                               + 'ETK_FILE record associated with more than one tracked data record. '\r\n                               + CHAR(13) + CHAR(10)\r\n                               + 'You must fix your site''s data before continuing. ' \r\n                               + CHAR(13) + CHAR(10)\r\n                               + ERROR_MESSAGE() \r\n                               + ' ' \r\n                               + @updateQuery, \r\n               @ErrorSeverity = ERROR_SEVERITY(), \r\n               @ErrorState = ERROR_STATE();\r\n        RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);\r\n      END CATCH;\r\n      FETCH fileCursor INTO @updateQuery\r\n    END\r\n  CLOSE fileCursor\r\n  DEALLOCATE fileCursor", "CREATE OR REPLACE \r\nPROCEDURE AEA_UPDATE_FILE_REFERENCE_ID AS\r\nBEGIN\r\n  FOR fileElement IN (SELECT 'UPDATE etk_file\r\n                              SET reference_id = NVL((SELECT id\r\n                                                      FROM ' || dataObject.TABLE_NAME || '\r\n                                                      WHERE ' || dataElement.COLUMN_NAME || ' = etk_file.id), reference_id)' QUERYTEXT\r\n                      FROM etk_data_object dataObject\r\n                      JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id\r\n                      WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive)\r\n                            AND dataElement.data_type = 8\r\n                      ORDER BY dataObject.TABLE_NAME, dataElement.COLUMN_NAME)\r\n  LOOP\r\n    BEGIN\r\n      EXECUTE IMMEDIATE(fileElement.QUERYTEXT);\r\n      \r\n      EXCEPTION\r\n        WHEN OTHERS THEN\r\n          RAISE_APPLICATION_ERROR(-20000, 'ETK_FILE data integrity error. '\r\n                                          || CHR(13) || CHR(10)\r\n                                          || 'ETK_FILE record associated with more than one tracked data record. '\r\n                                          || CHR(13) || CHR(10)\r\n                                          || 'You must fix your site''s data before continuing. ' \r\n                                          || CHR(13) || CHR(10) \r\n                                          || SQLERRM \r\n                                          || ' ' \r\n                                          || fileElement.QUERYTEXT);\r\n    END;\r\n  END LOOP;\r\nEND;");
    }
}

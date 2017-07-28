package net.micropact.aea.du.page.packageMover;

import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.localization.Localizations;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.query.EscapeLike;
import net.micropact.aea.core.utility.WorkspaceService;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This page is the controller code for a page which is intended to rename an entellitrak package.
 * It not only updates the package, but looks in various places within entellitrak such as
 * Script Objects, View Instructions, Form Instructions and Page Business Keys to update these as well.
 * It does text searching and replacing as opposed to intelligent refactoring as you might find in a java IDE
 * so it is capable of making mistakes.
 *
 * @author zmiller
 */
public class PackageMoverController implements PageController {

    private static final int NO_BASE_PACKAGE = -1;

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {

            /* This page expects to receive a parameter requestedAction which when it is "update" will attempt to
             * move fromPackage to toPackage.
             * It will place any errors in an "errors" list.
             * It will set resultAction to "success", "error" or "initial" */

            final TextResponse response = etk.createTextResponse();

            final String requestedAction = etk.getParameters().getSingle("requestedAction");
            final String fromPackage = etk.getParameters().getSingle("fromPackage");
            final String toPackage = etk.getParameters().getSingle("toPackage");
            List<String> errors = Collections.emptyList();

            final String resultAction;

            if("update".equals(requestedAction)){
                /* We do very basic validation */
                errors = doPreliminaryValidation(fromPackage, toPackage);

                /* If validation passed, we will try to update the packages. */
                if(errors.size() == 0){
                    movePackage(etk, fromPackage, toPackage);

                    resultAction = "success";
                }else{

                    resultAction = "error";
                }

            }else{
                // The default is the first time we visit the form
                resultAction = "initial";
            }

            response.put("resultAction", JsonUtilities.encode(resultAction));
            response.put("fromPackage", JsonUtilities.encode(fromPackage));
            response.put("toPackage", JsonUtilities.encode(toPackage));
            response.put("errors", JsonUtilities.encode(errors));

            return response;

        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * This will do very simple validation to make sure that fromPackage and toPackage have sensical values.
     * Currently just checks that they are not blank.
     *
     * @param fromPackage The fully qualified name of the package to be moved from.
     *          ie: "com.destinationSite.accellerators"
     * @param toPackage The fully qualified name of the package to be moved to.
     *          ie: "gov.cia.caseManagement.accelerator"
     * @return A list of user-readable validation errors.
     */
    private static List<String> doPreliminaryValidation(final String fromPackage, final String toPackage){
        final List<String> errors = new LinkedList<>();

        if(StringUtility.isBlank(fromPackage)){
            errors.add("From Package cannot be blank.");
        }

        if(StringUtility.isBlank(toPackage)){
            errors.add("To Package cannot be blank.");
        }

        if(toPackage != null && toPackage.startsWith(fromPackage)){
            errors.add("To Package cannot be a subpackage of From Package (because of implementation details)");
        }

        return errors;
    }

    /**
     * Attempts to move a package from fromPackage to toPackage. It not only moves the package structure, but also
     * updates the contents of Page Business Keys, Script Objects, Form Instructions, View Instructions.
     *
     * @param etk entellitrak ExecutionContext
     * @param fromPackage The source package
     * @param toPackage The destination Package
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static void movePackage(final ExecutionContext etk, final String fromPackage, final String toPackage)
            throws IncorrectResultSizeDataAccessException{
        /* Note: For ease of implementation, a number of things such as the replacements are done in java instead of
         * in the database. */

        /* May want to add an extra "." to the end of fromPackage when doing replacements in code
         * so that something like "contact" does not accidentally rename one called "contactBase" */
        updatePageBusinessKeys(etk, fromPackage, toPackage);
        updateScriptObjectContents(etk, fromPackage, toPackage);
        updateFormInstructions(etk, fromPackage, toPackage);
        updateViewInstructions(etk, fromPackage, toPackage);
        updatePackage(etk, fromPackage, toPackage);

        /* We have to update the revision number and clear the cache any time we touch the system repository. */
        etk.createSQL("UPDATE etk_workspace SET workspace_revision = workspace_revision + 1 WHERE workspace_name = 'system'")
            .execute();

        WorkspaceService.clearSystemWorkspaceCache();
    }

    /**
     * Updates page business keys which start with fromPackage and replaces that segment with toPackage.
     *
     * @param etk entellitrak ExecutionContext
     * @param fromPackage Source Package
     * @param toPackage Destination Package
     */
    private static void updatePageBusinessKeys(final ExecutionContext etk,
            final String fromPackage,
            final String toPackage){
        for(final Map<String, Object> page : etk.createSQL(Utility.isSqlServer(etk) ? "SELECT PAGE_ID, BUSINESS_KEY FROM etk_page WHERE business_key LIKE :fromPackage + '%' ESCAPE :escapeChar"
                                                                                      : "SELECT PAGE_ID, BUSINESS_KEY FROM etk_page WHERE business_key LIKE :fromPackage || '%' ESCAPE :escapeChar")
                .setParameter("fromPackage", EscapeLike.escapeLike(etk, fromPackage))
                .setParameter("escapeChar", EscapeLike.getEscapeCharString())
                .fetchList()){

            etk.createSQL("UPDATE etk_page SET business_key = :newBusinessKey WHERE page_id = :pageId")
                .setParameter("pageId", page.get("PAGE_ID"))
                .setParameter("newBusinessKey", ((String) page.get("BUSINESS_KEY")).replace(fromPackage, toPackage))
                .execute();
        }
    }

    /**
     * Does a replacement on any Script Object which has fromPackage in its content so that it instead has toPackage.
     *
     * @param etk entellitrak {@link ExecutionContext}
     * @param fromPackage Source Package
     * @param toPackage Destination Package
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static void updateScriptObjectContents(final ExecutionContext etk,
            final String fromPackage,
            final String toPackage)
                    throws IncorrectResultSizeDataAccessException{
        /* Since the code of script objects can be large, we're only going to pull the ids */
        for(final Map<String, Object> scriptObjectIds : etk.createSQL(Utility.isSqlServer(etk) ? "SELECT scriptObject.SCRIPT_ID FROM aea_script_pkg_view_sys_only soView JOIN etk_script_object scriptObject ON scriptObject.script_id = soView.script_id WHERE scriptObject.code LIKE '%' + :fromPackage + '%' ESCAPE :escapeChar"
                                                                                              : "SELECT scriptObject.SCRIPT_ID FROM aea_script_pkg_view_sys_only soView JOIN etk_script_object scriptObject ON scriptObject.script_id = soView.script_id WHERE scriptObject.code LIKE '%' || :fromPackage || '%' ESCAPE :escapeChar")
                .setParameter("fromPackage", EscapeLike.escapeLike(etk, fromPackage))
                .setParameter("escapeChar", EscapeLike.getEscapeCharString())
                .fetchList()){
            final Object scriptId = scriptObjectIds.get("SCRIPT_ID");

            etk.createSQL("UPDATE etk_script_object SET code = :newCode WHERE script_id = :scriptId")
                .setParameter("scriptId", scriptId)
                .setParameter("newCode", etk.createSQL("SELECT code FROM etk_script_object WHERE script_id = :scriptId")
                        .setParameter("scriptId", scriptId)
                        .fetchString()
                        .replace(fromPackage, toPackage))
                .execute();
        }
    }

    /**
     * Updates the contents of any Form Instructions which refer to fromPackage so that they instead refer to toPackage.
     *
     * @param etk entellitrak {@link ExecutionContext}
     * @param fromPackage Source Package
     * @param toPackage Destination Package
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static void updateFormInstructions(final ExecutionContext etk,
            final String fromPackage,
            final String toPackage)
            throws IncorrectResultSizeDataAccessException{
        /* Since the instructions can be large, we're only going to pull the ids */
        for(final Map<String, Object> dataFormIds : etk.createSQL(Utility.isSqlServer(etk) ? "SELECT dataForm.data_form_id DATA_FORM_ID FROM etk_data_object dataObject JOIN etk_data_form dataForm ON dataForm.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = (SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = (SELECT MAX(config_version) FROM etk_tracking_config)) AND dataForm.instructions LIKE '%' + :fromPackage + '%' ESCAPE :escapeChar"
                    : "SELECT dataForm.data_form_id DATA_FORM_ID FROM etk_data_object dataObject JOIN etk_data_form dataForm ON dataForm.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = (SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = (SELECT MAX(config_version) FROM etk_tracking_config)) AND dataForm.instructions LIKE '%' || :fromPackage || '%' ESCAPE :escapeChar")
                .setParameter("fromPackage", EscapeLike.escapeLike(etk, fromPackage))
                .setParameter("escapeChar", EscapeLike.getEscapeCharString())
                .fetchList()){
            final Object dataFormId = dataFormIds.get("DATA_FORM_ID");

            etk.createSQL("UPDATE etk_data_form SET instructions = :newInstructions WHERE data_form_id = :dataFormId")
                .setParameter("dataFormId", dataFormId)
                .setParameter("newInstructions", etk.createSQL("SELECT INSTRUCTIONS FROM etk_data_form WHERE data_form_id = :dataFormId")
                        .setParameter("dataFormId", dataFormId)
                        .fetchString()
                        .replace(fromPackage, toPackage))
                .execute();
        }
    }

    /**
     * Updates the contents of any View Instructions that refer to fromPackage so that they instead refer to toPackage.
     *
     * @param etk entellitrak ExecutionContext
     * @param fromPackage Source Package
     * @param toPackage Destination Package
     * @throws IncorrectResultSizeDataAccessException
                 If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static void updateViewInstructions(final ExecutionContext etk,
            final String fromPackage,
            final String toPackage)
            throws IncorrectResultSizeDataAccessException{
        /* Since the instructions can be large, we're only going to pull the ids */
        for(final Map<String, Object> dataViewIds : etk.createSQL(Utility.isSqlServer(etk) ? "SELECT dataView.data_view_id DATA_VIEW_ID FROM etk_data_object dataObject JOIN etk_data_view dataView ON dataView.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = (SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = (SELECT MAX(config_version) FROM etk_tracking_config)) AND dataView.text LIKE '%' + :fromPackage + '%' ESCAPE :escapeChar"
                : "SELECT dataView.data_view_id DATA_VIEW_ID FROM etk_data_object dataObject JOIN etk_data_view dataView ON dataView.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = (SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = (SELECT MAX(config_version) FROM etk_tracking_config)) AND dataView.text LIKE '%' || :fromPackage || '%' ESCAPE :escapeChar")
                .setParameter("fromPackage", EscapeLike.escapeLike(etk, fromPackage))
                .setParameter("escapeChar", EscapeLike.getEscapeCharString())
                .fetchList()){
            final Object dataViewId = dataViewIds.get("DATA_VIEW_ID");

            etk.createSQL("UPDATE etk_data_view SET text = :newText WHERE data_view_id = :dataViewId")
                .setParameter("dataViewId", dataViewId)
                .setParameter("newText", etk.createSQL("SELECT TEXT FROM etk_data_view WHERE data_view_id = :dataViewId")
                        .setParameter("dataViewId", dataViewId)
                        .fetchString()
                        .replace(fromPackage, toPackage))
                .execute();
        }
    }

    /**
     * Returns a package at a specific depth of the tree.
     *
     * @param packageList Full package node list.
     * @param depth Depth of node.
     * @return Fully qualified package name for specific node depth.
     */
    private static String getSubpackage(final String[] packageList, final int depth) {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i <= depth; i++) {
            sb.append(packageList[i]);

            if (i != depth) {
                sb.append(".");
            }
        }

        return sb.toString();
    }

    /**
     * Creates a new package UUID if none exists.
     *
     * @param packageName The name of the package.
     * @return Package Name with package. prepended and UUID random appended to end.
     */
    private static String getPackageUUID (final String packageName) {
        return "package." + packageName + "." + UUID.randomUUID();
    }

    /**
     * Inserts a new package into the database.
     * @param etk entellitrak ExecutionContext
     * @param packageName Name of the package.
     * @param parentNodeId Parent node ID of the package.
     * @return New package ID.
     * @throws IncorrectResultSizeDataAccessException IncorrectResultSizeDataAccessException.
     */
    private static Number insertPackageRecord (final ExecutionContext etk,
            final String packageName,
            final Number parentNodeId)
            throws IncorrectResultSizeDataAccessException {
        Number packageId = null;

        if (Utility.isSqlServer(etk)) {
            packageId = etk.createSQL(
                    "insert into ETK_PACKAGE_NODE (BUSINESS_KEY, WORKSPACE_ID,"
                    + "TRACKING_CONFIG_ID, NAME, PARENT_NODE_ID, MERGE_NAME,"
                    + "PRE_MERGE_U_PARENT_FULL_NAME, PRE_MERGE_S_PARENT_FULL_NAME,"
                    + "CREATED_BY, CREATED_ON, LAST_UPDATED_BY, LAST_UPDATED_ON,"
                    + "CREATED_LOCALLY, MODIFIED_LOCALLY, DELETED_LOCALLY,"
                    + "DELETE_MERGE_REQUIRED, REVISION) values "
                    + "(:packageUUID,:workspaceId,"
                    + ":trackingConfigId,:packageName,:parentNodeId,null,null,null,:currentUserId,"
                    + ":currentDate,"
                    + "null,null,0,0,0,0,1)")
                .setParameter("packageUUID", getPackageUUID(packageName))
                .setParameter("workspaceId", Utility.getSystemRepositoryWorkspaceId(etk))
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                .setParameter("packageName", packageName)
                .setParameter("parentNodeId", parentNodeId)
                .setParameter("currentUserId", etk.getCurrentUser().getAccountName())
                .setParameter("currentDate", new Date((Localizations.getCurrentServerTimestamp() != null)
                        ? Localizations.getCurrentServerTimestamp().getDateValue().getTime()
                        : new java.util.Date().getTime()))
                .executeForKey("PACKAGE_NODE_ID");

        } else {
            packageId = (Number)
                    etk.createSQL("select hibernate_sequence.nextval from dual")
                    .fetchObject();

            etk.createSQL(
                    "insert into ETK_PACKAGE_NODE (PACKAGE_NODE_ID, BUSINESS_KEY, WORKSPACE_ID,"
                    + "TRACKING_CONFIG_ID, NAME, PARENT_NODE_ID, MERGE_NAME,"
                    + "PRE_MERGE_U_PARENT_FULL_NAME, PRE_MERGE_S_PARENT_FULL_NAME,"
                    + "CREATED_BY, CREATED_ON, LAST_UPDATED_BY, LAST_UPDATED_ON,"
                    + "CREATED_LOCALLY, MODIFIED_LOCALLY, DELETED_LOCALLY,"
                    + "DELETE_MERGE_REQUIRED, REVISION) values "
                    + "(:packageId,:packageUUID,:workspaceId,"
                    + ":trackingConfigId,:packageName,:parentNodeId,null,null,null,:currentUserId,"
                    + ":currentDate,"
                    + "null,null,0,0,0,0,1)")
                .setParameter("packageId", packageId)
                .setParameter("packageUUID", getPackageUUID(packageName))
                .setParameter("workspaceId", Utility.getSystemRepositoryWorkspaceId(etk))
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                .setParameter("packageName", packageName)
                .setParameter("parentNodeId", parentNodeId)
                .setParameter("currentUserId", etk.getCurrentUser().getAccountName())
                .setParameter("currentDate", new Date((Localizations.getCurrentServerTimestamp() != null)
                        ? Localizations.getCurrentServerTimestamp().getDateValue().getTime()
                        : new java.util.Date().getTime()))
                .execute();
        }
        etk.getLogger().error("Create package with ID=" + (packageId != null ? packageId.toString() : "NULL") +
                ", name=" + packageName +
                ", parentNodeId=" + (parentNodeId != null ? parentNodeId.toString() : "NULL") );

        return packageId;
    }

    /**
     * Attempts to find a package and return its ID. If none exists, a package is inserted into the database and the
     * new package ID is returned.
     * @param etk entellitrak ExecutionContext
     * @param packagePath Path of the package.
     * @return New or existing package ID.
     * @throws IncorrectResultSizeDataAccessException IncorrectResultSizeDataAccessException.
     */
    private static Number findOrCreatePackage(final ExecutionContext etk, final String packagePath)
            throws IncorrectResultSizeDataAccessException {

        //Root path
        if (packagePath == null) {
            return null;
        }

        final Map<String, Number> packageMap = getPackageNodes(etk);

        if (packageMap.containsKey(packagePath)) {
            return packageMap.get(packagePath);
        } else {
            etk.getLogger().error("PACKAGE PATH: " + packagePath);
            final String[] packageList = packagePath.split("\\.");
            final int packageTreeDepth = packageList.length;

            String subPackage = "";
            int firstFoundNode = NO_BASE_PACKAGE;

            for (int i = packageTreeDepth - 1; i >= 0; i--) {
                subPackage = getSubpackage(packageList, i);
                etk.getLogger().error("subPackage: " + subPackage);

                if (packageMap.containsKey(subPackage)) {
                    firstFoundNode = i;
                    etk.getLogger().error("firstFoundNode: " + firstFoundNode);
                    break;
                } else {
                    etk.getLogger().error("No match, continuing");
                }
            }


            Number lastPackageId = null;

            if (firstFoundNode == NO_BASE_PACKAGE) {
                //Root does not exist, so we will create root.
                etk.getLogger().error("NO_BASE_PACKAGE");
                lastPackageId = insertPackageRecord (etk, packageList[0], null);
                etk.getLogger().error("lastPackageId=" + lastPackageId);
                firstFoundNode = 0;
            } else {
                //A root with the name does exist - find it and get its ID.
                etk.getLogger().error("ROOT EXISTS");
                lastPackageId = packageMap.get(getSubpackage(packageList, firstFoundNode));
                etk.getLogger().error("lastPackageId=" + lastPackageId);
            }


            //Move up the package tree from the base, adding nodes and setting the
            //parent ID to the last package ID.
            for (int i = firstFoundNode + 1; i < packageTreeDepth; i++) {
                lastPackageId = insertPackageRecord (etk, packageList[i], lastPackageId);
            }


            return lastPackageId;
        }
    }

    /**
     * Gets a Map of all packages in the system repository.
     *
     * @param etk entellitrak ExecutionContext
     * @return A Map of package nodes where the key is the fully qualified package name,
     *          and the value is the package node id.
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static Map<String, Number> getPackageNodes(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException{
        final List<Map<String, Object>> etkPackageList =
                etk.createSQL(
                    Utility.isSqlServer(etk)
                            ? "WITH etk_packages (package_node_id, path) AS " +
                            "( " +
                            "  SELECT obj.package_node_id, cast(obj.name as varchar(4000)) as path " +
                            "  FROM etk_package_node obj " +
                            "  WHERE obj.workspace_id = :workspaceId " +
                            "  AND obj.tracking_config_id = :trackingConfigId " +
                            "  AND obj.parent_node_id is null " +
                            "  UNION ALL " +
                            "  SELECT nplus1.package_node_id, "
                            + "cast(etk_packages.path + '.' + nplus1.name as varchar(4000)) as path " +
                            "  FROM etk_package_node nplus1, etk_packages  " +
                            "  WHERE nplus1.parent_node_id= etk_packages.package_node_id " +
                            ") " +
                            "select package_node_id as PACKAGE_NODE_ID, path as PATH from etk_packages "
                            : "WITH etk_packages (package_node_id, path) AS " +
                            "( " +
                            "  SELECT obj.package_node_id, obj.name as path " +
                            "  FROM etk_package_node obj " +
                            "  WHERE obj.workspace_id = :workspaceId " +
                            "  AND obj.tracking_config_id = :trackingConfigId " +
                            "  AND obj.parent_node_id is null " +
                            "  UNION ALL " +
                            "  SELECT nplus1.package_node_id, etk_packages.path || '.' || nplus1.name as path " +
                            "  FROM etk_package_node nplus1, etk_packages  " +
                            "  WHERE nplus1.parent_node_id= etk_packages.package_node_id " +
                            ") " +
                            "select package_node_id as PACKAGE_NODE_ID, path as PATH from etk_packages "
                     )
                    .setParameter("workspaceId", Utility.getSystemRepositoryWorkspaceId(etk))
                    .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                    .fetchList();

        final Map <String, Number> packageMap = new HashMap<>();

        for(final Map<String, Object> etkPackage : etkPackageList) {
                packageMap.put((String) etkPackage.get("PATH"),
                               (Number) etkPackage.get("PACKAGE_NODE_ID"));
        }

        return packageMap;
    }

    /**
     * Moves the package structure of fromPackage to toPackage.
     * If toPackage already exists, fromPackage's children will be reparented and fromPackage will be deleted.
     * If toPackage does not exist, fromPackage will be moved to newPackage and renamed
     * (making any necessary parent packages)
     *
     * @param etk entellitrak ExecutionContext
     * @param fromPackage Source Package
     * @param toPackage Destination Package
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static void updatePackage(final ExecutionContext etk, final String fromPackage, final String toPackage)
            throws IncorrectResultSizeDataAccessException{

        Map<String, Number> packageMap = getPackageNodes(etk);

        final Number fromPackageNodeId = packageMap.get(fromPackage);

        if(fromPackageNodeId == null){
            /* The from package does not exist.
             * This is not necessarily an error even though it very likely is. */

            etk.getLogger().error(
                    String.format("PackageMoverController.updatePackage is attempting to move package \"%s\" but this package does not exist. This is not necessarily indicative of an error, but it is highly likely to be."
                            , fromPackage));
            return;
        }else{
            if(packageMap.containsKey(toPackage)) {

                /* The destination package already exists */

                final Number toPackageNodeId = packageMap.get(toPackage);

                // Re-point all the existing script objects from the old package to the new one.
                etk.createSQL("UPDATE etk_script_object SET package_node_id = :toPackageNodeId WHERE package_node_id = :fromPackageNodeId")
                    .setParameter("toPackageNodeId", toPackageNodeId)
                    .setParameter("fromPackageNodeId", fromPackageNodeId)
                    .execute();

                // Re-point all the existing packages from the old package to the new one.
                etk.createSQL("UPDATE etk_package_node SET parent_node_id = :toPackageNodeId WHERE parent_node_id = :fromPackageNodeId")
                    .setParameter("toPackageNodeId", toPackageNodeId)
                    .setParameter("fromPackageNodeId", fromPackageNodeId)
                    .execute();

                /* Delete the old package.
                 * This is only safe because we are not allowing toPackage to be a subpackage of fromPackage. */
                etk.createSQL("DELETE FROM etk_package_node WHERE package_node_id = :packageNodeId")
                    .setParameter("packageNodeId", fromPackageNodeId)
                    .execute();
            }else{
                /* The destination package does not already exist.
                 * We need to ensure that everything up to put not including the destination path exists.
                 * We will then re-parent the from package */

                final String toParentPackageName = toPackage.substring(0, toPackage.lastIndexOf('.'));

                findOrCreatePackage(etk, toParentPackageName);

                packageMap = getPackageNodes(etk);

                etk.createSQL("UPDATE etk_package_node SET parent_node_id = :toParentNodeId, name = :nameSegment WHERE package_node_id = :fromPackageNodeId")
                    .setParameter("toParentNodeId", packageMap.get(toParentPackageName))
                    .setParameter("nameSegment", toPackage.substring(toPackage.lastIndexOf('.') + 1))
                    .setParameter("fromPackageNodeId", fromPackageNodeId)
                    .execute();
            }
        }
    }
}

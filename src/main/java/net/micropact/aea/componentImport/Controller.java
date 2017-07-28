package net.micropact.aea.componentImport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.SQLFacade;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.localization.Localizations;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.FileStream;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;
import com.micropact.entellitrak.system.Services;

/**
 * Import tool to use export xml made by componentExport.
 *
 * <strong>It is important that this class not depend on any other Script Objects within the system.</strong>
 *
 * @author aclee
 *
 */
public class Controller implements PageController {

    private static final int NO_BASE_PACKAGE = -1;
    private static final int ZIP_BUFFER_SIZE_BYTES = 4096;
    private Number nextTrackingConfigId = null;
    private Number workspaceId = null;
    private boolean cleanDataForms = true;
    private Map <String, Map<String, String>> cachedPackages = null;
    private final Map <String, Number> oldNewScriptIdMap = new HashMap<>();
    private final Map<String, Number> pageKeyMap = new HashMap<>();
    private PageExecutionContext etk = null;
    private final List<String> errorsAndWarnings = new ArrayList<>();
    private final Map<String, String> fileMap = new HashMap<>();
    private List<Map<String, Object>> aeaScriptViewSysOnly = null;
    private boolean readPackages = true;
    private final Map <String, Number> packageMap = new HashMap<>();
    private final Map <Number, Number> packageTypeMap = new HashMap<>();

    @Override
    public Response execute(final PageExecutionContext theContext) throws ApplicationException {

        etk = theContext;

        final TextResponse tr = etk.createTextResponse();
        tr.setContentType(ContentType.HTML);
        tr.put("form", "importFormView");

        createScriptPackageView();
        createScriptPackageViewSysOnly();
        refreshAeaScriptViewSysOnly();

        if (isSqlServer()) {
            try {
                final Integer tableCount =
                        etk.createSQL("select count(*) from sys.tables where name = 'T_AEA_COMPONENT_VERSION_INFO'").fetchInt();

                if (0 == tableCount) {
                    etk.createSQL(
                            "CREATE TABLE [T_AEA_COMPONENT_VERSION_INFO] " +
                                    " ( [ID] [numeric](19, 0) IDENTITY(1,1) NOT NULL, " +
                                    "   [C_CODE] [VARCHAR](255) NULL, " +
                                    "   [C_CURRENT_VERSION] [INT] NULL, " +
                                    "   [C_DATE_INSTALLED] [DATETIME] NULL, " +
                                    "   [C_NAME] [VARCHAR](255) NULL, " +
                                    "   [C_ORDER] [INT] NULL, " +
                                    "   [ETK_START_DATE] [DATETIME] NULL, " +
                                    "   [ETK_END_DATE] [DATETIME] NULL, " +
                                    "   PRIMARY KEY CLUSTERED " +
                                    "   ( " +
                                    "      [ID] ASC " +
                                    "   ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY] " +
                            ") ON [PRIMARY]").execute();
                }
            } catch (final Exception e) {
                throw new ApplicationException("Could not create table T_AEA_COMPONENT_VERSION_INFO, quitting.", e);
            }
        } else {
            try {
                final Integer tableCount =
                        etk.createSQL("select count(*) from user_tables where table_name = 'T_AEA_COMPONENT_VERSION_INFO'").fetchInt();

                if (0 == tableCount) {
                    etk.createSQL(
                            "CREATE TABLE \"T_AEA_COMPONENT_VERSION_INFO\" " +
                                    " ( \"ID\" NUMBER(19,0), " +
                                    " \"C_CODE\" VARCHAR2(255 BYTE), " +
                                    " \"C_CURRENT_VERSION\" NUMBER(10,0), " +
                                    " \"C_DATE_INSTALLED\" DATE, " +
                                    " \"C_NAME\" VARCHAR2(255 BYTE), " +
                                    " \"C_ORDER\" NUMBER(10,0), " +
                                    " \"ETK_END_DATE\" DATE, " +
                            " \"ETK_START_DATE\" DATE )").execute();


                    etk.createSQL("CREATE UNIQUE INDEX COMP_VERSION_ID ON \"T_AEA_COMPONENT_VERSION_INFO\" (\"ID\")").execute();
                    etk.createSQL("ALTER TABLE \"T_AEA_COMPONENT_VERSION_INFO\" ADD PRIMARY KEY (\"ID\") ENABLE").execute();
                    etk.createSQL("ALTER TABLE \"T_AEA_COMPONENT_VERSION_INFO\" MODIFY (\"ID\" NOT NULL ENABLE)").execute();
                }
            } catch (final Exception e) {
                throw new ApplicationException("Could not create table T_AEA_COMPONENT_VERSION_INFO, quitting.", e);
            }
        }

        try {

            nextTrackingConfigId = (Number) etk.createSQL(
                    "SELECT MAX(trackingConfig.tracking_config_id) TRACKINGCONFIGID "
                            + "FROM etk_tracking_config trackingConfig "
                            + "WHERE trackingConfig.config_version = "
                            + "(SELECT MAX(innerTrackingConfig.config_version) "
                            + "FROM etk_tracking_config innerTrackingConfig)")
                    .fetchObject();

            workspaceId = (Number) etk.createSQL("select workspace_id from etk_workspace where workspace_name = 'system' and user_id is null")
                    .fetchObject();


            cleanDataForms = StringUtility.isNotBlank(etk.getParameters().getSingle("cleanDataForms"));

        } catch (final Exception e) {
            throw new ApplicationException(e);
        }

        final FileStream fileParameter = etk.getParameters().getFile("importFile");

        if (fileParameter != null) {

            final String fileName = fileParameter.getFileName();
            final StringBuilder message = new StringBuilder();
            message.append("Import Result: <br><br>");

            if (fileName != null) {
                try(InputStream inputStream = fileParameter.getInputStream()) {

                    if (inputStream != null) {
                        performImport(inputStream);
                    }

                    for (final String anErrorOrWarning : errorsAndWarnings) {
                        message.append(anErrorOrWarning);
                        message.append("<br>");
                    }

                    message.append("File ");
                    message.append(fileName);
                    message.append(" imported successfully.");
                } catch (final FileNotFoundException e) {
                    try {
                        message.append("Error loading file ");
                        message.append(fileName);
                        message.append(", v3.15.0.0.0 bug? Trying to load ");
                        message.append("c_file from R_TEMP_FILE_UPLOAD with c_code = 'xmlImport'.");

                        final Map<String, Object> tempFileMap =
                                etk.createSQL("select CONTENT, FILE_NAME from etk_file where id = " +
                                        "(select c_file from R_TEMP_FILE_UPLOAD where c_code = 'xmlImport')")
                                .returnEmptyResultSetAs(null)
                                .fetchMap();

                        if (tempFileMap != null) {

                            InputStream fs = null;

                            try{
                                fs = new ByteArrayInputStream((byte[]) tempFileMap.get("CONTENT"));
                                performImport(fs);
                                message.append("<br>File ");
                                message.append(tempFileMap.get("FILE_NAME"));
                                message.append(" found, imported successfully.");
                            } finally{
                                IOUtils.closeQuietly(fs);
                            }
                        } else {
                            message.append("<br>No file found, no import performed.");
                        }

                    } catch (final Exception ex) {
                        throw new ApplicationException(ex);
                    }
                } catch (final Exception e) {
                    throw new ApplicationException ("Error importing data.", e);
                }
            }

            //Rebuild the system index....
            Services.getWorkspaceService().publishWorkspaceChanged(true);
            Services.getWorkspaceIndexService().invalidateIndex(Services.getWorkspaceService().getSystemWorkspace());

            tr.put("responseMessage", message.toString());
        }

        etk.getDataCacheService().clearDataCaches();
        etk.getCache().clearCache();

        return tr;
    }

    /**
     * This method updates the instance variable holding the aea_script_pkg_view_sys_only data.
     */
    private void refreshAeaScriptViewSysOnly() {
        aeaScriptViewSysOnly = etk.createSQL("SELECT * FROM AEA_SCRIPT_PKG_VIEW_SYS_ONLY").fetchList();
    }

    /**
     * Is Sql Server.
     *
     * @return is the DB sqlServer?
     */
    private boolean isSqlServer(){
        return com.entellitrak.platform.DatabasePlatform.SQL_SERVER.equals(etk.getPlatformInfo().getDatabasePlatform());
    }

    /**
     * Private helper message to write output to the screen.
     *
     * @param aMessage String to write to the screen on execute.
     */
    private void addMsg(final String aMessage) {
        etk.getLogger().error(aMessage);
    }

    /**
     * Creates AEA_SCRIPT_PKG_VIEW View for both Oracle and SQLServer. This view simplifies
     * retrieval of the fully qualified package associated with a script object.
     */
    private void createScriptPackageView() {
        try {

            if (isSqlServer()) {
                try {
                    etk.createSQL("DROP VIEW AEA_SCRIPT_PKG_VIEW").execute();
                    //addMsg ("Dropped existing AEA_SCRIPT_PKG_VIEW.");
                } catch (final Exception e) {
                    addMsg ("Warning: Could not drop existing AEA_SCRIPT_PKG_VIEW, ignore if first time import.");
                }

                etk.createSQL(
                        " CREATE VIEW AEA_SCRIPT_PKG_VIEW " +
                                " (SCRIPT_ID, SCRIPT_NAME, SCRIPT_LANGUAGE_TYPE, SCRIPT_BUSINESS_KEY, WORKSPACE_ID,  " +
                                " TRACKING_CONFIG_ID, PACKAGE_PATH, PACKAGE_NODE_ID, PACKAGE_TYPE, FULLY_QUALIFIED_SCRIPT_NAME, " +
                                " SCRIPT_HANDLER_TYPE) AS " +

        			" WITH etk_packages (package_node_id, workspace_id, tracking_config_id, path, package_type) AS  " +
        			"   (   " +
        			"     SELECT obj.package_node_id,  " +
        			"					         obj.workspace_id,  " +
        			"							 obj.tracking_config_id,  " +
        			"							 cast(obj.name as varchar(4000)) as path,   " +
        			"							 obj.package_type " +
        			"     FROM etk_package_node obj   " +
        			"     WHERE obj.parent_node_id is null " +
        			"     UNION ALL   " +
        			"     SELECT nplus1.package_node_id, " +
        			"           nplus1.workspace_id,  " +
        			"           nplus1.tracking_config_id, " +
        			"           cast(etk_packages.path + '.' + nplus1.name as varchar(4000)) as path,   " +
        			"			nplus1.package_type " +
        			"     FROM etk_package_node nplus1, etk_packages    " +
        			"     WHERE nplus1.parent_node_id= etk_packages.package_node_id   " +
        			"   )  					 " +
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
        			"	from etk_script_object so  " +
                        "   left join etk_packages ep on so.package_node_id = ep.package_node_id ")
                .execute();
            } else {
                etk.createSQL(
                        "	CREATE OR REPLACE VIEW AEA_SCRIPT_PKG_VIEW  " +
                                "	(SCRIPT_ID, SCRIPT_NAME, SCRIPT_LANGUAGE_TYPE, SCRIPT_BUSINESS_KEY, WORKSPACE_ID,  " +
                                "		TRACKING_CONFIG_ID, PACKAGE_PATH, PACKAGE_NODE_ID, PACKAGE_TYPE, FULLY_QUALIFIED_SCRIPT_NAME, " +
                                "       SCRIPT_HANDLER_TYPE) AS " +
                                "	WITH etk_packages (package_node_id, workspace_id, tracking_config_id, path, package_type) AS " +
                                "	  (SELECT obj.package_node_id, " +
                                "	    obj.workspace_id, " +
                                "	    obj.tracking_config_id, " +
                                "	    cast(obj.name as varchar(4000)) as path, " +
                                "		obj.package_type " +
                                "	  FROM etk_package_node obj " +
                                "	  WHERE obj.parent_node_id IS NULL " +
                                "	  UNION ALL " +
                                "	  SELECT nplus1.package_node_id, " +
                                "	    nplus1.workspace_id, " +
                                "	    nplus1.tracking_config_id, " +
                                "       cast(etk_packages.path || '.' || nplus1.name as varchar(4000)) as path, " +
                                "		nplus1.package_type " +
                                "	  FROM etk_package_node nplus1, " +
                                "	    etk_packages " +
                                "	  WHERE nplus1.parent_node_id= etk_packages.package_node_id " +
                                "	  ) " +
                                "	SELECT so.SCRIPT_ID     AS SCRIPT_ID, " +
                                "	  so.NAME               AS SCRIPT_NAME, " +
                                "	  so.LANGUAGE_TYPE      AS SCRIPT_LANGUAGE_TYPE, " +
                                "	  so.BUSINESS_KEY       AS SCRIPT_BUSINESS_KEY, " +
                                "	  so.WORKSPACE_ID       AS WORKSPACE_ID, " +
                                "	  so.TRACKING_CONFIG_ID AS TRACKING_CONFIG_ID, " +
                                "	  ep.path               AS PACKAGE_PATH, " +
                                "	  ep.package_node_id    AS PACKAGE_NODE_ID, " +
                                "         ep.package_type       AS PACKAGE_TYPE, " +
                                "	  CASE " +
                                "	    WHEN so.package_node_id IS NULL " +
                                "	    THEN so.NAME " +
                                "	    ELSE ep.path " +
                                "	      || '.' " +
                                "	      || so.NAME " +
                                "	  END AS FULLY_QUALIFIED_SCRIPT_NAME, " +
                                "   so.HANDLER_TYPE as SCRIPT_HANDLER_TYPE " +
                                "	FROM etk_script_object so " +
                                "	LEFT JOIN etk_packages ep " +
                        "	ON so.package_node_id = ep.package_node_id ")
                .execute();
            }

            //addMsg ("Successfully created view AEA_SCRIPT_PKG_VIEW");

        } catch (final Exception e) {
            addMsg ("Error creating AEA_SCRIPT_PKG_VIEW.");
            addMsg (ExceptionUtils.getFullStackTrace(e));
        }
    }

    /**
     * Creates AEA_SCRIPT_PKG_VIEW View for both Oracle and SQLServer. This view simplifies
     * retrieval of the fully qualified package associated with a script object.
     *
     * Filtered by SYSTEM workspace only. Useful because you don't need to filter this one by
     * workspace ID and tracking config ID.
     */
    private void createScriptPackageViewSysOnly() {
        try {
            try {
                etk.createSQL("DROP VIEW AEA_SCRIPT_PKG_VIEW_SYS_ONLY").execute();
                //addMsg ("Dropped existing AEA_SCRIPT_PKG_VIEW_SYS_ONLY.");
            } catch (final Exception e) {
                addMsg ("Warning: Could not drop existing AEA_SCRIPT_PKG_VIEW_SYS_ONLY, ignore if first time import.");
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

            //addMsg ("Successfully created view AEA_SCRIPT_PKG_VIEW_SYS_ONLY");
        } catch (final Exception e) {
            addMsg ("Error creating AEA_SCRIPT_PKG_VIEW_SYS_ONLY.");
            addMsg (ExceptionUtils.getFullStackTrace(e));
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
     * Creates a new package UUID if none exists.
     *
     * @param scriptName The fully qualified script object name
     * @return Package Name with package. prepended and UUID random appended to end.
     */
    private static String getScriptUUID (final String scriptName) {
        return "script." + scriptName.toLowerCase() + "." + UUID.randomUUID();
    }


    /**
     * Inserts a new package into the database.
     * @param packageName Name of the package.
     * @param fullPackagePath Fully qualified package path.
     * @param parentNodeId Parent node ID of the package.
     * @param packageType Standard or Data Type Plug-in package type
     * @return New package ID.
     * @throws IncorrectResultSizeDataAccessException IncorrectResultSizeDataAccessException.
     */
    private Number insertPackageRecord (final String packageName, final String fullPackagePath, final Number parentNodeId, final String packageType)
            throws IncorrectResultSizeDataAccessException {
        Number packageId = null;

        if (isSqlServer()) {
            packageId = etk.createSQL(
                    "insert into ETK_PACKAGE_NODE (BUSINESS_KEY, WORKSPACE_ID,"
                            + "TRACKING_CONFIG_ID, NAME, PARENT_NODE_ID, MERGE_NAME,"
                            + "PRE_MERGE_U_PARENT_FULL_NAME, PRE_MERGE_S_PARENT_FULL_NAME,"
                            + "CREATED_BY, CREATED_ON, LAST_UPDATED_BY, LAST_UPDATED_ON,"
                            + "CREATED_LOCALLY, MODIFIED_LOCALLY, DELETED_LOCALLY,"
                            + "DELETE_MERGE_REQUIRED, REVISION, PACKAGE_TYPE) values "
                            + "(:packageUUID,:workspaceId,"
                            + ":trackingConfigId,:packageName,:parentNodeId,null,null,null,:currentUserId,"
                            + ":currentDate,"
                            + "null,null,0,0,0,0,1,:packageType)")
                    .setParameter("packageUUID", getPackageUUID(packageName))
                    .setParameter("workspaceId", workspaceId)
                    .setParameter("trackingConfigId", nextTrackingConfigId)
                    .setParameter("packageName", packageName)
                    .setParameter("parentNodeId", parentNodeId)
                    .setParameter("currentUserId", etk.getCurrentUser().getAccountName())
                    .setParameter("currentDate", new java.sql.Date((Localizations.getCurrentServerTimestamp() != null)
                            ? Localizations.getCurrentServerTimestamp().getDateValue().getTime()
                            : new java.util.Date().getTime()))
                    .setParameter("packageType", StringUtility.isNotBlank(packageType) ? packageType : "1")
                    .executeForKey("PACKAGE_NODE_ID");

        } else {
            packageId = getNextOracleHibernateSequence();

            etk.createSQL(
                    "insert into ETK_PACKAGE_NODE (PACKAGE_NODE_ID, BUSINESS_KEY, WORKSPACE_ID,"
                            + "TRACKING_CONFIG_ID, NAME, PARENT_NODE_ID, MERGE_NAME,"
                            + "PRE_MERGE_U_PARENT_FULL_NAME, PRE_MERGE_S_PARENT_FULL_NAME,"
                            + "CREATED_BY, CREATED_ON, LAST_UPDATED_BY, LAST_UPDATED_ON,"
                            + "CREATED_LOCALLY, MODIFIED_LOCALLY, DELETED_LOCALLY,"
                            + "DELETE_MERGE_REQUIRED, REVISION, PACKAGE_TYPE) values "
                            + "(:packageId,:packageUUID,:workspaceId,"
                            + ":trackingConfigId,:packageName,:parentNodeId,null,null,null,:currentUserId,"
                            + ":currentDate,"
                            + "null,null,0,0,0,0,1,:packageType)")
            .setParameter("packageId", packageId)
            .setParameter("packageUUID", getPackageUUID(packageName))
            .setParameter("workspaceId", workspaceId)
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .setParameter("packageName", packageName)
            .setParameter("parentNodeId", parentNodeId)
            .setParameter("currentUserId", etk.getCurrentUser().getAccountName())
            .setParameter("currentDate", new java.sql.Date((Localizations.getCurrentServerTimestamp() != null)
                    ? Localizations.getCurrentServerTimestamp().getDateValue().getTime()
                    : new java.util.Date().getTime()))
            .setParameter("packageType", StringUtility.isNotBlank(packageType) ? packageType : "1")
            .execute();
        }
        etk.getLogger().error("Create package with ID=" + (packageId != null ? packageId.toString() : "NULL") +
                ", name=" + packageName +
                ", parentNodeId=" + (parentNodeId != null ? parentNodeId.toString() : "NULL") );

        packageMap.put(fullPackagePath, packageId);
        packageTypeMap.put(packageId, new BigDecimal(packageType));

        return packageId;
    }

    /**
     * Attempts to find a package and return its ID. If none exists, a package is inserted into the database and the
     * new package ID is returned.
     * @param packagePath Path of the package.
     * @param packageType Standard or Data Type Plug-in package type
     * @return New or existing package ID.
     * @throws IncorrectResultSizeDataAccessException IncorrectResultSizeDataAccessException.
     */
    private Number findOrCreatePackage(final String packagePath, final String packageType)
            throws IncorrectResultSizeDataAccessException {

        //Root path
        if (packagePath == null) {
            return null;
        }

        //Return the destination system't current etk package structure.
        if (readPackages) {
            final List<Map<String, Object>> etkPackageList =
                    etk.createSQL(
                            isSqlServer() ?
                                           "WITH etk_packages (package_node_id, path, package_type) AS " +
                                           "( " +
                                           "  SELECT obj.package_node_id, cast(obj.name as varchar(4000)) as path, obj.package_type " +
                                           "  FROM etk_package_node obj " +
                                           "  WHERE obj.workspace_id = :workspaceId " +
                                           "  AND obj.tracking_config_id = :trackingConfigId " +
                                           "  AND obj.parent_node_id is null " +
                                           "  UNION ALL " +
                                           "  SELECT nplus1.package_node_id, " +
                                           "  cast(etk_packages.path + '.' + nplus1.name as varchar(4000)) as path, " +
                                           "  nplus1.package_type " +
                                           "  FROM etk_package_node nplus1, etk_packages  " +
                                           "  WHERE nplus1.parent_node_id= etk_packages.package_node_id " +
                                           ") " +
                                           "select package_node_id as PACKAGE_NODE_ID, path as PATH, package_type as PACKAGE_TYPE from etk_packages "

	                    		:

	                    		    "WITH etk_packages (package_node_id, path, package_type) AS " +
	                    		    "( " +
	                    		    "  SELECT obj.package_node_id, obj.name as path, obj.package_type " +
	                    		    "  FROM etk_package_node obj " +
	                    		    "  WHERE obj.workspace_id = :workspaceId " +
	                    		    "  AND obj.tracking_config_id = :trackingConfigId " +
	                    		    "  AND obj.parent_node_id is null " +
	                    		    "  UNION ALL " +
	                    		    "  SELECT nplus1.package_node_id, etk_packages.path || '.' || nplus1.name as path, " +
	                    		    "  nplus1.package_type " +
	                    		    "  FROM etk_package_node nplus1, etk_packages  " +
	                    		    "  WHERE nplus1.parent_node_id= etk_packages.package_node_id " +
	                    		    ") " +
	                    		    "select package_node_id as PACKAGE_NODE_ID, path as PATH, package_type as PACKAGE_TYPE from etk_packages "
                            )
                    .setParameter("workspaceId", workspaceId)
                    .setParameter("trackingConfigId", nextTrackingConfigId)
                    .fetchList();

            //Use 2 maps instead of a private class....
            for (final Map<String, Object> etkPackage : etkPackageList) {
                packageMap.put((String) etkPackage.get("PATH"),
                        (Number) etkPackage.get("PACKAGE_NODE_ID"));

                packageTypeMap.put((Number) etkPackage.get("PACKAGE_NODE_ID"),
                        (Number) etkPackage.get("PACKAGE_TYPE"));
            }

            readPackages = false;
        }


        if (packageMap.containsKey(packagePath)) {
            final Number localNodeId = packageMap.get(packagePath);
            final Number localPackageType = packageTypeMap.get(localNodeId);
            final Number importedPackageType = new BigDecimal(packageType);

            if (!localPackageType.equals(importedPackageType)) {
                etk.createSQL("update etk_package_node "
                        + " set package_type = :packageType where package_node_id = :packageNodeId")
                .setParameter("packageType", importedPackageType)
                .setParameter("packageNodeId", localNodeId)
                .execute();

                //Update package type in cached list.
                packageTypeMap.put(localNodeId, importedPackageType);
            }

            return localNodeId;

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
                lastPackageId = insertPackageRecord (packageList[0], packageList[0], null, packagePath.equals(packageList[0]) ? packageType : "1");
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
            String fullPath = "";
            for (int i = firstFoundNode + 1; i < packageTreeDepth; i++) {
                fullPath = "";

                for (int j = 0; j <= i; j++) {
                    if (j != 0) {
                        fullPath += ".";
                    }
                    fullPath += packageList[j];
                }

                lastPackageId = insertPackageRecord (packageList[i], fullPath, lastPackageId, (i+1 == packageTreeDepth) ? packageType : "1");
            }

            return lastPackageId;
        }
    }

    /**
     * Gets an ETK_ table's data from the import file.
     *
     * @param tableName the etk_ table
     * @return the table data
     */
    private List<Map<String, String>> getTable (final String tableName) {
        return getTable("ETK_TABLES", tableName);
    }

    /**
     * This will return the table as a list of maps where the keys are the column names.
     *
     * @param tableName A table name.
     * @param subFolder the folder within the zip file
     * @return List of columns names and values for the given table.
     */
    private List<Map<String, String>> getTable(final String subFolder, final String tableName){


        Document document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new InputSource(
                            new StringReader(
                                    fileMap.get(subFolder + "/" + tableName + ".xml")
                                    )
                            )
                    );

            document.getDocumentElement().normalize();
        } catch (final Exception e) {
            throw new RuntimeException("DocumentBuilderFactory could not parse " + tableName + ".xml, aborting", e);
        }

        final List<Map<String, String>> returnList = new LinkedList<>();
        final NodeList tableNodeList = document.getElementsByTagName(tableName);
        final Node tableNode = tableNodeList.item(0);
        final NodeList tableChildrenNodeList = tableNode.getChildNodes();
        NodeList columnNodeList = null;

        for(int i = 0; i < tableChildrenNodeList.getLength(); i++) {
            final Map<String, String> row = new HashMap<>();
            final Node rowNode = tableChildrenNodeList.item(i);

            if ("#text".equals(rowNode.getNodeName()) && StringUtility.isBlank(rowNode.getNodeValue())) {
                continue;
            }

            columnNodeList = rowNode.getChildNodes();
            for(int j = 0; j < columnNodeList.getLength(); j++) {
                final Node columnNode = columnNodeList.item(j);

                if ("#text".equals(columnNode.getNodeName()) && StringUtility.isBlank(columnNode.getNodeValue())) {
                    continue;
                }

                final Node textNode = columnNode.getChildNodes().item(0);
                row.put(columnNode.getNodeName(), ((textNode == null) ? null : textNode.getNodeValue()));
            }

            returnList.add(row);
        }

        return returnList;
    }

    /**
     * Converts a possibly null String to a BigDecimal.
     *
     * @param s The String representation of the BigDecimal
     * @return the BigDecimal
     */
    private static Number toBigDecimal(final String s){
        return s == null ? null : new BigDecimal(s);
    }

    /**
     * Converts a possibly null String to an Integer.
     *
     * @param s The String representation of the Integer
     * @return the Integer
     */
    private static Integer toInteger(final String s) {
        return StringUtility.isBlank(s) ? null : new Integer(s);
    }

    /**
     * Converts a possibly-null String to a Timestamp.
     *
     * @param timeString The String representation of the Timestamp
     * @return The timestamp
     */
    private Timestamp toTimestamp (final String timeString) {
        if (timeString == null) {
            return null;
        }

        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            return new Timestamp(sdf.parse(timeString).getTime());
        } catch (final ParseException e) {
            etk.getLogger().error("net.micropact.aea.componentImport.Controller:getTimestamp() - " +
                    "Error parsing time \"" + timeString + "\" using format yyyy-MM-dd HH:mm:ss.SSS");
        }

        return null;
    }

    /**
     * Determine if two (possibly null) objects are equal.
     *
     * @param o1 first object
     * @param o2 second object
     * @return whether the two objects are equal
     */
    private static boolean equal(final Object o1, final Object o2){
        if(o1 == null){
            return o2 == null;
        }else{
            return o1.equals(o2);
        }
    }

    /**
     * Select a particular value from a Map from a List of Maps via a given property's value.
     *
     * @param objects maps to select from
     * @param searchKey the key property to search by
     * @param searchValue the key property's value to search for
     * @param valueKey the value key to select from the Map once the matching Map is found
     * @return The selected property from the appropriate Map
     */
    private static Object lookupValueInListOfMaps(final List<Map<String, String>> objects, final Object searchKey,
            final Object searchValue, final Object valueKey){
        for(final Map<String, String> o : objects){
            if(equal(o.get(searchKey), searchValue)){
                return o.get(valueKey);
            }
        }
        return null;
    }

    /**
     * Retrieve information about a script object's package from the export file.
     *
     * @param scriptId the script id
     * @return information about the package. return map contains keys for "PACKAGE_PATH" and "PACKAGE_TYPE".
     */
    private Map<String, String> getScriptPackageInfo (final String scriptId) {
        if (cachedPackages == null) {
            cachedPackages = new HashMap<>();

            final List<Map<String, String>> packages = getTable("ETK_PACKAGE_INFO");

            for(final Map<String, String> aPackage : packages) {
                final Map<String, String> packageInfo = new HashMap<>();
                packageInfo.put("PACKAGE_PATH", aPackage.get("PACKAGE_PATH"));
                packageInfo.put("PACKAGE_TYPE", aPackage.get("PACKAGE_TYPE"));
                cachedPackages.put(aPackage.get("SCRIPT_ID"), packageInfo);
            }
        }

        return cachedPackages.get(scriptId);
    }


    /**
     * This method takes a script object id used in the export file and finds the appropriate script object id
     * in the new system. This is needed because the export file referencing scripts by id (because the underlying
     * entellitrak tables reference them by id instead of path).
     *
     * @param oldScriptId the script_id in the export
     * @return the script object id in the new system
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private Number getMatchingScriptId (final String oldScriptId)
            throws ApplicationException {

        if (oldScriptId == null) {
            return null;
        }
        final List<Map<String, String>> scriptObjects = getTable("ETK_SCRIPT_OBJECT");

        final String oldScriptName = (String)
                lookupValueInListOfMaps(scriptObjects, "SCRIPT_ID", oldScriptId, "NAME");

        final Map<String, String> packageInfo = getScriptPackageInfo(oldScriptId);
        final String oldPath = packageInfo.get("PACKAGE_PATH");

        final List<Map<String, Object>> matchingScriptObjects = new ArrayList<>();

        if (StringUtility.isBlank(oldPath)) {
            for (final Map<String, Object> aScript : this.aeaScriptViewSysOnly) {
                if ((aScript.get("SCRIPT_NAME") != null) &&
                        aScript.get("SCRIPT_NAME").toString().equalsIgnoreCase(oldScriptName) &&
                        (aScript.get("PACKAGE_PATH") == null) &&
                        (aScript.get("PACKAGE_NODE_ID") == null)) {
                    matchingScriptObjects.add(aScript);
                }
            }
        } else {
            for (final Map<String, Object> aScript : this.aeaScriptViewSysOnly) {
                if ((aScript.get("SCRIPT_NAME") != null) &&
                        aScript.get("SCRIPT_NAME").toString().equalsIgnoreCase(oldScriptName) &&
                        (aScript.get("PACKAGE_PATH") != null) &&
                        aScript.get("PACKAGE_PATH").equals(oldPath)) {
                    matchingScriptObjects.add(aScript);
                }
            }
        }

        if (matchingScriptObjects.size() == 0) {
            throw new ApplicationException ("Could not find a matching script with Name=\"" + oldScriptName +
                    "\", Exported ID=\"" + oldScriptId +
                    "\" and Path=\"" + oldPath + "\". Please make sure you export this script.");
        } else if (matchingScriptObjects.size() > 1) {
            throw new ApplicationException ("Found more than 1 script with scriptName=" + oldScriptName +
                    " and path=" + oldPath + " , workspaceId=" + workspaceId +
                    " , trackingConfigId=" + nextTrackingConfigId + ". Oracle allows multiple scripts"
                    + " with different capitalizations in the same directory, please ensure only one script exists.");
        } else {
            return (Number) matchingScriptObjects.get(0).get("SCRIPT_ID");
        }
    }

    /**
     * Import ETK_SCRIPT_OBJECT (and ETK_PACKAGEs required by them).
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ApplicationException If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private void importScriptObjects() throws IncorrectResultSizeDataAccessException,
    ApplicationException {
        final List<Map<String, String>> scriptObjects = getTable("ETK_SCRIPT_OBJECT");
        for(final Map<String, String> scriptObject : scriptObjects) {

            final Map<String, String> packageInfo = getScriptPackageInfo(scriptObject.get("SCRIPT_ID"));
            final String scriptPath = packageInfo.get("PACKAGE_PATH");
            final String packageType = packageInfo.get("PACKAGE_TYPE");
            final String scriptName = scriptObject.get("NAME");
            final String scriptExtension = LanguageType.fromEntellitrakNumber((Integer.parseInt(scriptObject.get("LANGUAGE_TYPE")))).getFileExtension();

            //etk.getLogger().error("scriptPath = " + scriptPath);
            //etk.getLogger().error("scriptName = " + scriptName);
            //etk.getLogger().error("scriptExtension = " + scriptExtension);

            final String scriptZipRelativePath = scriptPath == null ? "" : scriptPath.replace('.', '/') + "/";

            final String scriptFilePath = "SCRIPT_OBJECTS/"
                    + scriptZipRelativePath
                    + scriptName
                    + "." + scriptExtension;

            final List<Map<String, Object>> matchingScriptObjects = new ArrayList<>();

            if (StringUtility.isBlank(scriptPath)) {
                for (final Map<String, Object> aScript : this.aeaScriptViewSysOnly) {
                    if ((aScript.get("SCRIPT_NAME") != null) &&
                            aScript.get("SCRIPT_NAME").toString().equalsIgnoreCase(scriptName) &&
                            (aScript.get("PACKAGE_PATH") == null) &&
                            (aScript.get("PACKAGE_NODE_ID") == null) &&
                            (aScript.get("PACKAGE_TYPE") == null)) {
                        matchingScriptObjects.add(aScript);
                    }
                }
            } else {
                for (final Map<String, Object> aScript : this.aeaScriptViewSysOnly) {
                    if ((aScript.get("SCRIPT_NAME") != null) &&
                            aScript.get("SCRIPT_NAME").toString().equalsIgnoreCase(scriptName) &&
                            (aScript.get("PACKAGE_PATH") != null) &&
                            aScript.get("PACKAGE_PATH").equals(scriptPath)) {
                        matchingScriptObjects.add(aScript);
                    }
                }
            }

            Number matchingScriptId = null;

            if (matchingScriptObjects.size() > 1) {
                throw new ApplicationException ("Found more than 1 script with scriptName=" + scriptObject.get("NAME") +
                        " and path=" + scriptPath + " , workspaceId=" + workspaceId +
                        " , trackingConfigId=" + nextTrackingConfigId + ". Oracle allows multiple scripts"
                        + " with different capitalizations in the same directory, please ensure only one script exists.");
            }

            if(matchingScriptObjects.size() == 0) {
                //Insert
                final Number packageId = findOrCreatePackage(scriptPath, packageType);

                //Check to see if the script object business key is already in use.
                final int existingRecords =
                        etk.createSQL("select count(*) from etk_script_object "
                                + "where BUSINESS_KEY = :businessKey")
                        .setParameter("businessKey", scriptObject.get("BUSINESS_KEY"))
                        .fetchInt();

                //If an existing script is using the same business key but is in a different location than
                //the newly imported script object, update the business key of the existing script object.
                if (existingRecords > 0) {
                    errorsAndWarnings.add("WARNING: Existing script object with business key "
                            + scriptObject.get("BUSINESS_KEY")
                            + " found (possibly refactored.) Changing existing script's business key and "
                            + "importing new copy at " + scriptPath);

                    etk.createSQL("update etk_script_object set business_key = :newBusinessKey "
                            + "where business_key = :oldBusinessKey")
                    .setParameter("newBusinessKey", getScriptUUID(scriptObject.get("NAME")))
                    .setParameter("oldBusinessKey", scriptObject.get("BUSINESS_KEY"))
                    .execute();
                }

                etk.getLogger().error("INSERTING NEW SCRIPT " + scriptObject.get("BUSINESS_KEY"));

                SQLFacade scriptQuery = null;
                if (isSqlServer()) {
                    scriptQuery =
                            etk.createSQL("INSERT INTO etk_script_object ( code, language_type, tracking_config_id, "
                                    + "business_key, name, description, created_by, created_on, "
                                    + "last_updated_by, last_updated_on,"
                                    + "MERGE_NAME, PRE_MERGE_U_PACKAGE_FULL_NAME, PRE_MERGE_S_PACKAGE_FULL_NAME,"
                                    + "HANDLER_TYPE, WORKSPACE_ID, PACKAGE_NODE_ID, DELETE_MERGE_REQUIRED,"
                                    + "MODIFIED_LOCALLY, DELETED_LOCALLY, CREATED_LOCALLY, REVISION, MERGE_CODE, public_resource"
                                    + " ) VALUES "
                                    + "( :code, :language_type, "
                                    + "( ( SELECT tracking_config_id FROM etk_tracking_config "
                                    + "WHERE config_version = "
                                    + "( SELECT MAX(config_version) FROM etk_tracking_config ) ) ), "
                                    + ":business_key, :name, :description, "
                                    + "( SELECT username FROM etk_user WHERE user_id = :userId ) , "
                                    + "dbo.ETKF_getServerTime(), NULL, NULL,"
                                    + ":mergeName, :preMergeU, :preMergeS, :handlerType, :workspaceId,"
                                    + ":packageNodeId, :deleteMergeReq, :modifiedLocally, :deletedLocally,"
                                    + ":createdLocally, :revision, :mergeCode, :publicResource)");
                } else {
                    scriptQuery = etk.createSQL(
                            "INSERT INTO etk_script_object(script_id, code, language_type, tracking_config_id,"
                                    + "business_key, name, description, created_by, created_on, last_updated_by, "
                                    + "last_updated_on,"
                                    + "MERGE_NAME, PRE_MERGE_U_PACKAGE_FULL_NAME, PRE_MERGE_S_PACKAGE_FULL_NAME,"
                                    + "HANDLER_TYPE, WORKSPACE_ID, PACKAGE_NODE_ID, DELETE_MERGE_REQUIRED,"
                                    + "MODIFIED_LOCALLY, DELETED_LOCALLY, CREATED_LOCALLY, REVISION, MERGE_CODE, public_resource"
                                    + ") VALUES( :scriptId, :code, "
                                    + ":language_type, ((SELECT tracking_config_id FROM etk_tracking_config "
                                    + "WHERE config_version = "
                                    + "(SELECT MAX(config_version) FROM etk_tracking_config))), "
                                    + ":business_key, :name, :description, "
                                    + "(SELECT username FROM etk_user WHERE user_id = :userId),"
                                    + " ETKF_GETSERVERTIME(), NULL, NULL,"
                                    + ":mergeName, :preMergeU, :preMergeS, :handlerType, :workspaceId,"
                                    + ":packageNodeId, :deleteMergeReq, :modifiedLocally, :deletedLocally,"
                                    + ":createdLocally, :revision, :mergeCode, :publicResource)");
                }

                scriptQuery
                .setParameter("code", "code")
                .setParameter("language_type", toBigDecimal(scriptObject.get("LANGUAGE_TYPE")))
                .setParameter("business_key", scriptObject.get("BUSINESS_KEY"))
                .setParameter("name", scriptObject.get("NAME"))
                .setParameter("description", scriptObject.get("DESCRIPTION"))
                .setParameter("userId", etk.getCurrentUser().getId())
                .setParameter("mergeName", scriptObject.get("MERGE_NAME"))
                .setParameter("preMergeU", scriptObject.get("PRE_MERGE_U_PACKAGE_FULL_NAME"))
                .setParameter("preMergeS", scriptObject.get("PRE_MERGE_S_PACKAGE_FULL_NAME"))
                .setParameter("handlerType", scriptObject.get("HANDLER_TYPE"))
                .setParameter("workspaceId", workspaceId)
                .setParameter("packageNodeId", packageId)
                .setParameter("deleteMergeReq", scriptObject.get("DELETE_MERGE_REQUIRED"))
                .setParameter("modifiedLocally", scriptObject.get("MODIFIED_LOCALLY"))
                .setParameter("deletedLocally", scriptObject.get("DELETED_LOCALLY"))
                .setParameter("createdLocally", scriptObject.get("CREATED_LOCALLY"))
                .setParameter("revision", scriptObject.get("REVISION"))
                .setParameter("mergeCode", scriptObject.get("MERGE_CODE"))
                .setParameter("publicResource", scriptObject.get("PUBLIC_RESOURCE"));

                if (isSqlServer()) {
                    matchingScriptId = scriptQuery.executeForKey("SCRIPT_ID");
                } else {
                    matchingScriptId = getNextOracleHibernateSequence();
                    scriptQuery.setParameter("scriptId", matchingScriptId);
                    scriptQuery.execute();
                }

                etk.createSQL("UPDATE etk_script_object SET code = :code WHERE script_id = :scriptId")
                .setParameter("code", fileMap.get(scriptFilePath))
                .setParameter("scriptId", matchingScriptId)
                .execute();

            } else {
                //Update
                etk.getLogger().error("UPDATING SCRIPT " + scriptObject.get("NAME"));

                matchingScriptId = (Number) matchingScriptObjects.get(0).get("SCRIPT_ID");

                etk.createSQL(isSqlServer()
                        ? "UPDATE etk_script_object SET "
                        + "code = :code, "
                        + "language_type = :language_type, "
                        + "name = :name, "
                        + "description = :description, "
                        + "last_updated_by = (SELECT username FROM etk_user WHERE user_id = :userId), "
                        + "last_updated_on = dbo.ETKF_getServerTime(), "
                        + "MERGE_NAME = :mergeName, "
                        + "PRE_MERGE_U_PACKAGE_FULL_NAME = :preMergeU, "
                        + "PRE_MERGE_S_PACKAGE_FULL_NAME = :preMergeS, "
                        + "HANDLER_TYPE = :handlerType, "
                        + "DELETE_MERGE_REQUIRED = :deleteMergeReq, "
                        + "MODIFIED_LOCALLY = :modifiedLocally, "
                        + "DELETED_LOCALLY = :deletedLocally, "
                        + "CREATED_LOCALLY = :createdLocally, "
                        + "REVISION = (REVISION + 1), "
                        + "MERGE_CODE = :mergeCode "
                        + "WHERE script_id = :scriptId"
                        : "UPDATE etk_script_object SET "
                        + "code = :code, "
                        + "language_type = :language_type, "
                        + "name = :name, "
                        + "description = :description, "
                        + "last_updated_by = (SELECT username FROM etk_user WHERE user_id = :userId), "
                        + "last_updated_on = ETKF_GETSERVERTIME(), "
                        + "MERGE_NAME = :mergeName, "
                        + "PRE_MERGE_U_PACKAGE_FULL_NAME = :preMergeU, "
                        + "PRE_MERGE_S_PACKAGE_FULL_NAME = :preMergeS, "
                        + "HANDLER_TYPE = :handlerType, "
                        + "DELETE_MERGE_REQUIRED = :deleteMergeReq, "
                        + "MODIFIED_LOCALLY = :modifiedLocally, "
                        + "DELETED_LOCALLY = :deletedLocally, "
                        + "CREATED_LOCALLY = :createdLocally, "
                        + "REVISION = (REVISION + 1), "
                        + "MERGE_CODE = :mergeCode, "
                        + "PUBLIC_RESOURCE = :publicResource "
                        + "WHERE script_id = :scriptId")
                .setParameter("scriptId", matchingScriptId)
                .setParameter("code", fileMap.get(scriptFilePath))
                .setParameter("language_type", scriptObject.get("LANGUAGE_TYPE"))
                .setParameter("name", scriptObject.get("NAME"))
                .setParameter("description", scriptObject.get("DESCRIPTION"))
                .setParameter("userId", etk.getCurrentUser().getId())
                .setParameter("mergeName", scriptObject.get("MERGE_NAME"))
                .setParameter("preMergeU", scriptObject.get("PRE_MERGE_U_PACKAGE_FULL_NAME"))
                .setParameter("preMergeS", scriptObject.get("PRE_MERGE_S_PACKAGE_FULL_NAME"))
                .setParameter("handlerType", scriptObject.get("HANDLER_TYPE"))
                .setParameter("deleteMergeReq", scriptObject.get("DELETE_MERGE_REQUIRED"))
                .setParameter("modifiedLocally", scriptObject.get("MODIFIED_LOCALLY"))
                .setParameter("deletedLocally", scriptObject.get("DELETED_LOCALLY"))
                .setParameter("createdLocally", scriptObject.get("CREATED_LOCALLY"))
                .setParameter("mergeCode", scriptObject.get("MERGE_CODE"))
                .setParameter("publicResource", scriptObject.get("PUBLIC_RESOURCE"))
                .execute();
            }

            oldNewScriptIdMap.put(scriptObject.get("SCRIPT_ID"), matchingScriptId);
        }

        refreshAeaScriptViewSysOnly();
    }

    /**
     * Import ETK_DATA_OBJECT.
     */
    private void importDataObjects() {
        /* This is going to be done in 2 phases, in the first phase, everything will be imported as NOT a child object (because children need to specify parents)
            In the 2nd phase, we will go back and update the child references */

        final List<Map<String, String>> dataObjects = getTable("ETK_DATA_OBJECT");

        /* 1st phase, make sure each dataObject gets into the table */
        for(final Map<String, String> dataObject : dataObjects){
            final List<Map<String, Object>> matchingDataObjects = etk.createSQL("SELECT DATA_OBJECT_ID FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :business_key")
                    .setParameter("trackingConfigId", nextTrackingConfigId)
                    .setParameter("business_key", dataObject.get("BUSINESS_KEY"))
                    .fetchList(); /* DATA_OBJECT_ID */
            if(matchingDataObjects.size() == 0){
                //Insert
                etk.createSQL(isSqlServer()
                        ? "INSERT INTO etk_data_object ( tracking_config_id, parent_object_id, base_object, table_name, table_space, object_type, applied_changes, list_order, list_style, searchable, label, object_name, separate_inbox, business_key, name, description, document_management_enabled, cardinality, designator, auto_assignment) VALUES ( :trackingConfigId, NULL, 1, :tableName, :tableSpace, :objectType, 0, :listOrder, :listStyle, :searchable, :label, :objectName, :separateInbox, :businessKey, :name, :description, :documentManagementEnabled, :cardinality, :designator, :autoAssignment)"
                          : "INSERT INTO etk_data_object (data_object_id, tracking_config_id, parent_object_id, base_object, table_name, table_space, object_type, applied_changes, list_order, list_style, searchable, label, object_name, separate_inbox, business_key, name, description, document_management_enabled, cardinality, designator, auto_assignment) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, :trackingConfigId, NULL, 1, :tableName, :tableSpace, :objectType, 0, :listOrder, :listStyle, :searchable, :label, :objectName, :separateInbox, :businessKey, :name, :description, :documentManagementEnabled, :cardinality, :designator, :autoAssignment)")
                .setParameter("trackingConfigId", nextTrackingConfigId)
                .setParameter("tableName", dataObject.get("TABLE_NAME"))
                .setParameter("tableSpace", dataObject.get("TABLE_SPACE"))
                .setParameter("objectType", dataObject.get("OBJECT_TYPE"))
                .setParameter("listOrder", dataObject.get("LIST_ORDER"))
                .setParameter("listStyle", dataObject.get("LIST_STYLE"))
                .setParameter("searchable", dataObject.get("SEARCHABLE"))
                .setParameter("label", dataObject.get("LABEL"))
                .setParameter("objectName", dataObject.get("OBJECT_NAME"))
                .setParameter("separateInbox", dataObject.get("SEPARATE_INBOX"))
                .setParameter("businessKey", dataObject.get("BUSINESS_KEY"))
                .setParameter("name", dataObject.get("NAME"))
                .setParameter("description", dataObject.get("DESCRIPTION"))
                .setParameter("documentManagementEnabled", dataObject.get("DOCUMENT_MANAGEMENT_ENABLED"))
                .setParameter("cardinality", dataObject.get("CARDINALITY"))
                .setParameter("designator", dataObject.get("DESIGNATOR"))
                .setParameter("autoAssignment", dataObject.get("AUTO_ASSIGNMENT"))
                .execute();
            }else{
                //Update
                etk.createSQL("UPDATE etk_data_object SET parent_object_id = NULL, base_object = 1, table_name = :tableName, table_space = :tableSpace, object_type = :objectType, applied_changes = 0, list_order = :listOrder, list_style = :listStyle, searchable = :searchable, label = :label, object_name = :objectName, separate_inbox = :separateInbox, name = :name, description = :description, document_management_enabled = :documentManagementEnabled, cardinality = :cardinality, designator = :designator, auto_assignment = :autoAssignment WHERE data_object_id = :dataObjectId")
                .setParameter("tableName", dataObject.get("TABLE_NAME"))
                .setParameter("tableSpace", dataObject.get("TABLE_SPACE"))
                .setParameter("objectType", dataObject.get("OBJECT_TYPE"))
                .setParameter("listOrder", dataObject.get("LIST_ORDER"))
                .setParameter("listStyle", dataObject.get("LIST_STYLE"))
                .setParameter("searchable", dataObject.get("SEARCHABLE"))
                .setParameter("label", dataObject.get("LABEL"))
                .setParameter("objectName", dataObject.get("OBJECT_NAME"))
                .setParameter("separateInbox", dataObject.get("SEPARATE_INBOX"))
                .setParameter("businessKey", dataObject.get("BUSINESS_KEY"))
                .setParameter("name", dataObject.get("NAME"))
                .setParameter("description", dataObject.get("DESCRIPTION"))
                .setParameter("documentManagementEnabled", dataObject.get("DOCUMENT_MANAGEMENT_ENABLED"))
                .setParameter("dataObjectId", matchingDataObjects.get(0).get("DATA_OBJECT_ID"))
                .setParameter("cardinality", dataObject.get("CARDINALITY"))
                .setParameter("designator", dataObject.get("DESIGNATOR"))
                .setParameter("autoAssignment", dataObject.get("AUTO_ASSIGNMENT"))
                .execute();
            }
        }

        /* 2nd phase, update the parents now that everything is in the table */
        for(final Map<String, String> dataObject : dataObjects){

            final String parentBusinessKey = (String) (dataObject.get("PARENT_OBJECT_ID") == null
                    ? null
                    : lookupValueInListOfMaps(dataObjects, "DATA_OBJECT_ID",
                            dataObject.get("PARENT_OBJECT_ID"), "BUSINESS_KEY"));

            etk.createSQL("UPDATE etk_data_object SET parent_object_id = (SELECT do.data_object_id FROM etk_data_object do WHERE do.business_key = :parentBusinessKey AND do.tracking_config_id = :trackingConfigId), base_object = :baseObject WHERE tracking_config_id = :trackingConfigId AND business_key = :businessKey")
            .setParameter("parentBusinessKey", parentBusinessKey)
            .setParameter("baseObject", dataObject.get("BASE_OBJECT"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .setParameter("businessKey", dataObject.get("BUSINESS_KEY"))
            .execute();
        }
    }

    /* Deletes dataObjects and Elements */
    /**
     * Deletes necessary ETK_DATA_OBJECT and ETK_DATA_ELEMENT records. We cannot do what we are doing for other tables
     * and blow away all data objects and elements and then re-import because there is a cyclical dependency between
     * data objects/elements and lookups of type data object.
     *
     * <p>
     *  CTOs will be deleted if their BTO in the target system matches an object in the import file but the
     *  import file does not contain the CTO.
     * </p>
     * <p>
     *  Data Elements will be deleted if they belong to a data object which is being imported, but the Data Element
     *  does not exist in the import.
     * </p>
     *
     */
    private void deleteDataObjectsAndElements() {
        /* We're trying to be careful with deleting dataObjects and dataElements because they can be referenced by
         * ETK_LOOKUP_DEFINITIONs which we didn't write. Which is an enormous inconvenience */
        final List<Map<String, String>> importedDataObjects = getTable("ETK_DATA_OBJECT");
        final List<Map<String, String>> importedBaseTrackedObjects = new LinkedList<>();
        /* This will hold the businessKeys of the objects */
        final List<String> currentTreeOfDataObjects = new LinkedList<>();
        final List<String> dataObjectKeysToKeep = new LinkedList<>();
        final List<Map<String, String>> importedDataElements = getTable("ETK_DATA_ELEMENT");
        final List<String> dataElementKeysToKeep = new LinkedList<>();

        /* Get the BTOs */
        for(final Map<String, String> importedDataObject : importedDataObjects){
            if(importedDataObject.get("BASE_OBJECT").equals("1")){
                importedBaseTrackedObjects.add(importedDataObject);
            }
        }
        for(final Map<String, String> importedBaseTrackedObject : importedBaseTrackedObjects){
            currentTreeOfDataObjects.add(importedBaseTrackedObject.get("BUSINESS_KEY"));
        }
        /* Breadth-first search */
        for(int i = 0; i < currentTreeOfDataObjects.size(); i++){
            final List<Map<String, Object>> currentItemsChildren = etk.createSQL("SELECT child.BUSINESS_KEY FROM etk_data_object parent JOIN etk_data_object child ON child.parent_object_id = parent.data_object_id WHERE parent.tracking_config_id = :trackingConfigId AND parent.business_key = :parentBusinessKey")
                    .setParameter("trackingConfigId", nextTrackingConfigId)
                    .setParameter("parentBusinessKey", currentTreeOfDataObjects.get(i))
                    .fetchList(); /* BUSINESS_KEY */
            for(final Map<String, Object> currentItemChild : currentItemsChildren){
                currentTreeOfDataObjects.add((String) currentItemChild.get("BUSINESS_KEY"));
            }
        }

        for(final Map<String, String> importedDataObject : importedDataObjects){
            dataObjectKeysToKeep.add(importedDataObject.get("BUSINESS_KEY"));
        }

        for(final Map<String, String> importedDataElement : importedDataElements){
            dataElementKeysToKeep.add(importedDataElement.get("BUSINESS_KEY"));
        }

        /* Delete data-elements */
        if(currentTreeOfDataObjects.size() > 0 && dataElementKeysToKeep.size() > 0){ //This condition might not technically be correct, but should be good enough
            final StringBuilder query = new StringBuilder();
            final Map<String, Object> paramMap = new HashMap<>();

            if (isSqlServer()) {
                query.append("DELETE FROM etk_data_element WHERE EXISTS ( SELECT * FROM etk_data_object DO WHERE do.data_object_id = etk_data_element.data_object_id AND do.tracking_config_id = :trackingConfigId AND ");
                addLargeInClause("do.business_key", "objectKeys", query, paramMap, currentTreeOfDataObjects);
                query.append(" ) AND ");
                addLargeNotInClause("etk_data_element.business_key", "elementsToKeep", query, paramMap, dataElementKeysToKeep);
            } else {
                query.append("DELETE FROM etk_data_element de WHERE EXISTS(SELECT * FROM etk_data_object do WHERE do.data_object_id = de.data_object_id AND do.tracking_config_id = :trackingConfigId AND ");
                addLargeInClause("do.business_key", "objectKeys", query, paramMap, currentTreeOfDataObjects);
                query.append(") AND ");
                addLargeNotInClause("de.business_key", "elementsToKeep", query, paramMap, dataElementKeysToKeep);
            }

            etk.createSQL(query.toString())
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .setParameter(paramMap)
            .execute();
        }

        /* Delete data-objects */
        if(currentTreeOfDataObjects.size() > 0 && dataObjectKeysToKeep.size() > 0){ //This condition might not technically be correct, but should be good enough
            final StringBuilder query = new StringBuilder();
            final Map<String, Object> paramMap = new HashMap<>();

            if (isSqlServer()) {
                query.append("DELETE FROM etk_data_object WHERE tracking_config_id = :trackingConfigId AND ");
                addLargeInClause("business_key", "allObjects", query, paramMap, currentTreeOfDataObjects);
                query.append(" AND ");
                addLargeNotInClause("business_key", "objectsToKeep", query, paramMap, dataObjectKeysToKeep);
            } else {
                query.append("DELETE FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND ");
                addLargeInClause("do.business_key", "allObjects", query, paramMap, currentTreeOfDataObjects);
                query.append(" AND ");
                addLargeNotInClause("do.business_key", "objectsToKeep", query, paramMap, dataObjectKeysToKeep);
            }

            etk.createSQL(query.toString())
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .setParameter(paramMap)
            .execute();
        }
    }

    /**
     * Delete all the entries from the tracking-config related tables that we will be importing (ETK_DATA_FORM,
     * ETK_DO_STATE, etc.).
     */
    private void deleteEntries() {
        StringBuilder query = new StringBuilder();
        Map<String, Object> paramMap = new HashMap<>();

        //Get a list of all the business keys and ids of dataObjects we're dealing with.

        final List<Map<String, String>> importedDataObjects = getTable("ETK_DATA_OBJECT");
        final List<Map<String, String>> importedBaseTrackedAndReferenceObjects = new LinkedList<>();
        final List<String> currentTreeOfDataObjects = new LinkedList<>();
        /* This will hold the businessKeys of the objects */
        final List<String> allImportedDataObjectBusinessKeys = new ArrayList<>();
        /* Get the BTOs */
        for(final Map<String, String> importedDataObject : importedDataObjects){
            if(importedDataObject.get("BASE_OBJECT").equals("1") || importedDataObject.get("OBJECT_TYPE").equals("2")){
                importedBaseTrackedAndReferenceObjects.add(importedDataObject);
            }

            allImportedDataObjectBusinessKeys.add(importedDataObject.get("BUSINESS_KEY"));
        }
        for(final Map<String, String> importedBaseTrackedReferenceObject : importedBaseTrackedAndReferenceObjects){
            currentTreeOfDataObjects.add(importedBaseTrackedReferenceObject.get("BUSINESS_KEY"));
        }
        /* Breath-first traversal */
        for(int i = 0; i < currentTreeOfDataObjects.size(); i++){
            final List<Map<String, Object>> currentItemsChildren = etk.createSQL("SELECT child.BUSINESS_KEY FROM etk_data_object parent JOIN etk_data_object child ON child.parent_object_id = parent.data_object_id WHERE parent.tracking_config_id = :trackingConfigId AND parent.business_key = :parentBusinessKey")
                    .setParameter("trackingConfigId", nextTrackingConfigId)
                    .setParameter("parentBusinessKey", currentTreeOfDataObjects.get(i))
                    .fetchList(); /* BUSINESS_KEY */
            for(final Map<String, Object> currentItemChild : currentItemsChildren){
                currentTreeOfDataObjects.add((String) currentItemChild.get("BUSINESS_KEY"));
            }
        }

        query.append("SELECT do.DATA_OBJECT_ID FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND ");
        addLargeInClause("do.business_key", "businessKey", query, paramMap, currentTreeOfDataObjects);


        List<Map<String, Object>> dataObjectIdMaps = currentTreeOfDataObjects.size() == 0
                ? new LinkedList<>()
                : etk.createSQL(query.toString())
                .setParameter(paramMap)
                .setParameter("trackingConfigId", nextTrackingConfigId)
                .fetchList();

                /* These are the dataObjectIds of items currently in the system */
                final List<BigDecimal> dataObjectIds = new ArrayList<>();

                for(final Map<String, Object> dataObjectIdMap : dataObjectIdMaps){
                    dataObjectIds.add((BigDecimal) dataObjectIdMap.get("DATA_OBJECT_ID"));
                }
                if(dataObjectIds.size() > 0) { //We're putting this check in because SQLFacade throws up on setting empty list parameters
                    //ETK_TRACKING_EVENT_LISTENER
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_tracking_event_listener WHERE ");
                    addLargeInClause("data_object_id", "dataObjectId", query, paramMap, dataObjectIds);
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();
                    //ETK_DISPLAY_MAPPING
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_display_mapping WHERE ");
                    addLargeInClause("data_object_id", "dataObjectId", query, paramMap, dataObjectIds);
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();
                    //ETK_DO_TIMER
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_do_timer WHERE do_state_id IN (SELECT do_state_id FROM etk_do_state WHERE ");
                    addLargeInClause("data_object_id", "dataObjectId", query, paramMap, dataObjectIds);
                    query.append(")");
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();
                    //ETK_DO_TRANSITION
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_do_transition WHERE do_previous_state_id IN (SELECT do_state_id FROM etk_do_state WHERE ");
                    addLargeInClause("data_object_id", "dataObjectId", query, paramMap, dataObjectIds);
                    query.append(")");
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();
                    //ETK_DO_STATE
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_do_state WHERE ");
                    addLargeInClause("data_object_id", "dataObjectId", query, paramMap, dataObjectIds);
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();
                    //ETK_DATA_EVENT_LISTENER
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_data_event_listener WHERE ");
                    addLargeInClause("data_object_id", "dataObjectId", query, paramMap, dataObjectIds);
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();
                    //ETK_FILTER_HANDLER
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_filter_handler WHERE ");
                    addLargeInClause("data_object_id", "dataObjectId", query, paramMap, dataObjectIds);
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();


                    //If we are not cleaning the data forms, we only want to delete forms/views for BTO/CTO/RDO objects that no longer exist.
                    //For example, if we had a BTO -> CTO -> Grandchild object, and we deleted Grandchild on DEV, we still want to delete forms and
                    //views that are attached to that element.
                    List<BigDecimal> obsoleteDataObjectIds = new ArrayList<>();
                    //Prevent null list createSQL exception
                    obsoleteDataObjectIds.add(new BigDecimal(-1));

                    if (!cleanDataForms) {
                        etk.getLogger().error("Preserving user created data forms/views that were created after last component import.");

                        //Find data objects of the BTO that are not included in the new import. We will assume
                        //these "orphaned" children are OBE and should be deleted.
                        currentTreeOfDataObjects.removeAll(allImportedDataObjectBusinessKeys);

                        //Get the data object IDs of the OBE orphaned children in the target system.

                        query = new StringBuilder();
                        paramMap = new HashMap<>();
                        query.append("SELECT do.DATA_OBJECT_ID FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND ");
                        addLargeInClause("do.business_key", "businessKey", query, paramMap, currentTreeOfDataObjects);

                        dataObjectIdMaps = currentTreeOfDataObjects.size() == 0
                                ? new LinkedList<>()
                                : etk.createSQL(query.toString())
                                .setParameter(paramMap)
                                .setParameter("trackingConfigId", nextTrackingConfigId)
                                .fetchList();

                                for(final Map<String, Object> dataObjectIdMap : dataObjectIdMaps){
                                    obsoleteDataObjectIds.add((BigDecimal) dataObjectIdMap.get("DATA_OBJECT_ID"));
                                }
                    } else {
                        //If doing this "clean", lets just delete all elements associated with the BTO data object ID.
                        etk.getLogger().error("Deleting all user created data forms/views for component on import.");
                        obsoleteDataObjectIds = dataObjectIds;
                    }

                    final List<BigDecimal> dataFormIds = new ArrayList<>();
                    final List<BigDecimal> dataViewIds = new ArrayList<>();
                    final List<String> importedFormBusinessKeys = new ArrayList<>();
                    importedFormBusinessKeys.add("null.form.business.key.to.prevent.exception");
                    final List<String> importedViewBusinessKeys = new ArrayList<>();
                    importedViewBusinessKeys.add("null.view.business.key.to.prevent.exception");

                    //If we are not deleting all forms, we still want to delete re-imported forms and views. We will
                    //attempt to "merge" forms at a future date, but for now we will consider default forms/views
                    //included with a component to be non-editable.
                    if (!cleanDataForms) {
                        final List<Map<String, String>> dataForms = getTable("ETK_DATA_FORM");
                        final List<Map<String, String>> dataViews = getTable("ETK_DATA_VIEW");

                        for (final Map<String, String> aDataForm : dataForms) {
                            importedFormBusinessKeys.add(aDataForm.get("BUSINESS_KEY"));
                        }

                        for (final Map<String, String> aDataView : dataViews) {
                            importedViewBusinessKeys.add(aDataView.get("BUSINESS_KEY"));
                        }
                    }

                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("SELECT EDF.DATA_FORM_ID, EDF.BUSINESS_KEY FROM ETK_DATA_FORM EDF ");
                    query.append("JOIN ETK_DATA_OBJECT EDO on EDO.DATA_OBJECT_ID = EDF.DATA_OBJECT_ID ");
                    query.append("WHERE (");
                    //EDF.DATA_OBJECT_ID IN (:dataObjectIds)
                    addLargeInClause("EDF.DATA_OBJECT_ID", "dataObjectId", query, paramMap, obsoleteDataObjectIds);
                    query.append(" OR ");
                    //EDF.BUSINESS_KEY in (:formBusinessKeys)
                    addLargeInClause("EDF.BUSINESS_KEY", "businessKey", query, paramMap, importedFormBusinessKeys);
                    query.append(")");
                    query.append(" AND ");
                    query.append("(EDO.TRACKING_CONFIG_ID = :trackingConfigId)");


                    //Find data form IDs in target system to delete.
                    final List<Map<String, Object>>
                    dataFormIdsResultSet = etk.createSQL(query.toString())
                    .setParameter(paramMap)
                    .setParameter("trackingConfigId", nextTrackingConfigId)
                    .fetchList();

                    for (final Map<String, Object> aDataFormIdRS : dataFormIdsResultSet) {
                        etk.getLogger().error("Deleting existing dataForm \"" + aDataFormIdRS.get("BUSINESS_KEY") + "\" and all elements.");
                        dataFormIds.add((BigDecimal) aDataFormIdRS.get("DATA_FORM_ID"));
                    }

                    query = new StringBuilder();
                    paramMap = new HashMap<>();

                    query.append("SELECT EDV.DATA_VIEW_ID, EDV.BUSINESS_KEY FROM ETK_DATA_VIEW EDV ");
                    query.append("JOIN ETK_DATA_OBJECT EDO on EDO.DATA_OBJECT_ID = EDV.DATA_OBJECT_ID ");
                    query.append("WHERE (");
                    //EDV.DATA_OBJECT_ID IN (:dataObjectIds)
                    addLargeInClause("EDV.DATA_OBJECT_ID", "dataObjectId", query, paramMap, obsoleteDataObjectIds);
                    query.append(" OR ");
                    //EDV.BUSINESS_KEY in (:viewBusinessKeys)
                    addLargeInClause("EDV.BUSINESS_KEY", "businessKey", query, paramMap, importedViewBusinessKeys);
                    query.append(")");
                    query.append(" AND ");
                    query.append("(EDO.TRACKING_CONFIG_ID = :trackingConfigId)");

                    //Find data view IDs in target system to delete.
                    final List<Map<String, Object>>
                    dataViewIdsResultSet = etk.createSQL(query.toString())
                    .setParameter(paramMap)
                    .setParameter("trackingConfigId", nextTrackingConfigId)
                    .fetchList();

                    for (final Map<String, Object> aDataViewIdRS : dataViewIdsResultSet) {
                        etk.getLogger().error("Deleting existing dataView \"" + aDataViewIdRS.get("BUSINESS_KEY") + "\" and all elements/controls.");
                        dataViewIds.add((BigDecimal) aDataViewIdRS.get("DATA_VIEW_ID"));
                    }


                    //ETK_DATA_VIEW_ELEMENT
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_data_view_element WHERE ");
                    addLargeInClause("data_view_id", "dataViewId", query, paramMap, dataViewIds);
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();

                    //ETK_DATA_VIEW
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_data_view WHERE ");
                    addLargeInClause("data_view_id", "dataViewId", query, paramMap, dataViewIds);
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();



                    //ETK_FORM_CONTROL_EVENT_HANDLER
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_form_control_event_handler WHERE form_control_id IN (SELECT form_control_id FROM etk_form_control WHERE ");
                    addLargeInClause("data_form_id", "dataFormId", query, paramMap, dataFormIds);
                    query.append(")");
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();


                    //ETK_FORM_CTL_LABEL_BINDING
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_form_ctl_label_binding WHERE form_control_id IN (SELECT form_control_id FROM etk_form_control WHERE ");
                    addLargeInClause("data_form_id", "dataFormId", query, paramMap, dataFormIds);
                    query.append(")");
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();


                    //ETK_DATA_FORM_EVENT_HANDLER
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_data_form_event_handler WHERE ");
                    addLargeInClause("data_form_id", "dataFormId", query, paramMap, dataFormIds);
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();

                    //ETK_FORM_CTL_LOOKUP_BINDING
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_form_ctl_lookup_binding WHERE form_control_id IN (SELECT form_control_id FROM etk_form_control WHERE ");
                    addLargeInClause("data_form_id", "dataFormId", query, paramMap, dataFormIds);
                    query.append(")");
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();

                    //ETK_FORM_CTL_ELEMENT_BINDING
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_form_ctl_element_binding WHERE form_control_id IN (SELECT form_control_id FROM etk_form_control WHERE ");
                    addLargeInClause("data_form_id", "dataFormId", query, paramMap, dataFormIds);
                    query.append(")");
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();

                    //ETK_FORM_CONTROL
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_form_control WHERE ");
                    addLargeInClause("data_form_id", "dataFormId", query, paramMap, dataFormIds);
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();

                    //ETK_DATA_FORM
                    query = new StringBuilder();
                    paramMap = new HashMap<>();
                    query.append("DELETE FROM etk_data_form WHERE ");
                    addLargeInClause("data_form_id", "dataFormId", query, paramMap, dataFormIds);
                    etk.createSQL(query.toString()).setParameter(paramMap).execute();
                }
                deleteDataObjectsAndElements();

    }


    /**
     * Delete ETK_PAGE_PERMISSION records which are no longer needed.
     */
    private void deletePagePermissions() {
        final List<Map<String, String>> pages = getTable("ETK_PAGE");

        for (final Map<String, String> aPage : pages) {

            final List<Map<String, Object>> pagePermissions =
                    etk.createSQL("select PAGE_PERMISSION_ID "
                            + "    from ETK_PAGE_PERMISSION where PAGE_ID = "
                            + "      (select PAGE_ID from ETK_PAGE "
                            + "       where BUSINESS_KEY = :businessKey)")
                    .setParameter("businessKey", aPage.get("BUSINESS_KEY"))
                    .fetchList();

            final List<Number> pagePermissionIds = new ArrayList<>();

            for (final Map<String, Object> aPagePermission : pagePermissions) {
                pagePermissionIds.add((Number)
                        aPagePermission.get("PAGE_PERMISSION_ID"));
            }


            etk.createSQL(
                    "delete from ETK_PAGE_PERMISSION where PAGE_ID = " +
                    "    (select PAGE_ID from ETK_PAGE where BUSINESS_KEY = :businessKey) ")
            .setParameter("businessKey", aPage.get("BUSINESS_KEY"))
            .execute();

            if (pagePermissionIds.size() > 0) {

                final StringBuilder query = new StringBuilder();
                final Map<String, Object> paramMap = new HashMap<>();
                query.append("delete from ETK_SHARED_OBJECT_PERMISSION where ");
                addLargeInClause("SHARED_OBJECT_PERMISSION_ID", "sharedObjectPermissionId", query, paramMap, pagePermissionIds);
                etk.createSQL(query.toString()).setParameter(paramMap).execute();
            }

            //etk.createSQL("delete from ETK_PAGE where BUSINESS_KEY = :businessKey")
            //   .setParameter("businessKey", aPage.get("BUSINESS_KEY"))
            //   .execute();
        }
    }

    /**
     * Import ETK_PLUGIN_REGISTRATION.
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private void importPlugins() throws IncorrectResultSizeDataAccessException {
        final List<Map<String, String>> pluginRegistrations = getTable("ETK_PLUGIN_REGISTRATION");

        for(final Map<String, String> pluginRegistration : pluginRegistrations){

            final Map<String, Object> queryParameters = new HashMap<>();
            queryParameters.put("class_name", pluginRegistration.get("CLASS_NAME"));
            queryParameters.put("plugin_type", pluginRegistration.get("PLUGIN_TYPE"));
            queryParameters.put("business_key", pluginRegistration.get("BUSINESS_KEY"));
            queryParameters.put("name", pluginRegistration.get("NAME"));
            queryParameters.put("description", pluginRegistration.get("DESCRIPTION"));
            queryParameters.put("tracking_config_id", nextTrackingConfigId);

            final Object matchingPluginRegistrationId = etk.createSQL("SELECT plugin_registration_id FROM etk_plugin_registration WHERE business_key = :business_key AND tracking_config_id = :tracking_config_id")
                    .setParameter(queryParameters)
                    .returnEmptyResultSetAs(null)
                    .fetchObject();

            if(matchingPluginRegistrationId == null){
                etk.createSQL(isSqlServer() ? "INSERT INTO etk_plugin_registration(class_name, plugin_type, tracking_config_id, business_key, name, description) VALUES(:class_name, :plugin_type, :tracking_config_id, :business_key, :name, :description)"
                                              : "INSERT INTO etk_plugin_registration(plugin_registration_id, class_name, plugin_type, tracking_config_id, business_key, name, description) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, :class_name, :plugin_type, :tracking_config_id, :business_key, :name, :description)")
                .setParameter(queryParameters)
                .execute();
            }else{
                etk.createSQL("UPDATE etk_plugin_registration SET class_name = :class_name, plugin_type = :plugin_type, business_key = :business_key, name = :name, description = :description WHERE plugin_registration_id = :plugin_registration_id")
                .setParameter(queryParameters)
                .setParameter("plugin_registration_id", matchingPluginRegistrationId)
                .execute();
            }
        }
    }

    /**
     * Import EKT_DATA_VIEW.
     */
    private void importDataViews() {
        final List<Map<String, String>> dataViews = getTable("ETK_DATA_VIEW");
        final List<Map<String, String>> dataObjects = getTable("ETK_DATA_OBJECT");

        for(final Map<String, String> dataView : dataViews){
            etk.getLogger().error("Inserting View " + dataView.get("BUSINESS_KEY"));

            etk.createSQL(isSqlServer()
                    ? "INSERT INTO etk_data_view ( data_object_id, title, text, default_view, search_view, business_key, name, description ) VALUES ( ( SELECT do.data_object_id FROM etk_data_object DO WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey ) , :title, :text, :default_view, :search_view, :business_key, :name, :description )"
                      : "INSERT INTO etk_data_view(data_view_id, data_object_id, title, text, default_view, search_view, business_key, name, description) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, (SELECT do.data_object_id FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey), :title, :text, :default_view, :search_view, :business_key, :name, :description)")
            .setParameter("text", dataView.get("TEXT"))
            .setParameter("title", dataView.get("TITLE"))
            .setParameter("business_key", dataView.get("BUSINESS_KEY"))
            .setParameter("default_view", dataView.get("DEFAULT_VIEW"))
            .setParameter("description", dataView.get("DESCRIPTION"))
            .setParameter("dataObjectBusinessKey", lookupValueInListOfMaps(dataObjects, "DATA_OBJECT_ID", dataView.get("DATA_OBJECT_ID"), "BUSINESS_KEY"))
            .setParameter("name", dataView.get("NAME"))
            .setParameter("search_view", dataView.get("SEARCH_VIEW"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .execute();
        }
    }

    /**
     * Import ETK_DO_STATE.
     */
    private void importDoStates() {
        final List<Map<String, String>> doStates = getTable("ETK_DO_STATE");
        final List<Map<String, String>> dataObjects = getTable("ETK_DATA_OBJECT");

        for(final Map<String, String> doState : doStates){
            etk.createSQL(isSqlServer()
                    ? "INSERT INTO etk_do_state ( name, description, type, data_object_id, applied_changes, x, y, business_key ) VALUES ( :name, :description, :type, ( SELECT do.data_object_id FROM etk_data_object DO WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey ) , 0, :x, :y, :business_key )"
                      : "INSERT INTO etk_do_state (do_state_id, name, description, type, data_object_id, applied_changes, x, y, business_key) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, :name, :description, :type, (SELECT do.data_object_id FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey), 0, :x, :y, :business_key)")
            .setParameter("name", doState.get("NAME"))
            .setParameter("description", doState.get("DESCRIPTION"))
            .setParameter("type", doState.get("TYPE"))
            .setParameter("dataObjectBusinessKey", lookupValueInListOfMaps(dataObjects, "DATA_OBJECT_ID", doState.get("DATA_OBJECT_ID"), "BUSINESS_KEY"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .setParameter("x", doState.get("X"))
            .setParameter("y", doState.get("Y"))
            .setParameter("business_key", doState.get("BUSINESS_KEY"))
            .execute();
        }
    }

    /**
     * Import ETK_DO_TRANSITION.
     *
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void importDoTransitions() throws ApplicationException{
        final List<Map<String, String>> doTransitions = getTable("ETK_DO_TRANSITION");
        final List<Map<String, String>> doStates = getTable("ETK_DO_STATE");
        for(final Map<String, String> doTransition : doTransitions){
            etk.createSQL(isSqlServer()
                    ? "INSERT INTO etk_do_transition ( name, description, seq, do_previous_state_id, do_next_state_id, action_handler, applied_changes, business_key, TRIGGER_SCRIPT_ID, TRANSITION_TYPE ) VALUES ( :name, :description, :seq, ( SELECT doState.do_state_id FROM etk_do_state doState JOIN etk_data_object DO ON do.data_object_id = doState.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND doState.business_key = :previousDoStateBusinessKey ) , ( SELECT doState.do_state_id FROM etk_do_state doState JOIN etk_data_object DO ON do.data_object_id = doState.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND doState.business_key = :nextDoStateBusinessKey ) , :action_handler, 0, :business_key, :triggerScriptId, :transitionType )"
                      : "INSERT INTO etk_do_transition(do_transition_id, name, description, seq, do_previous_state_id, do_next_state_id, action_handler, applied_changes, business_key, TRIGGER_SCRIPT_ID, TRANSITION_TYPE) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, :name, :description, :seq, (SELECT doState.do_state_id FROM etk_do_state doState JOIN etk_data_object do ON do.data_object_id = doState.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND doState.business_key = :previousDoStateBusinessKey), (SELECT doState.do_state_id FROM etk_do_state doState JOIN etk_data_object do ON do.data_object_id = doState.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND doState.business_key = :nextDoStateBusinessKey), :action_handler, 0, :business_key, :triggerScriptId, :transitionType)")
            .setParameter("nextDoStateBusinessKey", lookupValueInListOfMaps(doStates, "DO_STATE_ID", doTransition.get("DO_NEXT_STATE_ID"), "BUSINESS_KEY"))
            .setParameter("previousDoStateBusinessKey", lookupValueInListOfMaps(doStates, "DO_STATE_ID", doTransition.get("DO_PREVIOUS_STATE_ID"), "BUSINESS_KEY"))
            .setParameter("business_key", doTransition.get("BUSINESS_KEY"))
            //.setParameter("triggerCode", doTransition.get("TRIGGER_CODE"))
            .setParameter("description", doTransition.get("DESCRIPTION"))
            .setParameter("name", doTransition.get("NAME"))
            .setParameter("seq", doTransition.get("SEQ"))
            //.setParameter("trigger_language", doTransition.get("TRIGGER_LANGUAGE"))
            .setParameter("action_handler", doTransition.get("ACTION_HANDLER"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .setParameter("transitionType", doTransition.get("TRANSITION_TYPE"))
            .setParameter("triggerScriptId", getMatchingScriptId (doTransition.get("TRIGGER_SCRIPT_ID")))
            .execute();
        }
    }

    /**
     * Import ETK_TRACKING_EVENT_LISTENER.
     */
    private void importTrackingEventListeners() {
        final List<Map<String, String>> trackingEventListeners = getTable("ETK_TRACKING_EVENT_LISTENER");
        final List<Map<String, String>> dataObjects = getTable("ETK_DATA_OBJECT");
        final List<Map<String, String>> doTransitions = getTable("ETK_DO_TRANSITION");

        for(final Map<String, String> trackingEventListener : trackingEventListeners){
            etk.createSQL(isSqlServer()
                    ? "INSERT INTO etk_tracking_event_listener ( name, do_transition_id, data_object_id, tracking_event, business_key, description ) VALUES ( :name, ( SELECT transition.do_transition_id FROM etk_do_transition transition JOIN etk_do_state doState ON doState.do_state_id = transition.do_previous_state_id JOIN etk_data_object DO ON do.data_object_id = doState.data_object_id WHERE transition.business_key = :doTransitionBusinessKey AND do.tracking_config_id = :trackingConfigId ) , ( SELECT do.data_object_id FROM etk_data_object DO WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey ) , :tracking_event, :business_key, :description )"
                      : "INSERT INTO etk_tracking_event_listener(listener_id, name, do_transition_id, data_object_id, tracking_event, business_key, description) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, :name, (SELECT transition.do_transition_id FROM etk_do_transition transition JOIN etk_do_state doState ON doState.do_state_id = transition.do_previous_state_id JOIN etk_data_object do ON do.data_object_id = doState.data_object_id WHERE transition.business_key = :doTransitionBusinessKey AND do.tracking_config_id = :trackingConfigId), (SELECT do.data_object_id FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey), :tracking_event, :business_key, :description) ")
            .setParameter("business_key", trackingEventListener.get("BUSINESS_KEY"))
            .setParameter("description", trackingEventListener.get("DESCRIPTION"))
            .setParameter("dataObjectBusinessKey", lookupValueInListOfMaps(dataObjects, "DATA_OBJECT_ID", trackingEventListener.get("DATA_OBJECT_ID"), "BUSINESS_KEY"))
            .setParameter("doTransitionBusinessKey", lookupValueInListOfMaps(doTransitions, "DO_TRANSITION_ID", trackingEventListener.get("DO_TRANSITION_ID"), "BUSINESS_KEY"))
            .setParameter("tracking_event", trackingEventListener.get("TRACKING_EVENT"))
            .setParameter("name", trackingEventListener.get("NAME"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .execute();
        }
    }

    /**
     * Import ETK_ROLE.
     */
    private void importRoles() {

        final Map<String, Object> destinationRoleBusinessKeys = listToMap (
                etk.createSQL("SELECT BUSINESS_KEY as \"KEY\", 1 as \"VALUE\" FROM etk_role").fetchList());

        final List<Map<String, String>> importedRoles = getTable("ROLES", "ROLE");

        for(final Map<String, String> importedRole : importedRoles) {

            if (destinationRoleBusinessKeys.containsKey(importedRole.get("BUSINESS_KEY"))) {
                etk.createSQL("UPDATE ETK_ROLE set NAME = :name, DESCRIPTION = :description, PROFILE = :profile where BUSINESS_KEY = :businessKey")
                .setParameter("name", importedRole.get("NAME"))
                .setParameter("description", importedRole.get("DESCRIPTION"))
                .setParameter("businessKey", importedRole.get("BUSINESS_KEY"))
                .setParameter("profile", importedRole.get("PROFILE"))
                .execute();

                etk.getLogger().error("Updated Role " + importedRole.get("BUSINESS_KEY"));

            } else {
                etk.createSQL(isSqlServer()
                        ? "insert into etk_role (NAME, DESCRIPTION, BUSINESS_KEY, PROFILE) values (:name, :description, :businessKey, :profile)"
                          : "insert into etk_role (ROLE_ID, NAME, DESCRIPTION, BUSINESS_KEY, PROFILE) values (HIBERNATE_SEQUENCE.NEXTVAL, :name, :description, :businessKey, :profile)")
                .setParameter("name", importedRole.get("NAME"))
                .setParameter("description", importedRole.get("DESCRIPTION"))
                .setParameter("businessKey", importedRole.get("BUSINESS_KEY"))
                .setParameter("profile", importedRole.get("PROFILE"))
                .execute();

                etk.getLogger().error("Inserted Role " + importedRole.get("BUSINESS_KEY"));
            }
        }
    }

    /**
     * Import ETK_DATA_PERMISSION.
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private void importRoleDataPermissions() throws IncorrectResultSizeDataAccessException {

        final List<Map<String, String>> importedRolesDataPermissionsList = getTable("ROLES", "ROLE_DATA_PERMISSIONS");

        //Check to see if there is an existing data permission for this data object / role within the system.
        for (final Map<String, String> importedDataPermission : importedRolesDataPermissionsList) {
            final Map<String, Object> dataPermissionAndRoleIds = etk.createSQL(
                    " select do.DATA_PERMISSION_ID as DATA_PERMISSION_ID, r.ROLE_ID as ROLE_ID"
                            + " from ETK_DATA_PERMISSION do "
                            + " join etk_role r on do.role_id = r.role_id "
                            + " where do.DATA_OBJECT_TYPE = :dataObjectType "
                            + " and r.business_key = :roleBusinessKey "
                            + "and (data_element_type = :dataElementType OR (data_element_type IS NULL AND :dataElementType IS NULL))")
                    .setParameter("dataObjectType", importedDataPermission.get("DATA_OBJECT_TYPE"))
                    .setParameter("dataElementType", importedDataPermission.get("DATA_ELEMENT_TYPE"))
                    .setParameter("roleBusinessKey", importedDataPermission.get("ROLE_BUSINESS_KEY"))
                    .returnEmptyResultSetAs(new HashMap<String, Object>())
                    .fetchMap();

            final Number dataPermissionId = (Number) dataPermissionAndRoleIds.get("DATA_PERMISSION_ID");
            Number roleId = (Number) dataPermissionAndRoleIds.get("ROLE_ID");

            //If there is an existing data permission for the role / data object combination, update the
            //data permissions.
            if (dataPermissionId != null) {
                etk.createSQL ("update ETK_DATA_PERMISSION set "
                        + " ASSIGN_ACCESS_LEVEL = :assignAL, "
                        + " CREATE_ACCESS_LEVEL = :createAL, "
                        + " READ_ACCESS_LEVEL = :readAL, "
                        + " READ_CONTENT_ACCESS_LEVEL = :readContentAL, "
                        + " UPDATE_ACCESS_LEVEL = :updateAL, "
                        + " DELETE_ACCESS_LEVEL = :deleteAL, "
                        + " REPORTING_ACCESS_LEVEL = :reportingAL, "
                        + " SEARCHING_ACCESS_LEVEL = :searchAL, "
                        + " INBOX_ENABLED = :inboxEnabled "
                        + " WHERE DATA_PERMISSION_ID = :dataPermissionId")
                .setParameter("assignAL", getAccessLevel(importedDataPermission.get("ASSIGN")))
                .setParameter("createAL", getAccessLevel(importedDataPermission.get("CREATE")))
                .setParameter("readAL", getAccessLevel(importedDataPermission.get("READ")))
                .setParameter("readContentAL", getAccessLevel(importedDataPermission.get("READ_CONTENT")))
                .setParameter("updateAL", getAccessLevel(importedDataPermission.get("UPDATE")))
                .setParameter("deleteAL", getAccessLevel(importedDataPermission.get("DELETE")))
                .setParameter("reportingAL", getAccessLevel(importedDataPermission.get("REPORTING")))
                .setParameter("searchAL", getAccessLevel(importedDataPermission.get("SEARCH")))
                .setParameter("inboxEnabled", importedDataPermission.get("INBOX_ENABLED"))
                .setParameter("dataPermissionId", dataPermissionId)
                .execute();

                etk.getLogger().error("Updated ETK_DATA_PERMISSION with ID = " + dataPermissionId);
            } else {
                //If there is not an existing data object permission, ensure that the data object exists!
                final boolean dataObjectExists = etk.createSQL(
                        "select count(1) from ETK_DATA_OBJECT "
                                + " where TABLE_NAME = :dataObjectType"
                                + " and tracking_config_id = :nextTrackingConfigId ")
                        .setParameter("dataObjectType", importedDataPermission.get("DATA_OBJECT_TYPE"))
                        .setParameter("nextTrackingConfigId", nextTrackingConfigId)
                        .returnEmptyResultSetAs(new Integer(0))
                        .fetchInt() > 0;

                        final boolean dataElementExistsOrIsNull = dataObjectExists &&
                                (importedDataPermission.get("DATA_ELEMENT_TYPE") == null
                                || 0 < etk.createSQL("SELECT COUNT(*) FROM etk_data_object do JOIN etk_data_element de ON de.data_object_id = do.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND do.table_name = :dataObjectType AND de.column_name = :dataElementType")
                                .setParameter("trackingConfigId", nextTrackingConfigId)
                                .setParameter("dataObjectType", importedDataPermission.get("DATA_OBJECT_TYPE"))
                                .setParameter("dataElementType", importedDataPermission.get("DATA_ELEMENT_TYPE"))
                                .fetchInt());

                        //Only add the role permissions for the data object if the data object exists within the system.
                        if (dataObjectExists && dataElementExistsOrIsNull) {

                            final Map<String, Object> roleIdMap = etk.createSQL(
                                    " select r.ROLE_ID as ROLE_ID from etk_role r where r.business_key = :roleBusinessKey ")
                                    .setParameter("roleBusinessKey", importedDataPermission.get("ROLE_BUSINESS_KEY"))
                                    .returnEmptyResultSetAs(new HashMap<String, Object>())
                                    .fetchMap();

                            roleId = (Number) roleIdMap.get("ROLE_ID");

                            if (isSqlServer()) {
                                etk.createSQL ("insert into ETK_DATA_PERMISSION "
                                        + "(ROLE_ID, DATA_OBJECT_TYPE, DATA_ELEMENT_TYPE, ASSIGN_ACCESS_LEVEL, CREATE_ACCESS_LEVEL, READ_ACCESS_LEVEL, READ_CONTENT_ACCESS_LEVEL, UPDATE_ACCESS_LEVEL, DELETE_ACCESS_LEVEL, REPORTING_ACCESS_LEVEL, SEARCHING_ACCESS_LEVEL, INBOX_ENABLED) values "
                                        + "(:roleId, :dataObjectType, :dataElementType, :assignAL, :createAL, :readAL, :readContentAL, :updateAL, :deleteAL, :reportingAL, :searchAL, :inboxEnabled)")
                                .setParameter("roleId", roleId)
                                .setParameter("dataObjectType", importedDataPermission.get("DATA_OBJECT_TYPE"))
                                .setParameter("dataElementType", importedDataPermission.get("DATA_ELEMENT_TYPE"))
                                .setParameter("assignAL", getAccessLevel(importedDataPermission.get("ASSIGN")))
                                .setParameter("createAL", getAccessLevel(importedDataPermission.get("CREATE")))
                                .setParameter("readAL", getAccessLevel(importedDataPermission.get("READ")))
                                .setParameter("readContentAL", getAccessLevel(importedDataPermission.get("READ_CONTENT")))
                                .setParameter("updateAL", getAccessLevel(importedDataPermission.get("UPDATE")))
                                .setParameter("deleteAL", getAccessLevel(importedDataPermission.get("DELETE")))
                                .setParameter("reportingAL", getAccessLevel(importedDataPermission.get("REPORTING")))
                                .setParameter("searchAL", getAccessLevel(importedDataPermission.get("SEARCH")))
                                .setParameter("inboxEnabled", importedDataPermission.get("INBOX_ENABLED"))
                                .execute();
                            } else {
                                etk.createSQL ("insert into ETK_DATA_PERMISSION "
                                        + "(DATA_PERMISSION_ID, ROLE_ID, DATA_OBJECT_TYPE, DATA_ELEMENT_TYPE, ASSIGN_ACCESS_LEVEL, CREATE_ACCESS_LEVEL, READ_ACCESS_LEVEL, READ_CONTENT_ACCESS_LEVEL, UPDATE_ACCESS_LEVEL, DELETE_ACCESS_LEVEL, REPORTING_ACCESS_LEVEL, SEARCHING_ACCESS_LEVEL, INBOX_ENABLED) values "
                                        + "(HIBERNATE_SEQUENCE.NEXTVAL, :roleId, :dataObjectType, :dataElementType, :assignAL, :createAL, :readAL, :readContentAL, :updateAL, :deleteAL, :reportingAL, :searchAL, :inboxEnabled)")
                                .setParameter("roleId", roleId)
                                .setParameter("dataObjectType", importedDataPermission.get("DATA_OBJECT_TYPE"))
                                .setParameter("dataElementType", importedDataPermission.get("DATA_ELEMENT_TYPE"))
                                .setParameter("assignAL", getAccessLevel(importedDataPermission.get("ASSIGN")))
                                .setParameter("createAL", getAccessLevel(importedDataPermission.get("CREATE")))
                                .setParameter("readAL", getAccessLevel(importedDataPermission.get("READ")))
                                .setParameter("readContentAL", getAccessLevel(importedDataPermission.get("READ_CONTENT")))
                                .setParameter("updateAL", getAccessLevel(importedDataPermission.get("UPDATE")))
                                .setParameter("deleteAL", getAccessLevel(importedDataPermission.get("DELETE")))
                                .setParameter("reportingAL", getAccessLevel(importedDataPermission.get("REPORTING")))
                                .setParameter("searchAL", getAccessLevel(importedDataPermission.get("SEARCH")))
                                .setParameter("inboxEnabled", importedDataPermission.get("INBOX_ENABLED"))
                                .execute();
                            }

                            etk.getLogger().error("Created new ETK_DATA_PERMISSION for Role = " + roleId
                                    + ", dataObjectType = " + importedDataPermission.get("DATA_OBJECT_TYPE"));
                        } else {
                            etk.getLogger().error("DATA_OBJECT_TYPE " + importedDataPermission.get("DATA_OBJECT_TYPE")
                            + " does not exist in destination system, skipping ETK_DATA_PERMISSION for "
                            + "this object");
                        }
            }
        }
    }

    /**
     * Import ETK_ROLE_PERMISSION.
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private void importRolePermissions() throws IncorrectResultSizeDataAccessException {

        final List<Map<String, String>> importedRoles = getTable("ROLES", "ROLE");

        //Clear out existing role permissions for imported roles.
        for (final Map<String, String> aRole : importedRoles) {
            final Number roleId = (Number) etk.createSQL(
                    " select r.ROLE_ID as ROLE_ID from etk_role r "
                            + " where r.business_key = :roleBusinessKey ")
                    .setParameter("roleBusinessKey", aRole.get("BUSINESS_KEY"))
                    .returnEmptyResultSetAs(null)
                    .fetchObject();

            if (roleId == null) {
                throw new RuntimeException("Could not import Role Permissions for role with business key " +
                        aRole.get("BUSINESS_KEY") +
                        " - role definition should have been imported by now.");
            }

            etk.createSQL("delete from ETK_ROLE_PERMISSION where ROLE_ID = :roleId")
            .setParameter("roleId", roleId)
            .execute();
        }

        //Import new role permissions into the system.
        final List<Map<String, String>> importedRolePermissions = getTable("ROLES", "ROLE_PERMISSIONS");
        for (final Map<String, String> aRolePermission : importedRolePermissions) {
            etk.createSQL("insert into ETK_ROLE_PERMISSION (ROLE_ID, PERMISSION_KEY) values " +
                    " ((select ROLE_ID from etk_role where BUSINESS_KEY = :roleBusinessKey), :permissionKey)")
            .setParameter("roleBusinessKey", aRolePermission.get("BUSINESS_KEY"))
            .setParameter("permissionKey", aRolePermission.get("PERMISSION_KEY"))
            .execute();
        }
    }


    /**
     * Import ETK_JOB, ETK_JOB_SYSTEM, ETK_JOB_CUSTOM.
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void importJobs() throws IncorrectResultSizeDataAccessException, ApplicationException {
        final List<Map<String, String>> importedJobs = getTable("JOBS", "JOB_DATA");

        importQuartzJobDetails();
        importQuartzTriggers();
        importQuartzCalendars();
        importQuartzBlobTriggers();
        importQuartzCronTriggers();
        importQuartzFiredTriggers();
        importQuartzSimpleTriggers();
        importQuartzSimpropTriggers();

        for (final Map<String, String> importedJob : importedJobs) {

            final Number existingJobId = (Number) etk.createSQL(
                    "select JOB_ID from ETK_JOB where BUSINESS_KEY = :jobBusinessKey")
                    .setParameter("jobBusinessKey", importedJob.get("BUSINESS_KEY"))
                    .returnEmptyResultSetAs(null)
                    .fetchObject();

            if (existingJobId != null) {
                etk.createSQL("update ETK_JOB set"
                        + " NAME = :NAME, "
                        + " BUSINESS_KEY = :BUSINESS_KEY, "
                        + " DESCRIPTION = :DESCRIPTION, "
                        + " TRIGGER_TYPE = :TRIGGER_TYPE, "
                        + " CRON_TRIGGER_TYPE = :CRON_TRIGGER_TYPE, "
                        + " CRON_TRIGGER_EXPRESSION = :CRON_TRIGGER_EXPRESSION, "
                        + " REPEAT_INTERVAL = :REPEAT_INTERVAL, "
                        + " REPEAT_UNITS = :REPEAT_UNITS, "
                        + " END_DATE = :END_DATE, "
                        //+ " LAST_RUN_ON = :LAST_RUN_ON, "
                        //+ " EXCEPTION = :EXCEPTION, "
                        //+ " NEXT_RUN_ON = :NEXT_RUN_ON, "
                        + " ACTIVE = :ACTIVE, "
                        + " RUNNING = :RUNNING, "
                        + " SUSPEND_ON_ERROR = :SUSPEND_ON_ERROR, "
                        + " APPLICATION_DEFINITION = :APPLICATION_DEFINITION, "
                        + " LAST_UPDATED_BY = :LAST_UPDATED_BY, " +
                        (isSqlServer() ?
                                        " LAST_UPDATED_ON = dbo.ETKF_getServerTime() " :
                                " LAST_UPDATED_ON = ETKF_GETSERVERTIME() ")
                        + " WHERE JOB_ID = :JOB_ID")
                .setParameter("NAME", importedJob.get("NAME"))
                .setParameter("BUSINESS_KEY", importedJob.get("BUSINESS_KEY"))
                .setParameter("DESCRIPTION", importedJob.get("DESCRIPTION"))
                .setParameter("TRIGGER_TYPE", importedJob.get("TRIGGER_TYPE"))
                .setParameter("CRON_TRIGGER_TYPE", importedJob.get("CRON_TRIGGER_TYPE"))
                .setParameter("CRON_TRIGGER_EXPRESSION", importedJob.get("CRON_TRIGGER_EXPRESSION"))
                .setParameter("REPEAT_INTERVAL", importedJob.get("REPEAT_INTERVAL"))
                .setParameter("REPEAT_UNITS", importedJob.get("REPEAT_UNITS"))
                .setParameter("END_DATE", toTimestamp(importedJob.get("END_DATE")))
                .setParameter("ACTIVE", importedJob.get("ACTIVE"))
                .setParameter("RUNNING", importedJob.get("RUNNING"))
                .setParameter("SUSPEND_ON_ERROR", importedJob.get("SUSPEND_ON_ERROR"))
                .setParameter("APPLICATION_DEFINITION", importedJob.get("APPLICATION_DEFINITION"))
                .setParameter("LAST_UPDATED_BY", etk.getCurrentUser().getAccountName())
                .setParameter("JOB_ID", existingJobId)
                .execute();

                if ("SYSTEM".equals(importedJob.get("JOB_TYPE"))) {
                    etk.createSQL("update ETK_JOB_SYSTEM "
                            + " set JOB_TYPE = :JOB_TYPE "
                            + " where JOB_SYSTEM_ID = :JOB_ID")
                    .setParameter("JOB_TYPE", importedJob.get("SYSTEM_JOB_TYPE"))
                    .setParameter("JOB_ID", existingJobId)
                    .execute();
                } else {
                    etk.createSQL("update ETK_JOB_CUSTOM "
                            + " set SCRIPT_OBJECT_ID = :SCRIPT_OBJECT_ID"
                            + " where JOB_CUSTOM_ID = :JOB_ID")
                    .setParameter("SCRIPT_OBJECT_ID", getMatchingScriptId(importedJob.get("SCRIPT_OBJECT_ID")))
                    .setParameter("JOB_ID", existingJobId)
                    .execute();
                }
            } else {
                Number newJobId = new Integer(0);

                if (!isSqlServer()) {
                    newJobId = getNextOracleHibernateSequence();
                }

                final SQLFacade query =
                        etk.createSQL("insert into ETK_JOB ("
                                + (isSqlServer() ? "" : "JOB_ID, ")
                                + " NAME, "
                                + " BUSINESS_KEY, "
                                + " DESCRIPTION, "
                                + " TRIGGER_TYPE, "
                                + " CRON_TRIGGER_TYPE, "
                                + " CRON_TRIGGER_EXPRESSION, "
                                + " REPEAT_INTERVAL, "
                                + " REPEAT_UNITS, "
                                + " END_DATE, "
                                + " ACTIVE, "
                                + " RUNNING, "
                                + " SUSPEND_ON_ERROR, "
                                + " APPLICATION_DEFINITION, "
                                + " CREATED_BY, "
                                + " CREATED_ON, "
                                + " LAST_UPDATED_BY, "
                                + " LAST_UPDATED_ON "
                                + " ) VALUES ("
                                + (isSqlServer() ? "" : ":JOB_ID, ")
                                + " :NAME, "
                                + " :BUSINESS_KEY, "
                                + " :DESCRIPTION, "
                                + " :TRIGGER_TYPE, "
                                + " :CRON_TRIGGER_TYPE, "
                                + " :CRON_TRIGGER_EXPRESSION, "
                                + " :REPEAT_INTERVAL, "
                                + " :REPEAT_UNITS, "
                                + " :END_DATE, "
                                + " :ACTIVE, "
                                + " :RUNNING, "
                                + " :SUSPEND_ON_ERROR, "
                                + " :APPLICATION_DEFINITION, "
                                + " :CREATED_BY, " +
                                (isSqlServer() ?
                                                " dbo.ETKF_getServerTime(), " :
                                        " ETKF_GETSERVERTIME(), ")
                                + " :LAST_UPDATED_BY, " +
                                (isSqlServer() ?
                                                " dbo.ETKF_getServerTime() " :
                                        " ETKF_GETSERVERTIME() ")
                                + ")")
                        .setParameter("JOB_ID", newJobId)
                        .setParameter("NAME", importedJob.get("NAME"))
                        .setParameter("BUSINESS_KEY", importedJob.get("BUSINESS_KEY"))
                        .setParameter("DESCRIPTION", importedJob.get("DESCRIPTION"))
                        .setParameter("TRIGGER_TYPE", importedJob.get("TRIGGER_TYPE"))
                        .setParameter("CRON_TRIGGER_TYPE", importedJob.get("CRON_TRIGGER_TYPE"))
                        .setParameter("CRON_TRIGGER_EXPRESSION", importedJob.get("CRON_TRIGGER_EXPRESSION"))
                        .setParameter("REPEAT_INTERVAL", importedJob.get("REPEAT_INTERVAL"))
                        .setParameter("REPEAT_UNITS", importedJob.get("REPEAT_UNITS"))
                        .setParameter("END_DATE", toTimestamp(importedJob.get("END_DATE")))
                        .setParameter("ACTIVE", importedJob.get("ACTIVE"))
                        .setParameter("RUNNING", importedJob.get("RUNNING"))
                        .setParameter("SUSPEND_ON_ERROR", importedJob.get("SUSPEND_ON_ERROR"))
                        .setParameter("APPLICATION_DEFINITION", importedJob.get("APPLICATION_DEFINITION"))
                        .setParameter("CREATED_BY", etk.getCurrentUser().getAccountName())
                        .setParameter("LAST_UPDATED_BY", etk.getCurrentUser().getAccountName());

                if (isSqlServer()) {
                    newJobId = query.executeForKey("JOB_ID");
                } else {
                    query.execute();
                }

                if ("SYSTEM".equals(importedJob.get("JOB_TYPE"))) {
                    etk.createSQL("insert into ETK_JOB_SYSTEM (JOB_SYSTEM_ID, JOB_TYPE) VALUES (:JOB_ID, :JOB_TYPE)")
                    .setParameter("JOB_ID", newJobId)
                    .setParameter("JOB_TYPE", importedJob.get("SYSTEM_JOB_TYPE"))
                    .execute();
                } else {
                    etk.createSQL("insert into ETK_JOB_CUSTOM (JOB_CUSTOM_ID, SCRIPT_OBJECT_ID) VALUES (:JOB_ID, :SCRIPT_OBJECT_ID)")
                    .setParameter("JOB_ID", newJobId)
                    .setParameter("SCRIPT_OBJECT_ID", getMatchingScriptId(importedJob.get("SCRIPT_OBJECT_ID")))
                    .execute();
                }
            }

            //Execute to update quartz tables (hopefully!)
            //etk.getLogger().error("Begin save of job with BUSINESS_KEY = " + importedJob.get("BUSINESS_KEY"));
            //final Job tempJob = etk.getJobService().getJob(importedJob.get("BUSINESS_KEY"));
            //etk.getJobService().save(tempJob);
            //etk.getLogger().error("End save of job with BUSINESS_KEY = " + importedJob.get("BUSINESS_KEY"));
        }
    }

    /**
     * This would import ETK_DO_TIMER, however I don't have any timers in the system i'm building this in, so this
     * function is completely untested (and all timers should be migrated to quartz jobs instead of jbpm timers).
     */
    // private static void importDoTimers(){
    //final List<Map<String, String>> doTimers = getTable("ETK_DO_TIMER");
    // for (final Map<String, String> doTimer : doTimers){
    // TODO: Implement DO Timers
    // }
    // }

    /**
     * Imports ETK_DATA_FORM_EVENT_HANDLER.
     *
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void importDataFormEventHandlers() throws ApplicationException{
        final List<Map<String, String>> dataFormEventHandlers = getTable("ETK_DATA_FORM_EVENT_HANDLER");
        final List<Map<String, String>> dataForms = getTable("ETK_DATA_FORM");
        for (final Map<String, String> dataFormEventHandler : dataFormEventHandlers){
            etk.createSQL(isSqlServer()
                    ? "INSERT INTO etk_data_form_event_handler ( data_form_id, event_type, business_key, name, description, script_object_id ) VALUES ( ( SELECT df.data_form_id FROM etk_data_form df JOIN etk_data_object DO ON do.data_object_id = df.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND df.business_key = :dataFormBusinessKey ) , :event_type, :business_key, :name, :description, :scriptObjectId ) "
                      : "INSERT INTO etk_data_form_event_handler(data_form_event_handler_id, data_form_id, event_type, business_key, name, description, script_object_id) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, (SELECT df.data_form_id FROM etk_data_form df JOIN etk_data_object do ON do.data_object_id = df.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND df.business_key = :dataFormBusinessKey), :event_type, :business_key, :name, :description, :scriptObjectId)")
            //.setParameter("scriptObjectBusinessKey", lookupValueInListOfMaps(scriptObjects, "SCRIPT_ID", dataFormEventHandler.get("SCRIPT_OBJECT_ID"), "BUSINESS_KEY"))
            .setParameter("business_key", dataFormEventHandler.get("BUSINESS_KEY"))
            .setParameter("description", dataFormEventHandler.get("DESCRIPTION"))
            .setParameter("name", dataFormEventHandler.get("NAME"))
            .setParameter("event_type", dataFormEventHandler.get("EVENT_TYPE"))
            .setParameter("dataFormBusinessKey", lookupValueInListOfMaps(dataForms, "DATA_FORM_ID", dataFormEventHandler.get("DATA_FORM_ID"), "BUSINESS_KEY"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .setParameter("scriptObjectId", getMatchingScriptId (dataFormEventHandler.get("SCRIPT_OBJECT_ID")))
            .execute();
        }
    }

    /**
     * Import ETK_DISPLAY_MAPPING.
     *
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void importDisplayMappings() throws ApplicationException{
        final List<Map<String, String>> displayMappings = getTable("ETK_DISPLAY_MAPPING");
        final List<Map<String, String>> doStates = getTable("ETK_DO_STATE");
        final List<Map<String, String>> dataObjects = getTable("ETK_DATA_OBJECT");
        final List<Map<String, String>> dataViews = getTable("ETK_DATA_VIEW");
        final List<Map<String, String>> roles = getTable("ETK_ROLE");
        final List<Map<String, String>> dataForms = getTable("ETK_DATA_FORM");

        for (final Map<String, String> displayMapping : displayMappings){
            etk.createSQL(isSqlServer()
                    ? "INSERT INTO etk_display_mapping (process_data_object_id, data_object_id, data_form_id, data_view_id, role_id, do_state_id, name, use_default_form, use_default_view, disable_form, disable_view, read_only_form, evaluation_order, business_key, description, EVALUATION_SCRIPT_ID) VALUES ((SELECT do.data_object_id FROM etk_data_object do WHERE do.business_key = :processDataObjectBusinessKey AND do.tracking_config_id = :trackingConfigId), (SELECT do.data_object_id FROM etk_data_object do WHERE do.business_key = :dataObjectBusinessKey AND do.tracking_config_id = :trackingConfigId), (SELECT df.data_form_id FROM etk_data_form df JOIN etk_data_object do ON do.data_object_id = df.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND df.business_key = :dataFormBusinessKey), (SELECT dv.data_view_id FROM etk_data_view dv JOIN etk_data_object do ON do.data_object_id = dv.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND dv.business_key = :dataViewBusinessKey), (SELECT r.role_id FROM etk_role r WHERE business_key = :roleBusinessKey), (SELECT doState.do_state_id FROM etk_do_state doState JOIN etk_data_object do ON do.data_object_id = doState.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND doState.business_key = :doStateBusinessKey), :name, :use_default_form, :use_default_view, :disable_form, :disable_view, :read_only_form, :evaluation_order, :business_key, :description, :evaluationScriptId)"
                      : "INSERT INTO etk_display_mapping (form_mapping_id, process_data_object_id, data_object_id, data_form_id, data_view_id, role_id, do_state_id, name, use_default_form, use_default_view, disable_form, disable_view, read_only_form, evaluation_order, business_key, description, EVALUATION_SCRIPT_ID) VALUES (HIBERNATE_SEQUENCE.NEXTVAL, (SELECT do.data_object_id FROM etk_data_object do WHERE do.business_key = :processDataObjectBusinessKey AND do.tracking_config_id = :trackingConfigId), (SELECT do.data_object_id FROM etk_data_object do WHERE do.business_key = :dataObjectBusinessKey AND do.tracking_config_id = :trackingConfigId), (SELECT df.data_form_id FROM etk_data_form df JOIN etk_data_object do ON do.data_object_id = df.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND df.business_key = :dataFormBusinessKey), (SELECT dv.data_view_id FROM etk_data_view dv JOIN etk_data_object do ON do.data_object_id = dv.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND dv.business_key = :dataViewBusinessKey), (SELECT r.role_id FROM etk_role r WHERE business_key = :roleBusinessKey), (SELECT doState.do_state_id FROM etk_do_state doState JOIN etk_data_object do ON do.data_object_id = doState.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND doState.business_key = :doStateBusinessKey), :name, :use_default_form, :use_default_view, :disable_form, :disable_view, :read_only_form, :evaluation_order, :business_key, :description, :evaluationScriptId)")
            //.setParameter("evaluation_code", displayMapping.get("EVALUATION_CODE"))
            .setParameter("disable_view", displayMapping.get("DISABLE_VIEW"))
            .setParameter("doStateBusinessKey", lookupValueInListOfMaps(doStates, "DO_STATE_ID", displayMapping.get("DO_STATE_ID"), "BUSINESS_KEY"))
            .setParameter("use_default_form", displayMapping.get("USE_DEFAULT_FORM"))
            .setParameter("processDataObjectBusinessKey", lookupValueInListOfMaps(dataObjects, "DATA_OBJECT_ID", displayMapping.get("PROCESS_DATA_OBJECT_ID"), "BUSINESS_KEY"))
            .setParameter("business_key", displayMapping.get("BUSINESS_KEY"))
            .setParameter("read_only_form", displayMapping.get("READ_ONLY_FORM"))
            .setParameter("dataViewBusinessKey", lookupValueInListOfMaps(dataViews, "DATA_VIEW_ID", displayMapping.get("DATA_VIEW_ID"), "BUSINESS_KEY"))
            .setParameter("use_default_view", displayMapping.get("USE_DEFAULT_VIEW"))
            .setParameter("description", displayMapping.get("DESCRIPTION"))
            .setParameter("dataObjectBusinessKey", lookupValueInListOfMaps(dataObjects, "DATA_OBJECT_ID", displayMapping.get("DATA_OBJECT_ID"), "BUSINESS_KEY"))
            .setParameter("name", displayMapping.get("NAME"))
            .setParameter("evaluation_order", displayMapping.get("EVALUATION_ORDER"))
            .setParameter("disable_form", displayMapping.get("DISABLE_FORM"))
            .setParameter("roleBusinessKey", lookupValueInListOfMaps(roles, "ETK_ROLE", displayMapping.get("ROLE_ID"), "BUSINESS_KEY"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .setParameter("dataFormBusinessKey", lookupValueInListOfMaps(dataForms, "DATA_FORM_ID", displayMapping.get("DATA_FORM_ID"), "BUSINESS_KEY"))
            .setParameter("evaluationScriptId", getMatchingScriptId (displayMapping.get("EVALUATION_SCRIPT_ID")))
            .execute();
        }
    }

    /**
     * Import ETK_DATA_EVENT_LISTENER.
     *
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void importDataEventListeners() throws ApplicationException{
        final List<Map<String, String>> dataEventListeners = getTable("ETK_DATA_EVENT_LISTENER");
        final List<Map<String, String>> dataObjects = getTable("ETK_DATA_OBJECT");
        for(final Map<String, String> dataEventListener : dataEventListeners){
            etk.createSQL(isSqlServer()
                    ? "INSERT INTO etk_data_event_listener(data_object_id, data_event, name, business_key, description, script_object_id) VALUES((SELECT do.data_object_id FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey), :data_event, :the_name, :business_key, :description, :script_object_id)"
                      : "INSERT INTO etk_data_event_listener(listener_id, data_object_id, data_event, name, business_key, description, script_object_id) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, (SELECT do.data_object_id FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey), :data_event, :the_name, :business_key, :description, :script_object_id)")
            .setParameter("script_object_id", getMatchingScriptId(dataEventListener.get("SCRIPT_OBJECT_ID")))
            .setParameter("dataObjectBusinessKey", lookupValueInListOfMaps(dataObjects, "DATA_OBJECT_ID", dataEventListener.get("DATA_OBJECT_ID"), "BUSINESS_KEY"))
            .setParameter("business_key", dataEventListener.get("BUSINESS_KEY"))
            .setParameter("description", dataEventListener.get("DESCRIPTION"))
            .setParameter("data_event", dataEventListener.get("DATA_EVENT"))
            .setParameter("the_name", dataEventListener.get("NAME"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .execute();
        }
    }

    /**
     * Import ETK_FILTER_HANDLER.
     *
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void importFilterHandlers() throws ApplicationException{
        final List<Map<String, String>> filterHandlers = getTable("ETK_FILTER_HANDLER");
        final List<Map<String, String>> dataObjects = getTable("ETK_DATA_OBJECT");
        for(final Map<String, String> filterHandler : filterHandlers){
            etk.createSQL(isSqlServer()
                    ? "INSERT INTO etk_filter_handler(data_object_id, handler_type, name, business_key, description, script_object_id) VALUES((SELECT do.data_object_id FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey), :handler_type, :the_name, :business_key, :description, :script_object_id)"
                      : "INSERT INTO etk_filter_handler(handler_id, data_object_id, handler_type, name, business_key, description, script_object_id) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, (SELECT do.data_object_id FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey), :handler_type, :the_name, :business_key, :description, :script_object_id)")
            .setParameter("script_object_id", getMatchingScriptId(filterHandler.get("SCRIPT_OBJECT_ID")))
            .setParameter("dataObjectBusinessKey", lookupValueInListOfMaps(dataObjects, "DATA_OBJECT_ID", filterHandler.get("DATA_OBJECT_ID"), "BUSINESS_KEY"))
            .setParameter("business_key", filterHandler.get("BUSINESS_KEY"))
            .setParameter("description", filterHandler.get("DESCRIPTION"))
            .setParameter("handler_type", filterHandler.get("HANDLER_TYPE"))
            .setParameter("the_name", filterHandler.get("NAME"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .execute();
        }
    }

    /**
     * Import ETK_FORM_CTL_LABEL_BINDING.
     */
    private void importFormControlLabelBindings() {
        final List<Map<String, String>> formControlLabelBindings = getTable("ETK_FORM_CTL_LABEL_BINDING");
        final List<Map<String, String>> formControls = getTable("ETK_FORM_CONTROL");
        for (final Map<String, String> formControlLabelBinding : formControlLabelBindings){
            etk.createSQL("INSERT INTO etk_form_ctl_label_binding(form_control_id, label_control_id) VALUES((SELECT formControl.form_control_id FROM etk_form_control formControl JOIN etk_data_form dataForm ON dataForm.data_form_id = formControl.data_form_id JOIN etk_data_object dataObject ON dataObject.data_object_id = dataForm.data_object_id WHERE formControl.business_key = :formControlBusinessKey AND dataObject.tracking_config_id = :trackingConfigId), (SELECT formControl.form_control_id FROM etk_form_control formControl JOIN etk_data_form dataForm ON dataForm.data_form_id = formControl.data_form_id JOIN etk_data_object dataObject ON dataObject.data_object_id = dataForm.data_object_id WHERE formControl.business_key = :labelBusinessKey AND dataObject.tracking_config_id = :trackingConfigId))")
            .setParameter("formControlBusinessKey", lookupValueInListOfMaps(formControls, "FORM_CONTROL_ID", formControlLabelBinding.get("FORM_CONTROL_ID"), "BUSINESS_KEY"))
            .setParameter("labelBusinessKey", lookupValueInListOfMaps(formControls, "FORM_CONTROL_ID", formControlLabelBinding.get("LABEL_CONTROL_ID"), "BUSINESS_KEY"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .execute();
        }
    }

    /**
     * Import ETK_FORM_CTL_LOOKUP_BINDING.
     */
    private void importFormControlLookupBindings() {
        final List<Map<String, String>> formControlLookupBindings = getTable("ETK_FORM_CTL_LOOKUP_BINDING");
        final List<Map<String, String>> formControls = getTable("ETK_FORM_CONTROL");
        final List<Map<String, String>> lookups = getTable("ETK_LOOKUP_DEFINITION");
        for (final Map<String, String> formControlLookupBinding : formControlLookupBindings){
            etk.getLogger().error("***Lookup: "+ formControlLookupBinding.get("LOOKUP_DEFINITION_ID"));
            etk.createSQL("INSERT INTO etk_form_ctl_lookup_binding(form_control_id, lookup_definition_id) VALUES((SELECT formControl.form_control_id FROM etk_form_control formControl JOIN etk_data_form dataForm ON dataForm.data_form_id = formControl.data_form_id JOIN etk_data_object dataObject ON dataObject.data_object_id = dataForm.data_object_id WHERE formControl.business_key = :formControlBusinessKey AND dataObject.tracking_config_id = :trackingConfigId), (SELECT ld.lookup_definition_id FROM etk_lookup_definition ld WHERE ld.tracking_config_id = :trackingConfigId AND ld.business_key = :lookupBusinessKey))")
            .setParameter("formControlBusinessKey", lookupValueInListOfMaps(formControls, "FORM_CONTROL_ID", formControlLookupBinding.get("FORM_CONTROL_ID"), "BUSINESS_KEY"))
            .setParameter("lookupBusinessKey", lookupValueInListOfMaps(lookups, "LOOKUP_DEFINITION_ID", formControlLookupBinding.get("LOOKUP_DEFINITION_ID"), "BUSINESS_KEY"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .execute();
        }
    }

    /**
     * Import ETK_FORM_CONTROL_EVENT_HANDLER.
     *
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void importFormControlEventHandlers() throws ApplicationException{
        final List<Map<String, String>> formControlEventHandlers = getTable("ETK_FORM_CONTROL_EVENT_HANDLER");
        final List<Map<String, String>> formControls = getTable("ETK_FORM_CONTROL");
        for (final Map<String, String> formControlEventHandler : formControlEventHandlers){
            etk.createSQL(
                    isSqlServer() ? "INSERT INTO etk_form_control_event_handler(form_control_id, event_type, business_key, name, description, SCRIPT_OBJECT_ID) VALUES((SELECT formControl.form_control_id FROM etk_form_control formControl JOIN etk_data_form dataForm ON dataForm.data_form_id = formControl.data_form_id JOIN etk_data_object dataObject ON dataObject.data_object_id = dataForm.data_object_id WHERE formControl.business_key = :formControlBusinessKey AND dataObject.tracking_config_id = :trackingConfigId), :event_type, :business_key, :name, :description, :scriptObjectId)"
                                    : "INSERT INTO etk_form_control_event_handler(form_control_event_handler_id, form_control_id, event_type, business_key, name, description, SCRIPT_OBJECT_ID) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, (SELECT formControl.form_control_id FROM etk_form_control formControl JOIN etk_data_form dataForm ON dataForm.data_form_id = formControl.data_form_id JOIN etk_data_object dataObject ON dataObject.data_object_id = dataForm.data_object_id WHERE formControl.business_key = :formControlBusinessKey AND dataObject.tracking_config_id = :trackingConfigId), :event_type, :business_key, :name, :description, :scriptObjectId)")
            //.setParameter("scriptObjectBusinessKey", lookupValueInListOfMaps(scriptObjects, "SCRIPT_ID", formControlEventHandler.get("SCRIPT_OBJECT_ID"), "BUSINESS_KEY"))
            .setParameter("business_key", formControlEventHandler.get("BUSINESS_KEY"))
            .setParameter("formControlBusinessKey", lookupValueInListOfMaps(formControls, "FORM_CONTROL_ID", formControlEventHandler.get("FORM_CONTROL_ID"), "BUSINESS_KEY"))
            .setParameter("description", formControlEventHandler.get("DESCRIPTION"))
            .setParameter("name", formControlEventHandler.get("NAME"))
            .setParameter("event_type", formControlEventHandler.get("EVENT_TYPE"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .setParameter("scriptObjectId", getMatchingScriptId (formControlEventHandler.get("SCRIPT_OBJECT_ID")))
            .execute();
        }
    }

    /**
     * Import ETK_DATA_FORM.
     *
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void importDataForms() throws ApplicationException{
        final List<Map<String, String>> dataForms = getTable("ETK_DATA_FORM");
        final List<Map<String, String>> dataObjects = getTable("ETK_DATA_OBJECT");
        for(final Map<String, String> dataForm : dataForms){
            etk.getLogger().error("Inserting Form " + dataForm.get("BUSINESS_KEY"));

            etk.createSQL(isSqlServer()
                    ? "INSERT INTO etk_data_form ( data_object_id, title, instructions, default_form, enable_spell_checker, search_form, layout_type, business_key, name, description, script_object_id, OFFLINE_FORM ) VALUES ( ( SELECT do.data_object_id FROM etk_data_object DO WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey ) , :title, :instructions, :default_form, :enable_spell_checker, :search_form, :layout_type, :business_key, :name, :description, :scriptObjectId, :offlineForm )"
                      : "INSERT INTO etk_data_form(data_form_id, data_object_id, title, instructions, default_form, enable_spell_checker, search_form, layout_type, business_key, name, description, script_object_id, OFFLINE_FORM) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, (SELECT do.data_object_id FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey), :title, :instructions, :default_form, :enable_spell_checker, :search_form, :layout_type, :business_key, :name, :description, :scriptObjectId, :offlineForm)")
            .setParameter("title", dataForm.get("TITLE"))
            .setParameter("business_key", dataForm.get("BUSINESS_KEY"))
            .setParameter("instructions", dataForm.get("INSTRUCTIONS"))
            .setParameter("layout_type", dataForm.get("LAYOUT_TYPE"))
            .setParameter("description", dataForm.get("DESCRIPTION"))
            .setParameter("dataObjectBusinessKey", lookupValueInListOfMaps(dataObjects, "DATA_OBJECT_ID", dataForm.get("DATA_OBJECT_ID"), "BUSINESS_KEY"))
            .setParameter("name", dataForm.get("NAME"))
            .setParameter("search_form", dataForm.get("SEARCH_FORM"))
            .setParameter("default_form", dataForm.get("DEFAULT_FORM"))
            .setParameter("enable_spell_checker", dataForm.get("ENABLE_SPELL_CHECKER"))
            .setParameter("offlineForm", dataForm.get("OFFLINE_FORM"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .setParameter("scriptObjectId", getMatchingScriptId(dataForm.get("SCRIPT_OBJECT_ID")))
            .execute();
        }
    }

    /**
     * Import ETK_PAGE_PERMISSION (and associated ETK_SHARED_OBJECT_PERMISSION).
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private void importPagePermissions () throws IncorrectResultSizeDataAccessException {
        final List<Map<String, String>> pagePermissions = getTable("ETK_PAGE_PERMISSIONS");

        final List<Map<String, String>> roles = getTable("ETK_ROLE");

        final SQLFacade sharedPermissionSQL =
                etk.createSQL(
                        isSqlServer() ?
                                       "insert into ETK_SHARED_OBJECT_PERMISSION ("
                                       + "IS_EDIT, "
                                       + "IS_EXECUTE, "
                                       + "IS_DISPLAY, "
                                       + "SUBJECT_ID, "
                                       + "ROLE_ID, "
                                       + "IS_ALL_USERS) values ( "
                                       + ":isEdit, "
                                       + ":isExecute, "
                                       + ":isDisplay, "
                                       + ":subjectId, "
                                       + ":roleId,"
                                       + ":isAllUsers)"
                                       :
                                           "insert into ETK_SHARED_OBJECT_PERMISSION ("
                                           + "SHARED_OBJECT_PERMISSION_ID, "
                                           + "IS_EDIT, "
                                           + "IS_EXECUTE, "
                                           + "IS_DISPLAY, "
                                           + "SUBJECT_ID, "
                                           + "ROLE_ID, "
                                           + "IS_ALL_USERS) values ( "
                                           + ":newPermissionId, "
                                           + ":isEdit, "
                                           + ":isExecute, "
                                           + ":isDisplay, "
                                           + ":subjectId, "
                                           + ":roleId,"
                                           + ":isAllUsers)");

        final SQLFacade pagePermissionSQL =
                etk.createSQL(
                        //isSqlServer() ?
                        //"insert into ETK_PAGE_PERMISSION (PAGE_ID) values(:pageId)"
                        //:
                        "insert into ETK_PAGE_PERMISSION (PAGE_PERMISSION_ID, PAGE_ID) "
                        + "values(:pagePermissionId, :pageId)");

        Number newPermissionId = null;
        Number newRoleId = null;
        Number newPageId = null;
        String oldRoleId = null;
        String roleBusinessKey = null;

        final SQLFacade roleIdQuery =
                etk.createSQL("select role_id from etk_role where business_key = :roleBusinessKey");

        for (final Map<String, String> aPagePermission : pagePermissions) {


            //Match old role by business key to a role with the same business key.
            oldRoleId = aPagePermission.get("ROLE_ID");
            roleBusinessKey = (String)
                    lookupValueInListOfMaps(roles, "ROLE_ID", oldRoleId, "BUSINESS_KEY");
            newRoleId = (Number)
                    roleIdQuery.setParameter("roleBusinessKey", roleBusinessKey)
                    .returnEmptyResultSetAs(null)
                    .fetchObject();

            etk.getLogger().error("FOUND roleBusinessKey " + roleBusinessKey);

            newPageId = pageKeyMap.get(aPagePermission.get("PAGE_ID"));

            //If the old permission contains a role that does not exist in the
            //new system, or the page did not create successfully, continue.
            if ((StringUtility.isNotBlank(oldRoleId) && (newRoleId == null)) ||
                    (newPageId == null)) {
                continue;
            }

            if (isSqlServer()) {
                newPermissionId = sharedPermissionSQL
                        .setParameter("isEdit", aPagePermission.get("IS_EDIT"))
                        .setParameter("isExecute", aPagePermission.get("IS_EXECUTE"))
                        .setParameter("isDisplay", aPagePermission.get("IS_DISPLAY"))
                        .setParameter("subjectId", aPagePermission.get("SUBJECT_ID"))
                        .setParameter("roleId", newRoleId)
                        .setParameter("isAllUsers", aPagePermission.get("IS_ALL_USERS"))
                        .executeForKey("SHARED_OBJECT_PERMISSION_ID");

                pagePermissionSQL
                .setParameter("pagePermissionId", newPermissionId)
                .setParameter("pageId", newPageId)
                .execute();
            } else {
                newPermissionId = getNextOracleHibernateSequence();

                sharedPermissionSQL
                .setParameter("newPermissionId", newPermissionId)
                .setParameter("isEdit", aPagePermission.get("IS_EDIT"))
                .setParameter("isExecute", aPagePermission.get("IS_EXECUTE"))
                .setParameter("isDisplay", aPagePermission.get("IS_DISPLAY"))
                .setParameter("subjectId", aPagePermission.get("SUBJECT_ID"))
                .setParameter("roleId", newRoleId)
                .setParameter("isAllUsers", aPagePermission.get("IS_ALL_USERS"))
                .execute();

                pagePermissionSQL
                .setParameter("pagePermissionId", newPermissionId)
                .setParameter("pageId", newPageId)
                .execute();
            }
        }
    }

    /**
     * Get the User Id to import things such as pages under in the target system. This will be the administrator if it
     * still exists, otherwise it will be the current user.
     *
     * @return the user id
     */
    private long getUserIdToImportThingsUnder() {
        /* We will try to import under administrator (user_id = 1), otherwise we will use the current user */
        final long adminId = 1;

        final boolean adminExists = !etk.createSQL("SELECT user_id FROM etk_user WHERE user_id = :userId")
                .setParameter("userId", adminId)
                .fetchList()
                .isEmpty();

        return adminExists ? adminId : etk.getCurrentUser().getId();
    }

    /**
     * Import ETK_PAGE.
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private void importPages() throws IncorrectResultSizeDataAccessException {

        final List<Map<String, String>> pages = getTable("ETK_PAGE");

        final long userId = getUserIdToImportThingsUnder();

        //Build insert query
        final StringBuilder insertQuery = new StringBuilder();
        insertQuery.append("Insert into ETK_PAGE (");
        if (!isSqlServer()) {
            insertQuery.append("PAGE_ID, ");
        }
        insertQuery.append("BUSINESS_KEY, ");
        insertQuery.append("PUBLIC_PAGE, ");
        insertQuery.append("USER_ID, ");
        insertQuery.append("CREATED_BY, ");
        insertQuery.append("CREATED_ON, ");
        insertQuery.append("LAST_UPDATED_BY, ");
        insertQuery.append("LAST_UPDATED_ON, ");
        insertQuery.append("DESCRIPTION, ");
        insertQuery.append("NAME, ");
        insertQuery.append("COMPONENT_PAGE, ");
        insertQuery.append("CONTROLLER_SCRIPT_ID, ");
        insertQuery.append("VIEW_SCRIPT_ID) values (");
        if (!isSqlServer()) {
            insertQuery.append("hibernate_sequence.nextval, ");
        }
        insertQuery.append(":businessKey, ");
        insertQuery.append(":publicPage, ");
        insertQuery.append(":userId, ");
        insertQuery.append("'administrator (Imported)', ");
        if (isSqlServer()) {
            insertQuery.append("dbo.ETKF_getServerTime(), ");
        } else {
            insertQuery.append("ETKF_GETSERVERTIME(), ");
        }
        insertQuery.append("'administrator (Imported)', ");
        if (isSqlServer()) {
            insertQuery.append("dbo.ETKF_getServerTime(), ");
        } else {
            insertQuery.append("ETKF_GETSERVERTIME(), ");
        }
        insertQuery.append(":description, ");
        insertQuery.append(":name, ");
        insertQuery.append(":componentPage, ");
        insertQuery.append(":controllerScriptId, ");
        insertQuery.append(":viewScriptId)");


        //Build updateQuery
        final StringBuilder updateQuery = new StringBuilder();
        updateQuery.append("UPDATE ETK_PAGE set ");
        updateQuery.append("PUBLIC_PAGE = :publicPage, ");
        updateQuery.append("LAST_UPDATED_BY = 'administrator (Imported)', ");
        if (isSqlServer()) {
            updateQuery.append("LAST_UPDATED_ON = dbo.ETKF_getServerTime(), ");
        } else {
            updateQuery.append("LAST_UPDATED_ON = ETKF_GETSERVERTIME(), ");
        }
        updateQuery.append("DESCRIPTION = :description, ");
        updateQuery.append("NAME = :name, ");
        updateQuery.append("COMPONENT_PAGE = :componentPage, ");
        updateQuery.append("CONTROLLER_SCRIPT_ID = :controllerScriptId, ");
        updateQuery.append("VIEW_SCRIPT_ID = :viewScriptId ");
        updateQuery.append("WHERE BUSINESS_KEY = :businessKey");

        //Bind queries for use within the for loop.
        final SQLFacade boundInsertQuery = etk.createSQL(insertQuery.toString());
        final SQLFacade boundUpdateQuery = etk.createSQL(updateQuery.toString());
        final SQLFacade pageIdQuery = etk.createSQL("select PAGE_ID from etk_page where BUSINESS_KEY = :businessKey");

        for (final Map<String, String> aPage : pages) {

            Number pageId = (Number) pageIdQuery
                    .returnEmptyResultSetAs(null)
                    .setParameter("businessKey", aPage.get("BUSINESS_KEY"))
                    .fetchObject();

            //If the page does not exist, insert it and get the new page id. If it does exist, update it.
            if (pageId == null) {
                etk.getLogger().error("Creating new page with business key " + aPage.get("BUSINESS_KEY"));

                boundInsertQuery.setParameter("businessKey", aPage.get("BUSINESS_KEY"))
                .setParameter("publicPage", aPage.get("PUBLIC_PAGE"))
                .setParameter("userId", userId)
                .setParameter("description", aPage.get("DESCRIPTION"))
                .setParameter("name", aPage.get("NAME"))
                .setParameter("componentPage", aPage.get("COMPONENT_PAGE"))
                .setParameter("controllerScriptId", oldNewScriptIdMap.get(aPage.get("CONTROLLER_SCRIPT_ID")))
                .setParameter("viewScriptId", oldNewScriptIdMap.get(aPage.get("VIEW_SCRIPT_ID")))
                .execute();

                pageId = (Number)
                        pageIdQuery
                        .setParameter("businessKey", aPage.get("BUSINESS_KEY"))
                        .fetchObject();
            } else {
                etk.getLogger().error("Updating existing page with business key " + aPage.get("BUSINESS_KEY"));

                boundUpdateQuery.setParameter("businessKey", aPage.get("BUSINESS_KEY"))
                .setParameter("publicPage", aPage.get("PUBLIC_PAGE"))
                .setParameter("userId", userId)
                .setParameter("description", aPage.get("DESCRIPTION"))
                .setParameter("name", aPage.get("NAME"))
                .setParameter("componentPage", aPage.get("COMPONENT_PAGE"))
                .setParameter("controllerScriptId", oldNewScriptIdMap.get(aPage.get("CONTROLLER_SCRIPT_ID")))
                .setParameter("viewScriptId", oldNewScriptIdMap.get(aPage.get("VIEW_SCRIPT_ID")))
                .execute();
            }

            pageKeyMap.put(aPage.get("PAGE_ID"), pageId);
        }
    }

    /**
     * Import ETK_DATA_VIEW_ELEMENT.
     */
    private void importDataViewElement() {
        final List<Map<String, String>> dataViewElements = getTable("ETK_DATA_VIEW_ELEMENT");
        final List<Map<String, String>> dataElements = getTable("ETK_DATA_ELEMENT");
        final List<Map<String, String>> dataViews = getTable("ETK_DATA_VIEW");
        for(final Map<String, String> dataViewElement : dataViewElements){
            etk.createSQL(isSqlServer()
                    ? "INSERT INTO etk_data_view_element (data_view_id, name, data_element_id, display_order, label, display_size, multi_value_delimiter, responsiveness_factor, business_key, description) VALUES((SELECT dataView.data_view_id FROM etk_data_view dataView JOIN etk_data_object dataObject ON dataObject.data_object_id = dataView.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId AND dataView.business_key = :dataViewBusinessKey), :name, (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object do ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :dataElementBusinessKey), :display_order, :label, :display_size, :multi_value_delimiter, :responsiveness_factor, :business_key, :description)"
                      : "INSERT INTO etk_data_view_element (data_view_element_id, data_view_id, name, data_element_id, display_order, label, display_size, multi_value_delimiter, responsiveness_factor, business_key, description) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, (SELECT dataView.data_view_id FROM etk_data_view dataView JOIN etk_data_object dataObject ON dataObject.data_object_id = dataView.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId AND dataView.business_key = :dataViewBusinessKey), :name, (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object do ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :dataElementBusinessKey), :display_order, :label, :display_size, :multi_value_delimiter, :responsiveness_factor, :business_key, :description)")
            .setParameter("business_key", dataViewElement.get("BUSINESS_KEY"))
            .setParameter("dataElementBusinessKey", lookupValueInListOfMaps(dataElements, "DATA_ELEMENT_ID", dataViewElement.get("DATA_ELEMENT_ID"), "BUSINESS_KEY"))
            .setParameter("dataViewBusinessKey", lookupValueInListOfMaps(dataViews, "DATA_VIEW_ID", dataViewElement.get("DATA_VIEW_ID"), "BUSINESS_KEY"))
            .setParameter("multi_value_delimiter", dataViewElement.get("MULTI_VALUE_DELIMITER"))
            .setParameter("display_size", dataViewElement.get("DISPLAY_SIZE"))
            .setParameter("description", dataViewElement.get("DESCRIPTION"))
            .setParameter("name", dataViewElement.get("NAME"))
            .setParameter("label", dataViewElement.get("LABEL"))
            .setParameter("display_order", dataViewElement.get("DISPLAY_ORDER"))
            .setParameter("responsiveness_factor", dataViewElement.get("RESPONSIVENESS_FACTOR"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .execute();
        }
    }

    /**
     * Import ETK_FORM_CONTROL.
     */
    private void importFormControls() {
        final List<Map<String, String>> formControls = getTable("ETK_FORM_CONTROL");
        final List<Map<String, String>> dataForms = getTable("ETK_DATA_FORM");
        for(final Map<String, String> formControl : formControls){
            etk.createSQL(isSqlServer()
                    ? "INSERT INTO etk_form_control ( form_control_type, name, data_form_id, display_order, label, read_only, height, width, x, y, business_key, description, tooltip_text, mutable_read_only) VALUES ( :form_control_type, :name, ( SELECT dataForm.data_form_id FROM etk_data_form dataForm JOIN etk_data_object DO ON do.data_object_id = dataForm.data_object_id WHERE dataForm.business_key = :dataFormBusinessKey AND do.tracking_config_id = :trackingConfigId ) , :display_order, :label, :read_only, :height, :width, :x, :y, :business_key, :description, :tooltip_text, :mutable_read_only )"
                      : "INSERT INTO etk_form_control(form_control_id, form_control_type, name, data_form_id, display_order, label, read_only, height, width, x, y, business_key, description, tooltip_text, mutable_read_only) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, :form_control_type, :name, (SELECT dataForm.data_form_id FROM etk_data_form dataForm JOIN etk_data_object do ON do.data_object_id = dataForm.data_object_id WHERE dataForm.business_key = :dataFormBusinessKey AND do.tracking_config_id = :trackingConfigId), :display_order, :label, :read_only, :height, :width, :x, :y, :business_key, :description, :tooltip_text, :mutable_read_only)")
            .setParameter("read_only", formControl.get("READ_ONLY"))
            .setParameter("width", formControl.get("WIDTH"))
            .setParameter("label", formControl.get("LABEL"))
            .setParameter("display_order", formControl.get("DISPLAY_ORDER"))
            .setParameter("tooltip_text", formControl.get("TOOLTIP_TEXT"))
            .setParameter("form_control_type", formControl.get("FORM_CONTROL_TYPE"))
            .setParameter("height", formControl.get("HEIGHT"))
            .setParameter("business_key", formControl.get("BUSINESS_KEY"))
            .setParameter("description", formControl.get("DESCRIPTION"))
            .setParameter("name", formControl.get("NAME"))
            .setParameter("y", formControl.get("Y"))
            .setParameter("dataFormBusinessKey", lookupValueInListOfMaps(dataForms, "DATA_FORM_ID", formControl.get("DATA_FORM_ID"), "BUSINESS_KEY"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .setParameter("x", formControl.get("X"))
            .setParameter("mutable_read_only", formControl.get("MUTABLE_READ_ONLY"))
            .execute();
        }
    }

    /**
     * Import ETK_FORM_CTL_ELEMENT_BINDING.
     */
    private void importFormControlElementBindings() {
        final List<Map<String, String>> formControlElementBindings = getTable("ETK_FORM_CTL_ELEMENT_BINDING");
        final List<Map<String, String>> dataElements = getTable("ETK_DATA_ELEMENT");
        final List<Map<String, String>> formControls = getTable("ETK_FORM_CONTROL");
        for(final Map<String, String> formControlElementBinding : formControlElementBindings){
            etk.createSQL("INSERT INTO etk_form_ctl_element_binding(form_control_id, data_element_id) VALUES((SELECT formControl.form_control_id FROM etk_form_control formControl JOIN etk_data_form dataForm ON dataForm.data_form_id = formControl.data_form_id JOIN etk_data_object dataObject ON dataObject.data_object_id = dataForm.data_object_id WHERE formControl.business_key = :formControlBusinessKey AND dataObject.tracking_config_id = :trackingConfigId), (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object do ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :dataElementBusinessKey))")
            .setParameter("dataElementBusinessKey", lookupValueInListOfMaps(dataElements, "DATA_ELEMENT_ID", formControlElementBinding.get("DATA_ELEMENT_ID"), "BUSINESS_KEY"))
            .setParameter("formControlBusinessKey", lookupValueInListOfMaps(formControls, "FORM_CONTROL_ID", formControlElementBinding.get("FORM_CONTROL_ID"), "BUSINESS_KEY"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .execute();
        }
    }

    /**
     * Import ETK_DATA_ELEMENT and ETK_LOOKUP_DEFINITION records. The reason these two must be done together is because
     * there is a cyclical dependency between lookups and data objects/elements due to 'Data Object Type' lookups.
     *
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void importDataElementsAndLookups() throws ApplicationException {
        /* Data Elements and lookups are done together because they have circular dependencies (which we'll do our best to (but not perfectly) handle)
         * We'll do it in 3 phases
            1) make sure each element gets into the table
            2) make sure each lookup gets into the table
            3) update all the elements to reference the lookup that they are tied to */
        final List<Map<String, String>> dataElements = getTable("ETK_DATA_ELEMENT");
        final List<Map<String, String>> lookups = getTable("ETK_LOOKUP_DEFINITION");
        final List<Map<String, String>> dataObjects = getTable("ETK_DATA_OBJECT");
        final List<Map<String, String>> pluginRegistrations = getTable("ETK_PLUGIN_REGISTRATION");

        /* 1) make sure each dataElement gets into the table */
        for(final Map<String, String> dataElement : dataElements){
            final List<Map<String, Object>> matchingDataElements = etk.createSQL("SELECT de.DATA_ELEMENT_ID FROM etk_data_element de JOIN etk_data_object do ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :dataElementBusinessKey")
                    .setParameter("trackingConfigId", nextTrackingConfigId)
                    .setParameter("dataElementBusinessKey", dataElement.get("BUSINESS_KEY"))
                    .fetchList();/* DATA_ELEMENT_ID */
            if(matchingDataElements.size() == 0){
                //Insert
                etk.createSQL(isSqlServer()
                        ? "INSERT INTO etk_data_element ( data_object_id, name, data_type, required, validation_required, column_name, primary_key, system_field, index_type, default_value, searchable, is_unique, data_size, bound_to_lookup, lookup_definition_id, element_name, default_to_today, future_dates_allowed, identifier, logged, plugin_registration_id, applied_changes, table_name, business_key, description, stored_in_document_management, used_for_escan ) VALUES ( ( SELECT do.data_object_id FROM etk_data_object DO WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey ) , :name, :data_type, :required, :validation_required, :column_name, :primary_key, :system_field, :index_type, :default_value, :searchable, :is_unique, :data_size, :bound_to_lookup, NULL, :element_name, :default_to_today, :future_dates_allowed, :identifier, :logged, (SELECT plugin_registration_id FROM etk_plugin_registration WHERE business_key = :pluginRegistrationBusinessKey AND tracking_config_id = :trackingConfigId), 0, :table_name, :business_key, :description, :stored_in_document_management, :usedForEscan )"
                          : "INSERT INTO etk_data_element(data_element_id, data_object_id, name, data_type, required, validation_required, column_name, primary_key, system_field, index_type, default_value, searchable, is_unique, data_size, bound_to_lookup, lookup_definition_id, element_name, default_to_today, future_dates_allowed, identifier, logged, plugin_registration_id, applied_changes, table_name, business_key, description, stored_in_document_management, used_for_escan) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, (SELECT do.data_object_id FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey), :name, :data_type, :required, :validation_required, :column_name, :primary_key, :system_field, :index_type, :default_value, :searchable, :is_unique, :data_size, :bound_to_lookup, NULL, :element_name, :default_to_today, :future_dates_allowed, :identifier, :logged, (SELECT plugin_registration_id FROM etk_plugin_registration WHERE business_key = :pluginRegistrationBusinessKey AND tracking_config_id = :trackingConfigId), 0, :table_name, :business_key, :description, :stored_in_document_management, :usedForEscan)")
                .setParameter("dataObjectBusinessKey", lookupValueInListOfMaps(dataObjects, "DATA_OBJECT_ID", dataElement.get("DATA_OBJECT_ID"), "BUSINESS_KEY"))
                .setParameter("name", dataElement.get("NAME"))
                .setParameter("data_type", dataElement.get("DATA_TYPE"))
                .setParameter("required", dataElement.get("REQUIRED"))
                .setParameter("validation_required", dataElement.get("VALIDATION_REQUIRED"))
                .setParameter("column_name", dataElement.get("COLUMN_NAME"))
                .setParameter("primary_key", dataElement.get("PRIMARY_KEY"))
                .setParameter("system_field", dataElement.get("SYSTEM_FIELD"))
                .setParameter("index_type", dataElement.get("INDEX_TYPE"))
                .setParameter("default_value", dataElement.get("DEFAULT_VALUE"))
                .setParameter("searchable", dataElement.get("SEARCHABLE"))
                .setParameter("is_unique", dataElement.get("IS_UNIQUE"))
                .setParameter("data_size", dataElement.get("DATA_SIZE"))
                .setParameter("bound_to_lookup", dataElement.get("BOUND_TO_LOOKUP"))
                .setParameter("element_name", dataElement.get("ELEMENT_NAME"))
                .setParameter("default_to_today", dataElement.get("DEFAULT_TO_TODAY"))
                .setParameter("future_dates_allowed", dataElement.get("FUTURE_DATES_ALLOWED"))
                .setParameter("identifier", dataElement.get("IDENTIFIER"))
                .setParameter("logged", dataElement.get("LOGGED"))
                .setParameter("pluginRegistrationBusinessKey", lookupValueInListOfMaps(pluginRegistrations, "PLUGIN_REGISTRATION_ID", dataElement.get("PLUGIN_REGISTRATION_ID"), "BUSINESS_KEY"))
                .setParameter("table_name", dataElement.get("TABLE_NAME"))
                .setParameter("business_key", dataElement.get("BUSINESS_KEY"))
                .setParameter("description", dataElement.get("DESCRIPTION"))
                .setParameter("stored_in_document_management", dataElement.get("STORED_IN_DOCUMENT_MANAGEMENT"))
                .setParameter("usedForEscan", dataElement.get("USED_FOR_ESCAN"))
                .setParameter("trackingConfigId", nextTrackingConfigId)
                .execute();
            }else{
                //Update
                etk.createSQL("UPDATE etk_data_element SET data_object_id = (SELECT do.data_object_id FROM etk_data_object DO WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey), name = :name, data_type = :data_type, required = :required, validation_required = :validation_required, column_name = :column_name, primary_key = :primary_key, system_field = :system_field, index_type = :index_type, default_value = :default_value, searchable = :searchable, is_unique = :is_unique, data_size = :data_size, bound_to_lookup = :bound_to_lookup, lookup_definition_id = NULL, element_name = :element_name, default_to_today = :default_to_today, future_dates_allowed = :future_dates_allowed, identifier = :identifier, logged = :logged, plugin_registration_id = (SELECT plugin_registration_id FROM etk_plugin_registration WHERE business_key = :pluginRegistrationBusinessKey AND tracking_config_id = :trackingConfigId), applied_changes = 0, table_name = :table_name, business_key = :business_key, description = :description, stored_in_document_management = :stored_in_document_management, used_for_escan = :usedForEscan WHERE data_element_id = :dataElementId")
                .setParameter("dataObjectBusinessKey", lookupValueInListOfMaps(dataObjects, "DATA_OBJECT_ID", dataElement.get("DATA_OBJECT_ID"), "BUSINESS_KEY"))
                .setParameter("name", dataElement.get("NAME"))
                .setParameter("data_type", dataElement.get("DATA_TYPE"))
                .setParameter("required", dataElement.get("REQUIRED"))
                .setParameter("validation_required", dataElement.get("VALIDATION_REQUIRED"))
                .setParameter("column_name", dataElement.get("COLUMN_NAME"))
                .setParameter("primary_key", dataElement.get("PRIMARY_KEY"))
                .setParameter("system_field", dataElement.get("SYSTEM_FIELD"))
                .setParameter("index_type", dataElement.get("INDEX_TYPE"))
                .setParameter("default_value", dataElement.get("DEFAULT_VALUE"))
                .setParameter("searchable", dataElement.get("SEARCHABLE"))
                .setParameter("is_unique", dataElement.get("IS_UNIQUE"))
                .setParameter("data_size", dataElement.get("DATA_SIZE"))
                .setParameter("bound_to_lookup", dataElement.get("BOUND_TO_LOOKUP"))
                .setParameter("element_name", dataElement.get("ELEMENT_NAME"))
                .setParameter("default_to_today", dataElement.get("DEFAULT_TO_TODAY"))
                .setParameter("future_dates_allowed", dataElement.get("FUTURE_DATES_ALLOWED"))
                .setParameter("identifier", dataElement.get("IDENTIFIER"))
                .setParameter("logged", dataElement.get("LOGGED"))
                .setParameter("pluginRegistrationBusinessKey", lookupValueInListOfMaps(pluginRegistrations, "PLUGIN_REGISTRATION_ID", dataElement.get("PLUGIN_REGISTRATION_ID"), "BUSINESS_KEY"))
                .setParameter("table_name", dataElement.get("TABLE_NAME"))
                .setParameter("business_key", dataElement.get("BUSINESS_KEY"))
                .setParameter("description", dataElement.get("DESCRIPTION"))
                .setParameter("stored_in_document_management", dataElement.get("STORED_IN_DOCUMENT_MANAGEMENT"))
                .setParameter("usedForEscan", dataElement.get("USED_FOR_ESCAN"))
                .setParameter("trackingConfigId", nextTrackingConfigId)
                .setParameter("dataElementId", matchingDataElements.get(0).get("DATA_ELEMENT_ID"))
                .execute();
            }
        }

        /* 2) make sure each lookup gets into the table */
        for(final Map<String, String> lookup : lookups){
            final List<Map<String, Object>> matchingLookups = etk.createSQL("SELECT LOOKUP_DEFINITION_ID FROM etk_lookup_definition WHERE tracking_config_id = :trackingConfigId AND business_key = :lookupBusinessKey")
                    .setParameter("trackingConfigId", nextTrackingConfigId)
                    .setParameter("lookupBusinessKey", lookup.get("BUSINESS_KEY"))
                    .fetchList(); /* LOOKUP_DEFINITION_ID */

            final Number matchingScriptId = getMatchingScriptId(lookup.get("SQL_SCRIPT_OBJECT_ID"));

            if(matchingLookups.size() == 0){
                //Insert
                etk.createSQL(isSqlServer()
                        ? "INSERT INTO etk_lookup_definition ( lookup_source_type, data_object_id, value_element_id, display_element_id, order_by_element_id, ascending_order, start_date_element_id, end_date_element_id, SQL_SCRIPT_OBJECT_ID, plugin_registration_id, value_return_type, tracking_config_id, business_key, name, description, system_object_type, system_object_display_format, enable_caching ) VALUES ( :lookupSourceType, ( SELECT do.data_object_id FROM etk_data_object DO WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey ) , ( SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object DO ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :valueElementBusinessKey ) , ( SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object DO ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :displayElementBusinessKey ) , ( SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object DO ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :orderByElementBusinessKey ) , :ascending_order, ( SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object DO ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :startDateElementBusinessKey ) , ( SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object DO ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :endDateElementBusinessKey ) , :sql_script_object_id, :plugin_registration_id, :value_return_type, :trackingConfigId, :business_key, :name, :description, :system_object_type, :system_object_display_format, :enable_caching )"
                          : "INSERT INTO etk_lookup_definition(lookup_definition_id, lookup_source_type, data_object_id, value_element_id, display_element_id, order_by_element_id, ascending_order, start_date_element_id, end_date_element_id, SQL_SCRIPT_OBJECT_ID, plugin_registration_id, value_return_type, tracking_config_id, business_key, name, description, system_object_type, system_object_display_format, enable_caching ) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, :lookupSourceType, (SELECT do.data_object_id FROM etk_data_object do WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey), (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object do ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :valueElementBusinessKey), (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object do ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :displayElementBusinessKey), (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object do ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :orderByElementBusinessKey), :ascending_order, (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object do ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :startDateElementBusinessKey), (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object do ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :endDateElementBusinessKey), :sql_script_object_id, :plugin_registration_id, :value_return_type, :trackingConfigId, :business_key, :name, :description, :system_object_type, :system_object_display_format, :enable_caching )")
                .setParameter("startDateElementBusinessKey", lookup.get("START_DATE_ELEMENT_ID"))
                .setParameter("lookupSourceType", lookup.get("LOOKUP_SOURCE_TYPE"))
                .setParameter("plugin_registration_id", lookup.get("PLUGIN_REGISTRATION_ID"))
                .setParameter("valueElementBusinessKey", lookup.get("VALUE_ELEMENT_ID"))
                .setParameter("orderByElementBusinessKey", lookup.get("ORDER_BY_ELEMENT_ID"))
                .setParameter("business_key", lookup.get("BUSINESS_KEY"))
                .setParameter("value_return_type", lookup.get("VALUE_RETURN_TYPE"))
                .setParameter("description", lookup.get("DESCRIPTION"))
                .setParameter("dataObjectBusinessKey", lookup.get("DATA_OBJECT_ID"))
                .setParameter("name", lookup.get("NAME"))
                .setParameter("displayElementBusinessKey", lookup.get("DISPLAY_ELEMENT_ID"))
                .setParameter("ascending_order", lookup.get("ASCENDING_ORDER"))
                //.setParameter("lookup_sql", lookup.get("LOOKUP_SQL"))
                .setParameter("sql_script_object_id", matchingScriptId)
                .setParameter("endDateElementBusinessKey", lookup.get("END_DATE_ELEMENT_ID"))
                .setParameter("system_object_type", lookup.get("SYSTEM_OBJECT_TYPE"))
                .setParameter("system_object_display_format", lookup.get("SYSTEM_OBJECT_DISPLAY_FORMAT"))
                .setParameter("enable_caching", lookup.get("ENABLE_CACHING"))
                .setParameter("trackingConfigId", nextTrackingConfigId)
                .execute();
            }else{
                //Update
                etk.createSQL("UPDATE etk_lookup_definition SET lookup_source_type = :lookupSourceType, data_object_id = (SELECT do.data_object_id FROM etk_data_object DO WHERE do.tracking_config_id = :trackingConfigId AND do.business_key = :dataObjectBusinessKey ), value_element_id = (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object DO ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :valueElementBusinessKey ), display_element_id = (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object DO ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :displayElementBusinessKey ), order_by_element_id = (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object DO ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :orderByElementBusinessKey ), ascending_order = :ascending_order, start_date_element_id = (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object DO ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :startDateElementBusinessKey ), end_date_element_id = (SELECT de.data_element_id FROM etk_data_element de JOIN etk_data_object DO ON do.data_object_id = de.data_object_id WHERE do.tracking_config_id = :trackingConfigId AND de.business_key = :endDateElementBusinessKey ), sql_script_object_id = :sql_script_object_id, plugin_registration_id = :plugin_registration_id, value_return_type = :value_return_type, name = :name, description = :description, system_object_type = :system_object_type, system_object_display_format = :system_object_display_format, enable_caching = :enable_caching WHERE lookup_definition_id = :lookupDefinitionId")
                .setParameter("startDateElementBusinessKey", lookup.get("START_DATE_ELEMENT_ID"))
                .setParameter("lookupSourceType", lookup.get("LOOKUP_SOURCE_TYPE"))
                .setParameter("plugin_registration_id", lookup.get("PLUGIN_REGISTRATION_ID"))
                .setParameter("valueElementBusinessKey", lookup.get("VALUE_ELEMENT_ID"))
                .setParameter("orderByElementBusinessKey", lookup.get("ORDER_BY_ELEMENT_ID"))
                .setParameter("value_return_type", lookup.get("VALUE_RETURN_TYPE"))
                .setParameter("description", lookup.get("DESCRIPTION"))
                .setParameter("dataObjectBusinessKey", lookup.get("DATA_OBJECT_ID"))
                .setParameter("name", lookup.get("NAME"))
                .setParameter("displayElementBusinessKey", lookup.get("DISPLAY_ELEMENT_ID"))
                .setParameter("ascending_order", lookup.get("ASCENDING_ORDER"))
                //.setParameter("lookup_sql", lookup.get("LOOKUP_SQL"))
                .setParameter("sql_script_object_id", matchingScriptId)
                .setParameter("endDateElementBusinessKey", lookup.get("END_DATE_ELEMENT_ID"))
                .setParameter("trackingConfigId", nextTrackingConfigId)
                .setParameter("lookupDefinitionId", matchingLookups.get(0).get("LOOKUP_DEFINITION_ID"))
                .setParameter("system_object_type", lookup.get("SYSTEM_OBJECT_TYPE"))
                .setParameter("system_object_display_format", lookup.get("SYSTEM_OBJECT_DISPLAY_FORMAT"))
                .setParameter("enable_caching", lookup.get("ENABLE_CACHING"))
                .execute();
            }
        }

        /* 3) go back and update the dataElements to reference the correct lookup */
        for(final Map<String, String> dataElement : dataElements){
            //Update
            etk.createSQL(isSqlServer()
                    ? "UPDATE etk_data_element SET etk_data_element.lookup_definition_id = ( SELECT lookup.lookup_definition_id FROM etk_lookup_definition lookup WHERE lookup.tracking_config_id = :trackingConfigId AND lookup.business_key = :lookupBusinessKey ) WHERE etk_data_element.business_key = :business_key AND :trackingConfigId = ( SELECT do.tracking_config_id FROM etk_data_object DO WHERE do.data_object_id = etk_data_element.data_object_id )"
                      : "UPDATE etk_data_element de SET de.lookup_definition_id = (SELECT lookup.lookup_definition_id FROM etk_lookup_definition lookup WHERE lookup.tracking_config_id = :trackingConfigId AND lookup.business_key = :lookupBusinessKey) WHERE de.business_key = :business_key AND :trackingConfigId = (SELECT do.tracking_config_id FROM etk_data_object do WHERE do.data_object_id = de.data_object_id)")
            .setParameter("business_key", dataElement.get("BUSINESS_KEY"))
            .setParameter("lookupBusinessKey", lookupValueInListOfMaps(lookups, "LOOKUP_DEFINITION_ID", dataElement.get("LOOKUP_DEFINITION_ID"), "BUSINESS_KEY"))
            .setParameter("trackingConfigId", nextTrackingConfigId)
            .execute();
        }
    }

    /**
     * Import ETK_GROUP (and associated ETK_SUBJECT).
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void importGroups() throws IncorrectResultSizeDataAccessException, ApplicationException {

        final Map<String, Object> destinationGroupBusinessKeys = listToMap (
                etk.createSQL("SELECT BUSINESS_KEY as \"KEY\", GROUP_ID as \"VALUE\" FROM ETK_GROUP").fetchList());

        final List<Map<String, String>> importedSubjects = getTable("GROUPS", "ETK_SUBJECT");
        final List<Map<String, String>> importedGroups = getTable("GROUPS", "ETK_GROUP");


        Long existingGroupId;
        Number hierarchyId = null;

        for(final Map<String, String> importedGroup : importedGroups) {


            final String hierarchyCode = (String)
                    lookupValueInListOfMaps(importedSubjects, "SUBJECT_ID", importedGroup.get("SUBJECT_ID"), "CODE");

            if (StringUtility.isNotBlank(hierarchyCode)) {
                final List<Map<String, Object>> hierarchyIdList =
                        etk.createSQL("select HIERARCHY_ID from ETK_HIERARCHY where code = :code")
                        .returnEmptyResultSetAs(new HashMap<String, Object>())
                        .setParameter("code", importedGroup.get("SUBJECT_ID"))
                        .fetchList();

                if (hierarchyIdList.size() == 1) {
                    hierarchyId = (Number) hierarchyIdList.get(0).get("HIERARCHY_ID");
                } else if (hierarchyIdList.size() > 1) {
                    throw new ApplicationException("Error importing Groups, aborting import: "
                            + "More than one hierarchy node exists in the destination system with a Code = \"" +
                            hierarchyCode + "\". Code values must be unique for this import to succeed.");
                }
            }


            if (destinationGroupBusinessKeys.containsKey(importedGroup.get("BUSINESS_KEY"))) {

                existingGroupId = ((Number) destinationGroupBusinessKeys.get(importedGroup.get("BUSINESS_KEY"))).longValue();

                etk.createSQL("UPDATE ETK_SUBJECT set ALPHA_NAME = :alphaName, NAME = :name, HIERARCHY_ID = :hierarchyId where SUBJECT_ID = :existingGroupId")
                .setParameter("alphaName", lookupValueInListOfMaps(importedSubjects, "SUBJECT_ID", importedGroup.get("SUBJECT_ID"), "ALPHA_NAME"))
                .setParameter("name", lookupValueInListOfMaps(importedSubjects, "SUBJECT_ID", importedGroup.get("SUBJECT_ID"), "NAME"))
                .setParameter("hierarchyId", hierarchyId)
                .setParameter("existingGroupId", existingGroupId)
                .execute();

                etk.getLogger().error("Updated ETK_SUBJECT with SUBJECT_ID = " + existingGroupId);


                etk.createSQL("UPDATE ETK_GROUP set GROUP_NAME = :groupName, DESCRIPTION = :description where GROUP_ID = :existingGroupId")
                .setParameter("groupName", importedGroup.get("GROUP_NAME"))
                .setParameter("description", importedGroup.get("DESCRIPTION"))
                .setParameter("existingGroupId", existingGroupId)
                .execute();

                etk.getLogger().error("Updated Group " + importedGroup.get("BUSINESS_KEY"));

                etk.createSQL("delete from ETK_STATE_FILTER where INBOX_PREFERENCE_ID in ( "
                        + " select ip.INBOX_PREFERENCE_ID from ETK_INBOX_PREFERENCE ip where ip.SUBJECT_PREFERENCE_ID in ( "
                        + " select sp.SUBJECT_PREFERENCE_ID from ETK_SUBJECT_PREFERENCE sp where sp.SUBJECT_ID = :existingGroupId))")
                .setParameter("existingGroupId", existingGroupId)
                .execute();

                etk.createSQL("delete from ETK_INBOX_PREFERENCE where SUBJECT_PREFERENCE_ID in ( "
                        + " select sp.SUBJECT_PREFERENCE_ID from ETK_SUBJECT_PREFERENCE sp where sp.SUBJECT_ID = :existingGroupId)")
                .setParameter("existingGroupId", existingGroupId)
                .execute();

                etk.createSQL("delete from ETK_SUBJECT_PREFERENCE where SUBJECT_ID = :existingGroupId")
                .setParameter("existingGroupId", existingGroupId)
                .execute();

                etk.createSQL("delete from ETK_SUBJECT_ROLE where SUBJECT_ID = :existingGroupId")
                .setParameter("existingGroupId", existingGroupId)
                .execute();
            } else {
                Number subjectId = null;

                if (isSqlServer()) {
                    subjectId =
                            etk.createSQL("insert into ETK_SUBJECT (ALPHA_NAME, NAME, HIERARCHY_ID) values " +
                                    "(:ALPHA_NAME, :NAME, :HIERARCHY_ID)")
                            .setParameter("ALPHA_NAME", lookupValueInListOfMaps(importedSubjects, "SUBJECT_ID", importedGroup.get("SUBJECT_ID"), "ALPHA_NAME"))
                            .setParameter("NAME", lookupValueInListOfMaps(importedSubjects, "SUBJECT_ID", importedGroup.get("SUBJECT_ID"), "NAME"))
                            .setParameter("HIERARCHY_ID", hierarchyId)
                            .executeForKey("SUBJECT_ID");
                } else {
                    subjectId = getNextOracleHibernateSequence();

                    etk.createSQL("insert into ETK_SUBJECT (SUBJECT_ID, ALPHA_NAME, NAME, HIERARCHY_ID) values " +
                            "(:SUBJECT_ID, :ALPHA_NAME, :NAME, :HIERARCHY_ID)")
                    .setParameter("SUBJECT_ID", subjectId)
                    .setParameter("ALPHA_NAME", lookupValueInListOfMaps(importedSubjects, "SUBJECT_ID", importedGroup.get("SUBJECT_ID"), "ALPHA_NAME"))
                    .setParameter("NAME", lookupValueInListOfMaps(importedSubjects, "SUBJECT_ID", importedGroup.get("SUBJECT_ID"), "NAME"))
                    .setParameter("HIERARCHY_ID", hierarchyId)
                    .execute();
                }

                etk.createSQL("insert into ETK_GROUP (GROUP_ID, GROUP_NAME, DESCRIPTION, BUSINESS_KEY) values (:groupId, :groupName, :description, :businessKey)")
                .setParameter("groupId", subjectId)
                .setParameter("groupName", importedGroup.get("GROUP_NAME"))
                .setParameter("description", importedGroup.get("DESCRIPTION"))
                .setParameter("businessKey", importedGroup.get("BUSINESS_KEY"))
                .execute();

                etk.getLogger().error("Inserted Group " + importedGroup.get("BUSINESS_KEY"));
            }
        }
    }

    //    public void importSubjectAssociations (String groupBusinessKey) {
    //    	final List<Map<String, String>> subjectAssociations = getTable("GROUPS", "ETK_SUBJECT_ASSOCIATION");
    //
    //    	for (Map<String, String> aSubjectAssociation : subjectAssociations) {
    //    		if (groupBusinessKey.equalsIgnoreCase(aSubjectAssociation.get("GROUP_BUSINESS_KEY"))) {
    //
    //    		}
    //    	}
    //    }


    /**
     * Import ETK_SUBJECT_ROLE.
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private void importSubjectRoles() throws IncorrectResultSizeDataAccessException {
        final List<Map<String, String>> subjectRoles = getTable("GROUPS", "ETK_SUBJECT_ROLE");

        Number destinationRoleId;

        for (final Map<String, String> aSubjectRole : subjectRoles) {
            destinationRoleId = (Number) etk.createSQL("select role_id from etk_role where business_key = :roleKey")
                    .returnEmptyResultSetAs(null)
                    .setParameter("roleKey", aSubjectRole.get("ROLE_BUSINESS_KEY"))
                    .fetchObject();

            if (destinationRoleId != null) {
                etk.createSQL(isSqlServer()
                        ? "insert into ETK_SUBJECT_ROLE (ROLE_ID, SUBJECT_ID) values (:roleId, (select g.GROUP_ID from ETK_GROUP g WHERE g.BUSINESS_KEY = :groupBusinessKey))"
                          : "insert into ETK_SUBJECT_ROLE (SUBJECT_ROLE_ID, ROLE_ID, SUBJECT_ID) values (HIBERNATE_SEQUENCE.NEXTVAL, :roleId, (select g.GROUP_ID from ETK_GROUP g WHERE g.BUSINESS_KEY = :groupBusinessKey))")
                .setParameter("roleId", destinationRoleId)
                .setParameter("groupBusinessKey", aSubjectRole.get("GROUP_BUSINESS_KEY"))
                .execute();

                etk.getLogger().error("Inserted Subject Role \"" + aSubjectRole.get("ROLE_BUSINESS_KEY") +
                        "\" for Group \"" + aSubjectRole.get("GROUP_BUSINESS_KEY") + "\"");
            }
        }
    }


    /**
     * Import ETK_SUBJECT_PREFERENCE.
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void importSubjectPreferences () throws IncorrectResultSizeDataAccessException, ApplicationException {
        final List<Map<String, String>> importedGroups = getTable("GROUPS", "ETK_GROUP");
        final List<Map<String, String>> importedSubjectPreferences = getTable("GROUPS", "ETK_SUBJECT_PREFERENCE");

        String groupBusinessKey;
        Number newSubjectPreferenceId;
        for (final Map<String, String> importedSubjectPreference : importedSubjectPreferences) {

            groupBusinessKey = (String)
                    lookupValueInListOfMaps(
                            importedGroups, "GROUP_ID", importedSubjectPreference.get("SUBJECT_ID"), "BUSINESS_KEY");

            //	    	subjectPreferenceId = (Number) etk.createSQL("select SUBJECT_PREFERENCE_ID from ETK_SUBJECT_PREFERENCE "
            //			    			                          + " where SUBJECT_ID = "
            //			    			                          + "       (select g.GROUP_ID from ETK_GROUP g "
            //			    			                          + "        WHERE g.BUSINESS_KEY = :groupBusinessKey)")
            //	    			                 .returnEmptyResultSetAs(null)
            //	    			                 .setParameter("groupBusinessKey", groupBusinessKey)
            //	    			                 .fetchObject();
            //
            //	    	if (subjectPreferenceId != null) {
            //	    		etk.createSQL(" update ETK_SUBJECT_PREFERENCE set "
            //	    	                 + " MY_QUERIES_VISIBLE = :MY_QUERIES_VISIBLE, "
            //                             + " MY_REPORTS_VISIBLE = :MY_REPORTS_VISIBLE, "
            //                             + " MY_PAGES_VISIBLE = :MY_PAGES_VISIBLE, "
            //                             + " MY_SEARCHES_VISIBLE = :MY_SEARCHES_VISIBLE, "
            //                             + " MY_ANALYTICS_VISIBLE = :MY_ANALYTICS_VISIBLE "
            //                             + " WHERE SUBJECT_PREFERENCE_ID = :SUBJECT_PREFERENCE_ID ")
            //	    					.setParameter("MY_QUERIES_VISIBLE", importedSubjectPreference.get("MY_QUERIES_VISIBLE"))
            //	    					.setParameter("MY_REPORTS_VISIBLE", importedSubjectPreference.get("MY_REPORTS_VISIBLE"))
            //	    					.setParameter("MY_PAGES_VISIBLE", importedSubjectPreference.get("MY_PAGES_VISIBLE"))
            //	    					.setParameter("MY_SEARCHES_VISIBLE", importedSubjectPreference.get("MY_SEARCHES_VISIBLE"))
            //	    					.setParameter("MY_ANALYTICS_VISIBLE", importedSubjectPreference.get("MY_ANALYTICS_VISIBLE"))
            //	    					.setParameter("SUBJECT_PREFERENCE_ID", subjectPreferenceId)
            //	    					.execute();
            //	    	} else {
            if (isSqlServer()) {
                newSubjectPreferenceId =
                        etk.createSQL(" insert into ETK_SUBJECT_PREFERENCE "
                                + " (SUBJECT_ID, MY_QUERIES_VISIBLE, MY_REPORTS_VISIBLE, MY_PAGES_VISIBLE, MY_SEARCHES_VISIBLE, MY_ANALYTICS_VISIBLE) values "
                                + "  ("
                                + "    (select g.GROUP_ID from ETK_GROUP g WHERE g.BUSINESS_KEY = :groupBusinessKey), "
                                + "    :MY_QUERIES_VISIBLE, "
                                + "    :MY_REPORTS_VISIBLE, "
                                + "    :MY_PAGES_VISIBLE, "
                                + "    :MY_SEARCHES_VISIBLE, "
                                + "    :MY_ANALYTICS_VISIBLE "
                                + "  ) ")
                        .setParameter("groupBusinessKey", groupBusinessKey)
                        .setParameter("MY_QUERIES_VISIBLE", importedSubjectPreference.get("MY_QUERIES_VISIBLE"))
                        .setParameter("MY_REPORTS_VISIBLE", importedSubjectPreference.get("MY_REPORTS_VISIBLE"))
                        .setParameter("MY_PAGES_VISIBLE", importedSubjectPreference.get("MY_PAGES_VISIBLE"))
                        .setParameter("MY_SEARCHES_VISIBLE", importedSubjectPreference.get("MY_SEARCHES_VISIBLE"))
                        .setParameter("MY_ANALYTICS_VISIBLE", importedSubjectPreference.get("MY_ANALYTICS_VISIBLE"))
                        .executeForKey("SUBJECT_PREFERENCE_ID");
            } else {

                newSubjectPreferenceId = getNextOracleHibernateSequence();

                etk.createSQL(" insert into ETK_SUBJECT_PREFERENCE "
                        + " (SUBJECT_PREFERENCE_ID, SUBJECT_ID, MY_QUERIES_VISIBLE, MY_REPORTS_VISIBLE, MY_PAGES_VISIBLE, MY_SEARCHES_VISIBLE, MY_ANALYTICS_VISIBLE) values "
                        + "  ( "
                        + "     :newSubjectPreferenceId, "
                        + "     (select g.GROUP_ID from ETK_GROUP g WHERE g.BUSINESS_KEY = :groupBusinessKey), "
                        + "    :MY_QUERIES_VISIBLE, "
                        + "    :MY_REPORTS_VISIBLE, "
                        + "    :MY_PAGES_VISIBLE, "
                        + "    :MY_SEARCHES_VISIBLE, "
                        + "    :MY_ANALYTICS_VISIBLE"
                        + "  ) ")
                .setParameter("newSubjectPreferenceId", newSubjectPreferenceId)
                .setParameter("groupBusinessKey", groupBusinessKey)
                .setParameter("MY_QUERIES_VISIBLE", importedSubjectPreference.get("MY_QUERIES_VISIBLE"))
                .setParameter("MY_REPORTS_VISIBLE", importedSubjectPreference.get("MY_REPORTS_VISIBLE"))
                .setParameter("MY_PAGES_VISIBLE", importedSubjectPreference.get("MY_PAGES_VISIBLE"))
                .setParameter("MY_SEARCHES_VISIBLE", importedSubjectPreference.get("MY_SEARCHES_VISIBLE"))
                .setParameter("MY_ANALYTICS_VISIBLE", importedSubjectPreference.get("MY_ANALYTICS_VISIBLE"))
                .execute();
            }

            etk.getLogger().error("Inserted Subject Preference with ID \"" + newSubjectPreferenceId +
                    "\" for Group \"" + groupBusinessKey + "\"");

            importInboxPreference(importedSubjectPreference.get("SUBJECT_PREFERENCE_ID"), newSubjectPreferenceId);

            //}
        }
    }

    /**
     * Import ETK_INBOX_PREFERENCE.
     *
     * @param importedSubjectPreferenceId
     * @param newSubjectPreferenceId
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    /**
     * Import a specific ETK_INBOX_PREFERENCE.
     *
     * @param importedSubjectPreferenceId The id of the subject preference id in the import file
     * @param newSubjectPreferenceId The subject preference id in the new system (this could have been a return value
     *      of this function instead of an input parameter)
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void importInboxPreference (final String importedSubjectPreferenceId, final Number newSubjectPreferenceId)
            throws IncorrectResultSizeDataAccessException, ApplicationException {
        final List<Map<String, String>> importedInboxPreferences = getTable("GROUPS", "ETK_INBOX_PREFERENCE");

        Number newInboxPreferenceId;

        for (final Map<String, String> importedInboxPreference : importedInboxPreferences) {
            if (importedSubjectPreferenceId.equals(importedInboxPreference.get("SUBJECT_PREFERENCE_ID"))) {

                final int dataObjectCount =
                        etk.createSQL("select count(*) from ETK_DATA_OBJECT where TABLE_NAME = :tableName"
                                +  " and tracking_config_id = :nextTrackingConfigId")
                        .setParameter("tableName", importedInboxPreference.get("DATA_OBJECT_KEY"))
                        .setParameter("nextTrackingConfigId", nextTrackingConfigId)
                        .fetchInt();

                if (dataObjectCount == 1) {

                    if (isSqlServer()) {
                        newInboxPreferenceId =
                                etk.createSQL(
                                        " insert into ETK_INBOX_PREFERENCE ( "
                                                + " SUBJECT_PREFERENCE_ID, "
                                                + " DATA_OBJECT_KEY, "
                                                + " DISPLAY_ON_TRACKING_INBOX, "
                                                + " MAX_ROWS, "
                                                + " DISPLAYED_ON_DASHBOARD, "
                                                + " PAGE_SIZE, "
                                                + " DEFAULT_INBOX, "
                                                + " DEFAULT_TAB, "
                                                + " ASSIGNMENT_FILTER_DASHBOARD, "
                                                + " ASSIGNMENT_FILTER_INBOX) values ( "
                                                + " :SUBJECT_PREFERENCE_ID, "
                                                + " :DATA_OBJECT_KEY, "
                                                + " :DISPLAY_ON_TRACKING_INBOX, "
                                                + " :MAX_ROWS, "
                                                + " :DISPLAYED_ON_DASHBOARD, "
                                                + " :PAGE_SIZE, "
                                                + " :DEFAULT_INBOX, "
                                                + " :DEFAULT_TAB, "
                                                + " :ASSIGNMENT_FILTER_DASHBOARD, "
                                                + " :ASSIGNMENT_FILTER_INBOX "
                                                + " ) " )
                                .setParameter("SUBJECT_PREFERENCE_ID", newSubjectPreferenceId)
                                .setParameter("DATA_OBJECT_KEY", importedInboxPreference.get("DATA_OBJECT_KEY"))
                                .setParameter("DISPLAY_ON_TRACKING_INBOX", importedInboxPreference.get("DISPLAY_ON_TRACKING_INBOX"))
                                .setParameter("MAX_ROWS", importedInboxPreference.get("MAX_ROWS"))
                                .setParameter("DISPLAYED_ON_DASHBOARD", importedInboxPreference.get("DISPLAYED_ON_DASHBOARD"))
                                .setParameter("PAGE_SIZE", importedInboxPreference.get("PAGE_SIZE"))
                                .setParameter("DEFAULT_INBOX", importedInboxPreference.get("DEFAULT_INBOX"))
                                .setParameter("DEFAULT_TAB", importedInboxPreference.get("DEFAULT_TAB"))
                                .setParameter("ASSIGNMENT_FILTER_DASHBOARD", importedInboxPreference.get("ASSIGNMENT_FILTER_DASHBOARD"))
                                .setParameter("ASSIGNMENT_FILTER_INBOX", importedInboxPreference.get("ASSIGNMENT_FILTER_INBOX"))
                                .executeForKey("INBOX_PREFERENCE_ID");
                    } else {
                        newInboxPreferenceId = getNextOracleHibernateSequence();

                        etk.createSQL(
                                " insert into ETK_INBOX_PREFERENCE ( "
                                        + " INBOX_PREFERENCE_ID, "
                                        + " SUBJECT_PREFERENCE_ID, "
                                        + " DATA_OBJECT_KEY, "
                                        + " DISPLAY_ON_TRACKING_INBOX, "
                                        + " MAX_ROWS, "
                                        + " DISPLAYED_ON_DASHBOARD, "
                                        + " PAGE_SIZE, "
                                        + " DEFAULT_INBOX, "
                                        + " DEFAULT_TAB, "
                                        + " ASSIGNMENT_FILTER_DASHBOARD, "
                                        + " ASSIGNMENT_FILTER_INBOX) values ( "
                                        + " :INBOX_PREFERENCE_ID, "
                                        + " :SUBJECT_PREFERENCE_ID, "
                                        + " :DATA_OBJECT_KEY, "
                                        + " :DISPLAY_ON_TRACKING_INBOX, "
                                        + " :MAX_ROWS, "
                                        + " :DISPLAYED_ON_DASHBOARD, "
                                        + " :PAGE_SIZE, "
                                        + " :DEFAULT_INBOX, "
                                        + " :DEFAULT_TAB, "
                                        + " :ASSIGNMENT_FILTER_DASHBOARD, "
                                        + " :ASSIGNMENT_FILTER_INBOX "
                                        + " ) " )
                        .setParameter("INBOX_PREFERENCE_ID", newInboxPreferenceId)
                        .setParameter("SUBJECT_PREFERENCE_ID", newSubjectPreferenceId)
                        .setParameter("DATA_OBJECT_KEY", importedInboxPreference.get("DATA_OBJECT_KEY"))
                        .setParameter("DISPLAY_ON_TRACKING_INBOX", importedInboxPreference.get("DISPLAY_ON_TRACKING_INBOX"))
                        .setParameter("MAX_ROWS", importedInboxPreference.get("MAX_ROWS"))
                        .setParameter("DISPLAYED_ON_DASHBOARD", importedInboxPreference.get("DISPLAYED_ON_DASHBOARD"))
                        .setParameter("PAGE_SIZE", importedInboxPreference.get("PAGE_SIZE"))
                        .setParameter("DEFAULT_INBOX", importedInboxPreference.get("DEFAULT_INBOX"))
                        .setParameter("DEFAULT_TAB", importedInboxPreference.get("DEFAULT_TAB"))
                        .setParameter("ASSIGNMENT_FILTER_DASHBOARD", importedInboxPreference.get("ASSIGNMENT_FILTER_DASHBOARD"))
                        .setParameter("ASSIGNMENT_FILTER_INBOX", importedInboxPreference.get("ASSIGNMENT_FILTER_INBOX"))
                        .execute();
                    }

                    etk.getLogger().error("Inserted Inbox Preference with ID \"" + newInboxPreferenceId +
                            "\" for Subject Preference with ID \"" + newSubjectPreferenceId + "\"");

                    importStateFilter(importedInboxPreference.get("INBOX_PREFERENCE_ID"), newInboxPreferenceId);

                } else if (dataObjectCount > 1) {
                    throw new ApplicationException("More than one ETK_DATA_OBJECT found with TABLE_NAME = " +
                            importedInboxPreference.get("DATA_OBJECT_KEY") +
                            " while importing groups, exiting import.");
                }
            }
        }
    }

    /**
     * Import ETK_STATE_FILTER.
     *
     * @param importedInboxPreferenceId The inbox preference id in the export file
     * @param newInboxPreferenceId the inbox preference id in the new system
     */
    private void importStateFilter (final String importedInboxPreferenceId, final Number newInboxPreferenceId) {
        final List<Map<String, String>> importedStateFilters = getTable("GROUPS", "ETK_STATE_FILTER");

        for (final Map<String, String> importedStateFilter : importedStateFilters) {
            if (importedInboxPreferenceId.equals(importedStateFilter.get("INBOX_PREFERENCE_ID"))) {
                if(isSqlServer()) {
                    etk.createSQL("insert into ETK_STATE_FILTER ("
                            + " APPLIED_TO_DASHBOARD, "
                            + " APPLIED_TO_TRACKING_INBOX, "
                            + " STATE, "
                            + " INBOX_PREFERENCE_ID) values ( "
                            + " :APPLIED_TO_DASHBOARD, "
                            + " :APPLIED_TO_TRACKING_INBOX, "
                            + " :STATE,"
                            + " :INBOX_PREFERENCE_ID )")
                    .setParameter("APPLIED_TO_DASHBOARD", importedStateFilter.get("APPLIED_TO_DASHBOARD"))
                    .setParameter("APPLIED_TO_TRACKING_INBOX", importedStateFilter.get("APPLIED_TO_TRACKING_INBOX"))
                    .setParameter("STATE", importedStateFilter.get("STATE"))
                    .setParameter("INBOX_PREFERENCE_ID", newInboxPreferenceId)
                    .execute();
                } else {

                    etk.createSQL("insert into ETK_STATE_FILTER ("
                            + " STATE_FILTER_ID, "
                            + " APPLIED_TO_DASHBOARD, "
                            + " APPLIED_TO_TRACKING_INBOX, "
                            + " STATE, "
                            + " INBOX_PREFERENCE_ID) values ( "
                            + " hibernate_sequence.nextval, "
                            + " :APPLIED_TO_DASHBOARD, "
                            + " :APPLIED_TO_TRACKING_INBOX, "
                            + " :STATE,"
                            + " :INBOX_PREFERENCE_ID )")
                    .setParameter("APPLIED_TO_DASHBOARD", importedStateFilter.get("APPLIED_TO_DASHBOARD"))
                    .setParameter("APPLIED_TO_TRACKING_INBOX", importedStateFilter.get("APPLIED_TO_TRACKING_INBOX"))
                    .setParameter("STATE", importedStateFilter.get("STATE"))
                    .setParameter("INBOX_PREFERENCE_ID", newInboxPreferenceId)
                    .execute();
                }

                etk.getLogger().error("Inserted State Filter with State \"" + importedStateFilter.get("STATE") +
                        "\" for Inbox Preference with ID \"" + newInboxPreferenceId + "\"");
            }
        }
    }

    /**
     * Performs the actual import of a zip file.
     *
     * @param inputStream the zip file
     * @throws IOException If there was an underlying {@link IOException}
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void performImport(final InputStream inputStream) throws
    IOException,
    IncorrectResultSizeDataAccessException,
    ApplicationException {
        //Unzip the input and put all files into a file map.
        //Currently, this assumes that all files data is string.
        populateFileMap(inputStream);

        //If ignoreVersionOnImport is unchecked, validate versions of imported components to make sure they are not
        //older than existing components.
        etk.getLogger().error("Component Import - Beginning Version Checks");
        validateInstallationVersionInformation(StringUtility.isBlank(etk.getParameters().getSingle("ignoreVersionOnImport")));
        etk.getLogger().error("Component Import - End Version Checks");

        deleteEntries();
        importRoles();
        importScriptObjects();
        deletePagePermissions();
        importPages();
        importPagePermissions();
        importPlugins();
        importDataObjects();
        importDataElementsAndLookups();
        importDataViews();
        importDoStates();
        importDoTransitions();
        importTrackingEventListeners();
        //importDoTimers();
        importDataEventListeners();
        importFilterHandlers();
        importDataForms();
        importDataViewElement();
        importDataFormEventHandlers();
        importDisplayMappings();
        importFormControls();
        importFormControlElementBindings();
        importFormControlLabelBindings();
        importFormControlLookupBindings();
        importFormControlEventHandlers();
        importRoleDataPermissions();
        importRolePermissions();
        importJobs();
        importGroups();
        importSubjectRoles();
        importSubjectPreferences();

        etk.createSQL("update ETK_WORKSPACE "
                + "set workspace_revision = (workspace_revision + 1) "
                + "where workspace_name = 'system'").execute();
    }

    /**
     * Private method to validate the imported components against the system's T_AEA_COMPONENT_VERSION_INFO table.
     * Stores latest information about installed components. This will throw an ApplicationException if the program attempts to
     * install an older version of a component over a newer version.
     *
     * @param performAdditionalValidation flag indicating whether or not additional version validation should be done.
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private void validateInstallationVersionInformation(final boolean performAdditionalValidation)
            throws IncorrectResultSizeDataAccessException, ApplicationException {
        List<Map<String, String>> componentVersionInfo = null;
        try {
            componentVersionInfo = getTable("T_AEA_COMPONENT_VERSION_INFO");
        } catch (final Exception e) {
            etk.getLogger().error("Error retrieving T_AEA_COMPONENT_VERSION_INFO while importing components, "
                    + "export might be older than version table.");
        }

        //If no information was included in the exported XML about the component versioning, just return, don't block import.
        //This is necessary to keep this component backwards compatible with existing imports.
        if (componentVersionInfo == null) {
            return;
        }

        //Declare variables in for loop.
        SQLFacade query = null;
        String code;
        String name;
        Integer importedVersion;
        Integer order;
        java.sql.Timestamp etkStartDate;
        java.sql.Timestamp etkEndDate;
        final Timestamp currentServerTime = new Timestamp(Localizations.getCurrentServerTimestamp().getDateValue().getTime());
        final SQLFacade currentVersionQuery = etk.createSQL("select C_CURRENT_VERSION from T_AEA_COMPONENT_VERSION_INFO where C_CODE = :code");
        final String systemVersion = etk.createSQL("select current_version from ETK_INSTALLED_RELEASES").fetchString().replaceAll("\\.", "");


        for (final Map<String, String> aComponent : componentVersionInfo) {

            //Init variable values.
            code = aComponent.get("C_CODE");
            name = aComponent.get("C_NAME");
            importedVersion = toInteger(aComponent.get("C_CURRENT_VERSION"));
            order = toInteger(aComponent.get("C_ORDER"));
            etkStartDate = toTimestamp(aComponent.get("ETK_START_DATE"));
            etkEndDate = toTimestamp(aComponent.get("ETK_START_DATE"));

            //Throw an exception if code, name or importedVersion are null. These are required fields and should never be null.
            if ((code == null) || (name == null) || (importedVersion == null)) {
                throw new ApplicationException ("Error importing T_AEA_COMPONENT_VERSION_INFO"
                        + " - imported XML record missing C_CODE, C_NAME or C_CURRENT_VERSION.");
            }

            if (performAdditionalValidation && !aComponent.get("C_CURRENT_VERSION").startsWith(systemVersion)) {
                throw new ApplicationException ("Error importing component package"
                        + " - imported component version " + importedVersion
                        +  " was not tested / verified on entellitrak version " + systemVersion +". You must install "
                        + "a component with a version matching this entellitrak release or "
                        + "uncheck \"Ignore version check on import\" to bypass this check.");
            }


            //Determine what version of the component is currently installed, if any.
            final Integer currentVersion = currentVersionQuery
                    .setParameter("code", code)
                    .returnEmptyResultSetAs(null)
                    .fetchInt();

            //If no version is installed, insert a record into T_AEA_COMPONENT_VERSION_INFO for the newly installed component.
            if (currentVersion == null) {
                if (isSqlServer()) {
                    query = etk.createSQL("insert into T_AEA_COMPONENT_VERSION_INFO "
                            + "(C_CODE, C_CURRENT_VERSION, C_DATE_INSTALLED, C_NAME, C_ORDER, ETK_START_DATE, ETK_END_DATE) VALUES "
                            + "(:code, :importedVersion, :dateInstalled, :name, :order, :etkStartDate, :etkEndDate)");
                } else {
                    query = etk.createSQL("insert into T_AEA_COMPONENT_VERSION_INFO "
                            + "(ID, C_CODE, C_CURRENT_VERSION, C_DATE_INSTALLED, C_NAME, C_ORDER, ETK_START_DATE, ETK_END_DATE) VALUES "
                            + "(object_id.nextval, :code, :importedVersion, :dateInstalled, :name, :order, :etkStartDate, :etkEndDate)");
                }
            } else if (!performAdditionalValidation || currentVersion.compareTo(importedVersion) <= 0) {
                //If an older or equal version of the component is installed on the target system, update the
                //T_AEA_COMPONENT_VERSION_INFO to include the information about the updated component.
                query = etk.createSQL("update T_AEA_COMPONENT_VERSION_INFO set "
                        + "C_CURRENT_VERSION = :importedVersion, "
                        + "C_DATE_INSTALLED = :dateInstalled, "
                        + "C_NAME = :name, "
                        + "C_ORDER = :order, "
                        + "ETK_START_DATE = :etkStartDate, "
                        + "ETK_END_DATE = :etkEndDate "
                        + "where C_CODE = :code");
            } else {
                //If a newer version of the component is already installed, throw an exception so the user cannot overwrite the newer
                //component with an older version. Indicate the component that is already installed, the current version
                query = null;
                throw new ApplicationException ("Error - Imported component \"" + name + "\" is version = " + importedVersion
                        + ", which is older than the currently installed version = " + currentVersion
                        + ", rolling back and exiting component installation.");
            }

            //Perform the insert or update query.
            if (query != null) {
                query.setParameter("code", code)
                .setParameter("importedVersion", importedVersion)
                .setParameter("dateInstalled", currentServerTime)
                .setParameter("name", name)
                .setParameter("order", order)
                .setParameter("etkStartDate", etkStartDate)
                .setParameter("etkEndDate", etkEndDate)
                .execute();
            }

        }
    }

    /**
     * Populate the file map instance variable with data from the zip file.
     *
     * @param inputStream the zip file
     * @throws IOException If there was an underlying {@link IOException}
     */
    private void populateFileMap (final InputStream inputStream) throws IOException {
        final ZipInputStream is = new ZipInputStream(inputStream);

        final byte[] buffer = new byte[ZIP_BUFFER_SIZE_BYTES];
        ZipEntry entry;

        while ((entry = is.getNextEntry()) != null) {
            try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()){
                int len = 0;
                while ((len = is.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                baos.flush();

                fileMap.put(entry.getName(), new String(baos.toByteArray()));
            }
        }
    }

    /**
     * Quick helper method to convert a list of maps to a single map with key/value pairs.
     *
     * @param aListOfMaps A list of maps with key and value.
     * @return A Map with key/value pairs.
     */
    private static Map<String, Object> listToMap (final List<Map<String, Object>> aListOfMaps) {
        final Map<String, Object> returnMap = new HashMap<>();

        for (final Map<String, Object> aMap : aListOfMaps) {
            if ((aMap.get("KEY") != null) && (aMap.get("VALUE") != null)) {
                returnMap.put((String) aMap.get("KEY"), aMap.get("VALUE"));
            }
        }

        return returnMap;
    }

    /**
     * Add an in clause to an SQL string builder that supports more than 1000 records.
     *
     * inObjectList is split into a bracketed set of multiple groups:
     *
     * (columnName in (:inObjectList0-500)
     *  or columnName in (:inObjectList501-1000)
     *  or columnName in (:inObjectList1001-1500))
     *
     *  The resulting SQL is inserted directly into the provided queryBuilder.
     *
     * @param columnName The column name to compare in the in clause.
     * @param parameterName The name to use for the bind-parameter.
     * @param queryBuilder The query to insert the in clause into.
     * @param outputParamMap The parameter map that will be passed into the query.
     * @param inObjectList The list of objects to insert into the in(:objects) clause.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void addLargeInClause (final String columnName, final String parameterName, final StringBuilder queryBuilder,
            final Map outputParamMap, final List inObjectList) {
        final int groupSize = 1000;

        queryBuilder.append(" (");

        if ((inObjectList == null) || (inObjectList.size() == 0)) {
            queryBuilder.append(columnName);
            queryBuilder.append(" in (null)");
        } else if (inObjectList.size() == 1) {
            queryBuilder.append(columnName);
            queryBuilder.append(" = :");
            queryBuilder.append(parameterName);
            outputParamMap.put(parameterName, inObjectList.get(0));
        } else {
            int paramGroup = 0;

            for (int i = 0; i < inObjectList.size(); i=i+groupSize) {
                if ((i + groupSize) < inObjectList.size()) {
                    queryBuilder.append(columnName);
                    queryBuilder.append(" in (:" + parameterName + paramGroup + ") OR ");
                    outputParamMap.put(parameterName + paramGroup, inObjectList.subList(i, i+groupSize));
                } else {
                    queryBuilder.append(columnName);
                    queryBuilder.append(" in (:" + parameterName + paramGroup + ")");
                    outputParamMap.put(parameterName + paramGroup, inObjectList.subList(i, inObjectList.size()));
                }
                paramGroup++;
            }
        }

        queryBuilder.append(") ");
    }

    /**
     * Add an in clause to an SQL string builder that supports more than 1000 records.
     *
     * inObjectList is split into a bracketed set of multiple groups:
     *
     * (columnName in (:inObjectList0-500)
     *  or columnName in (:inObjectList501-1000)
     *  or columnName in (:inObjectList1001-1500))
     *
     *  The resulting SQL is inserted directly into the provided queryBuilder.
     *
     * @param columnName The column name to compare in the in clause.
     * @param parameterName The name to be used for the bind-parameter.
     * @param queryBuilder The query to insert the in clause into.
     * @param outputParamMap The parameter map that will be passed into the query.
     * @param inObjectList The list of objects to insert into the in(:objects) clause.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void addLargeNotInClause (final String columnName, final String parameterName, final StringBuilder queryBuilder,
            final Map outputParamMap, final List inObjectList) {
        final int groupSize = 1000;

        queryBuilder.append(" (");

        if ((inObjectList == null) || (inObjectList.size() == 0)) {
            queryBuilder.append(columnName);
            queryBuilder.append(" not in (null)");
        } else if (inObjectList.size() == 1) {
            queryBuilder.append(columnName);
            queryBuilder.append(" <> :");
            queryBuilder.append(parameterName);
            outputParamMap.put(parameterName, inObjectList.get(0));
        } else {
            int paramGroup = 0;

            for (int i = 0; i < inObjectList.size(); i=i+groupSize) {
                if ((i + groupSize) < inObjectList.size()) {
                    queryBuilder.append(columnName);
                    queryBuilder.append(" not in (:" + parameterName + paramGroup + ") AND ");
                    outputParamMap.put(parameterName + paramGroup, inObjectList.subList(i, i+groupSize));
                } else {
                    queryBuilder.append(columnName);
                    queryBuilder.append(" not in (:" + parameterName + paramGroup + ")");
                    outputParamMap.put(parameterName + paramGroup, inObjectList.subList(i, inObjectList.size()));
                }
                paramGroup++;
            }
        }

        queryBuilder.append(") ");
    }

    /**
     * Selects the next value from HIBERNATE_SEQUENCE.
     *
     * @return The next value
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private Number getNextOracleHibernateSequence() throws IncorrectResultSizeDataAccessException {
        return ((Number) etk.createSQL("select hibernate_sequence.nextval from dual").fetchObject()).longValue();
    }

    /**
     * Import ETK_QRTZ_JOB_DETAILS.
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private void importQuartzJobDetails() throws IncorrectResultSizeDataAccessException {
        final List<Map<String, String>> jobDetails = getTable("JOBS", "ETK_QRTZ_JOB_DETAILS");

        for (final Map<String, String> jobDetail : jobDetails) {
            final int recordCount = etk.createSQL("select count(*) from ETK_QRTZ_JOB_DETAILS where JOB_NAME = :JOB_NAME")
                    .setParameter("JOB_NAME", jobDetail.get("JOB_NAME"))
                    .fetchInt();

            if (recordCount >= 1) {
                etk.getLogger().error("Updating ETK_QRTZ_JOB_DETAILS with JOB_NAME=" + jobDetail.get("JOB_NAME"));

                etk.createSQL("update ETK_QRTZ_JOB_DETAILS set DESCRIPTION = :DESCRIPTION, JOB_CLASS_NAME = :JOB_CLASS_NAME, IS_DURABLE = :IS_DURABLE, IS_NONCONCURRENT = :IS_NONCONCURRENT, IS_UPDATE_DATA = :IS_UPDATE_DATA, REQUESTS_RECOVERY = :REQUESTS_RECOVERY, JOB_DATA = :JOB_DATA WHERE JOB_NAME = :JOB_NAME")
                .setParameter("DESCRIPTION", jobDetail.get("DESCRIPTION"))
                .setParameter("JOB_CLASS_NAME", jobDetail.get("JOB_CLASS_NAME"))
                .setParameter("IS_DURABLE", jobDetail.get("IS_DURABLE"))
                .setParameter("IS_NONCONCURRENT", jobDetail.get("IS_NONCONCURRENT"))
                .setParameter("IS_UPDATE_DATA", jobDetail.get("IS_UPDATE_DATA"))
                .setParameter("REQUESTS_RECOVERY", jobDetail.get("REQUESTS_RECOVERY"))
                .setParameter("JOB_DATA", StringUtility.isBlank(jobDetail.get("JOB_DATA")) ? null : Base64.decodeBase64(jobDetail.get("JOB_DATA").getBytes()))
                .setParameter("JOB_NAME", jobDetail.get("JOB_NAME"))
                .execute();
            } else {
                etk.getLogger().error("Creating new ETK_QRTZ_JOB_DETAILS with JOB_NAME=" + jobDetail.get("JOB_NAME"));

                etk.createSQL("INSERT INTO ETK_QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP, DESCRIPTION, JOB_CLASS_NAME, IS_DURABLE, IS_NONCONCURRENT, IS_UPDATE_DATA, REQUESTS_RECOVERY, JOB_DATA) VALUES (:SCHED_NAME, :JOB_NAME, :JOB_GROUP, :DESCRIPTION, :JOB_CLASS_NAME, :IS_DURABLE, :IS_NONCONCURRENT, :IS_UPDATE_DATA, :REQUESTS_RECOVERY, :JOB_DATA)")
                .setParameter("SCHED_NAME", jobDetail.get("SCHED_NAME"))
                .setParameter("JOB_NAME", jobDetail.get("JOB_NAME"))
                .setParameter("JOB_GROUP", jobDetail.get("JOB_GROUP"))
                .setParameter("DESCRIPTION", jobDetail.get("DESCRIPTION"))
                .setParameter("JOB_CLASS_NAME", jobDetail.get("JOB_CLASS_NAME"))
                .setParameter("IS_DURABLE", jobDetail.get("IS_DURABLE"))
                .setParameter("IS_NONCONCURRENT", jobDetail.get("IS_NONCONCURRENT"))
                .setParameter("IS_UPDATE_DATA", jobDetail.get("IS_UPDATE_DATA"))
                .setParameter("REQUESTS_RECOVERY", jobDetail.get("REQUESTS_RECOVERY"))
                .setParameter("JOB_DATA", StringUtility.isBlank(jobDetail.get("JOB_DATA")) ? null : Base64.decodeBase64(jobDetail.get("JOB_DATA").getBytes()))
                .execute();
            }
        }
    }


    /**
     * Import ETK_QRTZ_TRIGGERS.
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private void importQuartzTriggers() throws IncorrectResultSizeDataAccessException {
        final List<Map<String, String>> triggers = getTable("JOBS", "ETK_QRTZ_TRIGGERS");

        for (final Map<String, String> trigger : triggers) {
            final int recordCount = etk.createSQL("select count(*) from ETK_QRTZ_TRIGGERS where JOB_NAME = :JOB_NAME")
                    .setParameter("JOB_NAME", trigger.get("JOB_NAME"))
                    .fetchInt();



            if (recordCount >= 1) {
                etk.getLogger().error("Updating ETK_QRTZ_TRIGGERS with JOB_NAME=" + trigger.get("JOB_NAME"));

                etk.createSQL("update ETK_QRTZ_TRIGGERS set SCHED_NAME=:SCHED_NAME, TRIGGER_NAME=:TRIGGER_NAME, TRIGGER_GROUP=:TRIGGER_GROUP, JOB_GROUP=:JOB_GROUP, DESCRIPTION=:DESCRIPTION, NEXT_FIRE_TIME=:NEXT_FIRE_TIME, PREV_FIRE_TIME=:PREV_FIRE_TIME, PRIORITY=:PRIORITY, TRIGGER_STATE=:TRIGGER_STATE, TRIGGER_TYPE=:TRIGGER_TYPE, START_TIME=:START_TIME, END_TIME=:END_TIME, CALENDAR_NAME=:CALENDAR_NAME, MISFIRE_INSTR=:MISFIRE_INSTR, JOB_DATA=:JOB_DATA where JOB_NAME=:JOB_NAME")
                .setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
                .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
                .setParameter("TRIGGER_GROUP", trigger.get("TRIGGER_GROUP"))
                .setParameter("JOB_NAME", trigger.get("JOB_NAME"))
                .setParameter("JOB_GROUP", trigger.get("JOB_GROUP"))
                .setParameter("DESCRIPTION", trigger.get("DESCRIPTION"))
                .setParameter("NEXT_FIRE_TIME", trigger.get("NEXT_FIRE_TIME"))
                .setParameter("PREV_FIRE_TIME", trigger.get("PREV_FIRE_TIME"))
                .setParameter("PRIORITY", trigger.get("PRIORITY"))
                .setParameter("TRIGGER_STATE", trigger.get("TRIGGER_STATE"))
                .setParameter("TRIGGER_TYPE", trigger.get("TRIGGER_TYPE"))
                .setParameter("START_TIME", trigger.get("START_TIME"))
                .setParameter("END_TIME", trigger.get("END_TIME"))
                .setParameter("CALENDAR_NAME", trigger.get("CALENDAR_NAME"))
                .setParameter("MISFIRE_INSTR", trigger.get("MISFIRE_INSTR"))
                .setParameter("JOB_DATA", StringUtility.isBlank(trigger.get("JOB_DATA")) ? null : Base64.decodeBase64(trigger.get("JOB_DATA").getBytes()))
                .execute();
            } else {
                etk.getLogger().error("Creating new ETK_QRTZ_TRIGGERS with JOB_NAME=" + trigger.get("JOB_NAME"));

                etk.createSQL("Insert into ETK_QRTZ_TRIGGERS (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,JOB_NAME,JOB_GROUP,DESCRIPTION,NEXT_FIRE_TIME,PREV_FIRE_TIME,PRIORITY,TRIGGER_STATE,TRIGGER_TYPE,START_TIME,END_TIME,CALENDAR_NAME,MISFIRE_INSTR,JOB_DATA) values (:SCHED_NAME,:TRIGGER_NAME,:TRIGGER_GROUP,:JOB_NAME,:JOB_GROUP,:DESCRIPTION,:NEXT_FIRE_TIME,:PREV_FIRE_TIME,:PRIORITY,:TRIGGER_STATE,:TRIGGER_TYPE,:START_TIME,:END_TIME,:CALENDAR_NAME,:MISFIRE_INSTR,:JOB_DATA)")
                .setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
                .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
                .setParameter("TRIGGER_GROUP", trigger.get("TRIGGER_GROUP"))
                .setParameter("JOB_NAME", trigger.get("JOB_NAME"))
                .setParameter("JOB_GROUP", trigger.get("JOB_GROUP"))
                .setParameter("DESCRIPTION", trigger.get("DESCRIPTION"))
                .setParameter("NEXT_FIRE_TIME", trigger.get("NEXT_FIRE_TIME"))
                .setParameter("PREV_FIRE_TIME", trigger.get("PREV_FIRE_TIME"))
                .setParameter("PRIORITY", trigger.get("PRIORITY"))
                .setParameter("TRIGGER_STATE", trigger.get("TRIGGER_STATE"))
                .setParameter("TRIGGER_TYPE", trigger.get("TRIGGER_TYPE"))
                .setParameter("START_TIME", trigger.get("START_TIME"))
                .setParameter("END_TIME", trigger.get("END_TIME"))
                .setParameter("CALENDAR_NAME", trigger.get("CALENDAR_NAME"))
                .setParameter("MISFIRE_INSTR", trigger.get("MISFIRE_INSTR"))
                .setParameter("JOB_DATA", StringUtility.isBlank(trigger.get("JOB_DATA")) ? null : Base64.decodeBase64(trigger.get("JOB_DATA").getBytes()))
                .execute();
            }
        }
    }


    /**
     * Import ETK_QRTZ_BLOB_TRIGGERS.
     */
    private void importQuartzBlobTriggers(){
        final List<Map<String, String>> triggers = getTable("JOBS", "ETK_QRTZ_BLOB_TRIGGERS");

        for (final Map<String, String> trigger : triggers) {
            //    		int recordCount = etk.createSQL("select count(*) from ETK_QRTZ_BLOB_TRIGGERS where TRIGGER_NAME = :TRIGGER_NAME")
            //    				              .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            //    				              .fetchInt();
            //
            //    		if (recordCount >= 1) {
            //    			etk.getLogger().error("Updating ETK_QRTZ_BLOB_TRIGGERS with TRIGGER_NAME=" + trigger.get("TRIGGER_NAME"));
            //
            //    			etk.createSQL("update ETK_QRTZ_BLOB_TRIGGERS set SCHED_NAME=:SCHED_NAME, TRIGGER_GROUP=:TRIGGER_GROUP, BLOB_DATA=:BLOB_DATA where TRIGGER_NAME=:TRIGGER_NAME")
            //    			.setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
            //    			.setParameter("TRIGGER_GROUP", trigger.get("TRIGGER_GROUP"))
            //    			.setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            //	 			.setParameter("BLOB_DATA", StringUtility.isBlank(trigger.get("BLOB_DATA")) ? null : Base64.decodeBase64(trigger.get("BLOB_DATA").getBytes()))
            //	 			.execute();
            //    		} else {

            etk.createSQL("delete from ETK_QRTZ_BLOB_TRIGGERS where TRIGGER_NAME = :TRIGGER_NAME")
            .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            .execute();

            etk.getLogger().error("Creating ETK_QRTZ_BLOB_TRIGGERS with TRIGGER_NAME=" + trigger.get("TRIGGER_NAME"));

            etk.createSQL("insert into ETK_QRTZ_BLOB_TRIGGERS (SCHED_NAME, TRIGGER_GROUP, TRIGGER_NAME, BLOB_DATA) values (:SCHED_NAME, :TRIGGER_GROUP, :TRIGGER_NAME, :BLOB_DATA)")
            .setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
            .setParameter("TRIGGER_GROUP", trigger.get("TRIGGER_GROUP"))
            .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            .setParameter("BLOB_DATA", StringUtility.isBlank(trigger.get("BLOB_DATA")) ? null : Base64.decodeBase64(trigger.get("BLOB_DATA").getBytes()))
            .execute();
            //}
        }
    }

    /**
     * Import ETK_QRTZ_CRON_TRIGGERS.
     */
    private void importQuartzCronTriggers() {
        final List<Map<String, String>> triggers = getTable("JOBS", "ETK_QRTZ_CRON_TRIGGERS");

        for (final Map<String, String> trigger : triggers) {
            //    		int recordCount = etk.createSQL("select count(*) from ETK_QRTZ_CRON_TRIGGERS where TRIGGER_NAME = :TRIGGER_NAME")
            //    				              .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            //    				              .fetchInt();
            //
            //    		if (recordCount >= 1) {
            //    			etk.getLogger().error("Updating ETK_QRTZ_CRON_TRIGGERS with TRIGGER_NAME=" + trigger.get("TRIGGER_NAME"));
            //
            //    			etk.createSQL("update ETK_QRTZ_CRON_TRIGGERS set SCHED_NAME=:SCHED_NAME, TRIGGER_GROUP=:TRIGGER_GROUP, CRON_EXPRESSION=:CRON_EXPRESSION, TIME_ZONE_ID=:TIME_ZONE_ID  where TRIGGER_NAME=:TRIGGER_NAME")
            //    			.setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
            //    			.setParameter("TRIGGER_GROUP", trigger.get("TRIGGER_GROUP"))
            //    			.setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            //    			.setParameter("CRON_EXPRESSION", trigger.get("CRON_EXPRESSION"))
            //    			.setParameter("TIME_ZONE_ID", trigger.get("TIME_ZONE_ID"))
            //	 			.execute();
            //    		} else {

            etk.createSQL("delete from ETK_QRTZ_CRON_TRIGGERS where TRIGGER_NAME = :TRIGGER_NAME")
            .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            .execute();

            etk.getLogger().error("Creating ETK_QRTZ_CRON_TRIGGERS with TRIGGER_NAME=" + trigger.get("TRIGGER_NAME"));

            etk.createSQL("insert into ETK_QRTZ_CRON_TRIGGERS (SCHED_NAME, TRIGGER_GROUP, TRIGGER_NAME, CRON_EXPRESSION, TIME_ZONE_ID) values (:SCHED_NAME, :TRIGGER_GROUP, :TRIGGER_NAME, :CRON_EXPRESSION, :TIME_ZONE_ID)")
            .setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
            .setParameter("TRIGGER_GROUP", trigger.get("TRIGGER_GROUP"))
            .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            .setParameter("CRON_EXPRESSION", trigger.get("CRON_EXPRESSION"))
            .setParameter("TIME_ZONE_ID", trigger.get("TIME_ZONE_ID"))
            .execute();
            //}
        }
    }

    /**
     * Import ETK_QRTZ_FIRED_TRIGGERS.
     */
    private void importQuartzFiredTriggers() {
        final List<Map<String, String>> triggers = getTable("JOBS", "ETK_QRTZ_FIRED_TRIGGERS");

        for (final Map<String, String> trigger : triggers) {
            //    		int recordCount = etk.createSQL("select count(*) from ETK_QRTZ_FIRED_TRIGGERS where ENTRY_ID = :ENTRY_ID")
            //    				              .setParameter("ENTRY_ID", trigger.get("ENTRY_ID"))
            //    				              .fetchInt();
            //
            //    		if (recordCount >= 1) {
            //    			etk.getLogger().error("Updating ETK_QRTZ_FIRED_TRIGGERS with ENTRY_ID=" + trigger.get("ENTRY_ID"));
            //
            //    			etk.createSQL("update ETK_QRTZ_FIRED_TRIGGERS set SCHED_NAME=:SCHED_NAME, TRIGGER_NAME=:TRIGGER_NAME, TRIGGER_GROUP=:TRIGGER_GROUP, INSTANCE_NAME=:INSTANCE_NAME, FIRED_TIME=:FIRED_TIME, PRIORITY=:PRIORITY, STATE=:STATE, JOB_NAME=:JOB_NAME, JOB_GROUP=:JOB_GROUP, IS_NONCONCURRENT=:IS_NONCONCURRENT, REQUESTS_RECOVERY=:REQUESTS_RECOVERY, SCHED_TIME=:SCHED_TIME where ENTRY_ID=:ENTRY_ID")
            //    			.setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
            //    			.setParameter("ENTRY_ID", trigger.get("ENTRY_ID"))
            //    			.setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            //    			.setParameter("TRIGGER_GROUP", trigger.get("TRIGGER_GROUP"))
            //    			.setParameter("INSTANCE_NAME", trigger.get("INSTANCE_NAME"))
            //    			.setParameter("FIRED_TIME", trigger.get("FIRED_TIME"))
            //    			.setParameter("PRIORITY", trigger.get("PRIORITY"))
            //    			.setParameter("STATE", trigger.get("STATE"))
            //    			.setParameter("JOB_NAME", trigger.get("JOB_NAME"))
            //    			.setParameter("JOB_GROUP", trigger.get("JOB_GROUP"))
            //    			.setParameter("IS_NONCONCURRENT", trigger.get("IS_NONCONCURRENT"))
            //    			.setParameter("REQUESTS_RECOVERY", trigger.get("REQUESTS_RECOVERY"))
            //    			.setParameter("SCHED_TIME", trigger.get("SCHED_TIME"))
            //	 			.execute();
            //    		} else {
            etk.createSQL("delete from ETK_QRTZ_FIRED_TRIGGERS where TRIGGER_NAME = :TRIGGER_NAME")
            .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            .execute();

            etk.getLogger().error("Creating ETK_QRTZ_FIRED_TRIGGERS with TRIGGER_NAME=" + trigger.get("TRIGGER_NAME"));

            etk.createSQL("insert into ETK_QRTZ_FIRED_TRIGGERS (SCHED_NAME, ENTRY_ID, TRIGGER_NAME, TRIGGER_GROUP, INSTANCE_NAME, FIRED_TIME, PRIORITY, STATE, JOB_NAME, JOB_GROUP, IS_NONCONCURRENT, REQUESTS_RECOVERY, SCHED_TIME) values (:SCHED_NAME, :ENTRY_ID, :TRIGGER_NAME, :TRIGGER_GROUP, :INSTANCE_NAME, :FIRED_TIME, :PRIORITY, :STATE, :JOB_NAME, :JOB_GROUP, :IS_NONCONCURRENT, :REQUESTS_RECOVERY, :SCHED_TIME)")
            .setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
            .setParameter("ENTRY_ID", trigger.get("ENTRY_ID"))
            .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            .setParameter("TRIGGER_GROUP", trigger.get("TRIGGER_GROUP"))
            .setParameter("INSTANCE_NAME", trigger.get("INSTANCE_NAME"))
            .setParameter("FIRED_TIME", trigger.get("FIRED_TIME"))
            .setParameter("PRIORITY", trigger.get("PRIORITY"))
            .setParameter("STATE", trigger.get("STATE"))
            .setParameter("JOB_NAME", trigger.get("JOB_NAME"))
            .setParameter("JOB_GROUP", trigger.get("JOB_GROUP"))
            .setParameter("IS_NONCONCURRENT", trigger.get("IS_NONCONCURRENT"))
            .setParameter("REQUESTS_RECOVERY", trigger.get("REQUESTS_RECOVERY"))
            .setParameter("SCHED_TIME", trigger.get("SCHED_TIME"))
            .execute();
            //}
        }
    }

    /**
     * Import ETK_QRTTZ_SIMPLE_TRIGGERS.
     */
    private void importQuartzSimpleTriggers(){
        final List<Map<String, String>> triggers = getTable("JOBS", "ETK_QRTZ_SIMPLE_TRIGGERS");

        for (final Map<String, String> trigger : triggers) {
            //    		int recordCount = etk.createSQL("select count(*) from ETK_QRTZ_SIMPLE_TRIGGERS where TRIGGER_NAME = :TRIGGER_NAME")
            //    				              .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            //    				              .fetchInt();
            //
            //    		if (recordCount >= 1) {
            //    			etk.getLogger().error("Updating ETK_QRTZ_SIMPLE_TRIGGERS with TRIGGER_NAME=" + trigger.get("TRIGGER_NAME"));
            //
            //    			etk.createSQL("update ETK_QRTZ_SIMPLE_TRIGGERS set SCHED_NAME=:SCHED_NAME, TRIGGER_GROUP=:TRIGGER_GROUP, REPEAT_COUNT=:REPEAT_COUNT, REPEAT_INTERVAL=:REPEAT_INTERVAL, TIMES_TRIGGERED=:TIMES_TRIGGERED where TRIGGER_NAME=:TRIGGER_NAME")
            //    			.setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
            //    			.setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            //    			.setParameter("TRIGGER_GROUP", trigger.get("TRIGGER_GROUP"))
            //    			.setParameter("REPEAT_COUNT", trigger.get("REPEAT_COUNT"))
            //    			.setParameter("REPEAT_INTERVAL", trigger.get("REPEAT_INTERVAL"))
            //    			.setParameter("TIMES_TRIGGERED", trigger.get("TIMES_TRIGGERED"))
            //	 			.execute();
            //    		} else {
            etk.createSQL("delete from ETK_QRTZ_SIMPLE_TRIGGERS where TRIGGER_NAME = :TRIGGER_NAME")
            .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            .execute();

            etk.getLogger().error("Creating ETK_QRTZ_SIMPLE_TRIGGERS with TRIGGER_NAME=" + trigger.get("TRIGGER_NAME"));

            etk.createSQL("insert into ETK_QRTZ_SIMPLE_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, REPEAT_COUNT, REPEAT_INTERVAL, TIMES_TRIGGERED) values (:SCHED_NAME, :TRIGGER_NAME, :TRIGGER_GROUP, :REPEAT_COUNT, :REPEAT_INTERVAL, :TIMES_TRIGGERED)")
            .setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
            .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            .setParameter("TRIGGER_GROUP", trigger.get("TRIGGER_GROUP"))
            .setParameter("REPEAT_COUNT", trigger.get("REPEAT_COUNT"))
            .setParameter("REPEAT_INTERVAL", trigger.get("REPEAT_INTERVAL"))
            .setParameter("TIMES_TRIGGERED", trigger.get("TIMES_TRIGGERED"))
            .execute();
            //}
        }
    }


    /**
     * Import ETK_QRTZ_SIMPROP_TRIGGERS.
     */
    private void importQuartzSimpropTriggers(){
        final List<Map<String, String>> triggers = getTable("JOBS", "ETK_QRTZ_SIMPROP_TRIGGERS");

        for (final Map<String, String> trigger : triggers) {
            //    		int recordCount = etk.createSQL("select count(*) from ETK_QRTZ_SIMPROP_TRIGGERS where TRIGGER_NAME = :TRIGGER_NAME")
            //    				              .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            //    				              .fetchInt();
            //
            //    		if (recordCount >= 1) {
            //    			etk.getLogger().error("Updating ETK_QRTZ_SIMPROP_TRIGGERS with TRIGGER_NAME=" + trigger.get("TRIGGER_NAME"));
            //
            //    			etk.createSQL("update ETK_QRTZ_SIMPROP_TRIGGERS set SCHED_NAME=:SCHED_NAME, TRIGGER_GROUP=:TRIGGER_GROUP, STR_PROP_1=:STR_PROP_1, STR_PROP_2=:STR_PROP_2, STR_PROP_3=:STR_PROP_3, INT_PROP_1=:INT_PROP_1, INT_PROP_2=:INT_PROP_2, LONG_PROP_1=:LONG_PROP_1, LONG_PROP_2=:LONG_PROP_2, DEC_PROP_1=:DEC_PROP_1, DEC_PROP_2=:DEC_PROP_2, BOOL_PROP_1=:BOOL_PROP_1, BOOL_PROP_2=:BOOL_PROP_2 where TRIGGER_NAME=:TRIGGER_NAME")
            //    			.setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
            //    			.setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            //    			.setParameter("TRIGGER_GROUP", trigger.get("TRIGGER_GROUP"))
            //    			.setParameter("STR_PROP_1", trigger.get("STR_PROP_1"))
            //    			.setParameter("STR_PROP_2", trigger.get("STR_PROP_2"))
            //    			.setParameter("STR_PROP_3", trigger.get("STR_PROP_3"))
            //    			.setParameter("INT_PROP_1", trigger.get("INT_PROP_1"))
            //    			.setParameter("INT_PROP_2", trigger.get("INT_PROP_2"))
            //    			.setParameter("LONG_PROP_1", trigger.get("LONG_PROP_1"))
            //    			.setParameter("LONG_PROP_2", trigger.get("LONG_PROP_2"))
            //    			.setParameter("DEC_PROP_1", trigger.get("DEC_PROP_1"))
            //    			.setParameter("DEC_PROP_2", trigger.get("DEC_PROP_2"))
            //    			.setParameter("BOOL_PROP_1", trigger.get("BOOL_PROP_1"))
            //    			.setParameter("BOOL_PROP_2", trigger.get("BOOL_PROP_2"))
            //	 			.execute();
            //    		} else {
            etk.createSQL("delete from ETK_QRTZ_SIMPROP_TRIGGERS where TRIGGER_NAME = :TRIGGER_NAME")
            .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            .execute();

            etk.getLogger().error("Creating ETK_QRTZ_SIMPROP_TRIGGERS with TRIGGER_NAME=" + trigger.get("TRIGGER_NAME"));

            etk.createSQL("insert into ETK_QRTZ_SIMPROP_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, STR_PROP_1, STR_PROP_2, STR_PROP_3, INT_PROP_1, INT_PROP_2, LONG_PROP_1, LONG_PROP_2, DEC_PROP_1, DEC_PROP_2, BOOL_PROP_1, BOOL_PROP_2) values (:SCHED_NAME, :TRIGGER_NAME, :TRIGGER_GROUP, :STR_PROP_1, :STR_PROP_2, :STR_PROP_3, :INT_PROP_1, :INT_PROP_2, :LONG_PROP_1, :LONG_PROP_2, :DEC_PROP_1, :DEC_PROP_2, :BOOL_PROP_1, :BOOL_PROP_2)")
            .setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
            .setParameter("TRIGGER_NAME", trigger.get("TRIGGER_NAME"))
            .setParameter("TRIGGER_GROUP", trigger.get("TRIGGER_GROUP"))
            .setParameter("STR_PROP_1", trigger.get("STR_PROP_1"))
            .setParameter("STR_PROP_2", trigger.get("STR_PROP_2"))
            .setParameter("STR_PROP_3", trigger.get("STR_PROP_3"))
            .setParameter("INT_PROP_1", trigger.get("INT_PROP_1"))
            .setParameter("INT_PROP_2", trigger.get("INT_PROP_2"))
            .setParameter("LONG_PROP_1", trigger.get("LONG_PROP_1"))
            .setParameter("LONG_PROP_2", trigger.get("LONG_PROP_2"))
            .setParameter("DEC_PROP_1", trigger.get("DEC_PROP_1"))
            .setParameter("DEC_PROP_2", trigger.get("DEC_PROP_2"))
            .setParameter("BOOL_PROP_1", trigger.get("BOOL_PROP_1"))
            .setParameter("BOOL_PROP_2", trigger.get("BOOL_PROP_2"))
            .execute();
            //}
        }
    }

    /**
     * Imports ETK_QRTZ_CALENDARS.
     *
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private void importQuartzCalendars() throws IncorrectResultSizeDataAccessException {
        final List<Map<String, String>> triggers = getTable("JOBS", "ETK_QRTZ_CALENDARS");

        for (final Map<String, String> trigger : triggers) {
            final int recordCount = etk.createSQL("select count(*) from ETK_QRTZ_CALENDARS where CALENDAR_NAME = :CALENDAR_NAME")
                    .setParameter("CALENDAR_NAME", trigger.get("CALENDAR_NAME"))
                    .fetchInt();

            if (recordCount >= 1) {
                etk.getLogger().error("Updating ETK_QRTZ_CALENDARS with CALENDAR_NAME=" + trigger.get("CALENDAR_NAME"));

                etk.createSQL("update ETK_QRTZ_CALENDARS set SCHED_NAME=:SCHED_NAME, CALENDAR=:CALENDAR where CALENDAR_NAME=:CALENDAR_NAME")
                .setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
                .setParameter("CALENDAR_NAME", trigger.get("CALENDAR_NAME"))
                .setParameter("CALENDAR", StringUtility.isBlank(trigger.get("CALENDAR")) ? null : Base64.decodeBase64(trigger.get("CALENDAR").getBytes()))
                .execute();
            } else {
                etk.getLogger().error("Creating ETK_QRTZ_CALENDARS with CALENDAR_NAME=" + trigger.get("CALENDAR_NAME"));

                etk.createSQL("insert into ETK_QRTZ_CALENDARS (SCHED_NAME, CALENDAR_NAME, CALENDAR) values (:SCHED_NAME, :CALENDAR_NAME, :CALENDAR)")
                .setParameter("SCHED_NAME", trigger.get("SCHED_NAME"))
                .setParameter("CALENDAR_NAME", trigger.get("CALENDAR_NAME"))
                .setParameter("CALENDAR", StringUtility.isBlank(trigger.get("CALENDAR")) ? null : Base64.decodeBase64(trigger.get("CALENDAR").getBytes()))
                .execute();
            }
        }
    }

    /**
     * This method converts a named of an access level used in the export file, to the number used by entellitrak to
     * represent that access level.
     *
     * @param accessLevelString The name of the access level
     * @return The number entellitrak uses internally to represent the access level
     */
    private static int getAccessLevel(final String accessLevelString) {
        return AccessLevel.getByDisplay(accessLevelString).getEntellitrakNumber();
    }

    /**
     * Access Level used in role data permissions.
     *
     * @author Zachary.Miller
     */
    public enum AccessLevel{
        /** No Access. */
        NO_ACCESS(0, "No Access"),
        /** User. */
        USER(1, "User"),
        /** Org Unit. */
        ORG_UNIT(2, "Organizational Unit"),
        /** Parent Child. */
        PARENT_CHILD_ORG(3, "Parent: Child Organizational Units"),
        /** Global. */
        GLOBAL(4, "Organization Wide");

        private final int etkNumber;
        private final String display;

        /**
         * Simple constructor.
         *
         * @param theEntellitrakNumber The number entellitrak uses internally to refer to this access level
         * @param theDisplay The display value for this access level (this is used in the export file)
         */
        AccessLevel(final int theEntellitrakNumber, final String theDisplay){
            etkNumber = theEntellitrakNumber;
            display = theDisplay;
        }

        /**
         * This method fetches an access level represented by a particular display.
         *
         * @param display display value of the access level (used in the export)
         * @return The access level represented by that display or NO_ACCESS if there was no match.
         */
        public static AccessLevel getByDisplay(final String display){
            return Arrays.asList(values())
                .stream()
                .filter(accessLevel -> display.equals(accessLevel.getDisplay()))
                .findFirst()
                .orElse(NO_ACCESS);
        }

        /**
         * Converts the number entellitrak uses to refer to the access level to the actual access level.
         *
         * @param theEntellitrakNumber The number entellitrak uses internally to refer to the access level
         * @return the access level
         */
        public static AccessLevel getByEntellitrakNumber(final int theEntellitrakNumber){
            return Arrays.asList(values())
                    .stream()
                    .filter(accessLevel -> accessLevel.etkNumber == theEntellitrakNumber)
                    .findFirst()
                    .orElse(NO_ACCESS);
        }

        /**
         * Get the display value for this access level (this is used in the export file).
         *
         * @return The display string
         */
        public String getDisplay(){
            return display;
        }

        /**
         * Get the number which entellitrak uses internally to refer to this access level.
         *
         * @return The number
         */
        public int getEntellitrakNumber(){
            return etkNumber;
        }
    }


    /**
     * This enum represents Script Object Language types. This is needed because script objects are stored with the
     * appropriate extensions within the zip file.
     *
     * @author Zachary.Miller
     */
    public enum LanguageType{
        /** Beanshell. */
        BEANSHELL(1, "bsh"),
        /** Javascript. */
        JAVASCRIPT(2, "js"),
        /** Java. */
        JAVA(3, "java"),
        /** Groovy. */
        GROOVY(4, "groovy"),
        /** HTML. */
        HTML(5, "html"),
        /** SQL. */
        SQL(6, "sql"),
        /** CSS. */
        CSS(8, "css"),
        /** Unknown. */
        UNKNOWN(null, "");

        private final Integer etkNumber;
        private final String fileExtension;

        /**
         * Simple Constructor.
         *
         * @param theEntellitrakNumber the number entellitrak uses internally to refer to this language type
         * @param theFileExtension the file extension to be used in the zip file
         */
        LanguageType(final Integer theEntellitrakNumber, final String theFileExtension){
            etkNumber = theEntellitrakNumber;
            fileExtension = theFileExtension;
        }

        /**
         * Retrieve a Language Type based on the number entellitrak uses internally to refer to that Language Type.
         *
         * @param theEntellitrakNumber the number etnellitrak uses internally to refer to the language type
         * @return the Language Type, or other if it could not be found
         */
        public static LanguageType fromEntellitrakNumber(final Integer theEntellitrakNumber) {
            return Stream.of(values())
            .filter(languageType -> languageType.etkNumber == theEntellitrakNumber)
            .findAny()
            .orElse(UNKNOWN);
        }

        public String getFileExtension(){
            return fileExtension;
        }
    }
}
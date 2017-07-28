package net.micropact.aea.du.page.codeSearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;
import com.entellitrak.platform.DatabasePlatform;

/**
 * This page is used for finding references to specific Strings throughout entellitrak code blocks.
 * It not only searches Script Objects but also includes places such as Form Instructions,
 * View Instructions and Reports.
 *
 * I am trying to keep it so that the Code Search page does not have any other script object dependencies so that
 * it can be moved from one site to another easily.
 *
 * @author zmiller
 */
public class CodeSearchController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException{

        final TextResponse response = etk.createTextResponse();

        try {
            final String keyword = etk.getParameters().getSingle("keyword");
            final boolean isCaseSensitive = "1".equals(etk.getParameters().getSingle(
                    "caseSensitive"));
            final String workspaceParameter = etk.getParameters().getSingle(
                    "workspace");
            final long workspaceId = workspaceParameter != null
                    ? Long
                    .parseLong(workspaceParameter)
                    : getWorkspaceId(etk, etk
                            .getCurrentUser().getId());

                    if (keyword != null && !"".equals(keyword)) {

                        final Character escapeChar = '-';
                        final String escapedLikeTerm = escapeLike(etk, keyword, escapeChar);
                        final long trackingConfigId = getLatestTrackingConfigId(etk);

                        final Map<String, Object> queryParameterMap = new HashMap<>();
                        queryParameterMap.put("escapeCharacter", escapeChar.toString());
                        queryParameterMap.put("escapedKeyword", escapedLikeTerm);
                        queryParameterMap.put("trackingConfigId", trackingConfigId);
                        queryParameterMap.put("workspaceId", workspaceId);

                        final List<ResultGroup> resultGroups = new LinkedList<>();

                        // Script Objects
                        // Have to look into this query further. see whether the
                        // tracking_configuration_id part is correct or not

                        //ACL 07-08-2016 Refactor to call query 1x for performance improvement on SQLServer sites,
                        //instead of 1x per handler type.
                        final List<Map<String, Object>> scriptObjects = etk.createSQL((isSqlServer(etk) ? "WITH etk_packages (package_node_id, path) AS ( SELECT root.package_node_id, CONVERT(VARCHAR(MAX), root.name) AS path FROM etk_package_node root WHERE root.parent_node_id IS NULL UNION ALL SELECT children.package_node_id, CONVERT(VARCHAR(MAX), parents.path + '.' + children.name) AS path FROM etk_package_node children JOIN etk_packages parents ON children.parent_node_id = parents.package_node_id ) SELECT scriptObject.SCRIPT_ID, CASE WHEN scriptObject.package_node_id IS NOT NULL THEN etkPackages.path + '.' ELSE '' END + scriptObject.NAME FULL_NAME, scriptObject.HANDLER_TYPE, scriptObject.CODE FROM etk_script_object scriptObject LEFT JOIN etk_packages etkPackages ON etkPackages.package_node_id = scriptObject.package_node_id WHERE ( scriptObject.tracking_config_id = :trackingConfigId OR scriptObject.tracking_config_id IS NULL ) AND scriptObject.workspace_id = :workspaceId AND "
                                : "WITH etk_packages (package_node_id, path) AS (SELECT root.package_node_id, root.name AS path FROM etk_package_node root WHERE root.parent_node_id IS NULL UNION ALL SELECT children.package_node_id, parents.path || '.' || children.name AS path FROM etk_package_node children JOIN etk_packages parents ON children.parent_node_id = parents.package_node_id ) SELECT scriptObject.SCRIPT_ID, CASE WHEN scriptObject.package_node_id IS NOT NULL THEN etkPackages.path || '.' ELSE '' END || scriptObject.NAME FULL_NAME, scriptObject.HANDLER_TYPE, scriptObject.CODE FROM etk_script_object scriptObject LEFT JOIN etk_packages etkPackages ON etkPackages.package_node_id = scriptObject.package_node_id WHERE (scriptObject.tracking_config_id = :trackingConfigId OR scriptObject.tracking_config_id IS NULL) AND scriptObject.workspace_id = :workspaceId AND ")
                                + generateLikeClause(etk,
                                        "scriptObject.code",
                                        isCaseSensitive)
                                        + " ORDER BY FULL_NAME, SCRIPT_ID")
                                        .setParameter(queryParameterMap)
                                        .fetchList();

                        final HashMap<Integer, List<Map<String, Object>>> scriptsByHandlerType =
                        											new HashMap<>();
                        final HashMap<Integer, String> handlerNameMap = new HashMap<>();

                        for(final ScriptObjectHandlerType handlerType : ScriptObjectHandlerType.values()) {
                        	scriptsByHandlerType.put(handlerType.getId(), new ArrayList<Map<String, Object>>());
                        	handlerNameMap.put(handlerType.getId(), handlerType.getName());
                        }

                        if (scriptObjects != null) {
	                        for (final Map<String, Object> scriptObject : scriptObjects) {
	                        	final Integer scriptHandlerType = ((Number) scriptObject.get("HANDLER_TYPE")).intValue();

	                        	final List<Map<String, Object>> scriptListByHandler = scriptsByHandlerType.get(scriptHandlerType);
	                        	scriptListByHandler.add(scriptObject);
	                        	scriptsByHandlerType.put(scriptHandlerType, scriptListByHandler);
	                        }
                        }

                        for (final ScriptObjectHandlerType handlerType : ScriptObjectHandlerType.values()) {
                            resultGroups
                            .add(new ResultGroup(
                                    String.format("Script Objects (%s)", handlerNameMap.get(handlerType.getId())),
                                    convertMapsToResults(
                                    		scriptsByHandlerType.get(handlerType.getId()),
                                            "FULL_NAME",
                                            new SimpleAppendUrl(
                                                    "cfg.scriptobject.update.request.do?id=",
                                                    "SCRIPT_ID"), keyword, "CODE",
                                                    isCaseSensitive)));
                        }

                        // Form Instructions
                        final List<Map<String, Object>> dataForms = etk.createSQL("SELECT dataForm.DATA_FORM_ID, dataForm.NAME, dataForm.INSTRUCTIONS FROM etk_data_object dataObject JOIN etk_data_form dataForm ON dataForm.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId AND "
                                + generateLikeClause(etk,
                                        "dataForm.instructions",
                                        isCaseSensitive)
                                        + " ORDER BY NAME, INSTRUCTIONS, DATA_FORM_ID")
                                        .setParameter(queryParameterMap).fetchList();
                        resultGroups
                        .add(new ResultGroup(
                                "Data Forms",
                                convertMapsToResults(
                                        dataForms,
                                        "NAME",
                                        new SimpleAppendUrl(
                                                "cfg.ui.FormDesigner.do?method=getView&amp;id=",
                                                "DATA_FORM_ID"), keyword,
                                                "INSTRUCTIONS", isCaseSensitive)));

                        // View Instructions
                        final List<Map<String, Object>> dataViews = etk.createSQL("SELECT dataView.DATA_VIEW_ID, dataView.NAME, dataView.TEXT FROM etk_data_object dataObject JOIN etk_data_view dataView ON dataView.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId AND "
                                + generateLikeClause(etk,
                                        "dataView.text",
                                        isCaseSensitive)
                                        + " ORDER BY NAME, TEXT, DATA_VIEW_ID")
                                        .setParameter(queryParameterMap).fetchList();

                        resultGroups
                        .add(new ResultGroup(
                                "Data Views",
                                convertMapsToResults(
                                        dataViews,
                                        "NAME",
                                        new SimpleAppendUrl(
                                                "cfg.viewdesigner.do?method=requestUpdate&amp;id=",
                                                "DATA_VIEW_ID"), keyword,
                                                "TEXT", isCaseSensitive)));

                        // Reports
                        final List<Map<String, Object>> reports = etk
                                .createSQL(
                                        "SELECT SAVED_REPORT_ID, NAME, REPORT FROM etk_saved_report WHERE "
                                                + generateLikeClause(etk,
                                                        "report",
                                                        isCaseSensitive)
                                                        + " ORDER BY NAME, SAVED_REPORT_ID")
                                                        .setParameter(queryParameterMap).fetchList();
                        resultGroups
                        .add(new ResultGroup(
                                "Reports",
                                convertMapsToResults(
                                        reports,
                                        "NAME",
                                        new SimpleAppendUrl(
                                                "report.manager.ReportManager.do?method=open&amp;id=",
                                                "SAVED_REPORT_ID"), keyword,
                                                "REPORT", isCaseSensitive)));

                        // Queries
                        final List<Map<String, Object>> queries = etk
                                .createSQL(
                                        "SELECT QUERY_ID, NAME, SQL_SCRIPT FROM etk_query WHERE "
                                                + generateLikeClause(etk,
                                                        "sql_script",
                                                        isCaseSensitive)
                                                        + " ORDER BY NAME, QUERY_ID")
                                                        .setParameter(queryParameterMap).fetchList();
                        resultGroups.add(new ResultGroup("entelliSQL",
                                convertMapsToResults(queries, "NAME",
                                        new SimpleAppendUrl(
                                                "entellisql.update.request.do?id=",
                                                "QUERY_ID"), keyword, "SQL_SCRIPT",
                                                isCaseSensitive)));

                        /*
                         * The Database View code does not work because USER_VIEWS.text is a Long column in Oracle
                         *
                         * // Database Views
                         * final String viewsQuery = isSqlServer(etk) ? "SELECT table_name NAME, view_definition TEXT FROM INFORMATION_SCHEMA.VIEWS WHERE "
                                                                        + generateLikeClause(etk, "view_definition", isCaseSensitive)
                                                                        +" ORDER BY NAME"
                                                                   : "SELECT VIEW_NAME, TEXT FROM USER_VIEWS WHERE "
                                                                       + generateLikeClause(etk, "text", isCaseSensitive)
                                                                       + " ORDER BY VIEW_NAME";
                        final List<Map<String, Object>> views = etk.createSQL(viewsQuery)
                                .setParameter(queryParameterMap)
                                .fetchList();
                        resultGroups.add(new ResultGroup("Database Views",
                                convertMapsToResults(views, "NAME",
                                        new NullUrl(),
                                        keyword,
                                        "TEXT",
                                        isCaseSensitive)));*/

                        // Stored Procedures
                        resultGroups.add(new ResultGroup("Stored Procedures",
                                convertMapsToResults(getDatabaseObject(etk,
                                                                       isCaseSensitive,
                                                                       queryParameterMap,
                                                                       DatabaseObjectType.STORED_PROCEDURE),
                                        "NAME",
                                        new NullUrl(),
                                        keyword,
                                        "TEXT",
                                        isCaseSensitive)));

                        // Database Functions
                        resultGroups.add(new ResultGroup("Database Functions",
                                convertMapsToResults(getDatabaseObject(etk,
                                                                        isCaseSensitive,
                                                                        queryParameterMap,
                                                                        DatabaseObjectType.FUNCTION),
                                        "NAME",
                                        new NullUrl(),
                                        keyword,
                                        "TEXT",
                                        isCaseSensitive)));

                        // Database Views
                        resultGroups.add(new ResultGroup("Database Views",
                                convertMapsToResults(getDatabaseViews(etk,
                                                                     isCaseSensitive,
                                                                     keyword),
                                        "NAME",
                                        new NullUrl(),
                                        keyword,
                                        "TEXT",
                                        isCaseSensitive)));

                        response.put("resultGroups", resultGroups);
                    }

                    response.put("workspaces",
                            etk.createSQL("SELECT WORKSPACE_ID, WORKSPACE_NAME FROM etk_workspace ORDER BY workspace_name, workspace_id")
                            .fetchJSON());
                    response.put("workspaceId", workspaceId);
                    response.put("keyword", keyword);
                    response.put("caseSensitive", isCaseSensitive);
                    response.put("sensitivityChangeAllowed", etk.getPlatformInfo()
                            .getDatabasePlatform().equals(DatabasePlatform.ORACLE));
                    response.put("esc", new StringEscapeUtils());
                    response.setContentType(ContentType.HTML);
                    return response;
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * This enum keeps track of the different types of database objects which we can query for.
     *
     * @author zmiller
     */
    private enum DatabaseObjectType {
        STORED_PROCEDURE("PROCEDURE", new String[]{"P"}),
        FUNCTION("FUNCTION", new String[]{"FN", "TF"});

        private final List<String> oracleIdentifier;
        private final List<String> sqlServerIdentifiers;

        /**
         * Constructor for DatabaseObjectType.
         *
         * @param theOracleIdentifier The identifier that Oracle uses in all_source
         * @param theSqlServerIdentifiers The identifiers that SQL Server uses in sys.objects.type
         */
        DatabaseObjectType(final String theOracleIdentifier, final String[] theSqlServerIdentifiers){
            oracleIdentifier = Arrays.asList(theOracleIdentifier);
            sqlServerIdentifiers = Arrays.asList(theSqlServerIdentifiers);
        }

        /**
         * Gets the identifiers that Oracle uses in all_source, or SQL Server uses in sys.objects.type.
         *
         * @param etk entellitrak execution context
         * @return The identifiers that Oracle uses in all_source, or SQL Server uses in sys.objects.type
         */
        public List<String> getDatabaseIdentifiers(final ExecutionContext etk){
            return isSqlServer(etk) ? sqlServerIdentifiers : oracleIdentifier;
        }
    }

    /**
     * This function searches for text for objects within the database such as stored procedures.
     *
     * @param etk entellitrak execution context
     * @param isCaseSensitive Whether the search is case sensitive
     * @param queryParameterMap A map containing the default parameters such as those required by
     *          {@link #generateLikeClause(ExecutionContext, String, boolean)}
     * @param databaseObjectType The type of database object to search for
     * @return A list of matching results with "NAME" and "TEXT" as the map keys
     */
    private static List<Map<String, Object>> getDatabaseObject(final ExecutionContext etk,
            final boolean isCaseSensitive,
            final Map<String, Object> queryParameterMap,
            final DatabaseObjectType databaseObjectType){

        final List<Map<String, Object>> returnResults;

        final List<String> databaseIdentifiers = databaseObjectType.getDatabaseIdentifiers(etk);

        if(isSqlServer(etk)){
            returnResults = etk.createSQL("SELECT objects.name NAME, modules.definition TEXT FROM sys.sql_modules modules JOIN sys.objects objects ON modules.object_id = objects.object_id WHERE objects.type IN(:databaseIdentifiers) AND " +
                    generateLikeClause(etk, "modules.definition", isCaseSensitive)
                    + " ORDER BY NAME, TEXT")
                    .setParameter(queryParameterMap)
                    .setParameter("databaseIdentifiers", databaseIdentifiers)
                    .fetchList();
        }else{
            final List<Map<String, Object>> rawQueryResults = etk.createSQL("SELECT NAME, TEXT FROM all_source allSource WHERE allSource.owner = USER AND allSource.type = :databaseIdentifiers AND EXISTS( SELECT * FROM all_source matchingSource WHERE matchingSource.owner = allSource.owner AND matchingSource.name = allSource.name AND matchingSource.type = allSource.type AND "
                    + generateLikeClause(etk,
                            "matchingSource.text",
                            isCaseSensitive) + ") ORDER BY name, line")
                            .setParameter(queryParameterMap)
                            .setParameter("databaseIdentifiers", databaseIdentifiers)
                            .fetchList();

            returnResults = groupRawOracleQueryResults(rawQueryResults);
        }

        return returnResults;
    }


    /**
     * Gets all database views which contain keyword in their definitions.
     *
     * @param etk entellitrak execution context
     * @param isCaseSensitive whether the case should be case sensitive
     * @param keyword the keyword to search for
     * @return A list of matching results with "NAME" and "TEXT" keys for the maps
     */
    private static List<Map<String, Object>> getDatabaseViews(final ExecutionContext etk,
                                                             final boolean isCaseSensitive,
                                                             final String keyword){
    	List<Map<String, Object>> rawQueryResults = null;
    	final List<Map<String, Object>> matchingResults = new ArrayList<>();
    	final String caseTypedKeyword = (isCaseSensitive ? keyword : keyword.toLowerCase()).trim().replaceAll(" +", " ");

        if(isSqlServer(etk)){
        	 rawQueryResults =
             		etk.createSQL("SELECT objects.name NAME, modules.definition TEXT FROM sys.sql_modules modules JOIN sys.objects objects ON modules.object_id = objects.object_id WHERE objects.type IN('V') order by name")
                        .fetchList();
        } else {
            rawQueryResults =
            		etk.createSQL("select VIEW_NAME as NAME, TEXT from user_views order by name")
                       .fetchList();
        }

        if (rawQueryResults != null) {
	        String text = null;

	        for (final Map<String, Object> aRawResult : rawQueryResults) {
	        	if (aRawResult.get("TEXT") != null) {
	        	    text = (isCaseSensitive ? ((String) aRawResult.get("TEXT")) : ((String) aRawResult.get("TEXT")).toLowerCase())
	        	    		.trim().replaceAll(" +", " ");
	        	    aRawResult.put("TEXT", text);

	        	    if (text.contains(caseTypedKeyword)) {
	        	    	matchingResults.add(aRawResult);
	        	    }
	        	}
	        }
        }

        return groupRawOracleQueryResults(matchingResults);
    }


    /**
     * Within the all_sources table, oracle stores each line as a separate record. This function will combine all the
     * lines into one, however it expects that the input list has already been sorted primarily by NAME and secondarily
     * by TEXT.
     *
     * @param rawQueryResults The raw query results from etk.createSQL
     * @return A list of results with "NAME" and "TEXT" keys for th. maps
     */
    private static List<Map<String, Object>>
    groupRawOracleQueryResults(final List<Map<String, Object>> rawQueryResults) {
        final LinkedList<Map<String, Object>> returnList = new LinkedList<>();

        int startIndex;
        int currentIndex = 0;

        while(currentIndex < rawQueryResults.size()){
            startIndex = currentIndex;

            final String name = (String) rawQueryResults.get(startIndex).get("NAME");
            final StringBuilder textBuilder = new StringBuilder((String) rawQueryResults.get(startIndex).get("TEXT"));

            currentIndex = startIndex + 1;
            while(currentIndex < rawQueryResults.size() && name.equals(rawQueryResults.get(currentIndex).get("NAME"))){
                textBuilder.append(rawQueryResults.get(currentIndex).get("TEXT"));
                currentIndex = currentIndex + 1;
            }

            final Map<String, Object> completeResult = new HashMap<>();
            completeResult.put("NAME", name);
            completeResult.put("TEXT", textBuilder.toString());
            returnList.add(completeResult);
        }

        return returnList;
    }

    /**
     * This function is copied here even though it is in the Utility because code search is a special page in that I
     * do not want to create any additional dependencies.
     *
     * @param etk Entellitrak execution context.
     * @return Whether the database platform is SQL Server.
     */
    static boolean isSqlServer(final ExecutionContext etk){
        return etk.getPlatformInfo().getDatabasePlatform().equals(DatabasePlatform.SQL_SERVER);
    }


    /**
     * Converts maps representing matching result records into an actual {@link Result} objects.
     *
     * @param objects List of objects where each entry represents a matched object.
     * @param nameKey The key in the Map which contains the name of the result
     * @param url The object which will determine the URL of the resource given an object
     * @param keyword The keyword which was searched for
     * @param codeKey The key in the objects maps which holds the code.
     * @param isCaseSensitive Whether the search is case sensitive
     * @return A list of results which represenst the passed in list of objects.
     */
    private List<Result> convertMapsToResults(
            final List<Map<String, Object>> objects, final String nameKey, final IURL url,
            final String keyword, final String codeKey, final boolean isCaseSensitive) {
        final List<Result> results = new LinkedList<>();
        for (final Map<String, Object> object : objects) {
            results.add(new Result((String) object.get(nameKey),
                    url.getUrl(object),
                    keyword,
                    (String) object.get(codeKey),
                    isCaseSensitive));
        }
        return results;
    }

    /**
     * Escapes the special characters in an SQL LIKE clause such as "%" and "_".
     *
     * @param etk entellitrak execution context
     * @param string The string which is to be escaped.
     * @param escapeChar The character which is to be used as the escape character for the LIKE clause.
     * @return The escaped LIKE clause.
     */
    private static String escapeLike(final ExecutionContext etk, final String string, final Character escapeChar) {
        if (string == null) {
            return "";
        } else {

            final List<Character> specialCharacters = isSqlServer(etk)
                    ? Arrays.asList(escapeChar, '%', '_', '[')
                    : Arrays.asList(escapeChar, '%', '_');

                    String returnString = string;
                    for(final Character specialCharacter : specialCharacters){
                        returnString = returnString.replaceAll(Pattern.quote(specialCharacter.toString()),
                                escapeChar.toString() + specialCharacter.toString());
                    }

                    return returnString;
        }
    }

    /**
     * Determines the workspace id that the desired user is using for execution.
     *
     * @param etk entellitrak execution context.
     * @param userId The id of the desired user.
     * @return The Workspace Id of the workspace that the specified user is using for execution.
     * @throws IncorrectResultSizeDataAccessException
     *     If there was an underlying {@link IncorrectResultSizeDataAccessException}.
     */
    private static long getWorkspaceId(final PageExecutionContext etk, final Long userId)
            throws IncorrectResultSizeDataAccessException{
        return Long
                .parseLong(etk.createSQL(isSqlServer(etk) ? "SELECT ISNULL((SELECT workspace_id FROM etk_workspace WHERE user_id = :userId), (SELECT workspace_id FROM etk_workspace WHERE workspace_name = :defaultWorkspaceName))"
                                                            : "SELECT NVL((SELECT workspace_id FROM etk_workspace WHERE user_id = :userId), (SELECT workspace_id FROM etk_workspace WHERE workspace_name = :defaultWorkspaceName)) FROM DUAL")
                                                            .setParameter("userId", userId)
                                                            .setParameter("defaultWorkspaceName", "system")
                                                            .fetchString());
    }

    /**
     * Returns the tracking configuration id for data whih will be deployed AFTER the next apply changes.
     *
     * @param etk entellitrak execution context.
     * @return The tracking configuration id.
     * @throws IncorrectResultSizeDataAccessException
     *     If there was an underlying {@link IncorrectResultSizeDataAccessException}.
     */
    private static long getLatestTrackingConfigId(final PageExecutionContext etk)
            throws IncorrectResultSizeDataAccessException {
        return Long.parseLong(etk.createSQL("SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = (SELECT MAX(config_version) FROM etk_tracking_config)")
                .fetchString());
    }

    /**
     * Generates a clause which indicates whether the the specified database column is LIKE a specified String.
     * The clause generated will expect that :escapedKeyword will be bound to the term being searched and that
     * :escapeCharacter will be bound to the character which is to be used for escaping the LIKE clause.
     *
     * @param etk Entellitrak Execution Context.
     * @param columnName The name of the database column which is being searched.
     * @param isCaseSensitive Whether the search is case sensitive.
     * @return A string representing the LIKE clause.
     */
    private static String generateLikeClause(final ExecutionContext etk,
            final String columnName,
            final boolean isCaseSensitive) {
        if (isCaseSensitive) {
            return columnName
                    + (isSqlServer(etk) ? " LIKE '%' + :escapedKeyword + '%' ESCAPE :escapeCharacter"
                                          : " LIKE '%' || :escapedKeyword || '%' ESCAPE :escapeCharacter");
        } else {
            return
                    isSqlServer(etk) ? (columnName + " LIKE '%' + :escapedKeyword + '%' ESCAPE :escapeCharacter") :
                        ("LOWER(" + columnName + ") LIKE '%' || LOWER(:escapedKeyword) || '%' ESCAPE :escapeCharacter");
        }
    }

    /**
     * This interface represents something which can turn a {@link Map} generated by the search queries into a URL.
     *
     * @author zmiller
     */
    private interface IURL {

        /**
         * This method converts an object which is located at a URL into the actual URL it is located at.
         *
         * @param object A map representing a resource located at a particular URL.
         * @return The URL of the object
         */
        String getUrl(Map<String, Object> object);
    }

    /**
     * This implementation of {@link IURL} will generate the URL from a static prefix and a suffix which is
     * determined by a particular key in a map.
     * Currently all URLs for objects being searched can be expressed this way.
     *
     * @author zmiller
     */
    private class SimpleAppendUrl implements IURL {
        private final String prefix;
        private final String endingKey;

        /**
         * This will use prefix as the static prefix and endingKey as the key for the map which holds the suffix.
         *
         * @param urlPrefix The static URL prefix to use.
         * @param urlEndingKey The key for a map which will be used to extract the suffix of the URL.
         */
        SimpleAppendUrl(final String urlPrefix, final String urlEndingKey) {
            prefix = urlPrefix;
            endingKey = urlEndingKey;
        }

        @Override
        public String getUrl(final Map<String, Object> object) {
            return prefix + object.get(endingKey);
        }
    }

    /**
     * This will be used for URLs which do not go anywhere (such as those linking to stored procedures).
     *
     * @author zmiller
     */
    private class NullUrl implements IURL{

        /**
         * The default constructor is private, but we need to make it default for CodeSearchController to access it.
         */
        NullUrl(){}

        @Override
        public String getUrl(final Map<String, Object> object) {
            return "javascript:false;";
        }
    }

    /**
     * This class represents a group of results such as all results related to Reports.
     * This could instead be an interface that each result type could implement,
     * but the way this is seems easiest for now.
     * @author zmiller
     */
    public class ResultGroup {

        /**
         * The title of the group. For instance "Data Forms".
         */
        private final String title;
        /**
         * The matching results for this group.
         */
        private final List<Result> results;

        /**
         * Constructor for ResultGroup.
         *
         * @param groupTitle The title of the group.
         * @param groupResults The matching code repositories in the group.
         */
        public ResultGroup(final String groupTitle, final List<Result> groupResults) {
            title = groupTitle;
            results = groupResults;
        }

        /**
         * Gets the title of the group.
         *
         * @return The title of the group.
         */
        public String getTitle() {
            return title;
        }

        /**
         * Gets the results within the group.
         *
         * @return The matching results in this group.
         */
        public List<Result> getResults() {
            return results;
        }
    }

    /**
     * This class represents a particular matching block of code.
     * Within the matching block it will have multiple matching keywords.
     * The matches are the actual lines within the code block which match the keyword.
     * @author zmiller
     */
    public class Result {
        /**
         * The name of this block of code.
         */
        private final String name;
        /**
         * The URL where this block of code is located within the system.
         */
        private final String url;
        /**
         * The matching lines. Matches will contain actual HTML where specific matched keywords will be wrapped with
         * the CSS class "highlight";
         */
        private final List<String> matches;

        /**
         * Constructor for Result.
         *
         * @param resultName The name of the code block. The equivalent of a script object or report name.
         * @param resultUrl The URL of where to access the code block
         * @param searchKeyword The keyword which is being searhed for
         * @param resultText The text of the code block
         * @param isSearchCaseSensitive Whether the search is case sensitive
         */
        public Result(final String resultName, final String resultUrl, final String searchKeyword,
                final String resultText, final boolean isSearchCaseSensitive) {
            name = resultName;
            url = resultUrl;
            matches = findAllMatches(searchKeyword, resultText, isSearchCaseSensitive);
        }

        /**
         * Gets the name of the result.
         *
         * @return The name of the code block.
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the URL of where to access the code block.
         *
         * @return The URL of where to access the code block
         */
        public String getUrl() {
            return url;
        }

        /**
         * Gets the list of matching lines of code.
         *
         * @return A list of the matching lines as HTML fragments.
         *    The matching word itself will be contained in an HTML span called with class=highlight.
         */
        public List<String> getMatches() {
            return matches;
        }

        /**
         * This method takes a block of text, escapes the HTML and wraps items that match the keyword in an
         * HTML span with class=highlight.
         *
         * @param keyword The keyword which is being searched for.
         * @param text The text which the keyword is located within.
         * @param isCaseSensitive Whether the search is case sensitive.
         * @return The list of matching lines.
         */
        private List<String> findAllMatches(final String keyword, final String text,
                final boolean isCaseSensitive) {
            // It probably makes more sense to escape these after the matches are found
            final String escapedKeyword = StringEscapeUtils.escapeHtml(keyword);
            // It probably makes more sense to escape these after the matches are found
            final String escapedText = StringEscapeUtils.escapeHtml(text);

            final Pattern pattern; // The pattern depends on case sensitivity

            if (isCaseSensitive) {
                pattern = Pattern.compile(Pattern.quote(escapedKeyword), 0);
            } else {
                pattern = Pattern.compile(Pattern.quote(escapedKeyword),
                        Pattern.CASE_INSENSITIVE);
            }

            final List<String> lineMatches = new ArrayList<>();
            final String[] lines = escapedText.split("\r\n|\r|\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (pattern.matcher(line).find()) {
                    final int lineNumber = i + 1;
                    line = lineNumber
                            + ": "
                            + pattern.matcher(line).replaceAll(
                                    "<span class='highlight'>$0</span>");
                    lineMatches.add(line);
                }
            }
            return lineMatches;
        }
    }

    /**
     * This enum represents the possible type of Script Object Handlers which entellitrak supports.
     * The values can be found in com.micropact.entellitrak.workspace.model.HandlerType
     *
     * @author zmiller
     */
    private enum ScriptObjectHandlerType{

        NONE(0, "None"),
        ADVANCED_SEARCH_EVENT_HANDLER(1, "AdvancedSearchEventHandler"),
        DISPLAY_MAPPING_HANDLER(2, "DisplayMappingHandler"),
        FORM_ELEMENT_EVENT_HANDLER(3, "FormElementEventHandler"),
        FORM_EVENT_HANDLER(4, "FormEventHandler"),
        JOB_HANDLER(5, "JobHandler"),
        LOOKUP_HANDLER(6, "LookupHandler"),
        PAGE_CONTROLLER(7, "PageController"),
        TRANSITION_HANDLER(8, "TransitionHandler"),
        USER_EVENT_HANDLER(9, "UserEventHandler"),
        SCAN_EVENT_HANDLER(10, "ScanEventHandler"),
        STEP_BASED_PAGE(11, "StepBasedPageHandler"),
        FORM_EXECUTION_HANDLER(12, "FormExecutionHandler"),
        DATA_OBJECT_EVENT_HANDLER(13, "DataObjectEventHandler"),
        APPLY_CHANGES_EVENT_HANDLER(14, "ApplyChangesEventHandler"),
        CHANGE_HANDLER(15, "ChangeHandler"),
        CLICK_HANDLER(16, "ClickHandler"),
        NEW_HANDLER(17, "NewHandler"),
        READ_HANDLER(18, "ReadHandler"),
        SAVE_HANDLER(19, "SaveHandler"),
        OFFLINE_SYNC_HANDLER(20, "OfflineSyncHandler"),
        RDO_EVENT_HANDLER(21, "ReferenceObjectEventHandler"),
        ENDPOINT_HANDLER(22, "EndpointHandler"),
        DEPLOYMENT_HANDLER(23, "DeploymentHandler"),
        ELEMENT_FILTER_HANDLER(24, "ElementFilterHandler"),
        RECORD_FILTER_HANDLER(25, "RecordFilterHandler");

        /**
         * The id which entellitrak internally uses to represent the Script Object Handler.
         */
        private final int id;
        /**
         * The name entellitrak uses to represent the Script Object Handler.
         */
        private final String name;

        /**
         * Constructor for ScriptObjectHandlerType.
         *
         * @param scriptObjectHandlerTypeId The id which entellitrak internally uses to represent the
         *  Script Object Handler.
         * @param scriptObjectHandlerTypeName The name entellitrak uses to represent the Script Object Handler.
         */
        ScriptObjectHandlerType(final int scriptObjectHandlerTypeId, final String scriptObjectHandlerTypeName){
            id = scriptObjectHandlerTypeId;
            name = scriptObjectHandlerTypeName;
        }

        /**
         * Gets the id entellitrak internally uses to represent the Script Object Handler.
         *
         * @return The id entellitrak internally uses to represent the String Object Handler.
         */
        public int getId(){
            return id;
        }

        /**
         * Gets the name entellitrak uses to represent the Script Object Handler.
         *
         * @return The name entellitrak uses to represent the Script Object Handler.
         */
        public String getName(){
            return name;
        }
    }
}

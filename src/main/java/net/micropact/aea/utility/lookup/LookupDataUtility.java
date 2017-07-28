/**
 *
 * Utility methods used by Live Search.
 *
 * administrator 09/15/2014
 **/

package net.micropact.aea.utility.lookup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.micropact.aea.core.utility.StringEscapeUtils;
import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.DataObjectType;
import net.micropact.aea.utility.LookupSourceType;
import net.micropact.aea.utility.SystemObjectDisplayFormat;
import net.micropact.aea.utility.SystemObjectType;
import net.micropact.aea.utility.Utility;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataAccessException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.legacy.util.DateUtility;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;
import com.micropact.entellitrak.cfg.model.DataType;

/**
 * Main utility class for the Live Search Component.
 *
 * @author aclee
 *
 */
public class LookupDataUtility {
	private ExecutionContext etk = null;

    // Thread local variable containing this thread's previous lookup ID value.
    private static final ThreadLocal<Number> previousLookupId =
        new ThreadLocal<Number>() {
            @Override protected Number initialValue() {
                return new Integer(-1);
        }
    };

    // Thread local variable containing this thread's recursive depth counter.
    private static final ThreadLocal<Integer> recursionCounter =
        new ThreadLocal<Integer>() {
            @Override protected Integer initialValue() {
                return new Integer(1);
        }
    };

	/**
	 * Main constructor.
	 *
	 * @param thePageContext The PageExecutionContext.
	 */
	public LookupDataUtility (final ExecutionContext thePageContext) {
		this.etk = thePageContext;
	}

	/**
	 * Get a null-safe column name.
	 *
	 * @param colName the column name.
	 * @return null-safe column name.
	 */
	private String getColName(final String colName) {
		return StringUtility.isBlank(colName) ? "ID" : colName;
	}

	/**
	 * Gets the java long value representation of a BigDecimal / Long.
	 *
	 * @param aValue BigDecimal or Long value.
	 * @return long value representation of a BigDecimal / Long.
	 */
	private int getIntVal (final Object aValue) {

		if (aValue == null) {
			return -1;
		} else if (aValue instanceof BigDecimal) {
			return ((BigDecimal) aValue).intValueExact();
		} else if (aValue instanceof Number) {
			return ((Number) aValue).intValue();
		}

		return -1;
	}

	/**
	 * Replace select XYZ within a query with SELECT TOP.
	 *
	 * @param aString A Query
	 * @return The modified query.
	 */
	private String peformSelectReplacements(final String aString) {
		if (aString == null) {
			return null;
		}

		final Pattern p = Pattern.compile("(select)(\\s)+(?!top)", Pattern.CASE_INSENSITIVE);
		final Matcher m = p.matcher(aString);
		return m.replaceAll("SELECT TOP 2147483647 ");
	}


	/**
	 * Returns the search result table header.
	 *
	 * @param userHeaderParams Table Column Names
	 * @return HTML table header.
	 */
	public String writeTableHeader (final List<String> userHeaderParams) {
		final StringBuilder sb = new StringBuilder();

		sb.append("<thead><tr>");

		if (userHeaderParams != null) {
			for (final String userHeaderParam : userHeaderParams) {
				final String[] columnVars = userHeaderParam.split(":");

				if ((columnVars != null) && (columnVars.length == 2)) {
					sb.append("<th scope=\"col\" >");
					//sb.append("<th>");
					sb.append(getStringValue(columnVars[1]));
					sb.append("</th>");
				} else {
					throw new IllegalArgumentException("Error writing table header: \""
							+  userHeaderParam + "\" column header is malformed.");
				}
			}
		}

		sb.append("</tr></thead>");

		return sb.toString();
	}

	/**
	 * Converts a list of table rows headers to case insenstive.
	 *
	 * @param tableRow A list of table rows.
	 * @return A case-insensitive list of table rows.
	 */
	private Map<String, String> getCaseSensitiveHeaders (final Map<String, Object> tableRow) {

		final Map<String, String> caseSensitiveHeaders = new HashMap<String, String>();

		if (tableRow == null) {
			return caseSensitiveHeaders;
		}

		for (final String aKey : tableRow.keySet()) {
			caseSensitiveHeaders.put(aKey.toLowerCase(), aKey);
		}

		return caseSensitiveHeaders;
	}

	/**
	 * Returns an escaped HTMP string.
	 *
	 * @param anObject An Object.
	 * @return The escaped string.
	 */
	private String getStringValue(final Object anObject) {
		if (anObject == null) {
			return "";
		} else {
			return StringEscapeUtils.escapeHtml(anObject.toString());
		}
	}


	/**
	 * Writes the result body for the search result.
	 *
	 * @param userHeaderParams User header row names.
	 * @param tableRows Table row data.
	 * @param dataElementId The data element ID for the row in question.
	 * @return String HTML result table.
	 */
	public String writeTableBody (final List<String> userHeaderParams,
			final List<Map<String, Object>> tableRows, final String dataElementId) {
		final StringBuilder sb = new StringBuilder();

		sb.append("<tbody>");

		Map<String, String> csh = null;

		for (final Map<String, Object> tableRow : tableRows) {

			if (csh == null) {
				csh = getCaseSensitiveHeaders (tableRow);
			}

			sb.append("<tr id=\"TR_");
			sb.append(dataElementId);
			sb.append("_");
			sb.append(getStringValue(tableRow.get(csh.get("value"))));
			sb.append("\" name=\"TR_");
			sb.append(dataElementId);
			sb.append("_");
			sb.append(getStringValue(tableRow.get(csh.get("display"))));
			sb.append("\" style=\"cursor:pointer;\">");

			if (userHeaderParams != null) {
				for (final String userHeaderParam : userHeaderParams) {
					final String[] columnVars = userHeaderParam.split(":");

					if ((columnVars != null) && (columnVars.length == 2)) {
						sb.append("<td><a class=\"searchTableClickableLink\" href=\"javascript:void(0)\" ");
						sb.append("title=\"Select search result value ");
						sb.append(getStringValue(tableRow.get(csh.get(columnVars[0].toLowerCase()) )));
						sb.append("\" >");
						sb.append(getStringValue(tableRow.get(csh.get(columnVars[0].toLowerCase()) )));
						sb.append("</a></td>");
					} else {
						throw new IllegalArgumentException("Error writing live search table row: \""
								+  userHeaderParam + "\" column header is malformed.");
					}
				}
			} else {
				throw new IllegalArgumentException("Error writing live search table rows: table headers are required.");
			}

			sb.append("</tr>");
		}

		sb.append("</tbody>");

		return sb.toString();
	}

	/**
	 * Creates helper view AEA_LS_DATA_ELEMENT_VIEW.
	 * @throws IncorrectResultSizeDataAccessException
	 *         If there is an underlying {@link IncorrectResultSizeDataAccessException}
	 */
	private void createView() throws IncorrectResultSizeDataAccessException {

		String createOrAlter = "CREATE";

		if (Utility.isSqlServer(etk)) {
			final int existingViewCount =
				etk.createSQL(
					"SELECT count(*) FROM sys.views WHERE object_id = OBJECT_ID(N'AEA_LS_DATA_ELEMENT_VIEW')")
				.fetchInt();

			if (existingViewCount >= 1) {
				createOrAlter = "ALTER";
			}
		}

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
			+ "   NULL AS DATA_ELEMENT_BUSINESS_KEY "
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
	}

	/**
	 * Gets the lookup query for a data object / data element id.
	 *
	 * @param dataFormId The data form id.
	 * @param dataElementName The data element name.
	 * @param aLookupExecutionContext context to pass to the lookup handler
	 * @return The SQL query for the data object with the given ID.
	 * @throws IncorrectResultSizeDataAccessException Incorrect Result Size Data Access Exception.
	 * @throws DataAccessException Data Access Exception.
	 * @throws InstantiationException Instantiation Exception.
	 * @throws IllegalAccessException Illegal Access Exception.
	 * @throws ClassNotFoundException Class Not Found Exception.
	 * @throws ApplicationException Application Exception.
	 */
	public String getLookupQuery (final Long dataFormId,
			                      final String dataElementName,
			                      final LookupExecutionContext aLookupExecutionContext)
														throws IncorrectResultSizeDataAccessException,
																DataAccessException,
																InstantiationException,
																IllegalAccessException,
																ClassNotFoundException,
																ApplicationException {
		return getLookupQuery (dataFormId, dataElementName, null, aLookupExecutionContext, true);
	}


	/**
	 * Gets the lookup query for a data element by business key. If multiple, pulls the first result.
	 * Returns data object queries with no start/stop date filtering on the query.
	 *
	 * @param dataElementBusinessKey The data element business key.
	 * @param aLookupExecutionContext context to pass to the lookup handler
	 * @return The SQL query for the data object with the given ID.
	 * @throws IncorrectResultSizeDataAccessException Incorrect Result Size Data Access Exception.
	 * @throws DataAccessException Data Access Exception.
	 * @throws InstantiationException Instantiation Exception.
	 * @throws IllegalAccessException Illegal Access Exception.
	 * @throws ClassNotFoundException Class Not Found Exception.
	 * @throws ApplicationException Application Exception.
	 */
	public String getLookupQuery (final String dataElementBusinessKey,
			                      final LookupExecutionContext aLookupExecutionContext)
														throws IncorrectResultSizeDataAccessException,
																DataAccessException,
																InstantiationException,
																IllegalAccessException,
																ClassNotFoundException,
																ApplicationException {
		return getLookupQuery (null, null, dataElementBusinessKey, aLookupExecutionContext, false);
	}

	/**
	 * Gets the lookup query for a data object / data element id or by a data element by business key.
	 *
	 * @param dataFormId The data form id.
	 * @param dataElementName The data element name.
	 * @param dataElementBusinessKey The data element business key.
	 * @param aLookupExecutionContext context to pass to the lookup handler
	 * @param filterStartStopDate whether to add stop/start date filtering to data object lookup queries.
	 *
	 * @return The SQL query for the data object with the given ID.
	 * @throws IncorrectResultSizeDataAccessException Incorrect Result Size Data Access Exception.
	 * @throws DataAccessException Data Access Exception.
	 * @throws InstantiationException Instantiation Exception.
	 * @throws IllegalAccessException Illegal Access Exception.
	 * @throws ClassNotFoundException Class Not Found Exception.
	 * @throws ApplicationException Application Exception.
	 */
	private String getLookupQuery (final Long dataFormId,
			                       final String dataElementName,
			                       final String dataElementBusinessKey,
			                       final LookupExecutionContext aLookupExecutionContext,
			                       final boolean filterStartStopDate
			                      )
			throws IncorrectResultSizeDataAccessException,
			DataAccessException,
			InstantiationException,
			IllegalAccessException,
			ClassNotFoundException,
			ApplicationException {
		final boolean isSqlServer = Utility.isSqlServer(etk);

		String selectClause = null;
		String whereClause = null;

		//Determine whether to search by dataFormId/dataElementName combo or to return the first result for a dataElementBusinessKey
		if ((dataFormId == null) && (dataElementName == null)) {
			if (isSqlServer) {
				selectClause = "SELECT TOP 1";
				whereClause = "WHERE DATA_ELEMENT_BUSINESS_KEY = :dataElementBusinessKey";
			} else {
				selectClause = "SELECT";
				whereClause = "WHERE DATA_ELEMENT_BUSINESS_KEY = :dataElementBusinessKey and rownum = 1";
			}

		} else {
			selectClause = "SELECT";
			whereClause = "WHERE DATA_ELEMENT_NAME = :dataElementName and DATA_FORM_ID = :dataFormId";
		}

		String cacheKey = "aeaLsDataElementViewCache&dataFormId=" +
		                  (dataFormId == null ? "" : dataFormId) +
		                  "&dataElementName=" +
		                  (StringUtility.isBlank(dataElementName) ? "" : dataElementName) +
		                  "&dataElementBusinessKey=" +
		                  (StringUtility.isBlank(dataElementBusinessKey) ? "" : dataElementBusinessKey);

		@SuppressWarnings("unchecked")
		Map<String, Object> queryResult = (Map<String, Object>) etk.getCache().load(cacheKey);

		if (queryResult == null) {
		try {
			queryResult = etk.createSQL(
					  selectClause
					+ " LOOKUP_TYPE, TABLE_NAME, VALUE_COLUMN, DISPLAY_COLUMN, "
					+ "SYSTEM_OBJECT_TYPE, SYSTEM_OBJECT_DISPLAY_FORMAT, ENABLE_CACHING, "
					+ "ORDER_COLUMN, START_DATE_COLUMN, END_DATE_COLUMN, SCRIPT_JAVA_ID "
					+ "FROM AEA_LS_DATA_ELEMENT_VIEW "
					+ whereClause
					)
					.returnEmptyResultSetAs(null)
					.setParameter("dataFormId", dataFormId)
					.setParameter("dataElementName", dataElementName)
					.setParameter("dataElementBusinessKey", dataElementBusinessKey)
					.fetchMap();
		} catch (final Exception e) {
			try {
				//Attempt to recreate view if there was an exception, then try again (maybe the view was out of date...)
				createView();

				queryResult = etk.createSQL(
						 selectClause
						+ " LOOKUP_TYPE, TABLE_NAME, VALUE_COLUMN, DISPLAY_COLUMN, "
						+ "SYSTEM_OBJECT_TYPE, SYSTEM_OBJECT_DISPLAY_FORMAT, ENABLE_CACHING, "
						+ "ORDER_COLUMN, START_DATE_COLUMN, END_DATE_COLUMN, SCRIPT_JAVA_ID "
						+ "FROM AEA_LS_DATA_ELEMENT_VIEW "
						+ whereClause
						)
						.returnEmptyResultSetAs(null)
						.setParameter("dataFormId", dataFormId)
						.setParameter("dataElementName", dataElementName)
						.setParameter("dataElementBusinessKey", dataElementBusinessKey)
						.fetchMap();
			} catch (final IncorrectResultSizeDataAccessException irse) {
				throw new ApplicationException ("More than one row returned when querying AEA_LS_DATA_ELEMENT_VIEW."
						+ "\ndataFormId =  " + dataFormId
						+ "\ndataElementName =  " + dataElementName
						+ "\ndataElementBusinessKey =  " + dataElementBusinessKey, irse);
			} catch (final Exception ex) {
				throw new ApplicationException ("LookupDataUtility - Could not select data from AEA_LS_DATA_ELEMENT_VIEW.", ex);
			}
			}

			//Store result in cache.
			etk.getCache().store(cacheKey, queryResult);
		}

		if (queryResult == null) {
			throw new ApplicationException (
					"Error retrieving element info for form element."
					+ "\ndataFormId =  " + dataFormId
					+ "\ndataElementName =  " + dataElementName
					+ "\ndataElementBusinessKey =  " + dataElementBusinessKey);
		}

		final int lookupType = getIntVal(queryResult.get("LOOKUP_TYPE"));

		String returnString = null;

		if (LookupSourceType.DATA_OBJECT_LOOKUP.getEntellitrakNumber() == lookupType) {
			final StringBuilder queryBuilder = new StringBuilder();

			queryBuilder.append("SELECT ");
			queryBuilder.append(getColName((String) queryResult.get("DISPLAY_COLUMN")));
			queryBuilder.append(" AS DISPLAY, ");
			queryBuilder.append(getColName((String) queryResult.get("VALUE_COLUMN")));
			queryBuilder.append(" AS VALUE FROM ");
			queryBuilder.append(queryResult.get("TABLE_NAME"));
			queryBuilder.append(" WHERE 1=1 ");

			//For audit log we do not want to add this filtering. Form filtering should do this automatically,
			//and we need to catch "from" values that are in the past.
			if (filterStartStopDate) {
				if (isSqlServer) {
					if (queryResult.get("START_DATE_COLUMN") != null) {
						queryBuilder.append(" and (datediff (day, dbo.ETKF_getServerTime(), isNull(");
						queryBuilder.append(queryResult.get("START_DATE_COLUMN"));
						queryBuilder.append(", dbo.ETKF_getServerTime())) <= 0) ");
					}

					if (queryResult.get("END_DATE_COLUMN") != null) {
						queryBuilder.append(" and (datediff (day, dbo.ETKF_getServerTime(), isNull(");
						queryBuilder.append(queryResult.get("END_DATE_COLUMN"));
						queryBuilder.append(", dbo.ETKF_getServerTime() + 1)) > 0) ");
					}
				} else {
					if (queryResult.get("START_DATE_COLUMN") != null) {
						queryBuilder.append(" AND ((trunc(");
						queryBuilder.append(queryResult.get("START_DATE_COLUMN"));
						queryBuilder.append(") <= trunc(ETKF_GETSERVERTIME())) OR ");
						queryBuilder.append(queryResult.get("START_DATE_COLUMN"));
						queryBuilder.append(" IS NULL) ");
					}

					if (queryResult.get("END_DATE_COLUMN") != null) {
						queryBuilder.append(" AND ((trunc(");
						queryBuilder.append(queryResult.get("END_DATE_COLUMN"));
						queryBuilder.append(") > trunc(ETKF_GETSERVERTIME())) OR ");
						queryBuilder.append(queryResult.get("END_DATE_COLUMN"));
						queryBuilder.append(" IS NULL) ");
					}
				}
			}

			final String userSQL = (String) queryResult.get("USER_SQL");
			if ((userSQL != null) && StringUtility.isNotBlank(userSQL)) {
				queryBuilder.append(" AND ");
				queryBuilder.append(userSQL);
			}

			if (queryResult.get("ORDER_COLUMN") != null) {
				queryBuilder.append(" ORDER BY ");
				queryBuilder.append(queryResult.get("ORDER_COLUMN"));

				if ("0".equals(queryResult.get("ASC_DESC_ORDER") + "")) {
					queryBuilder.append(" DESC ");
				} else if ("1".equals(queryResult.get("ASC_DESC_ORDER") + "")) {
					queryBuilder.append(" ASC ");
				}
			}

			returnString = queryBuilder.toString();

		} else if (LookupSourceType.QUERY_LOOKUP.getEntellitrakNumber() == lookupType) {
			returnString = (String) queryResult.get("USER_SQL");
		} else if (LookupSourceType.SCRIPT_LOOKUP.getEntellitrakNumber() == lookupType) {

			final String javaScriptId = (String) queryResult.get("SCRIPT_JAVA_ID");

			final LookupHandler luHandler = (LookupHandler)
					Class.forName(javaScriptId).newInstance();


			returnString = luHandler.execute(aLookupExecutionContext);
		} else if (LookupSourceType.SYSTEM_OBJECT_LOOKUP.getEntellitrakNumber() == lookupType) {
			returnString = LookupDataUtility.getSystemObjectQuery (etk,
					  SystemObjectType.getById(((Number) queryResult.get("SYSTEM_OBJECT_TYPE")).intValue()),
					  SystemObjectDisplayFormat.getById(((Number) queryResult.get("SYSTEM_OBJECT_DISPLAY_FORMAT")).intValue()));
		}

		if (isSqlServer) {
			return peformSelectReplacements(returnString);
		} else {
			return returnString;
		}
	}

	public static String getSystemObjectQuery (ExecutionContext etk,
	        SystemObjectType theSystemObjectType,
	        SystemObjectDisplayFormat theSystemObjectDisplayFormat)
	                throws ApplicationException {
		if (SystemObjectType.USER == theSystemObjectType) {
			if (SystemObjectDisplayFormat.ACCOUNT_NAME == theSystemObjectDisplayFormat) {
				return " select username as DISPLAY, user_id as VALUE from etk_user order by lower(username) ";
			} else if (SystemObjectDisplayFormat.LASTNAME_FIRSTNAME_MI == theSystemObjectDisplayFormat) {
				if (Utility.isSqlServer(etk)) {
					return
					" select p.last_name + ', ' + p.first_name + "
					+ " case when p.middle_name is null then '' else ' ' + SUBSTRING (p.middle_name, 1, 1) end as DISPLAY, "
					+ " user_id as VALUE "
					+ " from etk_user u "
					+ " join etk_person p on p.person_id = u.person_id "
					+ " order by lower(p.last_name) ";
				} else {
					return
					" select p.last_name || ', ' || p.first_name ||  "
					+ " case when p.middle_name is null then '' else ' ' || SUBSTR(p.middle_name, 1, 1) end as DISPLAY, user_id as VALUE "
					+ " from etk_user u "
					+ " join etk_person p on p.person_id = u.person_id "
					+ " order by lower(p.last_name) ";
				}
			} else {
				throw new ApplicationException ("Unknown SYSTEM_OBJECT_DISPLAY_FORMAT encountered with value = " +
						                        theSystemObjectDisplayFormat);
			}
		} else {
			throw new ApplicationException ("Unknown SYSTEM_OBJECT_TYPE encountered with value = " +
					                        theSystemObjectType);
		}
	}

	private static Integer intValue (final Object aNumber) {
		if (aNumber == null) {
			return null;
		} else if (aNumber instanceof Number) {
			return ((Number) aNumber).intValue();
		}

		return null;
	}

	private static Long longValue (final Object aNumber) {
		if (aNumber == null) {
			return null;
		} else if (aNumber instanceof Number) {
			return ((Number) aNumber).longValue();
		}

		return null;
	}

	private static Boolean booleanValue (final Object aBoolean) {
		if (aBoolean == null) {
			return null;
		} else if (aBoolean instanceof Number) {
			return ((Number) aBoolean).intValue() == 1 ? true : false;
		}

		return null;
	}

	public static List<AeaEtkDataObject> getAllEtkDataObjects (final ExecutionContext theEtk) {
		List<Map<String, Object>> etkDataObjectInfoList = null;


		try {
			etkDataObjectInfoList =
					theEtk.createSQL("select * from etk_data_object "
							+ "where tracking_config_id = "
							+ "(select max(tracking_config_id) from etk_tracking_config_archive) "
							+ "order by LABEL")
						.returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
						.fetchList();
		} catch (final Exception e) {
			Utility.aeaLog(theEtk, "Could not retrieve select * from etk_data_object", e);
			return null;
		}

		if (etkDataObjectInfoList.size() == 0) {
			Utility.aeaLog(theEtk, "No ETK_DATA_OBJECTs found.");
			return null;
		}

		final List<AeaEtkDataObject> resultList = new ArrayList<AeaEtkDataObject>();

		for (final Map<String, Object> etkDataObjectInfo : etkDataObjectInfoList) {
			resultList.add(getEtkDataObject(theEtk, etkDataObjectInfo, true));
		}

		return resultList;
	}

	public static AeaEtkDataObject getEtkDataObjectById (final ExecutionContext theEtk, final Number anEtkDataObjectId, boolean loadElements) {
		Map<String, Object> etkDataObjectInfo = null;

		if (anEtkDataObjectId == null) {
			return null;
		}

		try {
			etkDataObjectInfo =
					theEtk.createSQL("select * from etk_data_object "
						    + "where DATA_OBJECT_ID = :dataObjectId ")
						.returnEmptyResultSetAs(null)
						.setParameter("dataObjectId", anEtkDataObjectId)
						.fetchMap();
		} catch (final Exception e) {
			Utility.aeaLog(theEtk, "Could not retrieve ETK_DATA_OBJECT with DATA_OBJECT_ID = " + anEtkDataObjectId, e);
			return null;
		}

		if (etkDataObjectInfo == null) {
			Utility.aeaLog(theEtk, "No ETK_DATA_OBJECT with DATA_OBJECT_ID = " + anEtkDataObjectId + " found.");
			return null;
		}

		return getEtkDataObject(theEtk, etkDataObjectInfo, loadElements);
	}

	public static AeaEtkDataObject getEtkDataObjectByTableName (final ExecutionContext theEtk, final String aTableName) {
		Map<String, Object> etkDataObjectInfo = null;

		if (aTableName == null) {
			return null;
		}

		try {
			etkDataObjectInfo =
					theEtk.createSQL("select * from etk_data_object "
						+ "where table_name = :tableName "
						+ "and tracking_config_id = "
						+ "(select max(tracking_config_id) from etk_tracking_config_archive) ")
						.returnEmptyResultSetAs(null)
						.setParameter("tableName", aTableName)
						.fetchMap();
		} catch (final Exception e) {
			Utility.aeaLog(theEtk, "Could not retrieve ETK_DATA_OBJECT with TABLE_NAME = " + aTableName, e);
			return null;
		}

		if (etkDataObjectInfo == null) {
			Utility.aeaLog(theEtk, "No ETK_DATA_OBJECT with TABLE_NAME = " + aTableName + " found.");
			return null;
		}

		return getEtkDataObject(theEtk, etkDataObjectInfo, true);
	}

	public static AeaEtkDataElement getEtkDataElementById (final ExecutionContext theEtk,
			                                               final AeaEtkDataObject theEtkDataObject,
			                                               final Number etkDataElementId) {
		Map<String, Object> etkDataElementInfo = null;

		if (etkDataElementId == null) {
			return null;
		}

		try {
			etkDataElementInfo =
					theEtk.createSQL("select * from etk_data_element "
						    + "where DATA_ELEMENT_ID = :dataElementId ")
						.returnEmptyResultSetAs(null)
						.setParameter("dataElementId", etkDataElementId)
						.fetchMap();
		} catch (final Exception e) {
			Utility.aeaLog(theEtk, "Could not retrieve ETK_DATA_ELEMENT with DATA_ELEMENT_ID = " + etkDataElementId, e);
			return null;
		}

		if (etkDataElementInfo == null) {
			Utility.aeaLog(theEtk, "No ETK_DATA_ELEMENT with DATA_ELEMENT_ID = " + etkDataElementId + " found.");
			return null;
		}

		return getEtkDataElement(theEtk, theEtkDataObject, etkDataElementInfo);
	}

	public static AeaEtkLookupDefinition getEtkLookupDefinitionById(final ExecutionContext theEtk,
			                                                        final Number etkLookupDefinitionId) {
		Map<String, Object> etkLookupDefinitionInfo = null;

		if (etkLookupDefinitionId == null) {
			return null;
		}

		try {
			etkLookupDefinitionInfo =
					theEtk.createSQL("select * from ETK_LOOKUP_DEFINITION "
							+ "where LOOKUP_DEFINITION_ID = :lookupDefinitionId ")
							.returnEmptyResultSetAs(null)
							.setParameter("lookupDefinitionId", etkLookupDefinitionId)
							.fetchMap();
		} catch (final Exception e) {
			Utility.aeaLog(theEtk, "Could not retrieve ETK_LOOKUP_DEFINITION with LOOKUP_DEFINITION_ID = " + etkLookupDefinitionId, e);
			return null;
		}

		if (etkLookupDefinitionInfo == null) {
			Utility.aeaLog(theEtk, "No ETK_LOOKUP_DEFINITION with LOOKUP_DEFINITION_ID = " + etkLookupDefinitionId + " found.");
			return null;
		}

		return getEtkLookupDefinition(theEtk, etkLookupDefinitionInfo);
	}


	private static void populateEtkDataElements (final ExecutionContext theEtk,
			                                    final AeaEtkDataObject theEtkDataObject) {

		List<Map<String, Object>> etkDataElementInfoList = null;

		if (theEtkDataObject.getDataObjectId() == null) {
			return;
		}

		try {
			etkDataElementInfoList =
					theEtk.createSQL("select * from etk_data_element "
							+ "where DATA_OBJECT_ID = :dataObjectId order by COLUMN_NAME ")
							.returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
							.setParameter("dataObjectId", theEtkDataObject.getDataObjectId())
							.fetchList();
		} catch (final Exception e) {
			Utility.aeaLog(theEtk, "Could not retrieve ETK_DATA_ELEMENT records for DATA_OBJECT_ID = " +
		                   theEtkDataObject.getDataObjectId(), e);
			return;
		}

		if (etkDataElementInfoList.size() == 0) {
			theEtkDataObject.setDataElements(new ArrayList<AeaEtkDataElement>());

			Utility.aeaLog(theEtk, "No ETK_DATA_ELEMENTS for ETK_DATA_OBJECT with DATA_OBJECT_ID = "
			                       + theEtkDataObject.getDataObjectId() + " found - blank data object?");
			return;
		} else {
			final List<AeaEtkDataElement> elements = new ArrayList<AeaEtkDataElement>();

			for (final Map<String, Object> etkDataObjectInfo : etkDataElementInfoList) {
				elements.add(getEtkDataElement(theEtk, theEtkDataObject, etkDataObjectInfo));
			}

			theEtkDataObject.setDataElements(elements);
		}
	}


	private static AeaEtkDataObject getEtkDataObject (final ExecutionContext theEtk, final Map<String, Object> etkDataObjectInfo, boolean loadElements) {

		final AeaEtkDataObject edo = new AeaEtkDataObject();

		edo.setBusinessKey((String) etkDataObjectInfo.get("BUSINESS_KEY"));
		edo.setCardinality(intValue(etkDataObjectInfo.get("CARDINALITY")));
		edo.setDataObjectId(longValue(etkDataObjectInfo.get("DATA_OBJECT_ID")));

		if (longValue(etkDataObjectInfo.get("OBJECT_TYPE")) != null) {
			edo.setDataObjectType(DataObjectType.getDataObjectType(longValue(etkDataObjectInfo.get("OBJECT_TYPE"))));
		} else {
			edo.setDataObjectType(null);
		}

		edo.setDescription((String) etkDataObjectInfo.get("DESCRIPTION"));
		edo.setIsAppliedChanges(booleanValue(etkDataObjectInfo.get("APPLIED_CHANGES")));
		edo.setIsBaseObject(booleanValue(etkDataObjectInfo.get("BASE_OBJECT")));
		edo.setIsDocumentManagementEnabled(booleanValue(etkDataObjectInfo.get("DOCUMENT_MANAGEMENT_ENABLED")));
		edo.setIsDocumentManagementObject(booleanValue(etkDataObjectInfo.get("DOCUMENT_MANAGEMENT_OBJECT")));
		edo.setIsSearchable(booleanValue(etkDataObjectInfo.get("SEARCHABLE")));
		edo.setIsSeperateInbox(booleanValue(etkDataObjectInfo.get("SEPARATE_INBOX")));
		edo.setIsAutoAssignment(booleanValue(etkDataObjectInfo.get("AUTO_ASSIGNMENT")));
		edo.setLabel((String) etkDataObjectInfo.get("LABEL"));
		edo.setListOrder(intValue(etkDataObjectInfo.get("LIST_ORDER")));
		edo.setListStyle(intValue(etkDataObjectInfo.get("LIST_STYLE")));
		edo.setDesignator(intValue(etkDataObjectInfo.get("DESIGNATOR")));
		edo.setName((String) etkDataObjectInfo.get("NAME"));
		edo.setObjectName((String) etkDataObjectInfo.get("OBJECT_NAME"));
		edo.setParentObjectId(longValue(etkDataObjectInfo.get("PARENT_OBJECT_ID")));
		edo.setTableName((String) etkDataObjectInfo.get("TABLE_NAME"));
		edo.setTableSpace((String) etkDataObjectInfo.get("TABLE_SPACE"));
		edo.setTrackingConfigId(longValue(etkDataObjectInfo.get("TRACKING_CONFIG_ID")));

		if (loadElements) {
		populateEtkDataElements(theEtk, edo);
		}

		return edo;
	}

	private static AeaEtkDataElement getEtkDataElement (final ExecutionContext theEtk,
														final AeaEtkDataObject theEtkDataObject,
			                                            final Map<String, Object> etkDataElementInfo) {
		final AeaEtkDataElement ede = new AeaEtkDataElement();

		ede.setAreFutureDatesAllowed(booleanValue(etkDataElementInfo.get("FUTURE_DATES_ALLOWED")));
		ede.setBusinessKey((String) etkDataElementInfo.get("BUSINESS_KEY"));
		ede.setColumnName((String) etkDataElementInfo.get("COLUMN_NAME"));
		ede.setDataElementId(longValue(etkDataElementInfo.get("DATA_ELEMENT_ID")));
		ede.setDataSize(intValue(etkDataElementInfo.get("DATA_SIZE")));

		if (longValue(etkDataElementInfo.get("DATA_TYPE")) != null) {
			ede.setDataType(DataElementType.getDataElementType(longValue(etkDataElementInfo.get("DATA_TYPE"))));
		} else {
			ede.setDataType(null);
		}

		ede.setDefaultValue((String) etkDataElementInfo.get("DEFAULT_VALUE"));
		ede.setDescription((String) etkDataElementInfo.get("DESCRIPTION"));
		ede.setElementName((String) etkDataElementInfo.get("ELEMENT_NAME"));

		if (theEtkDataObject == null) {
			ede.setEtkDataObject(getEtkDataObjectById(theEtk, (Number) etkDataElementInfo.get("DATA_OBJECT_ID"), false));
		} else {
			ede.setEtkDataObject(theEtkDataObject);
		}

		if (etkDataElementInfo.get("LOOKUP_DEFINITION_ID") != null) {

			//Prevent infinite lookup recursion
			if (etkDataElementInfo.get("LOOKUP_DEFINITION_ID").equals(previousLookupId.get())) {
				recursionCounter.set(recursionCounter.get() + 1);
			} else {
				recursionCounter.remove();
				previousLookupId.set((Number) etkDataElementInfo.get("LOOKUP_DEFINITION_ID"));
			}

			if (recursionCounter.get() < 10) {
				ede.setEtkLookupDefinition(getEtkLookupDefinitionById(theEtk,
						longValue(etkDataElementInfo
								.get("LOOKUP_DEFINITION_ID"))));
			} else {
				theEtk.getLogger()
						.error("LookupDataUtility:getEtkDataElement warning: Data Object \""
								+ theEtkDataObject.getName()
								+ "\" contains a self referenced lookup, skipping at recursive depth of 10.");

				recursionCounter.remove();
				ede.setEtkLookupDefinition(null);
			}
		} else {
			ede.setEtkLookupDefinition(null);
		}

		ede.setIndexType(intValue(etkDataElementInfo.get("INDEX_TYPE")));
		ede.setIsAppliedChanges(booleanValue(etkDataElementInfo.get("APPLIED_CHANGES")));
		ede.setIsBoundToLookup(booleanValue(etkDataElementInfo.get("BOUND_TO_LOOKUP")));
		ede.setIsDefaultToToday(booleanValue(etkDataElementInfo.get("DEFAULT_TO_TODAY")));
		ede.setIsIdentifier(booleanValue(etkDataElementInfo.get("IDENTIFIER")));
		ede.setIsLogged(booleanValue(etkDataElementInfo.get("LOGGED")));
		ede.setIsPrimaryKey(booleanValue(etkDataElementInfo.get("PRIMARY_KEY")));
		ede.setIsSearchable(booleanValue(etkDataElementInfo.get("SEARCHABLE")));
		ede.setIsStoredInDocumentManagement(booleanValue(etkDataElementInfo.get("STORED_IN_DOCUMENT_MANAGEMENT")));
		ede.setIsSystemField(booleanValue(etkDataElementInfo.get("SYSTEM_FIELD")));
		ede.setIsUnique(booleanValue(etkDataElementInfo.get("IS_UNIQUE")));
		ede.setIsUsedForEscan(booleanValue(etkDataElementInfo.get("USED_FOR_ESCAN")));
		ede.setIsValidationRequired(booleanValue(etkDataElementInfo.get("VALIDATION_REQUIRED")));
		ede.setmTableName((String) etkDataElementInfo.get("TABLE_NAME"));
		ede.setName((String) etkDataElementInfo.get("NAME"));
		ede.setPluginRegistrationId(longValue(etkDataElementInfo.get("PLUGIN_REGISTRATION_ID")));
		ede.setRequired((String) etkDataElementInfo.get("REQUIRED"));

		return ede;

	}

	private static AeaEtkLookupDefinition getEtkLookupDefinition(final ExecutionContext theEtk,
			                                                     final Map<String, Object> etkLookupDefinitionInfo) {
		final AeaEtkLookupDefinition ld = new AeaEtkLookupDefinition();

		ld.setAscendingOrder(booleanValue(etkLookupDefinitionInfo.get("ASCENDING_ORDER")));
		ld.setBusiness_key((String) etkLookupDefinitionInfo.get("BUSINESS_KEY"));
		ld.setDescription((String) etkLookupDefinitionInfo.get("DESCRIPTION"));

		AeaEtkDataObject ldDataObject = null;

		if (etkLookupDefinitionInfo.get("DATA_OBJECT_ID") != null) {
			ldDataObject = getEtkDataObjectById(theEtk, longValue(etkLookupDefinitionInfo.get("DATA_OBJECT_ID")), false);
		}

		if (etkLookupDefinitionInfo.get("DISPLAY_ELEMENT_ID") != null) {
			ld.setDisplayElement(getEtkDataElementById(theEtk, ldDataObject, longValue(etkLookupDefinitionInfo.get("DISPLAY_ELEMENT_ID"))));
		} else {
			ld.setDisplayElement(null);
		}
		if (etkLookupDefinitionInfo.get("END_DATE_ELEMENT_ID") != null) {
			ld.setEndDateElement(getEtkDataElementById(theEtk, ldDataObject, longValue(etkLookupDefinitionInfo.get("END_DATE_ELEMENT_ID"))));
		} else {
			ld.setEndDateElement(null);
		}

		ld.setEtkDataObject(ldDataObject);
		ld.setLookupDefinitonId(longValue(etkLookupDefinitionInfo.get("LOOKUP_DEFINITION_ID")));
		ld.setLookupSql((String) etkLookupDefinitionInfo.get("LOOKUP_SQL"));

		if (etkLookupDefinitionInfo.get("LOOKUP_SOURCE_TYPE") != null) {
		    ld.setLookupType(LookupSourceType.getLookupSourceType(longValue(etkLookupDefinitionInfo.get("LOOKUP_SOURCE_TYPE"))));
		} else {
			ld.setLookupType(null);
		}


		ld.setName((String) etkLookupDefinitionInfo.get("NAME"));

		if (etkLookupDefinitionInfo.get("ORDER_BY_ELEMENT_ID") != null) {
			ld.setOrderByElement(getEtkDataElementById(theEtk, ldDataObject, longValue(etkLookupDefinitionInfo.get("ORDER_BY_ELEMENT_ID"))));
		} else {
			ld.setOrderByElement(null);
		}

		ld.setPluginRegistrationId(longValue(etkLookupDefinitionInfo.get("PLUGIN_REGISTRATION_ID")));
		ld.setSqlScriptObjectId(longValue(etkLookupDefinitionInfo.get("SQL_SCRIPT_OBJECT_ID")));

		if (etkLookupDefinitionInfo.get("START_DATE_ELEMENT_ID") != null) {
			ld.setStartDateElement(getEtkDataElementById(theEtk, ldDataObject, longValue(etkLookupDefinitionInfo.get("START_DATE_ELEMENT_ID"))));
		} else {
			ld.setStartDateElement(null);
		}

		ld.setTrackingConfigId(longValue(etkLookupDefinitionInfo.get("TRACKING_CONFIG_ID")));

		if (etkLookupDefinitionInfo.get("VALUE_ELEMENT_ID") != null) {
		   ld.setValueElement(getEtkDataElementById(theEtk, ldDataObject, longValue(etkLookupDefinitionInfo.get("VALUE_ELEMENT_ID"))));
		} else {
			ld.setValueElement(null);
		}

		if (etkLookupDefinitionInfo.get("VALUE_RETURN_TYPE") != null) {
			ld.setValueReturnType(DataElementType.getDataElementType(longValue(etkLookupDefinitionInfo.get("VALUE_RETURN_TYPE"))));
		} else {
			ld.setValueReturnType(null);
		}

		if (etkLookupDefinitionInfo.get("SYSTEM_OBJECT_TYPE") != null) {
			ld.setSystemObjectType(SystemObjectType.getById(intValue(etkLookupDefinitionInfo.get("SYSTEM_OBJECT_TYPE"))));
		} else {
			ld.setSystemObjectType(null);
		}

		if (etkLookupDefinitionInfo.get("SYSTEM_OBJECT_DISPLAY_FORMAT") != null) {
			ld.setSystemObjectDisplayFormat(SystemObjectDisplayFormat.getById(intValue(etkLookupDefinitionInfo.get("SYSTEM_OBJECT_DISPLAY_FORMAT"))));
		} else {
			ld.setSystemObjectDisplayFormat(null);
		}

		ld.setEnableCaching(booleanValue(etkLookupDefinitionInfo.get("ENABLE_CACHING")));

		return ld;
	}

	/**
	 * Method to convert a string value to a typed object based on the lookups defined return type.
	 *
	 * @param theEtk entellitrak execution context
	 * @param dataValue String representation of the lookup's value.
	 * @param dataType The dataType of the lookup.
	 * @return Long/Double/java.sql.Date/String/null representation of the object.
	 */
	public static Object convertStringToTypedObject(ExecutionContext theEtk, String dataValue, DataType dataType) {
		Object returnValue = null;

		if ((dataValue == null) || (dataType == null)) {
			return null;
		} else if (dataType == DataType.NUMBER ||
				   dataType == DataType.YES_NO ||
				   dataType == DataType.FILE) {
			try {
				returnValue = new Long(dataValue);
			} catch (Exception e) {
				Utility.aeaLog(theEtk, "convertStringToTypedObject - Could not convert \"" + dataValue
						          + "\" with DataType " + dataType + " to Long data type.", e);
			}
		} else if (dataType == DataType.CURRENCY) {
			try {
				returnValue = new Double(dataValue);
			} catch (Exception e) {
				Utility.aeaLog(theEtk, "convertStringToTypedObject - Could not convert \"" + dataValue
						          + "\" with DataType " + dataType + " to Double data type.", e);
			}
		} else if (dataType == DataType.DATE) {
			try {
				returnValue = new java.sql.Date(DateUtility.parseDate(dataValue).getTime());
			} catch (Exception e) {
				Utility.aeaLog(theEtk, "convertStringToTypedObject - Could not convert \"" + dataValue
						          + "\" with DataType " + dataType + " to java.sql.Date data type.", e);
			}
		} else if (dataType == DataType.TIMESTAMP) {
			try {
				returnValue = new java.sql.Date(DateUtility.parseDateTime(dataValue).getTime());
			} catch (Exception e) {
				Utility.aeaLog(theEtk, "convertStringToTypedObject - Could not convert \"" + dataValue
						          + "\" with DataType " + dataType + " to java.sql.Date data type.", e);
			}
		} else {
			returnValue = dataValue;
		}

		return returnValue;
	}


}

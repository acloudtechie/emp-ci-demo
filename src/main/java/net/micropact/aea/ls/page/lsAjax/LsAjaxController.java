/**
 *
 * LsAjaxController
 *
 * administrator 09/15/2014
 **/

package net.micropact.aea.ls.page.lsAjax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.micropact.aea.core.exceptionTools.ExceptionUtility;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;
import net.micropact.aea.utility.lookup.AeaLookupExecutionContextImpl;
import net.micropact.aea.utility.lookup.LookupDataUtility;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.configuration.DataObject;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.lookup.For;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

/**
 * AJAX call for the Live Search Component.
 *
 * @author aclee
 *
 */
public class LsAjaxController implements PageController {
	private static final int MIN_SEARCH_RESULTS = 1;
	private static final int MAX_SEARCH_RESULTS = 200;
	private static final int DEFAULT_SEARCH_RESULTS = 20;

	@Override
	public Response execute(final PageExecutionContext etk) throws ApplicationException {
		final TextResponse tr = etk.createTextResponse();

		//Load parameters from AJAX call.
		final String trackingId = etk.getParameters().getSingle("trackingId");
		final String parentId = etk.getParameters().getSingle("parentId");
		final String baseId = etk.getParameters().getSingle("baseId");

		final Long trackingIdLong = StringUtility.isBlank(trackingId) ? null : new Long(trackingId);
		final Long parentIdLong = StringUtility.isBlank(parentId) ? null : new Long(parentId);
		final Long baseIdLong = StringUtility.isBlank(baseId) ? null : new Long(baseId);

		final String dataObjectKey = etk.getParameters().getSingle("dataObjectKey");
		final Long dataFormId = new Long(etk.getParameters().getSingle("dataFormId"));
		final String dataElementId  = etk.getParameters().getSingle("dataElementId");
		final String operation = etk.getParameters().getSingle("operation");
	    String userEnteredSearch = etk.getParameters().getSingle("userEnteredSearch");
		final List<String> columnHeaders = etk.getParameters().getField("columnHeaders");
		final List<String> replacementParams = etk.getParameters().getField("replacementParams");

		final Long userId = etk.getCurrentUser().getId();
		final Long ouId = etk.getCurrentUser().getHierarchy().getId();
		final Long roleId = etk.getCurrentUser().getRole().getId();

		final String breakText = "@r@Nd0mP@tT3rn@";

		final LookupDataUtility lsu = new LookupDataUtility(etk);

		DataObject dataObject = etk.getConfigurationService().loadDataObject(dataObjectKey);
		final String dataObjectBusinessKey = dataObject.getBusinessKey();
		final String dataObjectTableName = dataObject.getTableName();

		//Gets all {?parameters} in an SQL query.
		if ("getParamsNeededForAJAX".equals(operation)) {

			final StringBuilder sb = new StringBuilder();

			sb.append("{\"jsonResult\": {");

			//Attempts to retrieve the query and returns a JSON error message if the retrieval was unsuccessful.
			String lookupQuery = "";
			try {

				lookupQuery = lsu.getLookupQuery(dataFormId, dataElementId,
						                        new AeaLookupExecutionContextImpl(etk, For.TRACKING, trackingIdLong, baseIdLong,
						                        		parentIdLong, dataObjectBusinessKey, dataObjectTableName));
			} catch (final Throwable t) {

				net.micropact.aea.utility.Utility.aeaLog(etk, t);

				sb.append("\"isValid\": \"false\", \"errorMessage\":");
				sb.append(JsonUtilities.encode(ExceptionUtility.getFullStackTrace(t)));
				sb.append("}}");

				tr.setContentType(ContentType.JSON);
				tr.put("out", sb.toString());
				return tr;
			}

			//Log the query that was retrieved.
			//net.micropact.aea.utility.Utility.aeaLog(etk, "Live Search Query = " + lookupQuery);

			final Pattern p = Pattern.compile("\\{\\?.*?\\}");
			final Matcher m = p.matcher(lookupQuery);

			//Find all replacement parameters based on pattern above and add them to the JSON response.
			sb.append("\"isValid\": \"true\", \"parameters\": [");
			boolean deleteLastComma = false;
			while(m.find()){
				String tmp = m.group();
				tmp = tmp.substring(2, tmp.length() - 1);

				if ("trackingId".equals(tmp) ||
						"baseId".equals(tmp) ||
						"parentId".equals(tmp) ||
						"currentUser.id".equals(tmp) ||
						"currentUser.roleId".equals(tmp) ||
						"currentUser.ouId".equals(tmp) ||
						"assignmentRoleId".equals(tmp)) {
					continue;
				}

				sb.append("{\"replacementParam\":\"");
				sb.append(tmp);
				sb.append("\"},");
				deleteLastComma = true;
			}

			if (deleteLastComma) {
				sb.deleteCharAt(sb.length()-1);
			}

			sb.append("]}}");

			tr.setContentType(ContentType.JSON);
			tr.put("out", sb.toString());
		} else if ("displayLiveSearchResults".equals(operation)) {
			//This is the method that builds the response table with the search results.

			final StringBuilder sb = new StringBuilder();
			final boolean isSqlServer = Utility.isSqlServer(etk);

			try {
				//Load the configured max number of search results.
				Integer maxSearchResults = (Integer) etk.getCache().load("ls.maxSearchResults");

				if (maxSearchResults == null) {
					try {
						final String maxSearchResultsStr =
								etk.createSQL("select c_value from T_AEA_CORE_CONFIGURATION where c_code = 'ls.maxSearchResults'")
								.fetchString();

						if (StringUtility.isNotBlank(maxSearchResultsStr)) {
							maxSearchResults = new Integer(maxSearchResultsStr);

							if ((maxSearchResults < MIN_SEARCH_RESULTS) || (maxSearchResults > MAX_SEARCH_RESULTS)) {
								maxSearchResults = DEFAULT_SEARCH_RESULTS;
							}
						} else {
							Utility.aeaLog(etk, "No configuration value in T_AEA_CORE_CONFIGURATION for "
									+ "ls.maxSearchResults, defaulting to " + DEFAULT_SEARCH_RESULTS + " records.");
							maxSearchResults = DEFAULT_SEARCH_RESULTS;
						}
					} catch (final Exception e) {
						Utility.aeaLog(etk, "No configuration value in T_AEA_CORE_CONFIGURATION for "
								+ "ls.maxSearchResults, defaulting to " + DEFAULT_SEARCH_RESULTS + " records.");
						maxSearchResults = DEFAULT_SEARCH_RESULTS;
					}

					etk.getCache().store("ls.maxSearchResults", maxSearchResults);
				}

				final Map<String, Object> parameterMap = new HashMap<>();
				parameterMap.put("trackingId", trackingId);
				parameterMap.put("parentId", parentId);
				parameterMap.put("baseId", baseId);
				parameterMap.put("currentUser.id", userId);
				parameterMap.put("currentUser.ouId", ouId);
				parameterMap.put("currentUser.roleId", roleId);

				//Get the lookup query for the provided data element.
				String lookupQuery = lsu.getLookupQuery(dataFormId, dataElementId,
			                                new AeaLookupExecutionContextImpl(etk, For.TRACKING, trackingIdLong, baseIdLong,
					                        		parentIdLong, dataObjectBusinessKey, dataObjectTableName));
				lookupQuery = lookupQuery.replaceAll("\\{\\$isLiveSearchAjax\\}", "1");
				final StringBuffer modifiedQuery = new StringBuffer();

				final Pattern p = Pattern.compile("\\{\\?.*?\\}");
				final Matcher m = p.matcher(lookupQuery);

				//Find all replacement parameters and bind the replacementParams passed from the form
				//to the query.
				while(m.find()) {
					final String tmp = m.group();
					final String stripped = tmp.substring(2, tmp.length() - 1);

					if (!("trackingId".equals(stripped) ||
							"baseId".equals(stripped) ||
							"parentId".equals(stripped) ||
							"currentUser.id".equals(stripped) ||
							"currentUser.roleId".equals(stripped) ||
							"currentUser.ouId".equals(stripped) ||
							"assignmentRoleId".equals(stripped))) {
						parameterMap.put(stripped, null);
					}

					m.appendReplacement(modifiedQuery, Matcher.quoteReplacement(":" + stripped));
				}

				m.appendTail(modifiedQuery);

				final String inClauseQuery = modifiedQuery.toString();

				if (replacementParams != null) {
					for (final String aString : replacementParams) {

						if (StringUtility.isBlank(aString)) {
							continue;
						}

						final String[] params = aString.split(breakText);

						if (params.length == 2) {
							if (params[1].contains(",")) {
								final List<String> objectArray = new ArrayList<>();
								objectArray.add("-1935223"); //dummy value

								final String[] paramList = params[1].split(",");

								for (final String aParam : paramList) {
									if (StringUtility.isNotBlank(aParam)) {
										objectArray.add(aParam);
									}
								}

								parameterMap.put(params[0], objectArray);
							} else {
								parameterMap.put(params[0], params[1]);
							}
						}
					}
				}

				//Wrap the query in an outer select, replace all wild card variables.
				final StringBuilder finalQuery = new StringBuilder();
				finalQuery.append("select ");

				if (isSqlServer) {
					finalQuery.append(" top ");
					finalQuery.append(maxSearchResults);
				} else {
					userEnteredSearch = userEnteredSearch.replaceAll("\\\\", "\\\\\\\\");
					userEnteredSearch = userEnteredSearch.replaceAll("%", "\\\\%");
					userEnteredSearch = userEnteredSearch.replaceAll("_", "\\\\_");
				}

				finalQuery.append(" * from (");
				finalQuery.append(inClauseQuery);

				//Filter by the user entered search string. Escape Oracle special characters.
				if (!isSqlServer) {
					finalQuery.append(") where lower(display) like ('%' || :userEnteredSearch || '%') ");
					finalQuery.append(" ESCAPE '\\' and rownum <= ");
					finalQuery.append(maxSearchResults);
				} else {
					finalQuery.append(") TEMP_QUERY where lower(display) like ('%' + :userEnteredSearch + '%')");
				}

				parameterMap.put("userEnteredSearch", userEnteredSearch.toLowerCase());

				final List<Map<String, Object>> rowsToWrite =
						etk.createSQL (finalQuery.toString())
						.setParameter(parameterMap)
						.fetchList();

				sb.append("{\"jsonResult\": {\"isValid\": \"true\", \"tableResult\":");
				sb.append(JsonUtilities.encode(lsu.writeTableHeader(columnHeaders)
									        +  lsu.writeTableBody(columnHeaders, rowsToWrite, dataElementId)));
				sb.append("}}");

				tr.setContentType(ContentType.JSON);
				tr.put("out", sb.toString());

			} catch (final Exception e) {
				//If an exception occurs, return the message back to the front end in the JSON.
				sb.append("{\"jsonResult\": {\"isValid\": \"false\", \"errorMessage\":");
				sb.append(JsonUtilities.encode(ExceptionUtility.getFullStackTrace(e)));
				sb.append("}}");

				tr.setContentType(ContentType.JSON);
				tr.put("out", sb.toString());
			}
		}

		return tr;
	}
}

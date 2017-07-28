package net.micropact.aea.utility.rdoutils;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.entellitrak.DataAccessException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.legacy.util.DateUtility;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.localization.Localizations;
import com.entellitrak.lookup.For;
import com.entellitrak.lookup.LookupResult;

import net.entellitrak.aea.exception.FormValidationException;
import net.micropact.aea.core.dataTypePlugin.DataTypePluginClassUtility;
import net.micropact.aea.core.utility.StringEscapeUtils;
import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.Utility;

/**
 * This class takes the context, an RDO form's business key and a corresponding
 * view's business key, as well as a list of previously entered form parameter
 * values, and returns either a search form or a result table. The result is
 * similar to core's standard search for BTOs or CTOs.
 *
 * @author aclee
 *
 */
public class RdoSearchPageUtility {

	private final ExecutionContext etk;
	private StringBuilder jsNeeded;
	private final String dataFormBusinessKey;
	private final String dataViewBusinessKey;
	private final Map<String, List<String>> formParameters;
	private final boolean militaryTimeMode;
	private final boolean linkToRdo;

	/**
	 * Default constructor.
	 * @param theEtk The execution context.
	 * @param aDataFormBusinessKey RDO Form business key.
	 * @param aViewBusinessKey RDO View business key.
	 * @param theParameterMap Form parameter id / value map.
	 * @param isMilitaryTimeMode whether the search should be performed using the military timestamp format
	 * @param isLinkToRdo whether a link should be provided in the result table linking to the RDO
	 */
	public RdoSearchPageUtility (final ExecutionContext theEtk,
			                       final String aDataFormBusinessKey,
			                       final String aViewBusinessKey,
			                       final Map<String, List<String>> theParameterMap,
			                       final boolean isMilitaryTimeMode,
			                       final boolean isLinkToRdo) {
		this.etk = theEtk;
		this.dataFormBusinessKey = aDataFormBusinessKey;
		this.dataViewBusinessKey = aViewBusinessKey;
		this.formParameters = theParameterMap;
		this.militaryTimeMode = isMilitaryTimeMode;
		this.linkToRdo = isLinkToRdo;
	}

	/**
	 * Returns an HTML standard search type form (in a table tag) containing all form inputs
	 * defined in the dataFormBusinessKey entered in the main constructor.
	 *
	 * @return A String representing the HTML for the element search (similar to the table within the tracking form)
	 * @throws FormValidationException If a problem is encountered building the form
	 */
	public String getSearchPageForForm () throws FormValidationException {

		jsNeeded = new StringBuilder();

		//Returns a list of form controls and lookup IDs for elements on the search form.
		//Joins to DATA_ELEMENT_ID make sure that unbound elements are not returned.
		final List<Map<String, Object>> dataFormRowList =
			etk.createSQL(" select  "
						+ " efc.FORM_CONTROL_TYPE as FORM_CONTROL_TYPE, "
						+ " efc.FORM_CONTROL_ID as FORM_CONTROL_ID, "
						+ " efc.NAME as FIELD_NAME, "
						+ " efc.LABEL as FIELD_LABEL, "
						+ " edo.OBJECT_NAME as DATA_OBJECT_NAME, "
						+ " edo.BUSINESS_KEY as DATA_OBJECT_BUSINESS_KEY, "
						+ " ede.BUSINESS_KEY as DATA_ELEMENT_BUSINESS_KEY, "
						+ " ld.BUSINESS_KEY as LOOKUP_BUSINESS_KEY "
						+ " from etk_form_control efc "
						+ " join etk_data_form edf on edf.data_form_id = efc.data_form_id "
						+ " join etk_data_object edo on edo.data_object_id = edf.data_object_id "
						+ " join ETK_FORM_CTL_ELEMENT_BINDING fceb on fceb.FORM_CONTROL_ID = efc.FORM_CONTROL_ID "
						+ " join ETK_DATA_ELEMENT ede on ede.DATA_ELEMENT_ID = fceb.DATA_ELEMENT_ID "
						+ " left join ETK_LOOKUP_DEFINITION ld on ld.LOOKUP_DEFINITION_ID = ede.LOOKUP_DEFINITION_ID "
						+ " where edo.tracking_config_id = (select max(tracking_config_id) from etk_tracking_config_archive) "
						+ " and edf.business_key = :dataFormBusinessKey "
						+ " order by display_order ")
						.setParameter("dataFormBusinessKey", dataFormBusinessKey)
						.returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
						.fetchList();

		String label;
		String formPrefix;
		String fieldName;
		String formControlType;
		String lookupBusinessKey;
		String dataObjectBusinessKey;
		String dataElementBusinessKey;
		long formControlId;

		final StringBuilder sb = new StringBuilder();
		sb.append("<table class=\"formBody\">");
		sb.append("<tbody>");

		if (dataFormRowList.size() == 0) {
			throw new FormValidationException (
					"Error: No form elements found for formBusinessKey = \"" +
					dataFormBusinessKey + "\"");
		}

		//Build all search fields based on the form control type.
		for (final Map<String, Object> aFormRow : dataFormRowList) {
			label = StringEscapeUtils.escapeHtml((String) aFormRow.get("FIELD_LABEL"));
			formPrefix = StringEscapeUtils.escapeHtml((String) aFormRow.get("DATA_OBJECT_NAME"));
			fieldName = StringEscapeUtils.escapeHtml((String) aFormRow.get("FIELD_NAME"));

			formControlType = (String) aFormRow.get("FORM_CONTROL_TYPE");

			if ("yesno".equals(formControlType)) {
				sb.append(getBooleanSearchField(label, formPrefix, fieldName));
			} else if ("text".equals(formControlType)) {
				sb.append(getTextSearchField(label, formPrefix, fieldName));
			} else if ("date".equals(formControlType)) {
				sb.append(getDateSearchField(label, formPrefix, fieldName));
			} else if ("textarea".equals(formControlType)) {
				sb.append(getLongTextSearchField(label, formPrefix, fieldName));
			} else if ("timestamp".equals(formControlType)) {
				sb.append(getTimestampSearchField(label, formPrefix, fieldName));
			} else if ("select".equals(formControlType)) {
				lookupBusinessKey = (String) aFormRow.get("LOOKUP_BUSINESS_KEY");
				sb.append(getDropdownField(label, formPrefix, fieldName, lookupBusinessKey));
			} else if ("checkbox".equals(formControlType)) {
				lookupBusinessKey = (String) aFormRow.get("LOOKUP_BUSINESS_KEY");
				sb.append(getMultiselectField(label, formPrefix, fieldName, lookupBusinessKey));
			} else if ("custom".equals(formControlType)) {

				dataObjectBusinessKey = (String) aFormRow.get("DATA_OBJECT_BUSINESS_KEY");
				dataElementBusinessKey = (String) aFormRow.get("DATA_ELEMENT_BUSINESS_KEY");
				formControlId = ((Number) aFormRow.get("FORM_CONTROL_ID")).longValue();

				sb.append(getCustomDataTypeSearchField(label, formPrefix, fieldName,
						dataObjectBusinessKey, dataElementBusinessKey, formControlId));
			}
		}

		sb.append("</tbody>");
		sb.append("</table>");

		return sb.toString();
	}

	/**
	 * After getSearchPageForForm() is called, the form may require some JS code to be inserted into a &lt;script&gt;
	 * tag on the view. This method returns that JS which can then be inserted onto the form via velocity.
	 *
	 * @return Dynamically generated JS.
	 */
	public String getDynamicallyGeneratedJS() {
		final String jsString = jsNeeded.toString();
		jsNeeded = new StringBuilder();
		return jsString;
	}

	/**
	 * This method performs a search on an RDO and returns the results in a formatted HTML table with the columns
	 * provided as per the configured viewBusinessKey passed into this utility's constructor.
	 *
	 * Optionally, the result may be filtered to show only records created by user's in the current user's
	 * access level organizational hierarchy structure.
	 *
	 * @param filterByOrgHierarchy Whether or not to filter by organizational hierarchy.
	 * @return &lt;table&gt; HTML element
	 *
	 * @throws IncorrectResultSizeDataAccessException
	 *         If there was an underlying {@link IncorrectResultSizeDataAccessException}
	 * @throws FormValidationException If the values submitted to the search fail validation
	 */
	public String performSearch (final boolean filterByOrgHierarchy)
			throws IncorrectResultSizeDataAccessException, FormValidationException {

		//Fetch the table and object name for the etk data form.
		final Map<String, Object> tableAndObjectName = etk.createSQL(
				"select"
				+ " edo.TABLE_NAME as TABLE_NAME,"
				+ " edo.OBJECT_NAME as DATA_OBJECT_NAME,"
				+ " edo.BUSINESS_KEY as DATA_OBJECT_KEY"
				+ " from etk_data_form edf"
				+ " join etk_data_object edo on edo.data_object_id = edf.data_object_id"
				+ " where edo.tracking_config_id = (select max(tracking_config_id) from etk_tracking_config_archive)"
				+ " and edf.business_key = :dataFormBusinessKey"
				)
				.returnEmptyResultSetAs(null)
				.setParameter("dataFormBusinessKey", dataFormBusinessKey)
				.fetchMap();

		if (tableAndObjectName == null) {
		    throw new FormValidationException (
		            "Error: No ETK_DATA_FORM record found with BUSINESS_KEY = \"" +
		                    dataFormBusinessKey + "\"");
		}

		final String dataObjectKey = (String) tableAndObjectName.get("DATA_OBJECT_KEY");

		final String tableName = (String) tableAndObjectName.get("TABLE_NAME");
		final String dataObjectName = (String) tableAndObjectName.get("DATA_OBJECT_NAME");
		final String sqlAppendChar = Utility.isSqlServer(etk) ? "+" : "||";

		//Check to make sure that if filterByOrgHierarchy is enabled, C_ORGANIZATION_ID exists in the R_DATA
		//table
		if (filterByOrgHierarchy) {
			try {
				if (Utility.isSqlServer(etk)) {
                    etk.createSQL("select top 1 C_ORGANIZATION_ID from " + tableName).fetchList();
                } else {
    			    etk.createSQL("select C_ORGANIZATION_ID from " + tableName + " where rownum < 1 ").fetchList();
                }
			} catch (final Exception e) {
				throw new FormValidationException ("Error: no C_ORGANIZATION_ID for table " +
						tableName + ". Cannot perform search. Disable filterByOrgHierarchy "
						+ "if you do not wish to filter this reference data by organizational "
						+ "hierarchy or add a non-nullable column to the table that is "
						+ "populated with the creating user's organization.");
			}
		}

		//Fetch all form elements for the RDO Data Form
		final List<RdoDataElement> elementList = getRdoDataElements(etk.createSQL(
				"select"
				+ " efc.NAME as NAME,"
				+ " efc.LABEL as LABEL,"
				+ " efc.FORM_CONTROL_TYPE as FORM_CONTROL_TYPE, "
				+ " ede.TABLE_NAME as M_TABLE_NAME,"
				+ " ede.COLUMN_NAME as COLUMN_NAME, "
				+ " ede.DATA_TYPE as DATA_TYPE "
				+ " from etk_form_control efc"
				+ " join etk_data_form edf on edf.data_form_id = efc.data_form_id"
				+ " join etk_data_object edo on edo.data_object_id = edf.data_object_id"
				+ " join ETK_FORM_CTL_ELEMENT_BINDING fceb on fceb.FORM_CONTROL_ID = efc.FORM_CONTROL_ID"
				+ " join etk_data_element ede on ede.data_element_id = fceb.data_element_id"
				+ " where edo.tracking_config_id = (select max(tracking_config_id) from etk_tracking_config_archive)"
				+ " and edf.business_key = :dataFormBusinessKey "
				+ " order by display_order"
				)
				.returnEmptyResultSetAs(new ArrayList<HashMap<String, Object>>())
				.setParameter("dataFormBusinessKey", dataFormBusinessKey)
				.fetchList());

		//Fetch all view elements for the RDO Data View
		final List<RdoDataElement> dataViewColumnList = getRdoDataElements(etk.createSQL (
				" select "
				+ " edve.NAME as NAME, "
				+ " edve.LABEL as LABEL,  "
				+ " ede.DATA_TYPE as DATA_TYPE, "
				+ " ede.TABLE_NAME as M_TABLE_NAME, "
				+ " ede.COLUMN_NAME as COLUMN_NAME, "
				+ " ld.BUSINESS_KEY as LOOKUP_BUSINESS_KEY "
				+ " from ETK_DATA_VIEW edv "
				+ " join ETK_DATA_VIEW_ELEMENT edve on edv.data_view_id = edve.data_view_id "
				+ " join etk_data_object edo on edo.data_object_id = edv.data_object_id "
				+ " join etk_data_element ede on ede.data_element_id = edve.data_element_id "
				+ " left join ETK_LOOKUP_DEFINITION ld on ld.LOOKUP_DEFINITION_ID = ede.LOOKUP_DEFINITION_ID "
				+ " where edo.tracking_config_id = (select max(tracking_config_id) from etk_tracking_config_archive) "
				+ " and edv.business_key = :dataViewBusinessKey "
				+ " order by display_order "
				)
				.returnEmptyResultSetAs(new ArrayList<HashMap<String, Object>>())
				.setParameter("dataViewBusinessKey", dataViewBusinessKey)
				.fetchList());

		final StringBuilder sq = new StringBuilder();
		final StringBuilder columnList = new StringBuilder();
		final Map<String, Object> parameterMap = new HashMap<>();

		//Loop through all view elements and determine what columns need to be included
		//in the list of columns to retrieve. ID is always included, M_TABLE columns are not.
		if (dataViewColumnList.size() > 0) {

			columnList.append(" ID, ");

			for (final RdoDataElement anElement : dataViewColumnList) {

				if (StringUtility.isNotBlank(anElement.getMTableName())) {
					continue;
				}

				columnList.append(anElement.getColumnName());
				columnList.append(", ");
			}

			columnList.setLength(columnList.length() - 2);
		} else {
			columnList.append(" * ");
		}

		//Continue to build main query.
		sq.append(" from ");
		sq.append(tableName);
		sq.append(" where 1 = 1 ");

		String elementKey;
		String elementOperatorString;
		List<String> elementValues;
		List<String> lowerCaseValues;

		int paramNumber = 0;

		final StringBuilder andClause = new StringBuilder();

		final List<RdoFormValidationError> errorList = new ArrayList<>();

		//Loop through all the Form elements
		for (final RdoDataElement anElement : elementList) {
			elementKey = dataObjectName + "_" + anElement.getName(); //This is the name of the element on the HTML form.
			elementValues = formParameters.get(elementKey); //Existing value list for the HTML form element.

			//Only filter on elements where the user entered a search value.
			if ((elementValues != null) && (elementValues.size() > 0)) {

				//Skip if the value is blank.
				if ((elementValues.size() == 1) && (StringUtility.isBlank(elementValues.get(0)))) {
					continue;
				}

				lowerCaseValues = new ArrayList<>();

				for (final String aValue : elementValues) {
					if (aValue != null) {
						lowerCaseValues.add(aValue.toLowerCase());
					} else {
						lowerCaseValues.add(aValue);
					}
				}

				elementValues = lowerCaseValues;

				//Validate the form element and user input values. If there are errors with the user inputs, add
				//an error and continue to the next element.
				final List<RdoFormValidationError> errorsDetected = validateSearchValues (anElement, elementValues);

				if (errorsDetected.size() > 0) {
					errorList.addAll(errorsDetected);
					continue;
				}

				//Determine equality operator to compare the column to.
				elementOperatorString = formParameters.get(elementKey + "_operator").get(0);

				andClause.append(" and ");

				//For multi-select, we do not want to do a (column <operator> value) comparison, so only append
				//the column name if its not a multi-select comparison.
				if (!("contains_any".equals(elementOperatorString) ||
						"contains_none".equals(elementOperatorString))) {


					final DataElementType elementType = anElement.getDataType();

					if ((elementType == DataElementType.LONG_TEXT) ||
					    (elementType == DataElementType.TEXT)      ||
						(elementType == DataElementType.FILE)      ||
						(elementType == DataElementType.PASSWORD)  ||
						(elementType == DataElementType.STATE)) {
						andClause.append("lower(");
						andClause.append(anElement.getColumnName());
						andClause.append(")");
					} else {
						andClause.append(anElement.getColumnName());
					}
				}



				//Append an AND condition to the query for all fields where the user entered a search value.
				if ("equals".equals(elementOperatorString)) {
					andClause.append(" = :p");
					andClause.append(paramNumber);
					parameterMap.put("p" + paramNumber, getValue(anElement, elementValues));
				} else if ("not_equals".equals(elementOperatorString)) {
					andClause.append(" != :p");
					andClause.append(paramNumber);
					parameterMap.put("p" + paramNumber, getValue(anElement, elementValues));
				} else if ("like".equals(elementOperatorString)) {
					andClause.append(" like ('%' ");
					andClause.append(sqlAppendChar);
					andClause.append(" :p");
					andClause.append(paramNumber);
					andClause.append(" ");
					andClause.append(sqlAppendChar);
					andClause.append(" '%') ");
					parameterMap.put("p" + paramNumber, getValue(anElement, elementValues));
				} else if ("not_like".equals(elementOperatorString)) {
					andClause.append(" not like ('%' ");
					andClause.append(sqlAppendChar);
					andClause.append(" :p");
					andClause.append(paramNumber);
					andClause.append(" ");
					andClause.append(sqlAppendChar);
					andClause.append(" '%') ");
					parameterMap.put("p" + paramNumber, getValue(anElement, elementValues));
				} else if ("starts_with".equals(elementOperatorString)) {
					andClause.append(" like ( :p");
					andClause.append(paramNumber);
					andClause.append(" ");
					andClause.append(sqlAppendChar);
					andClause.append(" '%') ");
					parameterMap.put("p" + paramNumber, getValue(anElement, elementValues));
				} else if ("ends_with".equals(elementOperatorString)) {
					andClause.append(" like ('%' ");
					andClause.append(sqlAppendChar);
					andClause.append(" :p");
					andClause.append(paramNumber);
					andClause.append(" ) ");
					parameterMap.put("p" + paramNumber, getValue(anElement, elementValues));
				} else if ("before".equals(elementOperatorString)) {
					andClause.append(" < :p");
					andClause.append(paramNumber);
					parameterMap.put("p" + paramNumber, getValue(anElement, elementValues));
				} else if ("after".equals(elementOperatorString)) {
					andClause.append(" > :p");
					andClause.append(paramNumber);
					parameterMap.put("p" + paramNumber, getValue(anElement, elementValues));
				} else if ("between".equals(elementOperatorString)) {
					if (StringUtility.isBlank(formParameters.get(elementKey + "_end").get(0))) {
						continue;
					}

					andClause.append(" > :p");
					andClause.append(paramNumber);
					parameterMap.put("p" + paramNumber, getValue(anElement, elementValues));
					paramNumber++;

					andClause.append(" and ");
					andClause.append(anElement.getColumnName());
					andClause.append(" < :p");
					andClause.append(paramNumber);
					parameterMap.put("p" + paramNumber, getValue(anElement, formParameters.get(elementKey + "_end")));
				} else if ("contains_any".equals(elementOperatorString)) {
					andClause.append(" id in ( select id_owner from ");
					andClause.append(anElement.getMTableName());
					andClause.append(" where ");
					andClause.append(anElement.getColumnName());
					andClause.append(" in ( :p");
					andClause.append(paramNumber);
					andClause.append(" )) ");
					parameterMap.put("p" + paramNumber, elementValues);
				} else if ("contains_none".equals(elementOperatorString)) {
					andClause.append(" id not in ( select id_owner from ");
					andClause.append(anElement.getMTableName());
					andClause.append(" where ");
					andClause.append(anElement.getColumnName());
					andClause.append(" in ( :p");
					andClause.append(paramNumber);
					andClause.append(" )) ");
					parameterMap.put("p" + paramNumber, elementValues);
				}

				//Append the AND clause and reset the AND clause, increase the parameter counter
				sq.append(andClause);
				andClause.setLength(0);
				paramNumber++;
			}
		}

		//If errors were encountered, throw a FormValidationException with the error messages encountered.
		if (errorList.size() > 0) {
			final StringBuilder errorMessage = new StringBuilder();

			errorMessage.append("<ul class=\"validation\">");

			for (final RdoFormValidationError anError : errorList) {
				errorMessage.append("<li>");
				errorMessage.append("<a class=\"validation\" href=\"#" +
						StringEscapeUtils.escapeHtml(dataObjectName + "_" + anError.getElementKey()) + "_anchor\">");
				errorMessage.append("<label class=\"validation\" for=\"" +
						StringEscapeUtils.escapeHtml(dataObjectName + "_" + anError.getElementKey()) + "\">");
				errorMessage.append(StringEscapeUtils.escapeHtml(anError.getErrorMessage()));
				errorMessage.append("</label>");
				errorMessage.append("</a>");
				errorMessage.append("</li>");
			}

			errorMessage.append("</ul>");
			errorMessage.append("</div>");

			throw new FormValidationException(errorMessage.toString());
		}

		//Log parameter and query information.
		Utility.aeaLog(etk, "Form Parameter Map = " + Utility.debugMap(formParameters));
		Utility.aeaLog(etk, "Query Parameter Map = " + Utility.debugMap(parameterMap));

		//If configured to filter by organization, make sure that the RDATA record was created by a user within
		//one of the user's allowed organizations.
		if (filterByOrgHierarchy) {
            final List<Long> userOrgList = getOrgUserFilterByList();

            if (userOrgList.size() > 0) {
			   sq.append(" and ");
			   Utility.addLargeInClause("C_ORGANIZATION_ID", sq, parameterMap, userOrgList);

			   parameterMap.put("userAllowedOrgs", getOrgUserFilterByList());
            } else {
               sq.append(" and C_ORGANIZATION_ID in (0) ");
            }
		}

		Utility.aeaLog(etk, "COUNT QUERY = select count(*) " + sq.toString());


		//Determine the total number of rows that meet the user input search criteria and store that parameter in the
		//map of parameters.
		final int rowCount = etk.createSQL("select count(*) " + sq.toString())
				.setParameter(parameterMap)
				.fetchInt();
		Utility.aeaLog(etk, "rowCount = " + rowCount);
		addParameter("rowCount", rowCount);


		//Retrieve how many records per page a user should see (this is between 1 and 500) - this is a user input
		//field on the search results form.
		final int recordsPerPage = new Integer(getSingleParameter("recordsToDisplay"));
		Utility.aeaLog(etk, "recordsPerPage = " + recordsPerPage);


		//Calculates the total number of pages that will fit all records of the resultset.
		final int totalPages = new Integer(Math.round(Math.ceil(rowCount / (recordsPerPage + 0.0))) + "");
		addParameter("totalPages", totalPages);
		Utility.aeaLog(etk, "totalPages = " + getSingleParameter("totalPages"));


		//Get the current page of result records the user wants to view. If an invalid integer is entered, go to page 1.
		int currentPage = 1;
		try {
			currentPage = StringUtility.isBlank(getSingleParameter("navigateToPage")) ?
					      1 : new Integer(getSingleParameter("navigateToPage"));

			//Make sure that the current page is within a valid range of pages.
			if (currentPage < 1) {
				currentPage = 1;
			} else if (currentPage > totalPages) {
				currentPage = totalPages;
			}

		} catch (final Exception e) {
			currentPage = 1;
		}
		addParameter("navigateToPage", currentPage);
		Utility.aeaLog(etk, "currentPage = " + currentPage);

		//Determine the starting index of all records to show.
		final long startIndex = (currentPage <= 1) ? 0 : ((currentPage - 1) * recordsPerPage);
		Utility.aeaLog(etk, "startIndex = " + startIndex);
		addParameter("startIndex", startIndex);

		//Determine the ending index of all records to show.
		final long endIndex = ((currentPage) * recordsPerPage) > rowCount ? rowCount : ((currentPage) * recordsPerPage);
		Utility.aeaLog(etk, "endIndex = " + endIndex);
		addParameter("endIndex", endIndex);

		//If the user clicks on a column to sort the records, the results will be sorted by a sortColumn and sortOrder.
		String orderByClause = " order by null ";

        if (Utility.isSqlServer(etk)) {
           orderByClause = " order by id ";
        }

		String sortColumn;
		String sortOrder;
		if (StringUtility.isNotBlank(sortColumn = getSingleParameter("sortColumn")) &&
				StringUtility.isNotBlank(sortOrder = getSingleParameter("sortOrder"))) {
			orderByClause = " order by " + sortColumn + " " + sortOrder;
			orderByClause = StringEscapeUtils.escapeSql(orderByClause);
		}

		//This logs the completed query.
		Utility.aeaLog(etk, "QUERY = select * from (select row_number() over (partition by null "
				+ orderByClause
				+ ") aea_row_number, "
				+ columnList.toString()
				+ sq.toString()
				+ ") tempQuery where aea_row_number > "
				+ startIndex
				+ " and aea_row_number <= "
				+ endIndex
				);

		//Runs the query and returns a list of results to be printed per the configured view.
		final List<Map<String, Object>> resultSet = etk.createSQL(
				"select * from (select row_number() over (partition by null "
						+ orderByClause
						+ ") aea_row_number, "
						+ columnList.toString()
						+ sq.toString()
						+ ") tempQuery where aea_row_number > "
						+ startIndex
						+ " and aea_row_number <= "
						+ endIndex
				)
				.returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
				.setParameter(parameterMap)
				.fetchList();

		//Builds the auditLogResultTable.
		final RdoSearchPagedTableBuilder ptb = new RdoSearchPagedTableBuilder(etk, formParameters, militaryTimeMode, linkToRdo);
		ptb.setRowAttributeList(dataViewColumnList);
		ptb.setResultSet(resultSet);

		return ptb.buildTable(dataObjectKey, "auditLogResultTable");
	}


	private String getBooleanSearchField(final String aLabel, final String aFormPrefix, final String aFieldName) {
	    return "<tr>"
			 + "<td valign=\"top\">"
			 + "<label class=\"formLabel\" for=\"" + aFormPrefix + "_" + aFieldName + "\"> "
			 + aLabel
			 + " </label>"
			 + "</td>"
			 + "<td>"
			 + "<input type=\"hidden\" value=\"equals\" name=\"" + aFormPrefix + "_" + aFieldName + "_operator\"> "
			 + "EQUAL TO"
			 + " </td>"
			 + "<td>"
			 + "<label for=\"" + aFormPrefix + "_" + aFieldName + "_yes\">"
			 + "<input id=\"" + aFormPrefix + "_" + aFieldName + "_yes\" type=\"radio\" "
			 + isChecked(aFormPrefix + "_" + aFieldName, "1")
			 + " value=\"1\" name=\""
			 + aFormPrefix + "_" + aFieldName + "\"> "
			 + "Yes"
			 + " </label>"
			 + "<label for=\"" + aFormPrefix + "_" + aFieldName + "_no\">"
			 + "<input id=\"" + aFormPrefix + "_" + aFieldName + "_no\" type=\"radio\" "
			 + isChecked(aFormPrefix + "_" + aFieldName, "0")
			 + " value=\"0\" name=\""
			 + aFormPrefix + "_" + aFieldName + "\"> "
			 + "No"
			 + " </label>"
			 + "</td>"
			 + "</tr>";
	}

	private String getDateSearchField(final String aLabel, final String aFormPrefix, final String aFieldName) {
		jsNeeded.append("Calendar.setup({\n");
		jsNeeded.append("inputField : \"");
		jsNeeded.append(aFormPrefix);
		jsNeeded.append("_");
		jsNeeded.append(aFieldName);
		jsNeeded.append("\",\n");
		if (militaryTimeMode) {
			jsNeeded.append("ifFormat : \"%Y%m%d\",\n");
		} else {
			jsNeeded.append("ifFormat : \"%m/%d/%Y\",\n");
		}
		jsNeeded.append("showsTime : false,\n");
		jsNeeded.append("button : \"anchor_");
		jsNeeded.append(aFormPrefix);
		jsNeeded.append("_");
		jsNeeded.append(aFieldName);
		jsNeeded.append("\",\n");
		jsNeeded.append("singleClick : true,\n");
		jsNeeded.append("step : 1\n");
		jsNeeded.append("});\n");

		jsNeeded.append("Calendar.setup({\n");
		jsNeeded.append("inputField : \"");
		jsNeeded.append(aFormPrefix);
		jsNeeded.append("_");
		jsNeeded.append(aFieldName);
		jsNeeded.append("_end\",\n");
		if (militaryTimeMode) {
			jsNeeded.append("ifFormat : \"%Y%m%d\",\n");
		} else {
			jsNeeded.append("ifFormat : \"%m/%d/%Y\",\n");
		}
		jsNeeded.append("showsTime : false,\n");
		jsNeeded.append("button : \"anchor_");
		jsNeeded.append(aFormPrefix);
		jsNeeded.append("_");
		jsNeeded.append(aFieldName);
		jsNeeded.append("_end\",\n");
		jsNeeded.append("singleClick : true,\n");
		jsNeeded.append("step : 1\n");
		jsNeeded.append("});\n\n");

		jsNeeded.append("showBetween(document.getElementById('searchForm'), '" + aFormPrefix + "_" + aFieldName + "', '" + aFormPrefix + "_" + aFieldName + "_operator');\n");

		final StringBuilder ob = new StringBuilder();
	     ob.append("<tr>");
	     ob.append("<td valign=\"top\">");
	     ob.append("<label class=\"formLabel\" for=\"" + aFormPrefix + "_" + aFieldName + "\">");
	     ob.append(aLabel);
		 if (militaryTimeMode) {
			 ob.append("<img alt=\"yyyymmdd\" title=\"yyyymmdd\" src=\"themes/default/web-pub/images/spacers/1x1.gif\">");
		 } else {
			 ob.append("<img alt=\"mm/dd/yyyy\" title=\"mm/dd/yyyy\" src=\"themes/default/web-pub/images/spacers/1x1.gif\">");
		 }
	     ob.append("</label>");
	     ob.append("</td>");
	     ob.append("<td>");
	     ob.append("<select class=\"criteria\" onchange=\"showBetween(form, '" + aFormPrefix + "_" + aFieldName + "', '" + aFormPrefix + "_" + aFieldName + "_operator')\" title=\"" + aLabel + " operator\" alt=\"" + aLabel + " operator\" name=\"" + aFormPrefix + "_" + aFieldName + "_operator\">");
	     ob.append("<option value=\"equals\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "equals") + "> EQUAL TO </option>");
	     ob.append("<option value=\"not_equals\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "not_equals") + "> NOT EQUAL TO </option>");
	     ob.append("<option value=\"before\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "before") + "> BEFORE </option>");
	     ob.append("<option value=\"after\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "after") + "> AFTER </option>");
	     ob.append("<option value=\"between\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "between") + "> BETWEEN </option>");
	     ob.append("</select>");
	     ob.append("</td>");
	     ob.append("<td>");
	     ob.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">");
	     ob.append("<tbody>");
	     ob.append("<tr>");
	     ob.append("<td>");
	     ob.append("<input id=\"" + aFormPrefix + "_" + aFieldName + "\" type=\"text\" " + getValueParam(aFormPrefix + "_" + aFieldName) + " title=\"" + aLabel + " value\" alt=\"" + aLabel + " value\" name=\"" + aFormPrefix + "_" + aFieldName + "\" maxlength=\"10\" size=\"10\">");
	     ob.append("</td>");
	     ob.append("<td>");
	     ob.append("<img id=\"anchor_" + aFormPrefix + "_" + aFieldName + "\" border=\"0\" alt=\"Select Date\" title=\"Select Date\" src=\"themes/default/web-pub/images/icons/calendar.gif\">");
	     ob.append("</td>");
		 if (militaryTimeMode) {
			 ob.append("<td> (yyyymmdd) </td>");
		 } else {
			 ob.append("<td> (mm/dd/yyyy) </td>");
		 }
	     ob.append("<td>");
	     ob.append("<span id=\"" + aFormPrefix + "_" + aFieldName + "_and\" class=\"hide\"> AND </span>");
	     ob.append("</td>");
	     ob.append("<td>");
	     ob.append("<span id=\"" + aFormPrefix + "_" + aFieldName + "_end_text\" class=\"hide\">");
	     ob.append("<input id=\"" + aFormPrefix + "_" + aFieldName + "_end\" type=\"text\" " + getValueParam(aFormPrefix + "_" + aFieldName + "_end") + " title=\"Enter the search end date for " + aLabel + "\" alt=\"Enter the search end date for " + aLabel + "\" name=\"" + aFormPrefix + "_" + aFieldName + "_end\" maxlength=\"10\" size=\"10\">");
	     ob.append("</span>");
	     ob.append("</td>");
	     ob.append("<td>");
	     ob.append("<span id=\"" + aFormPrefix + "_" + aFieldName + "_end_lu\" class=\"hide\">");
	     ob.append("<img id=\"anchor_" + aFormPrefix + "_" + aFieldName + "_end\" border=\"0\" alt=\"Select Date\" title=\"Select Date\" src=\"themes/default/web-pub/images/icons/calendar.gif\">");
	     ob.append("</span>");
	     ob.append("</td>");
	     ob.append("<td>");
		 if (militaryTimeMode) {
			 ob.append("<span id=\"" + aFormPrefix + "_" + aFieldName + "_end_format\" class=\"hide\"> (yyyymmdd) </span>");
		 } else {
			 ob.append("<span id=\"" + aFormPrefix + "_" + aFieldName + "_end_format\" class=\"hide\"> (mm/dd/yyyy) </span>");
		 }
	     ob.append("</td>");
	     ob.append("</tr>");
	     ob.append("</tbody>");
	     ob.append("</table>");
	     ob.append("</td>");
	     ob.append("</tr>");

	     return ob.toString();
	}

	private String getLongTextSearchField (final String aLabel, final String aFormPrefix, final String aFieldName) {
		return "<tr>"
			 + "<td valign=\"top\">"
			 + "<label class=\"formLabel\" for=\"" + aFormPrefix + "_" + aFieldName + "\"> " + aLabel + " </label>"
			 + "</td>"
			 + "<td>"
			 + "<select class=\"criteria\" title=\"" + aLabel + " operator\" alt=\"" + aLabel + " operator\" name=\"" + aFormPrefix + "_" + aFieldName + "_operator\">"
			 + "<option value=\"like\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "like") + "> LIKE </option>"
			 + "<option value=\"not_like\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "not_like") + "> NOT LIKE </option>"
			 + "<option value=\"starts_with\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "starts_with") + "> STARTS WITH </option>"
			 + "<option value=\"ends_with\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "ends_with") + "> ENDS WITH </option>"
			 + "</select>"
			 + "</td>"
			 + "<td>"
			 + "<input id=\"" + aFormPrefix + "_" + aFieldName + "\" class=\"searchInput\" type=\"text\" " + getValueParam(aFormPrefix + "_" + aFieldName) + " title=\"" + aLabel + " value\" alt=\"" + aLabel + " value\" name=\"" + aFormPrefix + "_" + aFieldName + "\">"
			 + "</td>"
			 + "</tr>";
	}

	private String getTextSearchField (final String aLabel, final String aFormPrefix, final String aFieldName) {
	    return "<tr>"
			 + "<td valign=\"top\">"
			 + "<label class=\"formLabel\" for=\"" + aFormPrefix + "_" + aFieldName + "\"> " + aLabel + " </label>"
			 + "</td>"
			 + "<td>"
			 + "<select class=\"criteria\" title=\"" + aLabel + " operator\" alt=\"" + aLabel + " operator\" name=\"" + aFormPrefix + "_" + aFieldName + "_operator\">"
			 + "<option value=\"equals\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "equals") + "> EQUAL TO </option>"
			 + "<option value=\"not_equals\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "not_equals") + "> NOT EQUAL TO </option>"
			 + "<option value=\"like\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "like") + "> LIKE </option>"
			 + "<option value=\"not_like\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "not_like") + "> NOT LIKE </option>"
			 + "<option value=\"starts_with\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "starts_with") + "> STARTS WITH </option>"
			 + "<option value=\"ends_with\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "ends_with") + "> ENDS WITH </option>"
			 + "</select>"
			 + "</td>"
			 + "<td>"
			 + "<input id=\"" + aFormPrefix + "_" + aFieldName + "\" class=\"searchInput\" type=\"" + aFieldName + "\" " + getValueParam(aFormPrefix + "_" + aFieldName) + " title=\"" + aLabel + " value\" alt=\"" + aLabel + " value\" name=\"" + aFormPrefix + "_" + aFieldName + "\" maxlength=\"255\">"
			 + "</td>"
			 + "</tr>";
	}

	private String getCustomDataTypeSearchField (final String aLabel,
			                                     final String aFormPrefix,
			                                     final String aFieldName,
			                                     final String dataObjectBusinessKey,
			                                     final String dataElementBusinessKey,
			                                     final Long formControlId) {
		final StringBuilder sb = new StringBuilder();

	    sb.append("<tr>");
	    sb.append("<td valign=\"top\">");
	    sb.append("<label class=\"formLabel\" for=\"" + aFormPrefix + "_" + aFieldName + "\"> " + aLabel + " </label>");
	    sb.append("</td>");
	    sb.append("<td>");
	    sb.append("<select class=\"criteria\" title=\"" + aLabel + " operator\" alt=\"" + aLabel + " operator\" name=\"" + aFormPrefix + "_" + aFieldName + "_operator\">");
	    sb.append("<option value=\"equals\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "equals") + "> EQUAL TO </option>");
	    sb.append("<option value=\"not_equals\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "not_equals") + "> NOT EQUAL TO </option>");
	    sb.append("<option value=\"like\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "like") + "> LIKE </option>");
	    sb.append("<option value=\"not_like\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "not_like") + "> NOT LIKE </option>");
	    sb.append("<option value=\"starts_with\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "starts_with") + "> STARTS WITH </option>");
	    sb.append("<option value=\"ends_with\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "ends_with") + "> ENDS WITH </option>");
	    sb.append("</select>");
	    sb.append("</td>");
	    sb.append("<td>");
	    try {
			sb.append(DataTypePluginClassUtility.getPluginDataTypeWidget(
					etk,
					dataObjectBusinessKey,
					dataElementBusinessKey,
					(aFormPrefix + "_" + aFieldName),
					formControlId,
					getSingleParameter(aFormPrefix + "_" + aFieldName),
					false));
		} catch (final IOException e) {
			throw new DataAccessException (e);
		} catch (final IncorrectResultSizeDataAccessException e) {
			throw new DataAccessException (e);
		}
	    sb.append("</td>");
	    sb.append("</tr>");

	    return sb.toString();
	}

	private String getDropdownField(final String aLabel, final String aFormPrefix,
			                       final String aFieldName, final String lookupBusinessKey) {
		final StringBuilder sb = new StringBuilder();



		sb.append("<tr>");
		sb.append("<td valign=\"top\">");
		sb.append("<label class=\"formLabel\" for=\"" + aFormPrefix + "_" + aFieldName + "\"> " + aLabel + " </label>");
		sb.append("</td>");
		sb.append("<td>");
		sb.append("<select class=\"criteria\" title=\"" + aLabel + " operator\" alt=\"" + aLabel + " operator\" name=\"" + aFormPrefix + "_" + aFieldName + "_operator\">");
		sb.append("<option value=\"equals\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "equals") + "> EQUAL TO </option>");
		sb.append("<option value=\"not_equals\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "not_equals") + "> NOT EQUAL TO </option>");
		sb.append("</select>");
		sb.append("</td>");
		sb.append("<td valign=\"top\">");
		sb.append("<select id=\"" + aFormPrefix + "_" + aFieldName + "\" class=\"formInput\" name=\"" + aFormPrefix + "_" + aFieldName + "\" title=\"" + aLabel + " value\" alt=\"" + aLabel + " value\">");

		try {
		   final List<LookupResult> lookupValues = etk.getLookupService().getLookup(lookupBusinessKey).execute(For.SEARCH);

		   sb.append("<option value=\"\"></option>");

			for (final LookupResult aLookupValue : lookupValues) {
				sb.append("<option value=\"");
				sb.append(StringEscapeUtils.escapeHtml(aLookupValue.getValue()));
				sb.append("\" ");
				sb.append(isSelected(aFormPrefix + "_" + aFieldName, aLookupValue.getValue()));
				sb.append(">");
				sb.append(StringEscapeUtils.escapeHtml(aLookupValue.getDisplay()));
				sb.append("</option>");
			}
		} catch (final Exception e) {
			sb.append("<option value=\"\">Error Executing For.SEARCH lookup for Lookup with Business Key '");
			sb.append(StringEscapeUtils.escapeHtml(lookupBusinessKey));
			sb.append("'</option>");
		}

		sb.append("</select>");
		sb.append("</td>");
		sb.append("</tr>");

		return sb.toString();
	}

	private String getMultiselectField (final String aLabel, final String aFormPrefix,
                                       final String aFieldName, final String lookupBusinessKey) {
		final StringBuilder sb = new StringBuilder();


		sb.append("<tr>");
		sb.append("<td valign=\"top\">");
		sb.append("<label class=\"formLabel\" for=\"" + aFormPrefix + "_" + aFieldName + "\"> " + aLabel + " </label>");
		sb.append("</td>");
		sb.append("<td valign=\"top\">");
		sb.append("<select class=\"criteria\" title=\"" + aLabel + " operator\" alt=\"" + aLabel + " operator\" name=\"" + aFormPrefix + "_" + aFieldName + "_operator\">");
		sb.append("<option value=\"contains_any\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "contains_any") + "> CONTAINS ANY </option>");
		sb.append("<option value=\"contains_none\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "contains_none") + "> DOES NOT CONTAIN </option>");
		sb.append("</select>");
		sb.append("</td>");
		sb.append("<td valign=\"top\">");
		sb.append("<div class=\"overflowDiv\" id=\"" + aFormPrefix + "_" + aFieldName + "_multiValue\">");

		int msListNum = 1;

		try {
			final List<LookupResult> lookupValues = etk.getLookupService().getLookup(lookupBusinessKey).execute(For.SEARCH);

			for (final LookupResult aLookupValue : lookupValues) {
				sb.append("<label for=\"" + aFormPrefix + "_" + aFieldName + "_checkbox_" + msListNum + "\">");
				sb.append("<input ");
				sb.append(isCheckedMs(aFormPrefix + "_" + aFieldName, aLookupValue.getValue()));
				sb.append(" id=\"" + aFormPrefix + "_" + aFieldName + "_checkbox_" + msListNum++ + "\" type=\"checkbox\" value=\"" + StringEscapeUtils.escapeHtml(aLookupValue.getValue()) + "\" name=\"" + aFormPrefix + "_" + aFieldName + "\">");
				sb.append(StringEscapeUtils.escapeHtml(aLookupValue.getDisplay()));
				sb.append("</label>");
				sb.append("<br>");
			}
		} catch (final Exception e) {
			sb.append("<label for=\"" + aFormPrefix + "_" + aFieldName + "_checkbox_" + ++msListNum +
					  "\">Error Executing For.SEARCH lookup for Lookup with Business Key ");
			sb.append(StringEscapeUtils.escapeHtml(lookupBusinessKey));
			sb.append("'</label>");
			sb.append("<br>");
		}

		sb.append("</div>");
		sb.append("</td>");
		sb.append("</tr>");

		return sb.toString();
	}

	private String getTimestampSearchField(final String aLabel, final String aFormPrefix, final String aFieldName) {

		jsNeeded.append("Calendar.setup({\n");
		jsNeeded.append("inputField : \"");
		jsNeeded.append(aFormPrefix);
		jsNeeded.append("_");
		jsNeeded.append(aFieldName);
		jsNeeded.append("\",\n");
		if (militaryTimeMode) {
			jsNeeded.append("ifFormat : \"%Y%m%d %H%M\",\n");
		} else {
			jsNeeded.append("ifFormat : \"%m/%d/%Y %I:%M %p\",\n");
		}
		jsNeeded.append("showsTime : true,\n");
		jsNeeded.append("button : \"anchor_");
		jsNeeded.append(aFormPrefix);
		jsNeeded.append("_");
		jsNeeded.append(aFieldName);
		jsNeeded.append("\",\n");
		jsNeeded.append("singleClick : true,\n");
		jsNeeded.append("step : 1,\n");
		if (militaryTimeMode) {
			jsNeeded.append("timeFormat : \"24\"\n");
		} else {
			jsNeeded.append("timeFormat : \"12\"\n");
		}
		jsNeeded.append("});\n");

		jsNeeded.append("Calendar.setup({\n");
		jsNeeded.append("inputField : \"");
		jsNeeded.append(aFormPrefix);
		jsNeeded.append("_");
		jsNeeded.append(aFieldName);
		jsNeeded.append("_end\",\n");
		if (militaryTimeMode) {
			jsNeeded.append("ifFormat : \"%Y%m%d %H%M\",\n");
		} else {
			jsNeeded.append("ifFormat : \"%m/%d/%Y %I:%M %p\",\n");
		}
		jsNeeded.append("showsTime : true,\n");
		jsNeeded.append("button : \"anchor_");
		jsNeeded.append(aFormPrefix);
		jsNeeded.append("_");
		jsNeeded.append(aFieldName);
		jsNeeded.append("_end\",\n");
		jsNeeded.append("singleClick : true,\n");
		jsNeeded.append("step : 1,\n");
		if (militaryTimeMode) {
			jsNeeded.append("timeFormat : \"24\"\n");
		} else {
			jsNeeded.append("timeFormat : \"12\"\n");
		}
		jsNeeded.append("});\n\n");

		jsNeeded.append("showBetween(document.getElementById('searchForm'), '" + aFormPrefix + "_" + aFieldName + "', '" + aFormPrefix + "_" + aFieldName + "_operator');\n");

		String currentTimeZone = " ";
		if (etk.getCurrentUser().getTimeZonePreference() != null) {
			currentTimeZone = " " + StringEscapeUtils.escapeHtml(etk.getCurrentUser().getTimeZonePreference().getTimeZone().getDisplayString()) + " ";
		}

		final StringBuilder ob = new StringBuilder();
		ob.append("<tr>");
		 ob.append("<td valign=\"top\">");
		 ob.append("<label class=\"formLabel\" for=\"" + aFormPrefix + "_" + aFieldName + "\">");
		 ob.append(aLabel);
		 if (militaryTimeMode) {
			 ob.append("<img alt=\"yyyymmdd hhmm\" title=\"yyyymmdd hhmm\" src=\"themes/default/web-pub/images/spacers/1x1.gif\">");
		 } else {
			 ob.append("<img alt=\"mm/dd/yyyy hh:mm AM/PM\" title=\"mm/dd/yyyy hh:mm AM/PM\" src=\"themes/default/web-pub/images/spacers/1x1.gif\">");
		 }
		 ob.append("</label>");
		 ob.append("</td>");
		 ob.append("<td>");
		 ob.append("<select class=\"criteria\" onchange=\"showBetween(form, '" + aFormPrefix + "_" + aFieldName + "', '" + aFormPrefix + "_" + aFieldName + "_operator')\" title=\"" + aLabel + " operator\" alt=\"" + aLabel + " operator\" name=\"" + aFormPrefix + "_" + aFieldName + "_operator\">");
		 ob.append("<option value=\"equals\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "equals") + "> EQUAL TO </option>");
		 ob.append("<option value=\"not_equals\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "not_equals") + "> NOT EQUAL TO </option>");
		 ob.append("<option value=\"before\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "before") + "> BEFORE </option>");
		 ob.append("<option value=\"after\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "after") + "> AFTER </option>");
		 ob.append("<option value=\"between\" " + isSelected(aFormPrefix + "_" + aFieldName + "_operator", "between") + "> BETWEEN </option>");
		 ob.append("</select>");
		 ob.append("</td>");
		 ob.append("<td>");
		 ob.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">");
		 ob.append("<tbody>");
		 ob.append("<tr>");
		 ob.append("<td>");
		 ob.append("<input id=\"" + aFormPrefix + "_" + aFieldName + "\" type=\"text\" " + getValueParam(aFormPrefix + "_" + aFieldName) + " title=\"" + aLabel + " value\" alt=\"" + aLabel + " value\" name=\"" + aFormPrefix + "_" + aFieldName + "\" maxlength=\"19\" size=\"19\">");
		 ob.append("</td>");
		 ob.append("<td>");
		 ob.append("<img id=\"anchor_" + aFormPrefix + "_" + aFieldName + "\" border=\"0\" alt=\"Select " + aLabel + " timestamp\" title=\"Select " + aLabel + " timestamp\" src=\"themes/default/web-pub/images/icons/calendar.gif\">");
		 ob.append("</td>");
		 if (militaryTimeMode) {
			 ob.append("<td>" + currentTimeZone + "(yyyymmdd hhmm) </td>");
		 } else {
			 ob.append("<td>" + currentTimeZone + "(mm/dd/yyyy hh:mm AM/PM) </td>");
		 }
		 ob.append("<td>");
		 ob.append("<span id=\"" + aFormPrefix + "_" + aFieldName + "_and\" class=\"hide\"> AND </span>");
		 ob.append("</td>");
		 ob.append("<td>");
		 ob.append("<span id=\"" + aFormPrefix + "_" + aFieldName + "_end_text\" class=\"hide\">");
		 ob.append("<input id=\"" + aFormPrefix + "_" + aFieldName + "_end\" type=\"text\" " + getValueParam(aFormPrefix + "_" + aFieldName + "_end") + " name=\"" + aFormPrefix + "_" + aFieldName + "_end\" title=\"Enter the search end date for " + aLabel + "\" alt=\"Enter the search end date for " + aLabel + "\" maxlength=\"19\" size=\"19\">");
		 ob.append("</span>");
		 ob.append("</td>");
		 ob.append("<td>");
		 ob.append("<span id=\"" + aFormPrefix + "_" + aFieldName + "_end_lu\" class=\"hide\">");
		 ob.append("<img id=\"anchor_" + aFormPrefix + "_" + aFieldName + "_end\" border=\"0\" alt=\"Select " + aLabel + " timestamp\" title=\"Select " + aLabel + " timestamp\" src=\"themes/default/web-pub/images/icons/calendar.gif\">");
		 ob.append("</span>");
		 ob.append("</td>");
		 ob.append("<td>");
		 if (militaryTimeMode) {
			 ob.append("<span id=\"" + aFormPrefix + "_" + aFieldName + "_end_format\" class=\"hide\">" + currentTimeZone + "(yyyymmdd hhmm) </span>");
		 } else {
			 ob.append("<span id=\"" + aFormPrefix + "_" + aFieldName + "_end_format\" class=\"hide\">" + currentTimeZone + "(mm/dd/yyyy hh:mm AM/PM) </span>");
		 }
		 ob.append("</td>");
		 ob.append("</tr>");
		 ob.append("</tbody>");
		 ob.append("</table>");
		 ob.append("</td>");
		 ob.append("</tr>");

		 return ob.toString();
	}

	/**
	 * Gets a list of org hierarchy hierarchy_id values that the user is allowed to see (configured via ETK Access Level
	 * and the user's main configured hierarchy.) Includes all children.
	 *
	 * @return List of allowed hierarchy_id values for the current user.
	 */
	private List<Long> getOrgUserFilterByList() {
		final List<Long> orgIdList = new ArrayList<>();

		final List<Map<String, Object>> resultList = etk.createSQL(
			  " with all_hierarchys (hierarchy_id, node_id) AS ( "
			+ "		  SELECT main_hierarchy.hierarchy_id, main_hierarchy.node_id "
			+ "		  FROM etk_hierarchy main_hierarchy "
			+ "		  LEFT JOIN etk_access_level al on main_hierarchy.hierarchy_id = al.hierarchy_id "
			+ "		  LEFT JOIN etk_subject_role sr ON al.subject_role_id = sr.subject_role_id  "
			+ "		  WHERE (sr.subject_id = :userId AND sr.role_id = :userRoleId) "
			+ "       OR main_hierarchy.hierarchy_id = :userHierarchyId "

			+ "		  UNION ALL "

			+ "		  SELECT recursive_hierarchy.hierarchy_id, recursive_hierarchy.node_id "
			+ "		  FROM etk_hierarchy recursive_hierarchy, all_hierarchys "
			+ "		  WHERE recursive_hierarchy.parent_id = all_hierarchys.node_id  "
			+ "		  and recursive_hierarchy.parent_id != recursive_hierarchy.node_id "
			+ " ) "
			+ " select distinct HIERARCHY_ID from all_hierarchys "
		)
		.returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
		.setParameter("userId", etk.getCurrentUser().getId())
		.setParameter("userHierarchyId", etk.getCurrentUser().getHierarchy().getId())
		.setParameter("userRoleId", etk.getCurrentUser().getRole().getId())
		.fetchList();

		for (final Map<String, Object> aResult : resultList) {
			orgIdList.add(((BigDecimal) aResult.get("HIERARCHY_ID")).longValue());
		}

		return orgIdList;
	}

	/**
	 * This method validates a form search field to make sure the user input is a valid search value.
	 *
	 * @param anElement A Data element to validate.
	 * @param elementValues The user input values for that element.
	 * @return A list of errors found while validating the element.
	 */
	private List<RdoFormValidationError> validateSearchValues (final RdoDataElement anElement, final List<String> elementValues) {


		final DataElementType dataElementType = anElement.getDataType();
		final String label =  anElement.getLabel();
		final List<RdoFormValidationError> errorList = new ArrayList<>();

		String value;

		if (dataElementType == DataElementType.YES_NO) {
			value = elementValues.get(0);

			if (!StringUtility.isBlank(value)) {
				if (!("0".equals(value) || "1".equals(value))) {

					final RdoFormValidationError fve = new RdoFormValidationError();
					fve.setErrorMessage("\"" + label + "\" must be a Yes (1) or No (0) value.");
					fve.setElementKey(anElement.getName());

					errorList.add(fve);
				}
			}
		} else if (dataElementType == DataElementType.CURRENCY) {
			value = elementValues.get(0);

			if (!StringUtility.isBlank(value)) {
				try {
					new Double(value);
				} catch (final NumberFormatException nfe) {
					final RdoFormValidationError fve = new RdoFormValidationError();
					fve.setErrorMessage("" + label + " must be a valid currency (float).");
					fve.setElementKey(anElement.getName());

					errorList.add(fve);
				}
			}
		} else if (dataElementType == DataElementType.NUMBER || dataElementType == DataElementType.LONG) {
			value = elementValues.get(0);

			if (!StringUtility.isBlank(value)) {
				try {
					new Long(value);
				} catch (final NumberFormatException nfe) {
					final RdoFormValidationError fve = new RdoFormValidationError();
					fve.setErrorMessage("\"" + label + "\" must be a valid number (integer).");
					fve.setElementKey(anElement.getName());

					errorList.add(fve);
				}
			}
		} else if (dataElementType == DataElementType.DATE) {
			value = elementValues.get(0);

			if (!StringUtility.isBlank(value)) {
				Date tmpDate = null;

				if (militaryTimeMode) {
					try {
					   tmpDate = parseMilitaryDateString(value);
					} catch (final Exception e) {
						Utility.aeaLog(etk, e);
					}
				} else {
					tmpDate = DateUtility.parseDate(value);
				}

				//This means it could not parse the date.
				if (tmpDate == null) {
					final RdoFormValidationError fve = new RdoFormValidationError();

					if (militaryTimeMode) {
						fve.setErrorMessage("\"" + label + "\" must be a valid date in the format yyyymmdd.");
					} else {
						fve.setErrorMessage("\"" + label + "\" must be a valid date in the format mm/dd/yyyy.");
					}

					fve.setElementKey(anElement.getName());

					errorList.add(fve);

				}
			}
		} else if (dataElementType == DataElementType.TIMESTAMP) {
			value = elementValues.get(0);

			if (!StringUtility.isBlank(value)) {
				Date tmpDate = null;
				if (militaryTimeMode) {
					try {
					   tmpDate = parseMilitaryTimeString(value);
					} catch (final Exception e) {
						Utility.aeaLog(etk, e);
					}
				} else {
					tmpDate = DateUtility.parseDateTime(value);
				}


				//This means it could not parse the timestamp.
				if (tmpDate == null) {
					final RdoFormValidationError fve = new RdoFormValidationError();
					if (militaryTimeMode) {
						fve.setErrorMessage("\"" + label + "\" must be a valid date in the format yyyymmdd hhmm.");
					} else {
						fve.setErrorMessage("\"" + label + "\" must be a valid date in the format mm/dd/yyyy hh:mm AM/PM.");
					}

					fve.setElementKey(anElement.getName());

					errorList.add(fve);

				}
			}
		}

		return errorList;
	}

	/**
	 * Helper method to build a typed RdoDataElement from a generic Map query result.
	 *
	 * @param queryResult A query containing a list of Maps with RdoDataElement data in them.
	 * @return List of typed RdoDataElements.
	 */
	private List<RdoDataElement> getRdoDataElements (final List<Map<String, Object>> queryResult) {
		RdoDataElement fde;
		final List<RdoDataElement> fdeList = new ArrayList<>();

		for (final Map<String, Object> anElement : queryResult) {
			fde = new RdoDataElement();

			final BigDecimal dataElementTypeNumber = (BigDecimal) anElement.get("DATA_TYPE");

			if (dataElementTypeNumber != null) {
				fde.setDataType(DataElementType.getDataElementType(dataElementTypeNumber.longValue()));
			}

			fde.setColumnName((String) anElement.get("COLUMN_NAME"));
			fde.setFormControlType((String) anElement.get("FORM_CONTROL_TYPE"));
			fde.setLabel((String) anElement.get("LABEL"));
			fde.setLookupBusinessKey((String) anElement.get("LOOKUP_BUSINESS_KEY"));
			fde.setName((String) anElement.get("NAME"));
			fde.setMTableName((String) anElement.get("M_TABLE_NAME"));

			fdeList.add(fde);
		}

		return fdeList;
	}

	/**
	 * Returns a single parameter value from the form parameters map based on the parameter name.
	 *
	 * @param parameterName The name of a form parameter.
	 * @return The form parameter's value.
	 */
    private String getSingleParameter(final String parameterName) {
    	if (formParameters.get(parameterName) != null) {
    		return formParameters.get(parameterName).get(0);
    	}

    	return null;
    }

    private void addParameter (final String key, final Object value) {
    	final List <String> tmpList = new ArrayList<>();
    	tmpList.add(value + "");

    	formParameters.put(key, tmpList);
    }

    private String getValueParam (final String inputId) {
    	final String unEscapedValue = getSingleParameter (inputId);

    	if (unEscapedValue == null) {
    		return " value=\"\" ";
    	} else {
    		return " value =\"" + StringEscapeUtils.escapeHtml(unEscapedValue) + "\" ";
    	}
    }

    private String isSelected (final String fieldName, final String value) {
    	final String unEscapedValue = getSingleParameter(fieldName);

    	if (value.equals(unEscapedValue)) {
    		return " selected=\"\" ";
    	} else {
    		return " ";
    	}
    }

    private String isChecked (final String fieldName, final String value) {
    	final String unEscapedValue = getSingleParameter(fieldName);

    	if (value.equals(unEscapedValue)) {
    		return " checked=\"checked\" ";
    	} else {
    		return " ";
    	}
    }

    private String isCheckedMs (final String fieldName, final String value) {
    	final List<String> unEscapedValueList = formParameters.get(fieldName);

    	if (unEscapedValueList != null) {
    		for (final String aValue : unEscapedValueList) {
    			if (value.equals(aValue)) {
    				return " checked=\"\" ";
    			}
    		}
    	}

    	return " ";
    }

    /**
     * Returns an element value in the same expected format.
     *
     * @param anElement the data element
     * @param elementValues the value(s) of the element for a particular record
     * @return A java representation of the element's value
     */
    private Object getValue (final RdoDataElement anElement, final List<String> elementValues) {
		final DataElementType dataElementType = anElement.getDataType();

		if (dataElementType == DataElementType.YES_NO) {
			return new Integer(elementValues.get(0));
		} else if (dataElementType == DataElementType.CURRENCY) {
			return new Double(elementValues.get(0));
		} else if (dataElementType == DataElementType.NUMBER || dataElementType == DataElementType.LONG) {
			return new Long(elementValues.get(0));
		} else if (dataElementType == DataElementType.DATE) {
			if (militaryTimeMode) {
				try {
					return parseMilitaryDateString(elementValues.get(0));
				} catch (final ParseException e) {
					Utility.aeaLog(etk, "Error parsing date string " + elementValues.get(0));

					return null;
				}
			} else {
				return DateUtility.parseDate(elementValues.get(0));
			}

		} else if (dataElementType == DataElementType.TIMESTAMP) {
			  try {
				if (etk.getCurrentUser().getTimeZonePreference() != null) {
					if (militaryTimeMode) {
						final String tmpTime = DateUtility.getFormattedDateTime(
								 parseMilitaryTimeString(elementValues.get(0)));
					   return Localizations.toServerTimestamp(etk.getCurrentUser().getTimeZonePreference(),
								                               tmpTime).getDateValue();
					} else {
					   return Localizations.toServerTimestamp(etk.getCurrentUser().getTimeZonePreference(),
						                                 elementValues.get(0)).getDateValue();
					}
				} else {
					if (militaryTimeMode) {
						return parseMilitaryTimeString(elementValues.get(0));
					} else {
						return DateUtility.parseDateTime(elementValues.get(0));
					}
				}
			} catch (final Exception e) {
				Utility.aeaLog(etk, "Error localizing timestamp " + elementValues.get(0), e);

				if (militaryTimeMode) {
					try {
						return parseMilitaryTimeString(elementValues.get(0));
					} catch (final ParseException pe) {
						Utility.aeaLog(etk, pe);

						return null;
					}
				} else {
					return DateUtility.parseDateTime(elementValues.get(0));
				}
			}
		} else {
			return elementValues.get(0);
		}
    }

    private Date parseMilitaryDateString (final String aMilitaryDateString) throws ParseException  {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setLenient(false);
		return sdf.parse(aMilitaryDateString);
    }

    private Date parseMilitaryTimeString (final String aMilitaryTimeString) throws ParseException  {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmm");
		sdf.setLenient(false);
		return sdf.parse(aMilitaryTimeString);
    }

}

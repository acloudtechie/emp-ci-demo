/**
 * AeaLookupValueFinder
 *
 * This class makes heavy use of the private API to perform lookup value replacements
 * on TrackedDataObjects.
 *
 * alee 11/05/2014
 **/

package net.micropact.aea.auditLog.utility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import net.micropact.aea.core.dataTypePlugin.DataTypePluginClassUtility;
import net.micropact.aea.core.utility.StringUtils;
import net.micropact.aea.utility.Utility;
import net.micropact.aea.utility.lookup.AeaLookupExecutionContextImpl;
import net.micropact.aea.utility.lookup.LookupDataUtility;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataAccessException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.configuration.DataElement;
import com.entellitrak.configuration.DataType;
import com.entellitrak.legacy.util.DateUtility;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.lookup.For;

/**
 * Class to resolve lookup values based on an ID.
 *
 * @author MicroPact
 *
 */
public class AeaLookupValueFinder {

	private ExecutionContext etk = null;
	private static final int FIELD_NAME_POSITION = 3;

	/**
	 * Default constructor.
	 *
	 * @param etkTmp The Execution Context.
	 */
	public AeaLookupValueFinder(ExecutionContext etkTmp) {
		this.etk = etkTmp;
	}

	/**
	 * Fixes an issue with SQLServer not liking to wrap the output of selects with other selects.
	 * Adds a TOP 2147483647 parameter which fixes the issue.
	 *
	 * @param aString The query to wrap
	 * @return Query with all select replaced with select top 2147483647
	 */
    private String peformSelectReplacements(final String aString) {
        final boolean isSqlServer = Utility.isSqlServer(etk);

        if (isSqlServer) {
            if (aString == null) {
               return null;
            }

            final Pattern p = Pattern.compile("(select)(\\s)+(?!top)", Pattern.CASE_INSENSITIVE);
            final Matcher m = p.matcher(aString);

            return m.replaceAll("SELECT TOP 2147483647 ");
        } else {
            return aString;
        }
    }

    /**
     * Returns a data element's value.
     *
     * @param tde The data element to get the value for.
     * @param isLookupMap Map to indicate whether or not the data element is a lookup.
     * @param values Map containing all of the form's values, key is get element name.
     * @param parameterMap Map containing all the forms's parameters for inclusion in the query.
     * @return A data element's value.
     * @throws ApplicationException ETK fatal exception.
     */
	@SuppressWarnings("rawtypes")
	public String getValue(DataElement tde,
			               Map<String, String> isLookupMap,
			               Map<String, Object> values,
			               Map<String, Object> parameterMap) throws ApplicationException {


		Boolean isLookup = StringUtility.isNotBlank(isLookupMap.get(tde.getBusinessKey()));
		String fieldName = tde.getBusinessKey().split("\\.")[FIELD_NAME_POSITION];
		String value = "";

		if (isLookup) {

			final LookupDataUtility lsu = new LookupDataUtility(etk);
		    String lookupQuery = null;

			try {
				lookupQuery = lsu.getLookupQuery(tde.getBusinessKey(),
					          	new AeaLookupExecutionContextImpl(etk, For.VIEW, (Long) parameterMap.get("trackingId"),
					          			(Long) parameterMap.get("baseId"), (Long) parameterMap.get("parentId"),
					          			tde.getDataObject().getBusinessKey(), tde.getDataObject().getTableName()));
			} catch (DataAccessException e1) {
				throw new ApplicationException(e1);
			} catch (InstantiationException e1) {
				throw new ApplicationException(e1);
			} catch (IllegalAccessException e1) {
				throw new ApplicationException(e1);
			} catch (ClassNotFoundException e1) {
				throw new ApplicationException(e1);
			} catch (IncorrectResultSizeDataAccessException e1) {
				throw new ApplicationException(e1);
			}

			final StringBuffer modifiedQuery = new StringBuffer();

			final Pattern p = Pattern.compile("\\{\\?.*?\\}");
			final Matcher m = p.matcher(lookupQuery);

			//Find all replacement parameters and bind the replacementParams passed from the form
			//to the query.
			while(m.find()) {
				final String tmp = m.group();
				final String stripped = tmp.substring(2, tmp.length() - 1);
				m.appendReplacement(modifiedQuery, Matcher.quoteReplacement(":" + stripped));
			}

			m.appendTail(modifiedQuery);

			final String inClauseQuery = modifiedQuery.toString();

			//Wrap the query in an outer select, replace all wild card variables.
			final Map<String, Object> queryParamMap = new HashMap<>();
			final StringBuilder finalQuery = new StringBuilder();
			finalQuery.append("select ");
			finalQuery.append(" DISPLAY from (");
			finalQuery.append(inClauseQuery);
			finalQuery.append(") TEMP_QUERY WHERE ");

			if (tde.isMultiValued()) {
				Utility.addLargeInClause("VALUE", finalQuery, queryParamMap,
						(List) parameterMap.get(tde.getBusinessKey().split("\\.")[FIELD_NAME_POSITION]));
			} else {
				finalQuery.append("VALUE = :" + fieldName);
				queryParamMap.put(fieldName, parameterMap.get(fieldName));
			}

			List<String> valueList = new ArrayList<>();

			try {
				final List<Map<String, Object>> rowsToWrite =
						etk.createSQL (peformSelectReplacements(finalQuery.toString()))
						.setParameter(queryParamMap)
						.fetchList();

				for (Map<String, Object> aRow : rowsToWrite) {
					valueList.add(String.valueOf(aRow.get("DISPLAY")));
				}
			} catch (Exception e) {
				String errorMessage =
						"AEA Audit Log - Error executing query for resolving value for lookup on element \""
						+ tde.getBusinessKey()
						+ "\"\n\n. Query as executed = \"" + finalQuery.toString()
						+ "\"\n\n. Parameter Map = \"" + ReflectionToStringBuilder.toString(queryParamMap);

				throw new ApplicationException(errorMessage, e);
			}

			value = StringUtils.join(valueList, ", ");

		} else {

			Object tmpVal = values.get("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));

			if (tmpVal == null) {
				value = "";
			} else if (tde.getDataType() == DataType.CURRENCY) {
				value = ((BigDecimal) tmpVal).toString();
			} else if (tde.getDataType() == DataType.DATE) {
				value = DateUtility.getFormattedDate(((Date) tmpVal));
			} else if (tde.getDataType() == DataType.FILE) {
				try {
					value = etk.createSQL("SELECT FILE_NAME FROM etk_file WHERE id = :fileId")
    				            .setParameter("fileId", tmpVal)
					            .returnEmptyResultSetAs(tmpVal.toString())
					            .fetchString();
				} catch (Exception e) {
					Utility.aeaLog(etk, e);
					value = tmpVal + "";
				}
			} else if (tde.getDataType() == DataType.LONG_TEXT) {
				value = (String) tmpVal;
			} else if (tde.getDataType() == DataType.NUMBER) {
				value = tmpVal + "";
			} else if (tde.getDataType() == DataType.PASSWORD) {
				value = "*****";
			} else if (tde.getDataType() == DataType.TEXT) {
				value = (String) tmpVal;
			} else if (tde.getDataType() == DataType.TIMESTAMP) {
				value = DateUtility.getFormattedDateTime(((Date) tmpVal));
			} else if (tde.getDataType() == DataType.YES_NO) {
				value = (Boolean) tmpVal ? "Yes" : "No";
			} else if (tde.getDataType() == DataType.NONE){
				value = getDataTypePluginDisplay(etk, tde.getBusinessKey(), tmpVal);
				value = StringUtility.isBlank(value) ? "" : value;
			} else {
				value = tmpVal.toString();
			}
		}

		return value;
	}


	/**
	 * Returns the view string for a data element type plug-in.
	 *
	 * @param etkLocal The execution context.
	 * @param elementBusinessKey The plug-in element business key.
	 * @param value Value for a data-type plug-in.
	 * @return View SQL string for the data type plug-in.
	 *
	 * @throws ApplicationException ETK fatal Exception.
	 */
	@SuppressWarnings("unchecked")
	private static String getDataTypePluginDisplay(
			final ExecutionContext etkLocal, String elementBusinessKey, Object value) throws ApplicationException{
		return DataTypePluginClassUtility.instantiatePluginClass(etkLocal, elementBusinessKey).getViewString(value);
    }
}

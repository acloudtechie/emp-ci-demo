package net.micropact.aea.dbUtils.utility;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.For;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.core.utility.StringEscapeUtils;
import net.micropact.aea.utility.ImportExportUtility;
import net.micropact.aea.utility.LookupSourceType;
import net.micropact.aea.utility.SystemObjectType;
import net.micropact.aea.utility.lookup.AeaEtkLookupDefinition;
import net.micropact.aea.utility.lookup.AeaLookupExecutionContextImpl;

/**
 * Utility class containing helper methods to export RDO data.
 *
 * @author MicroPact
 *
 */
public final class RdoDataExportUtility {

	/**
	 * Private constructor.
	 */
	private RdoDataExportUtility() {
	}

	/**
	 * Maximum length that Oracle allows a varchar string to be.
	 */
	public static final int ORACLE_MAX_CHAR_STRING_LENGTH = 4000;

	/**
	 * Returns R_DATA_TABLE and R_DATA_VALUE_COLUMN values for an AeaEtkLookupDefinition object. Utilizes getLookupInfo
	 * method for script and SQL type lookups.
	 *
	 * First, determine whether the AeaEtkLookupDefinition is a script/sql or data object type.
	 *
	 * Then determine the table and value columns for the lookup and return them.
	 *
	 * @param fullyQualifiedScriptNames Map of fully qualified script names and their IDs
	 * @param localEtk The ExecutionContext.
	 * @param lookupDef The Lookup Definition to get a corresponding table / column pair.
	 * @return R_DATA_TABLE and R_DATA_VALUE_COLUMN values for an AeaEtkLookupDefinition object. Utilizes getLookupInfo
	 * method for script and SQL type lookups.
	 *
	 * @throws ApplicationException If a problem was encountered
	 * @throws InstantiationException If there was an underlying {@link InstantiationException}
	 * @throws IllegalAccessException If there was an underlying {@link IllegalAccessException}
	 * @throws ClassNotFoundException If there was an underlying {@link ClassNotFoundException}
	 */
	public static Map<String, String> getRDataTableAndValueColumn(final Map <Long, String> fullyQualifiedScriptNames,
			                                                       final ExecutionContext localEtk,
			                                                       final AeaEtkLookupDefinition lookupDef)
			                                                    		   throws ApplicationException,
			                                                    		   InstantiationException,
			                                                    		   IllegalAccessException,
			                                                    		   ClassNotFoundException {
		final Map <String, String> rDataTableAndValueColumn = new HashMap<String, String>();

		if (lookupDef.getLookupType() == LookupSourceType.DATA_OBJECT_LOOKUP) {

			rDataTableAndValueColumn.put("R_DATA_TABLE", lookupDef.getEtkDataObject().getTableName());
			rDataTableAndValueColumn.put("R_DATA_VALUE_COLUMN",
					lookupDef.getValueElement() == null ?
					"ID" : lookupDef.getValueElement().getColumnName());

		} else if (lookupDef.getLookupType() == LookupSourceType.QUERY_LOOKUP) {

			final String lookupSQL = lookupDef.getLookupSql();
			final String[] lookupInfo = getLookupTableValueColumn(lookupSQL, localEtk);
			rDataTableAndValueColumn.put("R_DATA_TABLE", lookupInfo[0]);
			rDataTableAndValueColumn.put("R_DATA_VALUE_COLUMN", lookupInfo[1]);

		} else if (lookupDef.getLookupType() == LookupSourceType.SYSTEM_OBJECT_LOOKUP) {
			if (lookupDef.getSystemObjectType() == SystemObjectType.USER) {
				rDataTableAndValueColumn.put("R_DATA_TABLE", "ETK_USER");
				rDataTableAndValueColumn.put("R_DATA_VALUE_COLUMN", "USER_ID");
			}
		} else if (lookupDef.getLookupType() == LookupSourceType.SCRIPT_LOOKUP) {

			String fullyQualifiedScriptName = fullyQualifiedScriptNames.get(lookupDef.getSqlScriptObjectId());

		    if (fullyQualifiedScriptName == null) {
		    	throw new ApplicationException ("Could not find script with ID " +
		                                        lookupDef.getSqlScriptObjectId());
		    }


		    //Create a new lookup handler and retrieve the forView SQL from the query.
			final LookupHandler luHandler = (LookupHandler)
					Class.forName(fullyQualifiedScriptName).newInstance();
			final String lookupSQL =
					luHandler.execute(
					           new AeaLookupExecutionContextImpl(localEtk, For.VIEW, 0L, 0L, 0L, "", ""));

			if (luHandler instanceof IAeaLookupHandler) {
				final String tableName = ((IAeaLookupHandler) luHandler).getValueTableName(localEtk);
				final String columnName = ((IAeaLookupHandler) luHandler).getValueColumnName(localEtk);

				rDataTableAndValueColumn.put("R_DATA_TABLE", tableName);
				rDataTableAndValueColumn.put("R_DATA_VALUE_COLUMN", columnName);
			} else {
				final String[] lookupInfo = getLookupTableValueColumn(lookupSQL, localEtk);

				rDataTableAndValueColumn.put("R_DATA_TABLE", lookupInfo[0]);
				rDataTableAndValueColumn.put("R_DATA_VALUE_COLUMN", lookupInfo[1]);
			}
		}

		return rDataTableAndValueColumn;
	}

	/**
	 * Method to extract the table name and value column id from a custom SQL query.
	 *
	 * @param aLookup Freeform SQL query.
	 * @param localEtk The ExecutionContext
	 * @return [0] = TABLE_NAME, [1] = VALUE COLUMN
	 */
	public static String[] getLookupTableValueColumn(final String aLookup, final ExecutionContext localEtk) {

		String lookup = aLookup;
		final int returnValArraySize = 3;
		final String[] returnVals = new String[returnValArraySize];

		//Put the query all on one line and remove duplicate spaces.
		if (lookup != null && lookup.length() > 0) {
			lookup = lookup
					.toLowerCase()
					.trim()
					.replaceAll("\t", " ")
					.replaceAll("\n", " ")
					.replaceAll(" +", " ");
		}

		//Detect if there is a comment in the lookup, if so skip past it.
		int beginCommentIndex = lookup.indexOf("/*");
		int endCommentIndex;

		while (beginCommentIndex != -1) {
			endCommentIndex = lookup.indexOf("*/");
			lookup = lookup.substring(0, beginCommentIndex) + lookup.substring(endCommentIndex + 1, lookup.length());
			beginCommentIndex = lookup.indexOf("/*");
		}

		//If this is a SQL (not script) type query, we want to find the forView part for easiest parsing.
		final int forViewIndex = lookup.indexOf("forview");
		final int elseIndex = lookup.indexOf("} else {");


		if (forViewIndex != -1) {
			lookup = lookup.substring(forViewIndex, lookup.length());
		} else if (elseIndex != -1) {
			lookup = lookup.substring(elseIndex, lookup.length());
		}

		int valueIndexEnd = -1;

		//Find the name of the variable preceding the VALUE alias.
		if (lookup.indexOf(" as \"value\"") != -1) {
			valueIndexEnd = lookup.indexOf(" as \"value\"");
		} else if (lookup.indexOf(" as value") != -1) {
			valueIndexEnd = lookup.indexOf(" as value");
		} else if (lookup.indexOf(" \"value\",") != -1) {
			valueIndexEnd = lookup.indexOf(" \"value\",");
		} else if (lookup.indexOf(" value,") != -1) {
			valueIndexEnd = lookup.indexOf(" value,");
		}

		if (valueIndexEnd == -1) {
			return returnVals;
		}

		int valueIndexBegin = 0;
		boolean nonSpaceEncountered = false;
		for (int i = valueIndexEnd - 1; i >= 0; i--) {
			if (lookup.charAt(i) == ' ' || lookup.charAt(i) == '\t') {
				valueIndexBegin = i + 1;

				if (nonSpaceEncountered) {
					break;
				}
			} else {
				nonSpaceEncountered = true;
			}
		}

		String valueVar = lookup.substring(valueIndexBegin, valueIndexEnd);

		final int indexOfPeriod = valueVar.indexOf('.');
		String tableName = "";

		int tableIndexBegin = 0;
		int tableIndexEnd = 0;
		String tableQualifier = "";

		//If the table of the value column has an alias (ex... T_TABLE theTable) attempt to find
		//the name of the table preceding the alias. Otherwise, look for the first table after the from
		//keyword.
		if (indexOfPeriod != -1) {

			for (int i = indexOfPeriod - 1; i >= 0; i--) {
				if (valueVar.charAt(i) == '"' ||
						valueVar.charAt(i) == '\n' ||
						valueVar.charAt(i) == ' ' ||
						valueVar.charAt(i) == '\t') {

					break;
				}

				tableQualifier = valueVar.charAt(i) + tableQualifier;
			}

			valueVar = valueVar.substring(indexOfPeriod + 1, valueVar.length());
			tableIndexEnd = lookup.indexOf(" " + tableQualifier + " ");

			if (tableIndexEnd <= -1) {
				if (lookup.endsWith(" " + tableQualifier)) {
					tableIndexEnd = lookup.length() - 2;
				}
			}

			if (tableIndexEnd <= -1) {
				tableIndexEnd = lookup.indexOf(" " + tableQualifier + "\n");
			}

			if (tableIndexEnd <= -1) {
				tableIndexEnd = lookup.indexOf(" " + tableQualifier + "\"");
			}

			for (int i = tableIndexEnd - 1; i >= 0; i--) {
				if (lookup.charAt(i) == ' ') {
					tableIndexBegin = i;
					break;
				}
			}

		} else {

			tableIndexBegin = lookup.indexOf("from ");

			if (tableIndexBegin != -1) {

				final int fromKeywordOffset = 5;
				tableIndexBegin += fromKeywordOffset; // Set index to the end

				for (int i = tableIndexBegin; i < lookup.length(); i++) {

					tableIndexEnd = i + 1;

					if (lookup.charAt(i) == '"' ||
						lookup.charAt(i) == '\t' ||
						lookup.charAt(i) == '\n' ||
						lookup.charAt(i) == ' ') {

						break;
					}
				}
			}
		}

		if ((tableIndexBegin != -1) && (tableIndexEnd != -1) && (tableIndexBegin < tableIndexEnd)) {
			tableName = lookup.substring(tableIndexBegin, tableIndexEnd);
		} else {
			tableName = "ERROR_PARSING_QUERY_SEE_LOG";
			localEtk.getLogger().error("RDO Exporter - Could not parse lookup query \""
			                       + lookup
			                       + "\", please implement IAeaLookupHandler for this lookup "
			                       + "and/or convert to a Script type lookup from SQL");
		}

		returnVals[0] = (tableName != null) ? tableName.trim().toUpperCase() : "";
		returnVals[1] = (valueVar != null) ? valueVar.trim().toUpperCase() : "";

		return returnVals;
	}

	/**
	 * Helper method to export Clob data as a String.
	 *
	 * @param value Clob data value.
	 * @param oracleSyntax Whether to return as Oracle or SqlServer format.
	 * @return Clob data as a String.
	 */
	public static String convertClobToSQLInsert (final Object value, final boolean oracleSyntax) {
		final String rawString = (String) value;
		final String escapedString = StringEscapeUtils.escapeSql(rawString);

		StringBuilder quotedClobTextString = new StringBuilder();

        if (!oracleSyntax || (escapedString.length() <= ORACLE_MAX_CHAR_STRING_LENGTH)) {
        	quotedClobTextString.append("'");
        	quotedClobTextString.append(escapedString);
            quotedClobTextString.append("'");
        } else {
           final int rawStringLength = rawString.length();
           final int maxLengthEscaped = ORACLE_MAX_CHAR_STRING_LENGTH / 2;
           int i = maxLengthEscaped;

           quotedClobTextString.append("to_clob('");
           quotedClobTextString.append(StringEscapeUtils.escapeSql(rawString.substring(0, i)));
           quotedClobTextString.append("') ");

           while (i <= rawStringLength) {

             if ( (i + maxLengthEscaped) >= rawStringLength) {
            	quotedClobTextString.append(" || to_clob('");
            	quotedClobTextString.append(StringEscapeUtils.escapeSql(rawString.substring(i, rawStringLength)));
            	quotedClobTextString.append("') ");

                i += maxLengthEscaped;
             } else {
            	quotedClobTextString.append(" || to_clob('");
            	quotedClobTextString.append(StringEscapeUtils.escapeSql(rawString.substring(i, i + maxLengthEscaped)));
            	quotedClobTextString.append("') ");

                i += maxLengthEscaped;
             }
           }
        }

        return quotedClobTextString.toString();
	}

    /**
     * Returns a Map of FULLY_QUALIFIED_SCRIPT_NAME, SCRIPT_ID from AEA_SCRIPT_PKG_VIEW.
     *
     * @param etk The execution context.
     * @return A Map of FULLY_QUALIFIED_SCRIPT_NAME, SCRIPT_ID from AEA_SCRIPT_PKG_VIEW.
     */
    public static Map <Long, String> getFullyQualifiedScriptNames (final ExecutionContext etk) {
        final Map <Long, String> fullyQualifiedScriptNames = new HashMap<Long, String>();

		final List<Map<String, Object>> fullyQualifiedScriptNameList =
				etk.createSQL(
				"select FULLY_QUALIFIED_SCRIPT_NAME, SCRIPT_ID from AEA_SCRIPT_PKG_VIEW")
				.returnEmptyResultSetAs(null)
				.fetchList();

		for (Map<String, Object> aScript : fullyQualifiedScriptNameList) {

			final Long longVal = ((Number) aScript.get("SCRIPT_ID")).longValue();

			fullyQualifiedScriptNames.put(longVal,
					                      (String) aScript.get("FULLY_QUALIFIED_SCRIPT_NAME"));
		}

		return fullyQualifiedScriptNames;
    }

	/**
	 * Escape a value to a format that is acceptable for an SQL string.
	 *
	 * @param aValue The value to escape.
	 * @return SQL escaped value.
	 */
	public static String valToSqlString (final Object aValue) {
		if (aValue == null) {
			return "null";
		} else if (aValue instanceof BigDecimal) {
			return ((BigDecimal) aValue).toPlainString();
		} else if (aValue instanceof Date) {
			return "to_date('"
					+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(aValue)
					+ "','RRRR-MM-DD HH24:MI:SS')";
		} else if (aValue instanceof Integer) {
			return ((Integer) aValue).intValue() + "";
		} else if (aValue instanceof String) {
			return "'" + StringEscapeUtils.escapeSql((String) aValue) + "'";
		} else {
			return "null";
		}
	}

	/**
	 * Converts an ETK_FILE record to XML.
	 *
	 * @param allFiles Files to export as XML.
	 * @return ETK_FILE records as XML data.
	 *
	 * @throws ApplicationException Unexpected ApplicationException.
	 */
	public static byte[] getXmlForFile (List<Map<String, Object>> allFiles) throws ApplicationException {
		   try {
			   final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		       final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		       final Document document = documentBuilder.newDocument();

		       final Element root = document.createElement("objects");

		       document.appendChild(root);

		       /*T_PDF_TEMPLATE_FILE*/
		       ImportExportUtility.addListToXml(document,
		                root,
		                "ETK_FILE",
		                allFiles);

		       return ImportExportUtility.getStringFromDoc(document).getBytes();
		   } catch (Exception e) {
			   throw new ApplicationException ("Error converting ETK_FILE records to XML.", e);
		   }
	}
}

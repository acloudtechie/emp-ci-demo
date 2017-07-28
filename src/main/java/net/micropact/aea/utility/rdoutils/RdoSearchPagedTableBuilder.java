/**
 *
 * PagedTableBuilder
 *
 * alee 08/18/2014
 **/

package net.micropact.aea.utility.rdoutils;


import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.entellitrak.DataAccessException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.SQLFacade;
import com.entellitrak.legacy.util.DateUtility;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.localization.Localizations;
import com.entellitrak.lookup.For;
import com.entellitrak.lookup.LookupResult;

import net.micropact.aea.core.utility.StringEscapeUtils;
import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.Utility;

/**
 * This class contains methods to build a paged HTML table given an fetchList result set of rows.
 *
 * @author aclee
 *
 */
public class RdoSearchPagedTableBuilder {

    List<Map<String, Object>> resultSet = null;
    List<RdoDataElement> rowAttributeList = null;
    List<String> cachedKeys = new ArrayList<String>();

    private StringBuilder tableBuilder = null;
    private String tableName = null;
    private String dataObjectKey = null;
    private Random randomGenerator = null;
    private final ExecutionContext etk;
    private final Map<String, List<String>> pageParameters;
    private final boolean militaryTimeMode;
    final boolean linkToRdo;

    public RdoSearchPagedTableBuilder(final ExecutionContext theEtk, final Map<String, List<String>> thePageParameters, final boolean isMilitaryTime, final boolean isLinkToRdo) {
        tableBuilder = new StringBuilder();
        randomGenerator = new Random();
        this.etk = theEtk;
        this.pageParameters = thePageParameters;
        this.militaryTimeMode = isMilitaryTime;
        this.linkToRdo = isLinkToRdo;
    }

    /**
     * Sets the attributes used to build the header and determine data types of rows.
     *
     * @param aRowAttributeList A list of FormDataElements that contain metadata about elements needed for header.
     */
    public void setRowAttributeList (final List<RdoDataElement> aRowAttributeList) {
        this.rowAttributeList = aRowAttributeList;
   }

    /**
     * The fetchList() resultset containing the row data.
     *
     * @param aResultSet results of a raw {@link SQLFacade#fetchList()} call
     */
    public void setResultSet (final List<Map<String, Object>> aResultSet) {
        this.resultSet = aResultSet;
    }

    /**
     * Returns an HTML data table built from the given resultSet and rowAttributeList.
     *
     * @return Table with random ID.
     */
    public String buildTable () {
        return buildTable(null, UUID.randomUUID() + "");
    }

    /**
     * Returns an HTML data table built from the given resultSet and rowAttributeList.
     *
     * @param theDataObjectKey the business key of the data object being displayed.
     * @param aTableName a unique id to give the table being generated
     *
     * @return Table with a given ID
     */
    public String buildTable (final String theDataObjectKey, final String aTableName) {
        dataObjectKey = theDataObjectKey;
        tableName = getEscVal(aTableName, DataElementType.TEXT);

        if ("".equals(tableName)) {
            tableName = randomGenerator.nextInt(100000) + "";
        }

        String className = "grid";

        if(linkToRdo){
            className += " linked";
        }

        //Create outer table.
        tableBuilder.append("<table id=\"");
        tableBuilder.append(StringEscapeUtils.escapeHtml(tableName));
        tableBuilder.append("\" class=\""+className+"\">");

        buildHeader();
        buildBody();

        tableBuilder.append("</table>");

        //Clean up cache of forView LookupResults.
        for (final String cachedQuery : cachedKeys) {
        	etk.getCache().remove(cachedQuery);
        }

        return tableBuilder.toString();
     }

    private String getEscVal(final Object aValue, DataElementType dataElementType) {

        if (aValue == null) {
           return "";
        } else if (dataElementType == DataElementType.TEXT ||
        		   dataElementType == DataElementType.STATE ||
        		   dataElementType == DataElementType.LONG_TEXT) {

           if (StringUtility.isBlank((String) aValue)) {
              return "";
           } else {
              return StringEscapeUtils.escapeHtml((String) aValue);
           }
        } else if (dataElementType == DataElementType.FILE) {
           try {
			return etk.createSQL("select FILE_NAME from ETK_FILE where ID = :fileId")
					   .setParameter("fileId", aValue)
					   .fetchString();
			} catch (Exception e) {
				return aValue + "";
			}
        } else if (dataElementType == DataElementType.DATE) {
           if (militaryTimeMode) {
        	   return toMilitaryDate((Date) aValue);
           } else {
        	   return DateUtility.getFormattedDate((Date) aValue);
           }
        } else if (dataElementType == DataElementType.TIMESTAMP) {
        	if (etk.getCurrentUser().getTimeZonePreference() != null) {
        		if (militaryTimeMode) {
        			return toMilitaryTime(Localizations.toLocalTimestamp(etk.getCurrentUser().getTimeZonePreference(), (Date) aValue).getDateValue());
        		} else {
        			return Localizations.toLocalTimestamp(etk.getCurrentUser().getTimeZonePreference(), (Date) aValue).getTimestampString();
        		}
        	} else {
        		if (militaryTimeMode) {
        			return toMilitaryTime((Date) aValue);
        		} else {
        			return DateUtility.getFormattedDateTime((Date) aValue);
        		}
        	}
         } else if (dataElementType == DataElementType.CURRENCY) {
        	 return NumberFormat.getCurrencyInstance().format(aValue);
         } else if ((dataElementType == DataElementType.NUMBER) ||
        		    (dataElementType == DataElementType.LONG)) {
        	 return ((Number) aValue).longValue() + "";
         } else if (dataElementType == DataElementType.YES_NO) {
        	 int intVal = ((Number) aValue).intValue();

        	 if (intVal == 1) {
            	return "Yes";
             } else if (intVal == 0){
            	return "No";
             } else {
            	return "";
             }
         } else if (dataElementType == DataElementType.PASSWORD) {
        	 return "*****";
         } else {
        	 return aValue + "";
         }
    }

    private void buildBody () {
       tableBuilder.append("<tbody>");

       Integer i = 1;
       final String randomTable = String.format("%09d", randomGenerator.nextInt(100000000));

       for (final Map<String, Object> aRow : resultSet) {

          tableBuilder.append("<tr name=\"dynamically_built_HRD_FW_row\" id=\"row_");
          tableBuilder.append(randomTable);
          tableBuilder.append("_");
          tableBuilder.append(String.format("%09d", i++));
          tableBuilder.append("_");
          tableBuilder.append(getEscVal(aRow.get("DATA_OBJECT_KEY"), DataElementType.TEXT));
          tableBuilder.append("_::-_-::_");
          tableBuilder.append(getEscVal(aRow.get("TRACKING_ID"), DataElementType.NUMBER));
          tableBuilder.append("\"");

          if(linkToRdo){
              final String url = "admin.refdata.update.request.do?dataObjectKey="+dataObjectKey+"&trackingId="+aRow.get("ID");
              tableBuilder.append(" onclick=\"window.open('"+url+"', '_blank')\"");
          }

          tableBuilder.append(">");

          for (final RdoDataElement aRowAttribute : rowAttributeList) {
             tableBuilder.append("<td>");
             tableBuilder.append(getColumnValue(aRow, aRowAttribute));
             tableBuilder.append("</td>");
          }

          tableBuilder.append("</tr>");
       }

       tableBuilder.append("</tbody>");
    }

    /**
     * Method to build the table's header.
     */
    private void buildHeader() {
       String ascDesc = "asc";
       String sortColumn = "";
       String sortOrder = "";

       //Determines if the table is currently sorted by a perticular column.
       if (pageParameters.containsKey("sortColumn") && pageParameters.containsKey("sortOrder")) {
	       sortColumn = pageParameters.get("sortColumn").get(0);
	       sortOrder = pageParameters.get("sortOrder").get(0);
       }

       tableBuilder.append("<thead><tr>");

       String columnName;
       String columnLabel;
       DataElementType columnDataElementType;

       //Loop through each of the column attributes.
       for (final RdoDataElement aColumn : rowAttributeList) {

    	  columnName =  getEscVal(aColumn.getColumnName(), DataElementType.TEXT);
    	  columnLabel = getEscVal(aColumn.getLabel(), DataElementType.TEXT);
    	  columnDataElementType = aColumn.getDataType();

    	  //Toggle the sort of the column.
    	  if (sortColumn.equals(columnName)) {
    		  if (sortOrder.equals("asc")) {
    			  ascDesc = "desc";
    		  }
    	  }

          tableBuilder.append("<th scope=\"col\">");

          //Core does not allow sort on long text fields, so I don't either.
          if (!(DataElementType.LONG_TEXT == columnDataElementType)) {

        	  //If this column is sorted, append an outer table to put a sort indicator image into.
	          if (sortColumn.equals(columnName)) {
	        	  tableBuilder.append("<table class=\"sortDirection\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">");
	        	  tableBuilder.append("<tbody class=\"sortDirection\">");
	        	  tableBuilder.append("<tr>");
	        	  tableBuilder.append("<td>");
	          }


	          tableBuilder.append("<a href=\"#\" onclick=\"searchPageNavigate(1, '");
	          tableBuilder.append(columnName);
	          tableBuilder.append("', '");
	          tableBuilder.append(ascDesc);
	          tableBuilder.append("');\">");
	          tableBuilder.append(columnLabel);
	          tableBuilder.append("</a>");

	          //If sorted, put an up or down arrow to the right of the column header link.
	          if (sortColumn.equals(columnName)) {
	        	  tableBuilder.append("</td><td>");

	        	  if (sortOrder.equals("asc")) {
	        		  tableBuilder.append("<img border=\"0\" alt=\"Sort Ascending Image\" src=\"");
	        		  tableBuilder.append(Utility.getWebPubPath(etk));
	        		  tableBuilder.append("/component/grid/images/arrow_down.gif\">");
	        	  } else {
	        		  tableBuilder.append("<img border=\"0\" alt=\"Sort Decending Image\" src=\"");
	        		  tableBuilder.append(Utility.getWebPubPath(etk));
	        		  tableBuilder.append("/component/grid/images/arrow_up.gif\">");
	        	  }

	        	  tableBuilder.append("</td></tr></tbody></table>");
	          }
          } else {
        	  //Do not allow users to sort by long text fields.
        	  tableBuilder.append(columnLabel);
          }

          tableBuilder.append("</th>");
       }

       tableBuilder.append("</tr></thead>");
    }

    /**
     * Helper method to determine a row's value.
     *
     * @param aRow The row to get the value for.
     * @param aRowAttribute The attributes of that row's data.
     * @return The display value of the column to display in the search result table
     */
    private String getColumnValue (final Map<String, Object> aRow, final RdoDataElement aRowAttribute) {
        String lookupBusinessKey;

    	if (StringUtility.isBlank(lookupBusinessKey = aRowAttribute.getLookupBusinessKey())) {
    		//If the row's data is not attached to a lookup, just return the value of the row.
    		return getEscVal(aRow.get(aRowAttribute.getColumnName()), aRowAttribute.getDataType());
    	} else {
    		//Otherwise, lookup the forView lookup results for the given row.
    		final List<LookupResult> results = getLookupResults(lookupBusinessKey);

    		String mTableName;
    		Object key;


    		if (StringUtility.isBlank(mTableName = aRowAttribute.getMTableName())) {
    			//For non M-Table values, we will return the first display value from the lookup
    			//result with a matching value key.
    			key = aRow.get(aRowAttribute.getColumnName());

    			if (key != null) {
	    			for (final LookupResult aResult : results) {
	    				if (aResult.getValue().equals(key.toString())) {
	    					return getEscVal(aResult.getDisplay(), DataElementType.TEXT);
	    				}
	    			}
    			}
    		} else {
    			//Form M-Table values, we need to first lookup all the possible key values
    			//based on the ID Owner, then we can match each of those values to a lookup
    			//result and return the display value of all combined M-Table values.

    			key = aRow.get("ID");
    			List<Map<String, Object>> lookupValues = new ArrayList<Map<String, Object>>();

    			//Get full key value list from M-Table
    			if (key != null) {
	    			try {
						lookupValues =
								etk.createSQL(" select " + aRowAttribute.getColumnName() +
										      " as VALUE from " + mTableName +
										      " where id_owner = :idOwner ")
							    .setParameter("idOwner", key)
							    .returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
							    .fetchList();
					} catch (final DataAccessException e) {
						Utility.aeaLog(etk, "MTABLE Lookup value retrieval error.", e);
					}
    			}

    			if (key != null && (lookupValues.size() > 0)) {
    				final StringBuilder outBuilder = new StringBuilder();

    				//Store value keys in a temp map for O(1) retrieval
    				final Map<String, Object> validKeys = new HashMap<String, Object>();
    				for (final Map<String, Object> aValue : lookupValues) {
    					validKeys.put(aValue.get("VALUE") + "", null);
    				}

    				//Loop through LookupResult list and append all selected values to the lookup list.
	    			for (final LookupResult aResult : results) {
	    				if (validKeys.containsKey(aResult.getValue())) {
	    					outBuilder.append(getEscVal(aResult.getDisplay(), DataElementType.TEXT));
	    					outBuilder.append("<br>");
	    				}
	    			}

	    			return outBuilder.toString();
    			}
    		}
    	}

    	return "";
    }

    /**
     * Temporary cache for forView LookupResult lists. This prevent's the system from re-loading
     * all R-data values for every single row in the table. The cached keys are stored in the
     * cachedKeys map and then cleared when buildTable is called.
     *
     * @param lookupBusinessKey The business key of the lookup whose data will be fetched
     * @return The lookup results
     */
    @SuppressWarnings("unchecked")
	private List<LookupResult> getLookupResults (final String lookupBusinessKey) {
    	List<LookupResult> resultList;

    	if ((resultList = (List<LookupResult>) etk.getCache().load("AEA_LOOKUP_CACHE_" + lookupBusinessKey)) == null) {
    		resultList = etk.getLookupService().getLookup(lookupBusinessKey).execute(For.VIEW);
    		etk.getCache().store("AEA_LOOKUP_CACHE_" + lookupBusinessKey, resultList);
    		cachedKeys.add("AEA_LOOKUP_CACHE_" + lookupBusinessKey);
    	}

    	return resultList;
    }

    private String toMilitaryDate (Date aMilitaryDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setLenient(false);
		return sdf.format(aMilitaryDate);
    }

    private String toMilitaryTime (Date aMilitaryTimeDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmm");
		sdf.setLenient(false);
		return sdf.format(aMilitaryTimeDate);
    }
}
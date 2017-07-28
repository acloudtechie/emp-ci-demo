/**
 *
 * Controller code for the RDO Data export page.
 *
 * alee 12/09/2014
 **/

package net.micropact.aea.dbUtils.page.rdodataexport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.FileResponse;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;
import com.entellitrak.platform.DatabasePlatform;

import net.micropact.aea.core.ioUtility.Base64;
import net.micropact.aea.core.ioUtility.IOUtility;
import net.micropact.aea.core.utility.StringEscapeUtils;
import net.micropact.aea.dbUtils.service.RdoExportLogic;
import net.micropact.aea.utility.DataObjectType;
import net.micropact.aea.utility.Utility;
import net.micropact.aea.utility.lookup.AeaEtkDataObject;
import net.micropact.aea.utility.lookup.LookupDataUtility;


/**
 * Page to export RDO data from an Oracle DB.
 *
 * @author aclee
 *
 */
public class RdoDataExportController implements PageController {

	/**
	 * Database platform.
	 */
    private DatabasePlatform databasePlatform = DatabasePlatform.ORACLE;

    /**
     * Whether or not to write debug output to log.
     */
    private boolean writeDebug = false;

    /**
     * Export for Sqldeveloper (true) or to execute as a stored procedure within entellitrak (false).
     */
    private boolean exportForStandalone = false;

    private PageExecutionContext etk;

    @SuppressWarnings("unchecked")
    @Override
    /**
     * Main execution method. On first run show tables user can export. On submit generate
     * RDO insert data.
     *
     * @param etk The Page Execution Context.
     * @throws ApplicationException Unexpected application exception.
     *
     * @return Response for the controller code.
     */
    public Response execute(final PageExecutionContext localEtk) throws ApplicationException {

        this.etk = localEtk;

        Integer rdoExportMaxLines = 0;

        try {
            writeDebug =
                    localEtk.createSQL("select count(*) from T_AEA_CORE_CONFIGURATION "
                            + "where c_code = 'dbutils.rdoExport.debugLogging' and c_value = 'true'")
                            .fetchInt() > 0 ? true : false;
        } catch (Exception e) {
            //Not a critical error, log nothing, we will just keep logging disabled.
            writeDebug = false;
        }

        try {
        	rdoExportMaxLines =
                    localEtk.createSQL("select c_value from T_AEA_CORE_CONFIGURATION "
                            + "where c_code = 'dbutils.rdoExport.rdoExportMaxLines'")
                            .fetchInt();

        	if (rdoExportMaxLines < 0) {
        		rdoExportMaxLines = 0;
        	}
        } catch (Exception e) {
        	//Not a critical error, log nothing, we will just not paginate exported data.
        	rdoExportMaxLines = 0;
        }

        if (StringUtility.isBlank(localEtk.getParameters().getSingle("exportRdoData"))) {

            String exportOrderConfig = "";
            String exportTableConfig = "";

            //Load Base64 string from T_AEA_CORE_CONFIGURATION containing saved export order values.
            try {
                exportOrderConfig =
                        localEtk.createSQL("select c_description from T_AEA_CORE_CONFIGURATION "
                                + "where c_code = 'dbutils.rdoExportOrder'")
                                .returnEmptyResultSetAs("")
                                .fetchString();
            } catch (final Exception e) {
                throw new ApplicationException("Could not read C_DESCRIPTION from T_AEA_CORE_CONFIGURATION "
                        + "WHERE CODE = 'dbutils.rdoExportOrder'", e);
            }

            try {
                exportTableConfig =
                        localEtk.createSQL("select c_description from T_AEA_CORE_CONFIGURATION "
                                + "where c_code = 'dbutils.rdoExportSelectedTables'")
                                .returnEmptyResultSetAs("")
                                .fetchString();
            } catch (final Exception e) {
                throw new ApplicationException("Could not read C_DESCRIPTION from T_AEA_CORE_CONFIGURATION "
                        + "WHERE CODE = 'dbutils.rdoExportSelectedTables'", e);
            }

            final HashMap<String, String> tableOrderingData = new HashMap<String, String>();

            //If the export order configuration is not blank, convert it from a Base64 string into an ArrayList
            //of RdoTable records, loop through them and put the ordering information into the tableOrderingData
            //hashmap.
            if (StringUtility.isNotBlank(exportOrderConfig)) {
            	ObjectInputStream ois = null;
                try {
                    ois
                    = new ObjectInputStream(
                            new ByteArrayInputStream(Base64.decodeBase64(exportOrderConfig.getBytes())));

                    final ArrayList<RdoTable> tableOrderRecords = (ArrayList<RdoTable>) ois.readObject();

                    for (final RdoTable aTable : tableOrderRecords) {
                        tableOrderingData.put(aTable.getTableName(),
                                aTable.getSortOrder() == Integer.MAX_VALUE ?
                                                                            "" : aTable.getSortOrder() + "");
                    }
                } catch(final Exception e) {
                    if (writeDebug) {
                        Utility.aeaLog(localEtk, "Error decoding table ordering data from "
                                + "dbutils.rdoExportOrder record in T_AEA_CORE_CONFIGURATION", e);
                    }
                } finally {
                	IOUtility.closeQuietly(ois);
                }
            }

            HashMap<String, String> selectedRdos = null;

            //If the checked RDO configuration is not blank, convert it from a Base64 string into an ArrayList
            //of RdoTable records, load data on which RDO objects are pre-checked for export.
            if (StringUtility.isNotBlank(exportTableConfig)) {
            	ObjectInputStream ois = null;
                try {
                    ois = new ObjectInputStream(
                            new ByteArrayInputStream(Base64.decodeBase64(exportTableConfig.getBytes())));

                    selectedRdos = (HashMap<String, String>) ois.readObject();
                } catch(final Exception e) {
                    if (writeDebug) {
                        Utility.aeaLog(localEtk, "Error decoding table ordering data from "
                                + "dbutils.rdoExportSelectedTables record in T_AEA_CORE_CONFIGURATION", e);
                    }
                } finally {
                	IOUtility.closeQuietly(ois);
                }
            }

            if (selectedRdos == null) {
                selectedRdos = new HashMap<String, String>();
            }

            final TextResponse tr = localEtk.createTextResponse();
            tr.setContentType(ContentType.HTML);
            final List<AeaEtkDataObject> allEtkDataObjects = LookupDataUtility.getAllEtkDataObjects(localEtk);

            //Build numeric ordering input fields and checkboxes for each of the tables, put data into the
            //context.
            tr.put("options", getRdoTableMSOptionList(allEtkDataObjects, tableOrderingData, selectedRdos));
            return tr;
        } else {

            if ("Oracle".equals(localEtk.getParameters().getSingle("dbExportType"))) {
            	databasePlatform = DatabasePlatform.ORACLE;
            } else {
            	databasePlatform = DatabasePlatform.SQL_SERVER;
            }

            if (StringUtility.isNotBlank(localEtk.getParameters().getSingle("exportForStandalone"))) {
            	exportForStandalone = true;
            }

            final List<AeaEtkDataObject> allEtkDataObjects = LookupDataUtility.getAllEtkDataObjects(localEtk);
            ArrayList<RdoTable> orderedTables = getOrderedTables(allEtkDataObjects);

            try {
				saveSelectedTables(orderedTables);
				saveTableOrdering(orderedTables);
			} catch (IOException | IncorrectResultSizeDataAccessException e1) {
				throw new ApplicationException (e1);
			}


            HashMap<String, Integer> selectedTableList = new HashMap<String, Integer>();
            final List<String> userSelectedRdoTablesToExport
  						= localEtk.getParameters().getField("rdo_table_select_checkbox");


            if (userSelectedRdoTablesToExport != null) {
	            for (String aTable : userSelectedRdoTablesToExport) {
	            	if (StringUtility.isNotBlank(localEtk.getParameters().getSingle(aTable + "_ORDER"))) {
	            		try {
	            			selectedTableList.put(aTable,
	            			             new Integer(localEtk.getParameters().getSingle(aTable + "_ORDER")).intValue());
	            		} catch (Exception e) {
	            			selectedTableList.put(aTable, Integer.MAX_VALUE);
	            		}
	            	} else {
	            		selectedTableList.put(aTable, Integer.MAX_VALUE);
	            	}
	            }
            }

            RdoExportLogic rel = new RdoExportLogic(localEtk, databasePlatform, writeDebug, exportForStandalone);
            FileResponse fr = null;

            if (StringUtility.isBlank(localEtk.getParameters().getSingle("exportFilesAsZIP"))) {
            	final String result = rel.exportToSqlOneStage(selectedTableList);

                fr = localEtk.createFileResponse("rdo_export_" + Calendar.getInstance().getTimeInMillis() + ".sql"
                        , result.getBytes());
                fr.setContentType("application/x-sql");
                fr.setContentLength(result.getBytes().length);
            } else {
                try {
                	final InputStream inputStream = rel.exportToZipTwoStage(selectedTableList, rdoExportMaxLines);
                	fr =
                	    localEtk.createFileResponse("rdo_export_" + Calendar.getInstance().getTimeInMillis() + ".zip",
                	      inputStream);
                    fr.setContentType("application/zip");
                } catch (IOException e) {
                    throw new ApplicationException ("Error creating ZIP for RDO Data Export, please review error logs.");
                }
            }

            return fr;
        }
    }

    /**
     * Returns sorted list of RdoTable given a collection of AeaEtkDataObject.
     *
     * @param allEtkDataObjects A list of ETK data object definition.s
     * @return A list of sorted RDO tables.
     */
    private ArrayList<RdoTable> getOrderedTables (final List<AeaEtkDataObject> allEtkDataObjects) {
    	//Create a list of all tables as RdoTable objects and associate them with a user input sort order.
        final ArrayList<RdoTable> orderedTables = new ArrayList<RdoTable>();
        RdoTable aTable;

        for (final AeaEtkDataObject aDataObject : allEtkDataObjects) {

            if (aDataObject.getDataObjectType() != DataObjectType.REFERENCE) {
                continue;
            }

            aTable = new RdoTable();
            aTable.setTableName(aDataObject.getTableName());

            try {
                aTable.setSortOrder(
                        new Integer(
                                etk.getParameters().getSingle(aDataObject.getTableName() + "_ORDER")).intValue());
            } catch (final Exception e) {
                aTable.setSortOrder(Integer.MAX_VALUE);
            }

            orderedTables.add(aTable);
        }

        //Sort the RdoTable list by the user input order.
        Collections.sort(orderedTables);

        return orderedTables;
    }

    /**
     * Saves selected RDO tables.
     *
     * @param orderedTables A list of RDO tables in order.
     *
     * @throws IOException Unexpected IOException.
     * @throws IncorrectResultSizeDataAccessException Unexpected IncorrectResultSizeDataAccessException.
     */
    private void saveSelectedTables(ArrayList<RdoTable> orderedTables) throws IOException, IncorrectResultSizeDataAccessException {
    	  if (StringUtility.isNotBlank(etk.getParameters().getSingle("saveSelectedTables"))) {

              //Get all RDO tables the user selected to export. Add them to a map.
              final List<String> userSelectedRdoTablesToExport
              			= etk.getParameters().getField("rdo_table_select_checkbox");
              final HashMap<String, String> userSelectedTableMap = new HashMap<String, String>();

              if (userSelectedRdoTablesToExport != null) {
                  for (final String aSelectedTable : userSelectedRdoTablesToExport) {
                      userSelectedTableMap.put(aSelectedTable, null);
                  }
              }

              final ByteArrayOutputStream baos = new ByteArrayOutputStream();
              final ObjectOutputStream oos = new ObjectOutputStream(baos);

              try {
	                oos.writeObject(userSelectedTableMap);
	                oos.flush();
	                oos.close();

	                final String checkedTables = new String(Base64.encodeBase64(baos.toByteArray()));
	                baos.close();


	                final boolean recordExists =
	                        etk.createSQL("select count(*) from T_AEA_CORE_CONFIGURATION where C_CODE = 'dbutils.rdoExportSelectedTables'")
	                        .returnEmptyResultSetAs(0)
	                        .fetchInt() > 0 ? true : false;

	                        if (recordExists) {
	                            etk.createSQL("update T_AEA_CORE_CONFIGURATION set C_DESCRIPTION = :description "
	                                    + "where C_CODE = 'dbutils.rdoExportSelectedTables'")
	                                    .setParameter("description", checkedTables)
	                                    .execute();
	                        } else {
	                            if (Utility.isSqlServer(etk)) {
	                                etk.createSQL("insert into T_AEA_CORE_CONFIGURATION (C_CODE, C_VALUE, C_DESCRIPTION) "
	                                        + "values ('dbutils.rdoExportSelectedTables', 'SYSTEM GENERATED, Do Not Edit', :description)")
	                                        .setParameter("description", checkedTables)
	                                        .execute();
	                            } else {
	                                etk.createSQL("insert into T_AEA_CORE_CONFIGURATION (ID, C_CODE, C_VALUE, C_DESCRIPTION) values "
	                                        + "(object_id.nextval, 'dbutils.rdoExportSelectedTables', 'SYSTEM GENERATED, Do Not Edit', :description)")
	                                        .setParameter("description", checkedTables)
	                                        .execute();
	                            }
	                        }

	                        //Log updated order.
	                        final StringBuilder orderedTablePrinter = new StringBuilder();
	                        orderedTablePrinter.append("UPDATED AEA RDO EXPORT TOOL SELECTED RDO TABLES:\n\n");

	                        for (final RdoTable aRdoTable : orderedTables) {
	                            orderedTablePrinter.append(aRdoTable.getTableName());
	                            orderedTablePrinter.append(" || ");
	                            orderedTablePrinter.append(aRdoTable.getSortOrder());
	                            orderedTablePrinter.append("\n");
	                        }
	                        if (writeDebug) {
	                            Utility.aeaLog(etk, orderedTablePrinter.toString());
	                        }
              } finally {
              	IOUtility.closeQuietly(oos);
              	IOUtility.closeQuietly(baos);
              }
          }
    }

    /**
     * Saves selected RDO table ordering.
     *
     * @param orderedTables A list of RDO tables in order.
     *
     * @throws IOException Unexpected IOException.
     * @throws IncorrectResultSizeDataAccessException Unexpected IncorrectResultSizeDataAccessException.
     */
    private void saveTableOrdering(ArrayList<RdoTable> orderedTables)
    		                                              throws IOException, IncorrectResultSizeDataAccessException {



        //If the user has selected the Save Order Values checkbox, save the RdoTable ordering information
        //to the T_AEA_CORE_CONFIGURATION table with a C_CODE = 'dbutils.rdoExportOrder'
        //The RdoOrder record array is serialized to a Base64 string and stored in the description field.
        if (StringUtility.isNotBlank(etk.getParameters().getSingle("saveOrdering"))) {

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream  oos = new ObjectOutputStream(baos);

            try {
                oos.writeObject(orderedTables);
                oos.flush();
                oos.close();

                final String sortedTableString = new String(Base64.encodeBase64(baos.toByteArray()));
                baos.close();


                final boolean recordExists =
                        etk.createSQL("select count(*) from T_AEA_CORE_CONFIGURATION where C_CODE = 'dbutils.rdoExportOrder'")
                        .returnEmptyResultSetAs(0)
                        .fetchInt() > 0 ? true : false;

                        if (recordExists) {
                            etk.createSQL("update T_AEA_CORE_CONFIGURATION set C_DESCRIPTION = :description "
                                    + "where C_CODE = 'dbutils.rdoExportOrder'")
                                    .setParameter("description", sortedTableString)
                                    .execute();
                        } else {
                            if (Utility.isSqlServer(etk)) {
                                etk.createSQL("insert into T_AEA_CORE_CONFIGURATION (C_CODE, C_VALUE, C_DESCRIPTION) "
                                        + "values ('dbutils.rdoExportOrder', 'SYSTEM GENERATED, Do Not Edit', :description)")
                                        .setParameter("description", sortedTableString)
                                        .execute();
                            } else {
                                etk.createSQL("insert into T_AEA_CORE_CONFIGURATION (ID, C_CODE, C_VALUE, C_DESCRIPTION) values "
                                        + "(object_id.nextval, 'dbutils.rdoExportOrder', 'SYSTEM GENERATED, Do Not Edit', :description)")
                                        .setParameter("description", sortedTableString)
                                        .execute();
                            }
                        }

                        //Log updated order.
                        final StringBuilder orderedTablePrinter = new StringBuilder();
                        orderedTablePrinter.append("UPDATED AEA RDO EXPORT TOOL ORDER TABLES:\n\n");

                        for (final RdoTable aRdoTable : orderedTables) {
                            orderedTablePrinter.append(aRdoTable.getTableName());
                            orderedTablePrinter.append(" || ");
                            orderedTablePrinter.append(aRdoTable.getSortOrder());
                            orderedTablePrinter.append("\n");
                        }
                        if (writeDebug) {
                            Utility.aeaLog(etk, orderedTablePrinter.toString());
                        }
            } finally {
            	IOUtility.closeQuietly(oos);
            	IOUtility.closeQuietly(baos);
            }
        }

    }

    /**
     * Returns a list of HTML input check boxes for each AeaEtkDataObject of type REFERENCE.
     *
     * Ex:
     *
     * AEA Audit Log (T_AEA_AUDIT_LOG) [X]
     * AEA CORE Configuration (T_AEA_CORE_CONFIGURATION) [X]
     *
     * @param dataObjects All etk data objects.
     * @param tableOrderingData mapping from table name to the export order (stored as a String)
     * @param selectedRdos A map where all keys are objects which should be exported
     * @return Input box element for each RDO object in format LABEL (TABLE_NAME)
     * @throws ApplicationException If a problem is encountered
     */
    private String getRdoTableMSOptionList (final List<AeaEtkDataObject> dataObjects,
            final HashMap<String, String> tableOrderingData,
            final HashMap<String, String> selectedRdos) throws ApplicationException {
        int msListNum = 1;
        final StringBuilder sb = new StringBuilder();

        for (final AeaEtkDataObject aDataObject : dataObjects) {

            if (aDataObject.getDataObjectType() != DataObjectType.REFERENCE) {
                continue;
            }

            final String storedDataObjectOrder = StringUtility.isBlank(
                    tableOrderingData.get(aDataObject.getTableName())) ?
                                                                        "" : tableOrderingData.get(aDataObject.getTableName());

            int recordsForTable = 0;
            try {
                recordsForTable = etk.createSQL("select count(1) from " + aDataObject.getTableName()).fetchInt();
            } catch (IncorrectResultSizeDataAccessException e) {
                throw new ApplicationException(e);
            }

            sb.append("<input style=\"height:30px; width:50px;\" ");
            sb.append(" type=\"text\" value=\"" + StringEscapeUtils.escapeHtml(storedDataObjectOrder) + "\" name=\"" + StringEscapeUtils.escapeHtml(aDataObject.getTableName()) + "_ORDER\">");
            sb.append("<label for=\"rdo_table_select_checkbox_" + msListNum + "\">");
            sb.append("<input ");

            if (selectedRdos.containsKey(aDataObject.getTableName())) {
                sb.append(" checked=\"\" ");
            } else {
                sb.append(isCheckedMs("rdo_table_select_checkbox", aDataObject.getTableName()));
            }

            sb.append(" id=\"rdo_table_select_checkbox_" + msListNum++ + "\" type=\"checkbox\" value=\"" + StringEscapeUtils.escapeHtml(aDataObject.getTableName()) + "\" name=\"rdo_table_select_checkbox\">");
            sb.append(StringEscapeUtils.escapeHtml(aDataObject.getLabel()));
            sb.append(" (");
            sb.append(StringEscapeUtils.escapeHtml(aDataObject.getTableName()));
            sb.append(" - ");
            sb.append(recordsForTable);
            sb.append(" Rows)</label>");
            sb.append("<br>");
        }

        return sb.toString();
    }

    /**
     * Method checks if a checkbox was selected before page refresh and writes a checked="" value back to the view
     * to maintain its checked status.
     *
     * @param fieldName Name of input checkbox.
     * @param value Value of checkbox.
     * @return "" or checked=""
     */
    private String isCheckedMs (final String fieldName, final String value) {
        final List<String> unEscapedValueList = etk.getParameters().getMap().get(fieldName);

        if (unEscapedValueList != null) {
            for (final String aValue : unEscapedValueList) {
                if (value.equals(aValue)) {
                    return " checked=\"\" ";
                }
            }
        }

        return " ";
    }
}
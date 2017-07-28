/**
 *
 * RdoExportLogic
 *
 * aclee 03/15/2017
 **/

package net.micropact.aea.dbUtils.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataAccessException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.platform.DatabasePlatform;

import net.micropact.aea.core.ioUtility.Base64;
import net.micropact.aea.core.ioUtility.Hex;
import net.micropact.aea.core.ioUtility.IOUtility;
import net.micropact.aea.core.utility.StringEscapeUtils;
import net.micropact.aea.dbUtils.page.rdodataexport.RdoTable;
import net.micropact.aea.dbUtils.utility.RdoDataExportUtility;
import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.DataObjectType;
import net.micropact.aea.utility.Utility;
import net.micropact.aea.utility.lookup.AeaEtkDataElement;
import net.micropact.aea.utility.lookup.AeaEtkDataObject;
import net.micropact.aea.utility.lookup.AeaEtkLookupDefinition;
import net.micropact.aea.utility.lookup.LookupDataUtility;

/**
 * Defines the logic necessary to export an entellitrak RDO data object.
 *
 * @author MicroPact
 *
 */
public class RdoExportLogic {
	private static final String NEWLINE_CHAR = "\n";
	private static final String STATEMENT_SEPERATOR_UUID = UUID.randomUUID().toString();
	private static final String STATEMENT_SEPERATOR =
			NEWLINE_CHAR + STATEMENT_SEPERATOR_UUID + NEWLINE_CHAR;
	private static final String PRINTED_STATEMENT_SEPERATOR =
			NEWLINE_CHAR + "-----------------------------------------------" + NEWLINE_CHAR;
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private final ExecutionContext etk;

    private final DatabasePlatform databasePlatform;
    private final boolean writeDebug;
    private final boolean plsqlStandalongSyntax;
    private String currentlyExportingRdo = "";
    private Map <Long, String> fullyQualifiedScriptNames = new HashMap<Long, String>();
    private final StringBuilder errorMessages = new StringBuilder();
    private List<Number> filesToTransfer = new ArrayList<Number>();


    /**
     * RDO Export logic constructor.
     *
     * @param localEtk Execution context.
     * @param aDatabasePlatform Oracle or SQLServer.
     * @param aWriteDebugToLog Whether to write additional debug to the logs.
     * @param aPlsqlStandaloneSyntax Whether to export for SQLDeveloper(true) or PLSQL in entellitrak (false).
     */
    public RdoExportLogic (final ExecutionContext localEtk,
    		               final DatabasePlatform aDatabasePlatform,
    		               final boolean aWriteDebugToLog,
    		               final boolean aPlsqlStandaloneSyntax) {
    	this.etk = localEtk;
    	this.databasePlatform = aDatabasePlatform;
    	this.writeDebug = aWriteDebugToLog;
    	this.plsqlStandalongSyntax = aPlsqlStandaloneSyntax;
    }

    /**
     * Helper method to return files attached to RDOs in ZIP file along with the SQL to perform the export.
     * Main method to generate a "2-part" export bundle.
     *
     * @param sqlRdoExportData SQL containing RDO data records to insert into target system.
     *
     * @return ZIP file containing all files attached to RDOs, SQL and instructions in ZIP file.
     *
     * @throws IOException Unexpected IOException.
     * @throws ApplicationException Unexpected ApplicationException.
     */
    private InputStream handleSeperateFileExport (final List<String> sqlRdoExportData)
    		throws IOException, ApplicationException {
        final PipedInputStream inputStream = new PipedInputStream();
        final ExecutionContext localEtk = etk;
        final PipedOutputStream pos = new PipedOutputStream(inputStream);
        final ZipOutputStream outputStream = new ZipOutputStream(pos);

        List<Map<String, Object>> allFiles = new ArrayList<Map<String, Object>>();

        if (filesToTransfer.size() > 0) {
            try {

            	StringBuilder query = new StringBuilder();
            	query.append("select ID, FILE_NAME, FILE_SIZE, CONTENT_TYPE, FILE_TYPE, FILE_EXTENSION, "
                        + "OBJECT_TYPE, REFERENCE_ID, RESOURCE_PATH, ETK_DM_RESOURCE_ID, TOKEN, "
                        + "TIME_REQUESTED from etk_file where ");
            	Map<String, Object> paramMap = new HashMap<String, Object>();
            	Utility.addLargeInClause("ID", query, paramMap, filesToTransfer);

                allFiles = localEtk.createSQL(query.toString())
                        .setParameter(paramMap)
                        .fetchList();
            } catch (DataAccessException e) {
                Utility.aeaLog(localEtk, e);
                throw new ApplicationException("Error retrieving metadata from etk_file", e);
            }
        }

        final List<Map<String, Object>> allFilesNM = allFiles;

        new Thread(new Runnable(){

            @Override
            public void run() {
                try {
                    outputStream.putNextEntry(new ZipEntry("README.txt"));
                    outputStream.write(("To perform RDO data import, perform the following steps.\n\n"
                    		+ "** If importing via SQLDeveloper **\n\n"

                            + "1. Exact files database_inserts.sql and ETK_FILE_DATA.zip from this archive.\n"
                            + "2. Install latest DBUtils package on the destination system.\n"
                            + "3. On the destination system, as an Administrator, open page "
                            + "AEA - DbUtils - RDO ETK_FILE Import Page.\n"
                            + "4. Browse for the extracted ETK_FILE_DATA.zip and select Import. "
                            + "If the import does not succeed, do not continue.\n"
                            + "5. Log out of entellitrak.\n"
                            + "6. Using SqlDeveloper or equivalent program, "
                            + "execute database_inserts.sql against the destination database."

							+ "** If importing via AEA - DbUtils - RDO Importer v2.0 **\n\n"

                            + "1. Install latest DBUtils package on the destination system.\n"
                            + "2. On the destination system, as an Administrator, "
                            + "open page AEA - DbUtils - RDO Importer v2.0.\n"
                            + "3. Select the rdo_export_xxxxxxxxxx.zip generated by the export page and select Import."

                    		).getBytes());
                    outputStream.closeEntry();

                    for (int i = 0; i < sqlRdoExportData.size(); i++) {
	                    outputStream.putNextEntry(new ZipEntry("database_inserts_" + i + ".sql"));
	                    outputStream.write(sqlRdoExportData.get(i).getBytes());
	                    outputStream.closeEntry();
                    }

                    if (allFilesNM.size() > 0) {
                        outputStream.putNextEntry(new ZipEntry("ETK_FILE_DATA.zip"));

                        final ZipOutputStream etkFilesZip = new ZipOutputStream(outputStream);


                        String fileName = "";
                        Long fileId = null;

                        for (Map<String, Object> aFile : allFilesNM) {

                            InputStream etkFileStream = null;

                            try{

                                fileName = (String) aFile.get("FILE_NAME");
                                fileId = ((Number) aFile.get("ID")).longValue();

                                etkFileStream = etk.getFileService().get(fileId).getContent();

                                etkFilesZip.putNextEntry(new ZipEntry(fileId + "_(" + fileName + ").zip"));

                                final ZipOutputStream fileDataZip = new ZipOutputStream(etkFilesZip);

                                fileDataZip.putNextEntry(new ZipEntry("ETK_FILE_METADATA.xml"));
                                fileDataZip.write(RdoDataExportUtility.getXmlForFile(Collections.singletonList(aFile)));
                                fileDataZip.closeEntry();

                                fileDataZip.putNextEntry(new ZipEntry(fileName));
                                IOUtility.copy(etkFileStream, fileDataZip);
                                fileDataZip.closeEntry();
                                fileDataZip.finish();

                                etkFilesZip.closeEntry();
                            }finally{
                                IOUtility.closeQuietly(etkFileStream);
                            }
                        }

                        etkFilesZip.finish();
                        outputStream.closeEntry();
                    } else {
                        outputStream.putNextEntry(new ZipEntry("NO_ETK_FILES_EXPORTED.txt"));
                        outputStream.write("No files were exported from ETK_FILE, no file import necessary".getBytes());
                        outputStream.closeEntry();
                    }

                    outputStream.finish();
                    outputStream.flush();
                    outputStream.close();

                    pos.flush();
                    pos.close();
                } catch (Exception e) {
                	IOUtility.closeQuietly(outputStream);
                	IOUtility.closeQuietly(pos);

                    Utility.aeaLog(localEtk, e);
                }
            }
        }).start();

        return inputStream;
    }

    /**
     * Returns an SQL string to filter an ETK or tracked data reference table.
     * Value stored in "Additional Filter Criteria" column of T_AEA_RDO_DATA_EXPORT_CONFIG
     *
     * @param rDataTableName The name of the reference data table.
     * @return Any filter criteria value that the user input for that table.
     */
    private String getAdditionalFilterCriteria (final String rDataTableName) {
        try {

            final String query = " select C_ADDITIONAL_FILTER_CRITERIA as ADD_FILT_CRIT " +
                    " from T_AEA_RDO_DATA_EXPORT_CONFIG " +
                    " where upper(c_database_table_name) = upper(:tableName) ";
            if (writeDebug) {
                Utility.aeaLog(etk, query + ", :tableName = " + rDataTableName);
            }

            return etk.createSQL(query)
                    .setParameter("tableName", rDataTableName)
                    .returnEmptyResultSetAs("")
                    .fetchString();
        } catch (final Exception e) {
            if (writeDebug) {
                Utility.aeaLog(etk, "Could not retrieve C_ADDITIONAL_FILTER_CRITERIA "
                        + "from T_AEA_RDO_DATA_EXPORT_CONFIG", e);
            }
        }

        return "";
    }

    /**
     * Returns all column and value mappings for a single row of a reference data table.
     *
     * @param rDataTableName The name of the reference data table.
     * @param rDataValueColumn The name of a column in the r-data table.
     * @param rawValue That columns raw value.
     * @param additonalFilterCriteria Any additional filter criteria to limit the number of rows returned.
     *
     * @return Single row of the r-data table.
     */
    private Map<String, Object> getLookupTableRow (final String rDataTableName,
            final String rDataValueColumn,
            final Object rawValue,
            final String additonalFilterCriteria) {
        Map<String, Object> lookupTableRowData = new HashMap<String, Object>();
        String query = "select * from " +
                rDataTableName +
                " where " + rDataValueColumn + " = :rawValue" ;

        try {
            if (StringUtility.isNotBlank(additonalFilterCriteria)) {
                query += " AND " + additonalFilterCriteria;
            }
            if (writeDebug) {
                Utility.aeaLog(etk, query + ", rawValue = " + rawValue);
            }

            lookupTableRowData = etk.createSQL(query)
                    .setParameter("rawValue", rawValue)
                    .returnEmptyResultSetAs(new HashMap<String, Object>())
                    .fetchMap();
        } catch (final IncorrectResultSizeDataAccessException irse) {
            if (writeDebug) {
                Utility.aeaLog(etk, currentlyExportingRdo + " - IncorrectResultSizeDataAccessException - "
                        + "Could not retrieve data from table " + rDataTableName, irse);
            }

            errorMessages.append("--" + currentlyExportingRdo + " - More than one value retrieved from TABLE=\""
                    + rDataTableName
                    + "\" WHERE "
                    + rDataValueColumn
                    + " = "
                    + rawValue
                    + " - Please define a UID combination for this table in the "
                    + "AEA RDO Data Export Config Listing or verify that all C_CODE column"
                    + "values are unique. Query = " + query);
            errorMessages.append(NEWLINE_CHAR);
        } catch (final DataAccessException dae) {
            if (writeDebug) {
                Utility.aeaLog(etk, currentlyExportingRdo + " - DataAccessException - Could not retrieve data from table " + rDataTableName, dae);
            }
            errorMessages.append("--" + currentlyExportingRdo + " - DataAccessException for query \"" + query + "\", rawValue = " + rawValue +
                    ", Exception =" + dae.toString());
            errorMessages.append(NEWLINE_CHAR);
        } catch (final Exception e) {
            if (writeDebug) {
                Utility.aeaLog(etk, currentlyExportingRdo + " - Exception - Could not retrieve data from table " + rDataTableName, e);
            }
            errorMessages.append("--" + currentlyExportingRdo + " - Exception for query \"" + query + "\", rawValue = " + rawValue +
                    ", Exception =" + e.toString());
            errorMessages.append(NEWLINE_CHAR);
        }

        return lookupTableRowData;
    }


    /**
     * Attempts to find any user defined "UID" columns for the table (the equivilant of C_CODE). This is necessary
     * for tables that are used in lookups but do not have C_CODE columns (ex.. etk tables). Stored in
     * T_AEA_RDO_DATA_EXPORT_CONFIG RDO.
     *
     * @param rDataTableName The name of the R_DATA Table.
     * @return Any defined UID columns from the T_AEA_RDO_DATA_EXPORT_CONFIG RDO.
     */
    private List<Map<String, Object>> getLookupTableUIDColumns (final String rDataTableName) {
        List<Map<String, Object>> lookupTableUIDColumns = new ArrayList<Map<String, Object>>();

        try {

            final String query = " select rdoUID.c_uid_column as COLUMN_NAME " +
                    " from T_AEA_RDO_DATA_EXPORT_CONFIG ec " +
                    " join M_RDO_UID_COLUMNS rdoUID on rdoUID.id_owner = ec.id " +
                    " where upper(ec.c_database_table_name) = upper(:tableName) ";
            if (writeDebug) {
                Utility.aeaLog(etk, query + ", :tableName = " + rDataTableName);
            }

            lookupTableUIDColumns = etk.createSQL(query)
                    .setParameter("tableName", rDataTableName)
                    .returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
                    .fetchList();
        } catch (final Exception e) {
            if (writeDebug) {
                Utility.aeaLog(etk, "Could not retrieve c_uid_column "
                        + "from M_RDO_UID_COLUMNS", e);
            }
        }

        return lookupTableUIDColumns;
    }

    /**
     * Exports RDO as SQL for one stage import.
     *
     * @return SQL containing RDO SQL insert/update statements.
     * @throws ApplicationException Unexpected exception.
     */
    public String exportToSqlOneStage() throws ApplicationException {
    	return exportToSqlOneStage(getSavedTablesToExport());
    }

    /**
     * Export RDO as ZIP containing SQL and Files in 2nd ZIP for "2-stage" import or for import via the
     * RDO Import Page v2.0.
     *
     * @param maxFileLength The maximum length of the database_inserts_X.sql in the ZIP file
     * @return ZIP file as InputStream.
     *
     * @throws ApplicationException  Unexpected exception.
     * @throws IOException  Unexpected exception.
     */
    public InputStream exportToZipTwoStage(int maxFileLength) throws ApplicationException, IOException {
    	return exportToZipTwoStage(getSavedTablesToExport(), maxFileLength);
    }

    /**
     * Exports RDO as SQL for one stage import.
     *
     * @param tableExportList Tables to export.
     * @return SQL containing RDO SQL insert/update statements.
     * @throws ApplicationException Unexpected exception.
     */
    public String exportToSqlOneStage(Map<String, Integer> tableExportList) throws ApplicationException {
    	List<String> sqlScript = buildSql(tableExportList, true, 0);

    	return sqlScript.get(0);
    }

	/**
	 * Export RDO as ZIP containing SQL and Files in 2nd ZIP for "2-stage" import or for import via the RDO Import Page
	 * v2.0.
	 *
	 * @param tableExportList
	 *            Tables to export.
	 * @param maxFileLength
	 *            The maximum size of the PLSQL procedure inside of the ZIP.
	 *
	 * @return ZIP file as InputStream.
	 *
	 * @throws ApplicationException
	 *             Unexpected exception.
	 * @throws IOException
	 *             Unexpected exception.
	 */
	public InputStream exportToZipTwoStage(Map<String, Integer> tableExportList, int maxFileLength)
			throws ApplicationException, IOException {

		List<String> sqlScript = buildSql(tableExportList, false, maxFileLength);

		return handleSeperateFileExport(sqlScript);
	}

    /**
     * Returns a Map of table names and orders to export.
     *
     * @return A Map of table names and orders to export.
     * @throws ApplicationException Unexpected ApplicationException.
     */
    @SuppressWarnings("unchecked")
	private HashMap<String, Integer> getSavedTablesToExport() throws ApplicationException {
        String exportOrderConfig = "";
        String exportTableConfig = "";

        //Load Base64 string from T_AEA_CORE_CONFIGURATION containing saved export order values.
        try {
            exportOrderConfig =
                    etk.createSQL("select c_description from T_AEA_CORE_CONFIGURATION "
                            + "where c_code = 'dbutils.rdoExportOrder'")
                            .returnEmptyResultSetAs("")
                            .fetchString();
        } catch (final Exception e) {
            throw new ApplicationException("Could not read C_DESCRIPTION from T_AEA_CORE_CONFIGURATION "
                    + "WHERE CODE = 'dbutils.rdoExportOrder'", e);
        }

        try {
            exportTableConfig =
                    etk.createSQL("select c_description from T_AEA_CORE_CONFIGURATION "
                            + "where c_code = 'dbutils.rdoExportSelectedTables'")
                            .returnEmptyResultSetAs("")
                            .fetchString();
        } catch (final Exception e) {
            throw new ApplicationException("Could not read C_DESCRIPTION from T_AEA_CORE_CONFIGURATION "
                    + "WHERE CODE = 'dbutils.rdoExportSelectedTables'", e);
        }

        final HashMap<String, Integer> tableOrderingData = new HashMap<String, Integer>();

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
                    tableOrderingData.put(aTable.getTableName(), aTable.getSortOrder());
                }
            } catch(final Exception e) {
                if (writeDebug) {
                    Utility.aeaLog(etk, "Error decoding table ordering data from "
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
                    Utility.aeaLog(etk, "Error decoding table ordering data from "
                            + "dbutils.rdoExportSelectedTables record in T_AEA_CORE_CONFIGURATION", e);
                }
            } finally {
            	IOUtility.closeQuietly(ois);
            }
        }

        HashMap<String, Integer> savedSelectedTableList = new HashMap<String, Integer>();

        for (String aTable : selectedRdos.keySet()) {
        	savedSelectedTableList.put(aTable, tableOrderingData.get(aTable));
        }

        return savedSelectedTableList;

    }


    /**
     * This is the main monster method that actually performs the RDO export logic. Takes a list of
     * all ETK data objects in the system and returns a String of PLSQL that gets written to an SQL file to perform
     * the data inserts.
     *
     * @param tableAndExportOrder Tables (and corresponding order of) to export.
     * @param exportAsSql Whether to export as SQL (true) or ZIP (false).
     * @param maxSqlFileLength The maximum length of the output SQL file.
     * @return PLSQL to insert RDO data into target environment.
     * @throws ApplicationException If a problem is encountered
     */
    private List<String> buildSql
              (final Map<String, Integer> tableAndExportOrder,
               final boolean exportAsSql,
               final int maxSqlFileLength) throws ApplicationException {
    	final List<String> sqlFiles = new ArrayList<String>();
        final List<AeaEtkDataObject> allEtkDataObjects = LookupDataUtility.getAllEtkDataObjects(etk);
        this.fullyQualifiedScriptNames = RdoDataExportUtility.getFullyQualifiedScriptNames(etk);

        try {
            //Create a list of all tables as RdoTable objects and associate them with a user input sort order.
            final ArrayList<RdoTable> orderedTables = new ArrayList<RdoTable>();
            RdoTable aTable;
            for (final AeaEtkDataObject aDataObject : allEtkDataObjects) {

                if (aDataObject.getDataObjectType() != DataObjectType.REFERENCE) {
                    continue;
                }

                aTable = new RdoTable();
                aTable.setTableName(aDataObject.getTableName());

                Integer order =
                    		tableAndExportOrder.get(aDataObject.getTableName()) == null ?
                    				Integer.MAX_VALUE : tableAndExportOrder.get(aDataObject.getTableName());

                aTable.setSortOrder(order);

                orderedTables.add(aTable);
            }
            //Sort the RdoTable list by the user input order.
            Collections.sort(orderedTables);

            String columnName;
            Object value;

            AeaEtkDataObject matchingEtkDataObjct = null;
            List<Map<String, Object>> tableDataRowList;

            //Output writers.

            final StringBuilder insertData = new StringBuilder();
            final StringBuilder variableDeclares = new StringBuilder();
            final StringBuilder variableInits = new StringBuilder();

            if (exportAsOracle()) {
                variableDeclares.append(NEWLINE_CHAR + "rawTempVar RAW(32000); " + NEWLINE_CHAR);
                variableDeclares.append(NEWLINE_CHAR + "longRawTempVar long raw; " + NEWLINE_CHAR);
            }

            int fileCounter = 0;

            if ((tableAndExportOrder != null) && (tableAndExportOrder.keySet().size() > 0)) {
                for (final RdoTable aRdoTable : orderedTables) {

                    final String rdoTableName = aRdoTable.getTableName();


                    //Only export the tables that the user has selected.
                    if (!tableAndExportOrder.containsKey(rdoTableName)) {
                        continue;
                    }
                    if (writeDebug) {
                        Utility.aeaLog(etk, "RDO Export Beginning for Table " + rdoTableName);
                    }
                    insertData.append("------------------BEGIN TABLE = ");
                    insertData.append(rdoTableName);
                    insertData.append("---------------------------");

                    insertData.append(NEWLINE_CHAR);

                    //Find the AeaEtkDataObject for the table that the user selected.
                    for (final AeaEtkDataObject etkDataObject : allEtkDataObjects) {
						if (rdoTableName.equalsIgnoreCase(etkDataObject.getTableName())) {
                            matchingEtkDataObjct = etkDataObject;
                            break;
                        }
                    }
                    currentlyExportingRdo = matchingEtkDataObjct.getLabel()
                    		               + "(" + matchingEtkDataObjct.getTableName() + ")";

                    //Return all rows from the selected RDO table and loop through them
                    tableDataRowList = etk.createSQL("select * from " + rdoTableName).fetchList();

                    //Define row write buffers.
                    StringBuilder plsqlStatement;
                    StringBuilder sqlServerFileInsertStatements;
                    StringBuilder insertStatement;
                    StringBuilder updateStatement;
                    StringBuilder mTableDeleteStatements;
                    StringBuilder mTableInsertStatements;
                    Object tableRowCCode;
                    Object tableRowId;

                    rowLoop:
                        for (final Map<String, Object> aTableRow : tableDataRowList) {

                            plsqlStatement = new StringBuilder();
                            insertStatement = new StringBuilder();
                            sqlServerFileInsertStatements = new StringBuilder();
                            updateStatement = new StringBuilder();
                            mTableDeleteStatements = new StringBuilder();
                            mTableInsertStatements = new StringBuilder();

                            //If the row of reference data being exported does not have a valid C_CODE value,
                            //add an error to the top of the file and continue with the next row. ALL exported rows must
                            //have a C_CODE value.
                            tableRowCCode = aTableRow.get("C_CODE");
                            tableRowId = aTableRow.get("ID");
                            if (tableRowCCode == null ||
                                    (
                                            (tableRowCCode instanceof String) &&
                                            (StringUtility.isBlank((String) tableRowCCode))
                                            )) {


                                errorMessages.append("--" + currentlyExportingRdo
                                		+" - ERROR exporting record with ID = " + tableRowId
                                		+ " from table " + rdoTableName
                                        + " - C_CODE is null or blank, skipping record.");

                                errorMessages.append(NEWLINE_CHAR);

                                continue;
                            }

                            if (exportAsOracle()) {
                                insertStatement.append("insert (");
                            } else {
                                insertStatement.append("insert into ");
                                insertStatement.append(rdoTableName);
                                insertStatement.append(" (");
                            }

                            for (final AeaEtkDataElement anElement : matchingEtkDataObjct.getDataElements()) {

                                //Only add column inserts for non-M data table elements.
                                if (StringUtility.isBlank(anElement.getmTableName())) {
                                    insertStatement.append(anElement.getColumnName());
                                    insertStatement.append(",");
                                }
                            }

                            if (exportAsOracle()) {
                                insertStatement.append("ID) values (");
                            } else {

                                if ((insertStatement.length() > 0) && insertStatement.charAt(insertStatement.length() - 1) == ',') {
                                    insertStatement.setLength(insertStatement.length() - 1);
                                }

                                insertStatement.append(") values (");
                            }

                            //Begin exporting columns (aka data element values) for each row of reference data.
                            for (final AeaEtkDataElement anElement : matchingEtkDataObjct.getDataElements()) {

                                columnName = anElement.getColumnName();
                                value = aTableRow.get(columnName);

                                if (writeDebug) {
                                    Utility.aeaLog(etk, "Processing AeaEtkDataElement with columnName = "
                                            + columnName + " || value = " + value);
                                }

                                if (!anElement.getIsBoundToLookup()) {
                                    if (writeDebug) {
                                        Utility.aeaLog(etk, columnName + " is not a lookup.");
                                    }

                                    if (value == null) {
                                        value = "null";

                                    } else if (DataElementType.NUMBER == anElement.getDataType() ||
                                    		   DataElementType.LONG == anElement.getDataType()) {
                                        value = ((Number) value).longValue();
                                    } else if (DataElementType.DATE == anElement.getDataType() ||
                                            DataElementType.TIMESTAMP == anElement.getDataType()) {

                                    	SimpleDateFormat dateF = new SimpleDateFormat(DATE_FORMAT);

                                        if (exportAsOracle()) {
                                            value = (value != null) ? "to_date('"
                                                    + dateF.format(value)
                                                    + "','RRRR-MM-DD HH24:MI:SS')"
                                                    : "null";
                                        } else {
                                            value = (value != null) ?
                                                                     "CONVERT(varchar, '" + dateF.format(value) + "', 120 )" :
                                                                         "null";
                                        }
                                    } else if (DataElementType.TEXT == anElement.getDataType()) {
                                        value = "'" + StringEscapeUtils.escapeSql((String) value) + "'";
                                    } else if (DataElementType.LONG_TEXT == anElement.getDataType()) {
                                        value = RdoDataExportUtility.convertClobToSQLInsert(value, exportAsOracle());
                                    } else if(DataElementType.FILE == anElement.getDataType()) {
                                        try {
                                            if (exportAsSql) {
                                                value = handleFile (variableDeclares, variableInits,
                                                        sqlServerFileInsertStatements,
                                                        fileCounter, value);
                                                fileCounter++;
                                            } else {
                                                filesToTransfer.add((Number) value);

                                                if (this.exportAsOracle()) {
                                                    value = "(select (select C_ETK_FILE_ID from T_AEA_RDO_FILE_STAGING where C_SOURCE_SYSTEM_ID = "
                                                            + ((Number) value).longValue()
                                                            + ") from dual)";
                                                } else {
                                                    value = "(select C_ETK_FILE_ID from T_AEA_RDO_FILE_STAGING where C_SOURCE_SYSTEM_ID = "
                                                            + ((Number) value).longValue()
                                                            + ")";
                                                }
                                            }
                                        } catch (final Exception e) {
                                        	Utility.aeaLog(etk,
                                        			       "Error processing file with ID " + value + ", continuing.",
                                        			       e);
                                            continue rowLoop;
                                        }
                                    }

                                    insertStatement.append(value);
                                    insertStatement.append(",");

                                    //If this is the c_code column, don't put an update statement in the record.
                                    if (!"c_code".equalsIgnoreCase(columnName)) {
                                        updateStatement.append(columnName);
                                        updateStatement.append(" = ");
                                        updateStatement.append(value);
                                        updateStatement.append(",");
                                    }
                                } else { //Value is bound to a lookup.
                                    final AeaEtkLookupDefinition lookupDef = anElement.getEtkLookupDefinition();
                                    if (writeDebug) {
                                        Utility.aeaLog(etk, columnName + " is a lookup of type " + lookupDef.getLookupType());
                                    }

                                    if (StringUtility.isBlank(anElement.getmTableName())) {
                                        //Handle Single Value Lookup
                                        handleSingleValueLookup (
                                                anElement, value, insertStatement, updateStatement);
                                    } else {
                                        //Handle Multi Value Lookup
                                        handleMDataLookup (anElement, rdoTableName, tableRowId, tableRowCCode,
                                                 mTableDeleteStatements, mTableInsertStatements);
                                    }
                                }
                            }

                            if (exportAsOracle()) {
                                insertStatement.append("object_id.nextval);");
                            } else {
                                if ((insertStatement.length() > 0) && insertStatement.charAt(insertStatement.length() - 1) == ',') {
                                    insertStatement.setLength(insertStatement.length() - 1);
                                }

                                insertStatement.append(");");
                            }


                            //Remove trailing , on update statement if one exists.
                            if ((updateStatement.length() > 0) &&
                                    updateStatement.charAt(updateStatement.length() -1 ) == ',') {
                                updateStatement.setLength(updateStatement.length() - 1);
                            }


                            if (exportAsOracle()) {

                            	//If the update statement is blank because C_CODE is the only other column in the table
                            	//other than ID, set ID = ID.
                            	if (StringUtility.isBlank(updateStatement.toString())) {
                                	updateStatement.append(" ID = ID ");
                                }

                                plsqlStatement.append(NEWLINE_CHAR);
                                plsqlStatement.append("tempCode := '");
                                plsqlStatement.append(StringEscapeUtils.escapeSql((String) aTableRow.get("C_CODE")));
                                plsqlStatement.append("';");
                                plsqlStatement.append(NEWLINE_CHAR);
                                plsqlStatement.append("MERGE INTO ");
                                plsqlStatement.append(rdoTableName);
                                plsqlStatement.append(" sr USING dual ON (sr.c_code = tempCode) ");
                                plsqlStatement.append(NEWLINE_CHAR);
                                plsqlStatement.append("WHEN MATCHED THEN");
                                plsqlStatement.append(NEWLINE_CHAR);
                                plsqlStatement.append(" UPDATE SET ");
                                plsqlStatement.append(updateStatement.toString());
                                plsqlStatement.append(NEWLINE_CHAR);
                                plsqlStatement.append("WHEN NOT MATCHED THEN ");
                                plsqlStatement.append(NEWLINE_CHAR);
                                plsqlStatement.append(insertStatement);
                            } else {
                                plsqlStatement.append(NEWLINE_CHAR);
                                plsqlStatement.append(sqlServerFileInsertStatements);
                                plsqlStatement.append(NEWLINE_CHAR);
                                plsqlStatement.append("SET @tempCode = '");
                                plsqlStatement.append(StringEscapeUtils.escapeSql((String) aTableRow.get("C_CODE")));
                                plsqlStatement.append("';");

                                //Only build an update statement if the updateStatement is not blank.
                                //RDOs with ONLY a C_CODE column can return a blank update statement...
                                if (StringUtility.isNotBlank(updateStatement.toString())) {
	                                plsqlStatement.append(NEWLINE_CHAR);
	                                plsqlStatement.append("IF EXISTS( SELECT 1 FROM ");
	                                plsqlStatement.append(rdoTableName);
	                                plsqlStatement.append(" WHERE lower(C_CODE) = lower(@tempCode))");

	                                plsqlStatement.append(NEWLINE_CHAR);
	                                plsqlStatement.append("BEGIN");
	                                plsqlStatement.append(NEWLINE_CHAR);
	                                plsqlStatement.append(" UPDATE ");
	                                plsqlStatement.append(rdoTableName);
	                                plsqlStatement.append(" SET ");
	                                plsqlStatement.append(updateStatement.toString());
	                                plsqlStatement.append(" WHERE lower(C_CODE) = lower(@tempCode)");
	                                plsqlStatement.append(NEWLINE_CHAR);
	                                plsqlStatement.append(" END ");
	                                plsqlStatement.append(NEWLINE_CHAR);
                                }

                                plsqlStatement.append(NEWLINE_CHAR);
                                plsqlStatement.append("IF NOT EXISTS( SELECT 1 FROM ");
                                plsqlStatement.append(rdoTableName);
                                plsqlStatement.append(" WHERE lower(C_CODE) = lower(@tempCode))");

                                plsqlStatement.append(NEWLINE_CHAR);
                                plsqlStatement.append("BEGIN");
                                plsqlStatement.append(NEWLINE_CHAR);
                                plsqlStatement.append(insertStatement);
                                plsqlStatement.append(NEWLINE_CHAR);
                                plsqlStatement.append(" END ");
                            }

                            plsqlStatement.append(NEWLINE_CHAR);
                            plsqlStatement.append(NEWLINE_CHAR);
                            plsqlStatement.append(mTableDeleteStatements);
                            plsqlStatement.append(NEWLINE_CHAR);
                            plsqlStatement.append(NEWLINE_CHAR);
                            plsqlStatement.append(mTableInsertStatements);

                            plsqlStatement.append(NEWLINE_CHAR);
                            insertData.append(plsqlStatement);

                            insertData.append(STATEMENT_SEPERATOR);
                        }
                }
            }

            String escapedErrors = "";

            if (errorMessages != null && errorMessages.length() > 0) {
                escapedErrors =	StringEscapeUtils.escapeSql(
                        errorMessages.toString().replaceAll("\\n", "\n--").replaceAll("\\r", "\r--"));
            }

            if (maxSqlFileLength == 0) {
	            final StringBuilder sqlOutput = new StringBuilder();

	            String insertDataString = insertData.toString();

	            sqlOutput.append(printSQLHeader(escapedErrors, variableDeclares.toString(), variableInits.toString()));
	            insertDataString =
	            		insertDataString.replaceAll(STATEMENT_SEPERATOR, PRINTED_STATEMENT_SEPERATOR);
	            sqlOutput.append(insertDataString);
	            sqlOutput.append(printSQLFooter(true));

	            sqlFiles.add(sqlOutput.toString());
            } else {

            	String header = printSQLHeader(escapedErrors, variableDeclares.toString(), variableInits.toString());
            	String footer = printSQLFooter(true);
            	String insertDataString = insertData.toString();

            	int headerLength = getLineCount(header);
            	int insertDataLength = getLineCount(insertDataString);
            	int footerLength = getLineCount(footer);
            	int totalLength = headerLength + insertDataLength + footerLength;


            	if (totalLength <= maxSqlFileLength) {
            		final StringBuilder sqlOutput = new StringBuilder();
            		sqlOutput.append(header);
            		insertDataString =
            				insertDataString.replaceAll(STATEMENT_SEPERATOR, PRINTED_STATEMENT_SEPERATOR);
    	            sqlOutput.append(insertDataString);
    	            sqlOutput.append(footer);

    	            sqlFiles.add(sqlOutput.toString());
            	} else {
            		int insertSplitSize = maxSqlFileLength - (headerLength + footerLength);
            		String[] insertStatementsSplit =
            				insertDataString.split(STATEMENT_SEPERATOR);

            		StringBuilder currentFileInsertData = new StringBuilder();
            		for (int i = 0; i < insertStatementsSplit.length; i++) {
            			currentFileInsertData.append(insertStatementsSplit[i]);
            			currentFileInsertData.append(PRINTED_STATEMENT_SEPERATOR);

            			if ((getLineCount(currentFileInsertData.toString()) >= insertSplitSize) ||
            					((i + 1) == insertStatementsSplit.length)) {

            				final StringBuilder sqlOutput = new StringBuilder();
            				sqlOutput.append(header);
            	            sqlOutput.append(currentFileInsertData);
            	            sqlOutput.append(printSQLFooter(false));
            	            sqlFiles.add(sqlOutput.toString());

            	            currentFileInsertData = new StringBuilder();
            			}
            		}

            		//Create file that only runs PLSQL procedure at end.
            		final StringBuilder sqlOutput = new StringBuilder();
            		sqlOutput.append(header);
    	            sqlOutput.append(footer);
    	            sqlFiles.add(sqlOutput.toString());
            	}
            }


        } catch (final Exception e) {
            throw new ApplicationException(e);
        }

        return sqlFiles;
    }

    /**
     * This method processes a single value lookup.
     *
     * Processes anElement / the value and writes necessary values to the insert/update/error
     * writers.
     *
     * @param anElement The element being processed
     * @param value the value of the element being processed
     * @param insertStatement the StringBuilder to write the portion of the query to be used within an insert statement
     * @param updateStatement the StringBulider to write the portion of the query to be used within an update statement
     * @throws ClassNotFoundException If there is an underlying {@link ClassNotFoundException}
     * @throws IllegalAccessException If there is an underlying {@link IllegalAccessException}
     * @throws InstantiationException If there is an underlying {@link InstantiationException}
     * @throws ApplicationException If a problem is encountered
     */
    private void handleSingleValueLookup (final AeaEtkDataElement anElement,
            final Object value,
            final StringBuilder insertStatement,
            final StringBuilder updateStatement) throws ApplicationException,
                                                        InstantiationException,
                                                        IllegalAccessException,
                                                        ClassNotFoundException {

        final AeaEtkLookupDefinition lookupDef = anElement.getEtkLookupDefinition();
        final String lookupName = lookupDef.getName();
        final String columnName = anElement.getColumnName();


        if (value == null) {
            insertStatement.append("null,");

            //If this is the c_code column, don't put an update statement in the record.
            if (!"c_code".equalsIgnoreCase(columnName)) {
                updateStatement.append(columnName);
                updateStatement.append(" = null,");
            }

            return;
        }

        //These statements attempt to determine what table the lookup definition is actually
        //pointed to and what column it returned for "VALUE".
        final Map<String, String> rDataTableAndValueColumn =
                RdoDataExportUtility.getRDataTableAndValueColumn (fullyQualifiedScriptNames, etk, lookupDef);
        final String rDataTableName = rDataTableAndValueColumn.get("R_DATA_TABLE");
        final String rDataValueColumn = rDataTableAndValueColumn.get("R_DATA_VALUE_COLUMN");

        if (writeDebug) {
            Utility.aeaLog(etk, "Lookup = " + lookupName);
            Utility.aeaLog(etk, "rDataTableName = " + rDataTableName);
            Utility.aeaLog(etk, "rDataValueColumn = " + rDataValueColumn);
        }

        //Determine if there is a filter for this R-Data data set in RDO Table
        //"AEA RDO Data Export Config". This is necessary for things like ETK_DATA_OBJECT
        //where you need to filter by max(trackingConfigId)
        final String additonalFilterCriteria = getAdditionalFilterCriteria(rDataTableName);

        //Determine if there is a set of UID columns for this R-Data data set in RDO Table
        //"AEA RDO Data Export Config". This is necessary for things like ETK_DATA_OBJECT
        //where you need to get values by BUSINESS_KEY, not by C_CODE.
        final List<Map<String, Object>> lookupTableUIDColumns = getLookupTableUIDColumns
                (rDataTableName);

        if (writeDebug) {
            Utility.aeaLog(etk, "additonalFilterCriteria = " + additonalFilterCriteria);
        }

        //Try and see if there were any configured values for this lookup in
        //AEA RDO Data Export Config. If not, see if the c_code column exists in the
        //table. If not, we are going to pass the hard DB value through and hope it maps
        //to something in the target DB!
        if (lookupTableUIDColumns.size() == 0) {
            try {
                if (Utility.isSqlServer(etk)) {
                    etk.createSQL("select top 1 c_code from " +
                            rDataTableName)
                            .returnEmptyResultSetAs(null)
                            .fetchObject();
                } else {
                    etk.createSQL("select c_code from " +
                            rDataTableName +
                            " where rownum < 2")
                            .returnEmptyResultSetAs(null)
                            .fetchObject();
                }

                final HashMap<String, Object> tmpMap = new HashMap<String, Object>();
                tmpMap.put("COLUMN_NAME", "C_CODE");
                lookupTableUIDColumns.add(tmpMap);

            } catch (final Exception e) {
                if (writeDebug) {
                    Utility.aeaLog(etk, currentlyExportingRdo + " - ERROR processing lookup \"" + lookupName
                    		+ "\" on column \"" + columnName + "\": Table " + rDataTableName + " does not have a "
                            + "C_CODE column or a configured value in "
                            + " AEA RDO Data Export Config. Setting to hard "
                            + "coded value.", e);
                }

                errorMessages.append("--" + currentlyExportingRdo + " - ERROR processing lookup \"" + lookupName
                		+ "\" on column \"" + columnName + "\": Table " + rDataTableName + " does not have a "
                        + "C_CODE column or a configured value in "
                        + " AEA RDO Data Export Config. Setting to hard "
                        + "coded value.");

                errorMessages.append(NEWLINE_CHAR);

                insertStatement.append(RdoDataExportUtility.valToSqlString(value));
                insertStatement.append(",");

                updateStatement.append(columnName);
                updateStatement.append(" = ");
                updateStatement.append(RdoDataExportUtility.valToSqlString(value));
                updateStatement.append(",");

                return;
            }

        }

        //Return the complete row of data from rDataTableName where
        //rDataValueColumn = value and any additional filter criteria have been applied.
        //There should be only a single row of data.
        final Map<String, Object> lookupTableRowData = getLookupTableRow (rDataTableName,
                rDataValueColumn,
                value,
                additonalFilterCriteria);

        if (exportAsOracle()) {
            insertStatement.append("(select (select ");
        } else {
            insertStatement.append("(select ");
        }

        insertStatement.append(rDataValueColumn);
        insertStatement.append(" from ");
        insertStatement.append(rDataTableName);

        String lookupTableWhereEqualsColumn;
        for (int i = 0; i < lookupTableUIDColumns.size(); i++) {
            lookupTableWhereEqualsColumn =
                    (String) lookupTableUIDColumns.get(i).get("COLUMN_NAME");

            if (i == 0) {
                insertStatement.append(" where ");
            } else {
                insertStatement.append(" and ");
            }

            insertStatement.append(lookupTableWhereEqualsColumn);
            insertStatement.append(" = ");
            insertStatement.append(
                    RdoDataExportUtility.valToSqlString(lookupTableRowData.get(lookupTableWhereEqualsColumn)));
        }

        if (StringUtility.isNotBlank(additonalFilterCriteria)) {
            insertStatement.append(" and ");
            insertStatement.append(additonalFilterCriteria);
        }

        if (exportAsOracle()) {
            insertStatement.append(") from dual),");
        } else {
            insertStatement.append("),");
        }

        //If this is the c_code column, don't put an update statement in the record.
        if (!"c_code".equalsIgnoreCase(columnName)) {
            updateStatement.append(columnName);
            updateStatement.append(" = ");
            updateStatement.append("(select ");
            updateStatement.append(rDataValueColumn);
            updateStatement.append(" from ");
            updateStatement.append(rDataTableName);

            for (int i = 0; i < lookupTableUIDColumns.size(); i++) {
                lookupTableWhereEqualsColumn =
                        (String) lookupTableUIDColumns.get(i).get("COLUMN_NAME");

                if (i == 0) {
                    updateStatement.append(" where ");
                } else {
                    updateStatement.append(" and ");
                }

                updateStatement.append(lookupTableWhereEqualsColumn);
                updateStatement.append(" = ");
                updateStatement.append(
                        RdoDataExportUtility.valToSqlString(lookupTableRowData.get(lookupTableWhereEqualsColumn)));
            }

            if (StringUtility.isNotBlank(additonalFilterCriteria)) {
                updateStatement.append(" and ");
                updateStatement.append(additonalFilterCriteria);
            }

            updateStatement.append("),");
        }
    }

    /**
     * This method processes a multi value (M-Data) lookup.
     *
     * Processes anElement / the value and writes necessary values to the insert/update/error
     * writers.
     *
     * @param anElement The data element being exported
     * @param rdoTableName The name of the table being exported
     * @param tableRowId The tracking id of the object being exported
     * @param tableRowCCode The c_code value of the row being exported
     * @param mTableDeleteStatements The StringBuilder to which the delete statements for the m table should be written
     * @param mTableInsertStatements The StringBuilder to which the insert statements for the m table should be written
     *
     * @throws ClassNotFoundException If there was an underlying {@link ClassNotFoundException}
     * @throws IllegalAccessException If there was an underlying {@link IllegalAccessException}
     * @throws InstantiationException If there was an underlying {@link InstantiationException}
     * @throws ApplicationException If a problem was encountered
     */
    private void handleMDataLookup (final AeaEtkDataElement anElement,
            final String rdoTableName,
            final Object tableRowId,
            final Object tableRowCCode,
            final StringBuilder mTableDeleteStatements,
            final StringBuilder mTableInsertStatements) throws ApplicationException,
                                                               InstantiationException,
                                                               IllegalAccessException,
                                                               ClassNotFoundException {

        final String mTableName = anElement.getmTableName();
        final AeaEtkLookupDefinition lookupDef = anElement.getEtkLookupDefinition();
        final String lookupName = lookupDef.getName();
        final String columnName = anElement.getColumnName();

        mTableDeleteStatements.append("delete from ");
        mTableDeleteStatements.append(mTableName);
        mTableDeleteStatements.append(" where id_owner = (select id from ");
        mTableDeleteStatements.append(rdoTableName);

        if (exportAsOracle()) {
            mTableDeleteStatements.append(" where c_code = tempCode);");
        } else {
            mTableDeleteStatements.append(" where c_code = @tempCode);");
        }

        mTableDeleteStatements.append(NEWLINE_CHAR);

        final List<Map<String, Object>> mDataRowList =
                etk.createSQL("select * from " + mTableName + " where id_owner = :idOwner")
                        .returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
                        .setParameter("idOwner", tableRowId)
                        .fetchList();

		StringBuilder anMTableInsert;

		mTableInsertStatementLoop:
        for(final Map<String, Object> aMDataRow : mDataRowList) {
			anMTableInsert = new StringBuilder();

			anMTableInsert.append("insert into ");
			anMTableInsert.append(mTableName);

            if (exportAsOracle()) {
				anMTableInsert.append("(ID, ID_OWNER, LIST_ORDER, ");
            } else {
				anMTableInsert.append("(ID_OWNER, LIST_ORDER, ");
            }

			anMTableInsert.append(columnName);

            if (exportAsOracle()) {
				anMTableInsert.append(") values (object_id.nextval, (select id from ");
            } else {
				anMTableInsert.append(") values ((select id from ");
            }

			anMTableInsert.append(rdoTableName);
            if (exportAsOracle()) {
				anMTableInsert.append(" where c_code = tempCode), ");
            } else {
				anMTableInsert.append(" where c_code = @tempCode), ");
            }

			anMTableInsert.append(aMDataRow.get("LIST_ORDER"));
			anMTableInsert.append(", ");

            final Object mDataValue = aMDataRow.get(columnName);

            if (mDataValue == null) {
				//This is an orphaned value, continue.
				continue;
            } else {
                //These statements attempt to determine what table the lookup definition is actually
                //pointed to and what column it returned for "VALUE".
                final Map<String, String> rDataTableAndValueColumn =
                        RdoDataExportUtility.getRDataTableAndValueColumn (fullyQualifiedScriptNames, etk, lookupDef);
                final String rDataTableName = rDataTableAndValueColumn.get("R_DATA_TABLE");
                final String rDataValueColumn = rDataTableAndValueColumn.get("R_DATA_VALUE_COLUMN");

                if (writeDebug) {
                    Utility.aeaLog(etk, "M TABLE DATA=" + mTableName
                            + " || value =" + mDataValue
                            + " || rDataTableName=" + rDataTableName
                            + " || rDataValueColumn=" + rDataValueColumn);
                }

                //Determine if there is a filter for this R-Data data set in RDO Table
                //"AEA RDO Data Export Config". This is necessary for things like ETK_DATA_OBJECT
                //where you need to filter by max(trackingConfigId)
                final String additonalFilterCriteria = getAdditionalFilterCriteria(rDataTableName);

                if (writeDebug) {
                    Utility.aeaLog(etk, "additonalFilterCriteria = " + additonalFilterCriteria);
                }

                //Determine if there is a set of UID columns for this R-Data data set in RDO Table
                //"AEA RDO Data Export Config". This is necessary for things like ETK_DATA_OBJECT
                //where you need to get values by BUSINESS_KEY, not by C_CODE.
                final List<Map<String, Object>> lookupTableUIDColumns = getLookupTableUIDColumns
                        (rDataTableName);


                //Try and see if there were any configured values for this lookup in
                //AEA RDO Data Export Config. If not, see if the c_code column exists in the
                //table. If not, we are going to pass the hard DB value through and hope it maps
                //to something in the target DB!
                if (lookupTableUIDColumns.size() == 0) {
                    try {
                        if (Utility.isSqlServer(etk)) {
                            etk.createSQL("select top 1 c_code from " +
                                    rDataTableName)
                                    .returnEmptyResultSetAs(null)
                                    .fetchObject();
                        } else {
                            etk.createSQL("select c_code from " +
                                    rDataTableName +
                                    " where rownum < 2")
                                    .returnEmptyResultSetAs(null)
                                    .fetchObject();
                        }

                        final HashMap<String, Object> tmpMap = new HashMap<String, Object>();
                        tmpMap.put("COLUMN_NAME", "C_CODE");
                        lookupTableUIDColumns.add(tmpMap);

                    } catch (final Exception e) {
                        if (writeDebug) {
                            Utility.aeaLog(etk, currentlyExportingRdo + " - ERROR processing lookup \"" + lookupName
                            		+ "\" on column \"" + columnName + "\": Table " + rDataTableName + " does not have a "
                                    + "C_CODE column or a configured value in "
                                    + " AEA RDO Data Export Config. Setting to hard "
                                    + "coded value.", e);
                        }

                        errorMessages.append("--" + currentlyExportingRdo + " - ERROR processing lookup \"" + lookupName
                        		+ "\" on column \"" + columnName + "\": Table " + rDataTableName + " does not have a "
                                + "C_CODE column or a configured value in "
                                + " AEA RDO Data Export Config. Setting to hard "
                                + "coded value.");

                        errorMessages.append(NEWLINE_CHAR);

						anMTableInsert.append(RdoDataExportUtility.valToSqlString(mDataValue));
						anMTableInsert.append(");");
						anMTableInsert.append(NEWLINE_CHAR);
						mTableInsertStatements.append(anMTableInsert.toString());
                        continue;
                    }
                }

                if (exportAsOracle()) {
					anMTableInsert.append("(select (select ");
                } else {
					anMTableInsert.append(" (select ");
                }

				anMTableInsert.append(rDataValueColumn);
				anMTableInsert.append(" from ");
				anMTableInsert.append(rDataTableName);

                //Return the complete row of data from rDataTableName where
                //rDataValueColumn = value and any additional filter criteria have been applied.
                //There should be only a single row of data.
                final Map<String, Object> lookupTableRowData = getLookupTableRow (rDataTableName,
                        rDataValueColumn,
                        mDataValue,
                        additonalFilterCriteria);

                String lookupTableWhereEqualsColumn;
                Object rowVal;
                for (int i = 0; i < lookupTableUIDColumns.size(); i++) {
                    lookupTableWhereEqualsColumn =
                            (String) lookupTableUIDColumns.get(i).get("COLUMN_NAME");

                    if (i == 0) {
						anMTableInsert.append(" where ");
                    } else {
						anMTableInsert.append(" and ");
                    }

					anMTableInsert.append(lookupTableWhereEqualsColumn);
					anMTableInsert.append(" = ");

					rowVal = lookupTableRowData.get(lookupTableWhereEqualsColumn);

					if ((lookupTableUIDColumns.size() == 1) && (rowVal == null)) {
						continue mTableInsertStatementLoop;
					}

					anMTableInsert.append(RdoDataExportUtility.valToSqlString(rowVal));
                }

                if (StringUtility.isNotBlank(additonalFilterCriteria)) {
					anMTableInsert.append(" and ");
					anMTableInsert.append(additonalFilterCriteria);
                }

                if (exportAsOracle()) {
					anMTableInsert.append(") from dual));");
                } else {
					anMTableInsert.append("));");
                }
            }

			anMTableInsert.append(NEWLINE_CHAR);
			mTableInsertStatements.append(anMTableInsert.toString());
        } //End M-Data row iterator
    }

    /**
     * Adds file storage declarations to the variableDeclares / variableInits and returns a
     * fileId# variable that can be placed into a PLSQL insert.
     *
     * @param variableDeclares StringBuilder to write the variable declarations for
     * @param variableInits StringBuilder to write the variable initialization (assignment) statements to
     * @param sqlServerFileInsertStatement The String Builder which this method will modify so that it contains the
     *          query necessary to insert files into a SQL Server database
     * @param fileCounter counter of how many  files have been processed
     * @param value The etk_file.id in the new system
     * @return fileId variable name.
     * @throws IncorrectResultSizeDataAccessException
     *      If there was an underlying {@link IncorrectResultSizeDataAccessException}
     * @throws ApplicationException If a problem is encountered
     */
    private String handleFile (final StringBuilder variableDeclares,
            final StringBuilder variableInits,
            final StringBuilder sqlServerFileInsertStatement,
            final int fileCounter,
            final Object value) throws IncorrectResultSizeDataAccessException, ApplicationException {

        final Map<String, Object> fileRecord = etk.createSQL(
                "select file_name, file_size, "
                        + "content_type, file_type, object_type, reference_id, content, "
                        + "file_extension from etk_file where id = :fileId")
                        .setParameter("fileId", value)
                        .returnEmptyResultSetAs(new HashMap<String, Object>())
                        .fetchMap();


        if (!exportAsOracle()) {
            sqlServerFileInsertStatement.append(NEWLINE_CHAR);
            sqlServerFileInsertStatement.append("insert into etk_file(file_name, file_size, ");
            sqlServerFileInsertStatement.append("content_type, file_type, object_type, content, file_extension) ");
            sqlServerFileInsertStatement.append("values ('");
            sqlServerFileInsertStatement.append(StringEscapeUtils.escapeSql((String) fileRecord.get("FILE_NAME")));
            sqlServerFileInsertStatement.append("',");
            sqlServerFileInsertStatement.append(fileRecord.get("FILE_SIZE"));
            sqlServerFileInsertStatement.append(",'");
            sqlServerFileInsertStatement.append(fileRecord.get("CONTENT_TYPE"));
            sqlServerFileInsertStatement.append("',");
            sqlServerFileInsertStatement.append(fileRecord.get("FILE_TYPE"));
            sqlServerFileInsertStatement.append(",'");
            sqlServerFileInsertStatement.append(fileRecord.get("OBJECT_TYPE"));
            sqlServerFileInsertStatement.append("',0x");
            sqlServerFileInsertStatement.append(new String(Hex.encodeHex((byte[]) fileRecord.get("CONTENT"))));
            sqlServerFileInsertStatement.append(",'");
            sqlServerFileInsertStatement.append(fileRecord.get("FILE_EXTENSION"));
            sqlServerFileInsertStatement.append("');");
            sqlServerFileInsertStatement.append(NEWLINE_CHAR);
            sqlServerFileInsertStatement.append(NEWLINE_CHAR);
            sqlServerFileInsertStatement.append("DECLARE @fileId");
            sqlServerFileInsertStatement.append(fileCounter);
            sqlServerFileInsertStatement.append(" int = SCOPE_IDENTITY();");
            sqlServerFileInsertStatement.append(NEWLINE_CHAR);

            return "@fileId" + fileCounter;
        }

        final String contentColumnDataType =
                etk.createSQL("select DATA_TYPE from USER_TAB_COLUMNS " //Oracle Specific
                        + "where table_name = 'ETK_FILE' and column_name = 'CONTENT'")
                        .returnEmptyResultSetAs("")
                        .fetchString();

        //This tool can only successfully export data in BLOB format. LONG RAW
        //exports are limited to 32kb in size.
        if ("BLOB".equalsIgnoreCase(contentColumnDataType) && !fileRecord.isEmpty()) {

            variableDeclares.append(NEWLINE_CHAR);
            variableDeclares.append("-- BEGIN DECLARES FOR FILE "
                    + fileRecord.get("FILE_NAME") +
                    " -- ");
            variableDeclares.append(NEWLINE_CHAR);

            final byte[] fileContent = (byte[]) fileRecord.get("CONTENT");
            final int maxVarcharLength = 32000;

            variableDeclares.append("file" + fileCounter + " BLOB; " + NEWLINE_CHAR);
            variableDeclares.append("fileId" + fileCounter + " NUMBER; " + NEWLINE_CHAR);

            variableInits.append(NEWLINE_CHAR);
            variableInits.append("-- BEGIN WRITING FILE "
                    + fileRecord.get("FILE_NAME") +
                    " -- ");
            variableInits.append(NEWLINE_CHAR);

            variableInits.append(
                    "DBMS_LOB.createtemporary(file" + fileCounter + ", FALSE);"
                            + NEWLINE_CHAR);

            variableInits.append(
                    "DBMS_LOB.open(file" + fileCounter + ", DBMS_LOB.lob_readwrite);"
                            + NEWLINE_CHAR);

            int filePartCounter = 0;
            final String fileContentBase64 = new String(Base64.encodeBase64(fileContent));
            for (int i=0; i < fileContentBase64.length(); i+=maxVarcharLength) {
                int copyLength = maxVarcharLength;
                if (i+maxVarcharLength>fileContentBase64.length()) {
                    copyLength = fileContentBase64.length() % maxVarcharLength;
                }

                final String filePartVariableName =
                        "file" + fileCounter + "Part" + filePartCounter;


                variableDeclares.append(filePartVariableName +
                        " VARCHAR2 ("+maxVarcharLength+") := '" +
                        fileContentBase64.substring(i, i+copyLength)+"';" +
                        NEWLINE_CHAR);


                variableInits.append(
                        "rawTempVar := utl_encode.base64_decode(UTL_RAW.CAST_TO_RAW(" +
                                filePartVariableName + "));" +
                                NEWLINE_CHAR);
                variableInits.append("DBMS_LOB.writeappend(file" + fileCounter +
                        ", utl_raw.length(rawTempVar), rawTempVar);" +
                        NEWLINE_CHAR);

                filePartCounter++;
            }


            variableDeclares.append(NEWLINE_CHAR);
            variableDeclares.append("-- END DECLARES FOR FILE "
                    + fileRecord.get("FILE_NAME") +
                    " -- ");
            variableDeclares.append(NEWLINE_CHAR);


            variableInits.append("select OBJECT_ID.nextval into fileId"
                    + fileCounter + " from dual;" + NEWLINE_CHAR);

            variableInits.append("insert into etk_file(id, file_name, file_size, "
                    + "content_type, file_type, object_type, content, file_extension) "
                    + "values (fileId" + fileCounter + ", '" +
                    StringEscapeUtils.escapeSql((String) fileRecord.get("FILE_NAME")) +
                    "', " + fileRecord.get("FILE_SIZE")
                    +",'" + fileRecord.get("CONTENT_TYPE")
                    + "', "+ fileRecord.get("FILE_TYPE")
                    +",'"  + fileRecord.get("OBJECT_TYPE")
                    +"', file" + fileCounter
                    +",'" +fileRecord.get("FILE_EXTENSION")+ "'); ");

            variableInits.append(NEWLINE_CHAR);
            variableInits.append("-- END WRITING FILE "
                    + fileRecord.get("FILE_NAME") +
                    " -- ");
            variableInits.append(NEWLINE_CHAR);
        } else if (!fileRecord.isEmpty()) {

        	final int maxContentLength = 32768;

            if (((byte[]) fileRecord.get("CONTENT")).length > maxContentLength) {
                final String errorMessage =
                		"--" + currentlyExportingRdo + " - ERROR: FILE '" + fileRecord.get("FILE_NAME")
                        + "' > 32768 bytes, "
                        + "you must convert ETK_FILE.CONTENT to BLOB "
                        + "type column to allow for export. --";
                errorMessages.append(errorMessage);
                errorMessages.append(NEWLINE_CHAR);

                throw new ApplicationException (errorMessage);
            }

            variableDeclares.append("fileId" + fileCounter + " NUMBER; " + NEWLINE_CHAR);

            final String fileContent =
                    new String(
                            Hex.encodeHex((byte[]) fileRecord.get("CONTENT"))).toUpperCase();

            variableInits.append(NEWLINE_CHAR);
            variableInits.append("-- BEGIN WRITING FILE "
                    + fileRecord.get("FILE_NAME") +
                    " -- ");
            variableInits.append(NEWLINE_CHAR);

            for (int i = 0; i < fileContent.length(); i+=RdoDataExportUtility.ORACLE_MAX_CHAR_STRING_LENGTH) {
                int endPosition = i + RdoDataExportUtility.ORACLE_MAX_CHAR_STRING_LENGTH;

                if (endPosition > fileContent.length()) {
                    endPosition = fileContent.length();
                }

                if (i == 0) {
                    variableInits.append("longRawTempVar := '");
                    variableInits.append(fileContent.substring(i, endPosition));
                    variableInits.append("';");
                } else {
                    variableInits.append("longRawTempVar := longRawTempVar || '");
                    variableInits.append(fileContent.substring(i, endPosition));
                    variableInits.append("';");
                }

                variableInits.append(NEWLINE_CHAR);
            }

            variableInits.append("select OBJECT_ID.nextval into fileId"
                    + fileCounter + " from dual;" + NEWLINE_CHAR);

            variableInits.append("insert into etk_file(id, file_name, file_size, "
                    + "content_type, file_type, object_type, content, file_extension) "
                    + "values (fileId" + fileCounter + ", '" +
                    StringEscapeUtils.escapeSql((String) fileRecord.get("FILE_NAME")) +
                    "', " + fileRecord.get("FILE_SIZE")
                    +",'" + fileRecord.get("CONTENT_TYPE")
                    + "', "+ fileRecord.get("FILE_TYPE")
                    +",'"  + fileRecord.get("OBJECT_TYPE")
                    +"', longRawTempVar"
                    +", '" + fileRecord.get("FILE_EXTENSION")+ "'); ");

            variableInits.append(NEWLINE_CHAR);
            variableInits.append("-- END WRITING FILE "
                    + fileRecord.get("FILE_NAME") +
                    " -- ");
            variableInits.append(NEWLINE_CHAR);
        }

        return "fileId" + fileCounter;
    }

    /**
     * Helper method to determine whether to export as Oracle or SQLServer SQL syntax.
     *
     * @return Whether to export as Oracle or SQLServer SQL syntax.
     */
    private boolean exportAsOracle() {
    	return databasePlatform == DatabasePlatform.ORACLE;
    }

    /**
     * Prints the header of the SQL File output.
     *
     * @param escapedErrors Error messages at the top of the file.
     * @param variableDeclares Declared variables.
     * @param variableInits Initialized variables.
     * @return SQL Header String.
     */
    private String printSQLHeader (final String escapedErrors,
    							   final String variableDeclares,
    							   final String variableInits) {
    	 final StringBuilder sqlOutput = new StringBuilder();

         if (exportAsOracle()) {
         	if (this.plsqlStandalongSyntax) {
	                sqlOutput.append("SET DEFINE OFF");
	                sqlOutput.append(NEWLINE_CHAR);
	                sqlOutput.append("SET SERVEROUTPUT ON");
	                sqlOutput.append(NEWLINE_CHAR);
         	} else {
         		sqlOutput.append("BEGIN");
	                sqlOutput.append(NEWLINE_CHAR);
         	}

             sqlOutput.append(escapedErrors);
             sqlOutput.append(NEWLINE_CHAR);
             sqlOutput.append("declare tempCode VARCHAR2 (");
             sqlOutput.append(RdoDataExportUtility.ORACLE_MAX_CHAR_STRING_LENGTH);
             sqlOutput.append(");");

             sqlOutput.append(variableDeclares);

             sqlOutput.append(NEWLINE_CHAR);
             sqlOutput.append("begin");
             sqlOutput.append(NEWLINE_CHAR);
             sqlOutput.append(variableInits);
         } else {
             sqlOutput.append(NEWLINE_CHAR);
             sqlOutput.append(escapedErrors);
             sqlOutput.append(NEWLINE_CHAR);
             sqlOutput.append("declare @tempCode varchar(");
             sqlOutput.append(RdoDataExportUtility.ORACLE_MAX_CHAR_STRING_LENGTH);
             sqlOutput.append(");");
         }

         sqlOutput.append(NEWLINE_CHAR);

         return sqlOutput.toString();
    }

    /**
     * Prints PLSQL footer statements.
     *
     * @param executeFileReferenceIdProcedure Whether or not to execute the AEA_UPDATE_FILE_REFERENCE_ID procedure.
     * @return SQL Ending statements.
     */
    private String printSQLFooter(boolean executeFileReferenceIdProcedure) {
    	final StringBuilder sqlOutput = new StringBuilder();

    	if (executeFileReferenceIdProcedure) {
    		sqlOutput.append(NEWLINE_CHAR);

	        if (exportAsOracle()) {
	        	sqlOutput.append("AEA_UPDATE_FILE_REFERENCE_ID;");
	        } else {
	        	sqlOutput.append("EXEC AEA_UPDATE_FILE_REFERENCE_ID;");
	        }
    	}

        sqlOutput.append(NEWLINE_CHAR);


		if (exportAsOracle()) {
			sqlOutput.append(NEWLINE_CHAR);
			sqlOutput.append("END;");
			if (this.plsqlStandalongSyntax) {
				sqlOutput.append(NEWLINE_CHAR);
				sqlOutput.append("/");
				sqlOutput.append(NEWLINE_CHAR);
				sqlOutput.append("commit;");
			} else {
				sqlOutput.append(NEWLINE_CHAR);
				sqlOutput.append("END;");
			}
		}

		return sqlOutput.toString();
    }

    /**
     * Returns a line count of NEWLINE_CHAR in String.
     *
     * @param aString A String.
     * @return Count of NEWLINE_CHAR in string.
     */
    private int getLineCount (String aString) {
    	String[] lines = aString.split(NEWLINE_CHAR);
    	return  lines.length;
    }

    /**
	 * Returns the number of lines in an RDO export sql file packaged inside of the zip.
	 *
	 * @param etk The execution context.
	 * @return Ideal number of lines per SQL file.
	 */
	public static int getMaxLines(ExecutionContext etk) {
		int rdoExportMaxLines = 0;
		try {
        	rdoExportMaxLines =
        			etk.createSQL("select c_value from T_AEA_CORE_CONFIGURATION "
                               + "where c_code = 'dbutils.rdoExport.rdoExportMaxLines'")
                               .fetchInt();

        	if (rdoExportMaxLines < 0) {
        		rdoExportMaxLines = 0;
        	}
        } catch (Exception e) {
        	//Not a critical error, log nothing, we will just not paginate exported data.
        	rdoExportMaxLines = 0;
        }

		return rdoExportMaxLines;
	}
}
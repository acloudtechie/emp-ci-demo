/**
 *
 * Public RDO Data Exporter API
 *
 * aclee 03/20/2017
 **/

package net.entellitrak.aea.dbUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.platform.DatabasePlatform;

import net.micropact.aea.dbUtils.service.RdoExportLogic;
import net.micropact.aea.dbUtils.service.RdoImportLogic;

/**
 * Public API for the RDO Import/Export utilities.
 *
 * @author MicroPact
 *
 */
public final class RdoDataExporter {

	/**
	 * Private constructor.
	 */
	private RdoDataExporter() {
	}

	/**
	 * Export RDOs as SQL using the last saved set of selected tables / ordering
	 * in the AEA - DbUtils - RDO Data Export Utility. Note - this method will only
	 * export LONG_RAW files up to 32kb.
	 *
	 * @param etk The Execution context.
	 * @param databasePlatform Whether to export the SQL in Oracle or SqlServer syntax.
	 * @param printDebugToFile Whether to print detailed logging to the debug logs.
	 * @param usePlsqlStandaloneSyntax Whether or not to format the resulting SQL in a manner that
	 * can be run in SQLDeveloper. Use false if you intend to execute via etk.executeSQLFromScriptObject(),
	 * true if you intend to execute the output via SQLDeveloper.
	 *
	 * @return RDO data in SQL format.
	 * @throws ApplicationException Unexpected exception when exporting RDO data.
	 */
	public static String exportToSqlOneStageUsingLastSavedConfig(final ExecutionContext etk,
			final DatabasePlatform databasePlatform,
			final boolean printDebugToFile,
			final boolean usePlsqlStandaloneSyntax) throws ApplicationException {
		RdoExportLogic rel = new RdoExportLogic(etk, databasePlatform, printDebugToFile, usePlsqlStandaloneSyntax);
		return rel.exportToSqlOneStage();
	}

	/**
	 * Export RDOs as 2-part ZIP using the last saved set of selected tables / ordering
	 * in the AEA - DbUtils - RDO Data Export Utility.
	 *
	 * @param etk The Execution context.
	 * @param databasePlatform Whether to export the SQL in Oracle or SqlServer syntax.
	 * @param printDebugToFile Whether to print detailed logging to the debug logs.
	 * @param usePlsqlStandaloneSyntax Whether or not to format the resulting SQL in a manner that
	 * can be run in SQLDeveloper. Use false if you intend to execute via etk.executeSQLFromScriptObject(),
	 * true if you intend to execute the output via SQLDeveloper.
	 *
	 * @return RDO data in a ZIP file containing all exported files and the SQL to perform the import.
	 *
	 * @throws IOException Unexpected IOException when exporting RDO data.
	 * @throws ApplicationException Unexpected exception when exporting RDO data.
	 */
	public static InputStream exportToZipTwoStageUsingLastSavedConfig(final ExecutionContext etk,
			final DatabasePlatform databasePlatform,
			final boolean printDebugToFile,
			final boolean usePlsqlStandaloneSyntax) throws ApplicationException,
			IOException {
		RdoExportLogic rel = new RdoExportLogic(etk, databasePlatform, printDebugToFile, usePlsqlStandaloneSyntax);
		return rel.exportToZipTwoStage(RdoExportLogic.getMaxLines(etk));
	}

	/**
	 * Export RDOs as 2-part ZIP using the last saved set of selected tables / ordering
	 * in the AEA - DbUtils - RDO Data Export Utility.
	 *
	 * @param etk The Execution context.
	 * @param databasePlatform Whether to export the SQL in Oracle or SqlServer syntax.
	 * @param printDebugToFile Whether to print detailed logging to the debug logs.
	 * @param usePlsqlStandaloneSyntax Whether or not to format the resulting SQL in a manner that
	 * @param maxFileLength The maximum size of the PLSQL DB insert script.
	 * can be run in SQLDeveloper. Use false if you intend to execute via etk.executeSQLFromScriptObject(),
	 * true if you intend to execute the output via SQLDeveloper.
	 *
	 * @return RDO data in a ZIP file containing all exported files and the SQL to perform the import.
	 *
	 * @throws IOException Unexpected IOException when exporting RDO data.
	 * @throws ApplicationException Unexpected exception when exporting RDO data.
	 */
	public static InputStream exportToZipTwoStageUsingLastSavedConfig(final ExecutionContext etk,
			final DatabasePlatform databasePlatform,
			final boolean printDebugToFile,
			final boolean usePlsqlStandaloneSyntax,
			final int maxFileLength) throws ApplicationException,
			IOException {
		RdoExportLogic rel = new RdoExportLogic(etk, databasePlatform, printDebugToFile, usePlsqlStandaloneSyntax);
		return rel.exportToZipTwoStage(maxFileLength);
	}



	/**
	 * Export RDOs as SQL using an explicitly provided set of tables.
	 * Note - this method will only export LONG_RAW files up to 32kb.
	 *
	 * @param etk The Execution context.
	 * @param databasePlatform Whether to export the SQL in Oracle or SqlServer syntax.
	 * @param tableExportList The list of T_TABLE (String) / Order (Integer) to export.
	 * @param printDebugToFile Whether to print detailed logging to the debug logs.
	 * @param usePlsqlStandaloneSyntax Whether or not to format the resulting SQL in a manner that
	 * can be run in SQLDeveloper. Use false if you intend to execute via etk.executeSQLFromScriptObject(),
	 * true if you intend to execute the output via SQLDeveloper.
	 *
	 * @return RDO data in SQL format.
	 *
	 * @throws ApplicationException Unexpected exception when exporting RDO data.
	 */
	public static String exportToSqlOneStage(final ExecutionContext etk,
			final DatabasePlatform databasePlatform,
			final Map<String, Integer> tableExportList,
			final boolean printDebugToFile,
			final boolean usePlsqlStandaloneSyntax) throws ApplicationException {
		RdoExportLogic rel = new RdoExportLogic(etk, databasePlatform, printDebugToFile, usePlsqlStandaloneSyntax);
		return rel.exportToSqlOneStage(tableExportList);
	}

	/**
	 * Export RDOs as 2-part ZIP using an explicit set of tables and table orders passed in the
	 * tableExportList.
	 *
	 * @param etk The Execution context.
	 * @param databasePlatform Whether to export the SQL in Oracle or SqlServer syntax.
	 * @param tableExportList The list of T_TABLE (String) / Order (Integer) to export.
	 * @param printDebugToFile Whether to print detailed logging to the debug logs.
	 * @param usePlsqlStandaloneSyntax Whether or not to format the resulting SQL in a manner that
	 * can be run in SQLDeveloper. Use false if you intend to execute via etk.executeSQLFromScriptObject(),
	 * true if you intend to execute the output via SQLDeveloper.
	 *
	 * @return RDO data in a ZIP file containing all exported files and the SQL to perform the import.
	 *
	 * @throws IOException Unexpected IOException when exporting RDO data.
	 * @throws ApplicationException Unexpected exception when exporting RDO data.
	 */
	public static InputStream exportToZipTwoStage(final ExecutionContext etk,
			final DatabasePlatform databasePlatform,
			final Map<String, Integer> tableExportList,
			final boolean printDebugToFile,
			final boolean usePlsqlStandaloneSyntax) throws ApplicationException,
			IOException {
		RdoExportLogic rel = new RdoExportLogic(etk, databasePlatform, printDebugToFile, usePlsqlStandaloneSyntax);
		return rel.exportToZipTwoStage(tableExportList, RdoExportLogic.getMaxLines(etk));
	}


	/**
	 * Export RDOs as 2-part ZIP using an explicit set of tables and table orders passed in the
	 * tableExportList.
	 *
	 * @param etk The Execution context.
	 * @param databasePlatform Whether to export the SQL in Oracle or SqlServer syntax.
	 * @param tableExportList The list of T_TABLE (String) / Order (Integer) to export.
	 * @param printDebugToFile Whether to print detailed logging to the debug logs.
	 * @param usePlsqlStandaloneSyntax Whether or not to format the resulting SQL in a manner that
	 * @param maxFileLength The maximum size of the PLSQL DB insert script.
	 * can be run in SQLDeveloper. Use false if you intend to execute via etk.executeSQLFromScriptObject(),
	 * true if you intend to execute the output via SQLDeveloper.
	 *
	 * @return RDO data in a ZIP file containing all exported files and the SQL to perform the import.
	 *
	 * @throws IOException Unexpected IOException when exporting RDO data.
	 * @throws ApplicationException Unexpected exception when exporting RDO data.
	 */
	public static InputStream exportToZipTwoStage(final ExecutionContext etk,
			final DatabasePlatform databasePlatform,
			final Map<String, Integer> tableExportList,
			final boolean printDebugToFile,
			final boolean usePlsqlStandaloneSyntax,
			final int maxFileLength) throws ApplicationException,
			IOException {
		RdoExportLogic rel = new RdoExportLogic(etk, databasePlatform, printDebugToFile, usePlsqlStandaloneSyntax);
		return rel.exportToZipTwoStage(tableExportList, maxFileLength);
	}

	/**
	 * Method to import an RDO ZIP created by the exportToZipTwoStage method.
	 *
	 * ZIP must be generated with usePlsqlStandaloneSyntax = false
	 *
	 * @param etk The execution context.
	 * @param inputStream Input stream containing the RDO zip.
	 * @return String report detailing status of import.
	 *
	 * @throws IOException Unexpected IOexception when exporting RDO data.
	 * @throws ApplicationException Unexpected exception when exporting RDO data.
	 */
	public static String importTwoPartRdoExportZip (final ExecutionContext etk, final InputStream inputStream)
			throws IOException, ApplicationException {
		return RdoImportLogic.importTwoPartRdoZipFile(etk, inputStream);
	}

}

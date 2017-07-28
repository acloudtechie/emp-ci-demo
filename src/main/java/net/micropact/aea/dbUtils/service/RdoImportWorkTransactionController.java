package net.micropact.aea.dbUtils.service;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.Work;
import com.entellitrak.configuration.Script;

import net.micropact.aea.core.ioUtility.IOUtility;
import net.micropact.aea.core.utility.WorkspaceService;
import net.micropact.aea.utility.Utility;

public class RdoImportWorkTransactionController implements Work {
	private static final String NEWLINE_CHAR = "\n";
	private final StringBuilder sb;
	final InputStream inputStream;

	public RdoImportWorkTransactionController(final StringBuilder sb,
			final InputStream inputStream) {
		this.sb = sb;
		this.inputStream = inputStream;
	}

	@Override
	public void execute(ExecutionContext etk) throws ApplicationException {

		ZipInputStream etkZipFileReader = null;
		HashMap<String, String> sqlToImport = new HashMap<String, String>();

		try {
			etkZipFileReader = new ZipInputStream(inputStream);

			ZipEntry entry;

			while ((entry = etkZipFileReader.getNextEntry()) != null) {
				if ("ETK_FILE_DATA.zip".equals(entry.getName())) {
					sb.append("Processing ETK_FILE_DATA.zip");
					sb.append(NEWLINE_CHAR);

					sb.append(RdoImportLogic.uploadInternalZipOfFilesForTwoPartTransfer(etk,
							new CloseShieldInputStream(etkZipFileReader)));

					sb.append(NEWLINE_CHAR);
				} else if (entry.getName().startsWith("database_inserts_")) {
					sb.append("Extracting " + entry.getName());
					sb.append(NEWLINE_CHAR);
					sqlToImport.put(entry.getName(), IOUtils.toString(etkZipFileReader, "UTF-8"));
				}
			}
		} catch (Exception e) {

			Utility.aeaLog(etk, "Error importing ETK_FILE data." + NEWLINE_CHAR + sb.toString());

			throw new ApplicationException (e);
		} finally {
			IOUtility.closeQuietly(etkZipFileReader);
			IOUtility.closeQuietly(inputStream);
		}

		sb.append(NEWLINE_CHAR);
		sb.append("Beginning SQL Import");
		sb.append(NEWLINE_CHAR);


		Long scriptId = null;
		int errorCount = 0;

		Collection<Script> scripts =
			etk.getWorkspaceService().getScripts(etk.getWorkspaceService().getActiveWorkspace());

		for (Script aScript : scripts) {
			if ("net.micropact.aea.dbUtils.temp.TemporarySqlForImport"
					.equalsIgnoreCase(aScript.getFullyQualifiedName())) {
				scriptId = new Long(aScript.getId());
				break;
			}
		}

		if (scriptId == null) {
			sb.append("Could not file file net.micropact.aea.dbUtils.temp.TemporarySqlForImport in working repository. "
					+ "Make sure you have checked out the latest component code from the system repository.");
			errorCount++;
		} else {
			for (int i = 0; i < sqlToImport.size(); i++) {
				String fileName = "database_inserts_" + i + ".sql";
				sb.append("Processing file " + fileName);
				sb.append(NEWLINE_CHAR);

				etk.getLogger().error("RDO Data Importer - Uploading file " + fileName);


				//Cannot use SQL to update the TemporarySqlForImport script. executeSQLFromScriptObject will pull
				//from Hibernate cache on all but the first execution.
//				etk.createSQL(
//						"update etk_script_object "
//								+ " set code = :code, last_updated_on = " +
//								(Utility.isSqlServer(etk) ? "DBO.ETKF_GETSERVERTIME()" : "ETKF_GETSERVERTIME()")
//								+ " where script_id = :scriptId ")
//				.setParameter("code", sqlToImport.get(fileName))
//				.setParameter("scriptId", scriptId)
//				.execute();

				WorkspaceService.updateScriptObject(etk, scriptId, sqlToImport.get(fileName));

				try {
					etk.getLogger().error("RDO Data Importer - Begin execute file " + fileName);
					etk.executeSQLFromScriptObject("net.micropact.aea.dbUtils.temp.TemporarySqlForImport");
					etk.getLogger().error("RDO Data Importer - End execute file " + fileName + ", successfully imported.");
				} catch (Exception e) {
					errorCount++;
					sb.append("Error processing file " + fileName);
					sb.append(NEWLINE_CHAR);
					etk.getLogger().error("RDO Data Importer - error processing file " + fileName, e);
				}
			}
		}

		if (errorCount > 0) {
			throw new ApplicationException("Errors occurred while importing RDO data, rolling back.");
		}
	}
}

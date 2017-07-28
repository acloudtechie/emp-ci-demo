package net.micropact.aea.dbUtils.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataAccessException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

import net.micropact.aea.core.ioUtility.IOUtility;
import net.micropact.aea.core.utility.StringEscapeUtils;
import net.micropact.aea.utility.ImportExportUtility;
import net.micropact.aea.utility.Utility;

/**
 * Contains logic to import RDO data ZIP files (used by RDO Import v2.0 and public API).
 *
 * @author MicroPact
 *
 */
public final class RdoImportLogic {
	private static final String NEWLINE_CHAR = "\n";

	/**
	 * Private constructor.
	 */
	private RdoImportLogic() {
	}

	/**
	 * Helper method to import a ZIP file containing RDO SQL and attached files.
	 *
	 * @param etk Execution context.
	 * @param inputStream A ZIP file containing RDO SQL and attached files.
	 * @return Report containing information about the import job.
	 *
	 * @throws IOException Unexpected IOException.
	 * @throws ApplicationException Unexpected ApplicationException.
	 */
	public static String importTwoPartRdoZipFile (final ExecutionContext etk,
			final InputStream inputStream)
					throws IOException, ApplicationException {

		StringBuilder sb = new StringBuilder();

		try {
			RdoImportWorkTransactionController transaction = new RdoImportWorkTransactionController (sb, inputStream);

			etk.doWork(transaction);

			sb.append(NEWLINE_CHAR);
			sb.append("RDO data import completed successfully, please close window.");
		} catch (Exception e) {
			sb.append(NEWLINE_CHAR);
			sb.append("RDO data import failed, rolling back changes. "
					+ "Please review the application container's log files, resolve the reported "
					+ "errors and re-run the RDO import.");
			sb.append(NEWLINE_CHAR);

			etk.getLogger().error("RDO data import failed.", e);
		} finally {
			IOUtility.closeQuietly(inputStream);
		}

		sb.append(NEWLINE_CHAR);

		return sb.toString();
	}




	/**
	 * Perform an import of the user provided XML file.
	 *
	 * @param etk ExecutionContext.
	 * @param inputStream input stream containing the import file
	 * @return message indicating the result of the import.
	 * @throws IOException If there was an underlying {@link IOException}
	 * @throws ApplicationException If there was an underlying {@link ApplicationException}
	 */
	public static String uploadInternalZipOfFilesForTwoPartTransfer(final ExecutionContext etk,
			final InputStream inputStream)
					throws IOException, ApplicationException {
		StringBuilder sb = new StringBuilder();

		ZipInputStream etkZipFileReader = null;

		try {
			etk.createSQL("delete from T_AEA_RDO_FILE_STAGING").execute();

			sb.append("T_AEA_RDO_FILE_STAGING cleared, beginning import.");
			sb.append(NEWLINE_CHAR);
			sb.append(NEWLINE_CHAR);

			etkZipFileReader = new ZipInputStream(inputStream);

			final int bufferSize = 4096;
			byte[] buffer = new byte[bufferSize];
			ZipEntry entry;

			while ((entry = etkZipFileReader.getNextEntry()) != null) {

				sb.append("Processing ETK_FILE ZIP = " + StringEscapeUtils.escapeHtml(entry.getName()));
				sb.append(NEWLINE_CHAR);

				int len = 0;
				File tempFile = null;
				RandomAccessFile rm = null;
				ZipFile zf = null;

				try {
					tempFile = File.createTempFile(UUID.randomUUID().toString(), null);
					rm = new RandomAccessFile(tempFile, "rw");

					while ((len = etkZipFileReader.read(buffer)) > 0){
						rm.write(buffer, 0, len);
					}

					zf = new ZipFile(tempFile);
					importFile(etk, zf, sb);
				} finally {
					//Clean up the temp ZIP file.
					try {
						rm.close();
					} catch (IOException e) {
						throw e;
					} finally {
						tempFile.delete();
					}
				}
			}
		} catch (Exception e) {
			Utility.aeaLog(etk, "Error importing ETK_FILE data." + NEWLINE_CHAR + sb.toString());

			throw new ApplicationException (e);
		} finally {
			IOUtility.closeQuietly(etkZipFileReader);
			IOUtility.closeQuietly(inputStream);
		}

		return sb.toString();
	}


	/**
	 * Private utility method to import files attached to RDO records into T_AEA_RDO_FILE_STAGING.
	 *
	 * @param etk ExecutionContext.
	 * @param zf Zip file containing files attached to RDO records.
	 * @param sb String builder used to capture information for printed report on upload success.
	 *
	 * @throws DataAccessException Unexpected DataAccessException.
	 * @throws IncorrectResultSizeDataAccessException Unexpected IncorrectResultSizeDataAccessException.
	 * @throws SAXException Unexpected SAXException.
	 * @throws IOException Unexpected IOException.
	 * @throws ParserConfigurationException Unexpected ParserConfigurationException.
	 * @throws FactoryConfigurationError Unexpected FactoryConfigurationError.
	 */
	private static void importFile(final ExecutionContext etk, final ZipFile zf, final StringBuilder sb)
			throws DataAccessException, IncorrectResultSizeDataAccessException,
			SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {

		final Document document = DocumentBuilderFactory
				.newInstance()
				.newDocumentBuilder()
				.parse(new InputSource(new InputStreamReader(zf.getInputStream(zf.getEntry("ETK_FILE_METADATA.xml")))));

		final List<Map<String, String>> fileData =
				ImportExportUtility.getTable(document, "ETK_FILE");

		String fileName = "";
		Long newFileId;
		Long oldFileId;

		for(final Map<String, String> etkFile : fileData){

			oldFileId = new Long(etkFile.get("ID"));
			fileName = etkFile.get("FILE_NAME");

			if (Utility.isSqlServer(etk)) {
				newFileId = new Long (
						etk.createSQL("INSERT INTO ETK_FILE "
								+ "(FILE_NAME, FILE_SIZE, CONTENT_TYPE, FILE_TYPE, FILE_EXTENSION, "
								+ "OBJECT_TYPE, RESOURCE_PATH, ETK_DM_RESOURCE_ID, "
								+ "TOKEN, TIME_REQUESTED) VALUES "
								+ "(:FILE_NAME, :FILE_SIZE, :CONTENT_TYPE, :FILE_TYPE, :FILE_EXTENSION, "
								+ ":OBJECT_TYPE, :RESOURCE_PATH, :ETK_DM_RESOURCE_ID, "
								+ ":TOKEN, :TIME_REQUESTED)")
						.setParameter("FILE_NAME", fileName)
						.setParameter("FILE_SIZE", etkFile.get("FILE_SIZE"))
						.setParameter("CONTENT_TYPE", etkFile.get("CONTENT_TYPE"))
						.setParameter("FILE_TYPE", etkFile.get("FILE_TYPE"))
						.setParameter("FILE_EXTENSION", etkFile.get("FILE_EXTENSION"))
						.setParameter("OBJECT_TYPE", etkFile.get("OBJECT_TYPE"))
						.setParameter("RESOURCE_PATH", etkFile.get("RESOURCE_PATH"))
						.setParameter("ETK_DM_RESOURCE_ID", etkFile.get("ETK_DM_RESOURCE_ID"))
						.setParameter("TOKEN", etkFile.get("TOKEN"))
						.setParameter("TIME_REQUESTED", etkFile.get("TIME_REQUESTED"))
						.executeForKey("ID"));


			} else {
				newFileId =
						((BigDecimal) etk.createSQL("select object_id.nextval from dual").fetchObject()).longValue();

				etk.createSQL("INSERT INTO ETK_FILE "
						+ "(ID, FILE_NAME, FILE_SIZE, CONTENT_TYPE, FILE_TYPE, FILE_EXTENSION, "
						+ "OBJECT_TYPE, RESOURCE_PATH, ETK_DM_RESOURCE_ID, "
						+ "TOKEN, TIME_REQUESTED) VALUES "
						+ "(:ID, :FILE_NAME, :FILE_SIZE, :CONTENT_TYPE, :FILE_TYPE, :FILE_EXTENSION, "
						+ ":OBJECT_TYPE, :RESOURCE_PATH, :ETK_DM_RESOURCE_ID, "
						+ ":TOKEN, :TIME_REQUESTED)")
				.setParameter("ID", newFileId)
				.setParameter("FILE_NAME", fileName)
				.setParameter("FILE_SIZE", etkFile.get("FILE_SIZE"))
				.setParameter("CONTENT_TYPE", etkFile.get("CONTENT_TYPE"))
				.setParameter("FILE_TYPE", etkFile.get("FILE_TYPE"))
				.setParameter("FILE_EXTENSION", etkFile.get("FILE_EXTENSION"))
				.setParameter("OBJECT_TYPE", etkFile.get("OBJECT_TYPE"))
				.setParameter("RESOURCE_PATH", etkFile.get("RESOURCE_PATH"))
				.setParameter("ETK_DM_RESOURCE_ID", etkFile.get("ETK_DM_RESOURCE_ID"))
				.setParameter("TOKEN", etkFile.get("TOKEN"))
				.setParameter("TIME_REQUESTED", etkFile.get("TIME_REQUESTED"))
				.execute();
			}

			com.entellitrak.file.File uploadedFile = etk.getFileService().get(newFileId);
			uploadedFile.setContent(zf.getInputStream(zf.getEntry(fileName)));
			etk.getFileService().update(uploadedFile);

			if (Utility.isSqlServer(etk)) {
				etk.createSQL(
						"insert into T_AEA_RDO_FILE_STAGING (C_CODE, C_ETK_FILE_ID, C_SOURCE_SYSTEM_ID, C_NAME) "
								+ "VALUES (:code, :newFileID, :oldFileId, :fileName) ")
				.setParameter("code", oldFileId + "_" + fileName)
				.setParameter("newFileID", newFileId)
				.setParameter("oldFileId", oldFileId)
				.setParameter("fileName", fileName)
				.execute();
			} else {
				etk.createSQL(
						"insert into T_AEA_RDO_FILE_STAGING (ID, C_CODE, C_ETK_FILE_ID, C_SOURCE_SYSTEM_ID, C_NAME) "
								+ "VALUES (object_id.nextval, :code, :newFileID, :oldFileId, :fileName) ")
				.setParameter("code", oldFileId + "_" + fileName)
				.setParameter("newFileID", newFileId)
				.setParameter("oldFileId", oldFileId)
				.setParameter("fileName", fileName)
				.execute();
			}

			sb.append("Successfully imported ETK_FILE with name = \"");
			sb.append(StringEscapeUtils.escapeHtml(fileName));
			sb.append("\", Source ETK_FILE ID = \"");
			sb.append(oldFileId);
			sb.append("\", New ETK_FILE ID = \"");
			sb.append(newFileId);
			sb.append("\"");
			sb.append(NEWLINE_CHAR);
			sb.append(NEWLINE_CHAR);
		}
	}
}

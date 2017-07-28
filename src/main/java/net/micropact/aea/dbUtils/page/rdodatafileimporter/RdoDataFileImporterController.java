package net.micropact.aea.dbUtils.page.rdodatafileimporter;

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
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.FileStream;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.ioUtility.IOUtility;
import net.micropact.aea.core.utility.StringEscapeUtils;
import net.micropact.aea.utility.ImportExportUtility;
import net.micropact.aea.utility.Utility;

/**
 * PDF Template File Import Tool.
 *
 * @author aclee
 */
public class RdoDataFileImporterController implements PageController {

	private PageExecutionContext etk;
	private StringBuilder sb = new StringBuilder();

	/**
	 * Newline character.
	 */
	private static final String NEWLINE = "<br>";

    @Override
    public Response execute(final PageExecutionContext theEtk)
            throws ApplicationException {

        try {
        	this.etk = theEtk;

            final TextResponse response = etk.createTextResponse();

            final FileStream fileStream = etk.getParameters().getFile("importFile");

            if(fileStream != null){
            	etk.createSQL("delete from T_AEA_RDO_FILE_STAGING").execute();

            	sb.append("T_AEA_RDO_FILE_STAGING cleared, beginning import.");
            	sb.append(NEWLINE);
            	sb.append(NEWLINE);

                performImport(fileStream.getInputStream());

                sb.append(NEWLINE);
                sb.append("The ETK_FILE_DATA import completed successfully, please close window.");

                response.put("result", sb.toString());
            }

            return response;
        } catch (final IOException e) {
            throw new ApplicationException(e);
        } catch (DataAccessException e) {
        	 throw new ApplicationException(e);
		}
    }

    /**
     * Perform import of files attached to RDOs by parsing XML inputStream and storing the
     * results in T_AEA_RDO_FILE_STAGING.
     *
     * @param inputStream XML inputStream.
     * @throws IOException Unexpected IOException.
     * @throws ApplicationException Unexpected ApplicationException.
     */
    private void performImport(final InputStream inputStream) throws IOException, ApplicationException {

    	File tempFile = File.createTempFile(UUID.randomUUID().toString(), null);
    	RandomAccessFile rm = new RandomAccessFile(tempFile, "rw");
    	ZipInputStream etkZipFileReader = null;

    	try {
    		etkZipFileReader = new ZipInputStream(inputStream);

    		final int bufferSize = 4096;
	    	byte[] buffer = new byte[bufferSize];
	    	ZipEntry entry;

	        while ((entry = etkZipFileReader.getNextEntry()) != null) {

	           sb.append("Processing ETK_FILE ZIP = " + StringEscapeUtils.escapeHtml(entry.getName()));
	           sb.append(NEWLINE);

	           int len = 0;
	           while ((len = etkZipFileReader.read(buffer)) > 0){
	        	   rm.write(buffer, 0, len);
	           }

	           ZipFile zf = new ZipFile(tempFile);
	           importFile(zf);

	           rm.seek(0);
	           rm.setLength(0);
	       }
    	} catch (Exception e) {

    		Utility.aeaLog(etk, "Error importing ETK_FILE data." + NEWLINE + sb.toString());

    		throw new ApplicationException (e);
    	} finally {

    		//Clean up the temp ZIP file.
    		try {
    			rm.close();
    		} catch (IOException e) {
    			throw e;
    		} finally {
    			tempFile.delete();
    		}

    		IOUtility.closeQuietly(etkZipFileReader);
    		IOUtility.closeQuietly(inputStream);
    	}
    }


    /**
     * Parses ZipFile containing ETK_FILE_METADATA and stores file data in T_AEA_RDO_FILE_STAGING / ETK_FILE.
     *
     * @param zf Zip file.
     *
     * @throws DataAccessException Unexpected DataAccessException parsing Zip File.
     * @throws IncorrectResultSizeDataAccessException Unexpected IncorrectResultSizeDataAccessException parsing Zip File.
     * @throws SAXException Unexpected SAXException parsing Zip File.
     * @throws IOException Unexpected IOException parsing Zip File.
     * @throws ParserConfigurationException Unexpected ParserConfigurationException parsing Zip File.
     * @throws FactoryConfigurationError Unexpected FactoryConfigurationError parsing Zip File.
     */
    private void importFile(ZipFile zf)
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
	        sb.append(NEWLINE);
	        sb.append(NEWLINE);
        }
    }
}
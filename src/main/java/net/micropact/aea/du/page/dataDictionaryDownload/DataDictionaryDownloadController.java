package net.micropact.aea.du.page.dataDictionaryDownload;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;

import net.micropact.aea.core.data.CsvTools;
import net.micropact.aea.core.query.Coersion;
import net.micropact.aea.utility.DataElementRequiredLevel;
import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.DataObjectType;
import net.micropact.aea.utility.Utility;

/**
 * This class serves as the controller code for a page which can generate a Data Dictionary of the objects
 * and elements within entellitrak.
 *
 * @author zachary.miller
 */
public class DataDictionaryDownloadController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            return etk.createFileResponse("dataDictionary.csv", generateCsv(etk).getBytes());
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Generates a CSV file containing the data dictionary of the objects in the system.
     *
     * @param etk entellitrak execution context
     * @return The CSV file
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static String generateCsv(final ExecutionContext etk) throws IncorrectResultSizeDataAccessException {
        final List<Map<String, Object>> elementInfos = etk.createSQL("SELECT dataObject.label OBJECT_LABEL, dataObject.business_key OBJECT_BUSINESS_KEY, dataObject.description OBJECT_DESCRIPTION, dataObject.table_name OBJECT_TABLE, parentObject.business_key PARENT_BUSINESS_KEY, dataElement.name ELEMENT_NAME, dataElement.business_key ELEMENT_BUSINESS_KEY, dataElement.description ELEMENT_DESCRIPTION, dataElement.data_type DATA_TYPE, dataElement.required REQUIRED, lookup.name LOOKUP_NAME, lookup.business_key LOOKUP_BUSINESS_KEY, dataElement.column_name COLUMN_NAME, dataElement.table_name ELEMENT_TABLE FROM etk_data_object dataObject LEFT JOIN etk_data_object parentObject ON parentObject.data_object_id = dataObject.parent_object_id LEFT JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id LEFT JOIN etk_lookup_definition lookup ON lookup.lookup_definition_id = dataElement.lookup_definition_id WHERE dataObject.tracking_config_id = :trackingConfigId AND dataObject.object_type IN (:objectTypes) ORDER BY OBJECT_LABEL, OBJECT_BUSINESS_KEY, ELEMENT_NAME, ELEMENT_BUSINESS_KEY")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                .setParameter("objectTypes", Arrays.asList(DataObjectType.TRACKING.getEntellitrakNumber()))
                .fetchList();

        elementInfos
            .stream()
            .filter(elementInfo -> elementInfo.get("ELEMENT_BUSINESS_KEY") != null)
            .forEach(elementInfo -> {
                elementInfo.put("DATA_TYPE", DataElementType.getDataElementType(Coersion.toLong(elementInfo.get("DATA_TYPE"))).getEspIdentifier());

                elementInfo.put("REQUIRED", DataElementRequiredLevel.getDataElementRequiredLevel((String) elementInfo.get("REQUIRED")).getDisplay());
            });

        final String[][] csvConfiguration = new String[][]{
            {"Object Label", "OBJECT_LABEL"},
            {"Object Business Key", "OBJECT_BUSINESS_KEY"},
            {"Object Description", "OBJECT_DESCRIPTION"},
            {"Object Table", "OBJECT_TABLE"},
            {"Parent Object Business Key", "PARENT_BUSINESS_KEY"},
            {"Element Name", "ELEMENT_NAME"},
            {"Element Business Key", "ELEMENT_BUSINESS_KEY"},
            {"Element Column", "COLUMN_NAME"},
            {"Element Description", "ELEMENT_DESCRIPTION"},
            {"Required", "REQUIRED"},
            {"Data Type", "DATA_TYPE"},
            {"Element Table", "ELEMENT_TABLE"},
            {"Lookup Name", "LOOKUP_NAME"},
            {"Lookup Business Key", "LOOKUP_BUSINESS_KEY"}};

        final String[] headers = extractArrayColumn(csvConfiguration, 0);
        final String[] mapKeys = extractArrayColumn(csvConfiguration, 1);

        final List<List<String>> csvData = new LinkedList<>();

        final List<String> formattedHeaders = new LinkedList<>();
        for(final String header : headers){
            formattedHeaders.add(String.format("*%s*", header));
        }

        csvData.add(formattedHeaders);

        for(final Map<String, Object> elementInfo : elementInfos){
            final List<String> rowData = new LinkedList<>();

            for(final String key : mapKeys){
                rowData.add((String) elementInfo.get(key));
            }

            csvData.add(rowData);
        }

        return CsvTools.encodeCsv(csvData);
    }

    /**
     * Extracts a column from a 2-dimensional array where the "column" is the 2nd dimension.
     *
     * @param array two dimensional array to extract the colum out of. The column is the 2nd dimension
     * @param column The column to extract (zero-indexed)
     * @return The ordered column values
     */
    private static String[] extractArrayColumn(final String[][] array, final int column) {
        final String[] returnValue = new String[array.length];

        for(int i = 0; i < array.length; i++){
            returnValue[i] = array[i][column];
        }

        return returnValue;
    }
}

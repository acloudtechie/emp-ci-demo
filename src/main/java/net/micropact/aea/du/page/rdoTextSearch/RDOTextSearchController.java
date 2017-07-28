package net.micropact.aea.du.page.rdoTextSearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.query.EscapeLike;
import net.micropact.aea.core.query.QueryUtility;
import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.DataObjectType;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This class is the controller code for a page which will do a text-search of RDOs. It accepts search criteria and
 * returns search results.
 *
 * This class is very dynamically typed (uses Maps and Lists instead of other objects). This could be changed, but it
 * seemed like a lot of effort for what might not be much benefit.
 *
 * @author zmiller
 */
public class RDOTextSearchController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();

            /* Get the parameters */

            /* Flag to indicate whether or not the form is being served for the first time. */
            final boolean isUpdate = "1".equals(etk.getParameters().getSingle("update"));
            final String searchText = etk.getParameters().getSingle("searchText");
            // entellitrak ids of data types to be searched
            final List<String> dataTypesParameter = etk.getParameters().getField("dataTypes");
            // business keys of data objects to be searched
            final List<String> dataObjectsParameter = etk.getParameters().getField("dataObjects");

            // This list will contain errors that we encounter as we try to process the request
            final List<String> errors = new LinkedList<>();

            /* We don't want to search on blank text, but we don't want to give an error when they bring up the page
             * for the first time. */
            if(isUpdate && "".equals(searchText)){
                errors.add("You must enter Search Text");
            }


            final List<String> dataObjectsParameterNonNull = dataObjectsParameter == null
                    ? Collections.<String>emptyList()
                     : dataObjectsParameter;

            /* Get the parameters in a form which is easier to work with. We don't want nulls, and we will set up
             * defaults for parameters which need them. */
            final List<String> dataTypesParameterNonNull = dataTypesParameter == null
                    ? Collections.<String>emptyList()
                    : dataTypesParameter;
            final List<String> selectedDataTypes = !isUpdate
                    ? Arrays.asList(DataElementType.TEXT.getEntellitrakNumber()+"",
                            DataElementType.LONG_TEXT.getEntellitrakNumber()+"")
                    : dataTypesParameterNonNull;

            final List<Map<String, Object>> dataObjects = getRDOs(etk);
            // Add a flag which indicates whether the data object is one that is supposed to be searched
            addSelectedPropertyToDataObjects(dataObjects, isUpdate, dataObjectsParameterNonNull);

            final List<String> selectedDataObjects = filterSelectedDataObjects(dataObjects);

            final List<Map<String, Object>> dataTypes = getDataTypes(selectedDataTypes);

            final boolean doSearch = isUpdate && errors.isEmpty();
            final String searchResults = doSearch
                    ? JsonUtilities.encode(performSearch(etk, searchText, selectedDataObjects, selectedDataTypes))
                      : "[]";

            response.put("errors", JsonUtilities.encode(errors));
            response.put("doSearch", JsonUtilities.encode(doSearch));
            response.put("searchText", JsonUtilities.encode(searchText));
            response.put("dataTypes", JsonUtilities.encode(dataTypes));
            response.put("dataObjects", JsonUtilities.encode(dataObjects));
            response.put("searchResults", searchResults);

            return response;

        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * This method takes a list of data object maps which have a "selected" key. It returns the business keys
     * of just the objects which have been selected.
     *
     * @param dataObjects Maps containing information about data objects as well as the "selected" key.
     * @return A list of the business keys of data objects which are selected
     */
    private static List<String> filterSelectedDataObjects(final List<Map<String, Object>> dataObjects){
        final List<String> dataObjectBusinessKeys = new LinkedList<>();

        for(final Map<String, Object> dataObject : dataObjects){
            if((Boolean) dataObject.get("selected")){
                dataObjectBusinessKeys.add((String) dataObject.get("BUSINESS_KEY"));
            }
        }

        return dataObjectBusinessKeys;
    }

    /**
     * Adds a "selected" key to the data object indicating whether or not it should be searched.
     * Data Objects will be searchable if we are on an update version of the form and they have been selected for
     * search.
     *
     * @param dataObjects Maps containing data object information.
     * @param isUpdate flag indicating whether or not we are on the update version of the form.
     * @param submittedObjectBusinessKeys The business keys that the user has selected to search over.
     */
    private static void addSelectedPropertyToDataObjects(
            final List<Map<String, Object>> dataObjects,
            final boolean isUpdate,
            final List<String> submittedObjectBusinessKeys) {
        for(final Map<String, Object> dataObject : dataObjects){
            if(!isUpdate || submittedObjectBusinessKeys.contains(dataObject.get("BUSINESS_KEY"))){
                dataObject.put("selected", true);
            }else{
                dataObject.put("selected", false);
            }
        }
    }

    /**
     * This method will get the data type information for the data types which are searchable and will have a flag
     * indicating whether or not the data type has been selected to be searched.
     *
     * @param selectedDataTypes the data types hat have been selected to search
     * @return Maps of data type information
     */
    private static List<Map<String, Object>> getDataTypes(final List<String> selectedDataTypes) {
        final List<Map<String, Object>> dataTypes = Arrays.asList(
                makeDataType(DataElementType.TEXT.getEntellitrakNumber()+"", "Text", selectedDataTypes),
                makeDataType(DataElementType.LONG_TEXT.getEntellitrakNumber()+"", "LongText", selectedDataTypes));
        return dataTypes;
    }

    /**
     * This method makes a Map which represents a data type. It includes whether or not the user has chosen to search
     * on this data type.
     *
     * @param value entellitrak number of the data type
     * @param display Display String of the data type
     * @param selectedDataTypes The data types that have been selected by the user to search
     * @return A Map of information about the data type
     */
    private static Map<String, Object> makeDataType(final String value, final String display, final List<String> selectedDataTypes) {
        return Utility.arrayToMap(String.class, Object.class, new Object[][]{
            {"value", value},
            {"display", display},
            {"selected", selectedDataTypes.contains(value) ? true : false}});
    }

    /**
     * This method actually performs a search for the text within all RDOs.
     *
     * @param etk entellitrak execution context
     * @param searchText The text that the user wishes to search for
     * @param selectedDataObjects The business keys of the data objects which should be searched
     * @param selectedDataTypes The data types that the user wishes to search over
     * @return A list of search results. It is a deeply nested data structure. If you are interested in the exact
     *          format, it would be easiest to look at the result of it using your browser's developer tools.
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static List<Map<String, Object>> performSearch(final ExecutionContext etk,
            final String searchText,
            final List<String> selectedDataObjects,
            final List<String> selectedDataTypes)
            throws IncorrectResultSizeDataAccessException{

        final List<Map<String, Object>> allResults = new LinkedList<>();

        for(final Map<String, Object> rdo : getRDOs(etk)){
            if(selectedDataObjects.contains(rdo.get("BUSINESS_KEY"))){
                final Map<String, Object> resultsForTable = getResultsForTable(etk, searchText, selectedDataTypes, rdo);
                if(((Collection<?>) resultsForTable.get("records")).size() > 0){
                    allResults.add(resultsForTable);
                }
            }
        }

        return allResults;
    }

    /**
     * This method will look for all occurrences of searchText within a particular reference data object.
     *
     * @param etk entellitrak execution context
     * @param searchText text to be searched for
     * @param selectedDataTypes The data types which should be searched over
     * @param rdo Map containing the configuration information about an RDO
     * @return A Map containing all the results for a particular RDO. Because of how the View code uses this result,
     *          the returned Map includes information about the RDO itself such as its Label.
     */
    private static Map<String, Object> getResultsForTable(final ExecutionContext etk,
            final String searchText,
            final List<String> selectedDataTypes,
            final Map<String, Object> rdo) {

        /* The high-level algorithm is that we will have a Map of tracking ids to matching data elements.
         * We will query for each element and add any results to its entry in matchingRecords.
         * When we are all done, we'll convert matchingRecords to a form more digestible by the View.
         * Doing it this way where we have a separate data query for each data element could have us potentially bring
         * a lot less data over the wire, because we only bring back data which matches, instead of bringing back all
         * data for a particular record when only one of its fields matches. */
        final Map<Long, List<Map<String, Object>>> matchingRecords = new HashMap<>();

        /* Get all text/longText elements on this RDO */
        final List<Map<String, Object>> textualDataElements = etk.createSQL("SELECT dataElement.NAME, dataElement.COLUMN_NAME FROM etk_data_element dataElement WHERE dataElement.data_object_id = :dataObjectId AND dataElement.data_type IN (:textDataTypes) AND dataElement.bound_to_lookup = 0 ORDER BY dataElement.NAME, dataElement.COLUMN_NAME")
                .setParameter("dataObjectId", rdo.get("DATA_OBJECT_ID"))
                .setParameter("textDataTypes", QueryUtility.toNonEmptyParameterList(selectedDataTypes))
                        .fetchList();

        for(final Map<String, Object> textualDataElement : textualDataElements){
            final String columnName = (String) textualDataElement.get("COLUMN_NAME");
            final String elementName = (String) textualDataElement.get("NAME");

            /* Get all matching records for this data element */
            for(final Map<String, Object> matchingValue : etk.createSQL(
                    String.format(Utility.isSqlServer(etk)
                            ? "SELECT ID, %s VALUE FROM %s WHERE %s LIKE '%%' + :searchText + '%%' ESCAPE :escapeChar"
                             : "SELECT ID, %s VALUE FROM %s WHERE UPPER(%s) LIKE '%%' || UPPER(:searchText) || '%%' ESCAPE :escapeChar",
                    columnName,
                    rdo.get("TABLE_NAME"),
                    columnName))
                    .setParameter("searchText", EscapeLike.escapeLike(etk, searchText))
                    .setParameter("escapeChar", EscapeLike.getEscapeCharString())
                    .fetchList()){
                /* Add the matching id/element information to our Map that we're building up */
                addMatchingItem(searchText,
                        matchingRecords,
                        ((Number) matchingValue.get("ID")).longValue(),
                        columnName,
                        elementName,
                        (String) matchingValue.get("VALUE"));
            }
        }

        // Build the return value for this method
        final Map<String, Object> tableResults = new HashMap<>();
        tableResults.put("TABLE_NAME", rdo.get("TABLE_NAME"));
        tableResults.put("BUSINESS_KEY", rdo.get("BUSINESS_KEY"));
        tableResults.put("LABEL", rdo.get("LABEL"));
        tableResults.put("records", convertMatchingRecordsMapToList(matchingRecords));

        return tableResults;
    }

    /**
     * This method adds a matching element/value to the map of matched objects.
     * This method will add an id if it doesn't exist in the map, otherwise it will update the id with the additional
     * match.
     *
     * @param searchText The text which is being searched for
     * @param matchingItems The current map of ids to matched values
     * @param id The id of the item to be added
     * @param columnName The column of the element to add
     * @param elementName The name of the element to add
     * @param value The value to add
     */
    private static void addMatchingItem(
            final String searchText,
            final Map<Long, List<Map<String, Object>>> matchingItems,
            final Long id,
            final String columnName,
            final String elementName,
            final String value) {

        List<Map<String, Object>> matchedItem = matchingItems.get(id);

        if(matchedItem == null){
            matchedItem = new LinkedList<>();
            matchingItems.put(id, matchedItem);
        }

        matchedItem.add(Utility.arrayToMap(String.class, Object.class, new Object[][]{
            {"COLUMN_NAME", columnName},
            {"ELEMENT_NAME", elementName},
            {"VALUE", findAllMatches(searchText, value)}
        }));
    }

    /**
     * This method will convert a Map of matching results to a List of Maps. This is because internally, the Map
     * is easier to construct originally, but the List is easier to work with in the View.
     *
     * @param matchingRecords Map of tracking ids to search results
     * @return List of search results
     */
    private static List<Map<String, Object>> convertMatchingRecordsMapToList(
            final Map<Long, List<Map<String, Object>>> matchingRecords){
        final List<Map<String, Object>> returnList = new LinkedList<>();

        for(final Map.Entry<Long, List<Map<String, Object>>> matchingRecord : matchingRecords.entrySet()){
            returnList.add(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"ID", matchingRecord.getKey()},
                {"COLUMNS", matchingRecord.getValue()}
            }));
        }

        Collections.sort(returnList, (o1, o2) -> ((Long) o1.get("ID")).compareTo((Long) o2.get("ID")));

        return returnList;
    }

    /**
     * This method adds a text fragment to an existing list of fragments if the fragment is not empty.
     *
     * @param fragments The list of fragments to be added to
     * @param fragment The fragment which is to be added
     * @param isMatch Whether the fragment matches the search text
     */
    private static void addFragment(final List<Map<String, Object>> fragments, final String fragment, final boolean isMatch){
        if(fragment.length() > 0){
            fragments.add(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"fragment", fragment},
                {"isMatch", isMatch}
            }));
        }
    }

    /**
     * This method takes a block of text and a keyword. It will find matches in the following way:
     * If a line has the keyword in it, we will return a list of fragments for that line, otherwise the line will thrown
     * out. The fragments that we split the lines into will have a flag indicating whether the fragment matches the
     * keyword, or does not match the keyword. This way the matching fragments can be highlighted in the View.
     *
     * @param keyword The word to be searched for
     * @param text the text to be searched over
     * @return A list of lines of fragments.
     */
    private static List<List<Map<String, Object>>> findAllMatches(final String keyword, final String text) {
        // We want to search for the text case-insensitive
        final Pattern pattern = Pattern.compile(Pattern.quote(keyword),
                Pattern.CASE_INSENSITIVE);

        // These will be the lines that contain a match
        final List<List<Map<String, Object>>> lineMatches = new ArrayList<>();

        final String[] lines = text.split("\r\n|\r|\n");
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            if (pattern.matcher(line).find()) { // The line contains a match

                // We are going to build a list of fragments
                final List<Map<String, Object>> fragments = new LinkedList<>();

                // We get a matcher
                final Matcher matcher = pattern.matcher(line);

                /* We store the index in the line where the matcher is going to start its next search */
                int lastIndex = 0;

                while(matcher.find(lastIndex)){ // The remainder of the line still contains a match
                    // Get the indices of the matching fragment
                    final int startIndex = matcher.start();
                    final int endIndex = matcher.end();

                    // everything between lastIndex and the beginning of the match is an unmatching fragment
                    addFragment(fragments, line.substring(lastIndex, startIndex), false);
                    // everything between the startIndex and endIndex of the match is a matching fragment
                    addFragment(fragments, line.substring(startIndex, endIndex), true);
                    // indicate that we will continue our search at the end of the current match
                    lastIndex = endIndex;
                }

                // There are no more matches, so we add the remainder of the line as an unmatched fragment
                addFragment(fragments, line.substring(lastIndex), false);

                lineMatches.add(fragments);
            }
        }
        return lineMatches;
    }

    /**
     * Gets information about all the RDOs in the system.
     *
     * @param etk entellitrak execution context
     * @return A List of RDO configuration information
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static List<Map<String, Object>> getRDOs(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException{
        return etk.createSQL("SELECT dataObject.DATA_OBJECT_ID, dataObject.TABLE_NAME, dataObject.LABEL, dataObject.BUSINESS_KEY FROM etk_data_object dataObject WHERE object_type = :objectType AND tracking_config_id = :trackingConfigId ORDER BY LABEL, TABLE_NAME")
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdCurrent(etk))
                .setParameter("objectType", DataObjectType.REFERENCE.getEntellitrakNumber())
                .fetchList();
    }
}

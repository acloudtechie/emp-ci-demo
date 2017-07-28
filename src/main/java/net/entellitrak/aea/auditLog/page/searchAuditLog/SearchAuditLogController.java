/**
 *
 * Page to search the AEA Audit Log
 *
 * alee 11/24/2014
 **/

package net.entellitrak.aea.auditLog.page.searchAuditLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.entellitrak.aea.exception.FormValidationException;
import net.micropact.aea.core.utility.StringEscapeUtils;
import net.micropact.aea.utility.Utility;
import net.micropact.aea.utility.rdoutils.RdoSearchPageUtility;

import com.entellitrak.ApplicationException;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;
import com.entellitrak.PageExecutionContext;


/**
 * This is a template RDO search controller used to search the AEA Audit Log.
 * This code can be reused by modifying the AE Configuration section below to create a search
 * page for any RDO.
 *
 * @author aclee
 *
 */
public class SearchAuditLogController implements PageController {

	/*** BEGIN AE CONFIGURATION. ***/
	//NOTE: RDO must have a column C_ORGANIZATION_ID indicating what ORG the user who created the record belongs to.
	private boolean filterByOrgHierarchy = false;
	private boolean militaryTimeMode = false;
	private boolean linkToRdo = true;

	//NOTE: This default can be overridden with a parameter recordsToDisplay=123
	private static final int DEFAULT_NUMBER_OF_RESULTS_PER_PAGE = 20;
	private static final int MAX_NUMBER_OF_RESULTS_PER_PAGE = 500;


	private String pageTitle = "Audit Log Search";
	private final String searchPageBusinessKey = "aea.auditLog.searchAuditLog";
	private String formBusinessKey = "object.aeaAuditLog.form.aeaAuditLogDefaultForm";
	private String viewBusinessKey = "object.aeaAuditLog.view.aeaAuditLogDefaultView";
	/*** END AE CONFIGURATION. ***/

	private TextResponse tr;
	private PageExecutionContext etk;
	private Map<String, List<String>> parameterMap;

    @Override
	public Response execute(final PageExecutionContext theEtk) throws ApplicationException {
    	final long startTime = new Date().getTime();

    	//Initialize class member variables.
    	etk = theEtk;
    	tr = etk.createTextResponse();
    	parameterMap = etk.getParameters().getMap();

    	//This is an HTML page.
    	tr.setContentType(ContentType.HTML);

    	/* BEGIN LOAD PARAMETERS */

    	//Determines whether or not the search uses pre-defined parameters. If true, the user
    	//must pass the parameters in the page URL.
        final boolean predefinedSearch = "true".equalsIgnoreCase(getSingleParameter("predefinedSearch")) ? true : false;

        if (StringUtility.isNotBlank(getSingleParameter("filterByOrgHierarchy"))) {
        	filterByOrgHierarchy = "true".equalsIgnoreCase(getSingleParameter("filterByOrgHierarchy")) ? true : false;
        }

        if (StringUtility.isNotBlank(getSingleParameter("pageTitle"))) {
        	pageTitle = getSingleParameter("pageTitle");
        }

        if (StringUtility.isNotBlank(getSingleParameter("formBusinessKey"))) {
        	formBusinessKey = getSingleParameter("formBusinessKey");
        }

        if (StringUtility.isNotBlank(getSingleParameter("viewBusinessKey"))) {
        	viewBusinessKey = getSingleParameter("viewBusinessKey");
        }

        if (StringUtility.isNotBlank(getSingleParameter("militaryTimeMode"))) {
        	militaryTimeMode = "true".equalsIgnoreCase(getSingleParameter("militaryTimeMode"));
        }


        try {
        	tr.put("filterByOrgHierarchy", filterByOrgHierarchy);
        	tr.put("militaryTimeMode", militaryTimeMode);
        	tr.put("pageTitle", StringEscapeUtils.escapeHtml(pageTitle));
        	tr.put("formBusinessKey", StringEscapeUtils.escapeHtml(formBusinessKey));
        	tr.put("viewBusinessKey", StringEscapeUtils.escapeHtml(viewBusinessKey));
        	tr.put("pageBusinessKey", searchPageBusinessKey);
        	tr.put("predefinedSearch", predefinedSearch);

        	//Clears the form after necessary variables are retained.
        	if ("true".equals(getSingleParameter("formReset"))) {
        		parameterMap.clear();
        	}

            //Create a new Search Page HTML builder.
            final RdoSearchPageUtility htmlBuilder = new RdoSearchPageUtility(etk,
            		                                                    formBusinessKey,
            		                                                    viewBusinessKey,
            		                                                    parameterMap,
            		                                                    militaryTimeMode,
            		                                                    linkToRdo);

        	//This call creates the actual search form from the provided formToCreateSearchForBusinessKey.
        	//The HTML is then put into the view.
			tr.put("inputSearchForm", htmlBuilder.getSearchPageForForm());

			//The inputSearchForm requires some JS for date range between. This puts the dynamically generated JS
			//into the view.
			tr.put("dynamicScripting", htmlBuilder.getDynamicallyGeneratedJS());

			//Determine number of records to display per page of search results.
			int recordsToDisplay = DEFAULT_NUMBER_OF_RESULTS_PER_PAGE;

			//Make sure that the user entered value is greater than or equal to 1 and less than or equal to 500.
			try {
				recordsToDisplay = StringUtility.isBlank(getSingleParameter("recordsToDisplay")) ?
					               DEFAULT_NUMBER_OF_RESULTS_PER_PAGE : new Integer(getSingleParameter("recordsToDisplay"));

				if (recordsToDisplay < 1) {
					recordsToDisplay = 1;
				} else if (recordsToDisplay > MAX_NUMBER_OF_RESULTS_PER_PAGE) {
					recordsToDisplay = MAX_NUMBER_OF_RESULTS_PER_PAGE;
				}
			} catch (final Exception e) {
				recordsToDisplay = DEFAULT_NUMBER_OF_RESULTS_PER_PAGE;
			}

			tr.put("recordsToDisplay", recordsToDisplay);
			addParameter("recordsToDisplay", recordsToDisplay);

			//If the user has clicked the search button or the
			if (parameterMap.containsKey("search") || predefinedSearch) {
				try {
					tr.put("searchResult", htmlBuilder.performSearch(filterByOrgHierarchy));
					tr.put("inputSearchFormFieldsStyle", "display: none;");
				} catch (final FormValidationException ve) {
					//If a validation exception is thrown during the search, handle it and display the errors
					//to the screen.
					tr.put("validationErrors", ve.getMessage());
				}
			} else {
				//If this is the initial opening of the search page, do not show the search result grid.
				tr.put("uIGridControlsStyle", "display: none;");
			}

			tr.put("sortColumnValue", getSingleParameter("sortColumn"));
			tr.put("sortOrderValue", getSingleParameter("sortOrder"));

			handlePagation();
		} catch (final Exception e) {
			throw new ApplicationException(e);
		}

        Utility.aeaLog(theEtk, "Time to Execute = " + (new Date().getTime() - startTime) + "ms");

        return tr;
    }

    /**
     * Returns a single parameter from the HTTP parameter map.
     *
     * @param parameterName Param name.
     * @return Param value.
     */
    private String getSingleParameter(final String parameterName) {
    	if (parameterMap.get(parameterName) != null) {
    		return parameterMap.get(parameterName).get(0);
    	}

    	return null;
    }

    /**
     * Adds a single parameter to the HTTP parameter map.
     *
     * @param key Param key.
     * @param value Param value.
     */
    private void addParameter (final String key, final Object value) {
    	final List <String> tmpList = new ArrayList<>();
    	tmpList.add(value + "");

    	parameterMap.put(key, tmpList);
    }

    /**
     * This method handles the increase and decrease of page range based on user button clicks. NOTE: These values
     * are also influenced / set in the performSearch method (because you have to know how many results the query
     * actually returned.)
     */
    private void handlePagation() {
    	final int totalPages = StringUtility.isBlank(getSingleParameter("totalPages")) ?
				 			   1 : new Integer(getSingleParameter("totalPages"));

    	final int currentPage = StringUtility.isBlank(getSingleParameter("navigateToPage")) ?
    						 1 : new Integer(getSingleParameter("navigateToPage"));

    	final int rowCount = StringUtility.isBlank(getSingleParameter("rowCount")) ?
				 0 : new Integer(getSingleParameter("rowCount"));

    	final int endIndex = StringUtility.isBlank(getSingleParameter("endIndex")) ?
				 0 : new Integer(getSingleParameter("endIndex"));

        final int startIndex = StringUtility.isBlank(getSingleParameter("startIndex")) ?
				 0 : endIndex == 0 ?
						  0 : (new Integer(getSingleParameter("startIndex")) + 1);


    	tr.put("currentPage", currentPage);
    	tr.put("previousPage", ((currentPage - 1) < 1) ? currentPage : (currentPage - 1));
    	tr.put("nextPage", ((currentPage + 1) > totalPages) ? totalPages : (currentPage + 1));
    	tr.put("totalPages", totalPages);
    	tr.put("rowCount", rowCount);
    	tr.put("startIndex", startIndex);
    	tr.put("endIndex", endIndex);

    	addParameter("currentPage", currentPage);
    	addParameter("previousPage", ((currentPage - 1) < 1) ? currentPage : (currentPage - 1));
    	addParameter("nextPage", ((currentPage + 1) > totalPages) ? totalPages : (currentPage + 1));
    	addParameter("totalPages", totalPages);
    }


}

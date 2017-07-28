package net.micropact.aea.du.page.systemPreferencesTool;

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.FileResponse;
import com.entellitrak.page.FileStream;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.entellitrak.aea.du.DuServiceFactory;
import net.micropact.aea.core.enums.SystemPreference;
import net.micropact.aea.core.ioUtility.IOUtility;
import net.micropact.aea.core.query.QueryUtility;
import net.micropact.aea.du.utility.systemPreference.SystemPreferenceExport;
import net.micropact.aea.du.utility.systemPreference.SystemPreferenceValue;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This class serves as the controller code for a page which provides tools for handling entellitrak system preferences.
 * This page can display, import, export, and set recommended production values for system preferences.
 *
 * TODO: Core now has a System Preferences service which could be used in this page.
 *
 * @author zachary.miller
 */
public class SystemPreferencesToolController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            /* We will handle each of the actions separately. They will each produce their own Response. While this
             * is a little annoying since the TextResponses all set similar response variables, the export actually
             * has to produce a FileResponse. */
            final String action = etk.getParameters().getSingle("action");

            if("export".equals(action)){
                return doExport(etk);
            }else if("import".equals(action)){
                return doImport(etk);
            }else if("setProductionValues".equals(action)){
                return doSetProductionValues(etk);
            }else{
                return doInitial(etk);
            }
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * This method adds the default parameters that all of the text responses should contain.
     *
     * @param etk entellitrak execution context
     * @param response Response to add the default parameters to
     */
    private static void addDefaultTextResponseParameters(final ExecutionContext etk, final TextResponse response){
        response.put("allPreferences", JsonUtilities.encode(getAllPreferences(etk)));
        response.put("productionPreferences", JsonUtilities.encode(getProductionPreferences()));
    }

    /**
     * Handles the import action. (uploading a file).
     *
     * @param etk entellitrak execution context
     * @return The response to be returned by the controller
     * @throws Exception If there was an underlying exception.
     */
    private static Response doImport(final PageExecutionContext etk) throws Exception {
        final TextResponse response = etk.createTextResponse();

        final FileStream fileStream = etk.getParameters().getFile("importFile");
        if(fileStream != null){
            InputStream inputStream = null;
            try{
                inputStream = fileStream.getInputStream();
                DuServiceFactory.getSystemPreferenceMigrationService(etk).importFromStream(inputStream);
            }finally{
                IOUtility.closeQuietly(inputStream);
            }
        }
        addDefaultTextResponseParameters(etk, response);
        return response;
    }

    /**
     * This method handles the export action (producing the XML file).
     *
     * @param etk entellitrak execution context
     * @return The response to be returned by the controller
     * @throws Exception If there was an underlying exception.
     */
    private static Response doExport(final PageExecutionContext etk) throws Exception {
        final List<String> preferences = etk.getParameters().getField("preferences");

        final FileResponse response = etk.createFileResponse("systemPreferenceExport.xml",
                DuServiceFactory.getSystemPreferenceMigrationService(etk).exportToStream(new HashSet<>(preferences)));
        response.setContentType("text/xml");
        return response;
    }

    /**
     * Handles the initial page load.
     *
     * @param etk entellitrak execution context
     * @return The response to be returned by the page controller
     */
    private static Response doInitial(final PageExecutionContext etk) {
        final TextResponse response = etk.createTextResponse();
        addDefaultTextResponseParameters(etk, response);
        return response;
    }

    /**
     * Handles the action of setting the production values to their default values.
     *
     * @param etk entellitrak execution context
     * @return The response to be returned by the page controller
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static Response doSetProductionValues(final PageExecutionContext etk)
            throws IncorrectResultSizeDataAccessException {
        final TextResponse response = etk.createTextResponse();

        final List<SystemPreferenceValue> preferenceValues =
                getProductionPreferenceValues(etk.getParameters().getField("preferences"));
        SystemPreferenceExport.importPreferences(etk, preferenceValues);

        addDefaultTextResponseParameters(etk, response);

        return response;
    }

    /**
     * This method gets a list of the default production values for a list of system preference names.
     *
     * @param preferences names of the preferences whose values should be gotten
     * @return The preferences and their corresponding default production value
     */
    private static List<SystemPreferenceValue> getProductionPreferenceValues(final List<String> preferences){
        final List<SystemPreferenceValue> preferenceValues = new LinkedList<>();
        if(preferences != null && !preferences.isEmpty()){
            for(final String preference : preferences){
                preferenceValues.add(new SystemPreferenceValue(preference,
                        Optional.of(SystemPreference.getPreferenceByName(preference).getDefaultProductionValue())));
            }
        }
        return preferenceValues;
    }

    /**
     * Gets a list of the preferences which are currently configured in the system along with their values.
     *
     * @param etk entellitrak execution context
     * @return A generic representation of the preferences in the system, their current value and whether they
     * are exportable by default.
     */
    private static List<Map<String, Object>> getAllPreferences(final ExecutionContext etk) {
        final List<Map<String, Object>> preferences  = new LinkedList<>();
        for(final SystemPreference preference : SystemPreference.values()){
            final String name = preference.getName();

            final List<String> currentValues = QueryUtility.toSimpleList(etk.createSQL("SELECT VALUE FROM etk_system_preference WHERE name = :name")
                    .setParameter("name", name)
                    .fetchList());

            preferences.add(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"name", name},
                {"currentValue", ((currentValues.size() == 1) ? currentValues.get(0) : null)},
                {"setInCurrentDB", (currentValues.size() == 1)},
                {"isExportableByDefault", preference.isExportable()}
            }));
        }
        return preferences;
    }

    /**
     * This method gets a list of all system preferences which have a recommended value in production.
     *
     * @return a list of production system preferences
     */
    private static List<SystemPreference> getProductionPreferences() {
        final List<SystemPreference> returnValue = new LinkedList<>();
        final Set<SystemPreference> preferences = SystemPreference.getProductionSystemPreferences();
        for(final SystemPreference preference : preferences){
            returnValue.add(preference);
        }
        return returnValue;
    }
}
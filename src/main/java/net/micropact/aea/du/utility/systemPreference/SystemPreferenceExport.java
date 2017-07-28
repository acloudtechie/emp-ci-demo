package net.micropact.aea.du.utility.systemPreference;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

import net.micropact.aea.utility.Utility;

/**
 * This class groups together functionality specifically dealing with exporting and importing preferences.
 *
 * @author zachary.miller
 */
public final class SystemPreferenceExport {

    /**
     * Utility classes do not need constructors.
     */
    private SystemPreferenceExport(){}

    /**
     * Gets the current value in the system for a set of preferences. The preferences must be ones which have actual
     * entries in ETK_SYSTEM_PREFERENCE, not ones which are missing from the table.
     *
     * @param etk entellitrak execution context
     * @param preferences The preference/value pairs which should be exported.
     * @return A collection of system preferences and their corresponding values.
     */
    public static Collection<SystemPreferenceValue> exportPreferences(final ExecutionContext etk,
            final Collection<String> preferences){
        final Collection<SystemPreferenceValue> exportPreferences = new LinkedList<>();

        for(final String name : preferences){
            final List<Map<String, Object>> dbValue = etk.createSQL("SELECT VALUE FROM etk_system_preference WHERE name = :name")
                .setParameter("name", name)
                .fetchList();

            Optional<String> optionalValue;

            if(dbValue.isEmpty()){
                optionalValue = Optional.empty();
            }else{
                optionalValue = Optional.of(Utility.nvl((String) dbValue.get(0).get("VALUE"), ""));
            }

            exportPreferences.add(new SystemPreferenceValue(name, optionalValue));
        }
        return exportPreferences;
    }

    /**
     * Imports a set of system preference/value pairs. Preferences will not be deleted, they will only be inserted
     * or updated, not deleted.
     *
     * @param etk entellitrak execution context
     * @param systemPreferences The System Preference/Value pairs which should be imported.
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static void importPreferences(final ExecutionContext etk, final Collection<SystemPreferenceValue> systemPreferences)
            throws IncorrectResultSizeDataAccessException{
        for(final SystemPreferenceValue systemPreference : systemPreferences){
            updatePreference(etk, systemPreference);
        }
    }

    /**
     * Imports or updates a single system preference value. It will not delete a preference.
     *
     * @param etk entellitrak execution context
     * @param systemPreference the system preference and its new value
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static void updatePreference(final ExecutionContext etk, final SystemPreferenceValue systemPreference)
            throws IncorrectResultSizeDataAccessException {
        final String preferenceName = systemPreference.getName();
        final Optional<String> optionalPreferenceValue = systemPreference.getValue();

        if(optionalPreferenceValue.isPresent()){
            final Integer matchingPreference = etk.createSQL("SELECT system_preference_id FROM etk_system_preference WHERE name = :systemPreferenceName")
                    .setParameter("systemPreferenceName", preferenceName)
                    .returnEmptyResultSetAs(null)
                    .fetchInt();

            String query;
            if(matchingPreference == null){
                query = Utility.isSqlServer(etk)
                        ? "INSERT INTO etk_system_preference(name, value) VALUES(:name, :value)"
                          : "INSERT INTO etk_system_preference(system_preference_id, name, value) VALUES(HIBERNATE_SEQUENCE.NEXTVAL, :name, :value)";
            }else{
                query = "UPDATE etk_system_preference SET value = :value WHERE system_preference_id = :systemPreferenceId";
            }

            etk.createSQL(query)
            .setParameter("name", preferenceName)
            .setParameter("value", systemPreference.getValue().get())
            .setParameter("systemPreferenceId", matchingPreference)
            .execute();
        }else{
            etk.createSQL("DELETE FROM etk_system_preference WHERE name = :name")
            .setParameter("name", preferenceName)
            .execute();
        }
    }
}
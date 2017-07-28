package net.micropact.aea.du.service;

import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.du.service.ISystemPreferenceMigrationService;
import net.micropact.aea.du.utility.systemPreference.SystemPreferenceExport;
import net.micropact.aea.du.utility.systemPreference.SystemPreferenceMarshaller;
import net.micropact.aea.du.utility.systemPreference.SystemPreferenceValue;

/**
 * This class is the private implementation of the {@link ISystemPreferenceMigrationService} interface in the
 * public API.
 *
 * @author zachary.miller
 */
public class SystemPreferenceMigrationService implements ISystemPreferenceMigrationService {

    private final ExecutionContext etk;

    /**
     * Simple constructor.
     *
     * @param executionContext entellitrak execution context
     */
    public SystemPreferenceMigrationService(final ExecutionContext executionContext) {
        etk = executionContext;
    }

    @Override
    public InputStream exportToStream(final Set<String> preferencesToExport) throws Exception {
        final Collection<SystemPreferenceValue> systemPreferenceValues =
                SystemPreferenceExport.exportPreferences(etk, preferencesToExport);

        return SystemPreferenceMarshaller.marshall(systemPreferenceValues);
    }

    @Override
    public void importFromStream(final InputStream xmlPreferenceStream)
            throws Exception{
        final Collection<SystemPreferenceValue> systemPreferences =
                SystemPreferenceMarshaller.unmarshall(xmlPreferenceStream);
        SystemPreferenceExport.importPreferences(etk, systemPreferences);
    }
}

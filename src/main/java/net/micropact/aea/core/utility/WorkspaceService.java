package net.micropact.aea.core.utility;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;

import net.micropact.aea.core.wrappedAPIs.WorkspaceServiceWrapper;

/**
 * This class contains methods for dealing with the core classes related to Script Object Workspaces.
 *
 * @author Zachary.Miller
 */
public final class WorkspaceService {

    /**
     * Utility classes do not need public constructors.
     */
    private WorkspaceService(){}

    /**
     * This method clears the cache for the System Workspace. It needs to be called after programmatically modifying
     * the workspace.
     */
    public static void clearSystemWorkspaceCache() {
        WorkspaceServiceWrapper.clearSystemWorkspaceCache();
    }

    /**
     * Helper method to update a script object in a manner that allows the Hibernate cache to pull the updated
     * script object's value. Directly using sql updates / sqlfacade is not an acceptable solution when making
     * repeated updates for the etk.executeSQLFromScriptObject
     *
     *
     * @param etk The execution context.
     * @param scriptObjectId A script object.
     * @param scriptObjectCode The script objects code.
     * @throws ApplicationException An exception if the update fails.
     */
    public static void updateScriptObject (final ExecutionContext etk,
            final Long scriptObjectId,
            final String scriptObjectCode) throws ApplicationException {
        WorkspaceServiceWrapper.updateScriptObject(etk, scriptObjectId, scriptObjectCode);
    }
}

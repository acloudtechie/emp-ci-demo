package net.micropact.aea.core.wrappedAPIs;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.micropact.ExecutionContextImpl;
import com.micropact.entellitrak.cfg.form.ScriptObjectForm;
import com.micropact.entellitrak.cfg.model.ScriptObject;
import com.micropact.entellitrak.cfg.model.ScriptObjectTO;
import com.micropact.entellitrak.config.SpringGlobalContext;
import com.micropact.entellitrak.exception.AccessDeniedException;
import com.micropact.entellitrak.exception.NameExistsException;
import com.micropact.entellitrak.system.Services;
import com.micropact.entellitrak.workspace.exception.AssociatedETOException;
import com.micropact.entellitrak.workspace.service.WorkspaceService;

/**
 * This class contains methods for dealing with the core classes related to Script Object Workspaces.
 *
 * @author Zachary.Miller
 */
public final class WorkspaceServiceWrapper {

	/**
	 * Utility classes do not need public constructors.
	 */
	private WorkspaceServiceWrapper(){}

	/**
	 * This method clears the cache for the System Workspace. It needs to be called after programmatically modifying
	 * the workspace.
	 */
	public static void clearSystemWorkspaceCache(){
		Services.getWorkspaceService().publishWorkspaceChanged(true);
		Services.getWorkspaceIndexService().invalidateIndex(Services.getWorkspaceService().getSystemWorkspace());
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
		final WorkspaceService ws = SpringGlobalContext.getBean(WorkspaceService.class);

		final ScriptObject originalScriptObject = ws.getScriptObject(scriptObjectId);
		final ScriptObjectTO scriptForm = new ScriptObjectForm();
		scriptForm.setId(originalScriptObject.getId() + "");
		scriptForm.transferFrom(originalScriptObject);
		scriptForm.setDescription(originalScriptObject.getDescription());
		scriptForm.setCode(scriptObjectCode);
		scriptForm.setPublicResource(false);
		scriptForm.setScriptLanguageTypeId(originalScriptObject.getScriptLanguageType().getId());
		scriptForm.setHandlerTypeId(originalScriptObject.getHandlerType().getId());

		// workaround: if this isn't set the package will be nulled out in the
        // ScriptObjectManager because the above transferFrom doesn't set it.
        if (originalScriptObject.getPackageNode() != null) {
            scriptForm.setParentNodeId(originalScriptObject.getPackageNode().getId());
        }

        final ExecutionContextImpl etki = (ExecutionContextImpl) etk;
        try {
			ws.updateScriptObject(scriptForm, etki.getUserContainer());
		} catch (NameExistsException | AccessDeniedException | AssociatedETOException e) {
			throw new ApplicationException(e);
		}
	}
}

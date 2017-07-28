package net.micropact.aea.rf.dao;

import net.entellitrak.aea.rf.dao.IScriptObject;

/**
 * Simple implementation of {@link ScriptObjectImpl}.
 *
 * @author zachary.miller
 */
public class ScriptObjectImpl implements IScriptObject {

    private final String fullyQualifiedName;

    /**
     * A Simple Constructor.
     *
     * @param theFullyQualifiedName the fully qualified name of the Script Object.
     */
    public ScriptObjectImpl(final String theFullyQualifiedName){
        fullyQualifiedName = theFullyQualifiedName;
    }

    @Override
    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }
}

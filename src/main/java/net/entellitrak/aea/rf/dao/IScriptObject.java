package net.entellitrak.aea.rf.dao;

/**
 * This is the DAO for Script Objects in the Rules Framework.
 *
 * @author zachary.miller
 */
public interface IScriptObject {

    /**
     * Gets the fully qualified script object name. This includes the package path and class name, but not a file
     * extension.
     *
     * @return the fully qualified script object name.
     */
    String getFullyQualifiedName();
}

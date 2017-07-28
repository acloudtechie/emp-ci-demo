package net.micropact.aea.core.reflection;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ExecutionContext;

import net.micropact.aea.utility.ScriptObjectLanguageType;
import net.micropact.aea.utility.Utility;

/**
 * This class contains utility functionality for determining what Script Objects implement a particular interface.
 *
 * @author zachary.miller
 */
public final class InterfaceImplementationUtility {

    /**
     * Utility classes do not need constructors.
     */
    private InterfaceImplementationUtility(){}

    /**
     * This method finds all Classes which are defined as top-level types in their Script Object files and implement a
     * particular interface.
     *
     * @param etk entellitrak execution context
     * @param theInterface the Interface which the classes must implement
     * @return The list of classes which implement the interface
     */
    public static List<Class<?>> getInterfaceImplementations(final ExecutionContext etk, final Class<?> theInterface){
        final List<Class<?>> implementations = new LinkedList<>();

        // TODO: This can be rewritten to use core's API, but doing so makes the code noticeably slower.
        final List<Map<String, Object>> scripts = etk.createSQL("SELECT FULLY_QUALIFIED_SCRIPT_NAME FROM aea_script_pkg_view_sys_only WHERE script_language_type = :languageType ORDER BY fully_qualified_script_name")
                .setParameter("languageType", ScriptObjectLanguageType.JAVA.getId())
                .fetchList();

        for(final Map<String, Object> script : scripts){
            final String name = (String) script.get("FULLY_QUALIFIED_SCRIPT_NAME");
            try {
                final Class<?> currentClass = Class.forName(name);

                if(doesClassImplementInterface(currentClass, theInterface)){
                    implementations.add(currentClass);
                }
            } catch (final ClassNotFoundException e) {
                Utility.aeaLog(etk,
                        String.format("ClassUtility could not determine whether class \"%s\" implements interface \"%s\"",
                                name,
                                theInterface.getName()),
                        e);
            }
        }
        return implementations;
    }

    /**
     * This method determines whether a class implements a particular interface.
     *
     * @param theClass the class which might implement the interface
     * @param theInterface the interface which must be implemneted
     * @return whether the class implements the interface
     */
    public static boolean doesClassImplementInterface(final Class<?> theClass, final Class<?> theInterface) {
        final int modifiers = theClass.getModifiers();

        return theInterface.isAssignableFrom(theClass)
                && !theClass.isInterface()
                && !Modifier.isAbstract(modifiers)
                && Modifier.isPublic(modifiers);
    }
}

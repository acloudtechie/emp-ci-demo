package net.entellitrak.aea.tu;

import java.util.Map;

import net.entellitrak.aea.exception.TemplateException;

/**
 * <p>
 *  This interface is tightly coupled with {@link ITemplater}.
 *  It defines objects which know how to replace variables within a template.
 * </p>
 * <p>
 *  Different IReplacers may have different strategies to do replacement.
 *  Some may do a static lookup, others may perform SQL calls while others may escape HTML
 * </p>
 *
 * @author zmiller
 * @see ITemplater
 */
public interface IReplacer{
    /**
     * This method maps a variable found within a template to its correct value.
     *
     * @param templater This is the ITempalter that is doing the replacement.
     *  It is mostly useful for IReplacers which are going to escape the result of another variable
     *  because they can ask the templater to replace the other variable and then do their escaping on the result
     * @param variableName The text that should be replaced
     * @param replacementVariables This is information that should be used to help map the name to its correct value
     * @return The result of replacing variableName
     * @throws TemplateException If there is any problem performing the replacement.
     *  Almost any underlying problem could cause the replacement to not be done
     *  and it will depend on the {@link IReplacer}
     */
    String replace(ITemplater templater, String variableName, Map<String, Object> replacementVariables)
            throws TemplateException;
}

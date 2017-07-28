package net.entellitrak.aea.tu;

import java.util.Map;

import net.entellitrak.aea.exception.TemplateException;

/**
 * <p>This defines the interface for a simple but useful way of replacing variables in a template String.</p>
 * The template Strings look like
 * <pre>Hello {name}. You are looking {disposition} today!</pre>
 * where "{" is the start character, "}" is the end character and {name} and {disposition}
 * are replaced with specific values like "zmiller" and "nervous"
 *
 * <p>
 *  Different implementations may exist which could use a DSL within the start and end characters so that the same
 *  template might instead be represented as:
 * </p>
 * <pre>Hello {!?name}. You are looking {^disposition} today!</pre>
 *
 * @author zmiller
 * @see IReplacer
 */
public interface ITemplater{

    /**
     * Gets the character which indicates the beginning of a section which is to be replaced.
     *
     * @return The Character.
     */
    Character getStartCharacter();

    /**
     * Gets the Character which indicates the end of a section to be replaced has been reached.
     *
     * @return The Character
     */
    Character getEndCharacter();

    /**
     * This method actually performs the template replacement.
     *
     * @param templateString This is the String which is the template text
     * @param replacementVariables This Map contains the information that should be used for the templater
     *     to determine what the values it needs to replace are. Different implementations of ITemplater may choose
     *     different schemes for replacement.
     * @return A String where the template has been reified (all the replacements have been made)
     * @throws TemplateException If there is any problem performing the replacement.
     *     Almost any underlying problem could cause a TemplateException to be thrown.
     */
    String parse(String templateString, Map<String, Object> replacementVariables) throws TemplateException;
}

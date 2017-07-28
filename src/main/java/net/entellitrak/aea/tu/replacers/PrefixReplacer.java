package net.entellitrak.aea.tu.replacers;

import java.util.Map;

import net.entellitrak.aea.exception.TemplateException;
import net.entellitrak.aea.tu.IReplacer;
import net.entellitrak.aea.tu.ITemplater;

/**
 * <p>
 *  PrefixReplacer is a replacer which is used in combination with another replacer.
 *  It uses the {@link ITemplater} to run the replacement and then inserts a prefix at the beginning of the result
 *  only if the result is not blank.
 *  An example where this would be useful would be when you have an AddressLine1 and optional AddressLine2.
 *  You would use the PrefixReplacer with "\n" as its prefix on AddressLine2 so that
 *  when AddressLine2 is blank, it doesn't cause a blank line to be printed.
 * </p>
 *
 * @author zmiller
 */
public final class PrefixReplacer implements IReplacer {

    /** The prefix which will be appended before non-null results. */
    private final String prefix;

    /**
     * Constructor for PrefixReplacer.
     *
     * @param replacementPrefix The prefix which will be added to the variable if it is not blank
     */
    public PrefixReplacer(final String replacementPrefix){
        prefix = replacementPrefix == null ? "" : replacementPrefix;
    }

    @Override
    public String replace(final ITemplater templater, final String variableName,
            final Map<String, Object> replacementVariables) throws TemplateException{
        final String replacement = templater.parse(templater.getStartCharacter()
                + variableName
                + templater.getEndCharacter(),
                replacementVariables);
        return "".equals(replacement) ? "" : prefix + replacement;
    }
}

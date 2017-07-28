package net.entellitrak.aea.tu.replacers;

import java.util.Map;

import net.entellitrak.aea.tu.IReplacer;
import net.entellitrak.aea.tu.ITemplater;

/**
 * <p>
 *  This replacement strategy is used to place a static String within a template which comes from
 *  the replacementVariables Map, but not from the Database.
 *  For instance to send a user's temporary password you could store the password in the replacementVariables map,
 *  and then use a TextReplacer to take it out.
 *  This replacer is also useful for debugging purposes since it gives you access to the replacementVariables map.
 * </p>
 *
 * @author zmiller
 */
public final class TextReplacer implements IReplacer {

    /**
     * All TextReplacers are identical so it is a Singleton.
     */
    private TextReplacer(){}

    /** The single instance of TextReplacer. */
    private static final TextReplacer TEXT_REPLACER = new TextReplacer();

    @Override
    public String replace(final ITemplater templater,
            final String variableName,
            final Map<String, Object> replacementVariables){
        final Object replacement = replacementVariables.get(variableName);
        return replacement == null ? "" : replacement.toString();
    }

    /**
     * Get an instance of TextReplacer.
     *
     * @return the singleton TextReplacer
     */
    public static TextReplacer getInstance(){
        return TEXT_REPLACER;
    }
}

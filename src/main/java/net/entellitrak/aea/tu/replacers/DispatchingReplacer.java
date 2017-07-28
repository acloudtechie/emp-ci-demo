package net.entellitrak.aea.tu.replacers;

import java.util.Map;

import net.entellitrak.aea.exception.TemplateException;
import net.entellitrak.aea.tu.IReplacer;
import net.entellitrak.aea.tu.ITemplater;

/**
 * <p>
 *  DispatchingReplacer is a replacer which will run a different replacer depending on the first character
 *  of the variableName.
 *  The mapping of Characters to IReplacers is specified through the constructor.
 *  The reason for having a DispatchingReplacer is that there are only a limited number
 *  of good characters to dispatch on
 *  and the DispatchingReplacer allows an {@link ITemplater} to have more {@link IReplacer}s than characters.
 * </p>
 *
 * @author zmiller
 */
public final class DispatchingReplacer implements IReplacer {

    private final Map<Character, IReplacer> replacersMap;

    /**
     * Constructor for DispatchingReplacer.
     *
     * @param dispatchingReplacersMap The mapping which defines this DispatchingReplacer
     */
    public DispatchingReplacer(final Map<Character, IReplacer> dispatchingReplacersMap){
        replacersMap = dispatchingReplacersMap;
    }

    @Override
    public String replace(final ITemplater templater, final String variableName,
            final Map<String, Object> replacementVariables) throws TemplateException {
        if(variableName.length() == 0){
            throw new TemplateException("Dispatching Replacer cannot be called with a blank variableName");
        }else{
            final char dispatchCharacter = variableName.charAt(0);
            final IReplacer replacer = replacersMap.get(dispatchCharacter);
            if(replacer == null){
                throw new TemplateException(
                        String.format("Error Encounterd in DispatchingReplacer. No Replacer mapped to character: %s",
                                dispatchCharacter));
            }else{
                return replacer.replace(templater, variableName.substring(1), replacementVariables);
            }
        }
    }
}

package net.entellitrak.aea.tu;

import java.util.Map;

import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.exception.TemplateException;
import net.entellitrak.aea.tu.replacers.DataElementReplacer;
import net.entellitrak.aea.tu.replacers.DispatchingReplacer;
import net.entellitrak.aea.tu.replacers.HtmlEscapeReplacer;
import net.entellitrak.aea.tu.replacers.PrefixReplacer;
import net.entellitrak.aea.tu.replacers.SectionReplacer;
import net.entellitrak.aea.tu.replacers.SqlReplacer;
import net.entellitrak.aea.tu.replacers.TextReplacer;
import net.micropact.aea.utility.Utility;

/**
 * <p>
 *  This is a single-dispatch ITemplater.
 *  When it encounters the start character,
 *  it then decides how to replace everything up to the end character by selecting an appropriate replacer
 *  based on the character immediately following the startChacater.
 * </p>
 * <p>
 *  A special case is when you want to have just the startCharacter,
 *  you can immediately follow it with the endCharacter.
 * </p>
 * <p>
 *  Note: The end character just displays itself, it does not need to be escaped.
 *  An example template String with { and } as the start and end characters could look like the following:
 * </p>
 * <pre>
 *  {^header}
 *  Hello {!?username}
 * </pre>
 * Where ^ is mapped to a replacer which looks up some static text, ! is mapped to a replacer which
 * replaces what follows it and then escapes the HTML and ? is a mapped to a replacer which makes an SQL call.
 *
 * @author zmiller
 */
public final class Templater implements ITemplater{

    /** This is the character that will indicate the start of replacements. */
    private final Character startCharacter;
    /** This is the character that will indicate the end of replacements. */
    private final Character endCharacter;
    /** Contains the mapping from dispatch-characters to IReplacers. */
    private final Map<Character, IReplacer> replacers;

    /**
     * This function is used to create a new Templater.
     *
     * @param templateStartCharacter The Character to determine that a variable is being started
     * @param templateEndCharacter The Character to determine that the end of a variable has been reached
     * @param templateReplacers This maps which Characters should correspond to which
     *  IReplacer when the template is parsed
     */
    public Templater(final Character templateStartCharacter,
            final Character templateEndCharacter,
            final Map<Character, IReplacer> templateReplacers){
        startCharacter = templateStartCharacter;
        endCharacter = templateEndCharacter;
        replacers = templateReplacers;
    }

    /**
     * This function is used to create a new {@link Templater} with entirely default options.
     *
     * @param etk entellitrak execution context
     */
    public Templater(final ExecutionContext etk){
        this(getDefaultStartCharacter(), getDefaultEndCharacter(), getDefaultReplacers(etk));
    }

    @Override
    public Character getStartCharacter(){
        return startCharacter;
    }

    @Override
    public Character getEndCharacter(){
        return endCharacter;
    }

    @Override
    public String parse(final String string, final Map<String, Object> replacementVariables)
            throws TemplateException{

        final StringBuilder outputString = new StringBuilder();

        int index = 0; //This is not a for loop because index will not always be incremented by exactly one
        while(index < string.length()){
            final Character currentCharacter = string.charAt(index);
            if(startCharacter.equals(currentCharacter)){
                //We have encountered the start of an escape sequence
                if(index == string.length() - 1){
                    //We can't start an escape sequence at the end of a string
                    throw new TemplateException(
                            String.format("Starting %s encountered at the end of the template",
                                    new Object[]{startCharacter}));
                }else{
                    index++;
                    //The next character determines which type of replacement we are going to do
                    final Character dispatchCharacter = string.charAt(index);
                    if(endCharacter.equals(dispatchCharacter)){
                        // The end character escapes the start character,
                        // so we add it and continue replacing the rest of the string
                        outputString.append(startCharacter);
                        index++;
                    }else{
                        //We're actually going to attempt to do an interesting replacement
                        final IReplacer replacer = replacers.get(dispatchCharacter);
                        if(replacer == null){
                            //No replacer was configured for this character
                            throw new TemplateException(
                                    String.format("Unknown Dispatch Character %s encountered while replacing",
                                            new Object[]{dispatchCharacter}));
                        }else{
                            //Now we actually get to do the replacement
                            final int closingLocation = string.indexOf(endCharacter, index);
                            final String variableName = string.substring(index + 1, closingLocation);
                            outputString.append(replacer.replace(this, variableName, replacementVariables));
                            //Skip over the rest of the name and closing special character
                            index = closingLocation + 1;
                        }
                    }
                }
            }else{
                //just append the character and continue our replacement
                outputString.append(currentCharacter);
                index ++;
            }
        }

        return outputString.toString();
    }

    /**
     * This function returns a default Mapping of Characters to IReplacers.
     * <ul>
     *  <li>? - {@link SqlReplacer}</li>
     *  <li>$ - {@link SectionReplacer}</li>
     *  <li>^ - {@link TextReplacer}</li>
     *  <li>_ - {@link DataElementReplacer}</li>
     *  <li>! - {@link HtmlEscapeReplacer}</li>
     *  <li>-n - {@link PrefixReplacer} "\n"</li>
     *  <li>-s - {@link PrefixReplacer} " "</li>
     * </ul>
     *
     * @param etk The context which the replacements should occur in.
     * Note that this means that you should not persist this map across requests.
     * @return Mapping of Character to {@link IReplacer}.
     */
    public static Map<Character, IReplacer> getDefaultReplacers(final ExecutionContext etk){
        return Utility.arrayToMap(Character.class, IReplacer.class, new Object[][]{
            {'?', new SqlReplacer(etk)},
            {'$', new SectionReplacer(etk)},
            {'!', HtmlEscapeReplacer.getInstance()},
            {'^', TextReplacer.getInstance()},
            {'_', new DataElementReplacer(etk)},
            {'-', new DispatchingReplacer(Utility.arrayToMap(Character.class, IReplacer.class, new Object[][]{
                {'n', new PrefixReplacer("\n")},
                {'s', new PrefixReplacer(" ")}}))}
        });
    }

    /**
     * Gets the Default Start Character the Templater uses if none is specified.
     *
     * @return the default starting character
     */
    public static char getDefaultStartCharacter(){
        return '{';
    }

    /**
     * Gets the Default End Character the Templater uses if none is specified.
     *
     * @return the default ending character
     */
    public static char getDefaultEndCharacter(){
        return '}';
    }
}

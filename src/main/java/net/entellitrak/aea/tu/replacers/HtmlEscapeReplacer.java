package net.entellitrak.aea.tu.replacers;

import java.util.Map;

import net.entellitrak.aea.exception.TemplateException;
import net.entellitrak.aea.tu.IReplacer;
import net.entellitrak.aea.tu.ITemplater;
import net.micropact.aea.core.utility.StringEscapeUtils;

/**
 * <p>
 *  This replacement strategy is used in conjunction with another {@link IReplacer} within a template.
 *  For instance, a templater may have
 *  ! mapped to an {@link HtmlEscapeReplacer} and ? mapped to an {@link SqlReplacer}. In that case
 *      <code>{!?username}</code>
 *  will perform the Sql Replacement of {?username}, and then escape the html of the result.
 * </p>
 *
 * @author zmiller
 */
public final class HtmlEscapeReplacer implements IReplacer{

    private static final HtmlEscapeReplacer HTML_ESCAPE_REPLACER = new HtmlEscapeReplacer();

    /** All HtmlEscapeReplars are identical, so there is no reason to make a new one from outside. */
    private HtmlEscapeReplacer(){}

    @Override
    public String replace(final ITemplater templater,
            final String variableName,
            final Map<String, Object> replacementVariables)
                    throws TemplateException{
        return StringEscapeUtils.escapeHtml(templater.parse(templater.getStartCharacter()
                + variableName
                + templater.getEndCharacter(),
                replacementVariables)).replaceAll("\\r?\\n", "<br />");
    }

    /**
     * Gets an instance of HtmlEscapeReplacer.
     *
     * @return the singleton HtmlEscapeReplacer
     */
    public static HtmlEscapeReplacer getInstance(){
        return HTML_ESCAPE_REPLACER;
    }
}

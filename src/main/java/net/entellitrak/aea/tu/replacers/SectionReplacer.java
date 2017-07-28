package net.entellitrak.aea.tu.replacers;
import java.util.List;
import java.util.Map;

import com.entellitrak.DataAccessException;
import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.exception.TemplateException;
import net.entellitrak.aea.tu.IReplacer;
import net.entellitrak.aea.tu.ITemplater;

/**
 * <p>
 *  This Replacer is used for replacing a variable with an entry from the T_TU_REPLACEMENT_SECTION reference data list.
 *  The {@link ITemplater} will  be called on the result of doing the replacement before it is returned.
 * </p>
 *
 * @author zmiller
 */
public final class SectionReplacer implements IReplacer{

    private final ExecutionContext etk;

    /**
     * Constructor for SectionReplacer.
     *
     * @param executionContext The context that the {@link SectionReplacer} should use for database queries
     */
    public SectionReplacer(final ExecutionContext executionContext){
        etk = executionContext;
    }

    @Override
    public String replace(final ITemplater templater,
            final String variableName,
            final Map<String, Object> replacementVariables) throws TemplateException{

        try {
            List<Map<String, Object>> sections;
            sections = etk.createSQL("SELECT C_TEXT FROM t_tu_replacement_section WHERE c_name = :name")
                    .setParameter("name", variableName)
                    .fetchList();

            if(sections.size() == 0){
                throw new TemplateException(String.format("Replacement Section not found: \"%s\"", variableName));
            }else{
                return templater.parse(sections.get(0).get("C_TEXT").toString(), replacementVariables);
            }
        } catch (final DataAccessException e) {
            throw new TemplateException(
                    String.format("Error performing Section Replacement on variable: %s", variableName), e);
        }
    }
}

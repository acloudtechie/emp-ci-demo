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
 *  This replacement strategy relies on a T_TU_REPLACEMENT_VARIABLE table.
 *  It will replace a variable name with a the result of running the query mapped to that name.
 *  The query will have access to bind variables which the user may pass into the query
 *  through the replacementVariables parameter.
 *  The names of bind variables in the SQL query must match names of the keys in the replacementVariables Map.
 * </p>
 *
 * @author zmiller
 */
public final class SqlReplacer implements IReplacer{
    private final ExecutionContext etk;

    /**
     * This generates a new SQL Replacer.
     *
     * @param executionContext The etk variable in entellitrak
     */
    public SqlReplacer(final ExecutionContext executionContext){
        etk = executionContext;
    }

    @Override
    public String replace(final ITemplater templater,
            final String variableName,
            final Map<String, Object> replacementVariables)
                    throws TemplateException{
        try{
            final String returnValue;

            final List<Map<String, Object>> sqlQueries = etk.createSQL("SELECT C_SQL FROM t_tu_replacement_variable WHERE c_name = :variableName")
                    .setParameter("variableName", variableName)
                    .fetchList();
            if(sqlQueries.size() == 0){
                throw new TemplateException(String.format("Replacement Variable not found: \"%s\".", variableName));
            }else{
                final String sqlQuery = sqlQueries.get(0).get("C_SQL").toString();
                final List<Map<String, Object>> values = etk.createSQL(sqlQuery)
                        .setParameter(replacementVariables)
                        .fetchList();
                if(values.size() == 0){
                    returnValue = "";
                }else if(values.size() == 1){
                    final Object value = (values.get(0)).values().iterator().next();
                    returnValue = value == null ? "" : value.toString();
                }else{
                    throw new TemplateException(
                            String.format("Replacement Variable query \"%s\" did not return exactly 0 or 1 rows. %s Rows Returned.",
                                    sqlQuery, values.size()));
                }
            }
            return returnValue;
        }catch(final DataAccessException e){
            throw new TemplateException(
                    String.format("Error Performing SQL Replacement on variable \"%s\"", variableName), e);
        }
    }
}

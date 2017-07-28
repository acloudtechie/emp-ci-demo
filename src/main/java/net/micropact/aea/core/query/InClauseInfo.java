package net.micropact.aea.core.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is to get around the restriction in Oracle that an IN clause can only have 1,000 entries by splitting the
 * IN clause into multiple in clauses.
 *
 * It has a method which will return a String in the format
 * <pre>
 * (columnName IN(:parameterPrefix0) OR columnName IN(:parameterPrefix1) ...)
 * </pre>
 * as well as the parameter map which would bind the parameter names it chooses to the correct values
 * @author zmiller
 */
public class InClauseInfo {

    private static final int IN_CLAUSE_LIMIT = 1000;
    private final String columnName;
    private final String parameterPrefix;
    private final List<?> parameters;

    /**
     * Construct a new {@link InClauseInfo}.
     *
     * @param theColumnName The column that will be compared to the IN clause values
     * @param theParameterPrefix The prefix to use for the query parameters
     * @param theParameters The values which should be bound to the parameters
     */
    public InClauseInfo(final String theColumnName, final String theParameterPrefix, final List<?> theParameters){
        columnName = theColumnName;
        parameterPrefix = theParameterPrefix;
        parameters = theParameters;
    }

    /**
     * Get a SQL query fragment which represents the in clause.
     *
     * @return A SQL query fragment which represents the IN clause.
     */
    public String getQueryFragment(){
        if(parameters.size() == 0){
            return String.format("(%s IN(NULL))", columnName);
        }else{
            final StringBuilder fragment = new StringBuilder(
                    String.format("(%s IN(:%s0)", columnName, parameterPrefix));

            for(int i = 1; i < getNumberOfInClauses(); i++){
                fragment.append(String.format(" OR %s IN(:%s%s)", columnName, parameterPrefix, i));
            }

            fragment.append(")");
            return fragment.toString();
        }
    }

    /**
     * Gets a map which should be used to bind parameters to {@link com.entellitrak.SQLFacade}.
     *
     * @return A map which should be used to bind parameters to {@link com.entellitrak.SQLFacade}
     */
    public Map<String, Object> getParameterMap(){
        final Map<String, Object> parameterMap = new HashMap<>();

        for(int i = 0; i < getNumberOfInClauses(); i++){
            final int startIndex = i * IN_CLAUSE_LIMIT;
            parameterMap.put(parameterPrefix + i, parameters.subList(startIndex,
                    Math.min(startIndex + IN_CLAUSE_LIMIT, parameters.size())));
        }

        return parameterMap;
    }

    /**
     * Get the total number of IN clauses that will exist. May return 0.
     *
     * @return The total number of IN clauses that will exist. May return 0.
     */
    private int getNumberOfInClauses(){
        return (parameters.size() + (IN_CLAUSE_LIMIT - 1)) / IN_CLAUSE_LIMIT;
    }
}

package net.micropact.aea.utility.data;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.micropact.aea.utility.Utility;

/**
 * This class is an experimental class for converting a List of Maps into groups similar to iReports groups.
 * A single {@link ResultGroup} represents a set of objects which are all part of the same group, as in iReports.
 * An entire report therefore uses a List of Report Groups.
 *
 * @author zmiller
 *
 * @param <Identifier> The type of the value of the identifier.
 */
public final class ResultGroup<Identifier>{
    private final Identifier identifier;
    private final List<Map<String, Object>> results;

    /**
     * Construct a new group of results and populate it with the first result record.
     *
     * @param theIdentifier The identifier which this particular group represents
     * @param result The first result in the group. This is part of the constructor because I have decided that each
     *          group must have at least one result.
     */
    private ResultGroup(final Identifier theIdentifier, final Map<String, Object> result){
        identifier = theIdentifier;
        results = new LinkedList<>();
        add(result);
    }

    /**
     * Adds a new result to the group.
     *
     * @param theResult A result
     */
    private void add(final Map<String, Object> theResult){
        results.add(theResult);
    }

    /**
     * Gets the identifier for the group.
     *
     * @return the identifier of the group
     */
    public Identifier getIdentifier(){
        return identifier;
    }

    /**
     * Get the results in the group.
     *
     * @return The results in this group
     */
    public List<Map<String, Object>> getResults(){
        return results;
    }

    /**
     * Converts a query result from etk.createSQL().fetchList() into groups where each item in the group shares the
     * same value for some identifier. This is useful if you want to display query results in velocity but you want all
     * the data grouped such as having a list of users, and then the cases for each user underneath.
     *
     * @param <Identifier> The type of the value of the identifier
     * @param identifierKey They key within the Maps which should be used as the identifier
     * @param results the results which should be broken up into groups.
     * @param identifierClass The Class of the Identifier value
     * @return A List of Groups
     */
    public static <Identifier> List<ResultGroup<Identifier>> buildResultGroups(final String identifierKey,
            final List<Map<String, Object>> results,
            final Class<Identifier> identifierClass){

        final LinkedList<ResultGroup<Identifier>> resultGroups = new LinkedList<>();

        if(!results.isEmpty()){
            Identifier oldIdentifier;
            Map<String, Object> result;

            final Iterator<Map<String, Object>> resultIter = results.iterator();
            result = resultIter.next();
            oldIdentifier = identifierClass.cast(result.get(identifierKey));
            resultGroups.add(new ResultGroup<>(oldIdentifier, result));

            while(resultIter.hasNext()){
                result = resultIter.next();
                final Identifier newIdentifier = identifierClass.cast(result.get(identifierKey));

                if(Utility.equal(newIdentifier, oldIdentifier)){
                    resultGroups.peekLast().getResults().add(result);
                }else{
                    resultGroups.add(new ResultGroup<>(newIdentifier, result));

                    oldIdentifier = newIdentifier;
                }
            }
        }

        return resultGroups;
    }
}

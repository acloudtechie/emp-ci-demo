package net.micropact.aea.core.query;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class contains methods to make it easier to deal with the query results and related functionality by providing
 * methods such as converting the return types of {@link com.entellitrak.SQLFacade#fetchList()} to a regular list.
 *
 * @author zmiller
 */
public final class QueryUtility {

    /**
     * Utility classes do not need public constructors.
     */
    private QueryUtility(){}

    /**
     * This method is used to get around the limitation that
     * {@link com.entellitrak.SQLFacade#setParameter(String, Object)} fails when the Object is an empty list.
     * When passed a list, this method will will either return the list (if the list is not empty),
     * or return a list just containing null, ensuring that calling setParameter with the result of this function
     * should not fail.
     *
     * @param <T> Type of the items in the List
     * @param list The list to be converted to a parameter list
     * @return A List which is either list, or a list containing just null.
     */
    public static <T> List<T> toNonEmptyParameterList(final List<T> list){
        final List<T> returnList;

        if(list.isEmpty()){
            returnList = new LinkedList<>();
            returnList.add(null);
        }else{
            returnList = list;
        }

        return returnList;
    }

    /**
     * Converts the results of {@link com.entellitrak.SQLFacade#fetchList()} to a simple list for the common use-case
     * of selecting only a single column from the database.
     *
     * @param <T> type of the selected column
     * @param queryResults The query results coming from {@link com.entellitrak.SQLFacade#fetchList()}
     * @return A list containing only the first (there should only be one) value from each of the maps.
     */
    public static <T> List<T> toSimpleList(final List<? extends Map<?, ?>> queryResults){
        return queryResults.stream()
                .map(map -> {
                    @SuppressWarnings("unchecked")
                    final T item = (T) map.values().iterator().next();
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * This is a utility method which converts a list of numbers to a list of longs.
     * This is useful in part because we get a lot of {@link java.math.BigDecimal}s back from the database when they
     * actually represent numbers that entellitrak treats as longs in other places.
     *
     * @param numbers list of numbers to be converted
     * @return the converted list of numbers
     */
    public static List<Long> numbersToLongs(final List<? extends Number> numbers){
        return numbers.stream()
                .map(number -> number == null ? null : number.longValue())
                .collect(Collectors.toList());
    }

    /**
     * This is convenience method which can convert the results of an {@link com.entellitrak.SQLFacade#fetchMap()}
     * call which only returns a single column of type number, to a {@link List} of {@link Long}.
     *
     * When calling this method it is your responsibility to make sure that the Maps only contain a single entry and
     * that the value of that entry is a {@link Number}
     *
     * @param queryResults The list of Maps which contains the Longs as values.
     * @return The list of longs
     */
    public static List<Long> mapsToLongs(final List<? extends Map<?, ?>> queryResults){
        return numbersToLongs(QueryUtility.<Number>toSimpleList(queryResults));
    }
}

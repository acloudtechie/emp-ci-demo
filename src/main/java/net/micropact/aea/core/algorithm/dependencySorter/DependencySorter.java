package net.micropact.aea.core.algorithm.dependencySorter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 *  This class contains the algorithm for sorting objects which depend on one another in such a way that all objects
 *  which depend on others, are after the objects it depends on.
 * </p>
 * <p>
 *  It is originally written for the RDO export page in which RDOs have to be exported after any RDOs which it depends
 *  on.
 * </p>
 * <p>
 *  There are a number of optimizations, or changes to the interface which could be made to improve it, however for the
 *  data density and shape of the data (very few, and very short dependency chains) which we currently have, and that
 *  it is currently only used in one place.
 * </p>
 * <p>
 *  Note: For future reference: If we find that there is a performance issue in the algorithm itself and wish to rewrite
 *  it, the name of this general problem is &quot;Topological Sorting&quot; of a &quot;Directed Acyclic Graph&quot;.
 *  We do have to account for cyclic graphs in this algorithm.
 * </p>
 *
 * @author zmiller
 *
 * @param <T> The type of the object which is being sorted
 */
public class DependencySorter <T>{

    private final DependsOn<T> comparator;

    /**
     * Construct a DependencySorter which will sort objects based on a particular comparator.
     *
     * @param dependencyComparator The comparator which determines if one object depends on another.
     */
    public DependencySorter (final DependsOn<T> dependencyComparator){
        comparator = dependencyComparator;
    }

    /**
     * This method sorts a list of items. It returns a new list and does not modify the existing one.
     * The items will be ordered <em>deterministically</em> such that no item depends on an item after it in the
     * returned list, ties will be determined be preserving the order in the input list, and <strong>any items which
     * contain circular dependencies will not be in the returned list</strong>.
     *
     * @param items The items to be sorted
     * @return A sorted list of items where no item in the list depends on an item after it.
     */
    public List<T> sortDependencies(final List<T> items){
        /* The algorithm we are going to use is to have the final list of dependencies, and the list of items still
         * left to add. If the list of items left to process is ever empty, we're done. We will loop over the list of
         * items to be processed, and if it only depends on things already added to our final list, we move it to the
         * final list and restart the processing.*/

        final List<T> returnList = new LinkedList<>();
        final List<T> itemsToBeProcessed = new LinkedList<>(items);

        /* I've added this variable to hopefully make it a little clearer to others what is going on.
         * The keepProcessing variable will indicate whether or not we have moved an item this iteration. It is what
         * prevents us from entering an infinite loop when all the items left to be processed depend on other items
         * still left to be processed. */
        boolean keepProcessing = true;
        while(keepProcessing && itemsToBeProcessed.size() > 0){
            keepProcessing = false;
            for(final T item : itemsToBeProcessed){
                if(!hasAnyDependencies(item, itemsToBeProcessed)){
                    /* All the items dependencies are satisfied, so we moved it from the items to be processed
                     * to the items which have their dependencies satisfied. We will restart at the beginning of the
                     * list.
                     * Restarting (break) at the beginning of the list instead of continuing to process this iteration
                     * is what preserves the order of the input list in the case of ties. */
                    keepProcessing = true;
                    returnList.add(item);
                    itemsToBeProcessed.remove(item);
                    break;
                }
            }
        }

        return returnList;
    }

    /**
     * This method determines whether item depends on any of the items in potentialDependencies.
     *
     * @param item Item we wish to determine whether it has any dependencies
     * @param potentialDependencies Items which item might depend on
     * @return whether item actually depends on any of the potentialDependencies
     */
    public boolean hasAnyDependencies(final T item, final Collection<T> potentialDependencies){
        return potentialDependencies
                .stream()
                .anyMatch(potentialDependency -> comparator.dependsOn(item, potentialDependency));
    }

    /**
     * This interface represents something which can determine whether one item depends on a particular
     * item of the same type.
     *
     * @author zmiller
     *
     * @param <T> The type of the items which have dependencies
     */
    public interface DependsOn<T>{

        /**
         * Determines whether item depends on potentialDependency.
         *
         * @param item Item which might depend on another
         * @param potentialDependency The item that might be depended on
         * @return Whether item depends on potentialDependency
         */
        boolean dependsOn(T item, T potentialDependency);
    }
}

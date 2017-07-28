package net.micropact.aea.core.debug;

import java.io.PrintStream;
import java.util.Map;

import net.micropact.aea.core.wrappedAPIs.MapUtilsWrapper;

/**
 * Utility methods for printing objects for debugging purposes.
 *
 * @author Zachary.Miller
 */
public final class DebugPrint {

    /**
     * Utility classes do not need public constructors.
     */
    private DebugPrint(){}

    /**
     * prints a pretty version of a {@link Map} for debugging purposes.
     *
     * @param printStream the stream to write the prettified map to.
     * @param info the label?
     * @param map the map to print
     */
    public static void printMap(final PrintStream printStream, final String info, final Map<?, ?> map) {
        MapUtilsWrapper.printMap(printStream, info, map);
    }
}

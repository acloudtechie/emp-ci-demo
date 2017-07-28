package net.micropact.aea.core.wrappedAPIs;

import java.io.PrintStream;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

/**
 * Wrapper around {@link MapUtils}.
 *
 * @author Zachary.Miller
 */
public final class MapUtilsWrapper {

    /**
     * Utility classes do not need public constructors.
     */
    private MapUtilsWrapper(){}

    /**
     * prints a pretty version of a {@link Map} for debugging purposes.
     *
     * @param printStream the stream to write the prettified map to.
     * @param info the label?
     * @param map the map to print
     */
    public static void printMap(final PrintStream printStream, final String info, final Map<?, ?> map) {
        MapUtils.debugPrint(printStream, info, map);
    }
}

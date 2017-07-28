package net.micropact.aea.core.utility;

import net.micropact.aea.core.wrappedAPIs.SystemUtilsWrapper;

/**
 * This class contains utility functionality related to the {@link System}.
 *
 * @author Zachary.Miller
 */
public final class SystemUtils {

    /**
     * Utility classes do not need public constructors.
     */
    private SystemUtils(){}

    /**
     * Get the line separator used by the system.
     *
     * @return The Line Separator.
     */
    public static String getLineSeparator() {
        return SystemUtilsWrapper.getLineSeparator();
    }
}

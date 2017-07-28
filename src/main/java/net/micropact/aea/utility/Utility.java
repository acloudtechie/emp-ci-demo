package net.micropact.aea.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.entellitrak.BaseObjectEventContext;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.platform.DatabasePlatform;

import net.micropact.aea.core.cache.AeaCoreConfiguration;
import net.micropact.aea.core.debug.DebugPrint;
import net.micropact.aea.core.exceptionTools.ExceptionUtility;
import net.micropact.aea.core.utility.StringUtils;
import net.micropact.aea.core.utility.SystemUtils;

/**
 * This class contains static functions which will be generally useful throughout entellitrak.
 * @author zmiller
 */
public final class Utility {

    /**
     * Hide default constructor since all methods are static.
     */
    private Utility(){}

    /**
     * This function is used to give java "reader syntax" for creating a Map.
     * @param <K> Type of the keys
     * @param <V> Type of the values
     * @param keyClass The class which the keys of the Map should be an instance of
     * @param valueClass The class which the values of the Map should be an instance of
     * @param array An Nx2 array where the first dimension contains the keys of the Map
     * , and the 2nd dimension contains the Values.
     * @return The Map specified by the input array.
     */
    public static <K, V> Map<K, V> arrayToMap(final Class<K> keyClass,
            final Class<V> valueClass,
            final Object[][] array){
        final Map<K, V> map = new LinkedHashMap<>();
        for(final Object[] item : array){

            if(item.length != 2){
                throw new RuntimeException(
                        String.format("array parameter's 2nd dimension should be 2, instead one of its entries has length of: %s",
                                item.length));
            }

            final K key;
            final V value;

            try{
                key = keyClass.cast(item[0]);
            }catch(final ClassCastException e){
                throw new RuntimeException(
                        String.format("Key does not match requested type. Expected type: %s, Actual type: %s",
                                keyClass.getName(),
                                item[0].getClass().getName()));
            }

            try{
                value = valueClass.cast(item[1]);
            }catch(final ClassCastException e){
                throw new RuntimeException(
                        String.format("Value does not match requested type. Expected type: %s, Actual type: %s",
                                valueClass.getName(),
                                item[1].getClass().getName()));
            }

            map.put(key, value);
        }
        return map;
    }

    /**
     * This function will return true if the database being used by entellitrak is some version of Microsoft SQL Server.
     *
     * @param etk entellitrak execution context
     * @return Whether or not the database is Microsoft SQL Server
     */
    public static boolean isSqlServer(final ExecutionContext etk){
        return etk.getPlatformInfo().getDatabasePlatform() == DatabasePlatform.SQL_SERVER;
    }

    /**
     * This function returns the first non-null entry in values.
     * @param <T> type of values.
     * @param values Possible null values
     * @return The first non-null value in values. If all values are null, then null is returned.
     */
    @SafeVarargs
    public static <T> T nvl(final T... values){
        return Stream.of(values)
                .filter(value -> value != null)
                .findAny()
                .orElse(null);
    }

    /**
     * Returns true if the string is null or the empty String.
     * @param string The string which should be tested.
     * @return Whether the string is null or the empty String.
     */
    public static boolean isBlank(final String string){
        return string == null || "".equals(string);
    }

    /**
     * Checks if 2 objects are equal to each other.
     *
     * @param o1 first object.
     * @param o2 second object.
     * @return Whether o1 and o2 are both null or both equal to eachother.
     */
    public static boolean equal(final Object o1, final Object o2){
        if(o1 == null){
            return o2 == null;
        }else{
            return o1.equals(o2);
        }
    }

    /**
     * Since result.cancelTransaction should always be accompanied by result.addMessage, this function combines the
     * two.
     *
     * @param etk entellitrak execution context.
     * @param message message to be displayed. HTML is NOT escaped.
     */
    public static void cancelTransactionMessage(final BaseObjectEventContext etk, final String message){
        etk.getResult().cancelTransaction();
        etk.getResult().addMessage(message);
    }

    /**
     * AEA convenience logger that will turn on debug logging if an exception occurs.
     *
     * @param etk The execution context.
     * @param anException An Exception.
     */
    public static void aeaLog(final ExecutionContext etk, final Throwable anException) {
        aeaLogPrivate(etk, null, anException, null);
    }

    /**
     * AEA convenience logger that will turn on debug logging if an exception occurs.
     *
     * @param etk The execution context.
     * @param aMessage A custom AE message to include in the log explaining the error.
     */
    public static void aeaLog(final ExecutionContext etk, final String aMessage) {
        aeaLogPrivate(etk, aMessage, null, null);
    }

    /**
     * AEA convenience logger that will turn on debug logging if an exception occurs.
     *
     * @param etk The execution context.
     * @param aMessage A custom AE message to include in the log explaining the error.
     * @param anException An Exception.
     */
    public static void aeaLog(final ExecutionContext etk, final String aMessage, final Throwable anException) {
        aeaLogPrivate(etk, aMessage, anException, null);
    }

    /**
     * AEA convenience logger to print the contents of a map.
     *
     * @param etk The execution context.
     * @param aMessage A custom AE message to include in the log explaining the error.
     * @param anObject An object to print.
     */
    public static void aeaLog(final ExecutionContext etk, final String aMessage, final Object anObject) {
        aeaLogPrivate(etk, aMessage, null, anObject);
    }

    /**
     * AEA convenience logger that will turn on debug logging if an exception occurs.
     *
     * NOTE = this has to be private, otherwise getStackTrace depth will not work.
     *
     * @param etk The execution context.
     * @param aMessage A custom AE message to include in the log explaining the error.
     * @param anException An Exception.
     * @param anObjectToPrint anObjectToPrint
     */
    private static void aeaLogPrivate(final ExecutionContext etk,
            final String aMessage,
            final Throwable anException,
            final Object anObjectToPrint) {

        boolean loggingEnabled;
        boolean advancedRecursiveDebug;

        try {
            loggingEnabled = AeaCoreConfiguration.getAeaWriteDebugToLog(etk);
        } catch (final Throwable t) {
            loggingEnabled = false;
            etk.getLogger().error("Error attempting to determine whether AE Architecture logging is enabled. Make sure that the T_AEA_CORE_CONFIGURATION table exists. "
                    + "AEA Debug logging will remain disabled. " + t.toString());
        }

        //This is a non-documented feature to enable recursive debug logging. Should not be used in production,
        //can result in memory leaks.
        try {
            advancedRecursiveDebug = AeaCoreConfiguration.getAeaAdvancedRecursiveDebug(etk);
        } catch (final Throwable t) {
            advancedRecursiveDebug = false;
        }

        if (loggingEnabled) {
            final StackTraceElement callerClass = new Exception().getStackTrace()[2];

            final StringBuilder sb = new StringBuilder();
            sb.append("\n-----------------------------------------\nAEA_DEBUG (");
            sb.append(callerClass.getClassName());
            sb.append(" ");
            sb.append(callerClass.getMethodName());
            sb.append(": Line #");
            sb.append(callerClass.getLineNumber());
            sb.append(")\n\nUser=\"");

            try {
                sb.append(etk.getCurrentUser().getAccountName());
            } catch (final Throwable t) {
                //If doing this via multi-threading, sometimes session is closed before you can call this.
            }

            sb.append("\"");

            if (aMessage != null) {
                sb.append("\nMessage=\"");
                sb.append(aMessage);
                sb.append("\"");
            }

            if (anException != null) {
                sb.append("\nStack Trace=\n--------------------------------------------------\n");
                sb.append(ExceptionUtility.getFullStackTrace(anException));
            }

            if (anObjectToPrint != null) {
                sb.append("\nData Dump=\n\n-------------- JSON Dump-----------------\n\n");

                try {
                    sb.append(JsonUtilities.encode(anObjectToPrint));
                } catch (final Exception e) {
                    sb.append("Error converting value to JSON");
                }

                sb.append("\n\n-------------- Reflection Dump -----------------\n\n");
                try {
                    if (advancedRecursiveDebug) {
                        sb.append(ReflectionToStringBuilder.toString(anObjectToPrint, RecursiveStringBuilder.getInstance()));
                    } else {
                        sb.append(ReflectionToStringBuilder.toString(anObjectToPrint));
                    }
                } catch (final Exception e) {
                    sb.append("Error converting value to Reflection Dump");
                }
            }

            sb.append("\n\n--------------------------------------------------\n\n");

            etk.getLogger().error(sb.toString());
        }
    }

    /**
     * Modified helper class that overrides the ToStringStyle, based upon an example from
     * http://stackoverflow.com/questions/3149951/java-tostring-tostringbuilder-not-sufficient-wont-traverse.
     */
    private static class RecursiveStringBuilder extends ToStringStyle {


        private static final long serialVersionUID = 2249824815665788260L;
        private static final RecursiveStringBuilder CURRENT_INSTANCE = new RecursiveStringBuilder(10);

        public static ToStringStyle getInstance() {
            return CURRENT_INSTANCE;
        }

        private final int maxRecursionDepth;
        private final String whiteSpaceTab;

        private final ThreadLocal<MutableInteger> depth = new ThreadLocal<MutableInteger>() {
            @Override
            protected MutableInteger initialValue() {
                return new MutableInteger(0);
            }
        };

        /**
         * Construct a recursive string builder with a given maximum depth of recursion.
         *
         * @param maxDepth the maximum depth of recursion
         */
        protected RecursiveStringBuilder(final int maxDepth) {
            this.maxRecursionDepth = maxDepth;
            whiteSpaceTab = StringUtils.repeat("\t", maxDepth);

            setUseShortClassName(true);
            setUseIdentityHashCode(false);
            setContentStart(" {");
            setFieldSeparator(SystemUtils.getLineSeparator());
            setFieldSeparatorAtStart(true);
            setFieldNameValueSeparator(" = ");
            setContentEnd("}");
        }

        private int getDepth() {
            return depth.get().get();
        }

        /**
         * Adds the appropriate padding to (presumably the beginning of a line) of a string buffer. The amount of
         * padding is calculated based on the current recursion depth.
         *
         * @param aStringBuffer buffer to append the padding to
         */
        private void padDepth(final StringBuffer aStringBuffer) {
            aStringBuffer.append(whiteSpaceTab, 0, getDepth());
        }

        /**
         * Appends value to string buffer, however if value contains newline, the newlines will be preserved but the
         * content will be indented appropriately based on the current depth of recursion. This keeps the content
         * left aligned as expected, instead of all the way at the leftmost possible character.
         *
         * @param aStringBuffer the string buffer to append the value to
         * @param value the value to append to the string buffer
         * @return the same string buffer which was passed in
         */
        private StringBuffer appendTabified(final StringBuffer aStringBuffer, final String value) {
            final Matcher matcher = Pattern.compile("\n").matcher(value);
            final String replacement = "\n" + whiteSpaceTab.substring(0, getDepth());
            while (matcher.find()) {
                matcher.appendReplacement(aStringBuffer, replacement);
            }
            matcher.appendTail(aStringBuffer);
            return aStringBuffer;
        }


        @Override
        protected void appendFieldSeparator(final StringBuffer aStringBuffer) {
            aStringBuffer.append(getFieldSeparator());
            padDepth(aStringBuffer);
        }

        @Override
        public void appendStart(final StringBuffer aStringBuffer, final Object object) {
            depth.get().increment();
            super.appendStart(aStringBuffer, object);
        }

        @Override
        public void appendEnd(final StringBuffer aStringBuffer, final Object object) {
            super.appendEnd(aStringBuffer, object);
            aStringBuffer.setLength(aStringBuffer.length() - getContentEnd().length());
            aStringBuffer.append(SystemUtils.getLineSeparator());
            depth.get().decrement();
            padDepth(aStringBuffer);
            appendContentEnd(aStringBuffer);
        }

        @Override
        protected void removeLastFieldSeparator(final StringBuffer aStringBuffer) {
            final int len = aStringBuffer.length();
            final int sepLen = getFieldSeparator().length() + getDepth();
            if (len > 0 && sepLen > 0 && len >= sepLen) {
                aStringBuffer.setLength(len - sepLen);
            }
        }

        /**
         * This method determines whether or not the object needs reflection in order to get good debug information
         * for it. Objects will not need reflection if they are null, in java.lang, or have overridden
         * {@link Object#toString()}
         *
         * @param value the object for which we are determining whether reflection is needed
         * @return whether reflection should be used on the object
         */
        private static boolean noReflectionNeeded(final Object value) {
            try {
                return value != null &&
                        (value.getClass().getName().startsWith("java.lang.")
                                || value.getClass().getMethod("toString").getDeclaringClass() != Object.class);
            } catch (final NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        protected void appendDetail(final StringBuffer aStringBuffer, final String fieldName, final Object value) {
            if (getDepth() >= maxRecursionDepth || noReflectionNeeded(value)) {
                appendTabified(aStringBuffer, String.valueOf(value));
            } else {
                new ReflectionToStringBuilder(value, this, aStringBuffer, null, false, false).toString();
            }
        }

        @Override
        protected void appendDetail(final StringBuffer buffer, final String fieldName, @SuppressWarnings("rawtypes") final Collection coll) {
            Object[] tempArray = coll.toArray();
            buffer.append(ReflectionToStringBuilder.toString(tempArray, this, true, true));
            tempArray = null;
        }

        /**
         * This class represents a mutable integer which can be incremented and decremented in order to keep track
         * of the depth of the recursive string builder.
         *
         * @author MicroPact
         */
        static class MutableInteger {
            private int value;

            /**
             * Construct a mutable integer with a specific initial value.
             *
             * @param theValue the initial value of the integer
             */
            MutableInteger(final int theValue) {
                this.value = theValue;
            }

            /**
             * Gets the value of the integer.
             *
             * @return the value of the integer
             */
            public final int get() {
                return value;
            }

            /**
             * Increments the value of the integer.
             */
            public final void increment() {
                ++value;
            }

            /**
             * Decrements the value of the integer.
             */
            public final void decrement() {
                --value;
            }
        }
    }

    /**
     * Helper method to indicate whether or not the site is configured to use the modern UI.
     * Value is stored in the etk cache for performance reasons.
     *
     * @param etk The execution context.
     * @return true if modern UI, false if classic UI.
     */
    public static boolean isModernUI (final ExecutionContext etk) {

        Integer isModernUI = (Integer) etk.getCache().load("aeaConfigIsModernUI");

        if (isModernUI == null) {
            try {
                isModernUI =
                        etk.createSQL(
                                "select case when current_theme = 'theme.default' then 1 else 0 end as IS_MODERN_THEME "
                                        + " from etk_tracking_config "
                                        + " where tracking_config_id = "
                                        + " (select max(tracking_config_id) from etk_tracking_config_archive) ").fetchInt();

                etk.getCache().store("aeaConfigIsModernUI", isModernUI);
            } catch (final Exception e) {
                Utility.aeaLog(etk, e);
            }
        }

        return (new Integer(1).equals(isModernUI));
    }

    /**
     * Returns the web-pub path for the modern or classic UI depending on the system's current setting.
     *
     * @param etk The Execution context.
     * @return The web-pub folder path.
     */
    public static String getWebPubPath (final ExecutionContext etk) {
        if (isModernUI(etk)) {
            return "themes/default/web-pub";
        } else {
            return "web-pub";
        }
    }

    /**
     * Generates a String representation of a map, including its keys and values which can be used for debugging
     * purposes.
     *
     * @param aMap map to debug
     * @return A String representation of the map
     */
    public static String debugMap (final Map<?, ?> aMap) {
        String mapContents = null;
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            final PrintStream ps = new PrintStream(baos);

            mapContents = "";
            DebugPrint.printMap(ps, "AEA Map Debug", aMap);
            ps.flush();
            mapContents = baos.toString("UTF8");
        }catch(final IOException e){
            // Do Nothing
        }
        return mapContents;
    }

    /**
     * Get the tracking configuration id of the configuration which will be deployed next time apply changes is done.
     * This is the configuration which is visible in the Configuration tab.
     *
     * @param etk entellitrak execution context
     * @return The next tracking config id
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static long getTrackingConfigIdNext(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException {
        return ((Number) etk.createSQL("SELECT tracking_config_id FROM etk_tracking_config WHERE config_version = (SELECT MAX(config_version) FROM etk_tracking_config)")
                .fetchObject()).longValue();

    }

    /**
     * Get the tracking configuration id of the configuration which is currently deployed.
     * This is the configuration that the users are currently interacting with, not the configuration which
     * the AE is interacting with under the configuration tab.
     *
     * @param etk entellitrak execution context
     * @return The currently deployed tracking config id
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static long getTrackingConfigIdCurrent(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException {
        return ((Number) etk.createSQL("SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive")
                .fetchObject()).longValue();

    }

    /**
     * Get the workspace id of the system repository.
     *
     * @param etk entellitrak execution context
     * @return The id of the system repository workspace
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static long getSystemRepositoryWorkspaceId(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException {
        return ((Number) etk.createSQL("select workspace_id from etk_workspace where workspace_name = 'system' and user_id is null")
                .fetchObject()).longValue();
    }

    /**
     * Add an in clause to an SQL string builder that supports more than 1000 records.
     *
     * inObjectList is split into a bracketed set of multiple groups:
     *
     * (columnName in (:inObjectList0-500)
     *  or columnName in (:inObjectList501-1000)
     *  or columnName in (:inObjectList1001-1500))
     *
     *  The resulting SQL is inserted directly into the provided queryBuilder.
     *
     * @param columnName The column name to compare in the in clause.
     * @param queryBuilder The query to insert the in clause into.
     * @param outputParamMap The parameter map that will be passed into the query.
     * @param inObjectList The list of objects to insert into the in(:objects) clause.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void addLargeInClause (final String columnName, final StringBuilder queryBuilder,
            final Map outputParamMap, final List inObjectList) {
        final int groupSize = 1000;
        final String noPeriodColumnName = columnName.replaceAll("\\.", "_");

        queryBuilder.append(" (");

        if ((inObjectList == null) || (inObjectList.size() == 0)) {
            queryBuilder.append(columnName);
            queryBuilder.append(" in (null)");
        } else if (inObjectList.size() == 1) {
            queryBuilder.append(columnName);
            queryBuilder.append(" = :");
            queryBuilder.append(noPeriodColumnName);
            outputParamMap.put(noPeriodColumnName, inObjectList.get(0));
        } else {
            int paramGroup = 0;

            for (int i = 0; i < inObjectList.size(); i=i+groupSize) {
                if ((i + groupSize) < inObjectList.size()) {
                    queryBuilder.append(columnName);
                    queryBuilder.append(" in (:" + noPeriodColumnName + paramGroup + ") OR ");
                    outputParamMap.put(noPeriodColumnName + paramGroup, inObjectList.subList(i, i+groupSize));
                } else {
                    queryBuilder.append(columnName);
                    queryBuilder.append(" in (:" + noPeriodColumnName + paramGroup + ")");
                    outputParamMap.put(noPeriodColumnName + paramGroup, inObjectList.subList(i, inObjectList.size()));
                }
                paramGroup++;
            }
        }

        queryBuilder.append(") ");
    }

    /**
     * Compares two objects using c1.compareTo(c2), however handles the case when either c1 or c2 is null such that
     * nulls are less than any non-null value.
     *
     * @param <C> Type of objects to be compared
     * @param c1 first item to compare
     * @param c2 second item to compare
     * @return A number in the same vein as @link {@link Comparable#compareTo(Object)} but with nulls less than all
     *          non-null values..
     */
    public static <C extends Comparable<C>> int compareNullSafe(final C c1, final C c2){
        final boolean c1IsNull = c1 == null;
        final boolean c2IsNull = c2 == null;
        if(c1IsNull && c2IsNull){
            return 0;
        }else if(c1IsNull){
            return -1;
        }else if(c2IsNull){
            return 1;
        }else{
            return c1.compareTo(c2);
        }
    }

    /**
     * Determines the workspace id that the desired user is using for execution.
     *
     * @param etk entellitrak execution context.
     * @param userId The id of the desired user.
     * @return The Workspace Id of the workspace that the specified user is using for execution.
     * @throws IncorrectResultSizeDataAccessException
     *     If there was an underlying {@link IncorrectResultSizeDataAccessException}.
     */
    public static long getWorkspaceId(final ExecutionContext etk, final Long userId)
            throws IncorrectResultSizeDataAccessException{
        return Long
                .parseLong(etk.createSQL(isSqlServer(etk) ? "SELECT ISNULL((SELECT workspace_id FROM etk_workspace workspace JOIN etk_user u ON u.user_id = workspace.user_id JOIN etk_development_preferences devPreferences ON devPreferences.development_preferences_id = u.development_preferences_id WHERE u.user_id = :userId AND devPreferences.use_system_workspace = 0), (SELECT workspace_id FROM etk_workspace WHERE workspace_name = :defaultWorkspaceName))"
                                                            : " SELECT NVL((SELECT workspace_id FROM etk_workspace workspace JOIN etk_user u ON u.user_id = workspace.user_id JOIN etk_development_preferences devPreferences ON devPreferences.development_preferences_id = u.development_preferences_id WHERE u.user_id = :userId AND devPreferences.use_system_workspace = 0), (SELECT workspace_id FROM etk_workspace WHERE workspace_name = :defaultWorkspaceName)) FROM DUAL ")
                        .setParameter("userId", userId)
                        .setParameter("defaultWorkspaceName", "system")
                        .fetchString());
    }
}
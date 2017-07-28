package net.micropact.aea.core.cache;

import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.core.cache.CacheManager;
import net.micropact.aea.core.enums.AeaCoreConfigurationItem;

/**
 * This class should be used to access values of {@link AeaCoreConfigurationItem}s.
 * This is because this class is strongly typed and will Cache the values.
 *
 * @author zmiller
 */
public final class AeaCoreConfiguration{

    /**
     * This is a Utility class and does not need a constructor.
     */
    private AeaCoreConfiguration(){}

    /**
     * This method will get a Cached Map of {@link AeaCoreConfigurationItem} codes to values.
     * The values will already have been deserialized to the correct type.
     *
     * @param etk entellitrak execution context
     * @return Map
     * @throws ApplicationException If there was an underlying exception.
     */
    private static Map<String, Object> getMap(final ExecutionContext etk) throws ApplicationException{
        return CacheManager.load(etk, new AeaCoreConfigurationCacheable(etk));
    }

    /**
     * This gets the cached, typed value for a particular {@link AeaCoreConfigurationItem}.
     *
     * @param etk entellitrak execution context
     * @param configurationItem {@link AeaCoreConfigurationItem} to get the value for
     * @return The value of the {@link AeaCoreConfigurationItem}
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private static Object getCacheValue(final ExecutionContext etk, final AeaCoreConfigurationItem configurationItem)
            throws ApplicationException{
        return getMap(etk).get(configurationItem.getCode());
    }

    /**
     * This method returns true if static pages should be cached.
     *
     * @param etk entellitrak execution context
     * @return whether static pages should be cached.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean isAeaCacheStaticContentEnabled(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.AEA_CORE_CACHE_STATIC_CONTENT);
    }

    /**
     * This method returns the number of days until emails should be deleted from the Email Queue.
     * Returns null if emails should never be deleted.
     *
     * @param etk entellitrak execution context
     * @return number of days until emails should be deleted from the Email Queue
     * @throws ApplicationException if there was an underlying {@link ApplicationException}
     */
    public static Long getEuDaysUntilDeleteEmailsFromQueue(final ExecutionContext etk)
            throws ApplicationException{
        return (Long) getCacheValue(etk, AeaCoreConfigurationItem.EU_DAYS_UNTIL_DELETE_EMAILS_FROM_QUEUE);
    }

    /**
     * Returns whether sending of Email Utility emails is enabled.
     *
     * @param etk entellitrak execution context
     * @return whether sending of Email Utility emails is enabled.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean isEuEmailEnabled(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.EU_ENABLE_EMAIL);
    }

    /**
     * Returns the number of minutes until the Email Queue will give up resending failed emails.
     *
     * @param etk entellitrak execution context
     * @return the number of minutes until the Email Queue will give up resending failed emails.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static Long getEuMinutesUntilAbortResendingErrors(final ExecutionContext etk)
            throws ApplicationException{
        return (Long) getCacheValue(etk, AeaCoreConfigurationItem.EU_MINUTES_UNTIL_ABORT_RESENDING_ERRORS);
    }

    /**
     * Returns whether or not Enhanced Inbox is enabled.
     *
     * @param etk entellitrak execution context
     * @return whether or not Enhanced Inbox is enabled.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean getDashboardToolsEnhancedInboxEnabled(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.DASHBOARD_TOOLS_ENHANCED_INBOX_ENABLED);
    }

    /**
     * Returns whether the enhanced inbox should group the inboxes into smaller groups.
     *
     * @param etk entellitrak execution context
     * @return whether the inboxes should be grouped
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean getEnhancedInboxUsesGroups(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.ENHANCED_INBOX_USES_GROUPS);
    }

    /**
     * Returns whether the enhanced inbox should show the total number of items in an inbox.
     *
     * @param etk entellitrak execution context
     * @return whether the enhanced inbox should show the total number of items in an inbox
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean getEnhancedInboxShowCount(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.ENHANCED_INBOX_SHOW_COUNT);
    }

    /**
     * Returns whether the enhanced inbox should show the total number of items in an inbox.
     *
     * @param etk entellitrak execution context
     * @return whether all the inboxes should be shown on the dashboard at once
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean getEnhancedInboxShowAllInboxes(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.ENHANCED_INBOX_SHOW_ALL_INBOXES);
    }

    /**
     * Returns the title which should be used for the enhanced inbox.
     *
     * @param etk entellitrak execution context
     * @return the title which should be used for the enhanced inbox
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static String getEnhancedInboxDisplayTitle(final ExecutionContext etk)
            throws ApplicationException{
        return (String) getCacheValue(etk, AeaCoreConfigurationItem.ENHANCED_INBOX_DISPLAY_TITLE);
    }

    /**
     * Returns the number of rows which should appear on a single page of an enhanced inbox grid.
     *
     * @param etk entellitrak execution context
     * @return the number of rows which should appear on a single grid
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static long getEnhancedInboxDisplayLength(final ExecutionContext etk)
            throws ApplicationException{
        return (Long) getCacheValue(etk, AeaCoreConfigurationItem.ENHANCED_INBOX_DISPLAY_LENGTH);
    }

    /**
     * Returns the business key of the custom javascript page which should be used for an inbox decorator.
     *
     * @param etk entellitrak execution context
     * @return the business key of the custom javascript page which should be used for an inbox decorator.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static String getEnhancedInboxCustomJavascript(final ExecutionContext etk)
            throws ApplicationException{
        return (String) getCacheValue(etk, AeaCoreConfigurationItem.ENHANCED_INBOX_CUSTOM_JAVASCRIPT);
    }

    /**
     * Returns the business key of the custom CSS page which should be used for the inbox.
     *
     * @param etk entellitrak execution context
     * @return the business key of the custom css page which should be used for the inbox.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static String getEnhancedInboxCustomCSS(final ExecutionContext etk)
            throws ApplicationException{
        return (String) getCacheValue(etk, AeaCoreConfigurationItem.ENHANCED_INBOX_CUSTOM_CSS);
    }

    /**
     * Returns a space separated list of classes to add to the class attribute of the inbox table.
     *
     * @param etk entellitrak execution context
     * @return a space separated list of classes to add to the class attribute of the inbox table.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static String getEnhancedInboxCustomCssClasses(final ExecutionContext etk)
            throws ApplicationException{
        return (String) getCacheValue(etk, AeaCoreConfigurationItem.ENHANCED_INBOX_CUSTOM_CSS_CLASSSES);
    }

    /**
     * Returns whether ae architecture debugging is enabled.
     *
     * @param etk entellitrak execution context
     * @return whether ae architecture debuging is enabled
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean getAeaWriteDebugToLog(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.AEA_CORE_WRITE_DEBUG_TO_LOG);
    }

    /**
     * Returns whether ae architecture recursive debugging is enabled.
     *
     * @param etk entellitrak execution context
     * @return whether ae architecture recursive debugging is enabled
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean getAeaAdvancedRecursiveDebug(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.AEA_CORE_ADVANCED_RECURSIVE_DEBUG);
    }

    /**
     * Returns whether or not System Wide Broadcast is enabled.
     *
     * @param etk entellitrak execution context
     * @return whether or not System Wide Broadcast is enabled.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean getDashboardToolsSystemWideBroadcastEnabled(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.DASHBOARD_TOOLS_SWB_ENABLED);
    }

    /**
     * Returns the label of the fieldset which contains the current broadcasts.
     *
     * @param etk entellitrak execution context
     * @return the label of the fieldset which contains the current broadcasts
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static String getSystemWideBroadcastLabel(final ExecutionContext etk)
            throws ApplicationException{
        return (String) getCacheValue(etk, AeaCoreConfigurationItem.SWB_LABEL);
    }

    /**
     * Returns whether or not the System Wide Broadcast fieldset should be shown when there are no
     * currently active broadcasts to display.
     *
     * @param etk entellitrak execution context
     * @return whether or not the System Wide Broadcast fieldset should be shown when there are no
     * currently active broadcasts to display.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean getSystemWideBroadcastShowFieldsetWhenEmpty(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.SWB_SHOW_FIELDSET_WHEN_EMPTY);
    }

    /**
     * Returns whether or not the date when the broadcast started appearing should be shown.
     *
     * @param etk entellitrak execution context
     * @return whether or not the date when the broadcast started appearing should be shown.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean getSystemWideBroadcastShowDate(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.SWB_SHOW_DATE);
    }

    /**
     * Returns the message to show when there are no currently active broadcasts.
     *
     * @param etk entellitrak execution context
     * @return the message to show when there are no currently active broadcasts.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static String getSystemWideBroadcastNoBroadcastsMessage(final ExecutionContext etk)
            throws ApplicationException{
        return (String) getCacheValue(etk, AeaCoreConfigurationItem.SWB_NO_BROADCASTS_MESSAGE);
    }

    /**
     * Returns whether or not to paginate the broadcasts.
     *
     * @param etk entellitrak execution context
     * @return whether or not to paginate the broadcasts
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean getSystemWideBroadcastPaginate(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.SWB_PAGINATE);
    }

    /**
     * Returns the height in pixels (Integer) or "dynamic".
     *
     * @param etk entellitrak execution context
     * @return the height in pixels (Integer) or "dynamic"
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static String getSystemWideBroadcastHeight(final ExecutionContext etk)
            throws ApplicationException{
        return (String) getCacheValue(etk, AeaCoreConfigurationItem.SWB_HEIGHT);
    }

    /**
     * Returns the number of broadcasts to be displayed per page when paginating.
     *
     * @param etk entellitrak execution context
     * @return the number of broadcasts to be displayed per page when paginating
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static long getSystemWideBroadcastBroadcastsPerPage(final ExecutionContext etk)
            throws ApplicationException{
        return (Long) getCacheValue(etk, AeaCoreConfigurationItem.SWB_BROADCASTS_PER_PAGE);
    }

    /**
     * Returns the number of months that should not be archived.  Default is 1
     * month (the current month).
     *
     * @param etk entellitrak execution context
     * @return the number of months that should not be archived
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static long getCalendarMonthsNotArchived(final ExecutionContext etk)
            throws ApplicationException{
        return (Long) getCacheValue(etk, AeaCoreConfigurationItem.CALENDAR_MONTHS_NOT_ARCHIVED);
    }

    /**
     * Returns whether or not Calendar is enabled.
     *
     * @param etk entellitrak execution context
     * @return whether or not Calendar is enabled.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean getDashboardToolsCalendarEnabled(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.DASHBOARD_TOOLS_CALENDAR_ENABLED);
    }

    /**
     * Returns the number of months that should not be deleted.  Default is 3
     * month (the current month).
     *
     * @param etk entellitrak execution context
     * @return the number of months that should not be deleted
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static long getCalendarMonthsNotDeleted(final ExecutionContext etk)
            throws ApplicationException{
        return (Long) getCacheValue(etk, AeaCoreConfigurationItem.CALENDAR_MONTHS_NOT_DELETED);
    }

    /**
     * Returns whether the archived events should be displayed on the calendar.
     *
     * @param etk entellitrak execution context
     * @return whether the archived events should be displayed on the calendar
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static boolean getCalendarDisplayArchivedEvents(final ExecutionContext etk)
            throws ApplicationException{
        return (Boolean) getCacheValue(etk, AeaCoreConfigurationItem.CALENDAR_DISPLAY_ARCHIVED_EVENTS);
    }

    /**
     * Returns the width in pixels (Integer) or "max".
     *
     * @param etk entellitrak execution context
     * @return the width in pixels (Integer) or "max"
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static String getCalendarWidth(final ExecutionContext etk)
            throws ApplicationException{
        return (String) getCacheValue(etk, AeaCoreConfigurationItem.CALENDAR_WIDTH);
    }


    /**
     * Returns a list of ; separated TABLE.COLUMN values that are excluded from being detected by the DU
     * Mismatched Column Types utility.
     *
     * @param etk entellitrak execution context
     * @return String containing TABLE.COLUMN;TABLE.COLUMN values to exclude.
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static List<String> getDuMismatchColumnExclusions(final ExecutionContext etk)
            throws ApplicationException{
        @SuppressWarnings("unchecked")
        final List<String> typedValue = (List<String>) getCacheValue(etk,
                AeaCoreConfigurationItem.DU_MISMATCHED_COLUMN_TYPE_EXCLUSIONS);
        return typedValue;
    }
}

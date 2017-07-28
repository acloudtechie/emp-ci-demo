package net.micropact.aea.core.enums;

import net.micropact.aea.core.deserializer.IDeserializer;
import net.micropact.aea.core.deserializer.IdentityDeserializer;
import net.micropact.aea.core.deserializer.LongDeserializer;
import net.micropact.aea.core.deserializer.NewlineTrimmedNoBlanksDeserializer;
import net.micropact.aea.core.deserializer.NotZeroDeserializer;
import net.micropact.aea.core.deserializer.TrueDeserializer;

/**
 * This Enum holds information regarding the AEA CORE Configuration RDO.
 *
 * @author zmiller
 */
public enum AeaCoreConfigurationItem{

    EU_DAYS_UNTIL_DELETE_EMAILS_FROM_QUEUE("eu.daysUntilDeleteEmailsFromQueue", true, new LongDeserializer(null)),
    EU_MINUTES_UNTIL_ABORT_RESENDING_ERRORS("eu.minutesUntilAbortResendingErrors", true, new LongDeserializer(null)),
    EU_ENABLE_EMAIL("eu.enableEmail", true, new NotZeroDeserializer()),

    AEA_CORE_CACHE_STATIC_CONTENT("aea.core.cacheStaticContent", true, new NotZeroDeserializer()),
    AEA_CORE_WRITE_DEBUG_TO_LOG("writeDebugToLog", true, new TrueDeserializer()),
    AEA_CORE_ADVANCED_RECURSIVE_DEBUG("advancedRecursiveDebug", true, new TrueDeserializer()),

    DU_MISMATCHED_COLUMN_TYPE_EXCLUSIONS("du.mismatchedColumnTypeExclusions",
            true, new
            NewlineTrimmedNoBlanksDeserializer()),

    DASHBOARD_TOOLS_ENHANCED_INBOX_ENABLED("dt.enhancedInboxEnabled", true, new TrueDeserializer()),
    DASHBOARD_TOOLS_SWB_ENABLED("dt.systemWideBroadcastEnabled", true, new TrueDeserializer()),
    DASHBOARD_TOOLS_CALENDAR_ENABLED("dt.calendarEnabled", true, new TrueDeserializer()),

    ENHANCED_INBOX_USES_GROUPS("enhancedInbox.usesGroups", true, new TrueDeserializer()),
    ENHANCED_INBOX_SHOW_COUNT("enhancedInbox.showCount", true, new TrueDeserializer()),
    ENHANCED_INBOX_SHOW_ALL_INBOXES("enhancedInbox.showAllInboxes", true, new TrueDeserializer()),
    ENHANCED_INBOX_DISPLAY_TITLE("enhancedInbox.displayTitle", true, new IdentityDeserializer()),
    ENHANCED_INBOX_DISPLAY_LENGTH("enhancedInbox.displayLength", true, new LongDeserializer(20L)),
    ENHANCED_INBOX_CUSTOM_JAVASCRIPT("enhancedInbox.customJavaScript", true, new IdentityDeserializer()),
    ENHANCED_INBOX_CUSTOM_CSS("enhancedInbox.customCSS", true, new IdentityDeserializer()),
    ENHANCED_INBOX_CUSTOM_CSS_CLASSSES("enhancedInbox.customCssClasses", true, new IdentityDeserializer()),

    SWB_LABEL("swb.label", true, new IdentityDeserializer()),
    SWB_SHOW_FIELDSET_WHEN_EMPTY("swb.showFieldsetWhenEmpty", true, new TrueDeserializer()),
    SWB_SHOW_DATE("swb.showDate", true, new TrueDeserializer()),
    SWB_PAGINATE("swb.paginate", true, new TrueDeserializer()),
    SWB_HEIGHT("swb.height", true, new IdentityDeserializer()),
    SWB_BROADCASTS_PER_PAGE("swb.broadcastsPerPage", true, new LongDeserializer(1L)),
    SWB_NO_BROADCASTS_MESSAGE("swb.noBroadcastsMessage", true, new IdentityDeserializer()),

    CALENDAR_MONTHS_NOT_ARCHIVED("calendar.monthsNotArchived", true, new LongDeserializer(1L)),
    CALENDAR_MONTHS_NOT_DELETED("calendar.monthsNotDeleted", true, new LongDeserializer(3L)),
    CALENDAR_DISPLAY_ARCHIVED_EVENTS("calendar.displayArchivedEvents", true, new TrueDeserializer()),
    CALENDAR_WIDTH("calendar.dashboardWidth", true, new IdentityDeserializer());

    private final String configurationCode;
    private final boolean isCacheable;
    private final IDeserializer<?> deserializer;

    /**
     * Simple Constructor.
     *
     * @param code The value of the &quot;Code&quot; element used to identify this Configuration Item
     * @param shouldBeCached whether or not the value of the configuration item should be cached
     * @param theDeserializer Method of deserializing the value stored in the RDO to an actual strongly typed value
     */
    AeaCoreConfigurationItem(final String code,
            final boolean shouldBeCached,
            final IDeserializer<?> theDeserializer){
        configurationCode = code;
        isCacheable = shouldBeCached;
        deserializer = theDeserializer;
    }

    /**
     * The value of the "Code" element used to identify this Configuration Item.
     *
     * @return The value of the "Code" element used to identify this Configuration Item
     */
    public String getCode(){
        return configurationCode;
    }

    /**
     * Whether this configuration item should have its value cached.
     *
     * @return if the {@link AeaCoreConfigurationItem} should have its value cached
     */
    public boolean isCacheable(){
        return isCacheable;
    }

    /**
     * An object which is capable of deserializing the String that comes from the RDO into a more appropriate type.
     *
     * @return an object which is capable of deserializing the String that comes from the RDO into a more appropriate
     * type.
     */
    public IDeserializer<?> getDeserializer(){
        return deserializer;
    }
}

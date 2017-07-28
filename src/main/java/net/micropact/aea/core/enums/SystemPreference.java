package net.micropact.aea.core.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import net.micropact.aea.utility.IJson;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This enum represents the values in the ETK_SYSTEM_PREFERENCE table.
 * The authoritative list seems to come from entellitrak-core/src/main/resources/SystemPreferences.xml
 *
 * @author zmiller
 */
public enum SystemPreference implements IJson{

    /* name, exportable by default?, default production value */
    ADJUST_DAYLIGHT_SAVING("adjustDaylightSaving", true, null),
    ALLOW_ACCESSIBLE_ASSIGNMENT_ROLES_ONLY("allowAccessibleAssignmentRolesOnly", true, null),
    ALLOW_PASSWORD_RESET_FOR_LOCKED_ACCOUNTS("allowPasswordResetForLockedAccounts", false, null),
    ANALYTICS_EMAIL_DOMAIN("analyticsEmailDomain", false, null),
    ANALYTICS_HOST("analyticsHost", false, null),
    ANALYTICS_SECRET("analyticsSecret", false, null),
    ANALYTICS_SSO_ACCOUNT_ID("analyticsSsoAccountId", false, null),
    ANALYTICS_SSO_CLIENT_ID("analyticsSsoClientId", false, null),
    ANALYTICS_SSO_CONFIG_HOST("analyticsSsoConfigHost", false, null),
    ANALYTICS_SSO_DM_ID("analyticsSsoDmId", false, null),
    ANALYTICS_SSO_EMBED_TOKEN("analyticsSsoEmbedToken", false, null),
    ANALYTICS_SSO_SECRET_TOKEN("analyticsSsoSecretToken", false, null),
    ANALYTICS_STATUS_DELAY_INTERVAL("analyticsStatusDelayInterval", false, null),
    ANALYTICS_STATUS_RETRY_COUNT("analyticsStatusRetryCount", false, null),
    ANALYTICS_TOKEN("analyticsToken", false, null),
    AUTO_REMEMBER_SEARCH_CRITERIA("autoRememberSearchCriteria", true, null),
    BLOCK_SIZE_JR_SWAP_FILE("blockSizeJRSwapFile", false, null),
    BUSINESS_DAY_EXPRESSED_IN_HOURS("businessDayExpressedInHours", true, null),
    BUSINESS_MONTH_EXPRESSED_IN_BUSINESS_DAYS("businessMonthExpressedInBusinessDays", true, null),
    BUSINESS_WEEK_EXPRESSED_IN_HOURS("businessWeekExpressedInHours", true, null),
    BUSINESS_YEAR_EXPRESSED_IN_BUSINESS_DAYS("businessYearExpressedInBusinessDays", true, null),
    CALCULATE_LEAP_TIME("calculateLeapTime", true, null),
    CALCULATE_OVERTIME("calculateOvertime", true, null),
    CAPTURE_READ_DATA_EVENTS_IN_AUDIT_LOG("captureReadDataEventsInAuditLog", false, null),
    CASE_SENSITIVE_USERNAME_AUTHENTICATION("caseSensitiveUsernameAuthentication", true, null),
    CURRENCY_PRECISION("currencyPrecision", true, null),
    CURRENCY_SCALE("currencyScale", true, null),
    CURRENT_ETDL_VERSION("currentETDLVersion", false, null),
    CUSTOM_LOGIN_SCREEN_AGREE("customLoginScreenAgree", true, null),
    CUSTOM_LOGIN_SCREEN_HEADER("customLoginScreenHeader", true, null),
    CUSTOM_LOGIN_SCREEN_TEXT("customLoginScreenText", true, null),
    DATA_GRID_COOKIE_AGE("dataGridCookieAge", true, null),
    DATE_FORMAT("dateFormat", true, null),
    DATE_TIME_FORMAT("dateTimeFormat", true, null),
    DEFAULT_MULTI_VALUE_DELIMITER("defaultMultiValueDelimiter", true, null),
    DEFAULT_TEXT_SIZE("defaultTextSize", true, null),
    DEFAULT_TIMESHEET_MANAGER_ROLE("defaultTimesheetManagerRole", true, null),
    DEFAULT_TIME_ZONE("defaultTimeZone", false, null),
    DEPLOY_USING_JBPM_API("deployUsingJbpmAPI", true, null),
    DISABLE_BROWSER_CACHING("disableBrowserCaching", false, null),
    DISABLE_TIMER_CREATION("disableTimerCreation", true, null),
    DISABLE_TIMER_MIGRATION_TOOL("disableTimerMigrationTool", true, null),
    DISPLAY_STATE_ON_SEARCH_FORM("displayStateOnSearchForm", true, null),
    DM_SEARCH_SHOW_LINKS("dmSearchShowLinks", true, null),
    DM_SEARCH_THRESHOLD("dmSearchThreshold", true, null),
    DOC_MGMT_SERVICE_ACCOUNT("docMgmtServiceAccount", false, null),
    DOWNLOAD_FILE_CHUNK_SIZE("downloadFileChunkSize", true, null),
    ENABLE_ADVANCED_SEARCH("enableAdvancedSearch", true, null),
    ENABLE_ADVANCED_SEARCH_LOOKUP_CONTEXT("enableAdvancedSearchLookupContext", true, null),
    ENABLE_ANALYTICS("enableAnalytics", true, null),
    ENABLE_ANTICLICKJACK("enableAnticlickjack", true, null),
    ENABLE_AUTOCOMPLETE_OFF("enableAutocompleteOff", true, null),
    ENABLE_DOCUMENT_MANAGEMENT("enableDocumentManagement", true, null),
    ENABLE_DOC_VERSION_COMPARISON("enableDocVersionComparison", true, null),
    ENABLE_ENDPOINTS("enableEndpoints", true, null),
    ENABLE_EXPORT_TO_CSV("enableExportToCsv", true, null),
    ENABLE_EXPORT_TO_DOC("enableExportToDoc", true, null),
    ENABLE_EXPORT_TO_PDF("enableExportToPdf", true, null),
    ENABLE_EXPORT_TO_XLS("enableExportToXls", true, null),
    ENABLE_FORM_CONTROL_TOOLTIP("enableFormControlTooltip", true, null),
    ENABLE_FORM_PDFPRINTING("enableFormPDFPrinting", true, null),
    ENABLE_HTML_ESCAPING("enableHtmlEscaping", true, null),
    ENABLE_LDAP_AUTHENTICATION("enableLdapAuthentication", false, null),
    ENABLE_LDAP_HIERARCHY("enableLdapHierarchy", false, null),
    ENABLE_LOCAL_AUTHENTICATION("enableLocalAuthentication", false, null),
    ENABLE_MOBILE_INBOX("enableMobileInbox", true, null),
    ENABLE_OAUTH2_AUTHENTICATION("enableOAuth2Authentication", true, null),
    ENABLE_ONLINE_HELP("enableOnlineHelp", true, null),
    ENABLE_ORACLE_CI_SEARCH("enableOracleCISearch", true, null),
    ENABLE_PASSWORD_RESET_FEATURE("enablePasswordResetFeature", true, null),
    ENABLE_PRINT_PERMISSION("enablePrintPermissions", true, null),
    ENABLE_PRINTER_FRIENDLY_FORMAT_AND_PRINT("enablePrinterFriendlyFormatAndPrint", true, null),
    ENABLE_PUBLIC_PAGES("enablePublicPages", true, null),
    ENABLE_QUICK_SEARCH("enableQuickSearch", true, null),
    ENABLE_REDIRECT_ON_SESSION_TIMEOUT("enableRedirectOnSessionTimeout", true, null),
    ENABLE_SAVE_AND_NEW_BUTTON("enableSaveAndNewButton", true, null),
    ENABLE_SEARCH_DETAILS("enableSearchDetails", true, null),
    ENABLE_SEARCH_EXECUTION_LOG("enableSearchExecutionLog", false, null),
    ENABLE_SEARCH_FORM_EVENT_HANDLERS("enableSearchFormEventHandlers", true, null),
    ENABLE_SINGLE_FILE_COMPILATION("enableSingleFileCompilation", true, null),
    ENABLE_SINGLE_RESULT_LOOKUP_CONTEXT("enableSingleResultLookupContext", true, null),
    ENABLE_SINGLE_SIGN_ON("enableSingleSignOn", false, null),
    ENABLE_TIME_ZONE_MODULE("enableTimeZoneModule", true, null),
    ENABLE_VIEW_FILTERS("enableViewFilters", true, null),
    ENABLE_WEB_SERVICES("enableWebServices", true, null),
    ENFORCE_CURRENT_PASSWORD("enforceCurrentPassword", false, "true"),
    ENFORCE_PASSWORD_HISTORY("enforcePasswordHistory", false, "true"),
    ENFORCE_SUBREPORT_PERMISSIONS("enforceSubreportPermissions", true, null),
    ENTELLISQL_ENABLED("entelliSqlEnabled", true, null),
    HELP_INDEX_BASE("helpIndexBase", false, null),
    IGNORE_MISSING_REPORT_FONTS("ignoreMissingReportFonts", true, null),
    INSTRUCTIONS_FIELD_TEXT("instructionsFieldText", true, null),
    JOB_STATUS_UPDATE_INTERVAL("jobStatusUpdateInterval", true, null),
    LDAP_BASE_DN("ldapBaseDN", false, null),
    LDAP_CODE_ATTRIBUTE("ldapCodeAttribute", false, null),
    LDAP_NAME_ATTRIBTUE("ldapNameAttribute", false, null),
    LDAP_PASSWORD("ldapPassword", false, null),
    LDAP_PRINCIPAL("ldapPrincipal", false, null),
    LDAP_URL("ldapUrl", false, null),
    LDAP_USER_ID_ATTRIBUTE("ldapUserIdAttribute", false, null),
    LOAD_CFG_FROM_ETDL("loadCfgFromETDL", true, null),
    LOCAL_DATE_TIME_FORMAT("localDateTimeFormat", true, null),
    MAX_NUM_BACKUP_INDICES("maxNumBackupIndices", false, null),
    MAX_NUM_CHARACTERS_IN_LONG_TEXT_COLUMNS("maxNumCharactersInLongTextColumns", true, null),
    MAX_PAGE_SIZE("maxPageSize", true, null),
    MAX_SIZE_JR_VIRTUAL("maxSizeJRVirtual", false, null),
    MAX_UPLOAD_FILE_SIZE("maxUploadFileSize", false, null),
    MIN_GROW_COUNT_JR_SWAP_FILE("minGrowCountJRSwapFile", false, null),
    MINUTES_LEFT_FOR_TIMEOUT_WARNING("minutesLeftForTimeoutWarning", false, null),
    NUM_PASSWORD_ROTATIONS("numPasswordRotations", false, "10"),
    OAUTH2_ACCESS_TOKEN_VALIDITY("oauth2AccessTokenValidity", true, null),
    OAUTH2_REFRESH_TOKEN_VALIDITY("oauth2RefreshTokenValidity", true, null),
    ON_CANCELED_TRANSACTION_CONTINUE_EVALUATING_SUBSEQUENT_TRANSITIONS("onCanceledTransactionContinueEvaluatingSubsequentTransitions", true, null),
    PASSWORD_EXPIRATION_IN_DAYS("passwordExpirationInDays", false, "90"),
    PASSWORD_FORMAT("passwordFormat", false, null),
    PASSWORD_FORMAT_MESSAGE("passwordFormatMessage", false, null),
    PASSWORD_RESET_EMAIL_TEXT("passwordResetEmailText", false, null),
    PASSWORDS_EXPIRE("passwordsExpire", false, "true"),
    PRESERVE_PREV_CFG("preservePrevCfg", false, null),
    REPORT_OPTION_MESSAGE("reportOptionMessage", true, null),
    RESET_BEANSHELL_INTERPRETER("resetBeanshellInterpreter", true, null),
    RETAIN_ASSOCIATED_FILES_WHEN_TDO_IS_DELETED("retainAssociatedFilesWhenTDOIsDeleted", true, null),
    RETRIEVE_USERS_FROM_LDAP("retrieveUsersFromLdap", false, null),
    SCAN_TOKEN_EXPIRATION_IN_MINUTES("scanTokenExpirationInMinutes", true, null),
    SCHEDULER_INTERVAL("schedulerInterval", true, null),
    SCHEDULER_SERVER("schedulerServer", false, null),
    SEARCH_QUERY_TIMEOUT("searchQueryTimeout", true, null),
    SERVER_TIME_OFFSET("serverTimeOffset", false, null),
    SESSION_TIMEOUT_WARNING_ENABLED("sessionTimeoutWarningEnabled", true, null),
    SHOW_STACK_TRACE("showStackTrace", false, "false"),
    SINGLE_SIGN_ON_HEADER("singleSignOnHeader", false, null),
    SINGLE_SIGN_ON_REDIRECT("singleSignOnRedirect", false, null),
    SINGLE_SIGN_ON_USE_REMOTE_USER("singleSignOnUseRemoteUser", false, null),
    SSO_LOGOUT_REDIRECT_URL("ssoLogoutRedirectUrl", false, null),
    SYSTEM_CLIENT_ID("systemClientId", false, null),
    TIME_LENGTH_FOR_CODE_EXPIRATION("timeLengthForCodeExpiration", true, null),
    TIMESHEET_ON_APPROVE("timesheetOnApprove", true, null),
    TIMESHEET_ON_SAVE("timesheetOnSave", true, null),
    TIMESHEET_ON_SIGN("timesheetOnSign", true, null),
    UPDATE_MODE("updateMode", true, null),
    USE_CUSTOM_LOGIN_SCREEN("useCustomLoginScreen", true, null),
    USE_DATA_GRIDS("useDataGrids", true, null),
    USER_DICTIONARY_ENABLED("userDictionaryEnabled", true, null),
    USE_SIMPLE_WORKFLOW("useSimpleWorkflow", false, null),
    USE_SSO_AUTH_KEY("useSsoAuthKey", false, null);

    private final String name;
    private final boolean exportableByDefault;
    private final String defaultProductionValue;

    /**
     * Simple Constructor.
     *
     * @param theName The name core uses to refer to the System Preference
     * @param isExportableByDefault If all environments should use the same value for the preference
     * @param theDefaultProductionValue The value which should be used in production (or null if there is no recommended
     *          value)
     */
    SystemPreference(final String theName, final boolean isExportableByDefault, final String theDefaultProductionValue){
        name = theName;
        exportableByDefault = isExportableByDefault;
        defaultProductionValue = theDefaultProductionValue;
    }

    /**
     * Get the name which core uses for the System Preference.
     *
     * @return The name core uses for the System Preference
     */
    public String getName(){
        return name;
    }

    /**
     * Is the System Preference one which should be the same in all environments in a project.
     *
     * @return Whether this preference is one which should be exported by default. Many values stored in the
     * ETK_SYSTEM_PREFERENCE table (such as analytics) should have different values in development and production
     */
    public boolean isExportable(){
        return exportableByDefault;
    }

    /**
     * Returns the default value recommended for production. For instance showStackTrace's would return "true".
     * If there is no recommended default for production, this method returns null.
     *
     * @return The recommended preference or null if there is no recommendation
     */
    public String getDefaultProductionValue(){
        return defaultProductionValue;
    }

    /**
     * Gets a System Preference by its name.
     *
     * @param name The name of the preference to find
     * @return The System Preference
     */
    public static SystemPreference getPreferenceByName(final String name){
        return Arrays.stream(values())
                .filter(preference -> preference.getName().equals(name))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Could not find system preference with name \"%s\"",
                                name)));
    }

    /**
     * Get all System Preferences which have a recommended value in project systems.
     *
     * @return All System Preferences which have a recommended production value.
     */
    public static Set<SystemPreference> getProductionSystemPreferences(){
        return Arrays.stream(values())
                .filter(preference -> null != preference.getDefaultProductionValue())
                .collect(Collectors.toSet());
    }

    /**
     * Get a list of system preferences which should be the same among all environments.
     *
     * @return All System Preferences which are exportable by default.
     */
    public static Set<SystemPreference> getExportableByDefaultPreferences(){
        return Arrays.stream(values())
                .filter(SystemPreference::isExportable)
                .collect(Collectors.toSet());
    }

    @Override
    public String encode() {
        return JsonUtilities.encode(Utility.arrayToMap(String.class, Object.class, new Object[][]{
            {"name", getName()},
            {"defaultProductionValue", getDefaultProductionValue()},
            {"isExportableByDefault", isExportable()},
        }));
    }
}

package net.micropact.aea.core.dao;

import java.util.Map;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

/**
 * This class is a DAO for the core entellitrak report object (those found in ETK_SAVED_REPORT).
 *
 * @author zmiller
 */
public class SavedReport{

    private final ExecutionContext etk;
    private final long reportId;
    private final String businessKey;
    private final String name;
    private final String username;

    /**
     * Constructs a new Saved Report with the given report id.
     *
     * @param executionContext entellitrak execution context
     * @param theReportId The saved report id
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    SavedReport(final ExecutionContext executionContext, final long theReportId)
            throws IncorrectResultSizeDataAccessException{
        etk = executionContext;
        reportId = theReportId;

        final Map<String, Object> reportInfo = etk.createSQL("SELECT report.BUSINESS_KEY, report.NAME, u.USERNAME FROM etk_saved_report report LEFT JOIN etk_user u ON u.user_id = report.user_id WHERE report.saved_report_id = :reportId")
                .setParameter("reportId", reportId)
                .fetchMap();

        businessKey = (String) reportInfo.get("BUSINESS_KEY");
        name = (String) reportInfo.get("NAME");
        username = (String) reportInfo.get("USERNAME");
    }

    /**
     * Gets the business key of the report.
     *
     * @return The business key of the report
     */
    public String getBusinessKey() {
        return businessKey;
    }

    /**
     * Gets the name of the report.
     *
     * @return The name of the report
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the username of the user who owns the report.
     *
     * @return The name of the user who owns the report
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the XML content of the report.
     *
     * @return The XML content of the report
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public String getReport() throws IncorrectResultSizeDataAccessException {
        return (String) etk.createSQL("SELECT REPORT FROM etk_saved_report WHERE saved_report_id = :reportId")
                .setParameter("reportId", reportId)
                .fetchObject();
    }

    /**
     * Service to access {@link SavedReport}s.
     * We should have a separate service package, but for now I am putting the service here.
     *
     * @author zmiller
     */
    public static class ReportService{

        /**
         * Loads the {@link SavedReport} with a particular saved report id.
         *
         * @param etk entellitrak execution context
         * @param reportId The saved report id
         * @return The Report
         * @throws IncorrectResultSizeDataAccessException
         *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
         */
        public static SavedReport loadReportById(final ExecutionContext etk, final long reportId)
                throws IncorrectResultSizeDataAccessException{
            return new SavedReport(etk, reportId);
        }

        /**
         * Loads the {@link SavedReport} with a particular business key.
         *
         * @param etk entellitrak execution context
         * @param businessKey business key of the report
         * @return The Report
         * @throws IncorrectResultSizeDataAccessException
         *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
         */
        public static SavedReport loadReportByBusinessKey(final ExecutionContext etk, final String businessKey) throws IncorrectResultSizeDataAccessException{
            final long reportId = ((Number) etk.createSQL("SELECT SAVED_REPORT_ID FROM etk_saved_report report WHERE report.business_key = :reportBusinessKey")
                    .setParameter("reportBusinessKey", businessKey)
                    .fetchObject()).longValue();

            return loadReportById(etk, reportId);
        }
    }
}
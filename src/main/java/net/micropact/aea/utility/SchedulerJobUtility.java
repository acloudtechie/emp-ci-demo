package net.micropact.aea.utility;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;

import net.micropact.aea.core.enums.SchedulerJobType;
import net.micropact.aea.core.utility.StringEscapeUtils;


/**
 * This class contains utility methods for dealing with Scheduler Jobs.
 * @author zmiller
 */
public final class SchedulerJobUtility {

    private static final long MINUTES_BEFORE_WARNING = 30;

    /** There is no reason to create a new SchedulerJobUtility. */
    private SchedulerJobUtility(){}

    /**
     * This method makes a best-guess as to whether there are errors with any of the Scheduler Jobs.
     * Currently it looks at active jobs that have an exception,
     * or have a next run time of 30 minutes or more in the past.
     * @param etk entellitrak execution context
     * @return whether there are likely scheduler job errors
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    public static boolean areThereLikelySchedulerJobErrors(final ExecutionContext etk) {
        try {
            return 0 < etk.createSQL(Utility.isSqlServer(etk) ? "SELECT COUNT(*) FROM etk_job etkJob WHERE etkJob.active = 1 AND (etkJob.exception IS NOT NULL OR etkJob.next_run_on < DATEADD(MINUTE, -1 * :minutesLate, DBO.ETKF_GETSERVERTIME()))"
                            : "SELECT COUNT(*) FROM etk_job etkJob WHERE etkJob.active = 1 AND (etkJob.exception IS NOT NULL OR etkJob.next_run_on < ETKF_GETSERVERTIME() - 1/24/60 * :minutesLate)")
                    .setParameter("minutesLate", MINUTES_BEFORE_WARNING)
                    .fetchInt();
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method makes a best-guess as to determining scheduler jobs which have errors.
     * Currently it looks at active jobs that have an exception,
     * or have a next run time of 30 minutes or more in the past.
     *
     * @param etk entellitrak execution context
     * @return List of scheduler jobs which contain errors
     */
    public static List<SchedulerJobError> getLikelySchedulerJobErrors(final ExecutionContext etk){
        final List<SchedulerJobError> returnList = new LinkedList<>();

        for(final Map<String, Object> jobInfo : etk.createSQL(
                Utility.isSqlServer(etk) ? "SELECT * FROM ( SELECT etkJob.JOB_ID, etkJob.BUSINESS_KEY, CASE WHEN EXISTS ( SELECT * FROM etk_job_custom jobCustom WHERE jobCustom.job_custom_id = etkJob.job_id ) THEN 'CUSTOM' WHEN EXISTS ( SELECT * FROM etk_job_system jobSystem WHERE jobSystem.job_system_id = etkJob.job_id ) THEN 'SYSTEM' END JOB_TYPE, etkJob.name JOB_NAME, CASE WHEN etkJob.exception IS NOT NULL THEN 1 ELSE 0 END HAS_ERROR, CASE WHEN etkJob.next_run_on < DATEADD(MINUTE, -1 * :minutesLate, DBO.ETKF_GETSERVERTIME()) THEN 1 ELSE 0 END IS_LATE FROM etk_job etkJob WHERE etkJob.active = 1 ) allJobs WHERE allJobs.HAS_ERROR = 1 OR allJobs.IS_LATE = 1 ORDER BY JOB_TYPE, JOB_name, JOB_ID"
                                           : "SELECT * FROM (SELECT etkJob.JOB_ID, etkJob.BUSINESS_KEY, CASE WHEN EXISTS (SELECT * FROM etk_job_custom jobCustom WHERE jobCustom.job_custom_id = etkJob.job_id ) THEN 'CUSTOM' WHEN EXISTS (SELECT * FROM etk_job_system jobSystem WHERE jobSystem.job_system_id = etkJob.job_id ) THEN 'SYSTEM' END JOB_TYPE, etkJob.name JOB_NAME, CASE WHEN etkJob.exception IS NOT NULL THEN 1 ELSE 0 END HAS_ERROR, CASE WHEN etkJob.next_run_on < ETKF_GETSERVERTIME() - 1/24/60 * :minutesLate THEN 1 ELSE 0 END IS_LATE FROM etk_job etkJob WHERE etkJob.active = 1 ) allJobs WHERE allJobs.HAS_ERROR = 1 OR allJobs.IS_LATE = 1 ORDER BY JOB_TYPE, JOB_name, JOB_ID")
                .setParameter("minutesLate", MINUTES_BEFORE_WARNING)
                .fetchList()){
            returnList.add(new SchedulerJobError(
                    (String) jobInfo.get("BUSINESS_KEY"),
                    SchedulerJobType.valueOf((String) jobInfo.get("JOB_TYPE")),
                    (String) jobInfo.get("JOB_NAME"),
                    1 == ((Number) jobInfo.get("IS_LATE")).longValue(),
                    1 == ((Number) jobInfo.get("HAS_ERROR")).longValue()));
        }

        return returnList;
    }

    /**
     * This method converts scheduler job errors to an HTML representation.
     *
     * @param schedulerJobErrors The list of errors to display as HTML
     * @param includeHyperlinks Whether or not to include hyperlinks in the HTML. Since hyperlinks are relative,
     *          you will want this option to be false for emails.
     * @return An HTML representation of the errors
     */
    public static String convertErrorsToHTML(final List<SchedulerJobError> schedulerJobErrors,
            final boolean includeHyperlinks) {
        final StringBuilder headerBuilder = new StringBuilder();
        final StringBuilder bodyBuilder = new StringBuilder();

        for(final String header : new String[]{"Type", "Name", "Has Error", "Is Late"}){
            headerBuilder.append(String.format("<th>%s</th>", StringEscapeUtils.escapeHtml(header)));
        }

        for(final SchedulerJobError schedulerJobError : schedulerJobErrors){
            bodyBuilder.append(String.format("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>",
                    StringEscapeUtils.escapeHtml(schedulerJobError.getSchedulerJobType().toString()),
                    includeHyperlinks ?
                                       String.format("<a href='admin.job.status.request.do?job=%s' target='_blank'>%s</a>",
                                               StringEscapeUtils.escapeHtml(schedulerJobError.getJobBusinessKey()),
                                               StringEscapeUtils.escapeHtml(schedulerJobError.getName()))
                                       : StringEscapeUtils.escapeHtml(schedulerJobError.getName()),
                    StringEscapeUtils.escapeHtml(schedulerJobError.hasError() ? "Yes" : "No"),
                    StringEscapeUtils.escapeHtml(schedulerJobError.isLate() ? "Yes" : "No")));
        }

        return String.format("<table class='grid'><thead><tr>%s</tr></thead><tbody>%s</tbody></table>",
                headerBuilder.toString(),
                bodyBuilder.toString());
    }

    /**
     * This class represents a particular scheduler job which has an error.
     *
     * @author zmiller
     */
    public static class SchedulerJobError{

        private final String jobBusinessKey;
        private final SchedulerJobType jobType;
        private final String name;
        private final boolean isLate;
        private final boolean hasError;

        /**
         * Constructor.
         *
         * @param theJobBusinessKey The business key of the job
         * @param theJobType The type of the scheduler job
         * @param theName The name of the scheduler job
         * @param jobIsLate Whether the scheduler job is late
         * @param jobHasError Whether the scheduler job has an error
         */
        SchedulerJobError(final String theJobBusinessKey, final SchedulerJobType theJobType, final String theName,
                final boolean jobIsLate, final boolean jobHasError){
            jobBusinessKey = theJobBusinessKey;
            jobType = theJobType;
            name = theName;
            isLate = jobIsLate;
            hasError = jobHasError;
        }

        /**
         * Gets the business key of the job.
         *
         * @return The business key of the job
         */
        public String getJobBusinessKey(){
            return jobBusinessKey;
        }

        /**
         * Gets whether the job is late.
         *
         * @return Whether the job is late
         */
        public boolean isLate(){
            return isLate;
        }

        /**
         * Gets whether the job has an exception.
         *
         * @return Whether the job has an exception
         */
        public boolean hasError(){
            return hasError;
        }

        /**
         * Gets the name of the job.
         *
         * @return The name of the job
         */
        public String getName(){
            return name;
        }

        /**
         * Get the type of the scheduler job.
         *
         * @return The type of scheduler job
         */
        public SchedulerJobType getSchedulerJobType(){
            return jobType;
        }
    }
}

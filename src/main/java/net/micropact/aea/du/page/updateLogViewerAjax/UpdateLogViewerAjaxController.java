package net.micropact.aea.du.page.updateLogViewerAjax;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.localization.Localizations;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.enums.UpdateLogStatus;
import net.micropact.aea.utility.JsonUtilities;

/**
 * This is the controller code for a page which displays information regarding the entellitrak Apply Changes Update log.
 *
 * @author zmiller
 */
public class UpdateLogViewerAjaxController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {

        final String limitParameter = etk.getParameters().getSingle("limit");
        final Integer limit = toInt(limitParameter);

        final TextResponse response = etk.createTextResponse();

        response.setContentType(ContentType.JSON);

        final List<Map<String, Object>> logEntries = etk.createSQL("SELECT * FROM( SELECT updateLog.UPDATE_LOG_ID, updateLog.STATUS, updateLog.DESCRIPTION, updateLog.END_TIMESTAMP, updateLog.START_TIMESTAMP, updateLog.IP_ADDRESS, u.USERNAME, ROW_NUMBER() OVER (ORDER BY start_timestamp DESC, update_log_id DESC) num FROM etk_update_log updateLog LEFT JOIN etk_user u ON u.user_id = updateLog.user_id ) updateLog WHERE :limit IS NULL OR updateLog.num <= :limit ORDER BY updateLog.update_log_id DESC ")
                .setParameter("limit", limit)
                .fetchList();

        for(final Map<String, Object> logEntry : logEntries){
            final Date startTime = (Date) logEntry.get("START_TIMESTAMP");
            final Date endTime = (Date) logEntry.get("END_TIMESTAMP");

            logEntry.put("START_TIMESTAMP", startTime.getTime());
            logEntry.put("END_TIMESTAMP", endTime.getTime());

            logEntry.put("startTimeStampDisplay", formatTime(etk, startTime));
            logEntry.put("endTimeStampDisplay", formatTime(etk, endTime));

            logEntry.put("updateLogStatusDisplay",
                    UpdateLogStatus.getFormattedString((String) logEntry.get("STATUS")));

            logEntry.put("duration", endTime.getTime() - startTime.getTime());
        }

        response.put("out", JsonUtilities.encode(logEntries));

        return response;
    }

    /**
     * Converts a String to an Integer. Returns null if the String does not represent an Integer.
     *
     * @param str String to be converted
     * @return The integer
     */
    private static Integer toInt(final String str){
        try{
            return Integer.parseInt(str);
        }catch(final NumberFormatException e){
            return null;
        }
    }

    /**
     * Display a timestamp in the format preferred by the current user.
     *
     * @param etk entellitrak execution context
     * @param date date to be formatted
     * @return Localized display of the time for the current user
     */
    private static String formatTime(final ExecutionContext etk, final Date date){
        return Localizations.toLocalTimestamp(etk.getCurrentUser().getTimeZonePreference(), date)
                .getTimestampString();
    }
}

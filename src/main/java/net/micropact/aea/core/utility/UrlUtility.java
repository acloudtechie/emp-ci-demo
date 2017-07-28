package net.micropact.aea.core.utility;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Optional;

import com.entellitrak.configuration.DataObject;
import com.entellitrak.configuration.ObjectType;

import net.micropact.aea.utility.Utility;

/**
 * Utility class for generating URLs to entellitrak screens.
 *
 * @author Zachary.Miller
 */
public final class UrlUtility {

    /**
     * Utility classes do not need public constructors.
     */
    private UrlUtility(){}

    /**
     * Get the relative URL for linking to a specific data object within entellitrak.
     *
     * @param dataObject the data object
     * @param trackingId the tracking id
     * @return the relative URL for accessing the object directly within entellitrak
     */
    public static String urlForDataObject(final DataObject dataObject, final long trackingId) {
        try {
            final String baseUrl = Optional.of(
                    Utility.arrayToMap(ObjectType.class, String.class, new Object[][]{
                        {ObjectType.TRACKING, "workflow.do"},
                        {ObjectType.REFERENCE, "admin.refdata.update.request.do"},
                        {ObjectType.ESCAN, "escan.data.update.request.do"}
                    }).get(dataObject.getObjectType()))
            .get();

            return String.format("%s?dataObjectKey=%s&trackingId=%s",
                    baseUrl,
                    URLEncoder.encode(dataObject.getBusinessKey(), "UTF-8"),
                    URLEncoder.encode(String.valueOf(trackingId), "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}

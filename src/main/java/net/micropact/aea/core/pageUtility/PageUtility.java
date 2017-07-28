package net.micropact.aea.core.pageUtility;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.page.Response;

import net.micropact.aea.core.cache.AeaCoreConfiguration;

/**
 * Class for providing utility functionality for pages. Once we get enough functionality, it should make sense to split
 * this class into smaller pieces.
 *
 * @author zmiller
 */
public final class PageUtility {

    /**
     * Utility classes do not need constructors.
     */
    private PageUtility(){}

    /**
     * This method checks to see whether caching has been enabled in the AEA Core Configuration.
     * If it has, it will set headers on the Response so that the page response is cached by browsers.
     *
     * @param etk entellitrak execution context
     * @param response response to set the headers on
     * @throws ApplicationException
     *          If there was an underlying {@link ApplicationException}
     */
    public static void setAEACacheHeaders(final ExecutionContext etk, final Response response)
            throws ApplicationException{
        if(AeaCoreConfiguration.isAeaCacheStaticContentEnabled(etk)){
            /* Set to expire in 1 day. This is long enough to make a huge difference, but short enough that if upgrades
             * happen during the weekend, that users will have more than enough time to get the new version when they
             * come in on Monday. */
            response.setHeader("Cache-Control", "public, max-age=86400");
            response.setHeader("Pragma", "");
        }
    }
}

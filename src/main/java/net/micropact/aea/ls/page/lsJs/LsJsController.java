/**
 *
 * LsJsController
 *
 * administrator 09/15/2014
 **/

package net.micropact.aea.ls.page.lsJs;

import net.micropact.aea.utility.Utility;

import com.entellitrak.ApplicationException;
import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;
import com.entellitrak.PageExecutionContext;

/**
 * Javascript for the live search component.
 *
 * @author aclee
 */
public class LsJsController implements PageController {
	private static final int MIN_CHARACTERS_NEEDED = 1;
	private static final int MAX_CHARACTERS_NEEDED = 10;
	private static final int DEFAULT_CHARACTERS_NEEDED = 3;

    @Override
	public Response execute(final PageExecutionContext etk) throws ApplicationException {

    	final TextResponse tr = etk.createTextResponse();
        tr.setContentType(ContentType.JAVASCRIPT);
        tr.put("web_pub_path", Utility.getWebPubPath(etk));

        //Load parameter ls.charactersNeededForSearch from cache.
		Integer charactersNeeded = (Integer) etk.getCache().load("ls.charactersNeededForSearch");

		//If it could not be found, load it from DB and store it in the cache.
		if (charactersNeeded == null) {
			try {
				final String charactersNeededString =
						etk.createSQL("select c_value from T_AEA_CORE_CONFIGURATION where c_code = 'ls.charactersNeededForSearch'")
						.fetchString();

				if (StringUtility.isNotBlank(charactersNeededString)) {
					charactersNeeded = new Integer(charactersNeededString);

					if ((charactersNeeded < MIN_CHARACTERS_NEEDED) || (charactersNeeded > MAX_CHARACTERS_NEEDED)) {
						charactersNeeded = DEFAULT_CHARACTERS_NEEDED;
					}
				} else {
					Utility.aeaLog(etk, "No configuration value in T_AEA_CORE_CONFIGURATION for "
					          + " ls.charactersNeededForSearch, defaulting to " + DEFAULT_CHARACTERS_NEEDED
					          + " characters.");
					charactersNeeded = DEFAULT_CHARACTERS_NEEDED;
				}
			} catch (final Exception e) {
				Utility.aeaLog(etk, "No configuration value in T_AEA_CORE_CONFIGURATION for "
						          + " ls.charactersNeededForSearch, defaulting to " + DEFAULT_CHARACTERS_NEEDED
						          + " characters.");
				charactersNeeded = DEFAULT_CHARACTERS_NEEDED;
			}

			etk.getCache().store("ls.charactersNeededForSearch", charactersNeeded);
		}

		//Load debug parameter ls.debugMode from cache.
		String debugMode = (String) etk.getCache().load("ls.debugMode");

		//If it could not be found, load it from DB and store it in the cache.
		if (debugMode == null) {
			try {
				debugMode =
						etk.createSQL("select c_value from T_AEA_CORE_CONFIGURATION where c_code = 'ls.debugMode'")
						.returnEmptyResultSetAs("false")
						.fetchString();

				if ("t".equalsIgnoreCase(debugMode) ||
					"true".equalsIgnoreCase(debugMode) ||
					"1".equalsIgnoreCase(debugMode)) {
					debugMode = "true";
				} else {
					debugMode = "false";
				}
			} catch (final Exception e) {
				Utility.aeaLog(etk, "No configuration value in T_AEA_CORE_CONFIGURATION for "
						          + " ls.debugMode, defaulting to \"false\".");
				debugMode = "false";
			}

			etk.getCache().store("ls.debugMode", debugMode);
		}

        tr.put("charactersNeededForSearch", charactersNeeded);
        tr.put("debugMode", debugMode);
        tr.put("isAccessibilityEnhanced", etk.getCurrentUser().getProfile().isAccessibilityEnhanced());

        //Cache this page on the users machine.
        tr.setHeader("Cache-Control", "public, max-age=86400");
        tr.setHeader("Pragma", "");
        return tr;
    }

}

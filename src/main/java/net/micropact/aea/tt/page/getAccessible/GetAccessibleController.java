package net.micropact.aea.tt.page.getAccessible;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.query.Coersion;
import net.micropact.aea.utility.JsonUtilities;

/**
 * This page controller is for indicating whether the current user has entellitrak accessibility enhancements turned on.
 *
 * @author zmiller
 */
public class GetAccessibleController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {
        try {
            final TextResponse response = etk.createTextResponse();
            response.setContentType(ContentType.JSON);
            response.put("out", JsonUtilities.encode(1 == Coersion.toLong(etk.createSQL("SELECT accessibility_enhanced FROM etk_user WHERE user_id = :userId")
                    .setParameter("userId", etk.getCurrentUser().getId())
                    .fetchObject())));
            return response;
        } catch (final Exception e) {
            throw new ApplicationException(e);
        }
    }
}

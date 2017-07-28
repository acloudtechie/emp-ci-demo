package net.micropact.aea.auditLog.page.getObjectLocationAjaxController;

import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.configuration.DataObject;
import com.entellitrak.dynamic.DataObjectInstance;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.utility.DynamicObjectConfigurationUtils;
import net.micropact.aea.core.utility.UrlUtility;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This serves as the controller of an entellitrak page which will be given a data object business key and tracking id
 * and will provide information necessary to access the object via hyperlink.
 *
 * This is needed because different data object types have different URLs.
 *
 * @author Zachary.Miller
 */
public class GetObjectLocationAjaxController implements PageController{

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {
            final String dataObjectKey = etk.getParameters().getSingle("dataObjectKey");
            final long trackingId = Long.valueOf(etk.getParameters().getSingle("trackingId"));

            final TextResponse response = etk.createTextResponse();
            response.setContentType(ContentType.JSON);

            final Map<String, Object> output;

            final DataObject dataObject = etk.getConfigurationService().loadDataObject(dataObjectKey);

            final DataObjectInstance dataObjectInstance = etk.getDynamicObjectService()
                    .get(DynamicObjectConfigurationUtils.getDynamicClass(etk, dataObjectKey),
                            trackingId);

            if(dataObjectInstance == null){
                output = Utility.arrayToMap(String.class, Object.class, new Object[][]{
                    {"type", "not_found"}
                });
            }else{
                output = Utility.arrayToMap(String.class, Object.class, new Object[][]{
                    {"type", "found"},
                    {"url", UrlUtility.urlForDataObject(dataObject, trackingId)}
                });
            }

            response.put("out", JsonUtilities.encode(output));

            return response;
        } catch (final ClassNotFoundException e) {
            throw new ApplicationException(e);
        }
    }
}

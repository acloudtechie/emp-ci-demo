/**
 *
 * Page to accept parameters for the Complaint Report and run the report.
 *
 * administrator 05/23/2017
 **/

package com.mptraining.refapp.common.report;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.lookup.For;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;
import com.mptraining.refapp.common.dao.EtkTablesDao;


public class ComplaintReportController implements PageController {

    @Override
	public Response execute(PageExecutionContext etk) throws ApplicationException {

    	TextResponse response = etk.createTextResponse();

		response.put("reportId", EtkTablesDao.getReportIdByBusinessKey(etk, "report.administrator.complaintsMasterReport"));
		response.put("sacList", etk.getLookupService().getLookup("lookup.sacUsers").execute(For.TRACKING));
		response.put("statusList", etk.getLookupService().getLookup("lookup.status").execute(For.TRACKING));
		response.setContentType(ContentType.HTML);

		return response;

    }

}

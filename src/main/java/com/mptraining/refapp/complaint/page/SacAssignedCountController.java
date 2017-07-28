/**
 *
 * Returns a count of the number of Complaints that currently has the given SAC assigned to it.
 *
 * administrator 10/06/2016
 **/

package com.mptraining.refapp.complaint.page;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;
import com.entellitrak.PageExecutionContext;
import com.mptraining.refapp.cmpcomplaint.dao.CmpComplaintDao;
import com.mptraining.refapp.complaint.dao.ComplaintDao;

public class SacAssignedCountController implements PageController {

    @Override
	public Response execute(PageExecutionContext etk) throws ApplicationException {

    	TextResponse response = etk.createTextResponse();
        Long sacId = Long.parseLong(etk.getParameters().getSingle("sacId"));
        Boolean isCmp = new Boolean(etk.getParameters().getSingle("isCmp"));

		try {
			Integer count = 0;
			if(isCmp){
				count = CmpComplaintDao.countOpenComplaintsAssignedToSac(etk, sacId);
			}
			else{
				count = ComplaintDao.countOpenComplaintsAssignedToSac(etk, sacId);
			}
			response.put("out", count);
	        return response;
		} catch (IncorrectResultSizeDataAccessException e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}

    }

}

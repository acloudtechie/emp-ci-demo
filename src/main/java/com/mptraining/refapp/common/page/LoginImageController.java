/**
 *
 * Serves up the login page logo image
 *
 * administrator 09/30/2016
 **/

package com.mptraining.refapp.common.page;

import com.entellitrak.ApplicationException;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.dynamic.SystemInformation;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.mptraining.refapp.rdo.dao.RdoDao;

public class LoginImageController implements PageController {

    @Override
	public Response execute(PageExecutionContext etk) throws ApplicationException {

		try {
			SystemInformation sysInfo = etk.getDynamicObjectService().get(SystemInformation.class, RdoDao.getIdFromCode(etk, "T_SYSTEM_INFORMATION", "loginLogo").longValue());
			return etk.createFileResponse(sysInfo.getFile());
		} catch (IncorrectResultSizeDataAccessException e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}

    }

}

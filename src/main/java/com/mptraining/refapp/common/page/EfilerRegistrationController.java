/**
 *
 * Controller for eFiler registration page
 *
 * administrator 10/19/2016
 **/

package com.mptraining.refapp.common.page;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.EmailValidator;

import com.entellitrak.ApplicationException;
import com.entellitrak.InputValidationException;
import com.entellitrak.NewUserInfo;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

public class EfilerRegistrationController implements PageController {

    @Override
	public Response execute(PageExecutionContext etk) throws ApplicationException {

        TextResponse response = etk.createTextResponse();

        Boolean createUser = new Boolean(etk.getParameters().getSingle("createUser"));
        String firstName = etk.getParameters().getSingle("firstName");
    	String lastName = etk.getParameters().getSingle("lastName");
    	String email = etk.getParameters().getSingle("email");
    	String password = etk.getParameters().getSingle("password");
    	String passwordConfirm = etk.getParameters().getSingle("passwordConfirm");

    	if(createUser &&
    			(StringUtils.isBlank(firstName) || StringUtils.isBlank(lastName) || StringUtils.isBlank(email) || StringUtils.isBlank(password))){
    		response.put("errors", Arrays.asList("All fields are required."));
    	}
    	else if(createUser){
    		if(!EmailValidator.getInstance().isValid(email)){
    			response.put("errors", Arrays.asList("'" + email + "' is an invalid Email Address"));
    			return response;
    		}

    		//Create User
    		NewUserInfo userInfo = etk.getAdminFactory().createUserInfo();
    		userInfo.setAccountName(email);
    		userInfo.setEmailAddress(email);
    		userInfo.setFirstName(firstName);
    		userInfo.setLastName(lastName);
    		userInfo.setAuthenticationType("2");
    		userInfo.setDefaultRoleId(etk.getUserService().getRole("role.efiler").getId());
    		userInfo.setNodeId("0");
    		userInfo.setPassword(password);
    		userInfo.setPasswordConfirm(passwordConfirm);

    		try {
    			etk.getUserService().createUser(userInfo);
    			response.put("success", true);
        		response.put("username", userInfo.getAccountName());
			} catch (InputValidationException e) {
				e.printStackTrace();
				response.put("errors", e.validationResult().errorMessages());
			}


    	}

        return response;

    }

}

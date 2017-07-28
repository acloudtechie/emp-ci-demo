/**
 *
 * Handles changes of new eFile users to customize their dashboard
 *
 * administrator 10/26/2016
 **/

package com.mptraining.refapp.common.script;

import java.util.Collection;

import com.entellitrak.ApplicationException;
import com.entellitrak.ProfileInfo;
import com.entellitrak.system.UserEventExecutionContext;
import com.entellitrak.system.UserEventHandler;
import com.entellitrak.user.Role;
import com.entellitrak.user.User;
import com.mptraining.refapp.common.dao.EtkTablesDao;

public class NewEfilerUserCreationHandler implements UserEventHandler {

    @Override
	public void execute(UserEventExecutionContext etk) throws ApplicationException {

        ProfileInfo profile = etk.getProfileInfo();

        User user = etk.getUserService().getUser(profile.getAccountName());
        Long groupId = EtkTablesDao.getGroupId(etk, "group.efilers");
        
		Collection<Role> userRoles = user.getRoles();
        for (Role role : userRoles) {
        	if("role.efiler".equals(role.getBusinessKey())){
        		EtkTablesDao.addUserToGroup(etk, "group.efilers", user.getId());
        		
        		//Set User to use the group's preferences
        		etk.getUserService().linkGroupPreference(profile, groupId);
        		break;
        	}
		}
        
    }

}

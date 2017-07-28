/**
 *
 * Convenience methods for setting object assignments to users or groups.
 *
 * administrator 05/22/2017
 **/

package com.mptraining.refapp.common.script;

import java.util.Date;

import com.entellitrak.ExecutionContext;
import com.entellitrak.InputValidationException;
import com.entellitrak.assignment.GroupAssignment;
import com.entellitrak.assignment.UserAssignment;
import com.entellitrak.dynamic.DataObjectInstance;
import com.entellitrak.group.Group;
import com.entellitrak.user.Role;
import com.entellitrak.user.User;

public class AssignmentUtilities {

	public static <T extends DataObjectInstance> void assignToUser(ExecutionContext etk, Long objectId, Class<T> objectClass, Long userId, String roleBusinessKey) throws InputValidationException{
		User user = etk.getUserService().getUser(userId);
		Role role = etk.getUserService().getRole(roleBusinessKey);
		DataObjectInstance object = etk.getDynamicObjectService().get(objectClass, objectId);
		UserAssignment assignment = etk.getDataObjectAssignmentService().createUserAssignment();
		assignment.setUser(user);
		assignment.setCurrent(true);
		assignment.setAssignmentDate(new Date());
		assignment.setDataObjectInstance(object);
		assignment.setRole(role);
		etk.getDataObjectAssignmentService().createSaveOperation(assignment).save();
	}

	public static <T extends DataObjectInstance> void assignToGroup(ExecutionContext etk, Long objectId, Class<T> objectClass, String groupBusinessKey, String roleBusinessKey) throws InputValidationException{
		Group group = etk.getGroupService().getGroup(groupBusinessKey);
		Role role = etk.getUserService().getRole(roleBusinessKey);
		DataObjectInstance object = etk.getDynamicObjectService().get(objectClass, objectId);
		GroupAssignment assignment = etk.getDataObjectAssignmentService().createGroupAssignment();
		assignment.setCurrent(true);
		assignment.setAssignmentDate(new Date());
		assignment.setDataObjectInstance(object);
		assignment.setGroup(group);
		assignment.setRole(role);
		etk.getDataObjectAssignmentService().createSaveOperation(assignment).save();
	}


}

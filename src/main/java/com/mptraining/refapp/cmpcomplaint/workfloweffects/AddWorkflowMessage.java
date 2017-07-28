package com.mptraining.refapp.cmpcomplaint.workfloweffects;

import java.util.List;

import net.entellitrak.aea.exception.RulesFrameworkException;
import net.entellitrak.aea.rf.IRulesFrameworkParameters;
import net.entellitrak.aea.rf.IScript;

import com.entellitrak.DataObjectEventContext;
import com.entellitrak.ExecutionContext;

public class AddWorkflowMessage implements IScript {

	@Override
	public void doEffect(ExecutionContext etk, IRulesFrameworkParameters parameters) throws RulesFrameworkException {
		DataObjectEventContext etk2 = (DataObjectEventContext) etk;
		if(!etk2.getResult().isTransactionCanceled()){
			List<String> messages = parameters.getCustomParameters().getMultiple("addWorkflowMessage.message");

			for (String message : messages) {
				etk2.getResult().addMessage(message);
			}
		}
	}

}

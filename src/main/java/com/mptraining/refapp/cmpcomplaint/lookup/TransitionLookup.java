/**
 *
 * Lookup of available state transitions
 *
 * administrator 09/30/2016
 **/

package com.mptraining.refapp.cmpcomplaint.lookup;

import net.entellitrak.aea.rf.RulesFrameworkLookup;

import com.entellitrak.ApplicationException;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

public class TransitionLookup implements LookupHandler {

	@Override
    public String execute(final LookupExecutionContext etk)
            throws ApplicationException {
        return RulesFrameworkLookup.generateChildTransitionLookup(etk, "workflow.cmpComplaint");
    }


}

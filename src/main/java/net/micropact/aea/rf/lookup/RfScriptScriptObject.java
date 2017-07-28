package net.micropact.aea.rf.lookup;

import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.rf.IScript;
import net.micropact.aea.core.lookup.AInterfaceImplementingLookup;

/**
 * Serves as the {@link LookupHandler} for the RF Script - Script Object element. Returns all elements which implement
 * the {@link IScript} interface.
 *
 * @author zachary.miller
 */
public class RfScriptScriptObject extends AInterfaceImplementingLookup {

    /**
     * Default constructor which is called automatically by entellitrak code. Sets up the appropriate values in the
     * super class.
     */
    public RfScriptScriptObject(){
        super(IScript.class, "object.rfScript.element.scriptObject");
    }
}

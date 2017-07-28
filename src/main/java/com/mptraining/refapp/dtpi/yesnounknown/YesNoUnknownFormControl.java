/**
 *
 * Form Control Definitions for the Yes/No/Unknown control
 *
 * administrator 09/28/2016
 **/

package com.mptraining.refapp.dtpi.yesnounknown;

import com.entellitrak.configuration.AbstractScriptBasedFormControlModel;

public class YesNoUnknownFormControl extends AbstractScriptBasedFormControlModel {

    @Override
	public String getJsIncludeScript() {
        return null;
    }

    @Override
	public String getEditableControlScript() {
        return getScriptCode("com.mptraining.refapp.dtpi.yesnounknown.control.YesNoUnknownControl");
    }

    @Override
	public String getReadOnlyControlScript() {
        return getScriptCode("com.mptraining.refapp.dtpi.yesnounknown.control.YesNoUnknownReadOnlyControl");
    }

    @Override
	public String getAccessibleEditableControlScript() {
        return getScriptCode("com.mptraining.refapp.dtpi.yesnounknown.control.YesNoUnknownControl");
    }

    @Override
	public String getAccessibleReadOnlyControlScript() {
        return getScriptCode("com.mptraining.refapp.dtpi.yesnounknown.control.YesNoUnknownReadOnlyControl");
    }

    @Override
	public String getSearchControlScript() {
        return getScriptCode("com.mptraining.refapp.dtpi.yesnounknown.control.YesNoUnknownControl");
    }
}

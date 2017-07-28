/* RF - Form - RF Script Parameter - Javascript
 * ZRM */

"use strict";

(function(jQuery) {
    
    addOnloadEvent(initDynamicParameters)
    addOnloadEvent(addTooltips)
    
    function addTooltips() {
        Tooltip.create()
            .done(function(tt) {
                tt.addSmartTips()
            })
    }
    
    function initDynamicParameters() {
        DynamicParametersDefinition.initialize("RfScriptParameter")
    }
}(jQuery))

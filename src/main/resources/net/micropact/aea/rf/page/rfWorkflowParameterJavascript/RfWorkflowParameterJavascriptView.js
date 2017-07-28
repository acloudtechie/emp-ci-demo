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
        DynamicParametersDefinition.initialize("RfWorkflowParameter")
    }
}(jQuery))

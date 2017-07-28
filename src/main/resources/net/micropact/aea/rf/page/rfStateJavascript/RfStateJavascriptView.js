/* RF - Form - RF State - Javascript
 * ZRM */

"use strict";

(function(jQ) {
    addOnloadEvent(addTooltips)
    addOnloadEvent(initializeMultiselects)

    function addTooltips() {
        Tooltip.create()
            .done(function(tt) {
                tt.addSmartTips();
            })
    }

    function initializeMultiselects() {
        AeaFormUtilitiesJavascriptLibrary.autoResizeMultiSelects();
        
        AeaFormUtilitiesJavascriptLibrary.createMultiSelectAllNone("allowedTransitions")
        AeaFormUtilitiesJavascriptLibrary.createMultiFilter("allowedTransitions")
    }
}(jQuery))

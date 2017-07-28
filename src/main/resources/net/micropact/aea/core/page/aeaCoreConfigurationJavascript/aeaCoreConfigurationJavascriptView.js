/* AEA - CORE - Form - AEA CORE Configuration - Javascript 
 * ZRM */
"use strict";

(function() {
    addOnloadEvent(addTooltips);

    function addTooltips() {
        Tooltip.create()
            .done(function(tt) {
                tt.addSmartTips()
            })
    }
}())

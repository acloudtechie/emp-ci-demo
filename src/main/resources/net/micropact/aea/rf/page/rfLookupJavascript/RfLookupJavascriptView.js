/* RF - Form - RF Lookup - Javascript
 * ZRM */

"use strict";

(function(jQ) {
    addOnloadEvent(addTooltips)

    function addTooltips() {
        Tooltip.create()
            .done(function(tt) {
                tt.addSmartTips();
            })
    }

}(jQuery))

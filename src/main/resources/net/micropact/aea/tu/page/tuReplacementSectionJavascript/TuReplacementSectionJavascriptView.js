"use strict";

(function() {
    addOnloadEvent(setupEditor)
    addOnloadEvent(addTooltips)

    function addTooltips() {
        Tooltip.create()
            .done(function(tt) {
                tt.addSmartTips()
            })
    }

    function setupEditor() {
        CKEDITOR.replace('TuReplacementSection_text', {
            enterMode: CKEDITOR.ENTER_BR
        })
    }
}())

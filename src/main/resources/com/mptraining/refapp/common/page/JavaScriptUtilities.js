
"use strict";

/* Namespaced as JavascriptUtilitiesLibrary */
var JavascriptUtilitiesLibrary = function(jQ, u) {

    /* RELOAD FUNCTIONS */

    var reloadFunctions;
    if (u) {
        reloadFunctions = u.reloadFunctions;
    } else {
        reloadFunctions = new Array();
    }

    function addOnreloadEvent(fnName) {
        reloadFunctions[reloadFunctions.length] = fnName;
    }
    if (typeof window.onCompleteEventHandler != 'undefined') {
        var core_onCompleteEventHandler = onCompleteEventHandler;
    }
    window.onCompleteEventHandler = function(http_request, jsonHeader) {
        core_onCompleteEventHandler.apply(window, arguments)
        for (var i = 0; i < reloadFunctions.length; i++) {
            reloadFunctions[i]();
        }
    };

    function addMultiloadEvent(fnName) {
        addOnloadEvent(fnName);
        addOnreloadEvent(fnName);
    }

    return {
        reloadFunctions: reloadFunctions,
        addOnreloadEvent: addOnreloadEvent,
        addMultiloadEvent: addMultiloadEvent
    }
}(jQuery, JavascriptUtilitiesLibrary);

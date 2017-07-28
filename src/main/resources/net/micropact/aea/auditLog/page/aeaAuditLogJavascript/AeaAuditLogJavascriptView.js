"use strict";

(function(jQuery, U){
    
    addOnloadEvent(initializeObjectLink)
    
    function initializeObjectLink(){
        var dataObjectKey = U.getValue("AeaAuditLog_dataObjectKey")
        var trackingId = U.getValue("AeaAuditLog_trackingId")
        
        jQuery.post("page.request.do",
                {page: "aea.auditLog.getObjectLocation.ajax",
                dataObjectKey: dataObjectKey,
                trackingId: trackingId})
                .done(addObjectLink)
    }
    
    function addObjectLink(response){
        if(response.type === "found"){
            U.appendTD(jQuery("#linkToObjectLabel-container")[0],
                    jQuery("<button>")
                    .prop({type: "button"})
                    .addClass("formButton")
                    .click(function(){
                        window.open(response.url, "_blank")
                    })
                    .text("View Object")[0])
        }
    }
    
}(jQuery, AeaFormUtilitiesJavascriptLibrary))
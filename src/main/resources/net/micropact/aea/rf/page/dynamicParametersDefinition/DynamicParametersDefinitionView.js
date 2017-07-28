"use strict";

var DynamicParametersDefinition = (function(jQuery, AeaFormUtilitiesJavascriptLibrary){
    
    function initialize(objectName){
        setupProcessElementVisibility(objectName)
    }
    
    function setupProcessElementVisibility(objectName) {
        //It would be nice to include a validator library to make this stuff easier.

        jQuery.post("page.request.do?page=rf.ajax.getRfParameterTypes").done(function(parameterTypes){
            /* The lookup field is conditionally required */
            jQuery("#" + objectName + "_lookup")
                .parent()
                .append(AeaFormUtilitiesJavascriptLibrary.generateRequiredIcon())
            
            /* We need to show/hide the lookup field when appropriate */
            function processElementVisibility() {
                if ("lookup" === AeaFormUtilitiesJavascriptLibrary.lookupValueInArray(parameterTypes, "ID", AeaFormUtilitiesJavascriptLibrary.getValue(objectName + "_type"), "C_CODE")) {
                    etk.app.dataForm.showControl(objectName + "_lookup")
                } else {
                    etk.app.dataForm.hideControl(objectName + "_lookup")
                    jQuery("[name=" + objectName + "_lookup]").val("")
                }
            }

            jQuery("#" + objectName + "_type").change(processElementVisibility)
            processElementVisibility()
        })
    }
    
    return {
        initialize: initialize
    }
}(jQuery, AeaFormUtilitiesJavascriptLibrary))
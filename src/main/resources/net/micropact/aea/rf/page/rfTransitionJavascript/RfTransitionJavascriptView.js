/*
 * RF - Form - RF Transition - Javascript
 * ZRM 
 */

"use strict";

(function() {
    var dynamicParameterUsageType = $dynamicParameterUsageType
    var tooltip //I believe qtip is creating a global variable called tooltip when it actually creates the popup so this doesn't work if we remove the function wrapping
    
    addOnloadEvent(defaultInitialTransition)
    addOnloadEvent(addTooltips)
    addOnloadEvent(initializeMultiselects)
    addOnloadEvent(AeaFormUtilitiesJavascriptLibrary.autoResizeSelects)
    addOnloadEvent(setupParameters)

    function addTooltips() {
        tooltip = Tooltip.create()
        tooltip.done(function(tt) {
            tt.addSmartTips()
            tt.addEtkTooltip('parametersLabel', "These are parameters that were defined specifically for this particular workflow in RF Workflow Parameter")
        })
    }

    function initializeMultiselects() {
        AeaFormUtilitiesJavascriptLibrary.autoResizeMultiSelects();
        
        ["RfTransition_fromStates", "RfTransition_roles", "workflowEffects"].forEach(function(elementId){
            AeaFormUtilitiesJavascriptLibrary.createMultiSelectAllNone(elementId)
            AeaFormUtilitiesJavascriptLibrary.createMultiFilter(elementId)
        })
    }

    /* Set initialTransition to No */
    function defaultInitialTransition() {
        if (undefined == AeaFormUtilitiesJavascriptLibrary.getValue('RfTransition_initialTransition')) {
            jQuery('#RfTransition_initialTransition_no').click()
        }
    }
    
    function setupParameters(){
        DynamicParameters.setupDynamicParameters(
                dynamicParameterUsageType, 
                tooltip, 
                function(){
                    return AeaFormUtilitiesJavascriptLibrary.getValue('baseId')
                    })
    }
}())

/* RF - Form - RF Workflow Effect - Javascript
 * ZRM 
 * */

"use strict";

(function(jQ) {
    var dynamicParameterUsageType = $dynamicParameterUsageType
    var tooltip
    
    addOnloadEvent(AeaFormUtilitiesJavascriptLibrary.autoResizeMultiSelects)
    addOnloadEvent(addTooltips)
    addOnloadEvent(setupScriptHelp)
    addOnloadEvent(setupScriptLink)
    addOnloadEvent(setupParameters)
    addOnloadEvent(setupTransitionMultiselect)
    
    function setupScriptLink(){
        jQuery("<div>")
            .prop({id: "rfScriptLinkHolder"})
            .css({display: "inline-block"})
            .appendTo(jQuery("#RfWorkflowEffect_script-container > td:nth(1)"))
        jQuery("#RfWorkflowEffect_script").change(updateScriptLink)
        updateScriptLink()
    }
    
    function updateScriptLink(){
        var holder = jQuery("#rfScriptLinkHolder")
            .html("")
        var rfScriptId = getRfScriptId()
        if(rfScriptId){
            holder.append(jQuery("<button>")
                    .prop({type: "button"})
                    .addClass("formButton")
                    .css({marginLeft: ".5em"})
                    .click(function(){
                        window.open("workflow.do?dataObjectKey=object.rfScript&trackingId=" + encodeURIComponent(rfScriptId),
                                "_blank")
                    })
                    .text("View RF Script"))
        }
    }
    
    function setupScriptHelp() {
        jQuery('<a/>').text('[?]')
        .css({
            cursor: "default"
        })
        .prop({
            name: 'scriptHelpLink'
        })
        .appendTo(jQuery('#RfWorkflowEffect_script-container > td:nth(1)'))
        jQuery("#RfWorkflowEffect_script").change(updateScriptHelp)
        updateScriptHelp()
    }

    function getRfScriptId() {
        return AeaFormUtilitiesJavascriptLibrary.getValue("RfWorkflowEffect_script")
    }

    function updateScriptHelp() {

        jQuery.post("page.request.do", {
                page: "rf.ajax.getRfScript",
                rfScriptId: getRfScriptId()
            })
            .done(function(response) {

                jQuery('a[name=scriptHelpLink]').hide()

                if (response.C_DESCRIPTION && response.ID == getRfScriptId()) {
                    jQuery('a[name=scriptHelpLink]')
                    .prop({title: response.C_DESCRIPTION})
                    .show()
                }
            })

    }

    function addTooltips() {
        tooltip = Tooltip.create()
            .done(function(tt) {
                tt.addSmartTips()
            })
    }

    function setupTransitionMultiselect() {
        AeaFormUtilitiesJavascriptLibrary.createMultiSelectAllNone('RfWorkflowEffect_transitions')
        AeaFormUtilitiesJavascriptLibrary.createMultiFilter('RfWorkflowEffect_transitions')
    }

    function setupParameters() {
        DynamicParameters.setupDynamicParameters(
            dynamicParameterUsageType,
            tooltip,
            function() {
                return AeaFormUtilitiesJavascriptLibrary.getValue('RfWorkflowEffect_script')
            })

        //When the script changes we have to rebuild the configuration and view. We do not deserialize the parameters because ensureInputsExist will set them all to null
        jQuery('#RfWorkflowEffect_script').change(DynamicParameters.refreshParameters)
    }

}(jQuery))
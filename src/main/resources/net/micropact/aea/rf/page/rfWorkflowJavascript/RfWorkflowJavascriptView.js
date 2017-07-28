/* RF - Form - RF Workflow - Javascript
 * ZRM */

"use strict";

(function(jQuery, AeaUtil) {

    addOnloadEvent(setupStyleSheets)
    AeaUtil.addMultiloadEvent(setupRefreshListeners)
    AeaUtil.addMultiloadEvent(setupButtonContainer)
    AeaUtil.addMultiloadEvent(setupViewWorkflow)
    AeaUtil.addMultiloadEvent(setupManageRoles)
    AeaUtil.addMultiloadEvent(setupReorderEffects)
    AeaUtil.addMultiloadEvent(setupCleanWorkflow)
    AeaUtil.addMultiloadEvent(addTooltips)
    AeaUtil.addMultiloadEvent(setupHideShowLogic)
    
    function setupHideShowLogic(){
        if(AeaUtil.isCreate()){
            jQuery("#buttonContainer-container").hide()
        }
    }
    
    function addTooltips() {
        Tooltip.create()
            .done(function(tt) {
                tt.addSmartTips()
            })
    }

    function setupRefreshListeners() {
        jQuery('#RfWorkflow_childObject').change(refreshTrackingForm)
    }

    function setupButtonContainer() {
        jQuery('#buttonContainer-container > td')
            .prop({
                colspan: 1
            })
            .parent()
            .append(jQuery('<td>')
                .append(jQuery('<div>')
                    .addClass('buttonHolderDiv')))
    }

    function createFormButton(value) {
        return jQuery('<input type="button">')
            .addClass('formButton')
            .css({
                margin: '0.2em'
            })
            .prop({
                value: value
            })
    }

    /* This sets up the viewWorkflow button*/
    function setupViewWorkflow() {
        jQuery('.buttonHolderDiv')
            .append(createFormButton('View Workflow')
                .click(function() {
                    window.open('page.request.do?page=rf.page.viewWorkflow&rfWorkflowId=' + AeaUtil.getValue('trackingId'), '_blank')
                    return false
                }))
    }

    /* This sets up the manageRoles button*/
    function setupManageRoles() {
        jQuery('.buttonHolderDiv')
            .append(createFormButton('Manage Roles')
                .click(function() {
                    window.open('page.request.do?page=rf.page.manageRoleTransitions&rfWorkflowId=' + AeaUtil.getValue('trackingId'), '_blank')
                    return false
                }))
    }

    /* This sets up the Reorder Effects button*/
    function setupReorderEffects() {
        jQuery('.buttonHolderDiv')
            .append(createFormButton('Reorder Effects')
                .click(function() {
                    window.open('page.request.do?page=rf.page.orderWorkflowEffects&rfWorkflowId=' + AeaUtil.getValue('trackingId'), '_blank')
                    return false
                }))
    }

    /* This sets up the cleanWorkflow button */
    function setupCleanWorkflow() {

        jQuery('.buttonHolderDiv')
            .append(createFormButton('Clean Workflow')
                .click(function() {
                    jQuery.post('page.request.do', {
                            page: 'rf.page.cleanWorkflow'
                        },
                        function(response) {
                            alert(response)
                        })
                    return false
                }))

    }

    function setupStyleSheets() {
        AeaUtil.loadStyleSheet('web-pub/aearchitecture/qtip/qtip.css')
    }

}(jQuery, AeaFormUtilitiesJavascriptLibrary))

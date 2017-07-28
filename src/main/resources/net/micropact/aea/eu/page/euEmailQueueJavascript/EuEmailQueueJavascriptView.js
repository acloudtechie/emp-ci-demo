/* EU - Form - EU Email Queue - Javascript
 * ZRM */

"use strict";

(function() {
    addOnloadEvent(setupEditor)
    addOnloadEvent(loadAttachments)
    addOnloadEvent(addFromAddressLink)
    addOnloadEvent(addTooltips)
    addOnloadEvent(convertTextAreas)
    
    function addTooltips() {
        Tooltip.create()
            .done(function(tt) {
                tt.addSmartTips()
                jQuery('#attachments').append(tt.tooltip('Attachments'))
            })
    }

    function setupEditor() {
        CKEDITOR.replace('EuEmailQueue_body', {
            enterMode: CKEDITOR.ENTER_BR,
            removePlugins: 'etk,forms,iframe,flash,smiley,preview'
        })
    }

    function addFromAddressLink(){
        if(AeaFormUtilitiesJavascriptLibrary.getValue("EuEmailQueue_fromAddress")){
            jQuery("input[name=EuEmailQueue_fromAddress]")
                .parent()
                .append(jQuery("<button>")
                        .prop({type: "button"})
                        .addClass("formButton")
                        .css({marginLeft: "1em"})
                        .click(function(){
                            window.open("admin.refdata.update.request.do?dataObjectKey=object.euQueueAddress&trackingId=" + encodeURIComponent(AeaFormUtilitiesJavascriptLibrary.getValue("EuEmailQueue_fromAddress")), 
                                    "_blank")
                        })
                        .text("View Address"))
        }
    }
    
    /* This gets and displays the attachments related to this particular email */
    function loadAttachments() {
        jQuery.post('page.request.do', {
                page: 'eu.form.euEmailQueue.ajax',
                emailQueueId: $$('[name=trackingId]')[0].value
            },
            function(response) {
                AeaFormUtilitiesJavascriptLibrary.appendTD($('attachments-container'),
                    jQuery('<ul>')
                    .append(jQuery(response)
                        .map(function() {
                            return jQuery('<li>')
                                .append(AeaFormUtilitiesJavascriptLibrary.createFileLink(this.C_FILE, this.FILE_NAME))[0]
                        }))[0])
            })
    }

    function convertTextAreas() {
        function convertTextArea(i, name) {
            var area = jQuery('#' + name)
            var areaParent = area.parent()
            area.remove();
            jQuery('<input type="text">')
                .prop({
                    id: area.prop('id'),
                    name: area.prop('name')
                })
                .css({
                    width: area.css('width')
                })
                .addClass('formInput')
                .val(area.val())
                .prependTo(areaParent)
        }

        jQuery(['EuEmailQueue_subject',
            'EuEmailQueue_recipients',
            'EuEmailQueue_ccRecipients',
            'EuEmailQueue_bccRecipients'
        ]).each(convertTextArea)
    }
}())

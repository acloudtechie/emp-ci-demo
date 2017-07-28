/* EU - Form - EU Email Template - Javascript
 * ZRM */
"use strict";

(function() {
    addOnloadEvent(setupSubject)
    addOnloadEvent(setupEditor)
    addOnloadEvent(addTooltips)
    
    function addTooltips() {
        Tooltip.create()
            .done(function(tt) {
                tt.addSmartTips()
            })
    }

    function setupEditor() {
        CKEDITOR.replace('EuEmailTemplate_body', {
            enterMode: CKEDITOR.ENTER_BR
        });
    }

    function setupSubject() {
        /* Convert it from a textarea to an input */
        var subject = jQuery('#EuEmailTemplate_subject').remove()
        jQuery('<input type="text">')
            .prop({
                id: subject.prop('id'),
                name: subject.prop('name')
            })
            .css({
                width: subject.css('width')
            })
            .addClass('formInput')
            .val(subject.val())
            .prependTo(jQuery('#EuEmailTemplate_subject-container > td:last'))


        /* Add the entellitrak icon*/
        jQuery('<img>')
            .attr({
                src: 'web-pub/images/logo/entellitrak-icon.gif'
            })
            .click(function() {
                AeaTuReplacementVariableChooser.openReplacementVariableDialog(function(myValue) {
                    //jQuery('#EuEmailTemplate_subject').val(jQuery('#EuEmailTemplate_subject').val() + value) 
                    jQuery('#EuEmailTemplate_subject').each(function() {
                        var me = this;

                        if (me.selectionStart || me.selectionStart == '0') { // Real browsers
                            var startPos = me.selectionStart,
                                endPos = me.selectionEnd,
                                scrollTop = me.scrollTop;
                            me.value = me.value.substring(0, startPos) + myValue + me.value.substring(endPos, me.value.length)
                            me.focus()
                            me.selectionStart = startPos + myValue.length
                            me.selectionEnd = startPos + myValue.length
                            me.scrollTop = scrollTop
                        } else {
                            me.value += myValue
                            me.focus()
                        }
                    })
                })
            }).insertAfter(jQuery('#EuEmailTemplate_subject'))
    }
}())

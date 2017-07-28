"use strict"

var AeaTuReplacementVariableChooser = function() {

    /* This function will prompt the user to chose a replacement variable and call callback with the value that the user has chosen */
    function openReplacementVariableDialog(callback) {
        var currentValue = ''; //Current value of the replacer which is being built
        var containingDiv = jQuery('<div id="replacementContainer">') /*Currently the popup uses ids. I'm not entirely sure yet the best way to remove them*/

        var replacementSections = $replacementSections
        var replacementVariables = $replacementVariables
        var dataObjects = $dataObjects
        var dataElements = $dataElements

        jQuery('body').append(containingDiv)

        var comboReplacers = [{
            name: "HTML Escaper",
            prefix: "!",
            description: "HTML Escaper should be added after any replacers that should be rendered as plain text. Be certain to use this on any replacer which displays data entered by a user."
        }, {
            name: "Leading Space Replacer",
            prefix: "-s",
            description: "Leading Space Replacer should be used in conjunction with another replacer when you want a space placed before the result, but only when the result of the other replacer is not blank."
        }, {
            name: "Leading Newline Replacer",
            prefix: "-n",
            description: "Leading Newline Replacer should be used in conjunction with another replacer when you want a newline placed before the result, but only when the result of the other replacer is not blank. An example would be using it on an AddressLine2, You only want a newline if the AddressLine2 is not blank."
        }]

        buildTabs([{
            header: "Data Element",
            divBuilder: setupDataElementReplacer
        }, {
            header: "Replacement Variable",
            divBuilder: setupReplacementVariable
        }, {
            header: "Replacement Section",
            divBuilder: setupReplacementSection
        }, {
            header: "Simple Text",
            divBuilder: setupTextReplacer
        }, {
            header: "Combo Replacers",
            divBuilder: function() {
                return setupComboReplacers(comboReplacers)
            }
        }, {
            header: "{}",
            divBuilder: setupBraceReplacer
        }])

        jQuery(containingDiv).dialog({
            draggable: true,
            height: window.innerHeight * .80 || document.documentElement.clientHeight * .80,
            hide: 'fade',
            modal: true,
            resizable: true,
            show: 'fade',
            stack: true,
            title: '',
            width: window.innerWidth * .80 || document.documentElement.clientWidth * .80,
            close: function() {
                containingDiv.remove();
            }
        })

        //We have to make ourselves above the core menus which have large and increasing z-indexes      
        var zIndex = AeaFormUtilitiesJavascriptLibrary.maximumZIndex() + 1;

        containingDiv.parents('.ui-dialog:first')[0].style.setProperty("z-index", zIndex, "important")
        
        /*This function takes a config in the form of [{header: String, divBuilder: function} ...] and appends the tabs specified to #replacementContainer */
        function buildTabs(config) {
            jQuery('#replacementContainer')
                .append(jQuery('<div>')
                    .append(jQuery('<span>')
                        .text("Current Expression: ")
                        .css({
                            'font-size': '1.5em'
                        }))
                    .append(jQuery("<span id='currentValue'/>") //More ids
                        .css({
                            'font-size': '2em'
                        }))
                    .append(jQuery("<div>")
                        .text('Finish')
                        .button()
                        .click(function() {
                            callback(jQuery('#currentValue').text())
                            containingDiv.dialog('close')
                            containingDiv.remove();
                        })))
            var tabsDiv = jQuery('<div>')
            jQuery('#replacementContainer')
                .append(tabsDiv)
            var tabsUl = jQuery('<ul>')
            tabsDiv.append(tabsUl)
            jQuery(config).each(function(i) {
                tabsUl.append(jQuery('<li>')
                    .append(jQuery('<a>')
                        .prop({
                            href: '#replacementTab_' + i
                        })
                        .append(jQuery('<span>')
                            .text(this.header))))
                tabsDiv.append(this.divBuilder()
                    .prop({
                        id: 'replacementTab_' + i
                    }))
            })
            tabsDiv.tabs();
            refreshCurrentValue();
        }

        //Updates the display once currentValue has changed
        function refreshCurrentValue() {
            jQuery('#currentValue').text(currentValue)
        }

        /* This should be used to set a new value, it will take care of updating currentValue and updating the display */
        function makeValueSetter(value) {
            return function() {
                currentValue = value;
                refreshCurrentValue()
            }
        }

        /* This builds a tab for { and } replacers */
        function setupBraceReplacer() {
            return jQuery('<div>')
                .append(jQuery('<div>')
                    .css({
                        'margin-bottom': '1em'
                    })
                    .text("Since \"{\" and \"}\" are the special characters for templates, they must be treated differently. Click on one of the buttons below to insert a single opening or closing brace."))
                .append(jQuery('<div>')
                    .text('{')
                    .button()
                    .click(function() {
                        currentValue = '{}'
                        refreshCurrentValue();
                    }))
                .append(jQuery('<div>')
                    .text('}')
                    .button()
                    .click(function() {
                        currentValue = '}'
                        refreshCurrentValue();
                    }))
        }

        /* Builds a tab for the Combo Replacers which include HTMLEscape, LeadingNewline and LeadingSpace*/
        function setupComboReplacers(comboReplacers) {
            return jQuery('<div>')
                .append(jQuery(comboReplacers)
                    .map(function(i, replacer) {
                        return jQuery('<div>')
                            .append(jQuery('<div>')
                                .css({
                                    float: 'left',
                                    padding: '0.5em'
                                })
                                .button()
                                .text(replacer.name)
                                .click(function() {
                                    if (currentValue.length <= 2) {
                                        alert(replacer.name + " needs to be applied to the result of another replacer")
                                    } else {
                                        currentValue = currentValue.substr(0, 1) + replacer.prefix + currentValue.substr(1);
                                    }
                                    refreshCurrentValue();
                                }))
                            .append(jQuery('<div>')
                                .css({
                                    display: 'block',
                                    'margin-bottom': '1.5em'
                                })
                                .text(replacer.description))[0]
                    }))
        }

        /* Builds a tab for the Text Replacer */
        function setupTextReplacer() {
            var input = jQuery('<input>')
                .prop({
                    type: 'text'
                })

            return jQuery('<div>')
                .append(jQuery('<div>')
                    .css({
                        'margin-bottom': '1em'
                    })
                    .html("Simple Text Replacers are very rarely used. <br/>They display values of parameters which were passed into the Email Templater such as userId or trackingId. <br/>Simple Text Replacers are used mainly for debugging or for getting information which cannot be gotten from the database."))
                .append(jQuery('<label>Text: </label>'))
                .append(input)
                .append(jQuery('<div>')
                    .text("Update")
                    .button()
                    .click(function() {
                        if (input.val() == '') {
                            alert("You must enter text for the Simple Text Replacer");
                        } else {
                            currentValue = '{^' + input.val() + '}'
                            refreshCurrentValue();
                        }
                    }))
        }

        /* Builds a tab for the Replacement Variable escaper */
        function setupReplacementVariable() {
            return jQuery('<div>')
                .append(jQuery('<div>')
                    .css({
                        'margin-bottom': '1em'
                    })
                    .html("Replacement Variables are one of the most common types of Replacer and are used for pulling values from the Database. <br/>They are defined in the &quot;TU Replacement Variable&quot; Reference Data List. <br/>They should almost always be combined with an HTML Escaper."))
                .append(jQuery(replacementVariables).map(function() {
                    return jQuery('<div>')
                        .append(jQuery('<div>')
                            .button()
                            .text(this.C_NAME)
                            .click(makeValueSetter('{?' + this.C_NAME + '}')))[0]
                }))
        }

        /* Builds a tab for the Replacement Section replacer */
        function setupReplacementSection() {
            return jQuery('<div>')
                .append(jQuery('<div>')
                    .css({
                        'margin-bottom': '1em'
                    })
                    .html("Replacement Sections are for pieces of Templates which are going to be used in multiple emails/letters. <br/>They are defined in the &quot;TU Replacement Section&quot; Reference Data List. <br/>They are often used for Headers, Footers, or Summaries of an Object."))
                .append(jQuery(replacementSections).map(function() {
                    return jQuery('<div>')
                        .append(jQuery('<div>')
                            .button()
                            .text(this.C_NAME)
                            .click(makeValueSetter('{$' + this.C_NAME + '}')))[0]
                }))
        }

        /* Builds a tab for the Data Element replacer */
        function setupDataElementReplacer() {
            var dataObjectInput = jQuery('<select>')
                .append(jQuery(dataObjects).map(function(i, dataObject) {
                    return jQuery('<option>')
                        .prop({
                            value: dataObject.DATA_OBJECT_ID
                        })
                        .text(dataObject.NAME + " (" + dataObject.LABEL + ")")[0]
                }))
                .change(refreshDataElements)

            var dataElementInput = jQuery('<select>');

            refreshDataElements()

            function refreshDataElements() {
                dataElementInput.html('');

                var currentDataObjectId = dataObjectInput.val();
                dataElementInput.append(jQuery(dataElements)
                    .filter(function(i, dataElement) {
                        return dataElement.DATA_OBJECT_ID == currentDataObjectId
                    })
                    .map(function(i, dataElement) {
                        return jQuery('<option>')
                            .prop({
                                value: dataElement.ELEMENT_NAME
                            })
                            .text(dataElement.NAME)[0]
                    }))
            }

            var button = jQuery('<div>')
                .text('Update')
                .button()
                .click(function() {
                    makeValueSetter('{_' + AeaFormUtilitiesJavascriptLibrary.lookupValueInArray(dataObjects, 'DATA_OBJECT_ID', dataObjectInput.val(), 'OBJECT_NAME') + '_' + dataElementInput.val() + '}')()
                })

            var table = jQuery('<table>')
                .append(jQuery('<tbody>')
                    .append(jQuery('<tr>')
                        .append(jQuery('<td>')
                            .append(jQuery('<label>')
                                .text('Data Object: ')))
                        .append(jQuery('<td>')
                            .append(dataObjectInput)))
                    .append(jQuery('<tr>')
                        .append(jQuery('<td>')
                            .append(jQuery('<label>')
                                .text('Data Element: ')))
                        .append(jQuery('<td>')
                            .append(dataElementInput))))

            return jQuery('<div>')
                .append(jQuery('<div>')
                    .css({
                        'margin-bottom': '1em'
                    })
                    .html("Data Element replacers are one of the most common types of Replacer.<br/>They simply get the value of an element on a form. <br/>Data Element Replacers should almost always be combined with an HTML Escaper.<br/>Data Element Replacers expect parameters to be passed in the form &lt;DataObjectName&gt;Id; for instance CaseId or ContactId.<br/>Data Element Replacers do not work with Lookups or Data Type Plug-ins."))
                .append(table)
                .append(button)
        }
    }

    return {
        openReplacementVariableDialog: openReplacementVariableDialog
    }
}();

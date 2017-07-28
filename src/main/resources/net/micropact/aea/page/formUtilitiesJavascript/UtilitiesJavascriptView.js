/* AEA - Form - Utilities - Javascript
 * 
 * This file contains javascript functions which are intended to be used internally by the System Design team.
 * The API of this file may change and should not be relied upon by project teams.
 * 
 * ZRM */

"use strict";

/* Namespaced as AeaFormUtilitiesJavascriptLibrary */
var AeaFormUtilitiesJavascriptLibrary = function(jQ, u) {

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

    //Sets width to auto
    function resizeElementWidth(elem) {
        //elem.style.width = (elem.offsetWidth + 10) + 'px';
        elem.style.width = 'auto';
    }

    //Sets height to auto
    function resizeElementHeight(elem) {
        //elem.style.height = (elem.scrollHeight + 0) + 'px';
        elem.style.height = 'auto';
    }

    //makes it to checkboxes don't get cut off like entellitrak does by default
    function autoResizeMultiSelects() {
        var divs = document.getElementsByTagName('div');
        for (var dd = 0; dd < divs.length; dd++) {
            if (divs[dd].id.indexOf('_multiValue') > -1) {
                resizeElementWidth(divs[dd]);
                resizeElementHeight(divs[dd]);
            }
        }
    }

    //Makes selects auto-width because entellitrak cuts them off
    function autoResizeSelects() {
        var selects = document.getElementsByTagName('select');
        for (var dd = 0; dd < selects.length; dd++) {
            resizeElementWidth(selects[dd]);
            resizeElementHeight(selects[dd]);
        }
    }

    /* Determine if we are on the create form for a data object (as opposed to update) */
    function isCreate(){
        return getValue("update") === "false"
    }

    /* dynamically loads a css style sheet */
    function loadStyleSheet(url) {
        if (document.createStyleSheet) {
            document.createStyleSheet(url);
        } else {
            var cssNode = document.createElement('link');
            cssNode.type = 'text/css';
            cssNode.rel = 'stylesheet';
            cssNode.href = url;
            document.getElementsByTagName("head")[0].appendChild(cssNode);
        }
    }

    /* Finds the first element in the array of objects with searchedProperty = searchedValue
     * Then returns the returnedProperty of that object */
    function lookupValueInArray(array, searchedProperty, searchedValue, returnedProperty) {
        var filtered = filterArray(array, searchedProperty, searchedValue);
        if (filtered.size() > 0) {
            return filtered[0][returnedProperty];
        } else {
            return null;
        }
    }

    /* Filters an array of objects with searchedProperty = searchedValue */
    function filterArray(array, searchedProperty, searchedValue) {
        return jQ(array).filter(function(index) {
            return this[searchedProperty] == searchedValue;
        });
    }

    /*Gets the value of the input specified by name*/
    function getValue(name) {
        if (jQ('#' + name + '_multiValue').size() > 0) {
            if (jQ('[name=' + name + ']:checkbox').size() > 0) {
                return jQ('[name=' + name + ']:checked').map(function(index, box) {
                    return box.value
                });
            } else {
                return jQ('input[name=' + name + ']').map(function(index, input) {
                    return input.value
                });
            }
        } else {
            return jQ('#' + name + ', input[type=hidden][name=' + name + '], #' + name + '_yes:checked, #' + name + '_no:checked, input[name=' + name + ']:checked').val();
        }
    }

    function nvl(test, option) {
        return test ? test : option;
    }

    function nvl2(test, trueOption, falseOption) {
        return test ? trueOption : falseOption;
    }

    /* returns a required icon*/
    function generateRequiredIcon() {
        return jQuery('<img title="Required" alt="Required" src="themes/default/web-pub/images/icons/required.gif">');
    }

    function appendTD(tableRow, element) {
        var td = document.createElement('td');
        td.appendChild(element);
        $(tableRow).childElements().last().colSpan = "1"
        tableRow.appendChild(td);
    }

    function createFileLink(fileId, fileName) {
        return jQuery('<a>')
            .attr({
                href: 'tracking.file.do?id=' + fileId
            })
            .append(jQuery('<img>')
                .attr({
                    border: '0',
                    align: 'middle',
                    alt: 'Download',
                    src: "themes/default/web-pub/images/icons/16x16/document_attachment.png"
                }))
            .append(jQuery('<span>')
                .text(fileName))
    }

    function maximumZIndex() {
        var maximumZIndex = 1;

        jQuery('*')
            .each(function(i, ele) {
                var curZIndex = Number(jQuery(ele).css('z-index'))
                if (curZIndex > maximumZIndex) {
                    maximumZIndex = curZIndex
                }
            })
        return maximumZIndex
    }
    
    function ensureMultiHeader(inputId) {
        var multiValueDiv = jQuery("#" + inputId + "_multiValue")

        var headerDiv = multiValueDiv.prev(".multi-header")

        if (headerDiv.size() == 0) {
            return jQuery("<div>")
                .addClass("multi-header")
                .css({
                    backgroundColor: "#eeeeee",
                    border: "1px solid #b9b9b9",
                    borderRadius: "0.3em",
                    marginBottom: "0.2em",
                    padding: "0.1em"
                })
                .insertBefore(multiValueDiv)
        } else {
            return headerDiv
        }
    }

    function createMultiSelectAllNone(inputId) {
        var multiValueDiv = jQuery("#" + inputId + "_multiValue")

        ensureMultiHeader(inputId)
            .append(jQuery("<label>")
                .css({
                    marginRight: "1em"
                })
                .append(jQuery("<input>")
                    .prop({
                        type: "checkbox"
                    }))
                .change(function(event) {
                    var isChecked = jQuery(event.target).is(":checked")

                    jQuery(multiValueDiv)
                        .find("label[for^=" + inputId + "_]")
                        .filter(function(i, label) {
                            return !jQuery(label).hasClass("aea-filtered")
                        }).find("[name=" + inputId + "]:checkbox")
                        .map(function(i, checkbox) {
                            jQuery(checkbox).prop({
                                checked: isChecked
                            })
                        })
                })
                .append(jQuery("<span>")
                    .text("Select All")))
    }

    function createMultiFilter(inputId) {
        var multiValueDiv = jQuery("#" + inputId + "_multiValue")

        jQuery(multiValueDiv)
            .find(":checkbox[name=" + inputId + "]")
            .change(function(event) {
                if (jQuery(event.target).parents("label.aea-filtered").size() > 0) {
                    jQuery(event.target).prop({
                        checked: !jQuery(event.target).is(":checked")
                    })
                }
            })

        ensureMultiHeader(inputId)
            .append(jQuery("<label>")
                .append(jQuery("<span>")
                    .text("Filter:"))
                .append(jQuery("<input>")
                    .prop({
                        type: "text"
                    }))
                .keyup(function(event) {
                    var filterText = jQuery(event.target).val()
                    multiValueDiv
                        .find("label")
                        .each(function(i, ele) {
                            var jEle = jQuery(ele)
                            if (jEle.text().indexOf(filterText) >= 0) {
                                jEle
                                    .removeClass("aea-filtered")
                                    .css({
                                        opacity: 1,
                                        color: "#000"
                                    })
                            } else {
                                jEle
                                    .addClass("aea-filtered")
                                    .css({
                                        // We would just set the opacity, but IE8 dosen't have it
                                        opacity: 0.5,
                                        color: "#888"
                                    })
                            }
                        })
                }))
    }

    return {
        reloadFunctions: reloadFunctions,
        addOnreloadEvent: addOnreloadEvent,
        addMultiloadEvent: addMultiloadEvent,
        autoResizeMultiSelects: autoResizeMultiSelects,
        autoResizeSelects: autoResizeSelects,
        isCreate: isCreate,
        loadStyleSheet: loadStyleSheet,
        lookupValueInArray: lookupValueInArray,
        getValue: getValue,
        filterArray: filterArray,
        nvl: nvl,
        nvl2: nvl2,
        createMultiSelectAllNone: createMultiSelectAllNone,
        createMultiFilter: createMultiFilter,
        generateRequiredIcon: generateRequiredIcon,
        appendTD: appendTD,
        createFileLink: createFileLink,
        maximumZIndex: maximumZIndex
    }
}(jQuery, AeaFormUtilitiesJavascriptLibrary);

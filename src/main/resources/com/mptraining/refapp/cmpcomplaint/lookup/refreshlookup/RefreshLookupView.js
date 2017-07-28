"use strict"

var RefreshLookup = (function(jQuery) {

    /* Generic, global failed promise to be used by various functions */
    var FAILED_PROMISE = jQuery.Deferred().reject().promise()

    /* IE8 does not have the console object unless the console is actually open. */
    function warn(msg) {
        try {
            console.warn(msg)
        } catch (e) {}
    }

    /* 
     * Returns an element which indicates that the form is refreshing or something is loading 
     * */
    function generateLoading() {
        /* This is based on the built-in entellitrak loading from the new theme.
         * Unfortunately, they used id selectors so we either had to hardcode the CSS here
         * or introduce a new CSS file. */
        return jQuery("<div>")
            .append(jQuery("<img>")
                .attr({
                    border: 0,
                    align: "absmiddle",
                    alt: "Loading ...",
                    title: "Loading ...",
                    src: "themes/default/web-pub/images/icons/ajax-loader.gif"
                })
                .css({}))
            .append(jQuery("<span>")
                .text("Loading ..."))
            .css({
                position: "fixed",
                right: "50%",
                top: "50%",
                backgroundColor: "#f1f1f1",
                border: "1px solid #999",
                borderRadius: "5px",
                color: "#000",
                padding: "10px"
            })
    }

    /* 
     * Displays a loading message and removes it once the promise is done.
     * This means that the loading icon will never be removed if the promise fails.
     * 
     * Returns the promise. 
     * */
    function withLoading(promise) {
        /* Using core's loading element is not an option because it has no way to track that it is waiting on
         * more than one request. 
         * 
         * We will use a new loading icon for each request because it is easier, 
         * however we could also hide/show a single element and keep a count of outstanding requests.
         * */

        var loading = generateLoading()
            .appendTo(document.body)

        return promise
            .done(function() {
                loading.remove()
            })
    }

    /* 
     * This will return lookup data for formControlName using the specified lookup context.
     * executeFor should either be "TRACKING" or "SINGLE". If forSingleResult is not enabled for the site,
     * then "TRACKING" will be used even if you specify "SINGLE".
     * 
     * Returns a promise which if successful has the form [{value: String, display: String}] 
     * */
    function getLookupResults(formControlName, executeFor) {

        /* We serialize the tracking form and add 2 additional parameters:
         * EXECUTE_FOR and FORM_CONTROL_NAME.
         * Although we can run into a problem if those names are inputs on the form, it is avoidable
         * and considerably simpler than actually serializing the data correctly. */
        return jQuery.post("page.request.do?page=refreshLookup.page.getLookupData&EXECUTE_FOR=" + encodeURIComponent(executeFor) + "&FORM_CONTROL_NAME=" + encodeURIComponent(formControlName),
                jQuery("#trackingForm").serialize())
            .fail(function() {
                warn("RefreshLookup: Error making AJAX call")
            })
            .then(function(response) {
                return jQuery(response)
                    .find("lookupResult")
                    .map(function(i, lookupResult) {
                        var jResult = jQuery(lookupResult)
                        return {
                            value: jResult.attr("value"),
                            display: jResult.attr("display")
                        }
                    })
            })
    }

    /*
     * Refreshes a standard, editable, entellitrak lookup on a Stack layout form.
     * It updates the options in the existing select, so you do not need to re-attach event listeners
     * 
     * It returns a promise with the lookup results in case any post-processing needs to be done
     * */
    function refreshLookupEditable(formControlName) {
        return withLoading(getLookupResults(formControlName, "TRACKING")
            .done(function(lookupResults) {
                var jElement = jQuery("#trackingForm #" + formControlName)

                // get the current value so that we can set it once we rebuild the select
                var currentValue = jElement.val()

                jElement
                // Clear all options
                    .html("")
                    // Add the blank option
                    .append(jQuery("<option>")
                        .attr({
                            value: ""
                        }))
                    // Add the new options
                    .append(jQuery(lookupResults).map(function(i, lookupResult) {
                        return jQuery("<option>")
                            .attr({
                                value: lookupResult.value
                            })
                            .text(lookupResult.display)[0]
                    }))
                    // Set the value
                    .val(currentValue)

            })
        )
    }

    /* 
     * Refreshes a standard, read-only lookup on a stack layout form.
     * 
     * Returns a promise with the lookup results in case any post-processing needs to be done
     * */
    function refreshLookupReadOnly(formControlName) {
        return withLoading(getLookupResults(formControlName, "SINGLE")
            .done(function(lookupResults) {
                var currentValue = jQuery("#trackingForm [name=" + formControlName + "]").val()

                var matchingResults = lookupResults.filter(function(i, lookupResult) {
                    return lookupResult.value == currentValue
                })

                if (matchingResults.size() > 0) {
                    // If the selected value was in the results, set the display
                    jQuery("#trackingForm span#" + formControlName + "_display").text(matchingResults[0].display)
                } else {
                    // If the selected value was not in the results, we clear out both the input and the display
                    jQuery("#trackingForm [name=" + formControlName + "]").val("")
                    jQuery("#trackingForm span#" + formControlName + "_display").text("")
                }
            })
        )
    }

    /*
     * Refreshes a lookup on an entellitrak stack-layout form.
     * This method will attempt to determine whether the lookup is editable or read-only and do the correct thing.
     * 
     * If you have done extensive modifications to the lookup in javascript, you may not be able to use this method
     * and may have to instead write your own method based on getLookupResults.
     * 
     * Returns a promise of the lookup results in case any post-processing needs to be done
     * */
    function refreshLookup(formControlName) {
        if (jQuery("#trackingForm select#" + formControlName).size() > 0) {
            // Editable
            return refreshLookupEditable(formControlName)
        } else if (jQuery("#trackingForm span#" + formControlName + "_display").size() > 0) {
            // Read Only
            return refreshLookupReadOnly(formControlName)
        } else {
            // Couldn't determine
            warn("RefreshLookup: Could not refresh lookup " + formControlName + " possibly because the lookup is not on the form")
            return FAILED_PROMISE
        }
    }

    /* 
     * Refreshes a standard read-only multiselect on a Stack Layout form.
     * 
     * Returns a promise of the lookup results in case any post-processing needs to be done.
     * */
    function refreshMultiselectReadOnly(formControlName) {
        return withLoading(getLookupResults(formControlName, "SINGLE")
            .done(function(lookupResults) {

                // Get the selected values so that we will know which results to keep
                var currentInputs = jQuery("#trackingForm input[name=" + formControlName + "]:hidden")
                var selectedValues = {}
                jQuery(currentInputs).each(function(i, input) {
                    selectedValues[jQuery(input).val()] = true
                })

                // We limit the results to just the ones which were selected
                var goodLookupResults = jQuery(lookupResults).filter(function(i, lookupResult) {
                    return selectedValues[lookupResult.value] === true
                })

                // Remove existing inputs
                currentInputs.remove()

                // Add the valid inputs
                jQuery("#trackingForm #" + formControlName + "-container > td:nth(1)")
                    .prepend(goodLookupResults.map(function(i, lookupResult) {
                        return jQuery("<input>")
                            .addClass("formInput")
                            .attr({
                                type: "hidden",
                                value: lookupResult.value,
                                name: formControlName
                            })[0]
                    }))

                // Add the displays
                jQuery("#trackingForm #" + formControlName + "_multiValue ul.multiValueReadOnlyUl")
                    .html("")
                    .append(goodLookupResults.map(function(i, lookupResult) {
                        return jQuery("<li>")
                            .text(" " + lookupResult.display)[0]
                    }))
            })
        )
    }

    /* 
     * Refreshes a standard editable multiselect on a stack layout form. 
     * This completely rebuilds the contents of the _multiValue container which means that you will
     * have to re-attach any event listeners or other processing.
     * 
     * Returns a promise of lookup results in case any post-processing needs to be done
     * */
    function refreshMultiselectEditable(formControlName) {
        return withLoading(getLookupResults(formControlName, "TRACKING")
            .done(function(lookupResults) {

                // Get the selected values
                var selectedValues = {}
                jQuery("#trackingForm [name=" + formControlName + "]:checked").each(function(i, checkbox) {
                    selectedValues[jQuery(checkbox).val()] = true
                })

                // Remove the contents of the _multiValue container
                var containingDiv = jQuery("#trackingForm #" + formControlName + "_multiValue")
                    .html("")

                // Add the new labels/inputs
                lookupResults.each(function(i, lookupResult) {
                    var id = formControlName + "_" + (i + 1)
                    containingDiv.append(jQuery("<label>")
                            .attr({
                                "for": id
                            })
                            .text(" " + lookupResult.display)
                            .prepend(jQuery("<input>")
                                .attr({
                                    id: id,
                                    type: "checkbox",
                                    value: lookupResult.value,
                                    checked: selectedValues[lookupResult.value] === true,
                                    name: formControlName
                                })))
                        .append(jQuery("<br>"))
                })
            })
        )
    }

    /* 
     * Refreshes a multiselect on a stack layout form.
     * 
     * Does its best to determine whether the multiselect is editable or read-only and then do the correct thing.
     * If you have modified the multiselect using javascript, you may not be able to use this method and instead
     * may have to create your own based on getLookupResults.
     * 
     * Returns a promise of lookup results in case any post-processing needs to be done.
     * */
    function refreshMultiselect(formControlName) {
        if (jQuery("#trackingForm span#" + formControlName + "_display").size() > 0) {
            // Read-Only
            return refreshMultiselectReadOnly(formControlName)
        } else if (jQuery("#trackingForm #" + formControlName + "_multiValue").size() > 0) {
            // Editable
            return refreshMultiselectEditable(formControlName)
        } else {
            // Could not determine
            warn("RefreshLookup: Could not refresh lookup " + formControlName + " possibly because the lookup is not on the form")
            return FAILED_PROMISE
        }
    }

    return {
        getLookupResults: getLookupResults,
        refreshLookup: refreshLookup,
        refreshMultiselect: refreshMultiselect
    }
}(jQuery))

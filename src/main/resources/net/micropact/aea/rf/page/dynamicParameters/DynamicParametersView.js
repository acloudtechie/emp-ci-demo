"use strict"

/* 
 * NOTE: at this point this script cannot be used multiple times on a particular form 
 * (which isn't going to be a problem)
 * */

var DynamicParameters = (function(){

    /* The declaration of the parameters here is why it can't be used multiple times */
    var dynamicParameterUsageType
    var data = {};
    var tooltip
    var getParameterDefinerId;

    function setupDynamicParameters(dynamicParametersType, tooltips, getParameterDefinerIdFn) {
        dynamicParameterUsageType = dynamicParametersType
        tooltip = tooltips
        getParameterDefinerId = getParameterDefinerIdFn

        setupParameters()
    }

    function setupParameters(){
        jQuery('#parameters-container').hide()

        AeaFormUtilitiesJavascriptLibrary.appendTD($('parametersLabel-container'),
                jQuery('<div>')
                .addClass('parametersHolder')[0])

                fetchConfiguration().done(function() {
                    deserializeParameters();
                    buildView();
                });

        jQuery('form#trackingForm')
        .submit(serializeParameters)
    }

    function restructureLookupParameters(response) {
        var lookupParameterIndex = 0;

        for (var parameterIndex = 0; parameterIndex < response.parameters.length; parameterIndex++) {
            if (response.parameters[parameterIndex].LOOKUPSQL != null) {
                response.parameters[parameterIndex].lookupItems = response.lookupParameters[lookupParameterIndex];
                lookupParameterIndex += 1;
            }
        }
    }

    function fetchConfiguration() {
        return jQuery.post('page.request.do', {
            page: 'rf.utility.dynamicParameters.ajax',
            dynamicParameterType: dynamicParameterUsageType,
            rfWorkflowId: AeaFormUtilitiesJavascriptLibrary.getValue('baseId'),
            parameterDefinerParentId: getParameterDefinerId()
        },
        function(response) {
            restructureLookupParameters(response);
            data.parameters = response.parameters;

            jQuery(data.parameters).each(function(i, parameter) {
                parameter.values = [null]
            })
        })
    }

    function deserializeParameters() {
        var dbParameters = jQuery(jQuery.parseXML(AeaFormUtilitiesJavascriptLibrary.getValue('parameters')))
        .find('parameter')
        .map(function(i, parameter) {
            return {
                id: jQuery(parameter).find('parameterid').text(),
                value: jQuery(parameter).find('value').text()
            }
        })

        dbParameters.each(function(i, dbParameter) {
            var parameter = AeaFormUtilitiesJavascriptLibrary.filterArray(data.parameters, 'PARAMETERID', dbParameter.id)[0];
            if (parameter.values[0] == null) {
                parameter.values[0] = dbParameter.value
            } else {
                parameter.values.push(dbParameter.value)
            }
        })
    }

    /* We serialize the parameters so much because it's (slightly) easier than making a global variable to persist the data across refreshTrackingForm */
    function serializeParameters() {

        var parameters = jQuery('<parameters>')

        jQuery(data.parameters).each(function(i, parameter) {
            jQuery(parameter.values).each(function(i, value) {
                parameters.append(jQuery('<parameter>')
                        .append(jQuery('<parameterid>')
                                .text(parameter.PARAMETERID))
                                .append(jQuery('<value>')
                                        .text(value == null ? "" : value)))
            })
        })
        jQuery('#parameters').val(jQuery('<root>')
                .append(parameters)
                .html())
    }

    function refreshParameters(){
        fetchConfiguration().done(function() {
            buildView();
        })
    }

    function addValue(parameterId) {
        AeaFormUtilitiesJavascriptLibrary.filterArray(data.parameters, 'PARAMETERID', parameterId)[0].values.push(null)
        buildView();
        serializeParameters()
    }

    function setValue(parameterId, index, value) {
        AeaFormUtilitiesJavascriptLibrary.filterArray(data.parameters, 'PARAMETERID', parameterId)[0].values[index] = value
        buildView();
        serializeParameters()
    }

    function removeValue(parameterId, index) {
        AeaFormUtilitiesJavascriptLibrary.filterArray(data.parameters, 'PARAMETERID', parameterId)[0].values.splice(index, 1)
        buildView();
        serializeParameters();
    }

    function buildView() {
        tooltip.done(function(tt) {
            ReactDOM.render(MakeParameters({parameters: data.parameters,
                addValue: addValue,
                setValue: setValue,
                removeValue: removeValue,
                tooltip: tt}),
                jQuery(".parametersHolder")[0])
        })
    }

    function MakeParameters(props){
        return React.DOM.div(null,
                props.parameters.map(function(parameter, i){
                    return MakeParameter({parameter: parameter,
                        addValue: function(){
                            props.addValue(parameter.PARAMETERID)
                        },
                        setValue: function(index, value){
                            props.setValue(parameter.PARAMETERID, index, value)
                        },
                        removeValue: function(index){
                            props.removeValue(parameter.PARAMETERID, index)
                        },
                        tooltip: props.tooltip})
                }))
    }

    function MakeParameterTooltip(props){
        return React.DOM.span(null, props.description)
    }
    
    var MakeParameterTooltip = React.createFactory(React.createClass({
        componentDidMount: function(){
            var props = this.props
            var ourSpan = ReactDOM.findDOMNode(this)
            if(props.description){
                jQuery(ourSpan).append(props.tooltip.tooltip(props.description))
            }
        },
        render: function(){
            return React.DOM.span(null)
        }
    }))
    
    function makeParameterTooltip(description, tooltip) {
        if (description) {
            return tooltip.tooltip(description)
            //.css({'vertical-align': 'top'})
        }
    }

    function MakeParameter(props){
        var parameter = props.parameter
        return React.DOM.div({className: "parameter-container",
            key: parameter.PARAMETERID},
            React.DOM.label({className: "parameter-label"},
                    parameter.PARAMETERNAME,
                    MakeParameterTooltip({description: parameter.DESCRIPTION,
                        tooltip: props.tooltip})),
                        /* entellitrak normally puts the required icon after the inputs so putting it before looks
                         * kind of weird, however putting it after 
                         * seemed to look even more weird. */
                        parameter.REQUIRED == 1 && AeaCoreReactComponents.MakeRequiredIcon(),
                        MakeParameterInputs(props))
    }

    function MakeRemoveButton(props){
        return React.DOM.button({className: "formButton",
            type: "button",
            onClick: function(event){
                event.preventDefault()
                props.removeValue()
            }},
        "Remove")
    }

    function MakeAddAnother(props){
        return React.DOM.button({className: "formButton",
            type: "button",
            onClick: function(event){
                event.preventDefault()
                props.addValue()
            }},
        "Add Another")
    }

    function MakeParameterInputs(props){
        var parameter = props.parameter
        var allowMultiple = parameter.ALLOWMULTIPLE == 1
        return React.DOM.div({className: "parameter-inputs"},
                parameter.values.map(function(value, i){
                    return MakeInputRow({parameter: parameter,
                        key: i,
                        value: value,
                        allowMultiple: allowMultiple,
                        setValue: function(value){
                            props.setValue(i, value)
                        },
                        removeValue: function(){
                            props.removeValue(i)
                        }})
                }),
                allowMultiple && MakeAddAnother({addValue: props.addValue}))
    }

    function MakeInputRow(props){
        return React.DOM.div({key: props.key},
                MakeInput(props),
                props.allowMultiple && MakeRemoveButton({removeValue: props.removeValue}))
    }

    function MakeInput(props){
        var parameter = props.parameter
        var value = props.value || "" /* React wants empty string instead of null for cleared out inputs */
        var setValue = props.setValue

        switch(parameter.PARAMETERTYPECODE){
        case "text":
            return React.DOM.input({type: "text",
                className: "formInput",
                onChange: function(event){
                    setValue(event.target.value)
                },
                value: value})
        case "longText":
            return React.DOM.textarea({className: "formInput",
                onChange: function(event){
                    setValue(event.target.value)
                },
                value: value})
        case "lookup":
            return React.DOM.select({value: value,
                onChange: function(event){
                    setValue(event.target.value)}},
                    React.DOM.option(),
                    parameter.lookupItems.map(function(lookupItem, i){
                        return React.DOM.option({value: lookupItem.VALUE,
                            key: i},
                            lookupItem.DISPLAY)
                    }))
        case "naturalNumber":
            return React.DOM.input({type: "text",
                className: "formInput",
                onChange: function(event){
                    setValue(event.target.value)
                },
                value: value})
        }
    }

    return {
        setupDynamicParameters: setupDynamicParameters,
        refreshParameters: refreshParameters
    }
}())
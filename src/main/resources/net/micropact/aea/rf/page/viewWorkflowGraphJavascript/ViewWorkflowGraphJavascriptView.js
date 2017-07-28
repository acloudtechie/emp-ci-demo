/* RF - Page - View Workflow - Graph - Javascript
 * ZRM */
"use strict";

var rfStates = $rfStates;
var rfTransitions = $rfTransitions;
var rfTransitionEffects = $rfTransitionEffects;
var rfWorkflow = $rfWorkflow;

(function() {

    jQuery(jsPlumbDemoInit)

    function getRfStateDisplay(rfStateId) {
        //Slightly inefficient having to search through the array twice but not enough to matter
        var rfState = AeaFormUtilitiesJavascriptLibrary.filterArray(rfStates, 'ID', rfStateId)[0];
        if (rfState) {
            return rfState.C_NAME + ' (' + rfState.C_CODE + ')';
        } else {
            return ''
        }
    }

    /* Creates a table describing the specified Transition*/
    function createTransitionDescriptionElement(transitionId) {
        var rfTransition = AeaFormUtilitiesJavascriptLibrary.filterArray(rfTransitions, 'TRANSITIONID', transitionId)[0];

        var transitionElement = jQuery('<table/>')
            .append(jQuery('<tbody/>')
                .append(jQuery('<tr/>')
                    .append(jQuery('<td/>').text('Name'))
                    .append(jQuery('<td/>').text(rfTransition.TRANSITIONNAME)))
                .append(jQuery('<tr/>')
                    .append(jQuery('<td/>').text('Code'))
                    .append(jQuery('<td/>').text(rfTransition.TRANSITIONCODE)))
                .append(jQuery('<tr/>')
                    .append(jQuery('<td/>').text('From State'))
                    .append(jQuery('<td/>').text(AeaFormUtilitiesJavascriptLibrary.nvl(AeaFormUtilitiesJavascriptLibrary.lookupValueInArray(rfStates, 'ID', rfTransition.FROMSTATE, 'C_NAME'), ''))))
                .append(jQuery('<tr/>')
                    .append(jQuery('<td/>').text('To State'))
                    .append(jQuery('<td/>').text(AeaFormUtilitiesJavascriptLibrary.nvl(AeaFormUtilitiesJavascriptLibrary.lookupValueInArray(rfStates, 'ID', rfTransition.TOSTATE, 'C_NAME'), ''))))
                .append(jQuery('<tr/>')
                    .append(jQuery('<td/>').text('Effects'))
                    .append(jQuery('<td/>').append(jQuery('<ul/>').append(AeaFormUtilitiesJavascriptLibrary.filterArray(rfTransitionEffects, 'C_TRANSITION', rfTransition.TRANSITIONID).map(function() {
                        return jQuery('<li>').text(this.C_NAME)[0]
                    }))))))
        return jQuery('<div/>')
            .append(jQuery('<div/>')
                .append(jQuery('<span/>').text('Transition '))
                .append(jQuery('<a/>')
                    .text('(View Transition)')
                    .prop({
                        href: 'workflow.do?dataObjectKey=object.rfTransition&trackingId=' + transitionId,
                        target: '_blank'
                    })))
            .append(transitionElement)
    }

    /*Creates a table describing the specified State*/
    function createStateDescriptionElement(rfStateId) {
        var rfState = AeaFormUtilitiesJavascriptLibrary.filterArray(rfStates, 'ID', rfStateId)[0];
        var stateDiv = jQuery('<div/>');
        stateDiv.append(jQuery('<div/>')
            .append(jQuery('<span/>')
                .text('State '))
            .append(jQuery('<a/>')
                .text('(View State)')
                .prop({
                    href: 'workflow.do?dataObjectKey=object.rfState&trackingId=' + rfStateId,
                    target: '_blank'
                })))
        stateDiv.append(jQuery('<table/>')
            .append(jQuery('<tbody/>')
                .append(jQuery('<tr/>')
                    .append(jQuery('<td/>').text('Name'))
                    .append(jQuery('<td/>').text(rfState.C_NAME)))
                .append(jQuery('<tr/>')
                    .append(jQuery('<td/>').text('Code'))
                    .append(jQuery('<td/>').text(rfState.C_CODE)))
                .append(jQuery('<tr/>')
                    .append(jQuery('<td/>').text('Transitions'))
                    .append(jQuery('<td/>')
                        .append(jQuery('<ul/>')
                            .append(jQuery(rfTransitions).filter(function() {
                                    return this.TOSTATE == null && this.FROMSTATE == rfStateId
                                })
                                .map(function() {
                                    return jQuery('<li/>')
                                        .append(createTransitionDescriptionElement(this.TRANSITIONID))[0]
                                })))))))
        return stateDiv;
    }

    /* Handle hiding the parent's InfoDiv */
    function createClearInfoDivButton() {
        return jQuery('<button>')
            .addClass('formButton')
            .text('Hide')
            .click(function() {
                jQuery(parent.jQuery('#infoDiv').hide('fast').html(''))
                return false;
            })
    }

    function handleStateClick(rfStateId) {
        /*Refresh the infoDiv with the information about the just-clicked State*/
        parent.jQuery('#infoDiv').html('').hide();
        var stateElement = createStateDescriptionElement(rfStateId)
        parent.jQuery('#infoDiv')
            .append(createClearInfoDivButton())
            .append(stateElement)
            .show('fast');
        //jsPlumb.repaintEverything();
    }

    function handleTransitionClick(transitionConnectionId) {
        /*Refresh the infoDiv with information about the just-clicked Transition*/
        parent.jQuery('#infoDiv').html('').hide()

        //jsPlumb.repaintEverything();
    }

    function handleTransitionDBLClick(transitionConnectionId) {
        /*Refresh the infoDiv with information about the just-clicked Transition*/
        parent.jQuery('#infoDiv').html('').hide()

        var rfTransitionId = AeaFormUtilitiesJavascriptLibrary.lookupValueInArray(rfTransitions, 'connectionId', transitionConnectionId, 'TRANSITIONID');
        var transitionElement = createTransitionDescriptionElement(rfTransitionId);

        parent.jQuery('#infoDiv')
            .append(createClearInfoDivButton())
            .append(transitionElement)
            .show('fast');
        //jsPlumb.repaintEverything();
    }



    /*A lot of the stuff here is adapted from the jsPlumb statemachine demo*/

    // helper method to generate a color from a cycle of colors. Makes transitions prettier.
    var curColourIndex = 1,
        maxColourIndex = 24,
        nextColour = function() {
            var R, G, B;
            R = parseInt(128 + Math.sin((curColourIndex * 3 + 0) * 1.3) * 128);
            G = parseInt(128 + Math.sin((curColourIndex * 3 + 1) * 1.3) * 128);
            B = parseInt(128 + Math.sin((curColourIndex * 3 + 2) * 1.3) * 128);
            curColourIndex = curColourIndex + 1;
            if (curColourIndex > maxColourIndex) curColourIndex = 1;
            return "rgb(" + R + "," + G + "," + B + ")";
        };

    function jsPlumbDemoInit() {
        // setup some defaults for jsPlumb. This makes it look state-machiney   
        jsPlumb.importDefaults({
            Endpoint: ["Dot", {
                radius: 2
            }],
            HoverPaintStyle: {
                strokeStyle: "#42a62c",
                lineWidth: 2
            },
            ConnectionOverlays: [
                ["Arrow", {
                    location: 1,
                    id: "arrow",
                    length: 14,
                    foldback: 0.8
                }]
            ]
        });

        //Add the starting state
        jQuery('#graphDiv')
            .append(jQuery('<div/>')
                .css({
                    top: AeaFormUtilitiesJavascriptLibrary.nvl(rfWorkflow.C_START_STATE_Y_COORDINATE, 0),
                    left: AeaFormUtilitiesJavascriptLibrary.nvl(rfWorkflow.C_START_STATE_X_COORDINATE, 0)
                })
                .addClass('w')
                .addClass('startState')
                .prop({
                    id: 'rfStartState'
                })
                .text('*Initial State*'))

        //Add the rest of the states
        jQuery(rfStates).each(function() {
            jQuery('#graphDiv')
                .append(jQuery('<div/>')
                    .css({
                        top: AeaFormUtilitiesJavascriptLibrary.nvl(this.C_Y_COORDINATE, 0),
                        left: AeaFormUtilitiesJavascriptLibrary.nvl(this.C_X_COORDINATE)
                    })
                    .addClass('w')
                    .prop({
                        id: 'rfState_' + this.ID
                    })
                    .text(this.C_NAME)
                    .mousedown(function() { /*jsPlumb.setSuspendDrawing(true)*/ })
                    .dblclick(function(rfState) {
                        return function() {
                            handleStateClick(rfState.ID)
                        }
                    }(this))
                    //.mouseup(function(){jsPlumb.setSuspendDrawing(false, true)})
                )
        })



        jsPlumb.draggable(jQuery(".w"));
        jQuery('.w').on('dragstop', function(event, ui) {
            //Use jQuery to constrain the dragging on the top and left sides

            function constrainSide(side) {
                if (jQuery(event.target).css(side)[0] == '-') {
                    jQuery(event.target).css(side, '0px')
                    setTimeout(jsPlumb.repaintEverything, 0); //We need to use setTimeout even though it is for 0 ms because otherwise jsPlumb will repaint with the old location
                }
            }

            constrainSide('top')
            constrainSide('left')
        })

        //Make transitions clickable
        /*jsPlumb.bind("click", function(connection) { 
            handleTransitionClick(connection.id);
        });*/

        jsPlumb.bind("dblclick", function(connection) {
            handleTransitionDBLClick(connection.id);
        });

        //Also help make state-machiney
        jQuery(".w").each(function(i, e) {
            jsPlumb.makeSource(jQuery(e), {
                anchor: "Continuous",
                connector: ["StateMachine", {
                    curviness: 20
                }],
                connectorStyle: {
                    strokeStyle: nextColour(),
                    lineWidth: 2
                }
            });
        });

        jsPlumb.bind("connection", function(info) {
            info.connection.setPaintStyle({
                strokeStyle: nextColour()
            }); //set color
            //info.connection.getOverlay("label").setLabel(info.connection.id);
        });

        //ZRM - the arrows point to the bottom of the element instead of top
        jsPlumb.makeTarget(jQuery(".w"), {
            anchor: "Continuous"
        });

        // Now add the transitions from state to state
        jQuery(rfTransitions)
            .filter(function() {
                return this.TOSTATE && this.FROMSTATE
            }).each(function() {
                var connection = jsPlumb.connect({
                    source: 'rfState_' + this.FROMSTATE,
                    target: 'rfState_' + this.TOSTATE,
                    label: this.TRANSITIONNAME
                })
                this.connectionId = connection.id;
                this.connection = connection
            })

        /*Add the initial transitions, this may actually not be correct if the initial transition can be made from other FROM States*/
        jQuery(rfTransitions)
            .filter(function() {
                return this.C_INITIAL_TRANSITION == 1 && this.TOSTATE
            })
            .each(function() {
                var connection = jsPlumb.connect({
                    source: 'rfStartState',
                    target: 'rfState_' + this.TOSTATE,
                    label: this.TRANSITIONNAME
                })
                this.connectionId = connection.id;
                this.connection = connection
            })
    }
}());
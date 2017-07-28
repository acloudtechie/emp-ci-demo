/* RF - Form - RF Script- Javascript
 * ZRM */

"use strict";

(function(jQuery) {
     addOnloadEvent(addTooltips)
     addOnloadEvent(setupScriptObjectLink)
     addOnloadEvent(setupAssociatedEffects)
     
     function setupScriptObjectLink(){
         var holder = jQuery("<div>")
             .insertAfter(jQuery("#RfScript_scriptObject-container > td:nth(1)"))
         jQuery("#RfScript_scriptObject").change(updateScriptObjectLink)
         updateScriptObjectLink()
         
         function updateScriptObjectLink(){
             holder.html("")
             
             var initialKey = getScriptQualifiedPath()
             
             if(initialKey){
                 jQuery.post("page.request.do?page=rf.ajax.getScriptObject",
                         {fullyQualifiedName: initialKey})
                 .done(function(response){
                     /* We perform a check that the name we got back matches the current value because the user may
                      * have changed the input's value while the ajax call was outstanding. */
                     if(response.FULLY_QUALIFIED_SCRIPT_NAME === getScriptQualifiedPath()){
                         holder.append(jQuery("<button>")
                                 .addClass("formButton")
                                 .prop({type: "button"})
                                 .click(function(){
                                     window.open("cfg.scriptobject.update.request.do?id=" + encodeURIComponent(response.SCRIPT_ID), 
                                             "_blank")
                                 })
                                 .text("View Script Object"))                         
                     }
                 })
             }
         }
     }
     
     function getScriptQualifiedPath(){
         return AeaFormUtilitiesJavascriptLibrary.getValue("RfScript_scriptObject")
     }
     
    function addTooltips() {
        Tooltip.create()
            .done(function(tt) {
                tt.addSmartTips();
            })
    }

    function EffectsView(props){
        return React.DOM.table({className: "grid aea-core-grid aea-core-grid-hyperlinked"},
                React.DOM.caption(null, "Associated Workflow Effects"),
                React.DOM.thead(null, 
                        React.DOM.tr(null, ["Workflow Name", "Effect Name"].map(function(header, i){
                            return React.DOM.th({key: i}, header)
                }))),
                React.DOM.tbody(null, props.effects.map(function(effect){
                    return React.DOM.tr({key: effect.EFFECT_ID,
                        onClick: function(event){
                            window.open("workflow.do?dataObjectKey=object.rfWorkflowEffect&trackingId=" + encodeURIComponent(effect.EFFECT_ID), 
                                    "_blank")
                        }.bind(this)},
                            ["WORKFLOW_NAME", "EFFECT_NAME"].map(function(effectProperty){
                                return React.DOM.td({key: effectProperty}, effect[effectProperty])
                            }))
                })))
    }
    
    function setupAssociatedEffects(){
        jQuery.post("page.request.do",
                {
            page: "rf.ajax.getEffectsUsingRfScript",
            rfScriptId: AeaFormUtilitiesJavascriptLibrary.getValue("trackingId")
        }).done(function(effects){
            var effectsContainer = jQuery("<td>")
            
            jQuery("#associatedRfWorkflowEffects")
                .parent()
                .prop({colspan: 1})
                .after(effectsContainer)
            
            ReactDOM.render(EffectsView({effects: effects}),
                    effectsContainer[0])
        })
    }
    
}(jQuery))

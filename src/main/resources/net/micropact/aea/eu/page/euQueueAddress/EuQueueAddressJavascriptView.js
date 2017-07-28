/* EU - Form - EU Queue Address - Javascript
 * ZRM */

"use strict";

(function(Tooltip) {
    addOnloadEvent(addTooltips)
    addOnloadEvent(setupAssociatedEmails)
    
    function addTooltips() {
        Tooltip.create()
            .done(function(tt) {
                tt.addSmartTips()
                jQuery('#attachments').append(tt.tooltip('Attachments'))
            })
    }
    
    function AssociatedEmailsView(props){
        return React.DOM.table({className: "grid aea-core-grid aea-core-grid-hyperlinked"},
                React.DOM.caption(null, "Associated Email"),
                React.DOM.thead(null, 
                        React.DOM.tr(null, ["Id", "Subject"].map(function(header, i){
                            return React.DOM.th({key: i}, header)
                }))),
                React.DOM.tbody(null, props.emails.map(function(email){
                    return React.DOM.tr({key: email.EMAIL_ID,
                        onClick: function(event){
                            window.open("admin.refdata.update.request.do?dataObjectKey=object.euEmailQueue&trackingId=" + encodeURIComponent(email.EMAIL_ID), 
                                    "_blank")
                        }.bind(this)},
                            ["EMAIL_ID", "SUBJECT"].map(function(emailProperty){
                                return React.DOM.td({key: emailProperty}, email[emailProperty])
                            }))
                })))
    }
    
    function setupAssociatedEmails(){
        jQuery.post("page.request.do",
                {
            page: "eu.form.euQueueAddress.ajax",
            euQueueAddressId: AeaFormUtilitiesJavascriptLibrary.getValue("trackingId")
        }).done(function(emails){
            var emailsContainer = jQuery("<td>")
            
            jQuery("#associatedEmails")
                .parent()
                .prop({colspan: 1})
                .after(emailsContainer)
            
            ReactDOM.render(AssociatedEmailsView({emails: emails}),
                    emailsContainer[0])
        })
    }
    
}(Tooltip))

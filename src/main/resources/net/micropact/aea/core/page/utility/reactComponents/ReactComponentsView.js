"use strict"

var AeaCoreReactComponents = (function() {

    var Application = React.createFactory(React.createClass({
        render: function() {
            return React.DOM.div(null,
                Header({
                    title: this.props.title
                }),
                Instructions({
                    instructions: this.props.instructions
                }),
                React.DOM.div({
                        className: "aea-core-mainContent"
                    },
                    this.props.mainContent),
                Footer(null))
        }
    }))

    var Header = React.createFactory(React.createClass({
        shouldComponentUpdate: function() {
            return false
        },
        render: function() {
            return React.DOM.ul({
                    className: "aea-core-navigation"
                },
                React.DOM.li({},
                    React.DOM.a({
                            href: "home.do"
                        },
                        "Home")),
                React.DOM.li({
                        className: "aea-core-active"
                    },
                    React.DOM.a({
                            href: window.location.href
                        },
                        this.props.title)))
        }
    }))

    var Footer = React.createFactory(React.createClass({
        shouldComponentUpdate: function() {
            return false
        },
        render: function() {
            return React.DOM.div({
                    id: "footer"
                },
                React.DOM.div({
                        id: "copyright"
                    },
                    "Powered by entellitrak \u24C7",
                    EtkIcon(null)))
        }
    }))

    var EtkIcon = React.createFactory(React.createClass({
        shouldComponentUpdate: function() {
            return false
        },
        render: function() {
            return React.DOM.img({
                border: "0",
                align: "absmiddle",
                alt: "Powered by entellitrak \u24C7",
                title: "Powered by entellitrak \u24C7",
                src: "themes/default/web-pub/images/logo/entellitrak-icon.gif"
            })
        }
    }))

    var Instructions = React.createFactory(React.createClass({
        shouldComponentUpdate: function() {
            return false
        },
        render: function() {
            var instructions = this.props.instructions
            var children
            if (instructions instanceof Array) {
                children = instructions.map(function(instruction, i) {
                    return React.DOM.p({
                        key: i
                    }, instruction)
                })
            } else {
                children = instructions
            }

            return React.DOM.div({
                    className: "aea-core-instructions"
                },
                children)
        }
    }))

    function Messages(props) {
        return props.messages.length > 0 && React.DOM.ul({
            className: "aea-core-message " + props.className
        }, props.messages.map(function(message, i) {
            return React.DOM.li({
                    key: i
                },
                message)
        }))
    }

    function Errors(props) {
        return Messages({
            messages: props.errors,
            className: "aea-core-error"
        })
    }

    function Successes(props) {
        return Messages({
            messages: props.successes,
            className: "aea-core-success"
        })
    }

    var Loading = React.createFactory(React.createClass({
        shouldComponentUpdate: function() {
            return false
        },
        render: function() {
            return React.DOM.div({
                className: "aea-core-loading"
            }, "Loading")
        }
    }))

    function DataImport(props) {
        return React.DOM.div(null,
            Errors({
                errors: props.errors
            }),
            Successes({
                successes: props.importCompleted ? ["The file has been imported"] : []
            }),
            React.DOM.form({
                    method: "POST",
                    action: "page.request.do?page=" + props.importPageKey,
                    encType: "multipart/form-data"
                },
                AeaStackForm.StackData({
                    children: [AeaStackForm.Hidden({
                            name: "update",
                            value: "1",
                            key: 0
                        }),
                        AeaStackForm.File({
                            name: "importFile",
                            label: "File",
                            key: 1
                        }),
                        AeaStackForm.Buttons({
                            buttons: [{
                                type: "submit",
                                name: "Import"
                            }],
                            key: 2
                        })
                    ]
                })))
    }

    function MakeRequiredIcon(){
        return React.DOM.img({title: "Required",
            alt: "Required",
            src: "themes/default/web-pub/images/icons/required.gif"})
    }
    
    function ImportPage(props) {
        return Application({
            title: props.componentName + " Data Import",
            instructions: ["This page is used to import data for the " + props.componentName + " component.",
                "It ingests a file which is generated by the page " + props.exportPageName + ".",
                "It is recommended that you backup your database before importing."
            ],
            mainContent: DataImport(props)
        })
    }
    
    function SimpleHeaderTableGrid(props){
        var headers = props.headers
        var caption = props.caption
        var className = props.className
        
        return React.DOM.table({className: "grid" + (className ? " " + className : "")},
                caption ? React.DOM.caption(null, caption) : null,
                React.DOM.thead(null,
                        React.DOM.tr(null,
                                headers.map(function(header, i){
                                    return React.DOM.th({key: i}, header)
                                }))),
                props.tbody)
    }
    
    return {
        Application: Application,
        Header: Header,
        Footer: Footer,
        EtkIcon: EtkIcon,
        Instructions: Instructions,
        Messages: Messages,
        Errors: Errors,
        Successes: Successes,
        Loading: Loading,
        MakeRequiredIcon: MakeRequiredIcon,
        
        SimpleHeaderTableGrid: SimpleHeaderTableGrid,
        
        ImportPage: ImportPage
    }
}())

/* 
 * EXPERIMENTAL!!!!!!!
 * This will be a set of components meant to be similar to the entellitrak stack form 
 * */
var AeaStackForm = (function() {

    function Cell(props) {
        return React.DOM.div(null,
            props.item)
    }

    function Row(props) {
        return React.DOM.div(props,
            Cell({
                item: Label({
                    label: props.label
                })
            }),
            Cell({
                item: props.element
            }))
    }

    function Label(props) {
        return React.DOM.label(null, props.label)
    }

    function FileInput(props) {
        return React.DOM.input({
            type: "file",
            name: props.name,
        })
    }

    function TextInput(props) {
        return React.DOM.input({
            type: "text",
            name: props.name
        })
    }

    function HiddenInput(props) {
        return React.DOM.input({
            type: "hidden",
            name: props.name,
            value: props.value
        })
    }

    function File(props) {
        return Row({
            key: props.key,
            label: props.label,
            element: FileInput(props)
        })
    }

    function Text(props) {
        return Row({
            key: props.key,
            label: props.label,
            element: TextInput(props)
        })
    }

    function Hidden(props) {
        return Row({
            key: props.key,
            className: "aea-core-hidden",
            label: props.label,
            element: HiddenInput(props)
        })
    }

    function Buttons(props) {
        return Row({
            key: props.key,
            element: React.DOM.div(null,
                props.buttons.map(function(buttonProps, i) {
                    buttonProps.key = i
                    return React.DOM.button(buttonProps, buttonProps.name)
                }))
        })
    }

    function StackData(props) {
        return React.DOM.div({
            className: "aea-core-stackData"
        }, props.children)
    }

    return {
        StackData: StackData,
        Row: Row,
        Cell: Cell,
        Label: Label,

        FileInput: FileInput,
        TextInput: TextInput,
        HiddenInput: HiddenInput,

        File: File,
        Text: Text,
        Hidden: Hidden,
        Buttons: Buttons
    }
}())
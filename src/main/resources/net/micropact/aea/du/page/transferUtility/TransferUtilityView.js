"use strict"

var AeaTransferUtility = (function(DUC) {

    function makeTransfer(users, items, toUser, initialSelectedFromUsers, initialSelectedItems) {
        var state = {
            users: users,
            items: items,

            selectedFromUsers: {},
            selectedToUser: toUser,
            selectedItems: {},
            selectAllUsers: false,
            selectAllItems: false,

            userIdIndex: {},
            itemIdIndex: {},
            itemsByUserIdIndex: {},

            listeners: []
        }

        /* Begin state initialization functions */
        addIndices()

        jQuery(initialSelectedItems).each(function(i, itemId) {
            selectItem(itemId)
        })

        jQuery(initialSelectedFromUsers).each(function(i, userId) {
            selectFromUser(userId)
        })

        function addMultiIndex(index, key, value) {
            if (key) {
                if (index[key] === undefined) {
                    index[key] = [] // Should probably also use a Map for this
                }
                index[key].push(value)
            }
        }

        function addIndices() {
            var users = state.users
            var items = state.items

            for (var u = 0; u < users.length; u++) {
                var user = users[u]
                state.userIdIndex[user.USER_ID] = user
            }

            for (var p = 0; p < items.length; p++) {
                var item = items[p]
                state.itemIdIndex[item.ITEM_ID] = item

                addMultiIndex(state.itemsByUserIdIndex, item.USER_ID, item)
            }
        }

        function callListeners() {
            for (var i = 0; i < state.listeners.length; i++) {
                state.listeners[i]()
            }
        }

        function addListener(func) {
            state.listeners.push(func)
        }

        function removeListener(func) {
            for (var i = 0; i < state.listeners.length; i++) {
                if (state.listeners[i] == func) {
                    state.listeners.splice(i, 1)
                    return
                }
            }
            console.error("Listener not found.")
        }

        function getSelectedToUser() {
            return state.selectedToUser
        }

        function getUsersWithCreate() {
            return state.users.filter(function(user, i) {
                return user.HASCREATE == 1
            })
        }

        /* Begin State interaction functions */
        function getUser(userId) {
            return state.userIdIndex[userId]
        }

        function getItem(itemId) {
            return state.itemIdIndex[itemId]
        }

        function getNumberOfUserItems(userId) {
            var items = state.itemsByUserIdIndex[userId]
            return items ? items.length : 0
        }

        function getUsersWithItems() {
            return jQuery(state.users).filter(function(i, user) {
                return getNumberOfUserItems(user.USER_ID) > 0
            }).toArray()
        }

        function getSelectedFromUsersItems() {
            var arr = []
            for (var userId in state.selectedFromUsers) {
                arr = arr.concat(state.itemsByUserIdIndex[userId] || [])
            }

            return arr.sort(function(a, b) {
                return a.NAME < b.NAME ? -1 : a.NAME == b.NAME ? 0 : 1
            })
        }

        function selectFromUser(userId) {
            state.selectedFromUsers[userId] = getUser(userId)
            callListeners()
        }

        function deselectFromUser(userId) {
            delete state.selectedFromUsers[userId]
            callListeners()
        }

        function itemIsSelected(itemId) {
            return state.selectedItems[itemId] !== undefined
        }

        function fromUserSelected(userId) {
            return state.selectedFromUsers[userId] !== undefined
        }

        function selectItem(itemId) {
            state.selectedItems[itemId] = getItem(itemId)
            callListeners()
        }

        function deselectItem(itemId) {
            delete state.selectedItems[itemId]
            callListeners()
        }

        function toggleSelectAllItems() {
            var isSelected = state.selectAllItems

            if (isSelected) {
                state.selectedItems = {};
            } else {
                jQuery(getSelectedFromUsersItems()).each(function(i, item) {
                    selectItem(item.ITEM_ID)
                })
            }
            state.selectAllItems = !state.selectAllItems
            callListeners()
        }

        function toggleFromUser(userId) {
            var isSelected = fromUserSelected(userId)
            if (isSelected) {
                deselectFromUser(userId)
            } else {
                selectFromUser(userId)
            }

            state.selectAllItems = false;

            callListeners()
        }

        function selectToUser(userId) {
            state.selectedToUser = userId
            callListeners()
        }

        return {
            getUsersWithCreate: getUsersWithCreate,
            getUser: getUser,
            getItem: getItem,
            getNumberOfUserItems: getNumberOfUserItems,
            getUsersWithItems: getUsersWithItems,
            getSelectedFromUsersItems: getSelectedFromUsersItems,
            itemIsSelected: itemIsSelected,
            fromUserSelected: fromUserSelected,
            selectItem: selectItem,
            deselectItem: deselectItem,
            toggleSelectAllItems: toggleSelectAllItems,
            toggleFromUser: toggleFromUser,
            selectToUser: selectToUser,
            getSelectedToUser: getSelectedToUser,

            addListener: addListener,
            removeListener: removeListener
        }
    }


    function Transferred(props) {
        return props.displayTransferred && 
        AeaCoreReactComponents.Successes({successes: [props.transferType + "s Successfully Transferred"]})
    }

    var TransferTo = React.createFactory(React.createClass({
        render: function() {
            var options = this.props.transfer.getUsersWithCreate().map(function(user) {
                return React.DOM.option({
                        value: user.USER_ID,
                        key: user.USER_ID
                    },
                    user.USERNAME)
            })

            return React.DOM.div(null,
                React.DOM.label({
                    className: "formItem"
                }, "Transfer To"),
                React.DOM.select({
                        name: "toUser",
                        value: this.props.transfer.getSelectedToUser(),
                        onChange: function(event) {
                            this.props.transfer.selectToUser(event.target.value)
                        }.bind(this)
                    },
                    React.DOM.option(),
                    options))
        }
    }))

    var UsersWithItems = React.createFactory(React.createClass({
        render: function() {
            var userItems = this.props.transfer.getUsersWithItems().map(function(user, i) {

                var toggleUser = function() {
                    this.props.transfer.toggleFromUser(user.USER_ID)
                }.bind(this)

                return React.DOM.li({
                        key: user.USER_ID
                    },
                    React.DOM.input({
                        type: "checkbox",
                        name: "fromUsers",
                        value: user.USER_ID,
                        checked: this.props.transfer.fromUserSelected(user.USER_ID),
                        onChange: toggleUser
                    }),
                    React.DOM.label({
                            onClick: toggleUser
                        },
                        React.DOM.span(null, user.USERNAME),
                        React.DOM.span({
                            className: "aea-core-count"
                        }, this.props.transfer.getNumberOfUserItems(user.USER_ID))))
            }.bind(this))
            return React.DOM.div(null,
                React.DOM.label({
                    className: "formItem"
                }, "Users with " + this.props.transferType + "s"),
                React.DOM.ul({
                        className: "fromUsers formItem"
                    },
                    userItems))
        }
    }))

    var SelectAllButton = React.createFactory(React.createClass({
        render: function() {
            return React.DOM.input({
                type: "checkbox",
                checked: this.props.transfer.selectAllItems,
                onChange: this.props.transfer.toggleSelectAllItems
            })
        }
    }))

    var Items = React.createFactory(React.createClass({
        render: function() {

            var rows = this.props.transfer.getSelectedFromUsersItems().map(function(item) {
                var isSelected = this.props.transfer.itemIsSelected(item.ITEM_ID)
                return React.DOM.tr({
                        key: item.ITEM_ID
                    },
                    React.DOM.td(null, React.DOM.input({
                        type: "checkbox",
                        name: "items",
                        value: item.ITEM_ID,
                        checked: isSelected,
                        onChange: function() {
                            if (isSelected) {
                                this.props.transfer.deselectItem(item.ITEM_ID)
                            } else {
                                this.props.transfer.selectItem(item.ITEM_ID)
                            }
                        }.bind(this)
                    })),
                    React.DOM.td(null, item.NAME),
                    React.DOM.td(null, this.props.transfer.getUser(item.USER_ID).USERNAME))
            }.bind(this))

            return React.DOM.table({
                    className: "grid aea-core-grid"
                },
                React.DOM.thead(null, React.DOM.tr(null,
                    React.DOM.th(null, React.DOM.label(null,
                        SelectAllButton({
                            transfer: this.props.transfer
                        }),
                        "Select All/None")),
                    React.DOM.th(null, this.props.transferType + " Name"),
                    React.DOM.th(null, "Owner"))),
                React.DOM.tbody(null, rows))
        }
    }))

    var Submit = React.createFactory(React.createClass({
        render: function() {
            return React.DOM.input({
                type: "submit",
                value: "Transfer " + this.props.transferType + "s"
            })
        }
    }))

    var ItemMover = React.createFactory(React.createClass({
        getInitialState: function() {
            return {
                transfer: this.props.transfer,
                transferCallback: null
            }
        },
        componentWillMount: function() {
            var callback = function() {
                this.setState({
                    transfer: this.state.transfer
                })
            }.bind(this)
            this.setState({
                transferCallback: callback
            })
            this.state.transfer.addListener(callback)
        },
        componentWillUnmount: function() {
            this.state.transfer.removeListener(this.state.transferCallback)
        },
        render: function() {
            var transferToProps = {
                transfer: this.props.transfer
            }

            return React.DOM.div(null,
                Transferred({
                    displayTransferred: this.props.displayTransferred,
                    transferType: this.props.transferType
                }),
                DUC.Errors({
                    errors: this.props.errors
                }),
                React.DOM.form({
                        method: "POST",
                        action: this.props.submissionUrl
                    },
                    React.DOM.input({
                        type: "hidden",
                        name: "action",
                        value: "transfer"
                    }),
                    UsersWithItems({
                        transfer: this.props.transfer,
                        transferType: this.props.transferType
                    }),
                    TransferTo(transferToProps),
                    Items({
                        transfer: this.props.transfer,
                        transferType: this.props.transferType
                    }),
                    TransferTo(transferToProps),
                    Submit({
                        transfer: this.props.transfer,
                        transferType: this.props.transferType
                    })))
        }
    }))

    var ItemMoverApp = React.createFactory(React.createClass({
        render: function() {

            return DUC.Application({
                title: "Transfer " + this.props.transferType + " Ownership",
                instructions: ["This page can transfer ownership of " + this.props.transferType + "s from one user to another.",
                    "This can become important when a developer leaves or deployments need to be done to production.",
                    "Currently if you delete a user, it deletes their " + this.props.transferType + "s and this can be a problem."
                ],
                mainContent: ItemMover({
                    transfer: this.props.transfer,
                    displayTransferred: this.props.displayTransferred,
                    errors: this.props.errors,
                    submissionUrl: this.props.submissionUrl,
                    transferType: this.props.transferType
                })
            })
        }
    }))

    return {
        makeTransfer: makeTransfer,
        ItemMoverApp: ItemMoverApp
    }
}(AeaCoreReactComponents))

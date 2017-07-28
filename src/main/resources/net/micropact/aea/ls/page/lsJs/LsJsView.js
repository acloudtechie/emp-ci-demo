/**
 * LS - Form - LS Script - Javascript
 *
 **/
 
var LiveSearch = Class.create();
LiveSearch.prototype = {
    initialize : function () {
        this.breakText = '@r@Nd0mP@tT3rn@';

        this.dataObjectKey = document.dataForm.dataObjectKey.value;
        this.dataFormId = document.dataForm.dataFormId.value;
        this.trackingId = document.dataForm.trackingId.value;
        this.baseId = document.dataForm.baseId.value == '' ?
            document.dataForm.trackingId.value :
            document.dataForm.baseId.value;
        this.parentId = document.dataForm.parentId.value == '' ?
            document.dataForm.trackingId.value :
            document.dataForm.parentId.value;
        
    },
    performLiveSearch : function (formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        var scope = this;
        var inputVal = aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').val();

        if ((inputVal != null) && (inputVal.length >= $charactersNeededForSearch)) {

            var replacementParamArray = [];
            var paramsNeeded = scope.replacementParamsNeeded[formElementPrefix + dataElementId];

            if (paramsNeeded) {
                for (var i = 0; i < paramsNeeded.length; i++) {
                    replacementParamArray[i] = paramsNeeded[i] + scope.breakText +
                        scope.getElementValue(formElementPrefix, paramsNeeded[i]);
                }
            }
            
            var elementName = dataElementId;
            
            if (isUnboundFormControl) {
			   elementName = formElementPrefix + dataElementId;
		    }

            new Ajax.Request('page.request.do', {
                parameters : {
                    page : 'ls.form.lsScript.ajax',
                    trackingId : scope.trackingId,
                    parentId : scope.parentId,
                    baseId : scope.baseId,
                    dataObjectKey : scope.dataObjectKey,
                    dataFormId : scope.dataFormId,
                    dataElementId : elementName,
                    columnHeaders : columnHeaders,
                    replacementParams : replacementParamArray,
                    userEnteredSearch : inputVal,
                    operation : 'displayLiveSearchResults'
                },
                onCreate : function () {
                    show('loading');
                },
                onFailure : function () {
                    hide('loading');
                },
                onComplete : function (transport) {
                
                   if (!aeaJQ('#' + formElementPrefix + dataElementId + '_undoButton').is(':visible')) {
                       //If the user already clicked undo, don't render the table
                       hide('loading');
                   } else {
	                   var result = transport.responseText.evalJSON().jsonResult;
	                
	                   if ('true' == result.isValid) {
	                    	scope.renderLSResultTable.bind(scope)(result.tableResult, formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl, inputVal);
	                   } else {
	                   		if ('$!debugMode' == 'true') {
	                   		   alert(result.errorMessage);
	                   		}
	                   }
	                   
	                   hide('loading');
	               }
                }
            });
        } else {
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResults-container').remove();
        }
    },
    performLiveSearchMS : function (formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        var scope = this;
        var inputVal = aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').val();

        if ((inputVal != null) && (inputVal.length >= $charactersNeededForSearch)) {

            var replacementParamArray = [];
            var paramsNeeded = scope.replacementParamsNeeded[formElementPrefix + dataElementId];

            if (paramsNeeded) {
                for (var i = 0; i < paramsNeeded.length; i++) {
                    replacementParamArray[i] = paramsNeeded[i] + scope.breakText +
                        scope.getElementValue(formElementPrefix, paramsNeeded[i]);
                }
            }
			
			var elementId = dataElementId;
			if (isUnboundFormControl) {
				elementId = formElementPrefix + dataElementId;
			}

            new Ajax.Request('page.request.do', {
                parameters : {
                    page : 'ls.form.lsScript.ajax',
                    trackingId : scope.trackingId,
                    parentId : scope.parentId,
                    baseId : scope.baseId,
                    dataObjectKey : scope.dataObjectKey,
                    dataFormId : scope.dataFormId,
                    dataElementId : elementId,
                    columnHeaders : columnHeaders,
                    replacementParams : replacementParamArray,
                    userEnteredSearch : inputVal,
                    operation : 'displayLiveSearchResults'
                },
                onCreate : function () {
                    show('loading');
                },
                onFailure : function () {
                    hide('loading');
                },
                onComplete : function (transport) {
                   if (!aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').is(':visible')) {
                       //If the user already clicked undo, don't render the table
                       hide('loading');
                   } else { 
	                   var result = transport.responseText.evalJSON().jsonResult;
	                
	                   if ('true' == result.isValid) {
	                    	scope.renderLSResultTableMS.bind(scope)(result.tableResult, formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
	                   } else {
	                      if ('$!debugMode' == 'true') {
	                   		  alert(result.errorMessage);
	                      }
	                   }
	                   
	                   hide('loading');
	               }
                }
            });
        } else {
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResultsDiv').empty();
            aeaJQ('#' + formElementPrefix + dataElementId + '_undoButton').show();
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchCloseButton').hide();
            
        }
    },
    renderLSResultTable : function (liveSearchData, formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl, inputVal) {
        var scope = this;
        var label = aeaJQ.trim(aeaJQ('#' + formElementPrefix + dataElementId + '-container .formLabel').text());

        // Create placeholder div if it doesn't exist yet
        if (aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResultsDiv').length == 0) {

            aeaJQ('#' + formElementPrefix + dataElementId + '-container').after(aeaJQ(
                    '<tr id ="' + formElementPrefix + dataElementId + '_liveSearchResults-container"> ' +
                    '<td> </td>' +
                    '<td>' +
                    '<div style="float:left; width:100%; margin-right:-25px;" role="alert" aria-live="assertive" ' +
                    'id="' + formElementPrefix + dataElementId + '_liveSearchResultsDiv' + '"></div>' +
                    '<div id="' + formElementPrefix + dataElementId + '_liveSearchResultsCloseDiv" style="float:right;padding: 6px 8px 0 0;">' +
                    '<a id="' + formElementPrefix + dataElementId + '_liveSearchCloseButton" class="liveSearchCloseResults" ' +
                    'href="javascript:void(0)" >' +
                    '<img alt="Close Live Search Results" src="$web_pub_path/images/icons/16x16/close.png"></a></div>' +
                    '</td>' +
                    '</tr>'));
        }

        var numberOfColumns = columnHeaders.length;
        var helpText = '';
        for (var i = 0; i < numberOfColumns; i++) {
           helpText = helpText + 'Column ' + (i+1) + ' Label reads ' + columnHeaders[i].split(":")[1];
           
           if ((i+1) < numberOfColumns) {
              helpText = helpText + ', ';
           }
         }
        

        // Generate table with the result matches for the user to select from
        aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResultsDiv')
        .empty()
        .append(aeaJQ('<table/>', {
                'id' : formElementPrefix + dataElementId + '_liveSearchResultMainTable',
                'class' : 'grid',
                'summary' : 'Search result table for user input value "' 
                           + inputVal 
                           + '" containing ' 
                           + numberOfColumns + ' columns. ' + helpText
            })
            .append(liveSearchData));

        aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchCloseButton').click(function () {
            var ddMenu = aeaJQ('select#' + formElementPrefix + dataElementId);
            var ddMenuSelected = aeaJQ('select#' + formElementPrefix + dataElementId + ' :selected');

            aeaJQ('#' + formElementPrefix + dataElementId + '_span').remove();
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResultsDiv').empty();
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResults-container').hide();
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').hide();

            #if ($isAccessibilityEnhanced)
            ddMenu.after(aeaJQ('<span>', {
                        'id' : formElementPrefix + dataElementId + '_span'
                    }).append(aeaJQ('<input>', {
                       'id' : formElementPrefix + dataElementId + '_readOnlyInput',
                       'type' : 'text',
                       'value' : ddMenuSelected.text(),
                       'readonly' : '',
                       'title' : 'Live Search field ' + label + '. Current value for field ' + label + ' is ' + ((ddMenuSelected.text() == '') ? 'blank.' : ddMenuSelected.text())
                    }))
                );
            #else
                ddMenu.after(aeaJQ('<span>', {
                        'id' : formElementPrefix + dataElementId + '_span'
                }).text(ddMenuSelected.text()));
            #end

            scope.addSearchIcon.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
            scope.addDeleteIcon.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);

            aeaJQ('#' + formElementPrefix + dataElementId + '_undoButton').hide();
            aeaJQ('#' + formElementPrefix + dataElementId + '_deleteButton').show();
            
            return false;
        });

        aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResults-container').show();

        var valueToReplace = 'TR_' + dataElementId + '_';
        
        if (isUnboundFormControl) {
           valueToReplace = 'TR_' + formElementPrefix + dataElementId + '_';
        }

        aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResultMainTable tbody tr').each(function () {
            var tableRow = aeaJQ(this);

            tableRow.click(function () {

                var rowId = aeaJQ(this).attr('id').replace(valueToReplace, '');
                var name = aeaJQ(this).attr('name').replace(valueToReplace, '');

                var mainDDMenu = aeaJQ('select#' + formElementPrefix + dataElementId);

                if (mainDDMenu.size() > 0) {
                    mainDDMenu.empty();
                    mainDDMenu.append(aeaJQ('<option>').attr('value', rowId).attr('selected', 'selected').text(name));
                    mainDDMenu.trigger('change');
                }

                aeaJQ('#' + formElementPrefix + dataElementId + '_span').remove();
                
                
                #if ($isAccessibilityEnhanced)
                 mainDDMenu.after(aeaJQ('<span>', {
                        'id' : formElementPrefix + dataElementId + '_span'
                    }).append(aeaJQ('<input>', {
                       'id' : formElementPrefix + dataElementId + '_readOnlyInput',
                       'type' : 'text',
                       'value' : name,
                       'readonly' : '',
                       'title' : 'Live Search field ' + label + '. Current value for field ' + label + ' is ' + ((name == '') ? 'blank.' : name) 
                    }))
                );
                #else
                mainDDMenu.after(aeaJQ('<span>', {
                        'id' : formElementPrefix + dataElementId + '_span'
                    }).text(name));
                #end
                    
                aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResultsDiv').empty();
                aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResults-container').hide();
                aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').hide();

                scope.addSearchIcon.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
                scope.addDeleteIcon.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);

                aeaJQ('#' + formElementPrefix + dataElementId + '_undoButton').hide();
                aeaJQ('#' + formElementPrefix + dataElementId + '_deleteButton').show();
                aeaJQ('#' + formElementPrefix + dataElementId + '_searchButton').focus();
                
                return false;
            });
        });
    },
    renderLSResultTableMS : function (liveSearchData, formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        var scope = this;
        
        //Hide the undo button and show the close button for the result table.
        aeaJQ('#' + formElementPrefix + dataElementId + '_undoButton').hide();
        aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchCloseButton').show();

        // Create placeholder div if it doesn't exist yet
        if (aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResultsDiv').length == 0) {

            aeaJQ('#' + formElementPrefix + dataElementId + '-container').after(aeaJQ(
                    '<tr id ="' + formElementPrefix + dataElementId + '_liveSearchResults-container"> ' +
                    '<td> </td>' +
                    '<td>' +
                    '<div style="float:left; width:100%; margin-right:-25px;" ' +
                    'id="' + formElementPrefix + dataElementId + '_liveSearchResultsDiv' + '"></div>' +
                    '<div id="' + formElementPrefix + dataElementId + '_liveSearchResultsCloseDiv" style="float:right; padding: 6px 8px 0 0;">' +
                    '<a id="' + formElementPrefix + dataElementId + '_liveSearchCloseButton" class="liveSearchCloseResults" ' +
                    'href="javascript:void(0)" >' +
                    '<img alt="Close Live Search Results" src="$web_pub_path/images/icons/16x16/close.png"></a></div>' +
                    '</td>' +
                    '</tr>'));
        }

		//Get the form label for the data element.
		var msLabel = aeaJQ.trim(aeaJQ('#' + formElementPrefix + dataElementId + '-container .formLabel').text());
        var tableSummary = 'Table containing the results for search value ' + 
                           aeaJQ('input#' + formElementPrefix + dataElementId +'_liveSearchInput').val();

        // Generate table with the result matches for the user to select from
        aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResultsDiv')
        .empty()
        .append(aeaJQ('<table />', {
                'id' : formElementPrefix + dataElementId + '_liveSearchResultMainTable',
                'summary' : tableSummary,
                'class' : 'grid',
                'role' : 'alert',
                'aria-live' : 'assertive'
            })
            .append(liveSearchData));

		//When the live search close button is clicked, close the result table and show the search button.
        aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchCloseButton').unbind().click(function () {
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResultsDiv').empty();
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResults-container').hide();
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').val('');
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').hide();
            aeaJQ('#' + formElementPrefix + dataElementId + '_searchButton').show().focus();
            return false;
        });

        aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResults-container').show();

        var valueToReplace = 'TR_' + dataElementId + '_';
        
        if (isUnboundFormControl) {
           valueToReplace = 'TR_' + formElementPrefix + dataElementId + '_';
        }
        
		//Loop through each result in the table that was just created to create event listeners for the
		//resulting rows.
        aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResultMainTable tbody tr').each(function () {
            

            var tmpRowId = aeaJQ(this).attr('id').replace(valueToReplace, '');
            var rowSize =
                aeaJQ('div#' + formElementPrefix + dataElementId + '_multiValue fieldset input[value="' + tmpRowId + '"]').size();

            if (rowSize > 0) {
                aeaJQ(this).css("background", "#C2FFC2");
            }
          	scope.selectMsResult.bind(this)(formElementPrefix, dataElementId, valueToReplace, msLabel);

        });
    },
    selectMsResult : function (formElementPrefix, dataElementId, valueToReplace, msLabel) {
            var tableRow = aeaJQ(this);
            
            //When a user clicks on a row in the table.
            tableRow.click(function (event) {

                var rowId = aeaJQ(this).attr('id').replace(valueToReplace, '');
                var name = aeaJQ(this).attr('name').replace(valueToReplace, '');

                var msMenu = aeaJQ('div#' + formElementPrefix + dataElementId + '_multiValue fieldset');

				//check to see if the value is already in the MS list. if it is not, create a new input and label
				//value and append it to the MS list on the form, along with its delete button and image.
				
				var matchingInput = msMenu.find('input[value="' + rowId + '"]');
                if (matchingInput.size() == 0) {

                    var newDeleteButton =
                        aeaJQ('<a />', {
                        	  'class' : 'deleteLiveSearchMSButton',
                        	  'href' : 'javascript:void(0);',
                        	   #if ($isAccessibilityEnhanced)
                                 'style' : 'display: none;'
                               #else
                                 'style' : 'padding:0px 3px 0px 6px;'
                               #end
                    });

                    var newInput = aeaJQ('<input />', {
                           'id' : formElementPrefix + dataElementId + '_' + rowId,
                           'type' : 'checkbox',
                           'checked' : '',
                           'value' : rowId,
                           #if (!$isAccessibilityEnhanced)
                           'style' : 'display: none;',
                           #end
                           'name' : formElementPrefix + dataElementId
                    });
                    
                    #if ($isAccessibilityEnhanced)
	                    newInput.keypress(function (e) {
							 var key = e.which;
							 if(key == 13) {
							    newDeleteButton.click();
							    return false;
							 }
                    });
                    
	                    newInput.click(function() {
	                       newDeleteButton.click();
	                    });
                    #end
                    
                    var newLabel = aeaJQ('<label />', {
                        'for' : formElementPrefix + dataElementId + '_' + rowId
                    });
                    
                    newLabel.text(' ' + name);
                    newLabel.prepend(newInput);
                    msMenu.append(newLabel);

                        
                   var newDeleteButtonImage =
                   		aeaJQ('<img />', {
                        	  'src' : '$web_pub_path/images/icons/16x16/delete.png',
                        	  'alt' : 'Delete value ' + name + ' from ' + msLabel + ' list.'
                        });
                        
                   newDeleteButton.append(newDeleteButtonImage);

                    msMenu.append(newDeleteButton);
                    msMenu.append(aeaJQ('<br>'));

                    newDeleteButton.click(function () {
                        var currentRowId = aeaJQ(this).prev().find('input').val();

                        aeaJQ(this).prev().remove();
                        aeaJQ(this).next().remove();
                        
                        var parentObject = aeaJQ(this).parent();
                    	aeaJQ(this).remove();
                    	parentObject.trigger('change');

                        aeaJQ('#' +
                            formElementPrefix +
                            dataElementId +
                            '_liveSearchResultMainTable tbody tr[id="TR_' +
                            dataElementId +
                            '_' +
                            currentRowId +
                            '"]').css("background", "white")

                    });
                    
                    aeaJQ(this).css("background", "#C2FFC2");
                } else {

                	//If there is a line break preceding the input, remove it.
                	if ('BR' == matchingInput.parent().prev().prop('tagName')) {
                	   matchingInput.parent().prev().remove();
                	}
                	
                	matchingInput.parent().next().remove(); //Remove delete button;
                	matchingInput.parent().remove(); //Remove the selection itself.
                	
                	//Removes the first BR tag in the MS list
                	var firstBr = aeaJQ('div#' + formElementPrefix + dataElementId + '_multiValue legend').next();
                    if (firstBr.is('br')) {
                        firstBr.remove();
                    }
                	
                	aeaJQ(this).css("background", "white");
                }
                
                //Trigger change on add or remove of element.
                msMenu.trigger('change');
                
                return false;
            });
    },
    addSearchIcon : function (formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        var scope = this;
        var msLabel = aeaJQ.trim(aeaJQ('#' + formElementPrefix + dataElementId + '-container .formLabel').text());
        var searchButton = aeaJQ('#' + formElementPrefix + dataElementId + '_searchButton');

        if (searchButton.size() > 0) {
            searchButton.show();
        } else {
             var searchButton = aeaJQ('<a />', {
        	                         'id' : formElementPrefix + dataElementId + '_searchButton',
        	                         'class' : 'search',
        	                         'href' : 'javascript:void(0);',
        	                         'style' : 'padding: 0px 3px 0px 6px;'
        						});
        	
        	var searchButtonImage = aeaJQ('<img />', {
        								'src' : '$web_pub_path/images/icons/16x16/standard_search.png',
        								'alt' : 'Open ' + msLabel + ' Live Search.'
        						});

            searchButton.append(searchButtonImage);
            aeaJQ('#' + formElementPrefix + dataElementId + '_span').after(searchButton);
            
            searchButton.click(function () {
                scope.createSearchField.bind(scope)(false, formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
                scope.addUndoButton.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
                scope.performLiveSearch.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);

                aeaJQ(this).hide();
                aeaJQ('#' + formElementPrefix + dataElementId + '_deleteButton').hide();
                
                return false;
            });
        }
    },
    addDeleteIcon : function (formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        var scope = this;
        var deleteButton = aeaJQ('#' + formElementPrefix + dataElementId + '_deleteButton');
        var label = aeaJQ.trim(aeaJQ('#' + formElementPrefix + dataElementId + '-container .formLabel').text());

        if (deleteButton.size() > 0) {
            deleteButton.show();
        } else {
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').after(
               aeaJQ('<a />', {
            
                    'id' : formElementPrefix + dataElementId + '_deleteButton',
                    'class' : 'search',
                    'href' : 'javascript:void(0);',
                    'style' : 'padding:0px 3px 0px 6px;'
                    }).append(aeaJQ('<img />', {
                    	      'src' : '$web_pub_path/images/icons/16x16/delete.png',
                    	      'alt' : 'Delete Button for ' + label
                    	     })
                    ));

            aeaJQ('#' + formElementPrefix + dataElementId + '_deleteButton').click(function () {
                aeaJQ('select#' + formElementPrefix + dataElementId).empty();
                aeaJQ('select#' + formElementPrefix + dataElementId).append(aeaJQ('<option value=""></option>'));
                aeaJQ('select#' + formElementPrefix + dataElementId).trigger('change');
                
                #if ($isAccessibilityEnhanced)
                    aeaJQ('#' + formElementPrefix + dataElementId + '_readOnlyInput').val('');
                    aeaJQ('#' + formElementPrefix + dataElementId + '_readOnlyInput')
                       .attr('title',  'Live Search field ' + label + '. Current value for field ' + label + ' is blank.');
                #else
                aeaJQ('#' + formElementPrefix + dataElementId + '_span').text('');
                #end
                
                return false;
            });
        }
    },
    addUndoButton : function (formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        var scope = this;
        var undoButton = aeaJQ('#' + formElementPrefix + dataElementId + '_undoButton');
        var label = aeaJQ.trim(aeaJQ('#' + formElementPrefix + dataElementId + '-container .formLabel').text());

        if (undoButton.size() > 0) {
            undoButton.show();
        } else {
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').after
            (aeaJQ('<a />', {
            
                    'id' : formElementPrefix + dataElementId + '_undoButton',
                    'class' : 'undo',
                    'href' : 'javascript:void(0);',
                    'style' : 'padding:0px 3px 0px 6px;'
                    }).append(aeaJQ('<img />', {
                    	      'src' : '$web_pub_path/images/icons/16x16/undo.png',
                    	      'alt' : 'Undo Button for ' + label
                    	     })
                    ));

            aeaJQ('#' + formElementPrefix + dataElementId + '_undoButton').click(function () {
                var ddMenu = aeaJQ('select#' + formElementPrefix + dataElementId);
                var ddMenuSelected = aeaJQ('select#' + formElementPrefix + dataElementId + ' :selected');

                aeaJQ('#' + formElementPrefix + dataElementId + '_span').remove();
                aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResultsDiv').empty();
                aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResults-container').hide();
                aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').hide();

                #if ($isAccessibilityEnhanced)
                  ddMenu.after(aeaJQ('<span>', {
                        'id' : formElementPrefix + dataElementId + '_span'
                    }).append(aeaJQ('<input>', {
                       'id' : formElementPrefix + dataElementId + '_readOnlyInput',
                       'type' : 'text',
                       'value' : ddMenuSelected.text(),
                       'readonly' : '',
                       'title' : 'Live Search field ' + label + '. Current value for field ' + label + ' is ' + ((ddMenuSelected.text() == '') ? 'blank.' : ddMenuSelected.text())
                    }))
                );
                #else
                ddMenu.after(aeaJQ('<span>', {
                        'id' : formElementPrefix + dataElementId + '_span'
                    }).text(ddMenuSelected.text()));
                #end
                
                scope.addSearchIcon.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
                scope.addDeleteIcon.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
                aeaJQ(this).hide();
                aeaJQ('#' + formElementPrefix + dataElementId + '_deleteButton').show();
                
                #if ($isAccessibilityEnhanced)
                   aeaJQ('input#' + formElementPrefix + dataElementId + '_readOnlyInput').focus();
                #end
                
                return false;
            });
        }
    },
    addUndoButtonMS : function (formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        var scope = this;
        var undoButton = aeaJQ('#' + formElementPrefix + dataElementId + '_undoButton');
        var label = aeaJQ.trim(aeaJQ('#' + formElementPrefix + dataElementId + '-container .formLabel').text());

        if (undoButton.size() > 0) {
            undoButton.show();
        } else {
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').after
            (aeaJQ('<a />', {
            
                    'id' : formElementPrefix + dataElementId + '_undoButton',
                    'class' : 'undo',
                    'href' : 'javascript:void(0);',
                    'style' : 'padding:0px 3px 0px 6px;'
                    }).append(aeaJQ('<img />', {
                    	      'src' : '$web_pub_path/images/icons/16x16/undo.png',
                    	      'alt' : 'Undo Button for ' + label
                    	     })
                    ));

            aeaJQ('#' + formElementPrefix + dataElementId + '_undoButton').click(function () {

                aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResultsDiv').empty();
                aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchResults-container').hide();
                aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').hide();

                scope.addSearchIcon.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
                aeaJQ(this).hide();
                
                return false;
            });
        }
    },
    createSearchField : function (initialLoad, formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        var scope = this;
        var ddMenu = aeaJQ('select#' + formElementPrefix + dataElementId);
        var label = aeaJQ.trim(aeaJQ('#' + formElementPrefix + dataElementId + '-container .formLabel').text());
        var ddMenuSelected = aeaJQ('select#' + formElementPrefix + dataElementId + ' :selected');

        //Hide the default DD menu. This should always remain hidden.
        ddMenu.hide();

        if (aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').size() == 0) {
            ddMenu.after(aeaJQ('<input>', {
                    'id' : formElementPrefix + dataElementId + '_liveSearchInput',
                    'class' : 'formInput',
                    'type' : 'text',
                    'value' : '',
                    'title' : label + ' text input search field.',
                    'style' : 'width: 200px; box-shadow: 0 0 5px #FED24C; border: 1px solid #FED24C; margin: 5px 5px 5px 0px;',
                    'name' : formElementPrefix + dataElementId + '_liveSearchInput'
            }));
            
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').attr('autocomplete', 'off');

			aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').on('keydown', function(e) {
    			if (e.keyCode == 13) {
       				 return false;
    			}
			});
			
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').on('keyup paste input' , this.delaySearch(500, function (event) {
                scope.performLiveSearch.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl)
            }));

        }

        if (initialLoad) {
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').hide();
            aeaJQ('#' + formElementPrefix + dataElementId + '_span').remove();
            
            #if ($isAccessibilityEnhanced)
            ddMenu.after(aeaJQ('<span>', {
                        'id' : formElementPrefix + dataElementId + '_span'
                    }).append(aeaJQ('<input>', {
                       'id' : formElementPrefix + dataElementId + '_readOnlyInput',
                       'type' : 'text',
                       'value' : ddMenuSelected.text(),
                       'readonly' : '',
                       'title' : 'Live Search field ' + label + '. Current value for field ' + label + ' is ' + ((ddMenuSelected.text() == '') ? 'blank.' : ddMenuSelected.text())
                    }))
                );
            #else
                ddMenu.after(aeaJQ('<span>', {
                        'id' : formElementPrefix + dataElementId + '_span'
                }).text(ddMenuSelected.text()));
            #end
            
            scope.addSearchIcon.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
            scope.addDeleteIcon.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
        } else {
            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').show().focus();
            aeaJQ('#' + formElementPrefix + dataElementId + '_span').remove();
        }
    },
    createSearchFieldMS : function (initialLoad, formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        var scope = this;
        var msMenuNoFS = aeaJQ('div#' + formElementPrefix + dataElementId + '_multiValue');
        var msLabel = aeaJQ.trim(aeaJQ('#' + formElementPrefix + dataElementId + '-container .formLabel').text());
        msMenuNoFS.not(':has(fieldset)').each(function() {
        	  aeaJQ(this).wrapInner("<fieldset style='border:0px'>"); 
        	  aeaJQ("fieldset", this).each(function() { 
        		  aeaJQ(this).prepend(
        		  		aeaJQ('<legend>', { 
        		      		#if(!$isAccessibilityEnhanced)
        		      		'style' : 'display: none;',
        		     		#end
        		      		'id' : formElementPrefix + dataElementId + '_legend'
        				}).text(aeaJQ.trim(aeaJQ(this).closest("tr[id*='-container']").find("label.formLabel").text()))
        		 );
           	  });
        });
        
        
        msMenuNoFS.parent().each(function(index, element) {
  			if (aeaJQ(element).is('td')) {
    			aeaJQ(element).css('padding', '2px 0px');
  			}
		});
		
        aeaJQ(aeaJQ('#' + formElementPrefix + dataElementId + '-container td')[1]).css('padding', '2px 0px');
        
        var msMenu = aeaJQ('div#' + formElementPrefix + dataElementId + '_multiValue fieldset');

        var i = 0;
        msMenu.find('input').each(function (index, element) {
            var jqElement = aeaJQ(element);
            
            // Prevent the browser from toggling the input when the label is clicked.
            jqElement.parent().click(function(event){
                event.preventDefault();
            });
            
			//If a value is not checked, remove it from the MS list entirely (prev is for the label).
            if (!jqElement.is(":checked")) {
                if (jqElement.parent().prev().is('br')) {
                jqElement.parent().prev().remove();
                }
                jqElement.parent().remove();
            } else {
            
                 //Create a new delete button with a delete image and append the image to the button.
                var newDeleteButton =
                        aeaJQ('<a />', {
                        	  'id' : element.id + '_deleteButton',
                        	  'class' : 'deleteLiveSearchMSButton',
                        	  'href' : 'javascript:void(0);',
                        	  #if ($isAccessibilityEnhanced)
                                 'style' : 'display: none;'
                               #else
                                 'style' : 'padding:0px 3px 0px 6px;'
                               #end
                });
            
                //If the value is checked, hide the checkbox itself so only the label is left.
                //For accessability mode, leave the checkbox enabled.
                if ($isAccessibilityEnhanced) {
                	jqElement.keypress(function (e) {
						 var key = e.which;
						 if(key == 13) {
						    newDeleteButton.click();
						    return false;
						 }
                        });
                        
                    jqElement.click(function() {
                         newDeleteButton.click();
                    });
   				} else {
   				   jqElement.hide();
   				}
   				
                var name = aeaJQ.trim(aeaJQ(element).parent().text());

                var newDeleteButtonImage =
                   		aeaJQ('<img />', {
                        	  'src' : '$web_pub_path/images/icons/16x16/delete.png',
                        	  'alt' : 'Delete value ' + name + ' from ' + msLabel + ' list.'
                        });
                   
                newDeleteButton.append(newDeleteButtonImage);
                        
                //Append a delete button after the label.
                jqElement.parent().after(newDeleteButton);
                
                //When the delete button is clicked, remove the value from the MS list, and also make sure
                //that the value in a result list (if it is open) returns to white.
                newDeleteButton.click(function () {
                    var currentRowId = aeaJQ(this).prev().find('input').val();

                    aeaJQ(this).prev().remove();
                    aeaJQ(this).next().remove();
                    
                    var parentObject = aeaJQ(this).parent();
                    aeaJQ(this).remove()
                    parentObject.trigger('change');

                    aeaJQ('#' + formElementPrefix +
                        dataElementId +
                        '_liveSearchResultMainTable tbody tr[id="TR_' +
                        dataElementId +
                        '_' +
                        currentRowId +
                        '"]').css("background", "white");
                    
                    return false;
                });
            }
        });

		//Removes the first BR tag in the MS list
		var extraBR = aeaJQ('div#' + formElementPrefix + dataElementId + '_multiValue legend').next();
        if (extraBR.is('br')) {
            extraBR.remove();
        }

	    msMenuNoFS.css("height", "auto");
        msMenuNoFS.css("width", "110%");

		//If no search button exists, append a search button after the MS menu.
        if (aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').size() == 0) {
        
        	var searchButton = aeaJQ('<a />', {
        	                         'id' : formElementPrefix + dataElementId + '_searchButton',
        	                         'class' : 'search',
        	                         'href' : 'javascript:void(0);',
        	                         'style' : 'padding:3px 4px; float:right;'
        						});
        	
        	var searchButtonImage = aeaJQ('<img />', {
        								'src' : '$web_pub_path/images/icons/16x16/standard_search.png',
        								'alt' : 'Open ' + msLabel + ' Live Search.'
        						});
        						
        	searchButton.append(searchButtonImage);
        	
        	//Append the search button after the MS menu.
            msMenu.after(searchButton);
            
            //Create a hidden input box that displays when the search button is clicked 
            //(NOTE: this is created display none)
            var liveSearchInput = aeaJQ('<input />', {
                     'id' : formElementPrefix + dataElementId + '_liveSearchInput',
                     'class' : 'formInput',
                     'type' : 'text',
                     'value' : '',
                     'name' : formElementPrefix + dataElementId + '_liveSearchInput',
                     'title' : msLabel + ' text input search field.',
                     'style' : 'width: 200px; box-shadow: 0 0 5px #FED24C; border: 1px solid #FED24C; margin: 5px 5px 5px 0px; display:none;'
                   });

            liveSearchInput.attr('autocomplete', 'off');
            
            msMenu.after(liveSearchInput);
            
            liveSearchInput.on('keydown', function(e) {
    			if (e.keyCode == 13) {
       				 return false;
    			}
			});

			//When characters are put into the live search box, perform a search 500ms after the last action on the box.
            liveSearchInput.on('keyup paste input', this.delaySearch(500, function (event) {
                scope.performLiveSearchMS.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
            }));

			//When the search icon is clicked, show the input box, and also attempt a search if there is already
			//a value in the box. Hide the search icon when clicked.
            aeaJQ('#' + formElementPrefix + dataElementId + '_searchButton').click(function () {
                aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').show();
                aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').focus();
                scope.addUndoButtonMS.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
                scope.performLiveSearchMS.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
                aeaJQ(this).hide();
                
                return false;
            });
        }
    },
    getElementValue : function (formElementPrefix, dataElementId) {
        var elementType = '';
        var formElements = document.getElementsByName(formElementPrefix + dataElementId);

        if (formElements[0]) {
            elementType = formElements[0].type;
        }

        var valueArray = [];

        if (('checkbox' == elementType) || ('radio' == elementType)) {
            var j = 0;
            for (var i = 0; i < formElements.length; i++) {
                if (formElements[i].checked) {
                    valueArray[j] = formElements[i].value;
                    j++;
                }
            }
        } else if (formElements[0]) {
            valueArray[0] = formElements[0].value;
        }

        return valueArray;
    },
    initLookupVariables : function (formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        //This function determines what parameters a lookup query needs. These parameters are later
        //read from the form and included in the subsequent AJAX call that performs the search.
        var elementName = dataElementId;
		if (isUnboundFormControl) {
			elementName = formElementPrefix + dataElementId;
		}
		
        var scope = this;
        var date = new Date();
        var dateTS = date.getTime();
        new Ajax.Request('page.request.do', {
            method:'get',
            parameters : {
                page : 'ls.form.lsScript.ajax',
                operation : 'getParamsNeededForAJAX',
                dataObjectKey : scope.dataObjectKey,
                dataFormId : scope.dataFormId,
                dataElementId : elementName,
                dateTS : dateTS
            },
            onCreate : function () {
                show('loading');
            },
            onFailure : function () {
                hide('loading');
            },
            onComplete : function (transport) {
                hide('loading');

                var result = transport.responseText.evalJSON().jsonResult;
                var paramsNeeded = [];
                if ('true' == result.isValid) {
                	var resultList = result.parameters;
                
	                for (var i = 0; i < resultList.length; i++) {
	                    paramsNeeded[i] = resultList[i].replacementParam;
						
						//Don't add a change listener for the element itself, otherwise infinate loop of making change
						//causes refresh for itself.
						if (paramsNeeded[i] != elementName) {
							//Add a listener to look for changes to the elements that make up the dd menu's 
							//query. If the value of one of those elements changes, the value will be cleared
							//and the search will open automatically.
		                    aeaJQ('.formBody [name=' + formElementPrefix + paramsNeeded[i] + ']').change(function () {
		                        if (aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').is(":hidden")) {
		
		                            var ddMenu = aeaJQ('select#' + formElementPrefix + dataElementId);
		                            var ddMenuValue = aeaJQ('select#' + formElementPrefix + dataElementId + ' :selected').text();
		                            ddMenu.empty();
		                            ddMenu.append(aeaJQ('<option>').attr('value', '').attr('selected', 'selected'));
		                            aeaJQ('#' + formElementPrefix + dataElementId + '_searchButton').click();
		                            aeaJQ('#' + formElementPrefix + dataElementId + '_undoButton').show();
		                            aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').val(ddMenuValue);
		                            scope.performLiveSearch.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
		                        } else {
		                            scope.performLiveSearch.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
		                        }
		                    });
		                }
	                }
	

	            } else {
	                if ('$!debugMode' == 'true') {
	            	   alert(result.errorMessage);
	            	}
	            }
	            
	            if (!scope.replacementParamsNeeded) {
	                 scope.replacementParamsNeeded = [];
	            }
	
	            scope.replacementParamsNeeded[formElementPrefix + dataElementId] = paramsNeeded;
            }
        });
    },
    initLookupVariablesMS : function (formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        //This function determines what parameters a lookup query needs. These parameters are later
        //read from the form and included in the subsequent AJAX call that performs the search.
		var elementName = dataElementId;
		if (isUnboundFormControl) {
			elementName = formElementPrefix + dataElementId;
		}
		
        var scope = this;
        var date = new Date();
        var dateTS = date.getTime();
        new Ajax.Request('page.request.do', {
            method:'get',
            parameters : {
                page : 'ls.form.lsScript.ajax',
                operation : 'getParamsNeededForAJAX',
                dataObjectKey : scope.dataObjectKey,
                dataFormId : scope.dataFormId,
                dataElementId : elementName,
                dateTS : dateTS
            },
            onCreate : function () {
                show('loading');
            },
            onFailure : function () {
                hide('loading');
            },
            onComplete : function (transport) {
                hide('loading');

                var result = transport.responseText.evalJSON().jsonResult;
                var paramsNeeded = [];
                
                if ('true' == result.isValid) {
                	var resultList = result.parameters;
                
	                
	                for (var i = 0; i < resultList.length; i++) {
	                    paramsNeeded[i] = resultList[i].replacementParam;
	                    
					    //Add a listener to look for changes to the elements that make up the dd menu's 
						//query. If the value of one of those elements changes, the value will be cleared
						//and the search will open automatically.
	                    aeaJQ('.formBody [name=' + formElementPrefix + paramsNeeded[i] + ']').change(function () {
	                        if (aeaJQ('#' + formElementPrefix + dataElementId + '_liveSearchInput').is(":hidden")) {
	
	                            aeaJQ('div#' + formElementPrefix + dataElementId + '_multiValue fieldset')
	                            .empty()
								.prepend(
				        		  		aeaJQ('<legend>', { 
				        		      		#if(!$isAccessibilityEnhanced)
				        		      		'style' : 'display: none;',
				        		     		#end
				        		      		'id' : formElementPrefix + dataElementId + '_legend'
				        				}).text(aeaJQ.trim(aeaJQ('div#' + formElementPrefix + dataElementId + '_multiValue').closest("tr[id*='-container']").find("label.formLabel").text()))
				        		 );
	                            
	                            
	                            
	                            aeaJQ('#' + formElementPrefix + dataElementId + '_searchButton').click();
	                            scope.performLiveSearchMS.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
	                        } else {
	                            scope.performLiveSearchMS.bind(scope)(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
	                        }
	                    });
	                }
	

	            } else {
	               if ('$!debugMode' == 'true') {
	                  alert(result.errorMessage);
	               }
	            }
	            
	            if (!scope.replacementParamsNeeded) {
	                  scope.replacementParamsNeeded = [];
	            } 
	            
	            scope.replacementParamsNeeded[formElementPrefix + dataElementId] = paramsNeeded;
            }
        });
    },
    delaySearch : function(delay, callback) {
       //This function prevents multiple AJAX calls in quick succession. A delay (500ms is used for LS)
       //must occur before the callback function is executed.
    
	    var timerId;

	    function wrappedCallbackFunction() {
	      var scope = this;
	      var args = arguments;
	      
	      function executeFunction() {
	        callback.apply(scope, args);
	      };
	      
	      if (!timerId) {
	         executeFunction();
	      } else {
	         clearTimeout(timerId);
	      }
	      
	      timerId = setTimeout(executeFunction, delay);
	    };
	    
	    return wrappedCallbackFunction;
    },
    makeLiveSearch : function (formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        this.createSearchField(true, formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
        this.initLookupVariables(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
    },
    makeLiveSearchMS : function (formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl) {
        if (aeaJQ('#' + formElementPrefix + dataElementId + '_multiValue ul.multiValueReadOnlyUl').size() == 0) {
            this.createSearchFieldMS(true, formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
            this.initLookupVariablesMS(formElementPrefix, dataElementId, columnHeaders, isUnboundFormControl);
        }
    },
    setLiveSearchSingleValue: function (formElementPrefix, dataElementId, display, value) {
          var mainDDMenu = aeaJQ('select#' + formElementPrefix + dataElementId);

          if (mainDDMenu.size() > 0) {
              mainDDMenu.empty();
              mainDDMenu.append(aeaJQ('<option>').attr('value', value).attr('selected', 'selected').text(display));
              mainDDMenu.trigger('change');
          }

          aeaJQ('#' + formElementPrefix + dataElementId + '_span').remove();
          
          
          #if ($isAccessibilityEnhanced)
        	  var label = aeaJQ.trim(aeaJQ('#' + formElementPrefix + dataElementId + '-container .formLabel').text());
        	  
           	  mainDDMenu.after(aeaJQ('<span>', {
                  'id' : formElementPrefix + dataElementId + '_span'
              }).append(aeaJQ('<input>', {
                 'id' : formElementPrefix + dataElementId + '_readOnlyInput',
                 'type' : 'text',
                 'value' : display,
                 'readonly' : '',
                 'title' : 'Live Search field ' + label + '. Current value for field ' + label + ' is ' + ((display == '') ? 'blank.' : display) 
              })));
          #else
              mainDDMenu.after(aeaJQ('<span>', {
                  'id' : formElementPrefix + dataElementId + '_span'
              }).text(display));
          #end
    },
    setLiveSearchMsValue : function (formElementPrefix, dataElementId, valueDisplayMapList) {

   	    var msLabel = aeaJQ.trim(aeaJQ('#' + formElementPrefix + dataElementId + '-container .formLabel').text());
   	    var msMenu = aeaJQ('div#' + formElementPrefix + dataElementId + '_multiValue fieldset');
   	    
   	    //Remove existing values.
   	    msMenu.find('a').remove();
   	    msMenu.find('label').remove();
   	    msMenu.find('br').remove();
   	    
   	    if (valueDisplayMapList && (valueDisplayMapList.length > 0)) {
   	
    	    for (var i = 0; i < valueDisplayMapList.length; i++) {
    	    
    	           var value = valueDisplayMapList[i]["value"];
    	           var display = valueDisplayMapList[i]["display"];
    	           
    	 			var newDeleteButton =
                        aeaJQ('<a />', {
                        	  'class' : 'deleteLiveSearchMSButton',
                        	  'href' : 'javascript:void(0);',
                        	   #if ($isAccessibilityEnhanced)
                                 'style' : 'display: none;'
                               #else
                                 'style' : 'padding:0px 3px 0px 6px;'
                               #end
                    });

                    var newInput = aeaJQ('<input />', {
                           'id' : formElementPrefix + dataElementId + '_' + value,
                           'type' : 'checkbox',
                           'checked' : '',
                           'value' : value,
                           #if (!$isAccessibilityEnhanced)
                           'style' : 'display: none;',
                           #end
                           'name' : formElementPrefix + dataElementId
                    });
                    
                    #if ($isAccessibilityEnhanced)
	                    newInput.keypress(function (e) {
							 var key = e.which;
							 if(key == 13) {
							    newDeleteButton.click();
							    return false;
							 }
                   	    });
                    
	                    newInput.click(function() {
	                       newDeleteButton.click();
	                    });
                    #end
                    
                    var newLabel = aeaJQ('<label />', {
                        'for' : formElementPrefix + dataElementId + '_' + value
                    });
                    
                    newLabel.text(' ' + display);
                    newLabel.prepend(newInput);
                    msMenu.append(newLabel);

                   var newDeleteButtonImage =
                   		aeaJQ('<img />', {
                        	  'src' : '$web_pub_path/images/icons/16x16/delete.png',
                        	  'alt' : 'Delete value ' + display + ' from ' + msLabel + ' list.'
                        });
                        
                   newDeleteButton.append(newDeleteButtonImage);

                    msMenu.append(newDeleteButton);
                    msMenu.append(aeaJQ('<br>'));

                    newDeleteButton.click(function () {
                        var currentRowId = aeaJQ(this).prev().find('input').val();

                        aeaJQ(this).prev().remove();
                        aeaJQ(this).next().remove();
                        
                        var parentObject = aeaJQ(this).parent();
                    	aeaJQ(this).remove();
                    	parentObject.trigger('change');

                        aeaJQ('#' +
                            formElementPrefix +
                            dataElementId +
                            '_liveSearchResultMainTable tbody tr[id="TR_' +
                            dataElementId +
                            '_' +
                            currentRowId +
                            '"]').css("background", "white");
                        
                        return false;
                    });
             } //end for
         }//end if
    }
};
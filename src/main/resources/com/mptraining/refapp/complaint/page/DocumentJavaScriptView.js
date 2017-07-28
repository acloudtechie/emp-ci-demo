/**
 * 
 * Main JavaScript to run on the Document form
 * 
 * administrator 09/29/2016
 */
(function() {

	JavascriptUtilitiesLibrary.addMultiloadEvent(refreshNameLookupOnTypeChange);
	JavascriptUtilitiesLibrary.addMultiloadEvent(initializeEditor);

	function refreshNameLookupOnTypeChange() {
		//If on Document
		if (jQuery('#Document_documentType').size() > 0) {
			jQuery('[name=Document_documentType]').change(function() {
				refreshTrackingForm();
			});
		} 
		//If on CMP Document
		else if (jQuery('#CmpDocument_documentType').size() > 0) {
			jQuery('[name=CmpDocument_documentType]').change(function() {
				RefreshLookup.refreshLookup("CmpDocument_documentName");
			});
		}
	}

	function initializeEditor() {
		//If on Document
		if (jQuery('#Document_description').size() > 0) {
			CKEDITOR.replace('Document_description', {
				enterMode : CKEDITOR.ENTER_BR,
				removePlugins : 'etk,forms,iframe,flash,smiley,preview'
			});
		} 
		//If on CMP Document
		else if (jQuery('#CmpDocument_description').size() > 0) {
			CKEDITOR.replace('CmpDocument_description', {
				enterMode : CKEDITOR.ENTER_BR,
				removePlugins : 'etk,forms,iframe,flash,smiley,preview'
			});
		}
	}

}())
AeaFormUtilitiesJavascriptLibrary.addMultiloadEvent(function() {
	jQuery('#AeaRdoDataExportConfig_databaseTableName').change(function() {
		refreshTrackingForm();
	});
});
/**
 *
 * Main JavaScript to run on the Complaint form
 *
 * administrator 09/29/2016
 **/
(function(){

	JavascriptUtilitiesLibrary.addMultiloadEvent(hideLocationWhenConfidential);
	JavascriptUtilitiesLibrary.addMultiloadEvent(refreshTypeLookupOnCategoryChange);
	JavascriptUtilitiesLibrary.addMultiloadEvent(refreshSacAssignedCount);
	JavascriptUtilitiesLibrary.addMultiloadEvent(makeSACrequired);
	
	if('$!role' == 'role.efiler'){
		JavascriptUtilitiesLibrary.addMultiloadEvent(setEfilerValues);
	}
	
	function refreshSacAssignedCount(){
		jQuery('[id$=Complaint_sacAssigned]').change(updateAssignedCount);
		updateAssignedCount();
	}
	
	function updateAssignedCount(){
		if(jQuery('[name$=Complaint_sacAssigned]').val() !== ''){
			jQuery.ajax({
		        data: {sacId : jQuery('[name$=Complaint_sacAssigned]').val(), isCmp : jQuery('[name=Complaint_sacAssigned]').length === 0 },
		        type: "POST", 
		        url: "page.request.do?page=page.formComplaintSacAssignedCountAjax",
		        cache: false
			}).done(function( data ) {
				if(jQuery.isNumeric(data)){
					jQuery('#sacCount').remove();
					jQuery('[name$=Complaint_sacAssigned]').parent().append('<span id="sacCount"> - Workload: ' + data + ' Open Complaint(s)</span>');
				}
		    });
		}
		else{
			jQuery('#sacCount').remove();
		}
	}
	
	function setEfilerValues(){
		if(jQuery('[id$=Complaint_category] option:selected').text() != 'Allegation'){
			jQuery('[id$=Complaint_category] option').each(function() {
				if(jQuery(this).text() == 'Allegation') {
					jQuery(this).attr('selected', 'selected');            
				}                        
			});
			jQuery('[id$=Complaint_category]').change();
		}
		
		jQuery('[id$=Complaint_source] option').each(function() {
			if(jQuery(this).text() == 'efiler') {
				jQuery(this).attr('selected', 'selected');            
			}                        
		});
		
	}
	
	function refreshTypeLookupOnCategoryChange() {
	   jQuery('[id$=Complaint_category]').change(function() {
	      refreshTrackingForm();
	   });
	}
	
	function makeSACrequired(){
		if(jQuery("[id$=Complaint_category] option:selected").text() == 'Case'){
			jQuery('[name$=Complaint_sacAssigned]').after(jQuery('<img id="sacRequired" title="Required" alt="Required" src="themes/default/web-pub/images/icons/required.gif" style="padding-left: 4px;">'));
		}
		else{
			jQuery('#sacRequired').remove();
		}
	}
	
	function hideLocationWhenConfidential(){
		jQuery('[name$=Complaint_confidential]').change(function() {
			if(this.value == 1){
				hideLocation();
			}
			else{
				showLocation();
			}
		});
		if(jQuery('[id$=Complaint_confidential_yes]:checked').length > 0){
			hideLocation();
		}
		else{
			showLocation();
		}
	}
	
	function hideLocation(){
		jQuery('[id^="location"]').hide();
		jQuery('[id$=Complaint_address-container]').hide();
		jQuery('[id$=Complaint_city-container]').hide();
		jQuery('[id$=Complaint_addressState-container]').hide();
		jQuery('[id$=Complaint_zipCode-container]').hide();
	}
	
	function showLocation(){
		jQuery('[id^="location"]').show();
		jQuery('[id$=Complaint_address-container]').show();
		jQuery('[id$=Complaint_city-container]').show();
		jQuery('[id$=Complaint_addressState-container]').show();
		jQuery('[id$=Complaint_zipCode-container]').show();
	}

}())
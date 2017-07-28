/**
 *
 * Autogenerate the Complaint ID
 *
 * administrator 10/05/2016
 **/

package com.mptraining.refapp.cmpcomplaint.workfloweffects;

import net.entellitrak.aea.exception.RulesFrameworkException;
import net.entellitrak.aea.rf.IRulesFrameworkParameters;
import net.entellitrak.aea.rf.IScript;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.entellitrak.DataObjectEventContext;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.InputValidationException;
import com.entellitrak.dynamic.CmpComplaint;
import com.entellitrak.dynamic.SystemInformation;
import com.mptraining.refapp.common.dao.SequenceDao;
import com.mptraining.refapp.rdo.dao.RdoDao;

public class GenerateComplaintId implements IScript {

	@Override
	public void doEffect(ExecutionContext etk, IRulesFrameworkParameters parameters) throws RulesFrameworkException {
		DataObjectEventContext doec = (DataObjectEventContext) etk;
		CmpComplaint complaint = etk.getDynamicObjectService().get(CmpComplaint.class, doec.getNewObject().properties().getId());
 		String generatedComplaintId = null;
 		SystemInformation yearInfo = null;
 		Integer currentYear = new DateTime().getYear();

 		//Get year info from System Information RDO
		try {
			yearInfo = etk.getDynamicObjectService().get(SystemInformation.class, RdoDao.getIdFromCode(etk, "T_SYSTEM_INFORMATION", "complaintIdSeqYear").longValue());
		} catch (IncorrectResultSizeDataAccessException e) {
			//If there is no existing record, create one
			yearInfo = etk.getDynamicObjectService().createNew(SystemInformation.class);
			yearInfo.setName("Complaint ID Sequence Year");
			yearInfo.setCode("complaintIdSeqYear");
			yearInfo.setValue(currentYear.toString());
			try {
				etk.getDynamicObjectService().createSaveOperation(yearInfo).save();
			} catch (InputValidationException e1) {
				e1.printStackTrace();
				throw new RulesFrameworkException(e1);
			}
		}
 		try {
			//If the year from System Information differs from the current year, regenerate the sequence so it starts at 0 again
			if(!yearInfo.getValue().equals(currentYear.toString())){
				SequenceDao.dropComplaintIdSeq(etk);
				SequenceDao.createComplaintIdSeq(etk);
				yearInfo.setValue(currentYear.toString());
				etk.getDynamicObjectService().createSaveOperation(yearInfo).save();
			}

			Integer seqNum = -1;
			try{
				seqNum = SequenceDao.selectFromComplaintIdSeq(etk);
			}catch(Exception e){
				SequenceDao.createComplaintIdSeq(etk);
				seqNum = SequenceDao.selectFromComplaintIdSeq(etk);
			}
			generatedComplaintId = currentYear + "-" + StringUtils.leftPad(seqNum.toString(), 4, '0');

			complaint.setComplaintId(generatedComplaintId);
			etk.getDynamicObjectService().createSaveOperation(complaint).setExecuteEvents(false).save();

		} catch (InputValidationException e) {
			e.printStackTrace();
			throw new RulesFrameworkException(e);
		}
	}

}

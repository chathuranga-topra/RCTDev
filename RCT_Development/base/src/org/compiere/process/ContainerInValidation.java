package org.compiere.process;

import java.util.Properties;

import org.compiere.apps.ADialog;
import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MOrder;

public class ContainerInValidation extends CalloutEngine{
	
	TpUtility tpUtility = new TpUtility();
	
	//org.compiere.process.ContainerInValidation.validate
	public String validate(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		/*if(mTab.getRecord_ID() <= 0)
			return "";*/
		
		if(value == null)
			return "";
		
		//validate for only custom clearence doctype
		if(!mTab.getValue("c_doctypetarget_id").toString().equals(tpUtility.getCustomerClearenceDoc_type()+"")){
			return "";
		}
		
		String conno = (mTab.getValue("description")+"").trim();
		String sqlwhere = " docstatus IN ('CO' , 'DR' , 'IP', 'IN') "
				   + " AND isactive = 'Y' "
				   + " AND  description like '%"+conno+"%'";
		
		int [] orders = MOrder.getAllIDs(MOrder.Table_Name, sqlwhere, mTab.getTrxInfo());
		//validate for same container no in operation 
		if(orders.length > 0){
			mTab.setValue("description", "");
			mTab.setValue("poreference", "");
			ADialog.error(WindowNo, null, "Duplicate Container Number" , "The Entered container number is now in the Operation!");
		}else{
			//validate for container number in correct format
			if(!this.isCorrectFormat(conno)){
				mTab.setValue("description", "");
				ADialog.error(WindowNo, null,"Invalid Container Number" , "Please insert correct formated container number!");
			}
		}
		return "";
	}
	
	private boolean isCorrectFormat(String conno){
		
		//length
		if(conno.length() != 11)
			return false;
		//Value format LLLL000000
		return conno.matches("^[A-Z]{4}\\d{7}");
	}
}

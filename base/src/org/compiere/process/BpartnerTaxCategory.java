package org.compiere.process;

import java.util.Properties;

import org.compiere.apps.ADialog;
import org.compiere.apps.ADialogDialog;
import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MTax;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class BpartnerTaxCategory extends CalloutEngine {

	// org.compiere.process.BpartnerTaxCategory.changeTax
	public String changeTax(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		if (value == null || mTab.getRecord_ID() == -1)
			return "";
		
		//Tax Number Validation
		String SQL = "SELECT NAICS from C_BPARTNER WHERE C_BPARTNER_ID = ?";
		String NAICS = DB.getSQLValueString(mTab.getTrxInfo(), SQL, (Integer) value);
		
		
		if(NAICS == null || NAICS.length() < 1){
			
			ADialog.warn(WindowNo, null, "Validation failed!!", "Please fill consingnee vat number and reassign.");
			/*MOrder mOrder = new MOrder(ctx, mTab.getRecord_ID(), null);
			mOrder.setC_BPartner_ID(1000891);//set default business partner
			mOrder.saveEx();*/
			
			mTab.setValue("C_Bpartner_ID", 1000891);//set default business partner
			mTab.dataSave(false);
			mTab.dataRefresh();
			
			return "";
		}
		
		// change tax category based on the bpartner
		SQL = "SELECT IsTaxExempt from C_BPARTNER WHERE C_BPARTNER_ID = ?";
		String IsTaxExempt = DB.getSQLValueString(mTab.getTrxInfo(), SQL, (Integer) value);
		
		if(IsTaxExempt.equalsIgnoreCase("Y")){
			
			//Order Id
			MOrder mOrder = new MOrder(ctx, mTab.getRecord_ID(), null);
			for(MOrderLine line  : mOrder.getLines()){
				line.setC_Tax_ID(this.getTaxGategory(mTab.getTrxInfo()));
				line.save();
			}
		}

		return "";
	}
	
	public int getTaxGategory(String trxInfor){
		
		String sqlwhere = "IsTaxExempt = 'Y' AND AD_Org_ID = " + Env.getContextAsInt(Env.getCtx(), "#AD_Org_ID");;
		int [] i = MTax.getAllIDs(MTax.Table_Name, sqlwhere, trxInfor);
		
		if(i.length > 0)
			return i[0];
		else
			return 0;
	}
}

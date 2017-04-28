package org.compiere.process;

import java.util.Properties;

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
		if (value == null)
			return "";
		// change tax category based on the bpartner
		String SQL = "SELECT IsTaxExempt from C_BPARTNER WHERE C_BPARTNER_ID = ?";
		String IsTaxExempt = DB.getSQLValueString(mTab.getTrxInfo(), SQL, (Integer) value);
		if(IsTaxExempt.equalsIgnoreCase("Y")){
			
			/*String sqlwhere = "IsTaxExempt = 'Y' AND AD_Org_ID = " + Env.getContextAsInt(Env.getCtx(), "#AD_Org_ID");;
			int [] i = MTax.getAllIDs(MTax.Table_Name, sqlwhere, mTab.getTrxInfo());*/
			
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

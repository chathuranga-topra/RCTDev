package org.compiere.process;

import java.util.Properties;

import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPartner;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MTax;
import org.compiere.util.Env;
import org.compiere.util.Trx;

//CHANGES OF RAX RATES BASED ON THE CONSIGNEE WHEN CHANGING THE CONSIGNEE
//org.compiere.process.TaxChange.setRate
public class TaxChange extends CalloutEngine{

	public String setRate(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value){
		
		if(value == null) return "";
		
		//VALIDATE  ONLY FOR CUSTOMER RELEASE ORDER
		if(mTab.getValue("c_doctypetarget_id").toString().equalsIgnoreCase("1000058")){
			
			Trx trx = Trx.get(Trx.createTrxName("POSave"), true);
			MBPartner mbPartner = MBPartner.get(ctx,Integer.parseInt(mTab.getValue("c_bpartner_id").toString()));
			
			if(mbPartner.isTaxExempt()){
				
				int c_tax_id = getSvatId(ctx , trx.getTrxName());
				MOrder mOrder = new MOrder(ctx, mTab.getRecord_ID(), trx.getTrxName());
				MOrderLine [] mliLines = mOrder.getLines();
				for(MOrderLine line : mliLines){
					line.setC_Tax_ID(c_tax_id);
					line.save();
				}
			}
						
			trx.commit();
			trx.close();
		}
		
		return "";
	}
	
	public int getSvatId(Properties ctx , String trxName){
		
		String sql = "isTaxExempt = 'Y' AND AD_CLIENT_ID = " + Env.getContextAsInt(ctx, "#AD_Client_ID");
		return MTax.getAllIDs(MTax.Table_Name, sql, trxName)[0]; 
	}
}

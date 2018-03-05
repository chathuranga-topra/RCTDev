package org.compiere.process;

import java.util.Properties;

import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.util.Trx;

public class ConSize extends CalloutEngine{
	//org.compiere.process.ConSize.chanegeSize
	public void chanegeSize(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value){
		
		if(value == null || (Integer)mField.getOldValue() == null || (Integer)mField.getOldValue() == value) return;
		//Change price in sales order
		if(mTab.getTableName().equalsIgnoreCase(MOrder.Table_Name)){
			
			Trx trx = Trx.get(Trx.createTrxName("POSave"), true);
			MOrder mOrder = new  MOrder(ctx, mTab.getRecord_ID(), trx.getTrxName());
			MOrderLine[] lines = mOrder.getLines();
			
			for(MOrderLine line : lines){
				
				line.setPrice((Integer)value);
				line.saveEx();
				//line
			}
			
			trx.commit();
			trx.close();
			
			mTab.dataSave(false);
			mTab.dataRefresh();
			
		}
	}
}

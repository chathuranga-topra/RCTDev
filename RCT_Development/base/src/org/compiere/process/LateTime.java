package org.compiere.process;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;

//org.compiere.process.LateTime.calculateLateHours
public class LateTime extends CalloutEngine{

	private int parkingProductId = 1000842;
	
	public String calculateLateHours(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{	
		if(mTab.getValue("C_INVOICE_ID") == null) return "";	
		
		try{
			
			MInvoice mInvoice = MInvoice.get(ctx , Integer.parseInt(mTab.getValue("C_INVOICE_ID").toString()));
			MInvoiceLine[] mInvoiceLines = mInvoice.getLines();
			
			for(MInvoiceLine line:mInvoiceLines){
			
				if(parkingProductId == line.getM_Product_ID()){
					//set service end time
					Timestamp ct = new Timestamp(System.currentTimeMillis());
					line.set_ValueOfColumn("end_time", ct);
					//set quantity and
					Timestamp start_time = (Timestamp) line.get_Value("start_time");
					BigDecimal bg = new BigDecimal(new ServiceStartEnd().getUmoDasedTimeDiff(line.getC_UOM_ID(), start_time, ct));
					bg = bg.setScale(0, RoundingMode.CEILING);
					
					line.set_ValueOfColumn("qtyinvoiced" , bg);
			    	line.set_ValueOfColumn("qtyentered" , bg);
					line.save();
				}
			}
			
			mInvoice.set_ValueOfColumn("docstatus", "CO");
			mInvoice.set_ValueOfColumn("docaction", "CL");
			mInvoice.completeIt();
			mInvoice.save();
			mTab.dataRefresh();
			
		}catch(Exception ex){
			ex.printStackTrace();
		}	
		return "";
	}
}

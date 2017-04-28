package org.compiere.process;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;
import org.compiere.apps.ADialog;
import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.util.Trx;

public class DMountingCharge extends CalloutEngine{
	
	TpUtility tpUtility = new TpUtility();
	ServiceStartEnd sse = new ServiceStartEnd();
	
	//org.compiere.process.DMountingCharge.addStorageCharge
	//When mounting and demounting with ladern storage charge is added it should be close the parking
	public String addStorageCharge(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if(value==null) 
			return "";
		
		//validate only for Mounting and DMounting product
		// (M_Product - M_Product_ID=1000880)
		int i = (Integer) value;
		if(!(i == tpUtility.getMountDemount()))
			return "";
		
		Trx trx = null;
		try{
			
			String tblName = mTab.getTableName();
			trx = Trx.get(Trx.createTrxName("POSave"), true);
			//product Laden Storage Charges
			//M_Product_ID=1000841)
			MProduct product = MProduct.get(ctx,tpUtility.getLadernStorage());
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			
			//order
			if(tblName.endsWith("C_OrderLine")){
				
				int C_Order_ID = Integer.parseInt(mTab.get_ValueAsString("C_Order_ID"));
				MOrder morder = new MOrder(ctx, C_Order_ID,trx.getTrxName());
				MOrderLine mOrderLine = new MOrderLine(morder);
				
				mOrderLine.setProduct(product);
				mOrderLine.setQty(new BigDecimal(1));
				mOrderLine.set_ValueOfColumn("n_starttime", ts);
				mOrderLine.setTax();
				mOrderLine.save();
				
				//close parking charge beforerelease
				String whereClause = "AND C_ORDER_ID = "+C_Order_ID+" AND m_product_id = " + tpUtility.getParkingBR();
				MOrderLine [] lines = morder.getLines(whereClause, "");
				
				if(lines.length == 1){
					//Close (Parking before release) charge
					MOrderLine line = lines[0];
					line.set_ValueOfColumn("n_endtime", ts);
					
					Date end_date = new Date();
					Date n_starttime = tpUtility.getFormat().parse(line.get_Value("n_starttime") == null?morder.get_Value("created").toString():line.get_Value("n_starttime").toString());
					Long diff = sse.getUmoDasedTimeDiff(104, n_starttime, end_date);
					
					line.set_ValueOfColumn("n_starttime", new  Timestamp(n_starttime.getTime()));
					line.set_ValueOfColumn("n_endtime", ts);
					
					BigDecimal bg = new BigDecimal(diff);
					bg = bg.subtract(new BigDecimal(1));
					line.setPrice(morder.getM_PriceList_ID());
					line.setQty(bg);
					line.save();
					
				}else{
					//default parking is no added
					//check for add parking before release
					Date start_date = new Date(morder.getCreated().getTime());
					Date end_date = new Date();
					//chack container arived more 1 day
					Long diff = sse.getUmoDasedTimeDiff(104, start_date, end_date);
					BigDecimal bg = new BigDecimal(diff);
	    			//Parking available
	    			if((bg.intValue() -1) > 0){
	    				//check parking already added and close for more then one demounting
	    				whereClause = "AND m_product_id = " + tpUtility.getParkingBR() + "AND n_starttime IS NULL";
	    				lines = morder.getLines(whereClause, "");
	    				//No line
	    				if(lines.length == 0){
	    					
	    					//VALIDATE ALREADY MOUNTING IS ADD
	    					//close parking charge beforerelease
	    					whereClause = "AND C_ORDER_ID = "+C_Order_ID+" AND m_product_id = " + tpUtility.getMountDemount();
	    					lines = morder.getLines(whereClause, "");
	    					//mounting ekak already tiyenawanam parking add karanna one na
	    					if(lines.length <= 0){
	    						bg = bg.subtract(new BigDecimal(1));
		    					//Create new parking line
		    					mOrderLine = new MOrderLine(morder);
		    					product = MProduct.get(ctx,tpUtility.getParkingBR());
		    					mOrderLine.setProduct(product);
		    					mOrderLine.setQty(bg);
		    					mOrderLine.setPrice();
		    					mOrderLine.set_ValueOfColumn("n_starttime", new  Timestamp(start_date.getTime()));
		    					mOrderLine.set_ValueOfColumn("n_endtime", ts);
		    					mOrderLine.setTax();
		    					mOrderLine.save();
	    					}
	    					
	    					
    					//paking already closed
	    				}else{
	    					//Hare nothing to do
	    				}
	    				
	    			}
				}
				
			//invoice
			}else if(tblName.endsWith("C_InvoiceLine")){
				
				int c_invoice_id = Integer.parseInt(mTab.get_ValueAsString("C_Invoice_ID"));
				MInvoice minvoice = new MInvoice(ctx, c_invoice_id,trx.getTrxName());
				MInvoiceLine mInvoiceLine = new MInvoiceLine(minvoice);
				
				mInvoiceLine.setProduct(product);
				mInvoiceLine.setQty(new BigDecimal(1));
				mInvoiceLine.set_ValueOfColumn("start_time", ts);
				mInvoiceLine.setTax();
				mInvoiceLine.save();
			}
			
		}catch(Exception ex){ }
		
		finally{
			trx.commit();
			trx.close();
		}
		
		return "";
	}
}

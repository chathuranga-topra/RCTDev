package org.compiere.process;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.sql.Timestamp;
import org.compiere.apps.ADialog;
import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.util.DB;
import org.compiere.util.Trx;

public class ServiceStartEnd extends CalloutEngine{
	
	TpUtility tpUtility = new TpUtility();
	
	private int lsc_id = tpUtility.getLadernStorage();
	private int mProduct_id = 0;
	int mtAndDeMt = tpUtility.getMountDemount();
 	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
 	
	//callout for sales orderline start/end time
	//org.compiere.process.ServiceStartEnd.setSOStartEnd
	public String setSOStartEnd(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		try{
			if(value!=null && mTab.getValue("n_starttime") != null && mTab.getValue("n_endtime") != null){
				
				//validate for product UOM only hours
				mProduct_id = Integer.parseInt(mTab.getValue("M_Product_ID").toString());
				MProduct mProduct = MProduct.get(ctx, mProduct_id);
				
				if(mProduct.getC_UOM_ID() == 101 || mProduct.getC_UOM_ID() == 104 || mProduct.getC_UOM_ID() == 102){
					
					Date n_starttime = format.parse(mTab.getValue("n_starttime").toString());
					Date n_endtime = format.parse(mTab.getValue("n_endtime").toString());
					
					BigDecimal bg = new BigDecimal(getUmoDasedTimeDiff(mProduct.getC_UOM_ID() , n_starttime , n_endtime));
					bg = bg.setScale(0, RoundingMode.CEILING);
					
					if(bg.intValue() > 0){
						
						if(mProduct.get_ID() == mtAndDeMt){
							
							mTab.setValue("qtyentered", new BigDecimal(1));
					    	mTab.setValue("qtyordered", new BigDecimal(1));
					    	
						}else{
							mTab.setValue("qtyentered", bg);
					    	mTab.setValue("qtyordered", bg);
						}
				    	
				    	//for ladern storage charge
						if((mProduct.getC_UOM_ID() == 104 || mProduct.getC_UOM_ID() == 102) && mProduct.get_ID() == mtAndDeMt){
							this.setLadernStorageWithMountDeMounting(ctx, mTab, n_endtime , bg);
						}
				    }
				    else{	
				    	
				    	mTab.setValue("n_endtime", null);
				    	mTab.getField("n_endtime").isMandatory(true);
				    	mTab.getField("n_endtime").setError(true);
				    	ADialog.error(WindowNo, null, "End time validation error" , "End time should greater than Start time!");
				    }
				}
			}
			
		}catch(Exception ex){
			ADialog.error(WindowNo, null, "System Error" , ex.toString());
			ex.printStackTrace();
		}
		return "";
		
	}
	
	//callout for sales Invoiceline start/end time
	//org.compiere.process.ServiceStartEnd.setSIStartEnd
	public String setSIStartEnd(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		try{
			if(value!=null && mTab.getValue("start_time") != null && mTab.getValue("end_time") != null){
				
				//validate for product UOM only hours
				mProduct_id = Integer.parseInt(mTab.getValue("M_Product_ID").toString());
				MProduct mProduct = MProduct.get(ctx, mProduct_id);
				
				if(mProduct.getC_UOM_ID() == 101 || mProduct.getC_UOM_ID() == 104 || mProduct.getC_UOM_ID() == 102){
				
					Date n_starttime = format.parse(mTab.getValue("start_time").toString());
					Date n_endtime = format.parse(mTab.getValue("end_time").toString());
					BigDecimal bg = new BigDecimal(getUmoDasedTimeDiff(mProduct.getC_UOM_ID() , n_starttime , n_endtime));
					
					bg = bg.setScale(0, RoundingMode.CEILING);
					
					//validate for end date is lass with start date
				    if(bg.intValue() > 0){
				    	
				    	if(mProduct.get_ID() == mtAndDeMt){
							mTab.setValue("qtyinvoiced", new BigDecimal(1));
					    	mTab.setValue("qtyentered", new BigDecimal(1));
						}else{
					    	mTab.setValue("qtyinvoiced", bg);
					    	mTab.setValue("qtyentered", bg);
						}
				    	
				    	//for ladern storage charge
						if((mProduct.getC_UOM_ID() == 104 || mProduct.getC_UOM_ID() == 102) && mProduct.get_ID() == mtAndDeMt){
							this.setLadernStorageWithMountDeMounting(ctx, mTab, n_endtime , bg);
						}
				    }
				    else{	
				    	
				    	mTab.setValue("end_time", null);
				    	mTab.getField("end_time").isMandatory(true);
				    	mTab.getField("end_time").setError(true);
				    	ADialog.error(WindowNo, null, "End time validation error" , "End time should greater than Start time!");
				    }
				}
			}
			
		}catch(Exception ex){
			ADialog.error(WindowNo, null, "System Error" , ex.toString());
			ex.printStackTrace();
		}
		return "";
	}

	
	public long getUmoDasedTimeDiff(int uom_id ,  Date n_starttime , Date n_endtime) throws ParseException{
		
		long dif = 0;
		//for hours
		if(uom_id == 101){
			dif = ((n_endtime.getTime() - n_starttime.getTime()));
			//Ten minutes release for next hour
			long difLongMins = dif / (60 * 1000);
			double difMins = dif / (60.0 * 1000.0);
			
			//an One hour release for next day
			if((difMins%60) > 10){
				dif = (long) (difMins/60) + 1;
			}else{
				dif = (long) (difMins/60);
			}
			//for same Hour
			if((difLongMins/60) == 0)
				dif = 1;
			//System.out.println(dif);
		//for days	
		}else if(uom_id == 104 || uom_id == 102){
			dif = ((n_endtime.getTime() - n_starttime.getTime()));
			long difLongHour = dif / (60 * 60 * 1000);
			double hourdif = dif / (60.0 * 60.0 * 1000.0);
			
			//an One hour release for next day
			if((hourdif%24) > 1){
				dif = (long) (hourdif/24) + 1;
			}else{
				dif = (long) (hourdif/24);
			}
			//for same day
			if((difLongHour/24) == 0)
				dif = 1;
		}
		
		return dif;
	}
	
	//when close mounting demounting it should be closed ladern storage charge also
	public void setLadernStorageWithMountDeMounting(Properties ctx  ,GridTab mtab,Date n_endtime , BigDecimal bg){
		
		String sql = "";
		
		String trxName = Trx.createTrxName("IVG");
		Trx trx = Trx.get(trxName, true);	//trx needs to be committed too
		Timestamp tp = new Timestamp(n_endtime.getTime());  
		
		try{
			trx.start();
			
			//Sales Order
			if(mtab.getTableName().equals(MOrderLine.Table_Name)){
				
				int C_ORDER_ID = Integer.parseInt(mtab.get_ValueAsString("C_Order_ID"));
				
				sql ="SELECT C_ORDERLINE_ID FROM C_ORDERLINE WHERE  ISACTIVE = 'Y' AND M_PRODUCT_ID = ? "
				+ "  AND C_ORDER_ID = ? ORDER BY C_ORDERLINE_ID DESC FETCH FIRST ROW ONLY";
				int C_ORDERLINE_ID = DB.getSQLValue(mtab.getTrxInfo(), sql, lsc_id , C_ORDER_ID);
				
				if(C_ORDERLINE_ID < 0)
					return;
				
				MOrderLine mOrder = new MOrderLine(ctx, C_ORDERLINE_ID , trxName);
				mOrder.set_CustomColumn("n_endtime", tp);
				mOrder.setQty(bg);
				mOrder.save();
				
			//Sales Invoice	
			}else if(mtab.getTableName().equals(MInvoiceLine.Table_Name)){
				
				int C_Invoice_ID = Integer.parseInt(mtab.get_ValueAsString("C_Invoice_ID"));
				
				sql ="SELECT C_INVOICELINE_ID FROM C_INVOICELINE WHERE  ISACTIVE = 'Y' AND M_PRODUCT_ID = ? "
				+ "  AND C_INVOICE_ID = ? ORDER BY C_INVOICELINE_ID DESC FETCH FIRST ROW ONLY";
				int C_INVOICELINE_ID = DB.getSQLValue(mtab.getTrxInfo(), sql, lsc_id , C_Invoice_ID);
				
				if(C_INVOICELINE_ID < 0)
					return;
				
				MInvoiceLine mInvoiceLine = new MInvoiceLine(ctx, C_INVOICELINE_ID, trxName);
				mInvoiceLine.set_CustomColumn("end_time", tp);
				mInvoiceLine.setQty(bg);
				mInvoiceLine.save();
			}
			
		}catch(Exception ex){
			ADialog.error(0, null, ex.toString());
		}finally{
			trx.commit();
			trx.close();
		}
	}
}

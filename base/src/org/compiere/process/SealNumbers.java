package org.compiere.process;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

import org.adempiere.exceptions.DBException;
import org.compiere.apps.ADialog;
import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPartner;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.X_AD_Sequence;
import org.compiere.util.DB;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Trx;

//org.compiere.process.SealNumbers.SealNumberGen
public class SealNumbers extends CalloutEngine{
	
	TpUtility tpUtility = new TpUtility();
	private int ad_sequence_ad = tpUtility.getSealNoSeq();
	private int Seal_Charge_id = tpUtility.getSeal_Charge_id();
	private int C_Invoice_ID = 0;
	Trx trx = Trx.get("AdditionalServise", true);	//trx needs to be committed too
	ServiceStartEnd serviceStartEnd = new ServiceStartEnd();
	String sql = "";
	int M_INOUT_ID = 0;
	
	public String SealNumberGen(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{	
		C_Invoice_ID = mTab.getRecord_ID();
		if(C_Invoice_ID < 1) return"";
		
		trx.start();
		MInvoice mInvoice = new MInvoice(ctx, mTab.getRecord_ID(), trx.getTrxName());
		try{
			
			//VALIDATE FOR OPEN Sales Orders
			String  orders = this.getInvoicedOrders(trx.getTrxName());
			sql = "SELECT COUNT(C_ORDER_ID) FROM C_ORDER WHERE C_ORDER_ID IN ("+orders+") AND DOCSTATUS <> 'CO' AND DOCACTION <> 'CL'";
			if(DB.getSQLValue(trx.getTrxName() ,sql) > 0 ){
				ADialog.error(WindowNo, null, "Sales order document status is incomplete" , "Please complete sales order  before completing the invoice.");
				return "";
			}
			 
			//VALIDATE FOR OPEN SERVICES
			//Validate for invoiced lines have non closed lines
			sql = " SELECT m_inoutline_id , c_invoiceline_id  FROM c_invoiceline "
				+ " WHERE C_Invoice_ID = ? AND c_uom_ID in(101,102,104) AND END_TIME IS NULL AND qtyinvoiced <> 0 ";
			
			KeyNamePair [] knps = DB.getKeyNamePairs(sql, false, C_Invoice_ID);
			
			if(knps.length >=1){
				//open service are available now we have to cross check with the sales order
				//check in sales order whether order service is closed
				for(KeyNamePair knp : knps){
					
					 int c_inv_line = Integer.parseInt(knp.getName());
					 MInOutLine mLine = new MInOutLine(ctx, knp.getKey(), trx.getTrxName());
					 MOrderLine mOrderLine = (MOrderLine) mLine.getC_OrderLine();
					 
					 if(mOrderLine.get_Value("n_starttime") == null && mOrderLine.get_Value("n_endtime") == null){
						ADialog.error(WindowNo, null, "Open services are available!" , "Please close opened services before completing the invoice.");
						return "";
					 }else{
						 MInvoiceLine mInvoiceLine = new MInvoiceLine(ctx, c_inv_line, trx.getTrxName());
						 mInvoiceLine.set_CustomColumn("start_time", mOrderLine.get_Value("n_starttime"));
						 mInvoiceLine.set_CustomColumn("end_time", mOrderLine.get_Value("n_endtime"));
						 mInvoiceLine.setQty(mOrderLine.getQtyDelivered());
						 mInvoiceLine.save();
					 }
				}
			}
		
			M_INOUT_ID = this.getShipmentId(mInvoice);
			int totalShipLine = this.getNoShipLines();
			int totalInvLine = mInvoice.getLines().length - 1;
			
			if(totalShipLine > totalInvLine){
				
				//create non invoiced shipment line
				sql = " SELECT m_inoutline.m_inoutline_id , m_inoutline.m_product_id , "
					+ " m_inoutline.qtyentered ,C_ORDER.c_order_id , C_ORDER.description , c_orderline.c_tax_id , "
					+ " c_orderline.n_starttime , c_orderline.n_endtime , C_ORDER.m_pricelist_id  FROM m_inoutline  "
					+ " LEFT JOIN c_invoiceline ON m_inoutline.m_inoutline_id = c_invoiceline.m_inoutline_id "
					+ " LEFT JOIN c_orderline ON m_inoutline.c_orderline_id = c_orderline.c_orderline_id "
					+ " LEFT JOIN C_ORDER ON c_orderline.c_order_id =  C_ORDER.c_order_id "
					+ " WHERE c_invoiceline.m_inoutline_id IS NULL "
					+ " AND m_inoutline.m_inout_id = ? "
					+ " AND m_inoutline.qtyentered <> 0 "
					+ " AND C_ORDER.c_order_id IN ("+ this.getInvoicedOrders(trx.getTrxName()) +");";
				
				PreparedStatement pstmt = null;
		        ResultSet rs = null;
		        try
		        {
		        	pstmt = DB.prepareStatement(sql, trx.getTrxName());
		        	pstmt.setInt(1, M_INOUT_ID);
		        	//pstmt.setString(2, this.getInvoicedOrders(trx.getTrxName()));
		            rs = pstmt.executeQuery();
		            MInvoiceLine line;
		            
		            while (rs.next()){
		            	//create invoice lines
		            	line = new MInvoiceLine(mInvoice);
		            	line.setM_Product_ID(rs.getInt("M_Product_ID"));
		            	line.setM_InOutLine_ID(rs.getInt("m_inoutline_id"));
		            	line.setDescription(rs.getString("description"));
		            	line.setQty(rs.getInt("qtyentered"));
		            	line.setC_Tax_ID(rs.getInt("c_tax_id"));
		            	line.set_CustomColumn("m_pricelist_id", rs.getInt("m_pricelist_id"));
		            	line.setPrice();
		            	//set times
		            	if(rs.getString("n_starttime") !=null && rs.getString("n_endtime")!=null){
		            		Date start_time = tpUtility.getFormat().parse(rs.getString("n_starttime"));
			            	Date end_time = tpUtility.getFormat().parse(rs.getString("n_endtime"));
			            	Timestamp tpstart = new Timestamp(start_time.getTime());
			            	Timestamp tpend = new Timestamp(end_time.getTime());
			            	line.set_CustomColumn("start_time", tpstart);
			            	line.set_CustomColumn("end_time", tpend);
		            	}
		            	
		            	line.save();
		            }
		        }
		        catch (SQLException e){
		            throw new DBException(e, sql);
		        }finally{
		            DB.close(rs, pstmt);
		            rs = null; pstmt = null;
		        }	
			}
			
			//SHIPMENT AND INVIOCED QUANTITY DIFFERENCE
			PreparedStatement pstmt = null;
	        ResultSet rs = null;
	        try
	        {
	        	pstmt = DB.prepareStatement(sql, trx.getTrxName());
	        	pstmt.setInt(1, M_INOUT_ID);
	            rs = pstmt.executeQuery();
	            MInvoiceLine line;
	            
		        sql = " SELECT m_inoutline.qtyentered , c_invoiceline.c_invoiceline_id , c_orderline.n_starttime ,c_orderline.n_endtime "
	        		+ " FROM m_inoutline "
	        		+ " INNER JOIN  c_invoiceline ON m_inoutline.m_inoutline_ID = c_invoiceline.m_inoutline_id "
	        		+ " INNER JOIN c_orderline ON m_inoutline.c_orderline_id = c_orderline.c_orderline_id "
	        		+ " AND m_inoutline.movementqty > c_invoiceline.qtyinvoiced "
	        		+ " AND m_inoutline.m_inout_id = ?";
				        
			        pstmt = DB.prepareStatement(sql, trx.getTrxName());
		        	pstmt.setInt(1, M_INOUT_ID);
		            rs = pstmt.executeQuery();
		            while (rs.next()){
		            	//create invoice lines
		            	line = new MInvoiceLine(ctx , rs.getInt("c_invoiceline_id") , trx.getTrxName());
		            	line.setQty(rs.getInt("qtyentered"));
		            	//set times
		            	if(rs.getString("n_starttime") !=null && rs.getString("n_endtime")!=null){
		            		Date start_time = tpUtility.getFormat().parse(rs.getString("n_starttime"));
			            	Date end_time = tpUtility.getFormat().parse(rs.getString("n_endtime"));
			            	Timestamp tpstart = new Timestamp(start_time.getTime());
			            	Timestamp tpend = new Timestamp(end_time.getTime());
			            	line.set_CustomColumn("start_time", tpstart);
			            	line.set_CustomColumn("end_time", tpend);
		            	}
		            	line.save();
		            }
			    
	        	}
			    catch (SQLException e){
		            throw new DBException(e, sql);
		        }finally{
		            DB.close(rs, pstmt);
		            rs = null; pstmt = null;
		        }
	        
			//SEAL NUMBER GENERATE
			for(MInvoiceLine line : mInvoice.getLines(true)){
				if(! (line.getProduct() == null || line.getProduct().get_ID() != Seal_Charge_id)){
					//validate for already added numbers
					if(line.get_Value("c_seal_no") == null || line.get_Value("c_seal_no").equals(""))
						this.setSealNumber(ctx , line);
				}
			}
			
			//Validate for invoiced lines have non closed lines
			sql = " SELECT COUNT(c_invoice_ID) FROM c_invoiceline "
				+ "WHERE C_Invoice_ID = ? AND c_uom_ID in(101,102,104) AND END_TIME IS NULL AND qtyinvoiced <> 0";
			int count = DB.getSQLValue(trx.getTrxName(), sql , mInvoice.get_ID());
			if(count >= 1){
				ADialog.error(WindowNo, null, "Open services are available!" , "Please close opened services before completing the invoice.");
				return "";
			}
			
			int tax_id = 0;
			//Here there should be a validation for invoice customer tax
			if(mInvoice.getC_BPartner().isTaxExempt())
				tax_id = 1000011;//NBT2 % SVAT
			else
				tax_id = 1000004;//NBT and VAT
			
			sql = "UPDATE C_InvoiceLine SET C_Tax_ID = "+tax_id+" where C_Invoice_ID = ?";
			DB.executeUpdate(sql, mInvoice.get_ID(), trx.getTrxName());
			
			//business partner deposit customer and validate the open balance
			MBPartner bp = (MBPartner) mInvoice.getC_BPartner();
			if(bp.get_Value("is_deposit") != null){
				if(bp.get_Value("is_deposit").equals(true)){
					//Check Open Balance
					if((bp.getTotalOpenBalance().intValue()*-1) < mInvoice.getGrandTotal().intValue()){
						ADialog.error(WindowNo, null, "Deposit customer less open balance!" , "Open Balance : " + bp.getTotalOpenBalance().doubleValue() + " Invoice Amount : " +  mInvoice.getGrandTotal());
						return "";
					}
				}
			}
			
			mInvoice.set_CustomColumn("is_seal_no_gen", "Y");
			mInvoice.save();
			
		}catch(Exception ex){
			ex.printStackTrace();
			ADialog.error(WindowNo, null, "Error", ex.getMessage());
		}finally{
			trx.commit();
			trx.close();
		}

		mTab.dataRefresh();
		return "";
	}
	
	//get shipment id based on the invoie
	private int getShipmentId(MInvoice mInvoice){
		
		int M_inout_id = 0;
		//get Shipment id
		for(MInvoiceLine line : mInvoice.getLines(true)){
			if(line.getM_InOutLine().getM_InOut_ID() >  0){
				M_inout_id = line.getM_InOutLine().getM_InOut_ID();
				break;
			}
		}
		
		return M_inout_id;
	}
	
	//get no of shipment lines 
	private int getNoShipLines(){
		sql = "SELECT count(m_inoutline_ID) FROM m_inoutline WHERE M_INOUT_ID = ?";
		return DB.getSQLValue(trx.getTrxName(), sql, M_INOUT_ID);
	}
	
	private void setSealNumber(Properties ctx ,MInvoiceLine line) throws Exception{
		
		X_AD_Sequence sequence = new X_AD_Sequence(ctx , this.ad_sequence_ad , line.get_TrxName());
		//get ad sequene current next and set to invoice line Seal nimber
		line.set_CustomColumn("c_seal_no", sequence.getCurrentNext());
		sequence.setCurrentNext(sequence.getCurrentNext() + sequence.getIncrementNo());
		
		line.save();
		sequence.save();
	}
	
	private String getInvoicedOrders(String trxName){
		
		sql = " SELECT  string_agg(DISTINCT OL.C_ORDER_ID ::text, ',') "
			+ " FROM C_INVOICELINE IL , C_ORDERLINE OL "
			+ " WHERE IL.c_invoice_id = ? "
			+ " AND IL.c_orderline_id = OL.c_orderline_id ";
		return DB.getSQLValueString(trxName, sql, C_Invoice_ID);
	}
}

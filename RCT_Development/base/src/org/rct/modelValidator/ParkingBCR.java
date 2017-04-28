package org.rct.modelValidator;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import org.adempiere.exceptions.DBException;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrderLine;
import org.compiere.process.ServiceStartEnd;
import org.compiere.process.TpUtility;
import org.compiere.util.DB;

//Calculate the parking before custom release and this parking UOM is day
public class ParkingBCR{
	
	TpUtility util = new TpUtility();
	ServiceStartEnd sse = new ServiceStartEnd();
	MInOut mInOut = null;
	
	//SET CUSTOMER BEFORE RELEASE PARKING WHEN COMPLETING THE SHIPMENT
	public void setParkingMinOut(MInOut mInOut) throws ParseException{
		this.mInOut = mInOut;
		//get All sales orders which are in shipment
		String sql = " SELECT DISTINCT c_order.c_order_id ,c_order.created , c_order.m_pricelist_id "
				+ " FROM M_InOutLine , c_orderline , c_order "
				+ " WHERE M_InOutLine.c_orderline_id  =  c_orderline.c_orderline_id "
				+ " AND c_orderline.c_order_id = c_order.c_order_id "
				+ " AND M_InOutLine.M_InOut_ID = ?";
		
		PreparedStatement pstmt = null;
        ResultSet rs = null;
        try
        {
        	pstmt = DB.prepareStatement(sql, mInOut.get_TrxName());
            pstmt.setInt(1, + mInOut.get_ID());
            rs = pstmt.executeQuery();
            
            int c_order_id = 0;
            String  created = "";
            int m_pricelist_id = 0;
            
            while (rs.next())
            {
            	Date n_starttime = util.getFormat().parse(rs.getString(2));	
    			Date n_endtime = new Date();
    			long dif = ((n_endtime.getTime() - n_starttime.getTime()));
    			dif = dif / (24 * 60 * 60 * 1000);
    			BigDecimal bg = new BigDecimal(dif);
    			
    			c_order_id = rs.getInt(1);
				created = rs.getString(2);
				m_pricelist_id = rs.getInt(3);
    			
    			//PARKING AVAILABILITY
    			if(bg.intValue() > 0){
    				
    				//CHECK FOR PARKING LINE IS AVAILABLE IN SO
    				sql = "SELECT C_ORDERLINE_ID FROM C_ORDERLINE WHERE C_ORDER_ID = ? AND m_product_id = ?";
    				int i = DB.getSQLValue(mInOut.get_TrxName(), sql, c_order_id ,util.getParkingBR());
    				//order is having a parking line
    				if(i > 	1){
        				//CHECK FOR SALES ORDER HAS MOUNTING DEMOUNTING AND LADERN STORAGE
        				//IF SO NO NEED TO CALCULATE pARKING _CBR
        				sql = "SELECT C_ORDERLINE_ID FROM C_ORDERLINE WHERE C_ORDER_ID = ? AND m_product_id IN (? , ?)";
        				
        				int mount_ladern = DB.getSQLValue(mInOut.get_TrxName(), sql, c_order_id ,util.getMountDemount() , util.getLadernStorage());
        				if(mount_ladern > 1){
        					//mounting demounting available
        					//NOTHING TO DO PARKING ALREADY CALCULATED WHEN PLACING MOUNTING DEMOUNTING
        				}else{
        					sql = "UPDATE C_ORDER SET DOCSTATUS = 'DR' , DOCACTION = 'CO' WHERE C_ORDER_ID = ?";
            				DB.executeUpdate(sql, c_order_id, mInOut.get_TrxName());
        					//UPDATE SALES ORDER LINES FOR PARKING
            				MOrderLine mOrderLine = new MOrderLine(mInOut.getCtx(), i, mInOut.get_TrxName());
            				mOrderLine.setQty(bg);
            				mOrderLine.setPrice(m_pricelist_id);
            				mOrderLine.setProcessed(true);
            				//SET START END TIME
            				mOrderLine.set_ValueOfColumn("n_starttime", new Timestamp(n_starttime.getTime()));
            				mOrderLine.set_ValueOfColumn("n_endtime", new Timestamp(n_endtime.getTime()));
            				mOrderLine.save();
            				//update shipment line quantity
            				sql = "update M_InOutLine  set movementqty = "+bg.intValue()+", qtyentered = "+bg.intValue()+" where M_InOut_ID= "+mInOut.get_ID()+" and c_orderline_id = ?";
        					DB.executeUpdate(sql, i, mInOut.get_TrxName());
            				//COMPLETE sales order
            				sql = "UPDATE C_ORDER SET DOCSTATUS = 'CO' , DOCACTION = 'CL' WHERE C_ORDER_ID = ?";
            				DB.executeUpdate(sql, c_order_id, mInOut.get_TrxName());
        				}
        				
					//line is not having a parking line	
    				}else{
    					
    					//CHECK FOR SALES ORDER HAS MOUNTING DEMOUNTING AND LADERN STORAGE
        				//IF SO NO NEED TO CALCULATE pARKING _CBR
        				sql = "SELECT C_ORDERLINE_ID FROM C_ORDERLINE WHERE C_ORDER_ID = ? AND m_product_id IN (? , ?)";
        				int mount_ladern = DB.getSQLValue(mInOut.get_TrxName(), sql, c_order_id ,util.getMountDemount() , util.getLadernStorage());
        				if(mount_ladern > 1){
        					//mounting demounting available
        					//NOTHING TO DO PARKING ALREADY CALCULATED WHEN PLACING MOUNTING DEMOUNTING
        				}else{
        					//open sales order
            				sql = "UPDATE C_ORDER SET DOCSTATUS = 'DR' , DOCACTION = 'CO' WHERE C_ORDER_ID = ?";
            				DB.executeUpdate(sql, c_order_id, mInOut.get_TrxName());
            				
            				//CREATE SALES ORDER LINES FOR PARKING
            				MOrderLine mOrderLine = new MOrderLine(mInOut.getCtx(), 0, mInOut.get_TrxName());
            				mOrderLine.setM_Product_ID(util.getParkingBR());
            				mOrderLine.setQty(bg);
            				mOrderLine.setC_Order_ID(c_order_id);
            				mOrderLine.setPrice(m_pricelist_id);
            				mOrderLine.setProcessed(true);
            				//SET START END TIME
            				mOrderLine.set_ValueOfColumn("n_starttime", new Timestamp(n_starttime.getTime()));
            				mOrderLine.set_ValueOfColumn("n_endtime", new Timestamp(n_endtime.getTime()));
            				mOrderLine.save();
            				
            				//CREATE SHIPMENT LINES
            				MInOutLine mInOutLine = new MInOutLine(mInOut);
            				mInOutLine.setOrderLine(mOrderLine, 0, bg);
            				mInOutLine.setQty(bg);
            				mInOutLine.setProcessed(true);
            				mInOutLine.saveEx();
            				//COMPLETE sales order
            				sql = "UPDATE C_ORDER SET DOCSTATUS = 'CO' , DOCACTION = 'CL' WHERE C_ORDER_ID = ?";
            				DB.executeUpdate(sql, c_order_id, mInOut.get_TrxName());
        				}
    				}
    			//NO parking	
    			}else{
    				//check for sales order is having parking line BUT NO PARKING
    				sql = "SELECT C_ORDERLINE_ID FROM C_ORDERLINE WHERE M_Product_ID = ? AND C_ORDER_ID = ?";
    				int i = DB.getSQLValueEx(mInOut.get_TrxName(), sql, util.getParkingBR(), c_order_id);
    				//parking line is available : remove it from shipment and sales order
    				if(i > 1){
    					//sales order
    					sql = "update C_OrderLine set n_starttime = CURRENT_DATE, n_endtime = CURRENT_DATE, qtyordered = 0 , qtyreserved = 0 , qtyentered = 0 where C_OrderLine_ID=?";
    					DB.executeUpdate(sql, i, mInOut.get_TrxName());
    					//shipment
    					sql = "update M_InOutLine  set movementqty = 0, qtyentered = 0 where M_InOut_ID= "+mInOut.get_ID()+" and c_orderline_id = ?";
    					DB.executeUpdate(sql, i, mInOut.get_TrxName());
    				}
    			}	
            }
        }
        catch (SQLException e){
            throw new DBException(e, sql);
        }finally{
            DB.close(rs, pstmt);
            rs = null; pstmt = null;
        }
	}
}

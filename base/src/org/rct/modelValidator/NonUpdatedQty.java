package org.rct.modelValidator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.process.TpUtility;
import org.compiere.util.DB;

public class NonUpdatedQty {
	
	TpUtility tp = new TpUtility();
	protected void setNonShipedQty(MOrder mOrder) {
		//get shipments with differennt order quantity
		String selectSql = " select sl.m_inoutline_id,ol.qtyentered FROM m_inoutline sl, c_orderline ol "
				+ " where sl.c_orderline_id = ol.c_orderline_id "
				+ " and ol.c_order_id = ? and sl.qtyentered != ol.qtyentered";
		
		String updateSql = ""; 
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		pstmt = DB.prepareStatement (selectSql, mOrder.get_TrxName());
		
		try{
			//Order Id
			pstmt.setInt(1, mOrder.get_ID());
			//Document type customer clearence order
			//pstmt.setInt(2, tp.getCustomerClearenceDoc_type());
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				updateSql = "UPDATE M_INOUTLINE SET qtyentered = " + rs.getInt("qtyentered") + ", movementqty = " + rs.getInt("qtyentered") + " "
						+ " WHERE  M_INOUTLINE_ID = " + rs.getInt("m_inoutline_id");
				DB.executeUpdate(updateSql);
				updateSql = "";
			}
			
			rs.close ();
			pstmt.close ();
			pstmt = null;
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	protected void setNonShipedLine(MOrder mOrder) {
		
		//Shipment is not still created only complete the sales order
		String sql = " SELECT DISTINCT m_inoutline.m_inout_id  FROM "
				+ " c_orderline LEFT JOIN m_inoutline ON c_orderline.c_orderline_id = m_inoutline.c_orderline_id "
				+ " WHERE m_inoutline.m_inoutline_id IS not NULL "
				+ " AND c_orderline.c_order_ID = ? ";
		
		int mInOut_id = DB.getSQLValue(mOrder.get_TrxName(), sql , mOrder.get_ID());
		if(mInOut_id <=0){
			//Shipment is not still created only complete the sales order
			return;
		}
		
		//GET NON SHIPED ORDERLINES
		sql = "SELECT c_orderline.c_orderline_ID , c_orderline.m_product_id FROM "
			+ " c_orderline LEFT JOIN m_inoutline ON c_orderline.c_orderline_id = m_inoutline.c_orderline_id "
			+ " WHERE m_inoutline.m_inoutline_id IS NULL AND c_orderline.c_order_ID = ? ";
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		pstmt = DB.prepareStatement (sql, mOrder.get_TrxName());
		MInOut mInOut = null;
		 mInOut_id = 0;
		
		try{
			pstmt.setInt(1, mOrder.get_ID());
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{	
				//get shipment and activate for use it
				if(rs.isFirst()){
					
					sql = " SELECT DISTINCT m_inoutline.m_inout_id  FROM "
							+ " c_orderline LEFT JOIN m_inoutline ON c_orderline.c_orderline_id = m_inoutline.c_orderline_id "
							+ " WHERE m_inoutline.m_inoutline_id IS not NULL "
							+ " AND c_orderline.c_order_ID = ? ";
					mInOut_id = DB.getSQLValue(mOrder.get_TrxName(), sql , mOrder.get_ID());
					//No shipment is created yet process should aborded
					if(mInOut_id <= 0){
						rs.close ();
						pstmt.close ();
						pstmt = null;
						return;
					}
					
					//activate the generated shipment
					sql = "UPDATE M_InOut SET DOCACTION = 'CO' , DOCSTATUS = 'DR',PROCESSED = 'N' WHERE M_InOut_ID = " + mInOut_id;
					DB.executeUpdate(sql);
					//Shipment is generated
					mInOut = new MInOut(mOrder.getCtx(), mInOut_id , mOrder.get_TrxName());
				}
				
				//HERE THE LOGIS GOES TO ADD NEW SHIPMENT LINES
				MOrderLine mOrderLine = new MOrderLine(mOrder.getCtx(), rs.getInt("C_ORDERLINE_ID"), mOrder.get_TrxName());
				MInOutLine mInOutLine = new MInOutLine(mInOut);
				mInOutLine.setOrderLine(mOrderLine, 0, mOrderLine.getQtyOrdered());
				mInOutLine.setQty(mOrderLine.getQtyOrdered());
				mInOutLine.setProcessed(true);
				mInOutLine.save();
			}
			
			//complete the shipment
			sql = "UPDATE M_InOut SET DOCACTION = 'CL' , DOCSTATUS = 'CO',PROCESSED = 'Y' WHERE M_InOut_ID = " + mInOut_id;
			DB.executeUpdate(sql);
			
			rs.close ();
			pstmt.close ();
			pstmt = null;
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}


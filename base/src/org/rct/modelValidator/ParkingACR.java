package org.rct.modelValidator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.process.ServiceStartEnd;
import org.compiere.process.TpUtility;
import org.compiere.util.DB;

public class ParkingACR {

	private ServiceStartEnd sse = new ServiceStartEnd();
	private MInOut mInOut =  null;
	private TpUtility tpUtility = new TpUtility();
	//When Shipment is completed new parking calculation started with invoice 
	public void setParkingACR(MInvoice mInvoice){
	
		String sql = " SELECT DISTINCT m_inout.m_inout_ID FROM m_inout , m_inoutline , c_invoiceline ,C_Invoice"
				+ " WHERE m_inout.m_inout_ID = m_inoutline.m_inout_ID "
				+ " AND m_inoutline.m_inoutline_ID = c_invoiceline.m_inoutline_ID "
				+ " AND c_invoiceline.C_Invoice_ID = C_Invoice.C_Invoice_ID "
				+ " AND C_Invoice.C_Invoice_ID=?";
		
		int M_InOut_ID = DB.getSQLValue(mInvoice.get_TrxName(), sql, mInvoice.get_ID());
		if(M_InOut_ID > 0){
			mInOut = new MInOut(mInvoice.getCtx(), M_InOut_ID, mInvoice.get_TrxName());
		}else
			return;
		
		try {
			//Date start = format.parse(mInOut.getCreated());
			Date start = new Date(mInOut.getCreated().getTime());
			Date end = new Date();
			
			BigDecimal bg = new BigDecimal(sse.getUmoDasedTimeDiff(101, start, end));
			bg = bg.setScale(0, RoundingMode.CEILING);
			//one hour is free for departure
			bg = bg.subtract(new BigDecimal(1));
			//Parking is eligible for particular containers
			if(bg.compareTo(new BigDecimal(1)) >=0){
				for (Integer i: this.getOrders(mInvoice))
				{
					String conno = DB.getSQLValueString(mInvoice.get_TrxName(), "select description from c_order where c_order_id = ?", i);
					MInvoiceLine mInvoiceLine = new MInvoiceLine(mInvoice);
					mInvoiceLine.setM_Product_ID(tpUtility.getParkingAR());
					mInvoiceLine.setQty(bg);
					mInvoiceLine.setDescription(conno);
					//Set start end time
					Timestamp start_tp = new Timestamp(start.getTime()); 
					Timestamp end_tp = new Timestamp(end.getTime());
					mInvoiceLine.set_ValueOfColumn("start_time", start_tp);
					mInvoiceLine.set_ValueOfColumn("end_time", end_tp);
					//save changes
					mInvoiceLine.save();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Set<Integer> getOrders(MInvoice mInvoice){
		
		Set<Integer> orders = new TreeSet<Integer>();
		for(MInvoiceLine line : mInvoice.getLines()){
			int id = line.getC_OrderLine().getC_Order_ID();
			if(id > 0)
			orders.add(id);
		}
		return orders;
	}
}

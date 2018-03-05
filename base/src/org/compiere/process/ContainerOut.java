package org.compiere.process;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.apps.ADialog;
import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.util.DB;
import org.compiere.util.Trx;
import org.topra.model.MCPostGatePass;

//org.compiere.process.ContainerOut.process
public class ContainerOut extends CalloutEngine{
	
	private Trx trx = null;
	private Properties ctx = null;
	private TpUtility tpUtility = null;
	private String message = null;
	
	
	public String process(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value){
		
		this.ctx = ctx;
		if(mTab.getRecord_ID() == -1) return "";
				
		try{
			trx = Trx.get(Trx.createTrxName("POSave"), true);
			trx.start();
			
			tpUtility = new TpUtility();
			MOrder mOrder = new MOrder(ctx, mTab.getRecord_ID(),trx.getTrxName());
			Timestamp currentTime = new Timestamp(System.currentTimeMillis());
			Timestamp invPaymentTime = null;
			
			//validate for already out containers
			if(mOrder.getDocStatus().equals("CL") && mOrder.getDocAction().equals("--"))
				throw new AdempiereException("STOP! This container is already out");
			
			//Validation 1 : all order has invoices and all are paid
			//Validate for invoice for main order type customer clearance
			int[] invIDs = MInvoice.getAllIDs(MInvoice.Table_Name, "DocStatus IN ('CO','CL') AND ISPAID = 'Y' AND C_Order_ID = "+mOrder.get_ID()+"", trx.getTrxName());
			if(invIDs.length == 0){
				
				String sql = "SELECT MIN(c_invoice_id) FROM  c_invoiceline "
						+ "WHERE C_OrderLine_ID=(SELECT MIN(C_OrderLine_ID) "
						+ "from C_OrderLine where c_order_id = ?)";
				
				int c_invoice_id = DB.getSQLValue(trx.getTrxName(), sql, mTab.getRecord_ID());
				MInvoice inv = new MInvoice(ctx, c_invoice_id, trx.getTrxName());
				
				if(inv.getDocStatus().equalsIgnoreCase("CO") || inv.getDocStatus().equalsIgnoreCase("CL")){
					
					if(!inv.isPaid()){
						throw new AdempiereException("Not Paid invoices are available");
					}
					
				}else{
					throw new AdempiereException("Not completed invoices are available");
				}
				
				//validate for one invoice for many sales order
			}
				//throw new AdempiereException("Non invoiced container! Please Invoice the container before release");
			
			//check for this container has post gate passes
			MOrder[] mOrders = MCPostGatePass.getPGPOrders(ctx,mTab.getRecord_ID(),trx.getTrxName());
			for(MOrder order : mOrders){
				
				MInvoice [] pgInvs = order.getInvoices();
				if(pgInvs.length == 0)
					throw new AdempiereException("STOP! Not paid invoices are available");
				
				for(MInvoice i : pgInvs){
					
					//validate the invoice for correct document status , there can be voided invoices
					if(i.getDocStatus().equalsIgnoreCase("CO") || i.getDocStatus().equalsIgnoreCase("CL")){
						if(!i.isPaid())
							throw new AdempiereException("STOP! Not paid invoices are available");
					}
				}
			}
			//Validation 2 : Parking 
			//validate for parking : Based on the customer clearance order and Invoice payment time
			//The particular container should out from RCT before an hour, other wise the container is entitled for parking
			//System automatically generate type of "POST GATE PASS" sales order then it should be paidnvoices a
			
			/*invPaymentTime = this.getInvoicePaymentTime(mOrder);
			long timeDiff = currentTime.getTime() - invPaymentTime.getTime();
			double diffHours = timeDiff/(60 * 60 * 1000);
			//diffHours = diffHours -1;//first hour is free to leave
			
			if(diffHours >=1){//check for parking is paid
				
				//validate for already creating parking invoices
				String sql = "SELECT count(c_orderline.*) from c_orderline , c_order , c_postgatepass WHERE c_orderline.c_order_id = c_order.c_order_id "
						+ " AND c_order.c_order_id = c_postgatepass.pg_orderid AND c_postgatepass.c_order_id = ? "
						+ " AND c_orderline.m_product_id = ? ";
				
				int i =  DB.getSQLValue(trx.getTrxName(), sql, mOrder.get_ID() , tpUtility.getParkingLateGatePass());
				
				if(i == 0){
					this.createPostGatePassParking(mOrder ,invPaymentTime);	
					throw new AdempiereException("Stop - Post gate pass parking available!");
					
				}else{
					mOrder.setDocAction("--");
					mOrder.setDocStatus("CL");
					
					if(mOrder.processIt("CL")){
						mOrder.save();
						ADialog.info(WindowNo, null, "Success!", "Container out.");
					}
				}
				
			}else{//ready to out the container
				
				mOrder.setDocAction("--");
				mOrder.setDocStatus("CL");
				
				if(mOrder.processIt("CL")){
					mOrder.save();
					ADialog.info(WindowNo, null, "Success!", "Container out.");
				}
			}*/
			
			mOrder.setDocAction("--");
			mOrder.setDocStatus("CL");
			
			if(mOrder.processIt("CL")){
				mOrder.save();
				ADialog.info(WindowNo, null, "Success!", "Container out.");
			}
			
		}catch(Exception ex){
			message = ex.getMessage();
			ADialog.error(WindowNo, null, "Error!" , message);
			
		}finally{
			trx.commit(); trx.close(); trx = null;
			mTab.dataRefresh();
		}
		
		return message;
	}
	
	private Timestamp getInvoicePaymentTime(MOrder order){
		
		String sql = null; Timestamp tp = null;
		
		//this select the last payment customer clearance order based invoice
		sql = "SELECT MAX(CREATED) AS CREATED FROM C_PAYMENT WHERE C_INVOICE_ID = ("
			+ " SELECT MAX(C_INVOICE_id) FROM C_INVOICE WHERE C_ORDER_ID =? AND DOCACTION  = 'CL' AND DOCSTATUS = 'CO' AND ISPAID = 'Y')"
			+ " AND DOCSTATUS = 'CO' AND DOCACTION = 'CL' AND ISACTIVE = 'Y'";
		
		System.out.println(sql);
		System.out.println(" order.get_ID()) : " + order.get_ID());
		
		tp = DB.getSQLValueTS(trx.getTrxName(), sql, order.get_ID());
		
		if(tp == null){//created time will be null for payment allocation
			
			sql = "SELECT MAX(CREATED) AS CREATED FROM c_allocationline WHERE C_INVOICE_ID = ("
					+ " SELECT MAX(C_INVOICE_id) FROM C_INVOICE WHERE C_ORDER_ID = ? AND DOCACTION  = 'CL' AND DOCSTATUS = 'CO' AND ISPAID = 'Y')";
			tp = DB.getSQLValueTS(trx.getTrxName(), sql, order.get_ID());
		}
		
		return tp;
	}
	
	private void createPostGatePassParking(MOrder mOrder ,Timestamp invPaymentTime ){
		
		MOrder order = new MOrder(ctx , 0 , trx.getTrxName());
		order.setC_DocTypeTarget_ID(tpUtility.getDocTypeParkingPostGatePass());
		order.setC_BPartner_ID(mOrder.getC_BPartner_ID());
		order.setPOReference(mOrder.getPOReference());
		order.setDescription(mOrder.getDescription());
		order.set_CustomColumn("c_order_vhno", mOrder.get_Value("c_order_vhno"));
		order.setM_PriceList_ID(mOrder.getM_PriceList_ID());
		order.setDocAction("CO");
		order.setDocStatus("DR");		
		order.save();
		
		//create lines
		MOrderLine mLine = new MOrderLine(order);
		mLine.setM_Product_ID(tpUtility.getParkingLateGatePass());
		
		Timestamp parkingStartTime = new Timestamp(invPaymentTime.getTime() + (60 * 60 * 1000));//parking will start after one hour from invoice payment
		mLine.set_CustomColumn("n_starttime", parkingStartTime);
		mLine.setQty(new BigDecimal(1));
		mLine.save();
		
		//creating record in C_PostGatePass table		
		MCPostGatePass mcPostGatePass = new MCPostGatePass(ctx, 0, trx.getTrxName());
		mcPostGatePass.setC_Order_ID(mOrder.get_ID());
		mcPostGatePass.setPG_OrderID(order.get_ID());
		mcPostGatePass.save();
	}
}






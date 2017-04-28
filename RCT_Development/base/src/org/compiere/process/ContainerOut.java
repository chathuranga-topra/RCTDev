package org.compiere.process;

import java.util.Properties;

import org.compiere.apps.ADialog;
import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.util.Trx;

//org.compiere.process.ContainerOut.process
public class ContainerOut extends CalloutEngine{
	
	private Trx trx = null;
	private Properties ctx = null;
	
	public String process(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value){
		
		this.ctx = ctx;
		if(mTab.getRecord_ID() == -1) return "";
				
		try{
			trx = Trx.get(Trx.createTrxName("POSave"), true);
			trx.start();
			
			MOrder mOrder = new MOrder(ctx, mTab.getRecord_ID(),trx.getTrxName());
			//validate for already release containers
			if(mOrder.getDocStatus().equals("CL") && mOrder.getDocAction().equals("--")){
				ADialog.error(WindowNo, null, "STOP!" , "This container is already out");
				return "";
			}
			//Validate for invoice
			MInvoice[] invoices = mOrder.getInvoices();
			if(invoices.length > 0){
				mOrder.setDocAction("--");
				mOrder.setDocStatus("CL");
				if(mOrder.processIt("CL")){
					mOrder.save();
					ADialog.info(WindowNo, null, "Success!", "Container out.");
				}
				
			}else{
				ADialog.error(WindowNo, null, "Non invoiced container!" , "You are not allowed out this container due to having open invoices.");
			}	
			
		}catch(Exception ex){
			
		}finally{
			trx.commit();
			trx.close();
			mTab.dataRefresh();
		}
		
		return null;
	}
	
	/*private void invoiceGen(MOrder mOrder){
		
		MInvoice mInvoice = new MInvoice(ctx, 0 ,trx.getTrxName());
		mInvoice.setC_BPartner_ID(mOrder.getC_BPartner_ID());
		mInvoice.setC_BPartner_Location_ID(mOrder.getC_BPartner_Location_ID());
		mInvoice.setC_DocTypeTarget_ID(this.C_DocTypeTarget_ID);
		mInvoice.setM_PriceList_ID(mOrder.getM_PriceList_ID());
		mInvoice.setDescription(mOrder.getDescription());
		mInvoice.setPOReference(mOrder.getPOReference());
		mInvoice.save();
		//System.out.println(mInvoice.getDocumentNo());
		MInvoiceLine mInvoiceLine = new MInvoiceLine(mInvoice);
		mInvoiceLine.setM_Product_ID(this.M_Product_ID);
		//set service start time
		mInvoiceLine.set_ValueOfColumn("start_time", this.shipment.getCreated());
		mInvoiceLine.save();
		
		//for reference the new invoice id is save to order field "c_order_tano"
		mOrder.set_ValueOfColumn("c_order_tano", mInvoice.get_ID());
		mOrder.save();
		trx.commit();
	}*/
}






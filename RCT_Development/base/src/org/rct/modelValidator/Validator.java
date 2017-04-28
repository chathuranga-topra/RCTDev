package org.rct.modelValidator;

import org.compiere.model.MClient;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.process.TpUtility;
import org.compiere.util.CLogger;

//org.rct.modelValidator.Validator
public class Validator implements ModelValidator{
	
	private int		m_AD_Client_ID = -1;
	private static CLogger log = CLogger.getCLogger(ParkingBCR.class);
	TpUtility tpUtility = new TpUtility();
	
	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		if (client != null) {	
			m_AD_Client_ID = client.getAD_Client_ID();
			log.info(client.toString());
		}	
		else
			log.info("Initializing global validator: "+this.toString());
		//Model Changes
		engine.addModelChange(MOrder.Table_Name, this);
		engine.addModelChange(MOrder.Table_Name, this);
		//engine.addModelChange(MInvoice.Table_Name, this);
		
		//Doc Validators
		engine.addDocValidate(MOrder.Table_Name, this);
		engine.addDocValidate(MInOut.Table_Name, this);
		//engine.addDocValidate(MInvoice.Table_Name, this);
		
	}

	@Override
	public int getAD_Client_ID() {
		return m_AD_Client_ID;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception {
		//Sales Order validate for uppercase - container number, cusdec
		if(po.get_TableName().equalsIgnoreCase(MOrder.Table_Name) && type == TYPE_BEFORE_NEW){
			//Make container number and cusdeck uppercase
			if(tpUtility.getCustomerClearenceDoc_type() == po.get_ValueAsInt("c_doctypetarget_id")){
				MOrder mOrder =(MOrder) po;
				mOrder.setDescription(mOrder.getDescription().toUpperCase().trim());
				mOrder.setPOReference(mOrder.getPOReference().toUpperCase().trim());
				mOrder.set_CustomColumn("c_order_vhno", mOrder.get_Value("c_order_vhno").toString().toUpperCase().trim());
			}
		}
		return null;
	}

	@Override
	public String docValidate(PO po, int timing) {
		try{
			//Work With Sales Order
			if(po.get_TableName().equalsIgnoreCase(MOrder.Table_Name) &&  timing == TIMING_BEFORE_COMPLETE){
				MOrder mOrder = (MOrder) po;
				//Validate for parking Charge
				//new ParkingChargeIN_Release().setParkingSO(mOrder);
				NonUpdatedQty nonUpdatedQty = new NonUpdatedQty();
				//Validate for non shipted quantity
				nonUpdatedQty.setNonShipedQty(mOrder);
				//Validate for non invoiced and quantity after(Not in the Shipment)
				nonUpdatedQty.setNonShipedLine(mOrder);
			//Work With Shipment
			}else if(po.get_TableName().equalsIgnoreCase(MInOut.Table_Name) && timing == TIMING_BEFORE_COMPLETE){
				MInOut mInOut = (MInOut) po;
				new ParkingBCR().setParkingMinOut(mInOut);
				
			}else if(po.get_TableName().equalsIgnoreCase(MInvoice.Table_Name) && timing == TIMING_AFTER_COMPLETE){
				
				//Auto Printing only for cashier role
				/*if(tpUtility.getCashierRole() == Env.getAD_Role_ID(po.getCtx())){
						MPrintFormat mPrintFormat = new MPrintFormat(po.getCtx(), tpUtility.getTaxInvoicePrintFormat(), po.get_TrxName());
						ReportCtl.startDocumentPrint(ReportEngine.INVOICE, mPrintFormat, po.get_ID(), null, 0, "");
				}*/
			}
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return null;
	}
}

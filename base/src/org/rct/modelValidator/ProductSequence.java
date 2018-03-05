package org.rct.modelValidator;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MSequence;
import org.compiere.model.PO;


//When Order line is placed automatically increment the particular sequence set in the product
public class ProductSequence {
	protected void setSequence(PO po){
	
		MOrderLine ol = (MOrderLine) po;
	 	MProduct mProduct =(MProduct) ol.getM_Product();
	 	
	 	if(mProduct == null){
	 		return;
	 	}
	 	
	 	if(mProduct.get_Value("M_ProductSequence") != null){
	 		
	 		int AD_Sequence_ID = mProduct.get_ValueAsInt("M_ProductSequence");
	 		MSequence sequence = new MSequence(po.getCtx(), AD_Sequence_ID, po.get_TrxName());
	 		
	 		//Preparing ticket no
	 		String seqNo = (sequence.getPrefix() == null?"":sequence.getPrefix()) + sequence.getCurrentNext()
	 				+ (sequence.getSuffix() == null ?"":sequence.getSuffix());
	 		sequence.setCurrentNext(sequence.getCurrentNext()+ sequence.getIncrementNo());
	 		sequence.save();
	 		//set sequence
	 		ol.set_CustomColumn("ticket_no", seqNo.trim());
	 		ol.save();
	 	}
	}
}

package org.rct.modelValidator;

import java.math.BigDecimal;

import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.process.TpUtility;
import org.compiere.util.Trx;

public class DefaultServices {
	
	TpUtility utility = new TpUtility();
	int ds[] = {utility.getPanel() ,utility.getSeal() , utility.getParkingBR()};

	//add default services
	public void addDefaultServices(MOrder order){
		
		/*Trx trx = Trx.get("AdditionalServise", true);	//trx needs to be committed too
		trx.start();
		try{
			//Is Already added
			if(order.getLines().length == 0){
				
				//for(int i : ds){
					
					MOrderLine mOrderLine = new MOrderLine(order.getCtx(), 0, trx.getTrxName());
					mOrderLine.setOrder(order);
					mOrderLine.setM_Product_ID(utility.getPanel(), true);
					mOrderLine.setC_BPartner_Location_ID(order.getBill_Location_ID());
					mOrderLine.setQty(new BigDecimal(1));
					mOrderLine.save();
					trx.commit();
				//}
			}
			
			
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			trx.commit();
			trx.close();
		}*/
		
	}
	
}

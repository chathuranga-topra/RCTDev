package org.topra.process;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.apps.AEnv;
import org.compiere.apps.AWindow;
import org.compiere.model.MOrder;
import org.compiere.model.MQuery;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Trx;
import org.topra.model.MCPostGatePass;

//org.topra.process.PostGatePassGen
public class PostGatePassGen extends SvrProcess{
	
	private int C_Order_ID = 0;
	
	private int C_DocTypeTarget_ID = 0;
	
	@Override
	protected void prepare() {
	
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("AD_Client_ID"))
				;
			
			else if (name.equals("AD_Org_ID"))
				;
			
			else if (name.equals("C_Order_ID"))
				C_Order_ID = para[i].getParameterAsInt();
			else if(name.equals("C_DocTypeTarget_ID"))
				C_DocTypeTarget_ID = para[i].getParameterAsInt();
				
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		
	}

	@Override
	protected String doIt() throws Exception {
		
		MOrder from = new MOrder(this.getCtx(), C_Order_ID , this.get_TrxName());
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		
		MOrder to = MOrder.copyFrom(from,ts , C_DocTypeTarget_ID, true, false, true, this.get_TrxName());
		to.setC_BPartner_ID(from.getC_BPartner_ID());
		
		//delete lines
		String sql = "DELETE FROM C_ORDERLINE WHERE C_ORDER_ID = ?";
		DB.executeUpdate(sql, to.get_ID(), this.get_TrxName());
		
		//set totals to zero
		to.setTotalLines(new BigDecimal(0));
		to.setGrandTotal(new BigDecimal(0));
		to.save();
		
		//create recoads in C_PosGatePass table
		MCPostGatePass postGatePass = new MCPostGatePass(this.getCtx(), 0 , this.get_TrxName());
		postGatePass.setC_Order_ID(from.get_ID());
		postGatePass.setPG_OrderID(to.get_ID());
		postGatePass.save();
		
		//commit the transaction
		Trx.get(this.get_TrxName(), false).commit();
		
		//Open Post gate pass window
		int AD_Window_ID = 1000038;// Post Gate pass Window ID
		String ColumnName = "C_ORDER_ID"; int Record_ID = to.get_ID();
		MQuery query = MQuery.getEqualQuery(ColumnName, Record_ID);
		
		AWindow frame = new AWindow();
		if (!frame.initWindow(AD_Window_ID, query))
			return "";
		AEnv.addToWindowManager(frame);
		AEnv.showCenterScreen(frame);
		frame = null;
		
		return "New Post gate pass Document No: " + to.getDocumentNo();
	}
}

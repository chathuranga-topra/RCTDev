package org.compiere.process;

import org.compiere.util.DB;

//org.compiere.process.ReGenerateReleaseNote
public class ReGenerateReleaseNote  extends SvrProcess{

	private int C_Order_ID;
	@Override
	protected void prepare() {
		
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null){
				;
			}else if (name.equals("C_Order_ID"))
				C_Order_ID = para[i].getParameterAsInt();	
		}		
	}

	@Override
	protected String doIt() throws Exception {
		
		// TODO Auto-generated method stub
		String sql = "UPDATE C_Order SET IsDelivered = 'N' WHERE C_ORDER_ID = ?" , message = "";
		int i = DB.executeUpdate(sql, C_Order_ID, this.get_TrxName());
		if(i==-1)
			message = "Process failed! System canot process this document, Please try to find the reason.";
		else
			 message = "Documet sucssesfully updated! You can regenerate the release!";
		 
		return message;
	}
}

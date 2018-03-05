package org.topra.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.model.I_C_Invoice;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.Query;
import org.compiere.util.DB;

public class MCPostGatePass extends X_C_PostGatePass{

	public MCPostGatePass(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	public MCPostGatePass(Properties ctx, int C_PostGatePass_ID, String trxName) {
		super(ctx, C_PostGatePass_ID, trxName);
		// TODO Auto-generated constructor stub
	}
	
	public static MOrder[]  getPGPOrders(Properties ctx,int orderID, String trxName){
		
		List<MOrder> list = new ArrayList<MOrder>();
		
		String sql = ""
			+ " SELECT pg.* FROM c_postgatepass pg , c_order o WHERE pg.c_order_id = ? "
			+ " AND pg.pg_orderid = o.c_order_id AND o.docstatus IN ('CO' , 'DR' , 'IP', 'IN') ORDER BY C_PostGatePass_ID DESC";
		
		PreparedStatement ps = null; ResultSet rs = null;
		try{
			ps = DB.prepareStatement(sql,trxName);
			ps.setInt(1, orderID);
			rs = ps.executeQuery();
			
			while(rs.next())
				list.add(new MOrder(ctx, rs.getInt("pg_orderid"), trxName));
			
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			DB.close(rs,ps);
			rs = null; ps = null;
		}

		return list.toArray(new MOrder[list.size()]);
	}
}

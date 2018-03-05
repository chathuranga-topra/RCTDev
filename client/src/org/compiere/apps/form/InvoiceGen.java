/******************************************************************************
 * Copyright (C) 2009 Low Heng Sin                                            *
 * Copyright (C) 2009 Idalica Corporation                                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.compiere.apps.form;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

import org.compiere.apps.IStatusBar;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.MOrder;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MPrivateAccess;
import org.compiere.model.MRMA;
import org.compiere.print.ReportEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Trx;

/**
 * Generate Invoice (manual) controller class
 * 
 */
public class InvoiceGen extends GenForm
{
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(InvoiceGen.class);
	
	public Object 			m_AD_Org_ID = null;
	public Object 			m_C_BPartner_ID = null;
	public Object			m_POReference= null;
	private int maxQueryRecords = 100;
	
	public void dynInit() throws Exception
	{
		setTitle("InvGenerateInfo");
		setReportEngineType(ReportEngine.INVOICE);
		setAskPrintMsg("PrintInvoices");
		
		//set user user max query recoards
		/*int currole_id = Env.getContextAsInt(Env.getCtx(), "#AD_Role_ID");	
		maxQueryRecords = new MRole(Env.getCtx(),currole_id , null).getMaxQueryRecords();
		
		if(maxQueryRecords == 0)
			maxQueryRecords = 50;*/
	}
	
	public void configureMiniTable(IMiniTable miniTable)
	{
		//  create Columns
		miniTable.addColumn("C_Order_ID");
		miniTable.addColumn("poreference");
		miniTable.addColumn("description");
		miniTable.addColumn("DocumentNo");
		miniTable.addColumn("C_BPartner_ID");
		miniTable.addColumn("Totallines");
		miniTable.addColumn("C_Oder_vhno");
		miniTable.addColumn("Inspec_dept");
		miniTable.addColumn("Release_Time");
		miniTable.addColumn("Size");
		//
		String A="CUSDEC Number";
		String B="Container Number";
		//String B2="Reference Number";
		String B2="Size";
		String D="Name of the Consignee";
		String E="Total Charge";
		String F="Lorry Number";
		String G="Department";
		String H="Release Time";
		//String I="Size";
		String I="In Doc No";
		//
		miniTable.setMultiSelection(true);
		//  set details
		miniTable.setColumnClass(0, IDColumn.class, false, " ");
		miniTable.setColumnClass(1, String.class, true, A);
		miniTable.setColumnClass(2, String.class, true, B);
		miniTable.setColumnClass(3, String.class, true, B2);
		miniTable.setColumnClass(4, String.class, true, D);
		miniTable.setColumnClass(5, BigDecimal.class, true, E);	
		miniTable.setColumnClass(6, String.class, true, F);
		miniTable.setColumnClass(7, String.class, true, G);
		miniTable.setColumnClass(8, String.class, true, H);
		miniTable.setColumnClass(9, String.class, true, I);
		//
		miniTable.autoSize();
	}
	/**
	 * Get SQL for Orders that needs to be shipped
	 * @return sql
	 */
	private String getOrderSQL()
	{
	    StringBuffer sql = new StringBuffer(
	            "SELECT  ic.C_Order_ID, ic.poreference, ic.description, ic.DocumentNo, bp.Name,"
	            + " ic.TotalLines,DateOrdered, o.Name, ic.c_order_vhno, ic.Inspec_dept, ic.created , ic.M_priceList_ID "
	    		+ "FROM c_taxInvoice_generate_v ic, AD_Org o, C_BPartner bp, C_DocType dt "
	            + "WHERE ic.AD_Org_ID=o.AD_Org_ID"
	            + " AND ic.C_BPartner_ID=bp.C_BPartner_ID"
	            + " AND ic.C_DocType_ID=dt.C_DocType_ID"
	            + " AND ic.AD_Client_ID=?");
	    
	    if (m_POReference.toString().length() > 0)
        	sql.append(" AND ic.poreference like'").append(m_POReference+"%'");
	    else
	    	sql.append(" AND ic.poreference =''");
        if (m_AD_Org_ID != null)
            sql.append(" AND ic.AD_Org_ID=").append(m_AD_Org_ID);
        if (m_C_BPartner_ID != null)
            sql.append(" AND ic.C_BPartner_ID=").append(m_C_BPartner_ID);
        
        // bug - [ 1713337 ] "Generate Invoices (manual)" show locked records.
        /* begin - Exclude locked records; @Trifon */
        int AD_User_ID = Env.getContextAsInt(Env.getCtx(), "#AD_User_ID");
        String lockedIDs = MPrivateAccess.getLockedRecordWhere(MOrder.Table_ID, AD_User_ID);
        if (lockedIDs != null)
        {
            if (sql.length() > 0)
                sql.append(" AND ");
            sql.append("C_Order_ID").append(lockedIDs);
            sql.append(" AND ic.totallines > 0 ");
            sql.append(" AND ic.docstatus = 'CO' ");
            
        }
        /* eng - Exclude locked records; @Trifon */

        //
        sql.append(" ORDER BY o.Name,bp.Name,DateOrdered fetch first "+maxQueryRecords+" rows only");
        
        return sql.toString();
	}
	
	/**
	 * Get SQL for Customer RMA that need to be invoiced
	 * @return sql
	 */
/**	private String getRMASql()
	{
		StringBuffer sql = new StringBuffer();
	    sql.append("SELECT rma.M_RMA_ID, org.Name, dt.Name, rma.DocumentNo, bp.Name, rma.Created, rma.Amt ");
        sql.append("FROM M_RMA rma INNER JOIN AD_Org org ON rma.AD_Org_ID=org.AD_Org_ID ");
        sql.append("INNER JOIN C_DocType dt ON rma.C_DocType_ID=dt.C_DocType_ID ");
        sql.append("INNER JOIN C_BPartner bp ON rma.C_BPartner_ID=bp.C_BPartner_ID ");
        sql.append("INNER JOIN M_InOut io ON rma.InOut_ID=io.M_InOut_ID ");
        sql.append("WHERE rma.DocStatus='CO' ");
        sql.append("AND dt.DocBaseType = 'SOO' ");
        // sql.append("AND NOT EXISTS (SELECT * FROM C_Invoice i ");
        // sql.append("WHERE i.M_RMA_ID=rma.M_RMA_ID AND i.DocStatus IN ('IP', 'CO', 'CL')) ");
        // sql.append("AND EXISTS (SELECT * FROM C_InvoiceLine il INNER JOIN M_InOutLine iol ");
        // sql.append("ON il.M_InOutLine_ID=iol.M_InOutLine_ID INNER JOIN C_Invoice i ");
        // sql.append("ON i.C_Invoice_ID=il.C_Invoice_ID WHERE i.DocStatus IN ('CO', 'CL') ");
        // sql.append("AND iol.M_InOutLine_ID IN ");
        // sql.append("(SELECT M_InOutLine_ID FROM M_RMALine rl WHERE rl.M_RMA_ID=rma.M_RMA_ID ");
        // sql.append("AND rl.M_InOutLine_ID IS NOT NULL)) ");
        sql.append("AND rma.AD_Client_ID=?");
        
        if (m_AD_Org_ID != null)
            sql.append(" AND rma.AD_Org_ID=").append(m_AD_Org_ID);
        if (m_C_BPartner_ID != null)
            sql.append(" AND bp.C_BPartner_ID=").append(m_C_BPartner_ID);
        
        int AD_User_ID = Env.getContextAsInt(Env.getCtx(), "#AD_User_ID");
        String lockedIDs = MPrivateAccess.getLockedRecordWhere(MRMA.Table_ID, AD_User_ID);
        if (lockedIDs != null)
        {
            sql.append(" AND rma.M_RMA_ID").append(lockedIDs);
        }
        
        sql.append(" ORDER BY org.Name, bp.Name, rma.Created ");
        
        return sql.toString();
	}**/
	
	/**
	 *  Query Info
	 */
	public void executeQuery(KeyNamePair docTypeKNPair, IMiniTable miniTable ,IStatusBar statusBar)
	{
		log.info("");
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		//  Create SQL
		
		String sql = "";
        
        if (docTypeKNPair.getKey() == MOrder.Table_ID)
        {
            sql = getOrderSQL();
        }
        else
        {
           // sql = getRMASql();
        }

		//  reset table
		int row = 0;
		miniTable.setRowCount(row);
		//  Execute
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, AD_Client_ID);
			ResultSet rs = pstmt.executeQuery();
			//
			while (rs.next())
			{
				//  extend table
				miniTable.setRowCount(row+1);
				//  set values
							miniTable.setValueAt(new IDColumn(rs.getInt(1)), row, 0);   //  C_Order_ID
				miniTable.setValueAt(rs.getString(2), row, 1);				//  Cusdec
				miniTable.setValueAt(rs.getString(3), row, 2);              //  DocType
				miniTable.setValueAt(rs.getString(4), row, 9);              //  Con Size
				miniTable.setValueAt(rs.getString(5), row, 4);              //  BPartner
				miniTable.setValueAt(rs.getBigDecimal(6), row, 5);			//	Totallines
				miniTable.setValueAt(rs.getString(9), row, 6);				//	Lorry No
				miniTable.setValueAt(rs.getString(10), row, 7);				//	Department
				miniTable.setValueAt(rs.getString(11), row, 8);				//	Relaese Time
				miniTable.setValueAt(rs.getString(12), row, 3);				//	Doc No
				
				
				//Table default select
				IDColumn id = (IDColumn)miniTable.getValueAt(row, 0);     //  ID in column 0
				id.setSelected(true);
				row++;
			}
			
			statusBar.setStatusDB(String.valueOf(miniTable.getRowCount()));
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		//
		miniTable.autoSize();
		
	}   //  executeQuery
	
	/**
	 *	Save Selection & return selecion Query or ""
	 *  @return where clause like C_Order_ID IN (...)
	 */
	public void saveSelection(IMiniTable miniTable)
	{
		log.info("");
		//  Array of Integers
		ArrayList<Integer> results = new ArrayList<Integer>();
		setSelection(null);

		//	Get selected entries
		int rows = miniTable.getRowCount();
		for (int i = 0; i < rows; i++)
		{
			IDColumn id = (IDColumn)miniTable.getValueAt(i, 0);     //  ID in column 0
		//	log.fine( "Row=" + i + " - " + id);
			if (id != null && id.isSelected())
				results.add(id.getRecord_ID());
		}

		if (results.size() == 0)
			return;
		log.config("Selected #" + results.size());
		setSelection(results);
	}	//	saveSelection

	
	
	//get shipment date for a particular order
	/*private String getShipmentdate(int C_Order_ID){
		
		Trx trx = Trx.get(Trx.createTrxName("FWFA"), true);
		trx.start();
		String sql = "SELECT io.created"
				+ " FROM m_inout io "
				+ " JOIN m_inoutline iol ON iol.m_inout_id = io.m_inout_id "
				+ " JOIN c_orderline ol "
				+ " ON iol.c_orderline_id = ol.c_orderline_id "
				+ " JOIN c_order o "
				+ " ON ol.c_order_id = o.c_order_id "
				+ " WHERE io.docstatus = 'CO' "
				+ " AND io.isactive = 'Y' "
				+ " AND o.c_order_id = ? "
				+ " FETCH  FIRST 1 ROWS ONLY "; 
		
		 String res =  DB.getSQLValueString(trx.getTrxName(), sql, C_Order_ID);
		 trx.commit();trx.close();trx = null;
		 return res;
	}
	
	/**************************************************************************
	 *	Generate Invoices
	 */
	public String generate(IStatusBar statusBar, KeyNamePair docTypeKNPair, String docActionSelected)
	{	
		String info = "";
		String trxName = Trx.createTrxName("IVG");
		Trx trx = Trx.get(trxName, true);	//trx needs to be committed too
		
		setSelectionActive(false);  //  prevents from being called twice
		statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "InvGenerateGen"));
		statusBar.setStatusDB(String.valueOf(getSelection().size()));

		//	Prepare Process
		int AD_Process_ID = 0;
        
        if (docTypeKNPair.getKey() == MRMA.Table_ID)
        {
            AD_Process_ID = 52002; // C_Invoice_GenerateRMA - org.adempiere.process.InvoiceGenerateRMA
        }
        else
        {
            AD_Process_ID = 134;  // HARDCODED    C_InvoiceCreate
        }
		MPInstance instance = new MPInstance(Env.getCtx(), AD_Process_ID, 0);
		if (!instance.save())
		{
			info = Msg.getMsg(Env.getCtx(), "ProcessNoInstance");
			return info;
		}
		
		//insert selection
		StringBuffer insert = new StringBuffer();
		insert.append("INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID) ");
		int counter = 0;
		for(Integer selectedId : getSelection())
		{
			counter++;
			if (counter > 1)
				insert.append(" UNION ");
			insert.append("SELECT ");
			insert.append(instance.getAD_PInstance_ID());
			insert.append(", ");
			insert.append(selectedId);
			insert.append(" FROM DUAL ");
			
			if (counter == 1000) 
			{
				if ( DB.executeUpdate(insert.toString(), trxName) < 0 )
				{
					String msg = "No Invoices";     //  not translated!
					info = msg;
					log.config(msg);
					trx.rollback();
					return info;
				}
				insert = new StringBuffer();
				insert.append("INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID) ");
				counter = 0;
			}
		}
		if (counter > 0)
		{
			if ( DB.executeUpdate(insert.toString(), trxName) < 0 )
			{
				String msg = "No Invoices";     //  not translated!
				info = msg;
				log.config(msg);
				trx.rollback();
				return info;  
			}
		}
		
		ProcessInfo pi = new ProcessInfo ("", AD_Process_ID);
		pi.setAD_PInstance_ID (instance.getAD_PInstance_ID());

		//	Add Parameters
		MPInstancePara para = new MPInstancePara(instance, 10);
		para.setParameter("Selection", "Y");
		if (!para.save())
		{
			String msg = "No Selection Parameter added";  //  not translated
			info = msg;
			log.log(Level.SEVERE, msg);
			return info;
		}
		
		para = new MPInstancePara(instance, 20);
		para.setParameter("DocAction", docActionSelected);
		
		if (!para.save())
		{
			String msg = "No DocAction Parameter added";  //  not translated
			info = msg;
			log.log(Level.SEVERE, msg);
			return info;
		}
		
		setTrx(trx);
		setProcessInfo(pi);
		
		return info;
	}	//	generateInvoices
}
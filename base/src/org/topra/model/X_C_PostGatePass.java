/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2007 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package org.topra.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for C_PostGatePass
 *  @author Adempiere (generated) 
 *  @version Release 3.7.0LTS - $Id$ */
public class X_C_PostGatePass extends PO implements I_C_PostGatePass, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20170712L;

    /** Standard Constructor */
    public X_C_PostGatePass (Properties ctx, int C_PostGatePass_ID, String trxName)
    {
      super (ctx, C_PostGatePass_ID, trxName);
      /** if (C_PostGatePass_ID == 0)
        {
			setC_PostGatePass_ID (0);
        } */
    }

    /** Load Constructor */
    public X_C_PostGatePass (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 1 - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_C_PostGatePass[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Order.
		@param C_Order_ID 
		Order
	  */
	public void setC_Order_ID (int C_Order_ID)
	{
		if (C_Order_ID < 1) 
			set_Value (COLUMNNAME_C_Order_ID, null);
		else 
			set_Value (COLUMNNAME_C_Order_ID, Integer.valueOf(C_Order_ID));
	}

	/** Get Order.
		@return Order
	  */
	public int getC_Order_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Order_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_PostGatePass.
		@param C_PostGatePass_ID C_PostGatePass	  */
	public void setC_PostGatePass_ID (int C_PostGatePass_ID)
	{
		if (C_PostGatePass_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_PostGatePass_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_PostGatePass_ID, Integer.valueOf(C_PostGatePass_ID));
	}

	/** Get C_PostGatePass.
		@return C_PostGatePass	  */
	public int getC_PostGatePass_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_PostGatePass_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set PG_OrderID.
		@param PG_OrderID PG_OrderID	  */
	public void setPG_OrderID (int PG_OrderID)
	{
		set_Value (COLUMNNAME_PG_OrderID, Integer.valueOf(PG_OrderID));
	}

	/** Get PG_OrderID.
		@return PG_OrderID	  */
	public int getPG_OrderID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PG_OrderID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}
package org.compiere.process;

import java.util.Properties;

import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MOrderLine;

//org.compiere.process.SevicesSequence.addSequence
public class SevicesSequence extends CalloutEngine {
	
	public void addSequence(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		
		if(value == null) return;
		
		//Sales Order Line
		if(mTab.getTableName().equals(MOrderLine.Table_Name)){
			
			
			
		}
	}
	
	
}

package org.compiere.process;

import java.text.SimpleDateFormat;

public class TpUtility {

	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	//Custome Clearence Order type
	private int customerClearenceDoc_type = 1000058;
	//Seal No Sequence
	private int sealNoSeq = 1000163;
	// (M_Product - M_Product_ID=1001988) Hard coded value for seal charge id
	private int Seal_Charge_id = 1001988;
	//default services (M_Product)
	private int panel = 1003005;
	//Before release
	private int parkingBR = 1000842;
	
	private int seal = 1001988;
	//Afer release
	private int parkingAR = 1004031;
	//Laden Storage Charges
	private int ladernStorage = 1000841;
	//Mounting Demounting and storaage (M_Product - M_Product_ID=1000880)
	private int mountDemount = 1000880;
	
	private int sealNumber = 1004044;
	
	private int taxInvoicePrintFormat = 1000688;
	
	private int cashierRole = 1000014;
	
	private int cusReleasePrintFormat = 1000109;
	
	public int getCusReleasePrintFormat() {
		return cusReleasePrintFormat;
	}
	public int getTaxInvoicePrintFormat() {
		return taxInvoicePrintFormat;
	}
	public void setTaxInvoicePrintFormat(int taxInvoicePrintFormat) {
		this.taxInvoicePrintFormat = taxInvoicePrintFormat;
	}
	public int getCashierRole() {
		return cashierRole;
	}
	public void setCashierRole(int cashierRole) {
		this.cashierRole = cashierRole;
	}
	public int getSealNumber() {
		return sealNumber;
	}
	public void setSealNumber(int sealNumber) {
		this.sealNumber = sealNumber;
	}
	public int getMountDemount() {
		return mountDemount;
	}
	public void setMountDemount(int mountDemount) {
		this.mountDemount = mountDemount;
	}
	public int getLadernStorage() {
		return ladernStorage;
	}
	public void setLadernStorage(int ladernStorage) {
		this.ladernStorage = ladernStorage;
	}
	public SimpleDateFormat getFormat() {
		return format;
	}
	public void setFormat(SimpleDateFormat format) {
		this.format = format;
	}
	public int getCustomerClearenceDoc_type() {
		return customerClearenceDoc_type;
	}
	public void setCustomerClearenceDoc_type(int customerClearenceDoc_type) {
		this.customerClearenceDoc_type = customerClearenceDoc_type;
	}
	public int getSealNoSeq() {
		return sealNoSeq;
	}
	public void setSealNoSeq(int sealNoSeq) {
		this.sealNoSeq = sealNoSeq;
	}
	public int getSeal_Charge_id() {
		return Seal_Charge_id;
	}
	public void setSeal_Charge_id(int seal_Charge_id) {
		Seal_Charge_id = seal_Charge_id;
	}
	public int getPanel() {
		return panel;
	}
	public void setPanel(int panel) {
		this.panel = panel;
	}
	public int getParkingBR() {
		return parkingBR;
	}
	public void setParkingBR(int parkingBR) {
		this.parkingBR = parkingBR;
	}
	public int getSeal() {
		return seal;
	}
	public void setSeal(int seal) {
		this.seal = seal;
	}
	public int getParkingAR() {
		return parkingAR;
	}
	public void setParkingAR(int parkingAR) {
		this.parkingAR = parkingAR;
	}
}

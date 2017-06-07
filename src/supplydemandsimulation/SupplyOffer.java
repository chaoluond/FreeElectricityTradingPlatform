/**
 * 
 */
package supplydemandsimulation;

import centralmanagment.PlatformController;

/**
 * @author Chao
 *
 */
public class SupplyOffer {
	public int offerid; // This bid id
	public int busid; // The bus id involved
	public long offerTime; // The time when the offer was submitted to the trading platform in minutes
	public double quantity; // Electricity offered in MWh
	public long minStartTime; // Earliest electricity supplying time in minutes
	public long maxStartTime; // Latest electricity supplying time in minutes
	public long startTime; // The matched start time
	public double minSourcePrice; // The minimum unit price ($/MWh) the supplier would like to receive from the user
	public double matchPrice;
	/*
	 * isContinuous == true ----> supply continuous and stable power
	 * isContinuous == false ----> supply discontinuous power
	 */
	public boolean isContinuous; 
	
	
	/*
	 * result == false ---- No match
	 * result == true ---- has a match
	 */
	public boolean result = false;
	public double quantitySupply = 0; // Supplied electricity in MWh
	public double supplyPlan = 0; // power supply (in MW) during each horizon
	public double totalSupplyPlan = 0; // Total power supply (in MWh) for this matched pair

	public SupplyOffer(int offerid, int busid, long offerTime, long minStartTime, 
			long maxStartTime, double quantity, double sourcePrice, boolean isContinuous) {
		this.offerid = offerid;
		this.busid = busid;
		this.offerTime = offerTime;
		this.minStartTime = minStartTime;
		this.maxStartTime = maxStartTime;
		this.quantity = quantity;
		this.minSourcePrice = sourcePrice;
		this.isContinuous = isContinuous;
	}
	
	public void setSupplyPlan(double supplyplan, double totalSupplyPlan) {
		this.supplyPlan = supplyplan;
		this.totalSupplyPlan = totalSupplyPlan;
	}
	
	public void setMatchPrice(double matchPrice) {
		this.matchPrice = matchPrice;
	}
	
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public void supply() {
		if (startTime <= PlatformController.standardTime)
			quantitySupply += supplyPlan * (PlatformController.timeInterval * 1.0 / PlatformController.min2hour);
	}
	
	
	public void print() {
		System.out.println("[ Bid id: " + offerid + ", bus id: " + busid + 
				", bidTime: " + offerTime + ", quantity: " + quantity + 
				", minTime: " + minStartTime + ", maxTime: " + 
				maxStartTime + ", sourcePrice: " + minSourcePrice + ", isContinuous: " + 
				isContinuous + ", result: " + result + ", matchPrice: " + matchPrice + 
				", quantitySupply: " + quantitySupply + ", startTime : " + startTime + 
				", supplyplan: " + supplyPlan + ", total supply plan: " + totalSupplyPlan + 
				"]");
	}

}

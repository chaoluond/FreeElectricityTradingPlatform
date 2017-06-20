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
	public double matchPrice; // The matched price
	/*
	 * isContinuous == true ----> supply continuous and stable power
	 * isContinuous == false ----> supply discontinuous power
	 */
	public boolean isContinuous; 
	
	
	/*
	 * isRenewable == true ---> renewable energy
	 * isRenewable == false ---> Not renewable energy
	 */
	public boolean isRenewable; 
	
	
	/*
	 * result == false ---- No match
	 * result == true ---- has a match
	 */
	public boolean result = false;
	public double quantitySupply = 0; // Supplied electricity in MWh
	public double minDeliverRate = 0; // minimum power deliver rate (in MW) during each interval
	public double maxDeliverRate = 0; // maximum power deliver rate (in MW) during each interval
	public double deliverRate = 0; // agreed deliver rate (in MW) during each interval
	public double totalSupplyPlan = 0; // Total power supply (in MWh) for this matched pair

	public SupplyOffer(int offerid, int busid, long offerTime, long minStartTime, 
			long maxStartTime, double quantity, double minDeliverRate, double maxDeliverRate,
			double sourcePrice, boolean isContinuous, boolean isRenewable) {
		this.offerid = offerid;
		this.busid = busid;
		this.offerTime = offerTime;
		this.minStartTime = minStartTime;
		this.maxStartTime = maxStartTime;
		this.quantity = quantity;
		this.minDeliverRate = minDeliverRate;
		this.maxDeliverRate = maxDeliverRate;
		this.minSourcePrice = sourcePrice;
		this.isContinuous = isContinuous;
		this.isRenewable = isRenewable;
	}
	
	public void setSupplyPlan(double supplyplan, double totalSupplyPlan) {
		this.deliverRate = supplyplan;
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
			quantitySupply += deliverRate * (PlatformController.timeInterval * 1.0 / PlatformController.min2hour);
	}
	
	
	public void print() {
		System.out.println("[ Bid id: " + offerid + ", bus id: " + busid + 
				", bidTime: " + offerTime + ", quantity: " + quantity + 
				", minTime: " + minStartTime + ", maxTime: " + 
				maxStartTime + ", sourcePrice: " + minSourcePrice + ", isContinuous: " + 
				isContinuous + ", result: " + result + ", matchPrice: " + matchPrice + 
				", quantitySupply: " + quantitySupply + ", startTime : " + startTime + 
				", deliverrate: " + deliverRate + ", total supply plan: " + totalSupplyPlan + 
				", renewable energy: " + isRenewable + "]");
	}

}

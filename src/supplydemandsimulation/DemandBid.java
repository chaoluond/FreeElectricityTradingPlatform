/**
 * 
 */
package supplydemandsimulation;

import centralmanagment.PlatformController;

/**
 * @author Chao
 *
 */
public class DemandBid {
	public int bidid; // This bid id
	public int busid; // The bus id involved
	public long bidTime; // The time when the bid was submitted to the trading platform in minutes
	public double quantity; // Electricity needed in MWh
	public long minStartTime; // Earliest electricity receiving time in minutes
	public long maxStartTime; // Latest electricity receiving time in minutes
	public long startTime;// The matched start time 
	public double maxSourcePrice; // The maximum unit price ($/MW) the bidder would like to pay to the power generator
	public double matchedPrice; // Matched source price ($/MW)
	public double deliverPrice; // The unit price ($/MW) the bidder would like to pay to the distribution company  
	
	/*isContinuous == true ---- only accept continuous stable electricity supply
	 *isContinuous == false ----- accept both continuous and discontinuous electricity supply
	 */
	public boolean isContinuous;
	
	/*
	 * result == true ----- This bid has been matched to a supplier
	 * result == false ----- This bid has not been matched to a supplier
	 */
	public boolean result = false;
	public double quantityRec = 0; // Electricity has been received.
	public double powerplan = 0; // power delivery (in MW) during each horizon. 
	
	public DemandBid(int bidid, int busid, long bidTime, long minStartTime, 
			long maxStartTime, double quantity, double maxPrice, boolean isContinuous) {
		this.bidid = bidid;
		this.busid = busid;
		this.bidTime = bidTime;
		this.minStartTime = minStartTime;
		this.maxStartTime = maxStartTime;
		this.quantity = quantity;
		this.maxSourcePrice = maxPrice;
		this.isContinuous = isContinuous;
	}
	
	
	public double getScore() {// calculate the fitness score
		// TODO by Chao, [Jun 4, 2017, 7:59:31 PM]. To implement later on
		return 100;
	}
	
	public void setDeliverPrice(double deliverPrice) {
		this.deliverPrice = deliverPrice;
	}
	
	public void setMatchPrice(double matchedPrice) {
		this.matchedPrice = matchedPrice;
	}
	
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public void setPowerPlan(double powerplan) {
		this.powerplan = powerplan;
	}
	
	public void receive() {
		if (startTime <= PlatformController.standardTime)
			quantityRec += powerplan * (PlatformController.timeInterval * 1.0 / PlatformController.min2hour);
	}
	
	
	public void print() {
		System.out.println("[ Bid id: " + bidid + ", bus id: " + busid + 
				", bidTime: " + bidTime + ", quantity: " + quantity + 
				", minTime: " + minStartTime + ", maxTime: " + 
				maxStartTime + ", sourcePrice: " + maxSourcePrice + ", isContinuous: " + 
				isContinuous + ", result: " + result + ", matchPrice: " + matchedPrice + 
				", quantityRec: " + quantityRec + ", startTime : " + startTime + 
				", powerplan: " + powerplan + "]");
	}
}


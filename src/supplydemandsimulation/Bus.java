/**
 * 
 */
package supplydemandsimulation;

import java.util.Iterator;
import java.util.Random;

import centralmanagment.PlatformController;
import flowoptimizer.SDPair;
import supplydemandmatch.SupplyDemandMatcher;

/**
 * @author Chao
 *
 */
public class Bus {
	public int busid; // the bus id used in the power network
	public int zoneid; // the zone id of the bus
	public double aggreDemand; // Aggregated demand from past
	public DemandBid currBid; // The current bid including matched or unmatched
	public double aggreSupply; // aggregated supply from the past
	public SupplyOffer currSupply; // Current supply offer
	public Random ran; // random number generator
	public int waitcount; // The number of waits for matching
	/*
	 * schedule == true ---- The bus is active during the current interval
	 * schedule == false ---- The bus is inactive during this current interval
	 */
	public boolean schedule; 
	
	public Bus(int busid, int zoneid) {
		this.busid = busid;
		this.zoneid = zoneid;
		aggreDemand = 0;
		aggreSupply = 0;
		currBid = null;
		currSupply = null;
		ran = new Random();
		waitcount = 0;
		schedule = false;
	}
	
	public void doWork() {
		ran = new Random();
		if (currBid == null && currSupply == null) { // The bus is free
			double p = ran.nextDouble();
			if (p <= PlatformController.pGenerate / 2) {// Generate a demand bid
				currBid = generateBid();
				// TODO by Chao, [Jun 4, 2017, 11:19:31 AM]. Synchronize this operation
				SupplyDemandMatcher.demanders.add(busid);
				
				//update totaldemander
				PlatformController.totalDemander++;
			}
			else if (p <= PlatformController.pGenerate) {// Generate a supply offer
				currSupply = generateOffer();
				// TODO by Chao, [Jun 4, 2017, 11:19:59 AM]. Synchronize this operation
				SupplyDemandMatcher.suppliers.add(busid);
			}
		}
		else if (currBid != null) {
			if (currBid.result) { // The current bid has a match
				if (schedule) {// The bus is active during this interval
					schedule = false;
					currBid.receive();
					
					if (currBid.currDeliverInterval >= currBid.deliverInterval) {// This delivery is finished
						aggreDemand += currBid.quantityRec;
						
						// check if this delivery is delayed
						if (PlatformController.standardTime >= currBid.endTime) {
							System.out.println("A delay delivery recorded!");
							PlatformController.totalDelaySDPair++;
						}
						
						currBid = null;
						
						// Sanity check
						if (!SupplyDemandMatcher.demandsupplypairs.containsKey(busid))
							System.out.println("demandsupplypairs hashtable error!");
						
						
						/*
						 * Clean work
						 */
						SupplyDemandMatcher.demandsupplypairs.remove(busid);
						
						// delete the corresponding SD pair from the pairqueue
						for (Iterator<SDPair> iter = SupplyDemandMatcher.pairqueue.iterator(); iter.hasNext(); ) {
							SDPair curr = iter.next();
							if (curr.demandBus == busid) {
								iter.remove();
								break;
							}
								
						}
						
					}
					
				}
			}
			else {// This demand bid does not have a match
				// check if the current bid has expired
				if (waitcount >= PlatformController.maxwait || 
						currBid.maxStartTime <= PlatformController.standardTime) {
					waitcount = 0;
					currBid = null;
					SupplyDemandMatcher.demanders.remove(busid);
					
					//update nomatcherdemander
					PlatformController.nomatchDemander++;
				}
				else {
					waitcount++;
				}
			}
		}
		else {// currSupply != null
			if (currSupply.result) { // The current supply has a match
				
				if (schedule) {// The bus is active during the current interval
					schedule = false;
					currSupply.supply();
					
					if (currSupply.currDeliverInterval >= currSupply.deliverInterval) {// This delivery is finished
						aggreSupply += currSupply.quantitySupply;
						currSupply = null;
						
						
						/*
						 * Clean work
						 */
						// Sanity check
						if (!SupplyDemandMatcher.supplydemandpairs.containsKey(busid))
							System.out.println("supplydemandpairs hashtable error");
						
						SupplyDemandMatcher.supplydemandpairs.remove(busid);
						
						// delete corresponding SD pair from pairqueue
						for (Iterator<SDPair> iter = SupplyDemandMatcher.pairqueue.iterator(); iter.hasNext(); ) {
							SDPair curr = iter.next();
							if (curr.supplyBus == busid) {
								iter.remove();
								break;
							}
						}
						
					}
					
				}
			}
			else {// This supply offer does not have a match.
				// Check if this supply offer has expired
				if (waitcount >= PlatformController.maxwait ||
						currSupply.maxStartTime <= PlatformController.standardTime) {
					waitcount = 0;
					currSupply = null;
					// TODO by Chao, [Jun 4, 2017, 11:53:09 AM]. Synchronize this operation
					SupplyDemandMatcher.suppliers.remove(busid);
				}
				else {
					waitcount++;
				}
			}
			
		}
	}
	
	
	public DemandBid generateBid() {
		int step1 = 1 + ran.nextInt(PlatformController.timeRangeBid);
		int step2 = 1 + ran.nextInt(PlatformController.timeRangeBid);
		
		int minStep = Math.min(step1, step2);
		int maxStep = Math.max(step1, step2);
		
		long minStartTime = PlatformController.standardTime + 
				minStep * PlatformController.timeInterval;
		long maxStartTime = PlatformController.standardTime + 
				maxStep * PlatformController.timeInterval;
		
		
		double quantity = ran.nextDouble() * (PlatformController.maxQuantity 
				- PlatformController.minQuantity) + PlatformController.minQuantity;
		
		int bidid = PlatformController.bidid++;
		
		boolean isContinuous = true; // Only accept continuous power delivery
		double sourcePrice = PlatformController.minSourcePriceBid + ran.nextDouble() * (PlatformController.maxSourcePriceBid - 
				PlatformController.minSourcePriceBid);
		
		// generate delivery interval
		int deliverInterval = 1 + ran.nextInt(PlatformController.powerPlanRange);
		double deliverRate = quantity / deliverInterval
				/ (PlatformController.timeInterval * 1.0 / PlatformController.min2hour);
		
		
		DemandBid db = new DemandBid(bidid, busid, PlatformController.standardTime, 
				minStartTime, maxStartTime, quantity, deliverRate, deliverInterval, sourcePrice, isContinuous);
		
		return db;
	}
	
	
	public SupplyOffer generateOffer() {
		// TODO by Chao, [Jun 4, 2017, 7:38:34 PM]. Need to edit this parameter later on!
		int step1 = 1 + ran.nextInt(PlatformController.timeRangeOffer);
		int step2 = 1 + ran.nextInt(PlatformController.timeRangeOffer);
		
		int minStep = Math.min(step1, step2);
		int maxStep = Math.max(step1, step2);
		
		long minStartTime = PlatformController.standardTime + 
				minStep * PlatformController.timeInterval;
		
		long maxStartTime = PlatformController.standardTime + 
				maxStep * PlatformController.timeInterval;
		
		
		double quantity = ran.nextDouble() * (PlatformController.maxQuantity 
				- PlatformController.minQuantity) + PlatformController.minQuantity;
		
		int offerid = PlatformController.offerid++;
		
		boolean isContinuous = true; // Supply continuous power
		boolean isRenewable = false; // Not renewable energy
		double sourcePrice = PlatformController.minSourcePriceOffer + ran.nextDouble() * 
				(PlatformController.maxSourcePriceOffer - PlatformController.minSourcePriceOffer);
		
		
		step1 = 1 + ran.nextInt(PlatformController.powerPlanRange);
		step2 = 1 + ran.nextInt(PlatformController.powerPlanRange / 2);
		minStep = Math.min(step1, step2);
		maxStep = Math.max(step1, step2);
		
		double minDeliverRate = quantity / maxStep / 
				(PlatformController.timeInterval * 1.0 / PlatformController.min2hour);
		double maxDeliverRate = quantity / minStep / 
				(PlatformController.timeInterval * 1.0 / PlatformController.min2hour);
		
		
		SupplyOffer so = new SupplyOffer(offerid, busid, PlatformController.standardTime, 
				minStartTime, maxStartTime, quantity, minDeliverRate, maxDeliverRate,
				sourcePrice, isContinuous, isRenewable);
		
		return so;
	}
	
	
	public void print() {
		System.out.println("Bus id: " + busid + "  Zone id: " + zoneid);
	}
}

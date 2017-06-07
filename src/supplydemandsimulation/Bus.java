/**
 * 
 */
package supplydemandsimulation;

import java.util.Random;

import centralmanagment.PlatformController;
import supplydemandmatch.SupplyDemandMatcher;

/**
 * @author Chao
 *
 */
public class Bus {
	public int busid; // the bus id used in the power network
	public double aggreDemand; // Aggregated demand from past
	public DemandBid currBid; // The current bid including matched or unmatched
	public double aggreSupply; // aggregated supply from the past
	public SupplyOffer currSupply; // Current supply offer
	public Random ran; // random number generator
	
	public Bus(int busid) {
		this.busid = busid;
		aggreDemand = 0;
		aggreSupply = 0;
		currBid = null;
		currSupply = null;
		ran = new Random();
	}
	
	public void doWork() {
		if (currBid == null && currSupply == null) { // The bus is free
			double p = ran.nextDouble();
			if (p <= PlatformController.pGenerate / 2) {// Generate a demand bid
				currBid = generateBid();
				// TODO by Chao, [Jun 4, 2017, 11:19:31 AM]. Synchronize this operation
				SupplyDemandMatcher.demanders.add(busid);
			}
			else if (p <= PlatformController.pGenerate) {// Generate a supply offer
				currSupply = generateOffer();
				// TODO by Chao, [Jun 4, 2017, 11:19:59 AM]. Synchronize this operation
				SupplyDemandMatcher.suppliers.add(busid);
			}
		}
		else if (currBid != null) {
			if (currBid.result) { // The current bid has a match
				if (currBid.quantityRec >= currBid.quantity) {// This delivery is finished
					aggreDemand += currBid.quantityRec;
					currBid = null;
					// TODO by Chao, [Jun 4, 2017, 11:31:56 AM]. Synchronize this operation
					if (!SupplyDemandMatcher.demandsupplypairs.containsKey(busid))
						System.out.println("demandsupplypairs hashtable error!");
					
					SupplyDemandMatcher.demandsupplypairs.remove(busid);
					SupplyDemandMatcher.haschange = true;
				}
				else {
					currBid.receive();
				}
			}
			else {
				// check if the current bid has expired
				if (currBid.maxStartTime <= PlatformController.standardTime) {
					currBid = null;
					SupplyDemandMatcher.demanders.remove(busid);
				}
			}
		}
		else {// currSupply != null
			if (currSupply.result) { // The current supply has a match
				if (currSupply.quantitySupply >= currSupply.totalSupplyPlan) {// This delivery is finished
					aggreSupply += currSupply.quantitySupply;
					currSupply = null;
					// TODO by Chao, [Jun 4, 2017, 11:48:06 AM]. Synchronize this operation
					if (!SupplyDemandMatcher.supplydemandpairs.containsKey(busid))
						System.out.println("supplydemandpairs hashtable error");
					
					SupplyDemandMatcher.supplydemandpairs.remove(busid);
					SupplyDemandMatcher.haschange = true;
					
				}
				else
					currSupply.supply();
			}
			else {
				// Check if this supply offer has expired
				if (currSupply.maxStartTime <= PlatformController.standardTime) {
					currSupply = null;
					// TODO by Chao, [Jun 4, 2017, 11:53:09 AM]. Synchronize this operation
					SupplyDemandMatcher.suppliers.remove(busid);
				}
			}
			
		}
	}
	
	
	public DemandBid generateBid() {
		int step = 1 + ran.nextInt(PlatformController.timeRangeBid);
		long startTime = PlatformController.standardTime + 
				step * PlatformController.timeInterval;
		
		
		double quantity = ran.nextDouble() * (PlatformController.maxQuantity 
				- PlatformController.minQuantity) + PlatformController.minQuantity;
		
		int bidid = -1;
		try {
			PlatformController.bididlock.acquire();
			bidid = PlatformController.bidid++;
			PlatformController.bididlock.release();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		boolean isContinuous = true; // Only accept continuous power delivery
		double sourcePrice = PlatformController.sourcePriceBid;
		
		DemandBid db = new DemandBid(bidid, busid, PlatformController.standardTime, 
				startTime, startTime, quantity, sourcePrice, isContinuous);
		
		return db;
	}
	
	
	public SupplyOffer generateOffer() {
		// TODO by Chao, [Jun 4, 2017, 7:38:34 PM]. Need to edit this parameter later on!
		int step = 1 + ran.nextInt(PlatformController.timeRangeOffer);
		long minStartTime = PlatformController.standardTime + 
				step * PlatformController.timeInterval;
		
		step = ran.nextInt(PlatformController.timeRangeOffer);
		long maxStartTime = minStartTime + step * PlatformController.timeInterval;
		
		
		double quantity = ran.nextDouble() * (PlatformController.maxQuantity 
				- PlatformController.minQuantity) + PlatformController.minQuantity;
		
		int offerid = -1;
		
		try {
			PlatformController.offeridlock.acquire();
			offerid = PlatformController.offerid++;
			PlatformController.offeridlock.release();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		boolean isContinuous = true; // Supply continuous power
		double sourcePrice = PlatformController.sourcePriceOffer;
		
		SupplyOffer so = new SupplyOffer(offerid, busid, PlatformController.standardTime, 
				minStartTime, maxStartTime, quantity, sourcePrice, isContinuous);
		
		return so;
	}
}

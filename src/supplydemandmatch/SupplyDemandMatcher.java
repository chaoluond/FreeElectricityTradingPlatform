/**
 * 
 */
package supplydemandmatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import centralmanagment.PlatformController;
import flowoptimizer.SDPair;
import powernetwork.Branch;
import powernetwork.NetworkGraph;
import powernetwork.Route;
import powernetwork.RouteUtility;
import supplydemandsimulation.Bus;

/**
 * @author Chao
 * This class is to match supply bids and demand offers. 
 *
 */
public class SupplyDemandMatcher {
	public int numBus;
	public NetworkGraph network;
	public Random ran;
	public static List<Bus> busPool;
	public static HashSet<Integer> suppliers;
	public static HashSet<Integer> demanders;
	public static HashMap<Integer, Integer> supplydemandpairs;
	public static HashMap<Integer, Integer> demandsupplypairs;
	public static HashMap<Integer, Double> congBrchCap; // congestion branch capacity
	//public static HashMap<Long, HashMap<Integer, Double>> congBFForecast; // The forecast power flow on congestion branches
	public static HashMap<Integer, Double> congBrchFlow; // The current power flow on congestion branches
	public static List<SDPair> pairqueue; // SD pairs
	
	
	 
	
	public SupplyDemandMatcher(NetworkGraph network, int numBus) {
		this.network = network;
		this.numBus = numBus;
		HashMap<Integer, int[]> bus = network.bus;
		ran = new Random();
		// Populate bus pool
		busPool = new ArrayList<>();
		busPool.add(new Bus(0, 0)); // Dummy bus, never will use it.
		for (int i = 1; i <= numBus; i++) { // Bus id starts from 1
			busPool.add(new Bus(i, bus.get(i)[1]));
		}
		
		suppliers = new HashSet<>();
		demanders = new HashSet<>();
		supplydemandpairs = new HashMap<>();
		demandsupplypairs = new HashMap<>();
		congBrchCap = new HashMap<>();
		congBrchFlow = new HashMap<>();
		
		// initialize congBrchCap
		for (int id : PlatformController.congestionBranch) {
			double cap = network.branch.get(id).capacity;
			congBrchCap.put(id, cap);
			congBrchFlow.put(id, 0.0);
		}
		
		pairqueue = new ArrayList<>();
		
	}
	
	public void matchVersion2() {
				
		for (Iterator<Integer> i = demanders.iterator(); i.hasNext(); ) {
			int demandbusid = i.next();
			Bus demandBus = busPool.get(demandbusid);
			int supplybusid = computeBestMatch(demandBus);
			if (supplybusid != -1) {
				Bus supplyBus = busPool.get(supplybusid);
				demandBus.currBid.result = true;
				supplyBus.currSupply.result = true;
				i.remove();
				suppliers.remove(supplybusid);
				supplydemandpairs.put(supplybusid, demandbusid);
				demandsupplypairs.put(demandbusid, supplybusid);
				
				
				// determine start time
				long startTime = 0;
				if (supplyBus.currSupply.minStartTime >= demandBus.currBid.minStartTime && 
						supplyBus.currSupply.minStartTime <= demandBus.currBid.maxStartTime) 
					startTime = supplyBus.currSupply.minStartTime;
				else if (demandBus.currBid.minStartTime >= supplyBus.currSupply.minStartTime && 
						demandBus.currBid.minStartTime <= supplyBus.currSupply.maxStartTime)
					startTime = demandBus.currBid.minStartTime;
				else
					System.out.println("startTime calculation error!");
				
				
				
				//determine price
				double price = supplyBus.currSupply.minSourcePrice;
				
				supplyBus.currSupply.setSupplyPlan(demandBus.currBid.deliverRate, demandBus.currBid.quantity);
				demandBus.currBid.setMatchPrice(price);
				supplyBus.currSupply.setMatchPrice(price);
				demandBus.currBid.setStartTime(startTime);
				supplyBus.currSupply.setStartTime(startTime);
				
			}
			
		}
	}
	
	
	public void matchVersion1() {
		for (Iterator<Integer> i = demanders.iterator(); i.hasNext();) {
			int demandbusid = i.next();
			Bus demandBus = busPool.get(demandbusid);
			for (Iterator<Integer> j = suppliers.iterator(); j.hasNext();) {
				int supplybusid = j.next();
				Bus supplyBus = busPool.get(supplybusid);
				if (matchHelper(demandBus, supplyBus)) {
					demandBus.currBid.result = true;
					supplyBus.currSupply.result = true;
					i.remove();
					j.remove();
					supplydemandpairs.put(supplybusid, demandbusid);
					demandsupplypairs.put(demandbusid, supplybusid);
					
					// determine start time
					long startTime = 0;
					if (supplyBus.currSupply.minStartTime >= demandBus.currBid.minStartTime && 
							supplyBus.currSupply.minStartTime <= demandBus.currBid.maxStartTime) 
						startTime = supplyBus.currSupply.minStartTime;
					else if (demandBus.currBid.minStartTime >= supplyBus.currSupply.minStartTime && 
							demandBus.currBid.minStartTime <= supplyBus.currSupply.maxStartTime)
						startTime = demandBus.currBid.minStartTime;
					else
						System.out.println("startTime calculation error!");
					
					//determine price
					double price = supplyBus.currSupply.minSourcePrice;
				
					
					supplyBus.currSupply.setSupplyPlan(demandBus.currBid.deliverRate, demandBus.currBid.quantity);
					demandBus.currBid.setMatchPrice(price);
					supplyBus.currSupply.setMatchPrice(price);
					demandBus.currBid.setStartTime(startTime);
					supplyBus.currSupply.setStartTime(startTime);
					break;
				}
			}
		}
	}
	

	
	private boolean matchHelper(Bus demandBus, Bus supplyBus) {
		
		// Check continuous / discontinuous
		if (demandBus.currBid.isContinuous != supplyBus.currSupply.isContinuous)
			return false;
		
		// Check quantity
		if (demandBus.currBid.quantity > supplyBus.currSupply.quantity)
			return false;
		
		// check price
		if (demandBus.currBid.maxSourcePrice < supplyBus.currSupply.minSourcePrice)
			return false;
		
		// check start time
		if (demandBus.currBid.maxStartTime < supplyBus.currSupply.minStartTime || 
				supplyBus.currSupply.maxStartTime < demandBus.currBid.minStartTime)
			return false;
		
		// check deliver rate
		if (demandBus.currBid.deliverRate < supplyBus.currSupply.minDeliverRate || 
				demandBus.currBid.deliverRate > supplyBus.currSupply.maxDeliverRate)
			return false;
		
		return true; // find a match
	}
	
	
	private int computeBestMatch(Bus demandBus) {
		
		double alpha1 = PlatformController.alpha1;
		double alpha2 = PlatformController.alpha2;
		double alpha3 = PlatformController.alpha3;
		double alpha4 = PlatformController.alpha4;
		double upperbound = PlatformController.maxSourcePriceBid;
		double lowerbound = PlatformController.minSourcePriceOffer;
		double[] gamma = PlatformController.gamma;
		int maxQuantity = PlatformController.maxQuantity;
		double threshold1 = PlatformController.threshold1;
		double threshold2 = PlatformController.threshold2;
		double maxSourcePriceGap = upperbound - lowerbound;
		double maxDeliverCost = gamma[2] * (maxQuantity / 
				(PlatformController.timeInterval * 1.0 / PlatformController.min2hour));
		
		
		
		// Filter out unqualified suppliers
		List<SupplyBusScore> list = new ArrayList<>();
		boolean ignore = false;
		boolean maxgamma = false;
		double delrate = demandBus.currBid.deliverRate;
		for (int j : suppliers) {
			ignore = false;
			maxgamma = false;
			Bus supplyBus = busPool.get(j);
			if (matchHelper(demandBus, supplyBus)) {
				double score = 0;
				// Compute source price gap component
				score += alpha1 * ((demandBus.currBid.maxSourcePrice - supplyBus.currSupply.minSourcePrice)
						/maxSourcePriceGap);
				
				// Compute deliver cost component
				double mgamma = 1;
				List<Route> routes = RouteUtility.findAllRoutes(network, supplyBus.busid, 
						demandBus.busid, PlatformController.maxRoute);
				
				for (Route rou : routes) {
					for (Branch bran : rou.route) {
						int branid = bran.id;
						if (PlatformController.congBrchSet.contains(branid)) {
							// There is a problem to compute aggregated congBrchFlow???
							double total = delrate + congBrchFlow.get(branid);
							if (total >= threshold1 * congBrchCap.get(branid) && 
									total < threshold2 * congBrchCap.get(branid)) {
								System.out.println("Use congestion branch id: " + branid + ". Exceed 80% capacity");
								mgamma = gamma[1];
							}
							else if (total >= threshold2 * congBrchCap.get(branid) && 
									total < congBrchCap.get(branid)) {
								System.out.println("Use congestion branch id: " + branid + ". Exceed 90% capacity");
								mgamma = gamma[2];
								maxgamma = true;
							}
							else if (total >= congBrchCap.get(branid)) {
								System.out.println("Use congestion branch id: " + branid + ". Exceed 100% capacity. "
										+ "Ignore this pair.");
								ignore = true;
							}
						}
						
						if (maxgamma || ignore)
							break;
						
					}
					
					if (maxgamma || ignore)
						break;
				}
				
				if (ignore) {
					System.out.println("Exceed capacity! Ignore this pair!");
					continue;
				}
				
				
				
				
				score += alpha2 * (mgamma * demandBus.currBid.deliverRate / maxDeliverCost);
				
				
				// Compute inter-zone and intra-zone cost
				if (demandBus.zoneid == supplyBus.zoneid)
					score += alpha3;
				
				
				// Compute renewable energy incentive
				if (supplyBus.currSupply.isRenewable)
					score += alpha4;
				
				
				SupplyBusScore busscore = new SupplyBusScore(j, score, routes);
				list.add(busscore);
			}
		}
		
		if (list.isEmpty())
			return -1;
		else {
			Collections.sort(list, new Comparator<SupplyBusScore>() {// sort entries in descending order

				@Override
				public int compare(SupplyBusScore bus1, SupplyBusScore bus2) {
					if (bus1.score < bus2.score)
						return 1;
					else if (bus1.score > bus2.score)
						return -1;
					else
						return 0;
				}
				
			});
			
			/*System.out.println("New ranking result: ");
			for (SupplyBusScore sbus : list)
				sbus.print();
			*/
			
			// We have a match. Add this SD pair to list
			SupplyBusScore winner = list.get(0);
			
			SDPair temp = new SDPair(winner.busid, demandBus.busid, delrate, winner.routes);
			pairqueue.add(temp);
			
			
			// update congbrchflow
			for (Route rou : winner.routes) {
				List<Branch> branches = rou.route;
				for (Branch bran : branches) {
					int branid = bran.id;
					if (congBrchFlow.containsKey(branid))
						congBrchFlow.put(branid, congBrchFlow.get(branid) + delrate);
				}
			}
			
			return list.get(0).busid;
		}
		
	}
	

	public List<SDPair> computeCurrSDPairs() {// generate the SD pairs for the current interval
		List<SDPair> result = new ArrayList<>();
		for (SDPair pair : pairqueue) {
			int demandBusId = pair.demandBus;
			Bus demandBus = busPool.get(demandBusId);
			if (demandBus.currBid.startTime <= PlatformController.standardTime)
				result.add(pair);
		}
		
		return result;
	}
	
	class SupplyBusScore {
		public int busid;
		public double score;
		public List<Route> routes;
		
		public SupplyBusScore(int busid, double score, List<Route> routes) {
			this.busid = busid;
			this.score = score;
			this.routes = routes; 
		}
		
		public void print() {
			System.out.println("bus id: " + busid + ", score is " + score);
			/*System.out.println("Routes are: ");
			for (Route r : routes)
				r.print();*/
		}
	}
	
	
}

/**
 * 
 */
package supplydemandmatch;

import java.util.ArrayList;
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
	public static boolean haschange;
	
	public double[] zonedemand;
	public double[] zonesupply;
	public double[] zonecap;
	
	 
	
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
		haschange = false;
		zonedemand = new double[PlatformController.zoneNum];
		zonesupply = new double[PlatformController.zoneNum];
		zonecap = new double[PlatformController.zoneNum];
		
		// intialize zonecap
		for (Branch brch : network.branch) {
			Bus fromBus = busPool.get(brch.bus1);
			Bus toBus = busPool.get(brch.bus2);
			
			if (fromBus.zoneid != toBus.zoneid) {
				zonecap[fromBus.zoneid - 1] += brch.capacity / 2.0;
				zonecap[toBus.zoneid - 1] += brch.capacity / 2.0;
			}
		}
		
	}
	
	public void matchVersion2() {
		updateZoneFlow();
		
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
				haschange = true;
				
				
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
					haschange = true;
					
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
	
	
	public List<SDPair> generateSDPairs() {
		
		List<SDPair> pairs = new ArrayList<>();
		for (int supplyBus : supplydemandpairs.keySet()) {
			int demandBus = supplydemandpairs.get(supplyBus);
			List<Route> routes = RouteUtility.findAllRoutes(network, supplyBus, 
					demandBus, PlatformController.maxRoute);
			double delrate = busPool.get(demandBus).currBid.deliverRate;
			SDPair pair = new SDPair(supplyBus, demandBus, delrate, routes);
			pairs.add(pair);
		}
		
		return pairs;
	}
	
	private void updateZoneFlow() {
		zonedemand = new double[PlatformController.zoneNum];
		zonesupply = new double[PlatformController.zoneNum];
		
		for (int supplykey : supplydemandpairs.keySet()) {
			int demandkey = supplydemandpairs.get(supplykey);
			Bus supplyBus = busPool.get(supplykey);
			Bus demandBus = busPool.get(demandkey);
			if (demandBus.zoneid != supplyBus.zoneid) {
				zonedemand[demandBus.zoneid - 1] += demandBus.currBid.deliverRate;
				zonesupply[supplyBus.zoneid - 1] += supplyBus.currSupply.deliverRate;
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
		double[] gammaIn = PlatformController.gammaIn;
		double[] gammaOut = PlatformController.gammaOut;
		int maxQuantity = PlatformController.maxQuantity;
		double threshold1 = PlatformController.threshold1;
		double threshold2 = PlatformController.threshold2;
		double maxSourcePriceGap = upperbound - lowerbound;
		double maxDeliverCost = gammaIn[2] * gammaOut[2] * (maxQuantity / 
				(PlatformController.timeInterval * 1.0 / PlatformController.min2hour));
		
		
		
		// Filter out unqualified suppliers
		List<SupplyBusScore> list = new ArrayList<>();
		for (int j : suppliers) {
			Bus supplyBus = busPool.get(j);
			if (matchHelper(demandBus, supplyBus)) {
				double score = 0;
				// Compute source price gap component
				score += alpha1 * ((demandBus.currBid.maxSourcePrice - supplyBus.currSupply.minSourcePrice)
						/maxSourcePriceGap);
				
				// Compute deliver cost component
				double mgammaIn = 1;
				double mgammaOut = 1;
				
				double delrate = demandBus.currBid.deliverRate;
				if (demandBus.zoneid != supplyBus.zoneid) {
					double totalDed = delrate + zonedemand[demandBus.zoneid - 1];
					double totalSup = delrate + zonesupply[supplyBus.zoneid - 1];
					if (totalDed >= threshold1 * zonecap[demandBus.zoneid - 1] && 
							totalDed < threshold2 * zonecap[demandBus.zoneid - 1]) {
						System.out.println("Zone id " + demandBus.zoneid + ", demand exceeds 80% capacity!");
						mgammaIn = gammaIn[1];
					}
					else if (totalDed >= threshold2 * zonecap[demandBus.zoneid - 1]) {
						System.out.println("Zone id " + demandBus.zoneid + ", demand exceeds 90% capacity!");
						mgammaIn = gammaIn[2];
					}
					
					
					if (totalSup >= threshold1 * zonecap[supplyBus.zoneid - 1] && 
							totalSup < threshold2 * zonecap[supplyBus.zoneid - 1]) {
						System.out.println("Zone id " + supplyBus.zoneid + ", supply exceeds 80% capacity!");
						mgammaOut = gammaOut[1];
					}
					else if (totalSup >= threshold2 * zonecap[supplyBus.zoneid - 1]) {
						System.out.println("Zone id " + supplyBus.zoneid + ", supply exceeds 90% capacity!");
						mgammaOut = gammaOut[2];
					}
						
				}
				
				score += alpha2 * (mgammaIn * mgammaOut * demandBus.currBid.deliverRate / maxDeliverCost);
				
				
				// Compute inter-zone and intra-zone cost
				if (demandBus.zoneid == supplyBus.zoneid)
					score += alpha3;
				
				
				// Compute renewable energy incentive
				if (supplyBus.currSupply.isRenewable)
					score += alpha4;
				
				
				SupplyBusScore busscore = new SupplyBusScore(j, score);
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
			
			for (SupplyBusScore sbus : list)
				sbus.print();
			
			return list.get(0).busid;
		}
		
	}
	

	class SupplyBusScore {
		public int busid;
		public double score;
		
		public SupplyBusScore(int busid, double score) {
			this.busid = busid;
			this.score = score;
		}
		
		public void print() {
			System.out.println("bus id: " + busid + ", score is " + score);
		}
	}
	
	
}

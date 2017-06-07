/**
 * 
 */
package supplydemandmatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import centralmanagment.PlatformController;
import flowoptimizer.SDPair;
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
	
	public SupplyDemandMatcher(NetworkGraph network, int numBus) {
		this.network = network;
		this.numBus = numBus;
		ran = new Random();
		// Populate bus pool
		busPool = new ArrayList<>();
		busPool.add(new Bus(0)); // Dummy bus, never will use it.
		for (int i = 1; i <= numBus; i++) { // Bus id starts from 1
			busPool.add(new Bus(i));
		}
		
		suppliers = new HashSet<>();
		demanders = new HashSet<>();
		supplydemandpairs = new HashMap<>();
		demandsupplypairs = new HashMap<>();
		haschange = false;
	}
	
	public void match() {
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
					
					//determine price and power delivery plan
					double price = supplyBus.currSupply.minSourcePrice;
					int step = 1 + ran.nextInt(PlatformController.powerPlanRange);
					double powerplan = demandBus.currBid.quantity / step 
							/ (PlatformController.timeInterval * 1.0 / PlatformController.min2hour);
					
					demandBus.currBid.setPowerPlan(powerplan);
					supplyBus.currSupply.setSupplyPlan(powerplan, demandBus.currBid.quantity);
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
			double power = busPool.get(supplyBus).currSupply.supplyPlan;
			SDPair pair = new SDPair(supplyBus, demandBus, power, routes);
			pairs.add(pair);
		}
		
		return pairs;
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
		
		return true; // find a match
		
		
	}
	
	
}

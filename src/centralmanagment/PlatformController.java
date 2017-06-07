/**
 * 
 */
package centralmanagment;
import java.util.List;
import java.util.concurrent.Semaphore;

import flowoptimizer.FlowOptimizer;
import flowoptimizer.SDPair;
import powernetwork.NetworkGraph;
import supplydemandmatch.SupplyDemandMatcher;



/**
 * @author Chao
 * This class is the central controller for the entire platform.
 */
public class PlatformController {

	/**
	 * @param args
	 */
	
	public static final long min2hour = 60; // One hour is equal to 60 minutes
	public static int powerPlanRange = 10; // The range for power plan 
	public static long standardTime = 0; // in minute
	public static long timeInterval = 15; // in minute
	public static int numInterval = 20; // The number of intervals
	public static double pGenerate = 0.5; // the probability that this bus will generate a new bid or offer is 90%
	public static int timeRangeBid = 10; // the start time range used in Demand bid generation
	public static int timeRangeOffer = 5; // the start time range used in supply offer generation
	public static int minQuantity = 5; // Min electricity demand is 20 MWh
	public static int maxQuantity = 10; // Max electricity demand is 200 MWh
	public static int bidid = 0; // global bid id counter
	public static int offerid = 0; // global offer id counter
	public static Semaphore bididlock = new Semaphore(1); // lock for bids
	public static Semaphore offeridlock = new Semaphore(1); // lock for offer;
	public static double maxSourcePriceBid = 10; // Max source price for bid 
	public static double minSourcePriceBid = 5; // Min source price for bid 
	public static double maxSourcePriceOffer = 10; // Max source price for offer
	public static double minSourcePriceOffer = 5; // Min source price for offer
	public static double deliverPrice = 10; // 
	public static int maxRoute = 1; // The max number of routes returned
	
	public NetworkGraph network;
	public SupplyDemandMatcher matcher;
	public FlowOptimizer foper;
	
	public PlatformController() {
		network = new NetworkGraph();
		int numBus = network.bus.size();
		matcher = new SupplyDemandMatcher(network, numBus);
	}
	
	
	public void run() {
		for (int step = 0; step < numInterval; step++) {
			System.out.println("Do match here!");
			matcher.match();
			
			System.out.println("Generate SD pairs for optimizaiton");
			List<SDPair> pairs = matcher.generateSDPairs();
			
			System.out.println("Number of pairs " + pairs.size());
			System.out.println("Solve optimization problem");
			if (pairs.isEmpty())
				System.out.println("No matched pairs! Do not invoke optimization routin.");
			else if (!SupplyDemandMatcher.haschange) {
				System.out.println("Matched pairs do not change. Do not invoke optimization routin.");
			}
			else {
				SupplyDemandMatcher.haschange = false;
				foper = new FlowOptimizer(network, pairs);
				foper.solve();
			}
			
			System.out.println("Update each bus");
			for (int i = 1; i <= network.bus.size(); i++)
				SupplyDemandMatcher.busPool.get(i).doWork();
			
			System.out.println("Update model standard time");
			PlatformController.standardTime += PlatformController.timeInterval;
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PlatformController pc = new PlatformController();
		pc.run();
	}

}

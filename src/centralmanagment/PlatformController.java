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
import unittest.WriteToFile;



/**
 * @author Chao
 * This class is the central controller for the entire platform.
 */
public class PlatformController {

	/**
	 * @param args
	 */
	
	public static final long min2hour = 60; // One hour is equal to 60 minutes
	public static int powerPlanRange = 6; // The range for power plan 
	public static long standardTime = 0; // in minute
	public static long timeInterval = 15; // in minute
	public static int numInterval = 2880; // The number of intervals
	public static double pGenerate = 0.2; // the probability that this bus will generate a new bid or offer is 90%
	public static int timeRangeBid = 10; // the start time range used in Demand bid generation
	public static int timeRangeOffer = 5; // the start time range used in supply offer generation
	public static int minQuantity = 10; // Min electricity demand is 20 MWh
	public static int maxQuantity = 30; // Max electricity demand is 200 MWh
	public static int bidid = 0; // global bid id counter
	public static int offerid = 0; // global offer id counter
	public static Semaphore bididlock = new Semaphore(1); // lock for bids
	public static Semaphore offeridlock = new Semaphore(1); // lock for offer;
	public static double maxSourcePriceBid = 10; // Max source price for bid 
	public static double minSourcePriceBid = 5; // Min source price for bid 
	public static double maxSourcePriceOffer = 10; // Max source price for offer
	public static double minSourcePriceOffer = 5; // Min source price for offer
	public static double deliverPrice = 10; // 
	public static int zoneNum = 3; // Number of zones
	public static int maxRoute = 2; // The max number of routes returned
	
	/*
	 * Parameters for ranking algorithm
	 */
	public static double alpha1 = 0.25; // Parameter for source price gap
	public static double alpha2 = 0.25; // Parameter for deliver price
	public static double alpha3 = 0.25; // Parameter for zone
	public static double alpha4 = 0.25; // Parameter for renewable energy
	public static double[] gammaIn = new double[]{1, 1.5, 2}; // Congestion penalty for incoming demand
	public static double[] gammaOut = new double[]{1, 1.5, 2}; // congestion penalty for outgoing supply
	public static double threshold1 = 0.8; // The threshold for incoming demand or outgoing supply capacity penatly
	public static double threshold2 = 0.9; // The threshold for ....
	
	public NetworkGraph network;
	public SupplyDemandMatcher matcher;
	public FlowOptimizer foper;
	
	public PlatformController() {
		network = new NetworkGraph();
		int numBus = network.bus.size();
		matcher = new SupplyDemandMatcher(network, numBus);
	}
	
	
	public void run() {
		double plsum = 0;
		int totalHops = 0;
		int countfeasible = 0;
		for (int step = 0; step < numInterval; step++) {
			System.out.println("Iteration #: " + step);
			System.out.println("Do match here!");
			matcher.matchVersion2();
			
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
				boolean result = foper.solve();
				if (result) { 
					plsum += foper.computePowerLoss();
					totalHops += foper.numHops;
					countfeasible++;
				}
			}
			
			System.out.println("Update each bus");
			for (int i = 1; i <= network.bus.size(); i++)
				SupplyDemandMatcher.busPool.get(i).doWork();
			
			System.out.println("Update model standard time");
			PlatformController.standardTime += PlatformController.timeInterval;
		}
		
		double avgpowloss = plsum / countfeasible;
		double avgnumhop = totalHops * 1.0 / countfeasible;
		
		double[][] matrix = new double[2][1];
		matrix[0][0] = avgpowloss;
		matrix[1][0] = avgnumhop;
		
		//WriteToFile.write2File(matrix, "result_maxroute6.txt");
		System.out.println("Average power loss is " + plsum / countfeasible);
		System.out.println("Average number of hops is " + totalHops * 1.0 / countfeasible);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PlatformController pc = new PlatformController();
		pc.run();
	}

}

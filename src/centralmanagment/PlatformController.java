/**
 * 
 */
package centralmanagment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Semaphore;

import flowoptimizer.FlowOptimizer;
import flowoptimizer.SDPair;
import powernetwork.Branch;
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
	public static int powerPlanRange = 20; // The range for power plan 
	public static long standardTime = 0; // in minute
	public static long timeInterval = 15; // in minute
	public static int numInterval = 30; // The number of intervals
	public static double pGenerate = 0.8; // the probability that this bus will generate a new bid or offer is 90%
	public static int timeRangeBid = 5; // the start time range used in Demand bid generation
	public static int timeRangeOffer = 2; // the start time range used in supply offer generation
	public static int minQuantity = 40; // Min electricity demand is 20 MWh
	public static int maxQuantity = 70; // Max electricity demand is 200 MWh
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
	public static int[] congestionBranch = {237, 156, 236, 303, 315, 211, 255, 257, 210, 215}; // The congestion branch id
	public static HashSet<Integer> congBrchSet;
	
	/*
	 * Parameters for ranking algorithm
	 */
	public static double alpha1 = 0; // Parameter for source price gap
	public static double alpha2 = 1; // Parameter for deliver price
	public static double alpha3 = 0; // Parameter for zone
	public static double alpha4 = 0; // Parameter for renewable energy
	public static double[] gamma = new double[]{1, 1.5, 2}; // Congestion penalty parameter
	public static double threshold1 = 0.7; // The threshold for incoming demand or outgoing supply capacity penatly
	public static double threshold2 = 0.9; // The threshold for ....
	
	public NetworkGraph network;
	public SupplyDemandMatcher matcher;
	public FlowOptimizer foper;
	
	public PlatformController() {
		network = new NetworkGraph();
		int numBus = network.bus.size();
		matcher = new SupplyDemandMatcher(network, numBus);
		congBrchSet = new HashSet<>();
		for (int i : congestionBranch)
			congBrchSet.add(i);
		
	}
	
	
	public void run() {
		for (int step = 0; step < numInterval; step++) {
			System.out.println("Iteration #: " + step);
			//System.out.println("Do match here!");
			matcher.matchVersion2();
			
			//System.out.println("Generate SD pairs for optimization");
			List<SDPair> pairs = matcher.computeCurrSDPairs();
						
			System.out.println("Number of pairs " + pairs.size());
			//System.out.println("Solve optimization problem");
			
			foper = new FlowOptimizer(network, pairs);
			boolean result = false;
			if (pairs.isEmpty())
				System.out.println("No matched pairs! Do not invoke optimization routin.");
			else 
				result = foper.solve();
			
			//System.out.println("Update each bus");
			for (int i = 1; i <= network.bus.size(); i++)
				SupplyDemandMatcher.busPool.get(i).doWork();
			
			//System.out.println("Update model standard time");
			PlatformController.standardTime += PlatformController.timeInterval;
			
			System.out.println("Update congestion branch flow for next interval");
			if (result) {
				SupplyDemandMatcher.congBrchFlow = foper.computeCongBrchFlow();
			}
		}
		
		/*double avgpowloss = plsum / countfeasible;
		double avgnumhop = totalHops * 1.0 / countfeasible;
		
		double[][] matrix = new double[2][1];
		matrix[0][0] = avgpowloss;
		matrix[1][0] = avgnumhop;
		
		//WriteToFile.write2File(matrix, "result_maxroute6.txt");
		System.out.println("Average power loss is " + plsum / countfeasible);
		System.out.println("Average number of hops is " + totalHops * 1.0 / countfeasible);*/
		
		/*for (Branch br : network.branch) {
			br.computeRatio();
		}
		
		Collections.sort(network.branch, new Comparator<Branch>() {
			@Override
			public int compare(Branch b1, Branch b2) {
				return b1.flow2capratio < b2.flow2capratio ? 1 : -1;
			}
		});
		
		List<String> content = new ArrayList<>();
		for (Branch br : network.branch)
			content.add(br.toString());
		*/
		//WriteToFile.write2File(content, "branchflow_lowpower_longtime.txt");
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PlatformController pc = new PlatformController();
		pc.run();
	}

}

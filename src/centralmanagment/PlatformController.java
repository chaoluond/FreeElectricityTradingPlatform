/**
 * 
 */
package centralmanagment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import flowoptimizer.FlowOptimizer;
import flowoptimizer.SDPair;
import powernetwork.Branch;
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
	
	public static int numBus = 118; // The number of buses for simulations
	public static final long min2hour = 60; // One hour is equal to 60 minutes
	public static int powerPlanRange = 5; // The range for power plan 
	public static long standardTime = 0; // in minute
	public static long timeInterval = 15; // in minute
	public static int numInterval = 20000; // The number of intervals
	public static double pGenerate = 1.0; // the probability that this bus will generate a new bid or offer is 90%
	public static int timeRangeBid = 10; // the start time range used in Demand bid generation
	public static int timeRangeOffer = 10; // the start time range used in supply offer generation
	public static int minQuantity = 20; // Min electricity demand is 20 MWh
	public static int maxQuantity = 45; // Max electricity demand is 200 MWh
	public static int bidid = 0; // global bid id counter
	public static int offerid = 0; // global offer id counter
	public static double maxSourcePriceBid = 10; // Max source price for bid 
	public static double minSourcePriceBid = 5; // Min source price for bid 
	public static double maxSourcePriceOffer = 10; // Max source price for offer
	public static double minSourcePriceOffer = 5; // Min source price for offer
	public static double deliverPrice = 10; // 
	public static int zoneNum = 3; // Number of zones
	public static int maxRoute = 2; // The max number of routes returned
	public static HashMap<Integer, Double> congBrchCapacity; // The capacity of congested branches
	public static int numCongBrch = 20; // The number of congestion branches we need to consider 
	public static int maxwait = 2; // The max number of waits for matching
	public static double feedbackPara = 10; // The parameter for multiplier in supplydemandmatcher
	
	public static double throughput = 0; // record the throughput in WM
	public static double powerloss = 0; // record the total energy loss in WM
	public static double utilization = 0; // record the total utilization in WM
	public static int congcount = 0; // record number of congestions
	public static int totalAccSDPair = 0; // Total number of accepted SD pairs
	public static int totalDelaySDPair = 0; // Total number of delayed SD pairs
	public static double averagePrice = 0; // The average purchase price in $/MWh
	public static double averagePriceGap = 0; // The average purchase price saving in $/MWh
	public static int totalDemander = 0; // Total number of demanders
	public static int nomatchDemander = 0; // The number of demanders which do not have a match.
	
	/*
	 * Parameters for ranking algorithm
	 */
	public static double alpha1 = 0.2; // Parameter for source price gap
	public static double alpha2 = -0.4; // Parameter for deliver price
	public static double alpha3 = 0.2; // Parameter for zone
	public static double alpha4 = 0.2; // Parameter for renewable energy
	public static double[] gamma = new double[]{1, 2, 4, 8}; // Congestion penalty parameter
	public static double threshold1 = 0.5; // The threshold for incoming demand or outgoing supply capacity penatly
	public static double threshold2 = 0.75; // The threshold for ....
	public static double threshold3 = 0.9; // The threshold for
	public static double currCongProb = 0; // Current congestion probability
	public static long seed = 0; // The seed for Random
	
	public static List<Branch> branchListOrdered = null; // The branchlist to be sorted
	
	public NetworkGraph network;
	public SupplyDemandMatcher matcher;
	public FlowOptimizer foper;
	
	public PlatformController() {
		network = new NetworkGraph();
		int numBus = network.bus.size();
		//numCongBrch = network.branch.size();
		matcher = new SupplyDemandMatcher(network, numBus);
		congBrchCapacity = new HashMap<>();
		// branchListOrdered is a shallow copy of network.branch
		branchListOrdered = new ArrayList<>(network.branch);
		
	}
	
	
	public void run() {
		
		for (int step = 0; step < numInterval; step++) {
			currCongProb = congcount * 1.0 / (step + 1);
			System.out.println("Congestion probability is: " + currCongProb);
			
			
			System.out.println("Iteration #: " + step);
			//System.out.println("Do match here!");
			
			matcher.matchVersion3();
			
			System.out.println("Number of SD pairs: " + SupplyDemandMatcher.pairqueue.size());
			
			//System.out.println("Generate SD pairs for optimization");
			List<SDPair> pairs = matcher.computeCurrSDPairs();
						
			System.out.println("Number of pairs " + pairs.size());
			
			
			//System.out.println("Solve optimization problem");	
			boolean result = false;
			if (pairs.isEmpty())
				System.out.println("No matched pairs! Do not invoke optimization routine.");
			else { 
				List<SDPair> templist = new ArrayList<>(pairs);
				foper = new FlowOptimizer(network, pairs);
				result = foper.solve();
				
				if (!result)
					congcount++;
				
				while (!result && !pairs.isEmpty()) {
					pairs.remove(pairs.size() - 1); // Remove the last SD pair
					foper = new FlowOptimizer(network, pairs);
					result = foper.solve();
				}
				
				if (result) {
					System.out.println("Final number of SD pair: " + pairs.size());
					powerloss += foper.computePowerLoss();
					foper.computeBranchFlow();
					for (SDPair p : pairs) {
						int demandbusid = p.demandBus;
						int supplybusid = p.supplyBus;
						SupplyDemandMatcher.busPool.get(demandbusid).schedule = true;
						SupplyDemandMatcher.busPool.get(supplybusid).schedule = true;
					}
				}
				else {
					System.out.println("Fatal error happens here!!!!!!!");
					for (SDPair sdpair : templist) {
						sdpair.print();
					}
				}
			}
			
			//System.out.println("Update active bus");
			for (int i = 1; i < SupplyDemandMatcher.busPool.size(); i++) {
				SupplyDemandMatcher.busPool.get(i).doWork();
			}
			
			
			//System.out.println("Update model standard time");
			standardTime += timeInterval;
			
			
			
			//update congestion branches
			Collections.sort(branchListOrdered, new Comparator<Branch>() {
				@Override
				public int compare(Branch b1, Branch b2) {
					if (b1.flow2capratio < b2.flow2capratio)
						return 1;
					else if (b1.flow2capratio > b2.flow2capratio)
						return -1;
					else
						return 0;
				}
			});
			
			congBrchCapacity.clear();
			for (int num = 0; num < numCongBrch; num++) {
				int cbid = branchListOrdered.get(num).id;
				double capp = branchListOrdered.get(num).capacity;
				congBrchCapacity.put(cbid, capp);
			}
			
			
		}
		
		System.out.println("Iteration number: " + numInterval);
		System.out.println("Genration probability: " + pGenerate);
		System.out.println("New matching algorithm used.");
		System.out.println("Max route number is " + maxRoute);
		System.out.println("Average delivery delay probability " + totalDelaySDPair * 1.0 / totalAccSDPair);
		System.out.println("Average throughput: " + throughput / numInterval);
		System.out.println("Average powerloss: " + powerloss / numInterval);
		System.out.println("Power loss ratio: " + powerloss / (throughput * 1E6));
		System.out.println("Average congestion probability: " + congcount * 1.0 / numInterval);
		System.out.println("Average price: " + averagePrice / totalAccSDPair);
		System.out.println("Average price gap: " + averagePriceGap / totalAccSDPair);
		System.out.println("Average demandAd matching probability: " + (1 - nomatchDemander * 1.0 / totalDemander));
		
		
		
		/*double avgpowloss = plsum / countfeasible;
		double avgnumhop = totalHops * 1.0 / countfeasible;
		
		double[][] matrix = new double[2][1];
		matrix[0][0] = avgpowloss;
		matrix[1][0] = avgnumhop;
		
		//WriteToFile.write2File(matrix, "result_maxroute6.txt");
		System.out.println("Average power loss is " + plsum / countfeasible);
		System.out.println("Average number of hops is " + totalHops * 1.0 / countfeasible);*/
		
		/*List<String> content = new ArrayList<>();
		for (Branch br : network.branch)
			content.add(br.toString());
		
		WriteToFile.write2File(content, "branchflowpower.txt");*/
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PlatformController pc = new PlatformController();
		pc.run();
	}

}

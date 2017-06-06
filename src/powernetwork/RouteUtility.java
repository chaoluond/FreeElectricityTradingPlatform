/**
 * 
 */
package powernetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Chao
 * This class contains utility functions to deal with routes.
 *
 */
public class RouteUtility {
	
	/**
	 * 
	 * @param graph: power network
	 * @param origin: start bus index
	 * @param destination: end bus indes
	 * @param max: the maximum number of routes will be returned
	 * @return
	 */
	
	public static List<Route> findAllRoutes(NetworkGraph network, int origin, 
			int destination, int max) {
		List<Route> result = new ArrayList<>();
		
		// Use a queue for BFS
		LinkedList<Integer> queue = new LinkedList<>();
		// There may be multiple shortest pathes from origin to the current bus
		HashMap<Integer, List<List<Branch>>> path = new HashMap<>(); 
		
		queue.add(origin);
		List<List<Branch>> outlist = new ArrayList<>();
		List<Branch> inlist = new ArrayList<>();
		outlist.add(inlist);
		path.put(origin, outlist);
		
		while (!queue.isEmpty()) {
			int prevBus = queue.poll();
			List<List<Branch>> prevoutList = path.get(prevBus);
			//System.out.println("PrevBus is " + prevBus);
			for (int currBus : network.graph.get(prevBus).keySet()) {
				if (currBus == destination) {
					List<Branch> candidates = network.graph.get(prevBus).get(currBus);
					for (Branch br : candidates) {
						for (List<Branch> previnList : prevoutList) {
							List<Branch> route = new ArrayList<>(previnList);
							route.add(br);
							Route r = new Route(route);
							result.add(r);
							max--;
							if (max == 0)
								return result;
						}
					}
				}
				else if (!path.containsKey(currBus)) {
					path.put(currBus, new ArrayList<>());
					List<Branch> candidates = network.graph.get(prevBus).get(currBus);
					for (Branch br : candidates) {
						for (List<Branch> previnList : prevoutList) {
							List<Branch> route = new ArrayList<>(previnList);
							route.add(br);
							path.get(currBus).add(route);
						}
					}
					
					queue.add(currBus);
				}
			}
		}
		
		return result;
	}
}

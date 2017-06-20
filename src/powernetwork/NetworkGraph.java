/**
 * 
 */
package powernetwork;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Chao
 * Throughout the power network, we use "MW" for power, "ohme" for resistance, "kV"
 * for voltage.
 *
 */
public class NetworkGraph {
	public HashMap<Integer, HashMap<Integer, List<Branch>>> graph = new HashMap<>();
	public HashMap<Integer, int[]> bus = new HashMap<>(); // (bus, [basevotage, zoneid])
	public List<Branch> branch = new ArrayList<>(); // A complete list of branches
	public double[] capacities = {200, 500}; // capacity in MW for 138kV and 345kV transmission lines
	public double baseMVA = 100; // baseMVA = 100 MVA
	public int numBranch = 0; // Number of branches
	
	
	
	public NetworkGraph(){
		BufferedReader reader = null;
		List<List<Double>> bch = new ArrayList<>();
		String s = "";
		int id = 0;
		
		// Read bux.txt and branch.txt into arraylists
		try {
			reader = new BufferedReader(new FileReader("bus.txt"));
			while ((s = reader.readLine()) != null) {
				String[] value = s.split("\\s+");
				int[] feature = new int[]{Integer.parseInt(value[1]), Integer.parseInt(value[2])};
				bus.put(Integer.parseInt(value[0]), feature);
			}
			
			reader.close();
			
			reader = new BufferedReader(new FileReader("branch.txt"));
			
			while ((s = reader.readLine()) != null) {
				String[] value = s.split("\\s+");
				ArrayList<Double> list = new ArrayList<Double>();
				list.add(Double.parseDouble(value[0]));
				list.add(Double.parseDouble(value[1]));
				list.add(Double.parseDouble(value[2]));
				bch.add(list);
			}
			reader.close();
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		numBranch = 2 * bch.size();
		// Create a hashmap to represent the graph
		for (List<Double> link : bch) {
			int bus1 = link.get(0).intValue();
			int bus2 = link.get(1).intValue();
			double resistancePU = link.get(2);
			double resistance = 0;
			double capacity = 0;
			int vol1 = bus.get(bus1)[0];
			int vol2 = bus.get(bus2)[0];
			
			
			if (vol1 != vol2 && resistancePU > 0) {
				System.out.println("error!");
				System.out.println(bus1);
				System.out.println(bus2);
				System.out.println(resistancePU);
			}
			
			if (vol1 == vol2) {
				resistance = vol1 * vol1 * 1.0 / baseMVA * resistancePU; 
			}
			
			capacity = vol1 == 138 ? capacities[0] : capacities[1];
			
			Branch br1to2 = new Branch(bus1, bus2, vol1, vol2, resistance, capacity, id++);
			Branch br2to1 = new Branch(bus2, bus1, vol2, vol1, resistance, capacity, id++);
			
			branch.add(br1to2);
			branch.add(br2to1);
			
			
			// add the branch to (bus1, bus2) pair
			if (!graph.containsKey(bus1))
				graph.put(bus1, new HashMap<Integer, List<Branch>>());
			
			if (!graph.get(bus1).containsKey(bus2))
				graph.get(bus1).put(bus2, new ArrayList<Branch>());
			
			graph.get(bus1).get(bus2).add(br1to2);
			
			
			
			
			// add the branch to (bus2, bus1) pair
			if (!graph.containsKey(bus2))
				graph.put(bus2, new HashMap<Integer, List<Branch>>());
			
			if (!graph.get(bus2).containsKey(bus1))
				graph.get(bus2).put(bus1, new ArrayList<Branch>());
			
			graph.get(bus2).get(bus1).add(br2to1);

		}
		
		
	}
	
}

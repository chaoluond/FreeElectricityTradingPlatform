/**
 * 
 */
package powernetwork;

/**
 * @author Chao
 *
 */
public class Branch {
	
	// flow direction from Bus 1 ----> Bus 2
	public int bus1; // Bus 1
	public int bus2; // Bus 2
	public double volbase1; // Voltage for bus 1 in kV
	public double volbase2; // Voltage for bus 2 in kV
	public double resistance; // resitance between bus 1 and bus 2 in Ohmes
	public double capacity; // the capacity for this branch in MW
	public int id; // branch id in the network;
	public double avgFlow; // average branch flow (in MW)
	public int totalCount; // usage counter
	public double flow2capratio; // avgFlow / capacity
	
	
	public Branch(int bus1, int bus2, double volbase1, double volbase2, 
			double resistance, double capacity, int id) {
		this.bus1 = bus1;
		this.bus2 = bus2;
		this.volbase1 = volbase1;
		this.volbase2 = volbase2;
		this.resistance = resistance;
		this.capacity = capacity;
		this.id = id;
		avgFlow = 0;
		totalCount = 0;
		flow2capratio = 0;
	}
	
	/*
	 * This function is to get the branch flow statistics
	 */
	public void updateTotalFlow(double flow) {
		avgFlow = (avgFlow * totalCount + flow) / (totalCount + 1);
		totalCount++;
	}
	
	public void computeRatio() {
		flow2capratio = avgFlow / capacity;
	}
	
	
	public void print() {
		System.out.println("Branch id: " + id + ", fromBus id: " + bus1 + ", toBus id: " + bus2 + 
				", flow2capratio: " + flow2capratio);
	}
	
	public String toString() {
		return "Branch id: " + id + ", fromBus id: " + bus1 + ", toBus id: " + bus2 + 
				", flow2capratio: " + flow2capratio;
	}
}

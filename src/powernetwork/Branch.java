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
	
	
	public Branch(int bus1, int bus2, double volbase1, double volbase2, 
			double resistance, double capacity, int id) {
		this.bus1 = bus1;
		this.bus2 = bus2;
		this.volbase1 = volbase1;
		this.volbase2 = volbase2;
		this.resistance = resistance;
		this.capacity = capacity;
		this.id = id;
	}
}

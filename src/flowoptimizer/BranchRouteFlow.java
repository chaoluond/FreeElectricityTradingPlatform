/**
 * 
 */
package flowoptimizer;

/**
 * @author Chao
 * Represent the power flow for each branch for each route for each SD pair
 */
public class BranchRouteFlow {
	public int bus1;
	public int bus2;
	public double powerflow;
	
	public BranchRouteFlow(int bus1, int bus2, double powerflow) {
		this.bus1 = bus1;
		this.bus2 = bus2;
		this.powerflow = powerflow;
	}
	
	public void print() {
		System.out.println("[From bus " + bus1 + " to bus " + bus2 + ". Carry power " + 
				powerflow + "]");
	}
}

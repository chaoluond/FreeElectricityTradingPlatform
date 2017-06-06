package flowoptimizer;

public class BranchFlow {
	
	// flow direction. Bus 1 ----> Bus 2
	public int bus1;
	public int bus2;
	public double powerflow;
	
	public BranchFlow(int bus1, int bus2, double powerflow) {
		this.bus1 = bus1;
		this.bus2 = bus2;
		this.powerflow = powerflow;
	}
	
	
	public void print() {
		System.out.println("[from bus " + bus1 + " to bus " + bus2 + " with total flow " + 
				powerflow + "]");
	}
}

package flowoptimizer;

import java.util.List;
import powernetwork.Route;

/*
 * Supply and demand pair
 */
public class SDPair {
	public int supplyBus;
	public int demandBus;
	public double requestPower; // In MW
	public List<Route> routes;
	
	
	public SDPair(int supply, int demand, double power, List<Route> r) {
		supplyBus = supply;
		demandBus = demand;
		requestPower = power;
		this.routes = r;
	}
	
	public void print() {
		System.out.println("supplybus id: " + supplyBus + ", demandbus id: " + demandBus + ", requestpower: " 
				+ requestPower + ", number of routes: " + routes.size());
	}
}

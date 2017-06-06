package unittest;


import org.junit.Test;

import centralmanagment.PlatformController;
import powernetwork.NetworkGraph;
import supplydemandmatch.SupplyDemandMatcher;
import supplydemandsimulation.Bus;

public class MatcherTest {

	@Test
	public void test() {
		SupplyDemandMatcher matcher = new SupplyDemandMatcher(new NetworkGraph(), 25);
		
		
		for (int i = 0; i < 20; i++) {
			
			System.out.println("Iteration:::::::::" + i + ". Standard time: " + PlatformController.standardTime);
			System.out.println("Demanders: ");
			for (int busid : SupplyDemandMatcher.demanders)
				SupplyDemandMatcher.busPool.get(busid).currBid.print();
			
			System.out.println("Suppliers: "); 
			for (int busid : SupplyDemandMatcher.suppliers)
				SupplyDemandMatcher.busPool.get(busid).currSupply.print();
			
			matcher.match();
			
			System.out.println("After match, demandiers: ");
			for (int busid : SupplyDemandMatcher.demanders)
				SupplyDemandMatcher.busPool.get(busid).currBid.print();
			
			System.out.println("After match, suppliers: ");
			for (int busid : SupplyDemandMatcher.suppliers)
				SupplyDemandMatcher.busPool.get(busid).currSupply.print();
			
			System.out.println("After match, matched pairs: ");
			for (int key : SupplyDemandMatcher.demandsupplypairs.keySet()){
				System.out.println("demander is ");
				SupplyDemandMatcher.busPool.get(key).currBid.print();
				System.out.println("supplier is ");
				SupplyDemandMatcher.busPool.get(SupplyDemandMatcher.demandsupplypairs.get(key)).currSupply.print();
			}
			
			for (Bus bus : SupplyDemandMatcher.busPool) {
				bus.doWork();
			}
			
			PlatformController.standardTime += PlatformController.timeInterval;
			
		
		}
		
		
		
		
		
	}

}

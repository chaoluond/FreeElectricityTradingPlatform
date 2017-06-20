package unittest;


import org.junit.Test;

import supplydemandsimulation.Bus;
import supplydemandsimulation.DemandBid;
import supplydemandsimulation.SupplyOffer;

public class BidOfferTest {

	@Test
	public void testDemandBid() {
		Bus bus = new Bus(0, 1);
		for (int i = 0; i < 8; i++) {
			DemandBid bid = bus.generateBid();
			bid.print();
		}
		
	}
	
	
	@Test
	public void testSupplyOffer() {
		Bus bus = new Bus(1, 1);
		for (int i = 0; i < 8; i++) {
			SupplyOffer offer = bus.generateOffer();
			offer.print();
		}
	}

}

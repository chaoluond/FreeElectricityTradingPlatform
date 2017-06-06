package unittest;

import java.util.List;

import org.junit.Test;

import powernetwork.NetworkGraph;
import powernetwork.Route;
import powernetwork.RouteUtility;

public class RouteTest {

	@Test
	public void test() {
		
		NetworkGraph network = new NetworkGraph();
		System.out.println("number of branches : " + network.numBranch);
		System.out.println(network.branch.get(network.branch.size() - 1).id);
		List<Route> routes = RouteUtility.findAllRoutes(network, 8, 100, 5);
		
		for (int i = 0; i < routes.size(); i++) {
			System.out.println(i);
			routes.get(i).print();
		}
		
		/*List<SDPair> pairs = new ArrayList<>();
		SDPair pair = new SDPair(1, 2, 100, routes);
		pairs.add(pair);
		FlowOptimizer fop = new FlowOptimizer(network, pairs);
		fop.solve();*/
	}

}

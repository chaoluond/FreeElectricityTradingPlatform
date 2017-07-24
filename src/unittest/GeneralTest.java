package unittest;


import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import flowoptimizer.FlowOptimizer;
import flowoptimizer.SDPair;
import powernetwork.NetworkGraph;
import powernetwork.Route;
import powernetwork.RouteUtility;

public class GeneralTest {

	@Test
	public void test() {
		//RouteTest rt = new RouteTest();
		//rt.test();
		//String fileName = "P.txt";
		//String fileNameA = "A.txt";
		//String fileNameb = "btest.txt";
		NetworkGraph network = new NetworkGraph();
		List<SDPair> pairs = new ArrayList<>();
		List<Route> routes1 = RouteUtility.findAllRoutes(network, 8, 100, 3);
		List<Route> routes2 = RouteUtility.findAllRoutes(network, 7, 66, 1);
		List<Route> routes3 = RouteUtility.findAllRoutes(network, 12, 55, 1);
		List<Route> routes4 = RouteUtility.findAllRoutes(network, 20, 88, 1);
		List<Route> routes5 = RouteUtility.findAllRoutes(network, 69, 19, 1);
		List<Route> routes6 = RouteUtility.findAllRoutes(network, 28, 101, 1);
		List<Route> routes7 = RouteUtility.findAllRoutes(network, 1, 92, 1);
		
		/*for (int i = 0; i < routes1.size(); i++) {
			System.out.println(i);
			routes1.get(i).print();
		}
		
		for (int i = 0; i < routes2.size(); i++) {
			System.out.println(i);
			routes2.get(i).print();
		}
		
		for (int i = 0; i < routes3.size(); i++) {
			System.out.println(i);
			routes3.get(i).print();
		}*/
		
		SDPair pair1 = new SDPair(8, 100, 18, routes1);
		/*SDPair pair2 = new SDPair(7, 66, 10, routes2);
		SDPair pair3 = new SDPair(12, 55, 10, routes3);
		SDPair pair4 = new SDPair(20, 88, 10, routes4);
		SDPair pair5 = new SDPair(69, 19, 10, routes5);
		SDPair pair6 = new SDPair(28, 101, 5, routes6);
		SDPair pair7 = new SDPair(1, 92, 5, routes7);
		pairs.add(pair1);
		pairs.add(pair2);
		pairs.add(pair3);
		pairs.add(pair4);
		pairs.add(pair5);
		pairs.add(pair6);
		pairs.add(pair7);*/
		pairs.add(pair1);
		
		FlowOptimizer fop = new FlowOptimizer(network, pairs);
		fop.solve();
		//WriteToFile.write2File(fop.P, fileName);
		//WriteToFile.write2File(fop.A, fileNameA);
		//WriteToFile.write2File(fop.btest, fileNameb);
	}

}

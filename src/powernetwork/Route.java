/**
 * 
 */
package powernetwork;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chao
 * The route contains a list of branches from the origin to the destination. 
 *
 */
public class Route {
	public List<Branch> route = new ArrayList<>();
	public Route(List<Branch> route) {
		this.route = route;
	}
	
	public void print() {
		String result = "";
		for (Branch br : route)
			result += "[" + br.bus1 + "," + br.bus2 + "," + br.resistance + "," + 
		br.volbase1 + "," + br.volbase2 + "," + br.capacity + "," + br.id + "]";
		
		System.out.println(result);
	}
}

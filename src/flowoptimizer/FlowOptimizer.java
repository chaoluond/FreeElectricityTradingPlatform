package flowoptimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;

import Jama.Matrix;
import centralmanagment.PlatformController;
import powernetwork.Branch;
import powernetwork.NetworkGraph;
import powernetwork.Route;


public class FlowOptimizer {
	
	public int numSD; // Number of supply and demand pairs
	public List<SDPair> pairs; // List of supply and demand pairs
	public NetworkGraph network; // power network
	public double[][] P; // matrix P for quadratic programming
	public double[][] A; // matrix A for quadratic programming
	public double[] b; // vector b for quadratic programming
	List<List<List<BranchRouteFlow>>> brchrouteflows; // The solution for power flow. For each SD pair, each route, each branch
	List<BranchFlow> branchflows; // The flows for each branch
	public double[] solution; // The solution from solve()
	public int numHops; // The total number of hops = the number of branches
	
	public FlowOptimizer(NetworkGraph network, List<SDPair> pairs) {
		this.network = network;
		this.pairs = pairs;
		numSD = pairs.size();
	}
	
	
	public boolean solve() {
		// Compute all branches involved in the delivery. 
		// int[0] --- SD pair id, int[1] --- route id, int[2] --- branch id
		// The first integer in the hashtable is the branch id.
		HashMap<Integer, List<int[]>> branchIndex = new HashMap<>();
		for (int k = 0; k < numSD; k++) {
			SDPair pair = pairs.get(k);
			List<Route> routes = pair.routes;
			for (int j = 0; j < routes.size(); j++) {
				Route curr = routes.get(j);
				for (Branch br : curr.route) {
					if (!branchIndex.containsKey(br.id))
						branchIndex.put(br.id, new ArrayList<>());
					
					int[] indices = new int[]{k, j, br.id};
					branchIndex.get(br.id).add(indices);
				}
				
			}
		}
		
		
		List<Integer> branchList = new ArrayList<>(branchIndex.keySet());
		Collections.sort(branchList);
		numHops = branchList.size();
		
		
		// Add the mapping from (k, j, i) ----> index used in the P matrix and X vector
		// This is useful when we construct the constraints in the optimization problem.
		HashMap<String, Integer> ind2Pind = new HashMap<>();
		int dim = 0;
		for (int key : branchList) {
			for (int[] tuple : branchIndex.get(key)) {
				String str = tuple[0] + "+" + tuple[1] + "+" + tuple[2];
				ind2Pind.put(str, dim++);
			}
		}
		
		
		/*
		 * f(x) = 1 / 2 * x^TPx + q^Tx + r
		 * s.t. Ax = b
		 *      Gx - h <= 0
		 */
		
		// Generate matrix P
		double unitconv = 1e6;
		P = new double[dim][dim];
		int startPos = 0;
		for (int brid : branchList) {// populate the coefficients in P branch by branch
			List<int[]> components = branchIndex.get(brid);
			int comSize = components.size();
			double resistance = network.branch.get(brid).resistance;
			double voltage = network.branch.get(brid).volbase1;
			double coeff = 2 * resistance / (voltage * voltage) * unitconv;
			
			for (int row = 0; row < comSize; row++) {
				for (int col = 0; col < comSize; col++) {
					P[row + startPos][col + startPos] = coeff;
				}
			}
			
			startPos += comSize;
			
		}
		
		
		
		// Equalities. Generate A matrix and b vector.
		List<double[]> AList = new ArrayList<>();
		List<Double> bList = new ArrayList<>();
		
		for (int j = 0; j < numSD; j++) {
			SDPair pair = pairs.get(j);
	
			List<Route> routes = pair.routes;
			double[] sumIndex = new double[dim];
			
			for (int k = 0; k < routes.size(); k++) {
				Route curr = routes.get(k);
				
				List<Branch> brs = curr.route;
				
				Branch start = brs.get(0);
				String key = j + "+" + k + "+" + start.id;
				if (!ind2Pind.containsKey(key))
					System.out.println("Hashmap error here!!!!!");
				int startInd = ind2Pind.get(key);
				
				/*
				 * Consider route sum constraint.
				 * All routes should sum up to the capacity for the SD pair.
				 * For each route, we only need to use the first branch flow for calculation. 
				 */
				sumIndex[startInd] = 1;
					
				
				/* Consider route constraint. 
				 * All branches in the same route should carry the same flow
				 */
				for (int i = 1; i < brs.size(); i++) {
					Branch next = brs.get(i);
					double[] temp = new double[dim];
					temp[startInd] = 1;
		
					key = j + "+" + k + "+" + next.id;
					if (!ind2Pind.containsKey(key))
						System.out.println("Hashmap error happens here!");
					
					int nextInd = ind2Pind.get(key);
					temp[nextInd] = -1;
					
					AList.add(temp);
					bList.add(0.0);
				}
				
			}
			
			AList.add(sumIndex);
			bList.add(pair.requestPower);			
		}
		
		A = new double[AList.size()][];
		b = new double[AList.size()];
		
		for (int i = 0; i < AList.size(); i++) {
			A[i] = AList.get(i);
			b[i] = bList.get(i);
		}
			
		if (PlatformController.maxRoute == 1)
			return solveLinearEquation(A, b, branchIndex, branchList, ind2Pind);
		else
			return solveOptimization(P, A, b, branchIndex, branchList, ind2Pind);
	}
	
	
	private boolean solveLinearEquation(double[][] A, double[] b, HashMap<Integer, List<int[]>> branchIndex, 
			List<Integer> branchList, HashMap<String, Integer> ind2Pind)  {
		Matrix lhs = new Matrix(A);
		Matrix rhs = new Matrix(b, b.length);
		
		// Solve the linear equations
		Matrix sol = lhs.solve(rhs);
		int dim = ind2Pind.size();
		if (dim != b.length)
			System.out.println("Dimension error here!");
		solution = new double[dim];
		for (int i = 0; i < b.length; i++)
			solution[i] = sol.get(i, 0);
		
		
		// check nonnegavility
		for (double var : solution) {
			if (var < 0) {
				System.out.println("Flow cannot be negative! Error!");
				return false;
			}
		}
		
		
		// check capacity constraint
		for (int branchid : branchIndex.keySet()) {
			Branch curr = network.branch.get(branchid);
			double capacity = curr.capacity;
			List<int[]> components = branchIndex.get(branchid);
			double sum = 0;
			for (int[] tuple : components) {
				String key = tuple[0] + "+" + tuple[1] + "+" + tuple[2];
				int ind = ind2Pind.get(key);
				sum += solution[ind];
			}
			
			if (sum > capacity) {
				System.out.println("Exceed branch capacity! Error!");
				return false;
			}
		}
				
		System.out.println("Power loss is " + computePowerLoss());
		return true;
	}
	
	
	private boolean solveOptimization(double[][] P, double[][] A, double[] b, 
			HashMap<Integer, List<int[]>> branchIndex, 
			List<Integer> branchList, HashMap<String, Integer> ind2Pind) {
		/*
		 * f(x) = 1 / 2 * x^TPx + q^Tx + r
		 * s.t. Ax = b
		 *      Gx - h <= 0
		 */
		
		int dim = ind2Pind.size();
		
		// Inequalities
		/*
		 * For each branch, the aggregated flow should not exceed the branch capacity
		 */
		ConvexMultivariateRealFunction[] inequalities = 
				new ConvexMultivariateRealFunction[branchList.size() + dim];
		for (int i = 0; i < branchList.size(); i++) {
			int branchid = branchList.get(i);
			Branch curr = network.branch.get(branchid);
			double capacity = curr.capacity;
			List<int[]> components = branchIndex.get(branchid);
			double[] temp = new double[dim];
			for (int[] tuple : components) {
				String key = tuple[0] + "+" + tuple[1] + "+" + tuple[2];
				if (!ind2Pind.containsKey(key))
					System.out.println("Something problem happens here!");
				
				int ind = ind2Pind.get(key);
				temp[ind] = 1;
			}
			
			inequalities[i] = new LinearMultivariateRealFunction(temp, -capacity);
			
		}
		
		/*
		 * Every variable should be non-negative
		 */
		for (int i = 0; i < dim; i++) {
			double[] temp = new double[dim];
			temp[i] = -1;
			inequalities[branchList.size() + i] = 
					new LinearMultivariateRealFunction(temp, 0);
		}
		
		
		
		// Solve the optimization problem
		PDQuadraticMultivariateRealFunction objectiveFunction = 
				new PDQuadraticMultivariateRealFunction(P, null, 0);
		
		OptimizationRequest or = new OptimizationRequest();
		or.setF0(objectiveFunction);
		or.setA(A);
		or.setB(b);
		//or.setInitialPoint(new double[] {0.290, 1.413, 0.045});
		or.setFi(inequalities);
		or.setToleranceFeas(1.E-2);
		or.setTolerance(1.E-2);
		
		JOptimizer opt = new JOptimizer();
		opt.setOptimizationRequest(or);
		int returncode = -1;
		try {
			returncode = opt.optimize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		System.out.println("Returncode = " + returncode);
		if (returncode == -1)
			return false;
		
		
		// Get the solution
		solution = opt.getOptimizationResponse().getSolution();
		
		System.out.println("Power loss is " + computePowerLoss());
		
		return true;
		
	}
	
	
	public double computePowerLoss() {
		Matrix X = new Matrix(solution, solution.length);
		Matrix Pmatrix = new Matrix(P);
		Matrix res = X.transpose().times(Pmatrix).times(X).times(0.5);
		return res.get(0, 0);
	}
	
	
	public void computeBranchRouteFlow(HashMap<String, Integer> ind2Pind) {	
		/*
		 * Construct the branch route flow
		 */
		brchrouteflows = new ArrayList<>();
		for (int k = 0; k < numSD; k++) {
			SDPair pair = pairs.get(k);
			List<Route> routes = pair.routes;
			List<List<BranchRouteFlow>> list = new ArrayList<>();
			for (int j = 0; j < routes.size(); j++) {
				List<BranchRouteFlow> flow = new ArrayList<>();
				Route curr = routes.get(j);
				List<Branch> brs = curr.route;
				for (Branch br : brs) {
					String key = k + "+" + j + "+" + br.id;
					if (!ind2Pind.containsKey(key))
						System.out.println("I am an error!");
					
					int ind = ind2Pind.get(key);
					double powerflow = solution[ind];
					BranchRouteFlow brf = new BranchRouteFlow(br.bus1, br.bus2, powerflow);
					flow.add(brf);
				}
				
				list.add(flow);
			}
			
			brchrouteflows.add(list);
			
		}
		
		
		// print out all flows
		/*for (int j = 0; j < numSD; j++) {
			System.out.println("SD Pair id: " + j);
			for (int k = 0; k < solution.get(j).size(); k++) {
				System.out.println("Route id: " + k);
				for (int i = 0; i < solution.get(j).get(k).size(); i++) {
					BranchRouteFlow brf = solution.get(j).get(k).get(i);
					brf.print();
				}
			}
		}*/
	}
	
	
	public void computeBranchFlow(HashMap<Integer, List<int[]>> branchIndex,
			List<Integer> branchList, HashMap<String, Integer> ind2Pind) {
		/*
		 * Construct branch flow
		 */
		branchflows = new ArrayList<>();
		for (int brid : branchList) {
			Branch br = network.branch.get(brid);
			List<int[]> components = branchIndex.get(brid);
			double sum = 0;
			
			for (int[] tuple : components) {
				String key = tuple[0] + "+" + tuple[1] + "+" + tuple[2];
				if (!ind2Pind.containsKey(key))
					System.out.println("Error!");
				
				int ind = ind2Pind.get(key);
				sum += solution[ind];
			}
			
			BranchFlow bf = new BranchFlow(br.bus1, br.bus2, sum);
			branchflows.add(bf);
		}
		
		
		/*System.out.println("Total powerflow for each branch:");
		for (BranchFlow bf : result) {
			bf.print();
		}*/
	}
	
	
	
}

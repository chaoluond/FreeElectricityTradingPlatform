package unittest;


import org.junit.Test;

import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;

public class JoptimizerTest {

	@Test
	public void test() {
		
		// Create objective function
		/*
		 * f(X) = 1/2 X^TPX + ......
		 * Do not forget coefficient 1/ 2
		 */
		double[][] P = {{2, 0, 1}, {0, 4, 0}, {1, 0, 6}};
		double[] q = {1, -2, 4};
		double r = 0;
		PDQuadraticMultivariateRealFunction objectiveFunction = 
				new PDQuadraticMultivariateRealFunction(P, q, r);
		
		
		// equalities
		double[][] A = new double[][]{{-2, -3, -4}};
		double[] b = new double[]{-5};
		
		
		// inequalities
		ConvexMultivariateRealFunction[] inequalities = 
				new ConvexMultivariateRealFunction[8];
		
		// inequalities please use GX - h <= 0 to populate the coefficients.
		inequalities[0] = new LinearMultivariateRealFunction(new double[]{3, 4, -2}, -10);
		inequalities[1] = new LinearMultivariateRealFunction(new double[]{3, -2, -1}, 2);
		inequalities[2] = new LinearMultivariateRealFunction(new double[]{-1, 0, 0}, 0);
		inequalities[3] = new LinearMultivariateRealFunction(new double[]{1, 0, 0}, -5);
		inequalities[4] = new LinearMultivariateRealFunction(new double[]{0, -1, 0}, 1);
		inequalities[5] = new LinearMultivariateRealFunction(new double[]{0, 1, 0}, -5);
		inequalities[6] = new LinearMultivariateRealFunction(new double[]{0, 0, -1}, 0);
		inequalities[7] = new LinearMultivariateRealFunction(new double[]{0, 0, 1}, -5);
		
		
		// solve optimization problem
		OptimizationRequest or = new OptimizationRequest();
		or.setF0(objectiveFunction);
		or.setA(A);
		or.setB(b);
		//or.setInitialPoint(new double[] {0.290, 1.413, 0.045});
		or.setFi(inequalities);
		or.setToleranceFeas(1.E-12);
		or.setTolerance(1.E-12);
		
		JOptimizer opt = new JOptimizer();
		opt.setOptimizationRequest(or);
		try {
			opt.optimize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		double[] sol = opt.getOptimizationResponse().getSolution();
		System.out.println("x1: " + sol[0] + "\n" + "x2: " + sol[1] +
				"\n" + "x3: " + sol[2]);
	}

}

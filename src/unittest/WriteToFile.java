package unittest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class WriteToFile {
	
	public static void write2File(double[][] matrix, String fileName) {
		
		DecimalFormat format = new DecimalFormat("#0.000");
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			for (int i = 0; i < matrix.length; i++) {
				for (int j = 0; j < matrix[0].length; j++)
					writer.write(format.format(matrix[i][j]) + ",");
				
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void write2File(List<String> content, String fileName) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			
			for (String str : content) {
				writer.write(str);
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

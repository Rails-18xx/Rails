/*
 * Created on Feb 27, 2005
 */
package test;

import game.*;

/**
 * @author Brett Lentz
 *
 */
public class StockTest {

	public static void StockChartTest()
	{
		int row, col, price;
		StockChart chart = new StockChart("1830");
		StockPrice square;
		
		System.out.print (" ");
		for (col=0; col<chart.getNumCols(); col++) {
			System.out.print("   " + Character.toString((char)('A'+col)));
		}
		System.out.println();
		for (row=0; row<chart.getNumRows(); row++) {
			System.out.print ((row<9 ? " " : "") + (row+1)); 
			for (col=0; col<chart.getNumCols(); col++) {
				square = chart.getStockPrice(row,col);
				if (square != null) {
					price = square.getPrice();
					System.out.print (" " + (price<100 ? " " : "") + price);
				} else {
					System.out.print("    ");
				}
			}
			System.out.println();
		}		
	}
	
	public static void main(String[] args) {

		StockChartTest();
	}
}

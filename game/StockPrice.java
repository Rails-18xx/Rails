/*
 * Created on 24-Feb-2005
 */
package game;

/**
 * Objects of this class represent a square on the StockMarket.
 * 
 * @author Erik Vos
 */
public class StockPrice {
	
	/*--- Class attributes ---*/
	
	/*--- Instance attributes ---*/
	private String name;
	private int row;
	private int column;
	private int price;
	private int colour;
	private boolean aboveLedge = false;		// For 1870
	private boolean closesCompany = false;	// For 1856 and other games
	private boolean endsGame = false;		// For 1841 and other games
	
	/*--- Constants ---*/
	public static final int WHITE = 0;
	public static final int YELLOW = 1;
	public static final int ORANGE = 2;
	public static final int BROWN = 3;
	// Other colours exist but can probably be mapped to this set.
	
	/*--- Contructors ---*/
	public StockPrice (String name, int price) {
		this (name, price, WHITE);
	}
	
	public StockPrice (String name, int price, int colour) {
		this.name = name;
		this.price = price;
		this.colour = colour;
		this.row = Integer.parseInt(name.substring(1)) - 1;
		this.column = (int)(name.toUpperCase().charAt(0) - '@') - 1;
	}
	// No constructors (yet) for the booleans, which are rarely needed. Use the setters.
		
	/*--- Getters ---*/
	/**
	 * @return TRUE is the square is just above a ledge.
	 */
	public boolean isAboveLedge() {
		return aboveLedge;
	}

	/**
	 * @return TRUE if the square closes companies landing on it.
	 */
	public boolean isClosesCompany() {
		return closesCompany;
	}

	/**
	 * @return The square's colour.
	 */
	public int getColour() {
		return colour;
	}

	/**
	 * @return TRUE if the game ends if a company lands on this square.
	 */
	public boolean isEndsGame() {
		return endsGame;
	}

	/**
	 * @return The stock price associated with the square.
	 */
	public int getPrice() {
		return price;
	}

	/**
	 * @return
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return
	 */
	public int getRow() {
		return row;
	}

	/*--- Setters ---*/
	/**
	 * @param b See isAboveLedge.
	 */
	public void setAboveLedge(boolean b) {
		aboveLedge = b;
	}

	/**
	 * @param b See isClosesCompany.
	 */
	public void setClosesCompany(boolean b) {
		closesCompany = b;
	}

	/**
	 * @param b See isEndsGame.
	 */
	public void setEndsGame(boolean b) {
		endsGame = b;
	}

}

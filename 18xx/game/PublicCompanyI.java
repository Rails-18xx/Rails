/*
 * Created on 05mar2005
 *
 */
package game;

import java.util.ArrayList;



/**
 * @author Erik Vos
 */
public interface PublicCompanyI extends CompanyI  {
	
	public void start (StockSpaceI startPrice);

	/**
	 * @return
	 */
	public String getBgColour();
	/**
	 * @return
	 */
	public boolean canBuyStock();
	/**
	 * @return
	 */
	public boolean canBuyPrivates();
	/**
	 * @return
	 */
	public String getFgColour();
	/**
	 * @return
	 */
	public boolean hasFloated();
	/**
	 * @return
	 */
	public StockSpaceI getParPrice();
	/**
	 * @return
	 */
	public ArrayList getTrainsOwned();

	/**
	 * @return
	 */
	public int getTreasury();

	/**
	 * @param list
	 */
	public void setTrainsOwned(ArrayList list);

	/**
	 * @param i
	 */
	public void setTreasury (int i);

	/**
	 * @return
	 */
	public StockSpaceI getCurrentPrice();
	/**
	 * @param price
	 */
	public void setCurrentPrice(StockSpaceI price);
	/**
	 * @param b
	 */
	public void setFloated(boolean b);


	/**
	 * @return
	 */
	public int getPublicNumber();
	/**
	 * @param string
	 */
	public void setBgColour(String string);
	/**
	 * @param string
	 */
	public void setFgColour(String string);


}

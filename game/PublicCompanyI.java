/*
 * Created on 05mar2005
 *
 */
package game;

import java.awt.Color;
import java.util.*;



/**
 * @author Erik Vos
 */
public interface PublicCompanyI extends CompanyI  {
	
	public void start (StockSpaceI startPrice);

	/**
	 * @return
	 */
	public Color getBgColour();
	/**
	 * @return
	 */
	public String getHexBgColour();
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
	public Color getFgColour();
	/**
	 * @return
	 */
	public String getHexFgColour();
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
	 * @param list
	 */
	public void setTrainsOwned(ArrayList list);

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
	public void setFloated(int initialCash);


	/**
	 * @return
	 */
	public int getPublicNumber();

	/**
	 * @return
	 */
	public List getCertificates();

	/**
	 * @param list
	 */
	public void setCertificates(List list);
	
	public void addCertificate (CertificateI certificate);
	/**
	 * @param spaceI
	 */
	public void setParPrice(StockSpaceI parPrice);
	
	public int getCash ();
	
	public int getLastRevenue();
	
	public Portfolio getPortfolio();
	
	public void payOut (int amount);
	
	public void withhold (int amount);

	public boolean isSoldOut ();

}

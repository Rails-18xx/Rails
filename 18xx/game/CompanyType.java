/*
 * $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/CompanyType.java,v 1.1 2005/03/20 23:52:23 wakko666 Exp $
 * Created on 19mar2005 by Erik Vos
 * Changes: 
 */
package game;

import org.w3c.dom.Element;

/**
 * Objects of this class represent a type of square on the StockMarket
 * with special properties,usually represented by a non-white square colour.
 * The default type is "white", which has no special properties. 
 * 
 * @author Erik Vos
 */
public class CompanyType implements CompanyTypeI {
	
	/*--- Class attributes ---*/
	
	/*--- Instance attributes ---*/
	protected String name;
	protected String className;
	protected Element domElement;
	protected String auctionType;
	protected int allClosePhase;
	
	public CompanyType (String name, String className, Element element) {
		this.name = name;
		this.className = className;
		this.domElement = element;
	}
	
	/*--- Getters and setters ---*/
	/**
	 * @return
	 */
	public int getAllClosePhase() {
		return allClosePhase;
	}

	/**
	 * @return
	 */
	public String getAuctionType() {
		return auctionType;
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param i
	 */
	public void setAllClosePhase(int i) {
		allClosePhase = i;
	}

	/**
	 * @param string
	 */
	public void setAuctionType(String string) {
		auctionType = string;
	}

	/**
	 * @return
	 */
	public String getClassName() {
		return className;
	}

	public void releaseDomElement () {
		domElement = null;
	}
	/**
	 * @return
	 */
	public Element getDomElement() {
		return domElement;
	}

}

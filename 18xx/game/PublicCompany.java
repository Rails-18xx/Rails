
/*
 * Created on 05mar2005
 *
 */
package game;

import java.util.ArrayList;

import org.w3c.dom.*;

import util.XmlUtils;

/**
 * @author Erik Vos
 */
public class PublicCompany extends Company implements PublicCompanyI {

	protected static int numberOfPublicCompanies = 0;

	protected String fgColour;
	protected String bgColour;
	protected int publicNumber; // For internal use

	protected StockSpaceI parPrice = null;
	protected StockSpaceI currentPrice = null;

	protected int treasury = 0;
	protected boolean hasFloated = false;
	protected boolean closed = false;
	protected boolean canBuyStock = false;
	protected boolean canBuyPrivates = false;
	protected float lowerPrivatePriceFactor;
	protected float upperPrivatePriceFactor;
	protected boolean ipoPaysOut = false;
	protected boolean poolPaysOut = false;

	protected ArrayList trainsOwned;
	protected ArrayList portfolio;
	protected ArrayList littleCoOwned;

	public PublicCompany() {
		super();
		this.publicNumber = numberOfPublicCompanies++;
	}


	public void configureFromXML(Element element) throws ConfigurationException {
		NamedNodeMap nnp = element.getAttributes();
		NamedNodeMap nnp2;

		/* Configure public company features */
		fgColour = XmlUtils.extractStringAttribute(nnp, "fgColour");
		if (fgColour == null)
			fgColour = "white";
		bgColour = XmlUtils.extractStringAttribute(nnp, "bgColour");
		if (bgColour == null)
			bgColour = "black";

		/* Complete configuration by adding features from the Public CompanyType */
		Element typeElement = type.getDomElement();
		if (typeElement != null) {
			NodeList properties = typeElement.getChildNodes();

			for (int j = 0; j < properties.getLength(); j++) {

				String propName = properties.item(j).getLocalName();
				if (propName == null)
					continue;

				if (propName.equalsIgnoreCase("CanBuyPrivates")) {
					canBuyPrivates = true;
					nnp2 = properties.item(j).getAttributes();
					String lower = XmlUtils.extractStringAttribute(nnp2, "lowerPriceFactor");
					if (!XmlUtils.hasValue(lower))
						throw new ConfigurationException("Lower private price factor missing");
					lowerPrivatePriceFactor = Float.parseFloat(lower);
					String upper = XmlUtils.extractStringAttribute(nnp2, "upperPriceFactor");
					if (!XmlUtils.hasValue(upper))
						throw new ConfigurationException("Upper private price factor missing");
					upperPrivatePriceFactor = Float.parseFloat(upper);

				} else if (propName.equalsIgnoreCase("PoolPaysOut")) {
					poolPaysOut = true;
				}

			}
		}
		
		type.releaseDomElement();
	}

	public void start(StockSpaceI startPrice) {
		parPrice = currentPrice = startPrice;
		hasFloated = true;
		parPrice.addToken(this);
	}

	/**
	 * @return
	 */
	public String getBgColour() {
		return bgColour;
	}

	/**
	 * @return
	 */
	public boolean canBuyStock() {
		return canBuyStock;
	}

	/**
	 * @return
	 */
	public boolean canBuyPrivates() {
		return canBuyPrivates;
	}

	/**
	 * @return
	 */
	public String getFgColour() {
		return fgColour;
	}

	/**
	 * @return
	 */
	public boolean hasFloated() {
		return hasFloated;
	}

	/**
	 * @return
	 */
	public ArrayList getPortfolio() {
		return portfolio;
	}

	/**
	 * @return
	 */
	public StockSpaceI getParPrice() {
		return parPrice;
	}

	/**
	 * @return
	 */
	public ArrayList getTrainsOwned() {
		return trainsOwned;
	}

	/**
	 * @return
	 */
	public int getTreasury() {
		return treasury;
	}

	/**
	 * @param list
	 */
	public void setTrainsOwned(ArrayList list) {
		trainsOwned = list;
	}

	/**
	 * @param i
	 */
	public void setTreasury(int i) {
		treasury = i;
	}

	/**
	 * @return
	 */
	public StockSpaceI getCurrentPrice() {
		return currentPrice;
	}

	/**
	 * @param price
	 */
	public void setCurrentPrice(StockSpaceI price) {
		currentPrice = price;
	}

	/**
	 * @param b
	 */
	public void setFloated(boolean b) {
		hasFloated = b;
	}

	/**
	 * @return
	 */
	public static int getNumberOfPublicCompanies() {
		return numberOfPublicCompanies;
	}

	/**
	 * @return
	 */
	public int getPublicNumber() {
		return publicNumber;
	}

	/**
	 * @param i
	 */
	public static void setNumberOfCompanies(int i) {
		numberOfCompanies = i;
	}

	/**
	 * @param string
	 */
	public void setBgColour(String string) {
		bgColour = string;
	}

	/**
	 * @param string
	 */
	public void setFgColour(String string) {
		fgColour = string;
	}

}

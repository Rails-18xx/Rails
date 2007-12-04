/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PrivateCompany.java,v 1.10 2007/12/04 20:25:20 evos Exp $ */
package rails.game;


import java.util.*;

import org.w3c.dom.*;

import rails.game.move.CashMove;
import rails.game.move.CertificateMove;
import rails.game.special.SpecialPropertyI;
import rails.game.special.SpecialTokenLay;
import rails.util.LocalText;
import rails.util.Tag;
import rails.util.Util;


public class PrivateCompany extends Company implements PrivateCompanyI
{

	protected static int numberOfPrivateCompanies = 0;
	protected int privateNumber; // For internal use

	protected int basePrice = 0;
	protected int revenue = 0;
	protected List<SpecialPropertyI> specialProperties = null;
	protected String auctionType;
	protected int closingPhase;
	protected boolean closeIfAllExercised = false; // Not yet used

	protected List<MapHex> blockedHexes = null;

	public PrivateCompany()
	{
		super();
		this.privateNumber = numberOfPrivateCompanies++;
	}

	/**
	 * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
	 */
	public void configureFromXML(Tag tag) throws ConfigurationException
	{
		/* Configure private company features */
		try
		{
			basePrice = tag.getAttributeAsInteger("basePrice", 0);
			revenue = tag.getAttributeAsInteger("revenue", 0);

			// Blocked hexes (until bought by a company)
			Tag blockedTag = tag.getChild("Blocking");
			if (blockedTag != null)
			{
				String[] hexes = blockedTag.getAttributeAsString("hex").split(",");
				if (hexes != null && hexes.length > 0)
				{
					blockedHexes = new ArrayList<MapHex>();
					//for (int i = 0; i < hexes.length; i++)
					for (String hexName : hexes)
					{
						MapHex hex = MapManager.getInstance().getHex(hexName);
						blockedHexes.add(hex);
						hex.setBlocked(true);
					}
				}
			}

			// Special properties
			Tag spsTag = tag.getChild("SpecialProperties");
			if (spsTag != null)
			{
				closeIfAllExercised = spsTag.getAttributeAsBoolean("closeIfAllExercised", closeIfAllExercised);
				
				specialProperties = new ArrayList<SpecialPropertyI>();
				List<Tag> spTags = spsTag.getChildren("SpecialProperty");
				String className;
				for (Tag spTag : spTags)
				{
					className = spTag.getAttributeAsString("class");
					if (!Util.hasValue(className))
						throw new ConfigurationException("Missing class in private special property");

					SpecialPropertyI sp = (SpecialPropertyI) Class.forName(className)
							.newInstance();
					sp.setCompany(this);
					specialProperties.add(sp);
					sp.configureFromXML(spTag);
					
					if (sp instanceof SpecialTokenLay 
							&& ((SpecialTokenLay)sp).getToken() instanceof BonusToken) {
						GameManager.setBonusTokensExist(true);
					}

				}
			}

		}
		catch (Exception e)
		{
			throw new ConfigurationException("Configuration error for Private "
					+ name, e);
		}

		/*
		 * Complete configuration by adding features from the Private
		 * CompanyType
		 */
		/* Probably unused
		Element typeElement = tag;
		if (typeElement != null)
		{
			NodeList properties = typeElement.getChildNodes();

			for (int j = 0; j < properties.getLength(); j++)
			{

				String propName = properties.item(j).getLocalName();
				if (propName == null)
					continue;

				if (propName.equalsIgnoreCase("AllClose"))
				{
					nnp2 = properties.item(j).getAttributes();
					closingPhase = XmlUtils.extractIntegerAttribute(nnp2,
							"phase",
							0);
				}

			}
		}
		*/
	}

	/**
	 * @return Private Company Number
	 */
	public int getPrivateNumber()
	{
		return privateNumber;
	}

	/**
	 * @return Base Price
	 */
	public int getBasePrice()
	{
		return basePrice;
	}

	/**
	 * @return Revenue
	 */
	public int getRevenue()
	{
		return revenue;
	}

	/**
	 * @return Phase this Private closes
	 */
	public int getClosingPhase()
	{
		return closingPhase;
	}

	/**
	 * @return Portfolio of this Private
	 */
	public Portfolio getPortfolio()
	{
		return portfolio;
	}

	/**
	 * @param b
	 */
	public void setClosed()
	{
		if (!isClosed())
		{
			super.setClosed();
			unblockHexes();
			new CertificateMove (getPortfolio(), Bank.getScrapHeap(), 
			        (Certificate)this);
			ReportBuffer.add(LocalText.getText("PrivateCloses", name));
		}
	}

	/**
	 * @param i
	 */
	public void setClosingPhase(int i)
	{
		closingPhase = i;
	}

	/**
	 * @param portfolio
	 */
	public void setHolder(Portfolio portfolio)
	{
		this.portfolio = portfolio;

		/*
		 * If this private is blocking map hexes, unblock these hexes as soon as
		 * it is bought by a company.
		 */
		if (portfolio.getOwner() instanceof CompanyI)
		{
			unblockHexes();
		}
	}

	protected void unblockHexes()
	{
		if (blockedHexes != null)
		{
			for (MapHex hex : blockedHexes)
			{
				hex.setBlocked(false);
			}
		}
	}

	public void payOut()
	{
		if (portfolio.getOwner() != Bank.getInstance()) {
			ReportBuffer.add(
					LocalText.getText("ReceivesFor", new String[] {
							portfolio.getOwner().getName(),
							Bank.format(revenue),
							name
					}));
			new CashMove (null, portfolio.getOwner(), revenue);
		}
	}

	public String toString()
	{
		return "Private: "+name;
	}

	public Object clone()
	{

		Object clone = null;
		try
		{
			clone = super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			log.fatal ("Cannot clone company " + name);
			return null;
		}
		return clone;
	}

	public List<SpecialPropertyI> getSpecialProperties()
	{
		return specialProperties;
	}

	public List getBlockedHexes()
	{
		return blockedHexes;
	}
	
	public void closeIfExcercised () {
	    
	}

}

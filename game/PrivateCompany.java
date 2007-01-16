package game;

import game.special.SpecialPropertyI;

import java.util.*;

import org.w3c.dom.*;

import util.Util;
import util.XmlUtils;

public class PrivateCompany extends Company implements PrivateCompanyI
{

	protected static int numberOfPrivateCompanies = 0;
	protected int privateNumber; // For internal use

	protected int basePrice = 0;
	protected int revenue = 0;
	protected List specialProperties = null;
	protected String auctionType;
	protected int closingPhase;

	protected List blockedHexes = null;

	protected boolean closed = false;

	public PrivateCompany()
	{
		super();
		this.privateNumber = numberOfPrivateCompanies++;
	}

	/**
	 * @see game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
	 */
	public void configureFromXML(Element element) throws ConfigurationException
	{
		NamedNodeMap nnp = element.getAttributes();
		NamedNodeMap nnp2;

		/* Configure private company features */
		try
		{
			basePrice = Integer.parseInt(XmlUtils.extractStringAttribute(nnp,
					"basePrice",
					"0"));
			revenue = Integer.parseInt(XmlUtils.extractStringAttribute(nnp,
					"revenue",
					"0"));

			// Blocked hexes (until bought by a company)
			Element blEl = (Element) element.getElementsByTagName("Blocking")
					.item(0);
			if (blEl != null)
			{
				String[] hexes = XmlUtils.extractStringAttribute(blEl.getAttributes(),
						"hex")
						.split(",");
				if (hexes != null && hexes.length > 0)
				{
					blockedHexes = new ArrayList();
					for (int i = 0; i < hexes.length; i++)
					{
						MapHex hex = MapManager.getInstance().getHex(hexes[i]);
						blockedHexes.add(hex);
						hex.setBlocked(true);
					}
				}
			}

			// Special properties
			Element spsEl = (Element) element.getElementsByTagName("SpecialProperties")
					.item(0);
			if (spsEl != null)
			{
				specialProperties = new ArrayList();
				NodeList spsNl = spsEl.getElementsByTagName("SpecialProperty");
				Element spEl;
				String condition, className;
				for (int i = 0; i < spsNl.getLength(); i++)
				{
					spEl = (Element) spsNl.item(i);
					nnp2 = spEl.getAttributes();
					condition = XmlUtils.extractStringAttribute(nnp2,
							"condition");
					if (!Util.hasValue(condition))
						throw new ConfigurationException("Missing condition in private special property");
					className = XmlUtils.extractStringAttribute(nnp2, "class");
					if (!Util.hasValue(className))
						throw new ConfigurationException("Missing class in private special property");

					SpecialPropertyI sp = (SpecialPropertyI) Class.forName(className)
							.newInstance();
					sp.setCompany(this);
					sp.setUsableIfOwnedByPlayer(condition.matches("(?i).*ifOwnedByPlayer.*"));
					sp.setUsableIfOwnedByCompany(condition.matches("(?i).*ifOwnedByCompany.*"));
					specialProperties.add(sp);
					sp.configureFromXML(spEl);

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
		Element typeElement = element;
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
	 * @return if Private is Closed
	 */
	public boolean isClosed()
	{
		return closed;
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
		if (!closed)
		{
			closed = true;
			unblockHexes();
			Portfolio.transferCertificate(this,
					portfolio,
					Bank.getUnavailable());
			ReportBuffer.add("Private " + name + " closes");
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
			Iterator it = blockedHexes.iterator();
			while (it.hasNext())
			{
				((MapHex) it.next()).setBlocked(false);
			}
		}
	}

	public void payOut()
	{
		if (portfolio.getOwner() != Bank.getInstance()) {
			ReportBuffer.add(portfolio.getOwner().getName() + " receives "
					+ Bank.format(revenue) + " for " + name);
			Bank.transferCash(null, portfolio.getOwner(), revenue);
		}
	}

	public String toString()
	{
		return "Private Company Number: " + privateNumber + " of "
				+ PrivateCompany.numberOfPrivateCompanies;
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

	public List getSpecialProperties()
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

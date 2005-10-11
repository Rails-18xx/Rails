/*
 * Created on 05mar2005
 *
 */
package game;

import org.w3c.dom.*;

import util.XmlUtils;

/**
 * @author Erik Vos
 */
public class PrivateCompany extends Company implements PrivateCompanyI
{

	protected static int numberOfPrivateCompanies = 0;
	protected int privateNumber; // For internal use

	protected int basePrice = 0;
	protected int revenue = 0;
	protected String auctionType;
	protected int closingPhase;

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
		closed = true;
		Portfolio.transferCertificate(this, portfolio, Bank.getUnavailable());
		Log.write("Private " + name + " closes");
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
	}

	public void payOut()
	{
		Log.write(portfolio.getOwner().getName() + " receives "
				+ Bank.format(revenue) + " for " + name);
		Bank.transferCash(null, portfolio.getOwner(), revenue);
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
			Log.error("Cannot clone company " + name);
			System.out.println(e.getStackTrace());
		}
		return clone;
	}

}

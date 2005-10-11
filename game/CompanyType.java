/*
 * $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/CompanyType.java,v 1.7 2005/10/11 17:35:29 wakko666 Exp $
 * Created on 19mar2005 by Erik Vos
 * Changes: 
 */
package game;

import org.w3c.dom.Element;

/**
 * Objects of this class represent a particular type of company, of which
 * typically multiple instances exist in a game. Examples: "Private", "Minor",
 * "Major", "Mountain" etc.
 * <p>
 * This class contains common properties of the companies of one type, and aids
 * in configuring the companies by reducing the need to repeatedly specify
 * common properties with different companies.
 * 
 * @author Erik Vos
 */
public class CompanyType implements CompanyTypeI
{

	/*--- Class attributes ---*/

	/*--- Instance attributes ---*/
	protected String name;
	protected String className;
	protected String auctionType;
	protected int allClosePhase;
	// protected ArrayList defaultCertificates;
	protected int capitalisation = PublicCompanyI.CAPITALISE_FULL;

	private CompanyI dummyCompany;

	/**
	 * The constructor.
	 * 
	 * @param name
	 *            Company type name ("Private", "Public", "Minor" etc.).
	 * @param className
	 *            Name of the class that will instantiate this type of company.
	 * @param element
	 *            The &lt;CompanyType&gt; DOM element, used to define this
	 *            company type.
	 */
	public CompanyType(String name, String className)
	{
		this.name = name;
		this.className = className;
	}

	   /**
	    * @see game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
	    */
	public void configureFromXML(Element element) throws ConfigurationException
	{

		/* Create a dummy company implementing this company type */
		try
		{
			dummyCompany = (Company) Class.forName(className).newInstance();
		}
		catch (Exception e)
		{
			throw new ConfigurationException("Class " + className
					+ " cannot be instantiated", e);
		}
		dummyCompany.init("", this);
		dummyCompany.configureFromXML(element);

		/*
		 * Must be rewritten to a new tag String capitalMode =
		 * XmlUtils.extractStringAttribute(nnp, "capitalisation", "full");
		 * setCapitalisation(capitalMode);
		 */

	}

	public CompanyI createCompany(String name, Element element)
			throws ConfigurationException
	{
		CompanyI newCompany = null;
		try
		{
			newCompany = (CompanyI) dummyCompany.clone();
			newCompany.init(name, this);
			newCompany.configureFromXML(element);
		}
		catch (CloneNotSupportedException e)
		{
			Log.error("Cannot create company " + name + " by cloning");
		}
		return newCompany;
	}

	/*--- Getters and setters ---*/
	/**
	 * @return Phase all privates close
	 */
	public int getAllClosePhase()
	{
		return allClosePhase;
	}

	/**
	 * @return Type of Auction
	 */
	public String getAuctionType()
	{
		return auctionType;
	}

	/**
	 * Get the company type name
	 * 
	 * @return The name of this company type.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param i
	 */
	public void setAllClosePhase(int i)
	{
		allClosePhase = i;
	}

	/**
	 * @param string
	 */
	public void setAuctionType(String string)
	{
		auctionType = string;
	}

	/**
	 * Get the name of the class that will implement this type of company.
	 * 
	 * @return The full class name.
	 */
	public String getClassName()
	{
		return className;
	}

	public void setCapitalisation(int mode)
	{
		this.capitalisation = mode;
	}

	public void setCapitalisation(String mode)
	{
		if (mode.equalsIgnoreCase("full"))
		{
			this.capitalisation = PublicCompanyI.CAPITALISE_FULL;
		}
		else if (mode.equalsIgnoreCase("incremental"))
		{
			this.capitalisation = PublicCompanyI.CAPITALISE_INCREMENTAL;
		}
	}

	public int getCapitalisation()
	{
		return capitalisation;
	}
}

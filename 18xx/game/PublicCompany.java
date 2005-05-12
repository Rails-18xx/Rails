/*
 * Created on 05mar2005
 *
 */
package game;

import java.awt.Color;
import java.util.*;

import org.w3c.dom.*;

import util.XmlUtils;

/**
 * This class provides an implementation of a (perhaps only basic) public
 * company. Public companies emcompass all 18xx company-like entities that lay
 * tracks and run trains.
 * <p>
 * Ownership of companies will always be performed by holding certificates. Some
 * minor company types may have only one certificate, but this will still be the
 * form in which ownership is expressed.
 * <p>
 * Company shares may or may not have a price on the stock market.
 * 
 * @author Erik Vos
 */
public class PublicCompany extends Company implements PublicCompanyI,
      CashHolder
{

   protected static int numberOfPublicCompanies = 0;

   /**
    * Foreground (i.e. text) colour of the company tokens (if pictures are not
    * used)
    */
   protected Color fgColour;

   /** Hexadecimal representation (RRGGBB) of the foreground colour. */
   protected String fgHexColour;

   /** Background colour of the company tokens */
   protected Color bgColour;

   /** Hexadecimal representation (RRGGBB) of the background colour. */
   protected String bgHexColour;

   /** Sequence number in the array of public companies - may not be useful */
   protected int publicNumber; // For internal use

   /** Initial (par) share price, represented by a stock market location object */
   protected StockSpaceI parPrice = null;

   /** Current share price, represented by a stock market location object */
   protected StockSpaceI currentPrice = null;

   /** Company treasury, holding cash */
   protected int treasury = 0;
   
   /** Has the company started? */
   protected boolean hasStarted = false;

   /** Revenue earned in the company's previous operating turn. */
   protected int lastRevenue = 0;

   /** Is the company operational ("has it floated")? */
   protected boolean hasFloated = false;

   /** Is the company closed (or bankrupt)? */
   protected boolean closed = false;

   protected boolean canBuyStock = false;

   protected boolean canBuyPrivates = false;

   /** Minimum price for buying privates, to be multiplied by the original price */
   protected float lowerPrivatePriceFactor;

   /** Maximum price for buying privates, to be multiplied by the original price */
   protected float upperPrivatePriceFactor;

   protected boolean ipoPaysOut = false;

   protected boolean poolPaysOut = false;

   protected ArrayList trainsOwned;

   /** The certificates of this company (minimum 1) */
   protected ArrayList certificates;

   /** Privates and Certificates owned by the public company */
   protected Portfolio portfolio;

   /** What percentage of ownership constitutes "one share" */
   protected int shareUnit = 10;

   /**
    * The constructor. The way this class is instantiated does not allow
    * arguments.
    */
   public PublicCompany()
   {
      super();
      this.publicNumber = numberOfPublicCompanies++;
   }

   /** Initialisation, to be called directly after instantiation */
   public void init(String name, CompanyTypeI type)
   {
      super.init(name, type);
      this.portfolio = new Portfolio(name, this);
   }

   /**
    * To configure all public companies from the &lt;PublicCompany&gt; XML
    * element
    */
   public void configureFromXML(Element element) throws ConfigurationException
   {
      NamedNodeMap nnp = element.getAttributes();
      NamedNodeMap nnp2;

      /* Configure public company features */
      fgHexColour = XmlUtils.extractStringAttribute(nnp, "fgColour");
      if (fgHexColour == null)
         fgHexColour = "FFFFFF";
      fgColour = new Color(Integer.parseInt(fgHexColour, 16));

      bgHexColour = XmlUtils.extractStringAttribute(nnp, "bgColour");
      if (bgHexColour == null)
         bgHexColour = "000000";
      bgColour = new Color(Integer.parseInt(bgHexColour, 16));

      /* Complete configuration by adding features from the Public CompanyType */
      Element typeElement = type.getDomElement();

      if (typeElement != null)
      {
         NodeList properties = typeElement.getChildNodes();

         for (int j = 0; j < properties.getLength(); j++)
         {

            String propName = properties.item(j).getNodeName();
            if (propName == null)
               continue;

            if (propName.equalsIgnoreCase("ShareUnit"))
            {
               shareUnit = XmlUtils.extractIntegerAttribute(properties.item(j)
                     .getAttributes(), "percentage", 10);
            }
            else if (propName.equalsIgnoreCase("CanBuyPrivates"))
            {
               canBuyPrivates = true;
               nnp2 = properties.item(j).getAttributes();
               String lower = XmlUtils.extractStringAttribute(nnp2,
                     "lowerPriceFactor");
               if (!XmlUtils.hasValue(lower))
                  throw new ConfigurationException(
                        "Lower private price factor missing");
               lowerPrivatePriceFactor = Float.parseFloat(lower);
               String upper = XmlUtils.extractStringAttribute(nnp2,
                     "upperPriceFactor");
               if (!XmlUtils.hasValue(upper))
                  throw new ConfigurationException(
                        "Upper private price factor missing");
               upperPrivatePriceFactor = Float.parseFloat(upper);

            }
            else if (propName.equalsIgnoreCase("PoolPaysOut"))
            {
               poolPaysOut = true;
            }

         }
      }
   }

   /**
    * Return the company token background colour.
    * 
    * @return Color object
    */
   public Color getBgColour()
   {
      return bgColour;
   }

   /**
    * Return the company token background colour.
    * 
    * @return Hexadecimal string RRGGBB.
    */
   public String getHexBgColour()
   {
      return bgHexColour;
   }

   /**
    * Return the company token foreground colour.
    * 
    * @return Color object.
    */
   public Color getFgColour()
   {
      return fgColour;
   }

   /**
    * Return the company token foreground colour.
    * 
    * @return Hexadecimal string RRGGBB.
    */
   public String getHexFgColour()
   {
      return fgHexColour;
   }

   /**
    * @return
    */
   public boolean canBuyStock()
   {
      return canBuyStock;
   }

	/**
	 * @param hasStarted The hasStarted to set.
	 */
	public void start (StockSpaceI startSpace) {
	    this.hasStarted = true;
	    setParPrice (startSpace);
	}
	
	/**
	 * @return Returns true is the company has started.
	 */
	public boolean hasStarted() {
	    return hasStarted;
	}

	/**
    * Float the company, put its initial cash in the treasury. <i>(perhaps the
    * cash can better be calculated initially?) </i>.
    * 
    * @param cash
    *           The initial cash amount.
    */
   public void setFloated(int cash)
   {
      this.hasFloated = true;
      Bank.transferCash (null, this, cash);
   }

   /**
    * Has the company already floated?
    * 
    * @return true if the company has floated.
    */
   public boolean hasFloated()
   {
      return hasFloated;
   }

   /**
    * Set the company par price.
    * <p><i>Note: this method should <b>not</b> be used to start a company!</i>
    * Use <code><b>start()</b></code> in stead.
    * 
    * @param spaceI
    */
   public void setParPrice(StockSpaceI space)
   {
      parPrice = currentPrice = space;
      space.addToken(this);
   }

   /**
    * Get the company par (initial) price.
    * 
    * @return StockSpace object, which defines the company start position on the
    *         stock chart.
    */
   public StockSpaceI getParPrice()
   {
      return parPrice;
   }

   /**
    * Set a new company price.
    * 
    * @param price
    *           The StockSpace object that defines the new location on the stock
    *           market.
    */
   public void setCurrentPrice(StockSpaceI price)
   {
      currentPrice = price;
   }

   /**
    * Get the current company share price.
    * 
    * @return The StockSpace object that defines the current location on the
    *         stock market.
    */
   public StockSpaceI getCurrentPrice()
   {
      return currentPrice;
   }

   /**
    * @return
    */
   public ArrayList getTrainsOwned()
   {
      return trainsOwned;
   }

   /**
    * Add a given amount to the company treasury.
    * 
    * @param amount
    *           The amount to add (may be negative).
    */
   public void addCash(int amount)
   {
      treasury += amount;
   }

   /**
    * Get the current company treasury.
    * 
    * @return The current cash amount.
    */
   public int getCash()
   {
      return treasury;
   }
   
   public String getFormattedCash () {
       return Bank.format (treasury);
   }


   /**
    * @param list
    */
   public void setTrainsOwned(ArrayList list)
   {
      trainsOwned = list;
   }

   /**
    * @return
    */
   public static int getNumberOfPublicCompanies()
   {
      return numberOfPublicCompanies;
   }

   /**
    * @return
    */
   public int getPublicNumber()
   {
      return publicNumber;
   }

   /**
    * Get a list of this company's certificates.
    * 
    * @return ArrayList containing the certificates (item 0 is the President's
    *         share).
    */
   public List getCertificates()
   {
      return certificates;
   }

   /**
    * Assign a predefined array of certificates to this company.
    * 
    * @param list
    *           ArrayList containing the certificates.
    */
   public void setCertificates(List list)
   {
      certificates = new ArrayList();
      Iterator it = list.iterator();
      PublicCertificateI cert;
      while (it.hasNext())
      {
         cert = ((PublicCertificateI) it.next()).copy();
         certificates.add(cert);
         cert.setCompany(this);
      }
   }

   /**
    * Add a certificate to the end of this company's list of certificates.
    * 
    * @param certificate
    *           The certificate to add.
    */
   public void addCertificate(PublicCertificateI certificate)
   {
      if (certificates == null)
         certificates = new ArrayList();
      certificates.add(certificate);
      certificate.setCompany(this);
   }

   /**
    * Get the Portfolio of this company, containing all privates and
    * certificates owned..
    * 
    * @return The Portfolio of this company.
    */
   public Portfolio getPortfolio()
   {
      return portfolio;
   }
   
   /**
    * Get the company President.
    * 
    */
   public Player getPresident () {
       return (Player) ((PublicCertificateI)certificates.get(0)).getPortfolio().getOwner();
   }

   /**
    * Store the last revenue earned by this company.
    * 
    * @param i
    *           The last revenue amount.
    */
   protected void setLastRevenue(int i)
   {
      lastRevenue = i;
   }

   /**
    * Get the last revenue earned by this company.
    * 
    * @return The last revenue amount.
    */
   public int getLastRevenue()
   {
      return lastRevenue;
   }

   /**
    * Pay out a given amount of revenue (and store it). The amount is
    * distributed to all the certificate holders, or the "beneficiary" if
    * defined (e.g.: BankPool shares may pay to the company).
    * 
    * @param The
    *           revenue amount.
    */
   public void payOut(int amount)
   {

       setLastRevenue(amount);

      Iterator it = certificates.iterator();
      PublicCertificateI cert;
      int part;
      CashHolder recipient;
      Map split = new HashMap();
      while (it.hasNext())
      {
         cert = ((PublicCertificateI) it.next());
         recipient = cert.getPortfolio().getBeneficiary(this);
         part = amount * cert.getShares() * shareUnit / 100;
         // For reporting, we want to add up the amounts per recipient
         if (split.containsKey(recipient))
         {
            part += ((Integer) split.get(recipient)).intValue();
         }
         split.put(recipient, new Integer(part));
      }
      // Report and add the cash
      it = split.keySet().iterator();
      while (it.hasNext())
      {
         recipient = (CashHolder) it.next();
         if (recipient instanceof Bank)
            continue;
         part = ((Integer) split.get(recipient)).intValue();
         Log.write(recipient.getName() + " receives " + part);
         Bank.transferCash(null, recipient, part);
      }

      // Move the token
      Game.getStockMarket().payOut(this);
   }

   /**
    * Withhold a given amount of revenue (and store it).
    * 
    * @param The
    *           revenue amount.
    */
   public void withhold(int amount)
   {

       setLastRevenue(amount);
      Bank.transferCash(null, this, amount);
      // Move the token
      Game.getStockMarket().withhold(this);
   }

   /**
    * Is the company completely sold out?
    * 
    * @return true if no certs are held by the Bank.
    * @TODO: This rule does not apply to all games (1870). Needs be sorted out.
    */
   public boolean isSoldOut()
   {
      Iterator it = certificates.iterator();
      PublicCertificateI cert;
      while (it.hasNext())
      {
         if (((PublicCertificateI) it.next()).getPortfolio().getOwner() instanceof Bank)
         {
            return false;
         }
      }
      return true;
   }

   /**
    * @return
    */
   public boolean canBuyPrivates()
   {
      return canBuyPrivates;
   }

   /**
    * Get the unit of share.
    * 
    * @return The percentage of ownership that is called "one share".
    */
   public int getShareUnit()
   {
      return shareUnit;
   }

   public String toString()
   {
      return name + ", " + publicNumber + " of " + numberOfPublicCompanies;
   }
   
   public static boolean startCompany(String playerName, String companyName, StockSpace startSpace)
   {
      //TODO: Should probably do error checking in case names aren't found.
      Player player = Game.getPlayerManager().getPlayerByName(playerName);
      PublicCompany company = (PublicCompany) Game.getCompanyManager().getPublicCompany(companyName);
      
      PublicCertificate cert = (PublicCertificate) company.certificates.get(0);
      
      if(player.getCash() >= (startSpace.getPrice() * (cert.getShare() / company.getShareUnit())))
      {
         company.setParPrice(startSpace);
         company.setClosed(false);
         int price = startSpace.getPrice() * (cert.getShare() / company.getShareUnit());
         player.buyShare(cert, price);
         
         return true;
      }
      else
         return false;
   }
   
   public PublicCertificateI getNextAvailableCertificate()
   {
      for(int i=0; i < certificates.size(); i++)
      {
         if(((PublicCertificateI)certificates.get(i)).isAvailable())
         {
            return (PublicCertificateI) certificates.get(i);
         }
      }
      return null; 
   }
   /**
    * @return Returns the lowerPrivatePriceFactor.
    */
   public float getLowerPrivatePriceFactor() {
       return lowerPrivatePriceFactor;
   }
   /**
    * @return Returns the upperPrivatePriceFactor.
    */
   public float getUpperPrivatePriceFactor() {
       return upperPrivatePriceFactor;
   }
}

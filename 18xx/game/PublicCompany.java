/*
 * Created on 05mar2005
 *
 */
package game;

import java.util.*;

import org.w3c.dom.*;

import util.XmlUtils;

/**
 * @author Erik Vos
 */
public class PublicCompany extends Company implements PublicCompanyI,
      CashHolder
{

   protected static int numberOfPublicCompanies = 0;

   protected String fgColour;

   protected String bgColour;

   protected int publicNumber; // For internal use

   protected StockSpaceI parPrice = null;

   protected StockSpaceI currentPrice = null;

   protected int treasury = 0;

   protected int lastRevenue = 0;

   protected boolean hasFloated = false;

   protected boolean closed = false;

   protected boolean canBuyStock = false;

   protected boolean canBuyPrivates = false;

   protected float lowerPrivatePriceFactor;

   protected float upperPrivatePriceFactor;

   protected boolean ipoPaysOut = false;

   protected boolean poolPaysOut = false;

   protected ArrayList trainsOwned;

   protected ArrayList certificates;

   protected Portfolio portfolio;

   public PublicCompany()
   {
      super();
      this.publicNumber = numberOfPublicCompanies++;
   }

   public void init(String name, CompanyTypeI type)
   {
      super.init(name, type);
      this.portfolio = new Portfolio(name, this);
   }

   public void configureFromXML(Element element) throws ConfigurationException
   {
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
      if (typeElement != null)
      {
         NodeList properties = typeElement.getChildNodes();

         for (int j = 0; j < properties.getLength(); j++)
         {

            String propName = properties.item(j).getLocalName();
            if (propName == null)
               continue;

            if (propName.equalsIgnoreCase("CanBuyPrivates"))
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

      type.releaseDomElement();
   }

   public void start(StockSpaceI startPrice)
   {
      parPrice = currentPrice = startPrice;
      hasFloated = true;
      parPrice.addToken(this);
   }

   /**
    * @return
    */
   public String getBgColour()
   {
      return bgColour;
   }

   /**
    * @return
    */
   public boolean canBuyStock()
   {
      return canBuyStock;
   }

   /**
    * @return
    */
   public boolean canBuyPrivates()
   {
      return canBuyPrivates;
   }

   /**
    * @return
    */
   public String getFgColour()
   {
      return fgColour;
   }

   /**
    * @return
    */
   public boolean hasFloated()
   {
      return hasFloated;
   }

   /**
    * @return
    */
   public StockSpaceI getParPrice()
   {
      return parPrice;
   }

   /**
    * @return
    */
   public ArrayList getTrainsOwned()
   {
      return trainsOwned;
   }

   /**
    * @return
    */
   public int getCash()
   {
      return treasury;
   }

   /**
    * @param list
    */
   public void setTrainsOwned(ArrayList list)
   {
      trainsOwned = list;
   }

   public void addCash(int amount)
   {
      treasury += amount;
   }

   /**
    * @return
    */
   public StockSpaceI getCurrentPrice()
   {
      return currentPrice;
   }

   /**
    * @param price
    */
   public void setCurrentPrice(StockSpaceI price)
   {
      currentPrice = price;
   }

   /**
    * @param b
    */
   public void setFloated(int cash)
   {
      this.hasFloated = true;
      this.treasury = cash;
      Log.write(name + " floats, treasury cash is " + cash);
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
    * @param i
    */
   public static void setNumberOfCompanies(int i)
   {
      numberOfCompanies = i;
   }

   /**
    * @param string
    */
   public void setBgColour(String string)
   {
      bgColour = string;
   }

   /**
    * @param string
    */
   public void setFgColour(String string)
   {
      fgColour = string;
   }

   /**
    * @return
    */
   public List getCertificates()
   {
      return certificates;
   }

   /**
    * @param list
    */
   public void setCertificates(List list)
   {
      certificates = new ArrayList();
      Iterator it = list.iterator();
      CertificateI cert;
      while (it.hasNext())
      {
         cert = ((CertificateI) it.next()).copy();
         certificates.add(cert);
         cert.setCompany(this);
      }
   }

   public void addCertificate(CertificateI certificate)
   {
      if (certificates == null)
         certificates = new ArrayList();
      certificates.add(certificate);
      certificate.setCompany(this);
   }

   /**
    * @param spaceI
    */
   public void setParPrice(StockSpaceI space)
   {
      parPrice = currentPrice = space;
      space.addToken(this);
   }

   /**
    * @return
    */
   public Portfolio getPortfolio()
   {
      return portfolio;
   }

   /**
    * @return
    */
   public int getLastRevenue()
   {
      return lastRevenue;
   }

   /**
    * @param i
    */
   protected void setLastRevenue(int i)
   {
      lastRevenue = i;
   }

   public void payOut(int amount)
   {

      Log.write(name + " earns " + amount);
      setLastRevenue(amount);

      Iterator it = certificates.iterator();
      CertificateI cert;
      int part;
      CashHolder recipient;
      Map split = new HashMap();
      while (it.hasNext())
      {
         cert = ((CertificateI) it.next());
         recipient = cert.getPortfolio().getBeneficiary(this);
         part = amount * cert.getShare() / 100;
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
      Game.getInstance().getStockMarket().payOut(this);
   }

   public void withhold(int amount)
   {

      Log.write(name + " earns " + amount + " and withholds it");
      setLastRevenue(amount);
      Bank.transferCash(null, this, amount);
      // Move the token
      Game.getInstance().getStockMarket().withhold(this);
   }

   public boolean isSoldOut()
   {
      Iterator it = certificates.iterator();
      CertificateI cert;
      while (it.hasNext())
      {
         if (((CertificateI) it.next()).getPortfolio().getOwner() instanceof Bank)
         {
            return false;
         }
      }
      return true;
   }
}

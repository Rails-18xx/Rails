/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Portfolio.java,v 1.31 2006/06/07 22:21:42 evos Exp $
 *
 * Created on 09-Apr-2005 by Erik Vos
 *
 * Change Log:
 */
package game;

import game.model.ModelObject;
import game.model.PrivatesModel;
import game.model.ShareModel;
import game.model.TrainsModel;
import game.special.SpecialProperty;
import game.special.SpecialPropertyI;

import java.util.*;

import util.Util;

/**
 * @author Erik
 */
public class Portfolio
{

   /** Owned private companies */
   protected List privateCompanies = new ArrayList();
   protected PrivatesModel privatesModel = new PrivatesModel (this);

   /** Owned public company certificates */
   protected List certificates = new ArrayList();

   /** Owned public company certificates, organised in a HashMap per company */
   protected Map certPerCompany = new HashMap();
   protected Map shareModelPerCompany = new HashMap();
   
   /** Owned trains */
   protected List trains = new ArrayList();
   protected Map trainsPerType = new HashMap();
   protected TrainsModel trainsModel = new TrainsModel(this);
   
   
   /** Special properties. It is easier to maintain a map of these
    * that to have to search through the privates on each and every action. */
   protected List specialProperties = new ArrayList();

   /** Who owns the portfolio */
   protected CashHolder owner;

   /** Who receives the dividends (may differ from owner if that is the Bank) */
   protected boolean paysToCompany = false;

   /** Name of portfolio */
   protected String name;

   public Portfolio(String name, CashHolder holder, boolean paysToCompany)
   {
      this.name = name;
      this.owner = holder;
      this.paysToCompany = paysToCompany;
   }

   public Portfolio(String name, CashHolder holder)
   {
      this.name = name;
      this.owner = holder;
   }

   public void buyPrivate(PrivateCompanyI privateCompany, Portfolio from,
         int price)
   {

      if (from == Bank.getIpo()) {
	      Log.write(name + " buys " + privateCompany.getName() 
	              + " for " + Bank.format(price) + ".");
      } else {
	      Log.write(name + " buys " + privateCompany.getName() + " from "
	              + from.getName() + " for " + Bank.format(price) + ".");
      }

      // Move the private certificate
      transferCertificate (privateCompany, from, this);

      // Move the money
      Bank.transferCash(owner, from.owner, price);
   }

   public void buyCertificate(PublicCertificateI certificate, Portfolio from,
         int price)
   {

      // Move the certificate
   	  transferCertificate (certificate, from, this);

      //PublicCertificate is no longer for sale.
      // Erik: this is not the intended use of available (which is now redundant).
      certificate.setAvailable(false);

      // Move the money. 
      if (price != 0) {
      	/* If the company has floated and capitalisation is incremental,
      	 * the money goes to the company, even if the certificates
      	 * are still in the IPO (as in 1835)
      	 */
      	PublicCompanyI comp = certificate.getCompany();
      	CashHolder recipient;
      	if (comp.hasFloated() 
      			&& from.owner == Bank.getIpo()
      			&& comp.getCapitalisation() == PublicCompanyI.CAPITALISE_INCREMENTAL) {
      		recipient = (CashHolder) comp;
      	} else {
      		recipient = from.owner;
      	}
      	Bank.transferCash(owner, recipient, price);
      }
   }

   //Sales of stock always go to the Bank pool
   //This method should be overridden for 1870 and other games
   //that allow price protection.
   public static void sellCertificate(PublicCertificateI certificate, Portfolio from,
         int price)
   {

      Log.write(from.getName() + " sells " + certificate.getShare() + "% of "
            + certificate.getCompany().getName() + " to the Bank for " 
            + Bank.format(price));

      // Move the certificate
      from.removeCertificate(certificate);
      Bank.getPool().addCertificate(certificate);
      certificate.setPortfolio(Bank.getPool());

      //PublicCertificate is for sale again
      certificate.setAvailable(true);

      // Move the money
      Bank.transferCash(Bank.getInstance(), from.owner, price);
   }
   
   public static void transferCertificate (Certificate certificate, 
   			Portfolio from,Portfolio to) {
       if (certificate instanceof PublicCertificateI) {
	       if (from != null) from.removeCertificate((PublicCertificateI)certificate);
	       to.addCertificate((PublicCertificateI)certificate);
       } else if (certificate instanceof PrivateCompanyI) {
           if (from != null) from.removePrivate((PrivateCompanyI)certificate);
           to.addPrivate((PrivateCompanyI)certificate);
       }
   }

   public void addPrivate(PrivateCompanyI privateCompany)
   {
      privateCompanies.add(privateCompany);
      privateCompany.setHolder(this);
      if (privateCompany.getSpecialProperties() != null) {
          updateSpecialProperties();
      }
      privatesModel.update();
   }

   public void addCertificate(PublicCertificateI certificate)
   {
      certificates.add(certificate);
      String companyName = certificate.getCompany().getName();
      if (!certPerCompany.containsKey(companyName))
      {
         certPerCompany.put(companyName, new ArrayList());
      }
      ((ArrayList) certPerCompany.get(companyName)).add(certificate);
      certificate.setPortfolio(this);

      getShareModel (certificate.getCompany()).addShare(certificate.getShare());
   }

   public boolean removePrivate(PrivateCompanyI privateCompany)
   {
      for (int i = 0; i < privateCompanies.size(); i++)
      {
         if (privateCompanies.get(i) == privateCompany)
         {
            privateCompanies.remove(i);
            privatesModel.update();
            if (privateCompany.getSpecialProperties() != null) {
                updateSpecialProperties();
            }
            return true;
         }
      }
      return false;
   }

   public void removeCertificate(PublicCertificateI certificate)
   {
      for (int i = 0; i < certificates.size(); i++)
      {
         if (certificates.get(i) == certificate)
         {
            certificates.remove(i);
         }
      }
      String companyName = certificate.getCompany().getName();
      ArrayList certs = (ArrayList) getCertificatesPerCompany(companyName);
      
      for (int i = 0; i < certs.size(); i++)
      {
         if (certs.get(i) == certificate)
         {
            certs.remove(i);
         }
      }

      getShareModel (certificate.getCompany()).addShare(-certificate.getShare());
  }
   
   public ShareModel getShareModel (PublicCompanyI company) {
       
       if (!shareModelPerCompany.containsKey(company)) {
           shareModelPerCompany.put (company, new ShareModel (this, company));
       }
       return (ShareModel) shareModelPerCompany.get(company);
   }

   public List getPrivateCompanies()
   {
      return privateCompanies;
   }

   public List getCertificates()
   {
      return certificates;
   }
   
   public Map getCertsPerCompanyMap () {
       return certPerCompany;
   }

   public List getCertificatesPerCompany(String compName)
   {
      if (certPerCompany.containsKey(compName))
      {
         return (List) certPerCompany.get(compName);
      }
      else
      {
         //TODO: This is bad. If we don't find the company name
         // we should check to see if certPerCompany has been loaded
         // or possibly throw a config error.
         return new ArrayList();
      }
   }
   
   /**
    * Get a list of unique (i.e. unequal) certificates in this Portfolio
    * at the current price.
    * @return List of unique TradeableCertificate objects.
    */
    public List getUniqueTradeableCertificates () {
       
        List uniqueCerts = new ArrayList();
        PublicCertificateI cert, cert2, prevCert = null;
        TradeableCertificate tCert2;
        Iterator it2;

outer:  for (Iterator it = certificates.iterator(); it.hasNext(); ) {
            cert = (PublicCertificateI) it.next();
            if (cert.equals(prevCert)) continue;
            prevCert = cert;
            for (it2 = uniqueCerts.iterator(); it2.hasNext(); ) {
                tCert2 = (TradeableCertificate) it2.next();
                cert2 = tCert2.getCert();
                if (cert.equals(cert2)) continue outer;
                if (!cert.getCompany().equals(cert2.getCompany())) continue;
                /* From here on we are comparing certs of the same company */
                /* Exclude president share if there also is a non-president share available */
                if (cert.isPresidentShare()) continue outer;
                if (cert2.isPresidentShare()) it2.remove();
                
            }
	        uniqueCerts.add(new TradeableCertificate(cert,
	                cert.getCompany().getCurrentPrice().getPrice() * cert.getShares()));
	    }
        return uniqueCerts;

   }

   /*
   public PublicCertificateI getNextAvailableCertificate()
   {
      for (int i = 0; i < certificates.size(); i++)
      {
         if (((PublicCertificateI) certificates.get(i)).isAvailable())
         {
            return (PublicCertificateI) certificates.get(i);
         }
      }
      return null;
   }
   */
   /**
    * Find a certificate for a given company.
    * @param company The public company for which a certificate is found.
    * @param president Whether we look for a president or non-president certificate.
    * If there is only one certificate, this parameter has no meaning.
    * @return The certificate, or null if not found./
    */
   public PublicCertificateI findCertificate(PublicCompanyI company, boolean president) {
       return findCertificate(company, 1, president);
   }
   
   /** Find a certificate for a given company. */
   public PublicCertificateI findCertificate(PublicCompanyI company, int unit, boolean president)
   {
      String companyName = company.getName();
      if (!certPerCompany.containsKey(companyName)) {
         return null;
      }
      Iterator it = ((List) certPerCompany.get(companyName)).iterator();
      PublicCertificateI cert;
      while (it.hasNext())
      {
         cert = (PublicCertificateI) it.next();
         if (cert.getCompany() == company) {
             if (company.getShareUnit() == 100
             		|| president && cert.isPresidentShare()
                    || !president && !cert.isPresidentShare() && cert.getShares() == unit) {
                 return cert;
             }
         }
      }
      return null;
   }

   /**
    * @return
    */
   public CashHolder getBeneficiary(PublicCompanyI company)
   {
      if (paysToCompany)
      {
         return (CashHolder) company;
      }
      else
      {
         return owner;
      }
   }

   /**
    * @return
    */
   public CashHolder getOwner()
   {
      return owner;
   }

   /**
    * @param object
    */
   public void setOwner(CashHolder owner)
   {
      this.owner = owner;
   }

   /**
    * @return
    */
   public HashMap getCertPerCompany()
   {
      return (HashMap) certPerCompany;
   }

   /**
    * @return
    */
   public String getName()
   {
      return name;
   }

   /**
    * Returns percentage that a portfolio contains of one company.
    * 
    * @param company
    * @return
    */
   public int ownsShare(PublicCompanyI company)
   {
      int share = 0;
      String name = company.getName();
      PublicCertificateI cert;
      if (certPerCompany.containsKey(name))
      {
         Iterator it = ((List) certPerCompany.get(name)).iterator();
         while (it.hasNext())
         {
             cert = (PublicCertificateI) it.next();
            share += cert.getShare();
            //System.out.println(name+": comp="+name+" share="+cert.getShare()+", total="+share);
         }
      }
      return share;
   }

   public int ownsCertificates (PublicCompanyI company, int unit, boolean president) {
       int certs = 0;
       String name = company.getName();
       PublicCertificateI cert;
       if (certPerCompany.containsKey(name)) {
           Iterator it = ((List) certPerCompany.get(name)).iterator();
           while (it.hasNext()) {
               cert = (PublicCertificateI)it.next();
               if (president) {
                   if (cert.isPresidentShare()) return 1;
               } else if (cert.getShares() == unit) {
                   certs++;
               }
           }
       }
       return certs;
   }
   
   /**
    * Swap this Portfolio's President certificate for common shares in another Portfolio.
    * @param company The company whose Presidency is handed over.
    * @param other The new President's portfolio.
    * @return The common certificates returned.
    */
   public List swapPresidentCertificate (PublicCompanyI company, Portfolio other) {

       List swapped = new ArrayList();
       PublicCertificateI swapCert;
       
       // Find the President's certificate
       PublicCertificateI cert = this.findCertificate(company, true);
       if (cert == null) return null;
       int shares = cert.getShares();
       
       // Check if counterparty has enough single certificates
       if (other.ownsCertificates(company, 1, false) >= shares) {
           for (int i=0; i<shares; i++) {
               swapCert = other.findCertificate(company, 1, false);
               Portfolio.transferCertificate (swapCert, other, this);
               swapped.add (swapCert);
               
           }
       } else if (other.ownsCertificates(company, shares, false) >= 1) {
           swapCert = other.findCertificate(company, 2, false);
           Portfolio.transferCertificate(swapCert, other, this);
           swapped.add (swapCert);
       } else {
           return null;
       }
       Portfolio.transferCertificate (cert, this, other);
       return swapped;
   }
   
   public void addTrain (TrainI train) {
       
       trains.add(train);
       TrainTypeI type = train.getType();
       if (trainsPerType.get(type) == null) trainsPerType.put (type, new ArrayList());
       ((List)trainsPerType.get(train.getType())).add(train);
       train.setHolder(this);
       trainsModel.update();
   }
   
   public void removeTrain (TrainI train) {
       trains.remove(train);
       TrainTypeI type = train.getType();
       ((List)trainsPerType.get(train.getType())).remove(train);
       train.setHolder(null);
       trainsModel.update();
       
       /*
       List list = (List)trainsPerType.get(train.getType());
       Iterator it = list.iterator();
       while (it.hasNext()) {
       	if ((TrainI)it.next() == train) {
       		list.remove(train);
       		break;
       	}
       }
       */
   }
   
   public void buyTrain (TrainI train, int price) {
   	CashHolder oldOwner = train.getOwner();
   	transferTrain (train, train.getHolder(), this);
  	Bank.transferCash(owner, oldOwner, price);
   }
   
   public void discardTrain (TrainI train) {
       transferTrain (train, this, Bank.getPool());
       Log.write("Company "+name+" discards "+train.getName()+"-train to Pool");
   }
   
   public static void transferTrain (TrainI train, Portfolio from, Portfolio to) {
   	from.removeTrain(train);
   	to.addTrain(train);
   }
   
   public TrainI[] getTrains () {
       
       return (TrainI[]) trains.toArray(new TrainI[0]);
   }
   
   public TrainI[] getTrainsPerType (TrainTypeI type) {
       
       List trainsFound = new ArrayList();
       for (Iterator it = trains.iterator(); it.hasNext(); ) {
           TrainI train = (TrainI)it.next();
           if (train.getType() == type) trainsFound.add(train);
       }
       
       return (TrainI[]) trainsFound.toArray(new TrainI[0]);
   }
   
   public ModelObject getTrainsModel() {
       return trainsModel;
   }
   
   /** Returns one train of any type held */
   public List getUniqueTrains () {
       
       List trainsFound = new ArrayList();
       Map trainTypesFound = new HashMap();
       for (Iterator it = trains.iterator(); it.hasNext(); ) {
           TrainI train = (TrainI)it.next();
           if (!trainTypesFound.containsKey(train.getType())) {
               trainsFound.add(train);
               trainTypesFound.put(train.getType(), null);
           }
       }
       return trainsFound;
      
   }
   
   public TrainI getTrainOfType (TrainTypeI type) {
       for (Iterator it = trains.iterator(); it.hasNext(); ) {
           TrainI train = (TrainI)it.next();
           if (train.getType() == type) return train;
       }
       return null;
   }
   
   public TrainI getTrainOfType (String name) {
       
       return getTrainOfType (TrainManager.get().getTypeByName(name));
   }
   
   public void updateSpecialProperties () {
       
       if (owner instanceof Player || owner instanceof CompanyI) {
	       specialProperties.clear();
	       Iterator it = privateCompanies.iterator();
	       Iterator it2;
	       PrivateCompanyI priv;
	       List sps;
	       SpecialPropertyI sp;
	       Class clazz;
	       List list;
	       while (it.hasNext()) {
	           priv = (PrivateCompanyI) it.next();
	           sps = priv.getSpecialProperties();
	           if (sps == null) continue;
	           it2 = sps.iterator();
	           while (it2.hasNext()) {
	               sp = (SpecialPropertyI) it2.next();
	               if (sp.isExercised()) continue; 
	               specialProperties.add(sp);
	           }
	       }
       }
   }
   
   public List getSpecialProperties (Class clazz) {
       List result = new ArrayList();
       if (specialProperties != null && specialProperties.size() > 0) {
	       Iterator it = specialProperties.iterator();
	       SpecialProperty sp;
	       while (it.hasNext()) {
	           sp = (SpecialProperty)it.next();
	           if (sp.isExecutionable() 
	                   && Util.isInstanceOf (sp, clazz)) result.add (sp);
	       }
       }
       return result;
   }
   
   public ModelObject getPrivatesModel () {
       return privatesModel;
   }

}

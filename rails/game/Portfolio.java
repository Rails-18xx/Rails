/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Portfolio.java,v 1.17 2007/12/23 16:30:37 evos Exp $
 *
 * Created on 09-Apr-2005 by Erik Vos
 *
 * Change Log:
 */
package rails.game;


import java.util.*;

import org.apache.log4j.Logger;

import rails.game.model.ModelObject;
import rails.game.model.PrivatesModel;
import rails.game.model.ShareModel;
import rails.game.model.TrainsModel;
import rails.game.move.CashMove;
import rails.game.move.Moveable;
import rails.game.move.MoveableHolderI;
import rails.game.special.SpecialPropertyI;
import rails.util.LocalText;
import rails.util.Util;


/**
 * @author Erik
 */
public class Portfolio
implements TokenHolderI, MoveableHolderI
{

	/** Owned private companies */
	protected List<PrivateCompanyI> privateCompanies 
		= new ArrayList<PrivateCompanyI>();
	protected PrivatesModel privatesOwnedModel 
		= new PrivatesModel(privateCompanies);

	/** Owned public company certificates */
	protected List<PublicCertificateI> certificates 
		= new ArrayList<PublicCertificateI>();

	/** Owned public company certificates, organised in a HashMap per company */
	protected Map<String, List<PublicCertificateI>> certPerCompany 
		= new HashMap<String, List<PublicCertificateI>>();
	
	/** Owned public company certificates, organised in a HashMap per
	 * unique certificate type (company, share percentage, presidency).
	 * The key is the certificate type id (see PublicCertificate),
	 * the value is the number of certificates of that type. 
	 */
	protected Map<String, List<PublicCertificateI>> certsPerType 
		= new HashMap<String, List<PublicCertificateI>>();

	/** Share model per company */
	protected Map<PublicCompanyI, ShareModel> shareModelPerCompany 
		= new HashMap<PublicCompanyI, ShareModel>();

	/** Owned trains */
	protected List<TrainI> trains = new ArrayList<TrainI>();
	protected Map<TrainTypeI, List<TrainI>> trainsPerType 
		= new HashMap<TrainTypeI, List<TrainI>>();
	protected TrainsModel trainsModel = new TrainsModel(this);

    /** Owned tokens */
    // TODO Currently only used to discard expired Bonus tokens.
    protected List<TokenI> tokens = new ArrayList<TokenI>();
    
    /** Private-independent special properties. 
     * When moved here, a special property no longer depends
     * on the private company being alive.
     * Example: 18AL named train tokens.
     */
    protected List<SpecialPropertyI> specialProperties;
    
	/** Who owns the portfolio */
	protected CashHolder owner;

	/** Name of portfolio */
	protected String name;
	
	/** A map allowing finding portfolios by name, for use in deserialization */
	protected static Map<String, Portfolio> portfolioMap
			= new HashMap<String, Portfolio> ();

	protected static Logger log = Logger.getLogger(Portfolio.class.getPackage().getName());

	public Portfolio(String name, CashHolder holder)
	{
		this.name = name;
		this.owner = holder;
		portfolioMap.put(name, this);
		
		if (owner instanceof PublicCompanyI) {
			trainsModel.setOption(TrainsModel.FULL_LIST);
			privatesOwnedModel.setOption(PrivatesModel.SPACE);
		} else if (owner instanceof Bank) {
			trainsModel.setOption(TrainsModel.ABBR_LIST);
		} else if (owner instanceof Player) {
			privatesOwnedModel.setOption(PrivatesModel.BREAK);
		}
	}
	
	public static Portfolio getByName (String name) {
		return portfolioMap.get(name);
	}

	public void buyPrivate(PrivateCompanyI privateCompany, Portfolio from,
			int price)
	{

		if (from != Bank.getIpo())
		/* The initial buy is reported from StartRound. 
		 * This message should also move to elsewhere. */
		{
			ReportBuffer.add(LocalText.getText("BuysPrivateFromFor", new String[] {
					name,
					privateCompany.getName(),
					from.getName(),
					Bank.format(price)
			}));
		}

		// Move the private certificate
		//new CertificateMove (from, this, privateCompany);
		privateCompany.moveTo(this);

		// Move the money
		if (price > 0) new CashMove (owner, from.owner, price);
        
        // Move any special abilities, if configured so
		List<SpecialPropertyI> sps = privateCompany.getSpecialProperties();
		if (sps != null) {
			// Need intermediate List to avoid ConcurrentModificationException
			List<SpecialPropertyI> spsToMove = new ArrayList<SpecialPropertyI>(2);
	        for (SpecialPropertyI sp : sps) {
	            if (sp.getTransferText().equalsIgnoreCase("toCompany")
	                    && owner instanceof PublicCompanyI
	             || sp.getTransferText().equalsIgnoreCase("toPlayer")
	                    && owner instanceof Player) {
	            	spsToMove.add (sp);
	            }
	        }
	        for (SpecialPropertyI sp : spsToMove) {
	            log.debug("Moving SP "+sp+" to "+name);
	        	sp.moveTo(this);
	        }
		}
	}

	public void buyCertificate(PublicCertificateI certificate, Portfolio from,
			int price)
	{

		// Move the certificate
	    //new CertificateMove (from, this, certificate);
		certificate.moveTo(this);


		// PublicCertificate is no longer for sale.
		// Erik: this is not the intended use of available (which is now
		// redundant).
		certificate.setAvailable(false);

		// Move the money.
		if (price != 0)
		{
			/*
			 * If the company has floated and capitalisation is incremental, the
			 * money goes to the company, even if the certificates are still in
			 * the IPO (as in 1835)
			 */
			PublicCompanyI comp = certificate.getCompany();
			CashHolder recipient;
			if (comp.hasFloated()
					&& from == Bank.getIpo()
					&& comp.getCapitalisation() == PublicCompanyI.CAPITALISE_INCREMENTAL)
			{
				recipient = (CashHolder) comp;
			}
			else
			{
				recipient = from.owner;
			}
			//Bank.transferCash(owner, recipient, price);
			new CashMove (owner, recipient, price);
		}
	}

	// Sales of stock always go to the Bank pool
	// This method should be overridden for 1870 and other games
	// that allow price protection.
	public static void sellCertificate(PublicCertificateI certificate,
			Portfolio from, int price)
	{

		ReportBuffer.add(LocalText.getText("SellsItemFor", new String[] {
		        from.getName(),
		        certificate.getName(),
				Bank.format(price)
		}));

		// Move the certificate
		//new CertificateMove (from, Bank.getPool(), certificate);
		certificate.moveTo(Bank.getPool());

		// Move the money
		new CashMove (Bank.getInstance(), from.owner, price);
	}

	public static void transferCertificate(Certificate certificate,
			Portfolio from, Portfolio to)
	{
		if (certificate instanceof PublicCertificateI)
		{
			if (from != null)
				from.removeCertificate((PublicCertificateI) certificate);
			to.addCertificate((PublicCertificateI) certificate);
		}
		else if (certificate instanceof PrivateCompanyI)
		{
			if (from != null)
				from.removePrivate((PrivateCompanyI) certificate);
			to.addPrivate((PrivateCompanyI) certificate);
		}
		
		/* Update player's worth */
		if (from.owner instanceof Player) {
			updatePlayerWorth ((Player)from.owner, from, certificate);
		}
		if (to.owner instanceof Player) {
			updatePlayerWorth ((Player)to.owner, to, certificate);
		}
	}
	
	protected static void updatePlayerWorth (Player player, 
			Portfolio portfolio, Certificate certificate) {
		
		PublicCompanyI company;
		
		/* Update player worth */
		player.getWorthModel().update();
		
		/* Make sure that future price changes will update the worth too */
		if (certificate instanceof PublicCertificateI) {
			company = ((PublicCertificateI)certificate).getCompany();
			if (portfolio.certPerCompany.containsKey(company.getName())) {
				company.getCurrentPriceModel().addDependent(player.getWorthModel());
			} else {
				company.getCurrentPriceModel().removeDependent(player.getWorthModel());
			}
		}
	}

	public void addPrivate(PrivateCompanyI privateCompany)
	{
		privateCompanies.add(privateCompany);
		privateCompany.setHolder(this);
		log.debug ("Adding "+privateCompany.getName()+" to portfolio of "+name);
		if (privateCompany.getSpecialProperties() != null)
		{
		    log.debug (privateCompany.getName()+" has special properties!");
		} else {
		    log.debug (privateCompany.getName()+" has no special properties");
		}
		privatesOwnedModel.update();
	}

	public void addCertificate(PublicCertificateI certificate)
	{
	    // When undoing a company start, put the President back at the top.
	    boolean atTop = certificate.isPresidentShare() && this == Bank.getIpo();
	    
	    if (atTop)
	        certificates.add(0, certificate);
	    else 
	        certificates.add(certificate);
	        
		String companyName = certificate.getCompany().getName();
		if (!certPerCompany.containsKey(companyName))
		{
			certPerCompany.put(companyName, new ArrayList<PublicCertificateI>());
		}
		if (atTop)
		    (certPerCompany.get(companyName)).add(0, certificate);
		else
		    (certPerCompany.get(companyName)).add(certificate);
		
		String certTypeId = certificate.getTypeId();
		if (!certsPerType.containsKey(certTypeId)) {
			certsPerType.put(certTypeId, new ArrayList<PublicCertificateI>());
		}
		certsPerType.get(certTypeId).add(certificate);
		
		certificate.setPortfolio(this);

		getShareModel(certificate.getCompany()).addShare(certificate.getShare());
	}

	public boolean removePrivate(PrivateCompanyI privateCompany)
	{
	    boolean removed = privateCompanies.remove(privateCompany);
		if (removed) {
			privatesOwnedModel.update();
		}
		return removed;
	}

	public void removeCertificate(PublicCertificateI certificate)
	{
	    certificates.remove(certificate);

		String companyName = certificate.getCompany().getName();
		
		List certs = (List) getCertificatesPerCompany(companyName);
		certs.remove(certificate);
		
		String certTypeId = certificate.getTypeId();
		if (certsPerType.containsKey(certTypeId)) {
			certsPerType.get(certTypeId).remove(0);
			if (certsPerType.get(certTypeId).isEmpty()) {
				certsPerType.remove(certTypeId);
			}
		}

		getShareModel(certificate.getCompany()).addShare(-certificate.getShare());
	}

	public ShareModel getShareModel(PublicCompanyI company)
	{

		if (!shareModelPerCompany.containsKey(company))
		{
			shareModelPerCompany.put(company, new ShareModel(this, company));
		}
		return (ShareModel) shareModelPerCompany.get(company);
	}

	public List<PrivateCompanyI> getPrivateCompanies()
	{
		return privateCompanies;
	}

	public List<PublicCertificateI> getCertificates()
	{
		return certificates;
	}

	/** Get the number of certificates that count against the certificate limit */
	public int getNumberOfCountedCertificates()
	{

		int number = privateCompanies.size(); // May not hold for all games
		PublicCompanyI comp;

		for (PublicCertificateI cert : certificates)
		{
			comp = cert.getCompany();
			if (!comp.hasFloated()
					|| !comp.hasStockPrice()
					|| !cert.getCompany().getCurrentPrice().isNoCertLimit())
				number++;
		}
		return number;
	}

	public Map<String, List<PublicCertificateI>> getCertsPerCompanyMap()
	{
		return certPerCompany;
	}

	public List<PublicCertificateI> getCertificatesPerCompany(String compName)
	{
		if (certPerCompany.containsKey(compName))
		{
			return certPerCompany.get(compName);
		}
		else
		{
			// TODO: This is bad. If we don't find the company name
			// we should check to see if certPerCompany has been loaded
			// or possibly throw a config error.
			return new ArrayList<PublicCertificateI>();
		}
	}

	/**
	 * Find a certificate for a given company.
	 * 
	 * @param company
	 *            The public company for which a certificate is found.
	 * @param president
	 *            Whether we look for a president or non-president certificate.
	 *            If there is only one certificate, this parameter has no
	 *            meaning.
	 * @return The certificate, or null if not found./
	 */
	public PublicCertificateI findCertificate(PublicCompanyI company,
			boolean president)
	{
		return findCertificate(company, 1, president);
	}

	/** Find a certificate for a given company. */
	public PublicCertificateI findCertificate(PublicCompanyI company, int unit,
			boolean president)
	{
		String companyName = company.getName();
		if (!certPerCompany.containsKey(companyName))
		{
			return null;
		}
		Iterator it = ((List) certPerCompany.get(companyName)).iterator();
		PublicCertificateI cert;
		while (it.hasNext())
		{
			cert = (PublicCertificateI) it.next();
			if (cert.getCompany() == company)
			{
				if (company.getShareUnit() == 100 || president
						&& cert.isPresidentShare() || !president
						&& !cert.isPresidentShare() && cert.getShares() == unit)
				{
					return cert;
				}
			}
		}
		return null;
	}
	
	public Map<String, List<PublicCertificateI>> getCertsPerType() {
		return certsPerType;
	}
	
	public List<PublicCertificateI> getCertsOfType (String certTypeId) {
		if (certsPerType.containsKey(certTypeId)) {
			return certsPerType.get(certTypeId);
		} else {
			return null;
		}
	}

	public PublicCertificateI getCertOfType (String certTypeId) {
		if (certsPerType.containsKey(certTypeId)) {
			return certsPerType.get(certTypeId).get(0);
		} else {
			return null;
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
	public int getShare(PublicCompanyI company)
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
			}
		}
		return share;
	}

	public int ownsCertificates(PublicCompanyI company, int unit,
			boolean president)
	{
		int certs = 0;
		String name = company.getName();
		PublicCertificateI cert;
		if (certPerCompany.containsKey(name))
		{
			Iterator it = ((List) certPerCompany.get(name)).iterator();
			while (it.hasNext())
			{
				cert = (PublicCertificateI) it.next();
				if (president)
				{
					if (cert.isPresidentShare())
						return 1;
				}
				else if (cert.getShares() == unit)
				{
					certs++;
				}
			}
		}
		return certs;
	}

	/**
	 * Swap this Portfolio's President certificate for common shares in another
	 * Portfolio.
	 * 
	 * @param company
	 *            The company whose Presidency is handed over.
	 * @param other
	 *            The new President's portfolio.
	 * @return The common certificates returned.
	 */
	public List<PublicCertificateI> swapPresidentCertificate
			(PublicCompanyI company, Portfolio other)
	{

		List<PublicCertificateI> swapped 
			= new ArrayList<PublicCertificateI>();
		PublicCertificateI swapCert;

		// Find the President's certificate
		PublicCertificateI cert = this.findCertificate(company, true);
		if (cert == null)
			return null;
		int shares = cert.getShares();

		// Check if counterparty has enough single certificates
		if (other.ownsCertificates(company, 1, false) >= shares)
		{
			for (int i = 0; i < shares; i++)
			{
				swapCert = other.findCertificate(company, 1, false);
				//new CertificateMove (other, this, swapCert);
				swapCert.moveTo(this);
				swapped.add(swapCert);

			}
		}
		else if (other.ownsCertificates(company, shares, false) >= 1)
		{
			swapCert = other.findCertificate(company, 2, false);
			//new CertificateMove(other, this, swapCert);
			swapCert.moveTo(this);
			swapped.add(swapCert);
		}
		else
		{
			return null;
		}
		//new CertificateMove (this, other, cert);
		cert.moveTo(other);

		// Make sure the old President is no longer marked as such
		getShareModel(company).setShare();

		return swapped;
	}

	public void addTrain(TrainI train)
	{

		trains.add(train);
		TrainTypeI type = train.getType();
		if (trainsPerType.get(type) == null)
			trainsPerType.put(type, new ArrayList<TrainI>());
		trainsPerType.get(train.getType()).add(train);
		train.setHolder(this);
		trainsModel.update();
	}

	public void removeTrain(TrainI train)
	{
		trains.remove(train);
		trainsPerType.get(train.getType()).remove(train);
		train.setHolder(null);
		trainsModel.update();
	}

	public void buyTrain(TrainI train, int price)
	{
		CashHolder oldOwner = train.getOwner();
		//new TrainMove (train, train.getHolder(), this);
        train.moveTo(this);
		if (price > 0) new CashMove (owner, oldOwner, price);
	}

	public void discardTrain(TrainI train)
	{
		//new TrainMove (train, this, Bank.getPool());
        train.moveTo(Bank.getPool());
		ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain", new String[] {
				name,
				train.getName()
		}));
	}
    
    public void updateTrainsModel() {
        trainsModel.update();
    }

	public int getNumberOfTrains() {
		return trains.size();
	}
    
    public List<TrainI> getTrainList () {
        return trains;
    }

	public TrainI[] getTrainsPerType(TrainTypeI type)
	{

		List<TrainI> trainsFound = new ArrayList<TrainI>();
		for (Iterator it = trains.iterator(); it.hasNext();)
		{
			TrainI train = (TrainI) it.next();
			if (train.getType() == type)
				trainsFound.add(train);
		}

		return (TrainI[]) trainsFound.toArray(new TrainI[0]);
	}

	public ModelObject getTrainsModel()
	{
		return trainsModel;
	}

	/** Returns one train of any type held */
	public List<TrainI> getUniqueTrains()
	{

		List<TrainI> trainsFound = new ArrayList<TrainI>();
		Map<TrainTypeI, Object> trainTypesFound 
			= new HashMap<TrainTypeI, Object>();
		for (Iterator it = trains.iterator(); it.hasNext();)
		{
			TrainI train = (TrainI) it.next();
			if (!trainTypesFound.containsKey(train.getType()))
			{
				trainsFound.add(train);
				trainTypesFound.put(train.getType(), null);
			}
		}
		return trainsFound;

	}

	public TrainI getTrainOfType(TrainTypeI type)
	{
		for (Iterator it = trains.iterator(); it.hasNext();)
		{
			TrainI train = (TrainI) it.next();
			if (train.getType() == type)
				return train;
		}
		return null;
	}

	public TrainI getTrainOfType(String name)
	{

		return getTrainOfType(TrainManager.get().getTypeByName(name));
	}
    
    /** 
     * Add a special property.
     * Used to make special properties independent of the
     * private company that originally held it.
     * @param property The special property object to add.
     * @return True if successful.
     */
    public boolean addSpecialProperty (SpecialPropertyI property) {
        if (specialProperties == null) {
            specialProperties
                = new ArrayList<SpecialPropertyI>(2);
         }
        return specialProperties.add(property);
    }
    
    /** Remove a special property.
     * Not currently used.
     * @param property The special property object to remove.
     * @return True if successful.
     */
    public boolean removeSpecialProperty (SpecialPropertyI property) {
        if (specialProperties != null) {
            return specialProperties.remove(property);
        } else {
            return false;
        }
    }

    /** 
     * Add an object.
     * @param object The object to add.
     * @return True if successful.
     */
    public boolean addObject (Moveable object) {
    	if (object instanceof PublicCertificateI) {
    		addCertificate ((PublicCertificateI)object);
    		return true;
    	} else if (object instanceof PrivateCompanyI) {
    		addPrivate ((PrivateCompanyI) object);
    		return true;
    	} else if (object instanceof TrainI) {
    		addTrain ((TrainI)object);
    		return true;
    	} else if (object instanceof SpecialPropertyI) {
            return addSpecialProperty ((SpecialPropertyI)object);
        } else {
            return false;
        }
    }
    
    /** Remove an object.
     * Not currently used.
     * @param object The object to remove.
     * @return True if successful.
     */
    public boolean removeObject (Moveable object) {
    	if (object instanceof PublicCertificateI) {
    		removeCertificate ((PublicCertificateI)object);
    		return true;
    	} else if (object instanceof PrivateCompanyI) {
    		removePrivate ((PrivateCompanyI) object);
    		return true;
    	} else if (object instanceof TrainI) {
    		removeTrain ((TrainI)object);
    		return true;
    	} else if (object instanceof SpecialPropertyI) {
            return removeSpecialProperty ((SpecialPropertyI)object);
        } else {
            return false;
        }
    }

    /**
     * @return ArrayList of all special properties we have.
     */
    public List<SpecialPropertyI> getSpecialProperties()
    {
        return specialProperties;
    }

    /**
     * Do we have any special properties?
     * 
     * @return Boolean
     */
    public boolean hasSpecialProperties() {
        return specialProperties != null 
            && !specialProperties.isEmpty();
    }
    


	public <T extends SpecialPropertyI> List<T> getSpecialProperties
		(Class<T> clazz, boolean includeExercised)
	{
		List<T> result = new ArrayList<T>();
        List<SpecialPropertyI> sps;

        if (owner instanceof Player || owner instanceof PublicCompanyI) {
            
            for (PrivateCompanyI priv : privateCompanies)
            {
                sps = priv.getSpecialProperties();
                if (sps == null)
                    continue;
    
                for (SpecialPropertyI sp : sps)
        		{
        			if ((clazz == null || Util.isInstanceOf(sp, clazz))
        			        && sp.isExecutionable() 
        			        && (!sp.isExercised() || includeExercised)
        			        && (owner instanceof Company && sp.isUsableIfOwnedByCompany()
        			             || owner instanceof Player && sp.isUsableIfOwnedByPlayer())) {
        			    log.debug ("Adding private SP: "+sp);
        				result.add((T)sp);
        			}
        		}
            }
            
            // Private-independent special properties
            if (specialProperties != null) {
	            for (SpecialPropertyI sp : specialProperties)
	            {
	                if ((clazz == null || Util.isInstanceOf(sp, clazz))
	                        && sp.isExecutionable() 
	                        && (!sp.isExercised() || includeExercised)
	                        && (owner instanceof Company && sp.isUsableIfOwnedByCompany()
	                             || owner instanceof Player && sp.isUsableIfOwnedByPlayer())) {
	                    log.debug ("Adding persistent SP: "+sp);
	                    result.add((T)sp);
	                }
	            }
            }
            
        }

		return result;
	}

	public ModelObject getPrivatesOwnedModel()
	{
		return privatesOwnedModel;
	}
    
    public boolean addToken (TokenI token) {
        return tokens.add(token);
    }
    
    public boolean removeToken (TokenI token) {
        return tokens.remove(token);
    }

    public List<TokenI> getTokens() {
        return tokens;
    }

    public boolean hasTokens() {
        return tokens != null && !tokens.isEmpty();
    }
    
    public void rustObsoleteTrains () {
    	
    	List<TrainI> trainsToRust = new ArrayList<TrainI>();
        for (TrainI train : trains) {
            if (train.isObsolete()) {
            	trainsToRust.add(train);
            }
        }
        // Need to separate selection and execution,
        // otherwise we get a ConcurrentModificationException on trains.
        for (TrainI train : trainsToRust) {
            log.debug("Obsolete train " + train.getUniqueId()
                    + " (owned by " + name
                    + ") rusted");
            train.setRusted();
        }
        trainsModel.update();
    }

}

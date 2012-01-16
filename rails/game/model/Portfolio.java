package rails.game.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import rails.common.LocalText;
import rails.game.Bank;
import rails.game.Bonus;
import rails.game.Company;
import rails.game.GameManager;
import rails.game.Player;
import rails.game.PrivateCompany;
import rails.game.PublicCertificate;
import rails.game.PublicCompany;
import rails.game.ReportBuffer;
import rails.game.Token;
import rails.game.Train;
import rails.game.TrainCertificateType;
import rails.game.TrainType;
import rails.game.special.LocatedBonus;
import rails.game.special.SpecialPropertyI;
import rails.game.state.Item;

// FIXME: Solve id, name and uniquename clashes

/**
 * A Portfolio(Model) stores several HolderModels
 * 
 * For the important MoveAble objects own methods are implemented
 * 
 * All other HolderModels can be added by the general methods
 * 
 * @author evos, freystef (2.0)
 */
public final class Portfolio extends DirectOwner {

    protected static Logger log =
        Logger.getLogger(Portfolio.class.getPackage().getName());

    /** Owner */
    private Owner owner;
    private String ownerName; // TODO: Is this still required?
    
    /** Owned certificates */
    private final CertificatesModel certificates = new CertificatesModel();
    
    /** Owned private companies */
    private final PrivatesModel privates = new PrivatesModel();

    /** Owned trains */
    private TrainsModel trains;

    /** Owned tokens */
    // TODO Currently only used to discard expired Bonus tokens.
    private StorageModel<Token> tokens;
    
    /**
     * Private-independent special properties. When moved here, a special
     * property no longer depends on the private company being alive. Example:
     * 18AL named train tokens.
     */
    private StorageModel<SpecialPropertyI> specialProperties;

    private final GameManager gameManager;

    /**
     * Portfolio is initialized with a default id "Portfolio"
     */
    public Portfolio() {
        super("Portfolio");

        // TODO: Replace this with a better mechanism
        gameManager = GameManager.getInstance();
        gameManager.addPortfolio(this);
    }
     
    public Portfolio init(Item parent) {
        super.init(parent);
        if (parent instanceof Owner) {
            this.owner = (Owner)parent;
            this.ownerName = owner.getId(); // FIXME
            
        } else {
            throw new IllegalArgumentException("Portfolio init() only works for Owner(s)");
        }

        // init models
        certificates.init(owner);
        privates.init(owner);
        trains = TrainsModel.create(owner);
        tokens = StorageModel.create(owner, Token.class);
        specialProperties = StorageModel.create(owner, SpecialPropertyI.class);
        
        // change display style dependent on owner
        if (owner instanceof PublicCompany) {
            trains.setAbbrList(false);
            privates.setLineBreak(false);
        } else if (owner instanceof Bank) {
            trains.setAbbrList(true);
        } else if (owner instanceof Player) {
            privates.setLineBreak(true);
        }
        return this;
    }
    

    public void transferAssetsFrom(Portfolio otherPortfolio) {

        // Move trains
        Owners.moveAll(otherPortfolio, this, Train.class);

        // Move treasury certificates
        Owners.moveAll(otherPortfolio, this, PublicCertificate.class);
    }

    /** Low-level method, only to be called by the local addObject() method and by initialisation code. */
    public void addPrivate(PrivateCompany privateCompany, int position) {

        // add to private Model
        privates.addObject(privateCompany, position);
        
        // change the holder inside the private Company
        privateCompany.setHolder(this);
        
        log.debug("Adding " + privateCompany.getId() + " to portfolio of "
                + owner.getId());
        
        if (privateCompany.getSpecialProperties() != null) {
            log.debug(privateCompany.getId() + " has special properties!");
        } else {
            log.debug(privateCompany.getId() + " has no special properties");
        }

        // TODO: This should not be necessary as soon as a PlayerModel
        updatePlayerWorth ();
    }

    // FIXME: Solve the presidentShare problem, should not be identified at position zero
    
    protected void updatePlayerWorth () {
        if (owner instanceof Player) {
            ((Player)owner).updateWorth();
        }
    }
   
   public CertificatesModel getShareModel(PublicCompany company) {
       // FIXME: This has to rewritten
       return null;
    }

    public ImmutableList<PrivateCompany> getPrivateCompanies() {
        return privates.view();
    }

    public ImmutableSet<PublicCertificate> getCertificates() {
        return certificates.getCertificates();
    }

    /** Get the number of certificates that count against the certificate limit */
    public float getCertificateCount() {

        float number = privates.size(); // TODO: May not hold for all games, for example 1880

        return number + certificates.getCertificateCount();
    }
    
    // TODO: This will be removed as this is certificates itself
/*    public Map<String, List<PublicCertificate>> getCertsPerCompanyMap() {
        return certPerCompany;
    }
*/

    public ImmutableSet<PublicCertificate> getCertificates(PublicCompany company) {
        return certificates.getCertificates(company);
    }

    /**
     * Find a certificate for a given company.
     *
     * @param company The public company for which a certificate is found.
     * @param president Whether we look for a president or non-president
     * certificate. If there is only one certificate, this parameter has no
     * meaning.
     * @return The certificate, or null if not found./
     */
    public PublicCertificate findCertificate(PublicCompany company,
            boolean president) {
        return findCertificate(company, 1, president);
    }

    /** Find a certificate for a given company. */
    public PublicCertificate findCertificate(PublicCompany company,
            int shares, boolean president) {
        if (!certificates.contains(company)) {
            return null;
        }
        for (PublicCertificate cert : certificates.getCertificates(company)) {
            if (cert.getCompany() == company) {
                if (company.getShareUnit() == 100 || president
                        && cert.isPresidentShare() || !president
                        && !cert.isPresidentShare() && cert.getShares() == shares) {
                    return cert;
                }
            }
        }
        return null;
    }

    // FIXME: Rewrite that do use a better structure
/*    public Map<String, List<PublicCertificate>> getCertsPerType() {
        return certsPerType;
    }
*/
    // FIXME: Rewrite using a better structure
/*    public List<PublicCertificate> getCertsOfType(String certTypeId) {
        for (PublicCertificate  )
        if (getCertificates.containsKey(certTypeId)) {
            return certsPerType.get(certTypeId);
        } else {
            return null;
        }
    }
*/
    
    // FIXME: Write using a better structure
/*    public PublicCertificate getCertOfType(String certTypeId) {
        if (certsPerType.containsKey(certTypeId)) {
            return certsPerType.get(certTypeId).get(0);
        } else {
            return null;
        }
    }
*/
    
    public Owner getOwner() {
        return owner;
    }


    // TODO: Check if this is needed and should be supported (owner should be final?)
/*
    public void setOwner(CashHolder owner) {
        this.owner = owner;
    }
*/
    
    /**
     * @return
     */
    public String getId() {
        return null; // FIXME
//        return name;
    }

    /** Get unique name (prefixed by the owners class type, to avoid Bank, Player and Company
     * namespace clashes).
     * @return
     */
    public String getUniqueName () {
        return null; // FIXME: For the unique name
//        return uniqueName;
    }

    /**
     * Returns percentage that a portfolio contains of one company.
     *
     * @param company
     * @return
     */
    public int getShare(PublicCompany company) {
        return certificates.getShare(company);
    }

    public int ownsCertificates(PublicCompany company, int unit,
            boolean president) {
        int certs = 0;
        if (certificates.contains(company)) {
            for (PublicCertificate cert : certificates.getCertificates(company)) {
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
     * Swap this Portfolio's President certificate for common shares in another
     * Portfolio.
     *
     * @param company The company whose Presidency is handed over.
     * @param other The new President's portfolio.
     * @return The common certificates returned.
     */
    public List<PublicCertificate> swapPresidentCertificate(
            PublicCompany company, Portfolio other) {

        List<PublicCertificate> swapped = new ArrayList<PublicCertificate>();
        PublicCertificate swapCert;

        // Find the President's certificate
        PublicCertificate cert = this.findCertificate(company, true);
        if (cert == null) return null;
        int shares = cert.getShares();

        // Check if counterparty has enough single certificates
        if (other.ownsCertificates(company, 1, false) >= shares) {
            for (int i = 0; i < shares; i++) {
                swapCert = other.findCertificate(company, 1, false);
                swapCert.moveTo(this);
                swapped.add(swapCert);

            }
        } else if (other.ownsCertificates(company, shares, false) >= 1) {
            swapCert = other.findCertificate(company, 2, false);
            swapCert.moveTo(this);
            swapped.add(swapCert);
        } else {
            return null;
        }
        cert.moveTo(other);

        // Make sure the old President is no longer marked as such
        // getShareModel(company).setShare();
        getShareModel(company).update(); // FIXME: Is this still required

        return swapped;
    }

    public void discardTrain(Train train) {
        train.moveTo(GameManager.getInstance().getBank().getPool());
        ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                ownerName, train.getId() ));
    }

    // TODO: Is this still needed?
    public void updateTrainsModel() {
        trains.update();
    }

    public int getNumberOfTrains() {
        return trains.size();
    }

    public ImmutableList<Train> getTrainList() {
        return trains.view();
    }

    public Train[] getTrainsPerType(TrainType type) {

        List<Train> trainsFound = new ArrayList<Train>();
        for (Train train : trains) {
            if (train.getType() == type) trainsFound.add(train);
        }

        return trainsFound.toArray(new Train[0]);
    }

    public TrainsModel getTrainsModel() {
        return trains;
    }

    /** Returns one train of any type held */
    public List<Train> getUniqueTrains() {

        List<Train> trainsFound = new ArrayList<Train>();
        Map<TrainType, Object> trainTypesFound =
            new HashMap<TrainType, Object>();
        for (Train train : trains) {
            if (!trainTypesFound.containsKey(train.getType())) {
                trainsFound.add(train);
                trainTypesFound.put(train.getType(), null);
            }
        }
        return trainsFound;

    }

    public Train getTrainOfType(TrainCertificateType type) {
        return trains.getTrainOfType(type);
    }


    /**
     * Add a special property. Used to make special properties independent of
     * the private company that originally held it.
     * Low-level method, only to be called by Move objects.
     *
     * @param property The special property object to add.
     * @return True if successful.
     */
    @Deprecated
    public boolean addSpecialProperty(SpecialPropertyI property, int position) {

        /*
        boolean result = specialProperties.addObject(property, position);
        if (!result) return false;

        property.setOwner(specialProperties);
        */
        // Special case for bonuses with predefined locations
        // TODO Does this belong here?
        // FIXME: This does not belong here as this method is not called anymore from anywhere
        if (owner instanceof PublicCompany && property instanceof LocatedBonus) {
            PublicCompany company = (PublicCompany)owner;
            LocatedBonus locBonus = (LocatedBonus)property;
            Bonus bonus = new Bonus(company, locBonus.getId(), locBonus.getValue(),
                    locBonus.getLocations());
            company.addBonus(bonus);
            ReportBuffer.add(LocalText.getText("AcquiresBonus",
                    owner.getId(),
                    locBonus.getId(),
                    Bank.format(locBonus.getValue()),
                    locBonus.getLocationNameString()));
        }

        return false;
    }
    
    /**
     * Add an object.
     * Low-level method, only to be called by Move objects.
     * @param object The object to add.
     * @return True if successful.
     */
    // TODO: Is this still required?

    /*    public boolean addObject(Holdable object, int position) {
        if (object instanceof PublicCertificate) {
            if (position == null) position = new int[] {-1, -1, -1};
            addCertificate((PublicCertificate) object, position);
            return true;
        } else if (object instanceof PrivateCompany) {
            addPrivate((PrivateCompany) object, position == null ? -1 : position[0]);
            return true;
        } else if (object instanceof Train) {
            if (position == null) position = new int[] {-1, -1, -1};
            addTrain((Train) object, position);
            return true;
        } else if (object instanceof SpecialPropertyI) {
            return addSpecialProperty((SpecialPropertyI) object, position == null ? -1 : position[0]);
        } else if (object instanceof Token) {
            return addToken((Token) object, position == null ? -1 : position[0]);
        } else {
            return false;
        }
    }
*/
    
    /**
     * Remove an object.
     * Low-level method, only to be called by Move objects.
     *
     * @param object The object to remove.
     * @return True if successful.
     */
    // TODO: Is this still required?
/*
    public boolean removeObject(Holdable object) {
        if (object instanceof PublicCertificate) {
            removeCertificate((PublicCertificate) object);
            return true;
        } else if (object instanceof PrivateCompany) {
            removePrivate((PrivateCompany) object);
            return true;
        } else if (object instanceof Train) {
            removeTrain((Train) object);
            return true;
        } else if (object instanceof SpecialPropertyI) {
            return removeSpecialProperty((SpecialPropertyI) object);
        } else if (object instanceof Token) {
            return removeToken((Token) object);
        } else {
            return false;
        }
    }
*/
    
    // TODO: Check if this is still required
/*    public int[] getListIndex (Holdable object) {
        if (object instanceof PublicCertificate) {
            PublicCertificate cert = (PublicCertificate) object;
            return new int[] {
                   certificates.indexOf(object),
                   certPerCompany.get(cert.getCompany().getId()).indexOf(cert),
                   certsPerType.get(cert.getTypeId()).indexOf(cert)
            };
        } else if (object instanceof PrivateCompany) {
            return new int[] {privateCompanies.indexOf(object)};
        } else if (object instanceof Train) {
            Train train = (Train) object;
            return new int[] {
                    trains.indexOf(train),
                    train.getPreviousType() != null ? trainsPerType.get(train.getPreviousType()).indexOf(train) : -1,
                    trainsPerCertType.get(train.getCertType()).indexOf(train)
            };
        } else if (object instanceof SpecialPropertyI) {
            return new int[] {specialProperties.indexOf(object)};
        } else if (object instanceof Token) {
            return new int[] {tokens.indexOf(object)};
        } else {
            return Holdable.AT_END;
        }
    }
*/
    
    /**
     * @return ArrayList of all special properties we have.
     */
    public ImmutableList<SpecialPropertyI> getPersistentSpecialProperties() {
        return specialProperties.view();
    }

    public ImmutableList<SpecialPropertyI> getAllSpecialProperties() {
        ImmutableList.Builder<SpecialPropertyI> sps = new ImmutableList.Builder<SpecialPropertyI>();
        if (specialProperties != null) sps.addAll(specialProperties);
        for (PrivateCompany priv : privates) {
            if (priv.getSpecialProperties() != null) {
                sps.addAll(priv.getSpecialProperties());
            }
        }
        return sps.build();
    }

    /**
     * Do we have any special properties?
     *
     * @return Boolean
     */
    public boolean hasSpecialProperties() {
        return specialProperties != null && !specialProperties.isEmpty();
    }

    public Storage<SpecialPropertyI> getSpecialProperties() {
        return specialProperties;
    }

    @SuppressWarnings("unchecked")
    public <T extends SpecialPropertyI> List<T> getSpecialProperties(
            Class<T> clazz, boolean includeExercised) {
        List<T> result = new ArrayList<T>();
        List<SpecialPropertyI> sps;

        if (owner instanceof Player || owner instanceof PublicCompany) {

            for (PrivateCompany priv : privates) {

                sps = priv.getSpecialProperties();
                if (sps == null) continue;

                for (SpecialPropertyI sp : sps) {
                    if ((clazz == null || clazz.isAssignableFrom(sp.getClass()))
                            && sp.isExecutionable()
                            && (!sp.isExercised() || includeExercised)
                            && (owner instanceof Company && sp.isUsableIfOwnedByCompany()
                                    || owner instanceof Player && sp.isUsableIfOwnedByPlayer())) {
                        log.debug("Portfolio "+ownerName+" has SP " + sp);
                        result.add((T) sp);
                    }
                }
            }

            // Private-independent special properties
            if (specialProperties != null) {
                for (SpecialPropertyI sp : specialProperties) {
                    if ((clazz == null || clazz.isAssignableFrom(sp.getClass()))
                            && sp.isExecutionable()
                            && (!sp.isExercised() || includeExercised)
                            && (owner instanceof Company && sp.isUsableIfOwnedByCompany()
                                    || owner instanceof Player && sp.isUsableIfOwnedByPlayer())) {
                        log.debug("Portfolio "+ownerName+" has persistent SP " + sp);
                        result.add((T) sp);
                    }
                }
            }

        }

        return result;
    }

    public PrivatesModel getPrivatesOwnedModel() {
        return privates;
    }

    public StorageModel<Token> getTokenHolder() {
        return tokens;
    }
    
    /** Low-level method, only to be called by the local addObject() method. */
    public boolean addToken(Token token, int position) {
        tokens.addObject(token, position);
        return true;
    }

    /** Low-level method, only to be called by the local addObject() method. */
    public boolean removeToken(Token token) {
        return tokens.removeObject(token);
    }

    public boolean hasTokens() {
        return tokens != null && !tokens.isEmpty();
    }

    public void rustObsoleteTrains() {

        List<Train> trainsToRust = new ArrayList<Train>();
        for (Train train : trains) {
            if (train.isObsolete()) {
                trainsToRust.add(train);
            }
        }
        // Need to separate selection and execution,
        // otherwise we get a ConcurrentModificationException on trains.
        for (Train train : trainsToRust) {
            ReportBuffer.add(LocalText.getText("TrainsObsoleteRusted",
                    train.getId(), ownerName));
            log.debug("Obsolete train " + train.getUniqueId() + " (owned by "
                    + ownerName + ") rusted");
            train.setRusted();
        }
        // TODO: Still required?
        trains.update();
    }

    
    // FIXME: This mechanism has to be rewritten
    public Map<String, List<PublicCertificate>> getCertsPerCompanyMap() {
        return null;
    }

    // FIXME: This mechanism has to be rewritten
    public AbstractOwnable getCertOfType(String string) {
        // TODO Auto-generated method stub
        return null;
    }


}

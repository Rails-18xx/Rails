package rails.game.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;

import rails.common.LocalText;
import rails.game.Bank;
import rails.game.BankPortfolio;
import rails.game.Bonus;
import rails.game.BonusToken;
import rails.game.Company;
import rails.game.Currency;
import rails.game.GameManager;
import rails.game.Player;
import rails.game.PrivateCompany;
import rails.game.PublicCertificate;
import rails.game.PublicCompany;
import rails.game.ReportBuffer;
import rails.game.Train;
import rails.game.TrainCertificateType;
import rails.game.TrainType;
import rails.game.special.LocatedBonus;
import rails.game.special.SpecialProperty;
import rails.game.state.Model;
import rails.game.state.Owner;
import rails.game.state.Portfolio;
import rails.game.state.PortfolioSet;

// FIXME: Solve id, name and uniquename clashes

/**
 * A Portfolio(Model) stores several portfolios
 */
public class PortfolioModel extends Model {
    public static final String ID = "PortfolioModel";
    
    protected static Logger log =
        LoggerFactory.getLogger(PortfolioModel.class);
    
    /** Owned certificates */
    private final CertificatesModel certificates;
    
    /** Owned private companies */
    private final PrivatesModel privates;

    /** Owned trains */
    private final TrainsModel trains;

    /** Owned tokens */
    // TODO Currently only used to discard expired Bonus tokens.
    private final Portfolio<BonusToken> bonusTokens;
    
    /**
     * Private-independent special properties. When moved here, a special
     * property no longer depends on the private company being alive. Example:
     * 18AL named train tokens.
     */
    private final Portfolio<SpecialProperty> specialProperties;  

    private final GameManager gameManager;

    private PortfolioModel(Owner parent, String id) {
        super(parent, id);

        // TODO: Replace this with a better mechanism
        gameManager = GameManager.getInstance();

        // create internal models and portfolios
        certificates = CertificatesModel.create(parent);
        privates = PrivatesModel.create(parent);
        trains = TrainsModel.create(parent);
        bonusTokens = PortfolioSet.create(parent, "BonusTokens", BonusToken.class);
        specialProperties = PortfolioSet.create(parent, "SpecialProperties", SpecialProperty.class);
        
        // change display style dependent on owner
        if (parent instanceof PublicCompany) {
            trains.setAbbrList(false);
            privates.setLineBreak(false);
        } else if (parent instanceof BankPortfolio) {
            trains.setAbbrList(true);
        } else if (parent instanceof Player) {
            privates.setLineBreak(true);
        }

        gameManager.addPortfolio(this);
    }
    
    public static PortfolioModel create(PortfolioOwner parent) {
        return new PortfolioModel(parent, ID);
    }

    public void finishConfiguration() {
        certificates.initShareModels(gameManager.getAllPublicCompanies());
    }
        
    @Override
    public PortfolioOwner getParent() {
        return (PortfolioOwner)super.getParent();
    }
    
    public void transferAssetsFrom(PortfolioModel otherPortfolio) {

        // Move trains
        otherPortfolio.getTrainsModel().getPortfolio().moveAll(this.getParent());

        // Move treasury certificates
        otherPortfolio.moveAllCertificates(this.getParent());
    }

    /** Low-level method, only to be called by the local addObject() method and by initialisation code. */
    // TODO: Ignores position now, is this necessary?
    public void addPrivateCompany(PrivateCompany company) {

        // add to private Model
        privates.moveInto(company);
        
        if (company.getSpecialProperties() != null) {
            log.debug(company.getId() + " has special properties!");
        } else {
            log.debug(company.getId() + " has no special properties");
        }

        // TODO: This should not be necessary as soon as a PlayerModel works correctly
        updatePlayerWorth ();
    }

    // FIXME: Solve the presidentShare problem, should not be identified at position zero
    
    protected void updatePlayerWorth () {
        if (getParent() instanceof Player) {
            ((Player)getParent()).updateWorth();
        }
    }
   
   public CertificatesModel getCertificatesModel() {
       return certificates;
   }
   
   public ShareModel getShareModel(PublicCompany company) {
       return certificates.getShareModel(company);
   }
   
    public ImmutableSet<PrivateCompany> getPrivateCompanies() {
        return privates.getPortfolio().items();
    }

    public ImmutableSortedSet<PublicCertificate> getCertificates() {
        return certificates.getPortfolio().items();
    }

    /** Get the number of certificates that count against the certificate limit */
    public float getCertificateCount() {

        float number = privates.getPortfolio().size(); // TODO: May not hold for all games, for example 1880

        return number + certificates.getCertificateCount();
    }

    public ImmutableSetMultimap<PublicCompany, PublicCertificate> getCertsPerCompanyMap() {
        return certificates.getPortfolio().view();
    }

    public ImmutableSortedSet<PublicCertificate> getCertificates(PublicCompany company) {
        return certificates.getPortfolio().items(company);
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

    /** Find a specified certificate
     * @return (first) certificate found, null if not found */
    public PublicCertificate findCertificate(PublicCompany company,
            int shares, boolean president) {
        for (PublicCertificate cert : certificates.getPortfolio().items(company)) {
            if (company.getShareUnit() == 100 || president
                    && cert.isPresidentShare() || !president
                    && !cert.isPresidentShare() && cert.getShares() == shares) {
                return cert;
            }
        }
        return null;
    }

    public ImmutableList<PublicCertificate> getCertsOfType(String certTypeId) {
        Builder<PublicCertificate> list = ImmutableList.builder();
        for (PublicCertificate cert : certificates) {
            if (cert.getTypeId().equals(certTypeId)) {
                list.add(cert);
            }
        }
        return list.build();
    }
    
   public PublicCertificate getAnyCertOfType(String certTypeId) {
       for (PublicCertificate cert : certificates) {
           if (cert.getTypeId().equals(certTypeId)) {
               return cert;
           }
       }
       return null;
    }
    
    /**
     * Returns percentage that a portfolio contains of one company.
     */
    public int getShare(PublicCompany company) {
        return certificates.getShare(company);
    }

    public int ownsCertificates(PublicCompany company, int unit,
            boolean president) {
        int certs = 0;
        if (certificates.contains(company)) {
            for (PublicCertificate cert : certificates.getPortfolio().items(company)) {
                if (president) {
                    if (cert.isPresidentShare()) return 1;
                } else if (cert.getShares() == unit) {
                    certs++;
                }
            }
        }
        return certs;
    }
    
    public void moveAllCertificates(Owner owner) {
        certificates.getPortfolio().moveAll(owner);
    }

    /**
     * Swap this Portfolio's President certificate for common shares in another
     * Portfolio.
     *
     * @param company The company whose Presidency is handed over.
     * @param other The new President's portfolio.
     * @return The common certificates returned.
     * 
     * TODO: Rewrite the method replacing PortfolioModel with Owner
     */
    public List<PublicCertificate> swapPresidentCertificate(
            PublicCompany company, PortfolioModel other) {

        // FIXME: Rewrite this parrt
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
                swapCert.moveTo(getParent());
                swapped.add(swapCert);
            }
        } else if (other.ownsCertificates(company, shares, false) >= 1) {
            swapCert = other.findCertificate(company, 2, false);
            swapCert.moveTo(getParent());
            swapped.add(swapCert);
        } else {
            return null;
        }
        cert.moveTo(other.getParent());

        return swapped;
    }

    public void discardTrain(Train train) {
        // FIXME: This is a horrible list of method calls
        GameManager.getInstance().getBank().getPool().getPortfolioModel().getTrainsModel().getPortfolio().moveInto(train);
        
        
        ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                getParent().getId(), train.getId() ));
    }

    // FIXME: Is this still needed?
    public void updateTrainsModel() {
        // trains.update();
    }

    public int getNumberOfTrains() {
        return trains.getPortfolio().size();
    }

    public ImmutableSet<Train> getTrainList() {
        return trains.getPortfolio().items();
    }

    public Train[] getTrainsPerType(TrainType type) {

        List<Train> trainsFound = new ArrayList<Train>();
        for (Train train : trains.getPortfolio()) {
            if (train.getType() == type) trainsFound.add(train);
        }

        return trainsFound.toArray(new Train[0]);
    }

    public TrainsModel getTrainsModel() {
        return trains;
    }

    /** Returns one train of any type held */
    public Set<Train> getUniqueTrains() {

        Set<Train> trainsFound = new HashSet<Train>();
        Map<TrainType, Object> trainTypesFound =
            new HashMap<TrainType, Object>();
        for (Train train : trains.getPortfolio()) {
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
     * Add a train to the train portfolio
     */
    public boolean addTrain(Train train) {
        return trains.getPortfolio().moveInto(train);
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
    public boolean addSpecialProperty(SpecialProperty property, int position) {

        /*
        boolean result = specialProperties.addObject(property, position);
        if (!result) return false;

        property.setOwner(specialProperties);
        */
        // Special case for bonuses with predefined locations
        // TODO Does this belong here?
        // FIXME: This does not belong here as this method is not called anymore from anywhere
        if (getParent() instanceof PublicCompany && property instanceof LocatedBonus) {
            PublicCompany company = (PublicCompany)getParent();
            LocatedBonus locBonus = (LocatedBonus)property;
            Bonus bonus = new Bonus(company, locBonus.getId(), locBonus.getValue(),
                    locBonus.getLocations());
            company.addBonus(bonus);
            ReportBuffer.add(LocalText.getText("AcquiresBonus",
                    getParent().getId(),
                    locBonus.getId(),
                    Currency.format(getParent(), locBonus.getValue()),
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
        } else if (object instanceof SpecialProperty) {
            return addSpecialProperty((SpecialProperty) object, position == null ? -1 : position[0]);
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
        } else if (object instanceof SpecialProperty) {
            return removeSpecialProperty((SpecialProperty) object);
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
        } else if (object instanceof SpecialProperty) {
            return new int[] {specialProperties.indexOf(object)};
        } else if (object instanceof Token) {
            return new int[] {tokens.indexOf(object)};
        } else {
            return Holdable.AT_END;
        }
    }
*/
    
    /**
     * @return Set of all special properties we have.
     */
    public ImmutableSet<SpecialProperty> getPersistentSpecialProperties() {
        return specialProperties.items();
    }

    public ImmutableList<SpecialProperty> getAllSpecialProperties() {
        ImmutableList.Builder<SpecialProperty> sps = new ImmutableList.Builder<SpecialProperty>();
        if (specialProperties != null) sps.addAll(specialProperties);
        for (PrivateCompany priv : privates.getPortfolio()) {
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

    public Portfolio<SpecialProperty> getSpecialProperties() {
        return specialProperties;
    }

    
    // TODO: Check if this code can be simplified
    @SuppressWarnings("unchecked")
    public <T extends SpecialProperty> List<T> getSpecialProperties(
            Class<T> clazz, boolean includeExercised) {
        List<T> result = new ArrayList<T>();
        Set<SpecialProperty> sps;

        if (getParent() instanceof Player || getParent() instanceof PublicCompany) {

            for (PrivateCompany priv : privates.getPortfolio()) {

                sps = priv.getSpecialProperties();
                if (sps == null) continue;

                for (SpecialProperty sp : sps) {
                    if ((clazz == null || clazz.isAssignableFrom(sp.getClass()))
                            && sp.isExecutionable()
                            && (!sp.isExercised() || includeExercised)
                            && (getParent() instanceof Company && sp.isUsableIfOwnedByCompany()
                                    || getParent()instanceof Player && sp.isUsableIfOwnedByPlayer())) {
                        log.debug("Portfolio "+getParent().getId()+" has SP " + sp);
                        result.add((T) sp);
                    }
                }
            }

            // Private-independent special properties
            if (specialProperties != null) {
                for (SpecialProperty sp : specialProperties) {
                    if ((clazz == null || clazz.isAssignableFrom(sp.getClass()))
                            && sp.isExecutionable()
                            && (!sp.isExercised() || includeExercised)
                            && (getParent() instanceof Company && sp.isUsableIfOwnedByCompany()
                                    || getParent() instanceof Player && sp.isUsableIfOwnedByPlayer())) {
                        log.debug("Portfolio "+getParent().getId()+" has persistent SP " + sp);
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

    public boolean addBonusToken(BonusToken token){
        return bonusTokens.moveInto(token);
    }
    
    public Portfolio<BonusToken> getTokenHolder() {
        return bonusTokens;
    }
    
    public void rustObsoleteTrains() {

        List<Train> trainsToRust = new ArrayList<Train>();
        for (Train train : trains.getPortfolio()) {
            if (train.isObsolete()) {
                trainsToRust.add(train);
            }
        }
        // Need to separate selection and execution,
        // otherwise we get a ConcurrentModificationException on trains.
        for (Train train : trainsToRust) {
            ReportBuffer.add(LocalText.getText("TrainsObsoleteRusted",
                    train.getId(), getParent().getId()));
            log.debug("Obsolete train " + train.getId() + " (owned by "
                    + getParent().getId() + ") rusted");
            train.setRusted();
        }
        // FIXME:: Still required?
        // trains.update();
    }

    /**
     * Used to identify portfolios on reload
     */
    @Deprecated
    public String getName() {
        return getParent().getId();
    }
    
    /**
     * Used to identify portfolios on reload
     * TODO: Remove that in the future
     */
    @Deprecated
    public String getUniqueName () {
        // For BankPortfolios use Bank
        if (getParent() instanceof BankPortfolio) {
            return Bank.class.getSimpleName() + "_" + getParent().getId();
        }
        return getParent().getClass().getSimpleName() + "_" + getParent().getId();
    }

}

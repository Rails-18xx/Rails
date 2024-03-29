package net.sf.rails.game.model;

import java.util.*;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedMultiset;


// FIXME: Solve id, name and uniquename clashes

/**
 * A Portfolio(Model) stores several portfolios
 */
public class PortfolioModel extends RailsModel {
    public static final String ID = "PortfolioModel";

    private static final Logger log = LoggerFactory.getLogger(PortfolioModel.class);

    /** Owned certificates */
    private final CertificatesModel certificates;

    /** Owned Bonds */
    private final HashMapState<PublicCompany, BondsModel> bonds;

    /** Owned private companies */
    private final PrivatesModel privates;

    /** Owned train cards */
    private final TrainsModel trainCards;

    /* /** Owned trains */
    //private final TrainsModel trains;

    /** Owned tokens */
    // TODO Currently only used to discard expired Bonus tokens.
    private final Portfolio<BonusToken> bonusTokens;

    /**
     * Private-independent special properties. When moved here, a special
     * property no longer depends on the private company being alive. Example:
     * 18AL named train tokens.
     */
    private final SpecialPropertiesModel specialProperties;

    private PortfolioModel(RailsOwner parent, String id) {
        super(parent, id);

        // create internal models and portfolios
        certificates = CertificatesModel.create(parent);
        bonds = HashMapState.create (parent, parent.getId() + "_bondModels");
        privates = PrivatesModel.create(parent);
        //trains = TrainsModel.create(parent);
        trainCards = TrainsModel.create(parent);
        bonusTokens = PortfolioSet.create(parent, "BonusTokens", BonusToken.class);
        specialProperties = SpecialPropertiesModel.create(parent);

        // change display style dependent on owner
        if (parent instanceof PublicCompany) {
            //trains.setAbbrList(false);
            trainCards.setAbbrList(false);
            privates.setLineBreak(false);
        } else if (parent instanceof BankPortfolio) {
            //trains.setAbbrList(true);
            trainCards.setAbbrList(true);
        } else if (parent instanceof Player) {
            privates.setLineBreak(true);
        }

    }

    public static PortfolioModel create(PortfolioOwner parent) {
        return new PortfolioModel(parent, ID);
    }

    public void finishConfiguration() {
        certificates.initShareModels(getRoot().getCompanyManager().getAllPublicCompanies());
        getRoot().getPortfolioManager().addPortfolio(this);
    }

    @Override
    public PortfolioOwner getParent() {
        return (PortfolioOwner) super.getParent();
    }

    // returns the associated MoneyOwner
    public MoneyOwner getMoneyOwner() {
        if (getParent() instanceof BankPortfolio) {
            return ((BankPortfolio) getParent()).getParent();
        }
        return (MoneyOwner) getParent();
    }

    public void transferAssetsFrom(PortfolioModel otherPortfolio) {

        // Move trains
        otherPortfolio.getTrainsModel().getPortfolio().moveAll(this.getParent());

        // Move treasury certificates
        otherPortfolio.moveAllCertificates(this.getParent());
    }

    /**
     * Low-level method, only to be called by the local addObject() method and
     * by initialisation code.
     */
    // TODO: Ignores position now, is this necessary?
    public void addPrivateCompany(PrivateCompany company) {

        // add to private Model
        privates.moveInto(company);

        if (company.hasSpecialProperties()) {
            log.debug("{} has special properties!", company.getId());
        } else {
            log.debug("{} has no special properties", company.getId());
        }

        // TODO: This should not be necessary as soon as a PlayerModel works
        // correctly
        updatePlayerWorth();
    }

    // FIXME: Solve the presidentShare problem, should not be identified at position zero

    protected void updatePlayerWorth() {
        if (getParent() instanceof Player) {
            ((Player) getParent()).updateWorth();
        }
    }

    /* Certificates and shares */
    public CertificatesModel getCertificatesModel() {
        return certificates;
    }

    public ShareModel getShareModel(PublicCompany company) {
        return certificates.getShareModel(company);
    }

    public ImmutableSortedSet<PublicCertificate> getCertificates() {
        return certificates.getPortfolio().items();
    }

    public ShareDetailsModel getShareDetailsModel(PublicCompany company) {
        return certificates.getShareDetailsModel(company);
    }

    /** Get the number of certificates that count against the certificate limit */
    public float getCertificateCount() {
        return privates.getCertificateCount() +  certificates.getCertificateCount();
    }

    public ImmutableSetMultimap<PublicCompany, PublicCertificate> getCertsPerCompanyMap() {
        return certificates.getPortfolio().view();
    }

    public ImmutableSortedSet<PublicCertificate> getCertificates(
            PublicCompany company) {
        return certificates.getCertificates(company);
    }

    /* Bonds */
    public BondsModel getBondsModel (PublicCompany company) {
        if (!bonds.containsKey(company)) bonds.put (company, BondsModel.create(getParent(), company));
        return bonds.get(company);
    }

    public int getBondsCount(PublicCompany company) {
        return getBondsModel(company).getBondsCount();
    }

    /* Privates */
    public ImmutableSet<PrivateCompany> getPrivateCompanies() {
        return privates.getPortfolio().items();
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

    /**
     * Find a specified certificate
     *
     * @return (first) certificate found, null if not found
     */
    public PublicCertificate findCertificate(PublicCompany company, int shares, boolean president) {
        log.debug("Looking in {} for {} {} shares, pres={}",
                this, company, shares, president);
        for (PublicCertificate cert : certificates.getPortfolio().items(company)) {
            if (company.getShareUnit() == 100
                    || president && cert.isPresidentShare()
                    || !president && !cert.isPresidentShare() && cert.getShares() == shares) {
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

    /**
     * @return a sorted Multiset<Integer> of shareNumbers of the certificates
     * Remark: excludes the presdident share if not of a different size as the standard share...
     */
    // FIXME: Integers could be replaced later by CertificateTypes
    public SortedMultiset<Integer> getCertificateTypeCounts(PublicCompany company) {
        return certificates.getCertificateTypeCounts(company);
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


    /**
     * @return the number of shares owned by the PortfolioModel for this company
     */
    public int getShares(PublicCompany company) {
        return certificates.getShareNumber(company);
    }

    /**
     * @param maxShareNumber maximum share number that is to achieved
     * @return sorted list of share numbers that are possible for that company
     */
   public SortedSet<Integer> getShareNumberCombinations(PublicCompany company, int maxShareNumber) {
        return certificates.getshareNumberCombinations(company, maxShareNumber);
    }

   /**
    * @return true if portfolio contains a multiple (non-president) certificate
    */
    public boolean containsMultipleCert(PublicCompany company) {
        return certificates.containsMultipleCert(company);
    }


    public int ownsCertificates(PublicCompany company, int unit, boolean president) {
        int certs = 0;
        if (certificates.contains(company)) {
            for (PublicCertificate cert : certificates.getPortfolio().items(
                    company)) {
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
     */
    public List<PublicCertificate> swapPresidentCertificate(
            PublicCompany company, PortfolioModel other) {
        return swapPresidentCertificate (company, other, 0);
    }

    public List<PublicCertificate> swapPresidentCertificate(
            PublicCompany company, PortfolioModel other, int swapShareSize) {

        List<PublicCertificate> swapped = new ArrayList<>();
        PublicCertificate swapCert;

        // Find the President's certificate
        PublicCertificate presCert = this.findCertificate(company, true);
        if (presCert == null) return null;
        int shares = presCert.getShares();

        // If a double cert is requested, try that first
        if (swapShareSize > 1 && other.ownsCertificates(company, swapShareSize, false)*swapShareSize >= shares) {
            swapCert = other.findCertificate(company, swapShareSize, false);
            swapCert.moveTo(this.getParent());
            swapped.add(swapCert);
        } else if (other.ownsCertificates(company, 1, false) >= shares) {
            // Check if counterparty has enough single certificates
            for (int i = 0; i < shares; i++) {
                swapCert = other.findCertificate(company, 1, false);
                swapCert.moveTo(this.getParent());
                swapped.add(swapCert);

            }
        } else if (other.ownsCertificates(company, shares, false) >= 1) {
            swapCert = other.findCertificate(company, 2, false);
            swapCert.moveTo(this.getParent());
            swapped.add(swapCert);
        } else {
            return null;
        }
        presCert.moveTo(other.getParent());

        return swapped;
    }

    public int getNumberOfTrains() {
        return trainCards.getPortfolio().size();
    }

    public Set<Train> getTrainList() {
        Set<Train> trains = new LinkedHashSet<>();
        for (TrainCard card : trainCards.getPortfolio()) {
            if (card.getActualTrain() != null) {
                trains.add(card.getActualTrain());
            } else {
                trains.addAll(card.getTrains());
            }
        }
        return trains;
    }

    public Train[] getTrainsPerType(TrainType type) {

        List<Train> trainsFound = new ArrayList<>();
        for (Train train : getTrainList()) {
            if (train.getType() == type) trainsFound.add(train);
        }

        return trainsFound.toArray(new Train[0]);
    }

    public TrainsModel getTrainsModel() {
        return trainCards;
    }
    /** Returns one train of any type held */
    public Set<Train> getUniqueTrains() {
        ImmutableSortedSet.Builder<Train> trainsFound = ImmutableSortedSet.naturalOrder();
        Set<TrainType> trainTypesFound = Sets.newHashSet();
        for (Train train : getTrainList()) {
            if (!trainTypesFound.contains(train.getType())) {
                trainsFound.add(train);
                trainTypesFound.add(train.getType());
            }
        }
        return trainsFound.build();
    }

    public Train getTrainOfType(TrainType trainType) {
        return trainCards.getTrainOfType(trainType);
    }

    public TrainCard getTrainCardOfType(TrainCardType trainCardType) {
        return trainCards.getTrainCardOfType(trainCardType);
    }


    /**
     * Add a train to the train portfolio
     * Only train cards will be moved,
     * use addTrainCard(); its trains will follow automatically.
     */
    @Deprecated
    public void addTrain(Train train) {
        //trains.getPortfolio().add(train);
        addTrainCard(train.getCard());
    }

    /**
     * Add a train card to the portfolio.
     * The actual trains are defined via the cards.
     * @param card A train card to be added to the portfolio
      */
    public void addTrainCard (TrainCard card) {
        trainCards.getPortfolio().add(card);
    }

    /**
     * @return Set of all special properties we have.
     */
    public ImmutableSet<SpecialProperty> getPersistentSpecialProperties() {
        return specialProperties.getPortfolio().items();
    }

    public ImmutableList<SpecialProperty> getAllSpecialProperties() {
        ImmutableList.Builder<SpecialProperty> sps =
                new ImmutableList.Builder<>();
        sps.addAll(specialProperties.getPortfolio().items());
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
        return !specialProperties.getPortfolio().isEmpty();
    }

    // TODO: Check if this code can be simplified
    @SuppressWarnings("unchecked")
    public <T extends SpecialProperty> List<T> getSpecialProperties(Class<T> clazz, boolean includeExercised) {
        List<T> result = new ArrayList<>();
        Set<SpecialProperty> sps;

        if (getParent() instanceof Player
            || getParent() instanceof PublicCompany) {

            for (PrivateCompany priv : privates.getPortfolio()) {

                sps = priv.getSpecialProperties();
                if (sps == null) continue;

                for (SpecialProperty sp : sps) {
                    if ((clazz == null || clazz.isAssignableFrom(sp.getClass()))
                        && sp.isExecutionable()
                        && (!sp.isExercised() || includeExercised)
                        && (getParent() instanceof Company
                            && sp.isUsableIfOwnedByCompany() || getParent() instanceof Player
                                                                && sp.isUsableIfOwnedByPlayer())) {
                        log.debug("Portfolio {} has SP {}", getParent().getId(), sp);
                        result.add((T) sp);
                    }
                }
            }

            // Private-independent special properties
            for (SpecialProperty sp : specialProperties.getPortfolio()) {
                if ((clazz == null || clazz.isAssignableFrom(sp.getClass()))
                    && sp.isExecutionable()
                    && (!sp.isExercised() || includeExercised)
                    && (getParent() instanceof Company
                        && sp.isUsableIfOwnedByCompany() || getParent() instanceof Player
                                                            && sp.isUsableIfOwnedByPlayer())) {
                    log.debug("Portfolio {} has persistent SP {}", getParent().getId(), sp);
                    result.add((T) sp);
                }
            }

        }

        return result;
    }

    public PrivatesModel getPrivatesOwnedModel() {
        return privates;
    }

    public boolean addBonusToken(BonusToken token) {
        return bonusTokens.add(token);
    }

    public Portfolio<BonusToken> getTokenHolder() {
        return bonusTokens;
    }

    public void rustObsoleteTrains() {

        List<Train> trainsToRust = new ArrayList<>();
        for (Train train : trainCards.getTrains()) {
            if (train.isObsolete()) {
                trainsToRust.add(train);
            }
        }
        // Need to separate selection and execution,
        // otherwise we get a ConcurrentModificationException on trains.
        for (Train train : trainsToRust) {
            ReportBuffer.add(this, LocalText.getText("TrainsObsoleteRusted", train.toText(), getParent().getId()));
            log.debug("Obsolete train {} (owned by {}) rusted", train.getId(), getParent().getId());
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
     * Used to identify portfolios on reload TODO: Remove that in the future
     */
    @Deprecated
    public String getUniqueName() {
        // For BankPortfolios use Bank
        if (getParent() instanceof BankPortfolio) {
            return Bank.class.getSimpleName() + "_" + getParent().getId();
        }
        return getParent().getClass().getSimpleName() + "_"
               + getParent().getId();
    }

    @Override
    public String toString() {
        return getParent().getId();
    }
}

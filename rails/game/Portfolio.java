package rails.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.Maps;

import rails.common.LocalText;
import rails.game.model.AbstractModel;
import rails.game.model.CertificateCountModel;
import rails.game.model.HolderModel;
import rails.game.model.PrivatesModel;
import rails.game.model.CertificatesModel;
import rails.game.model.TrainsModel;
import rails.game.special.LocatedBonus;
import rails.game.special.SpecialPropertyI;
import rails.game.state.AbstractItem;
import rails.game.state.ArrayListState;
import rails.game.state.MoveUtils;
import rails.game.state.Moveable;
import rails.game.state.Holder;
import rails.util.Util;

/**
 * A Portfolio(Model) stores several HolderModels
 * 
 * For the important MoveAble objects own methods are implemented
 * 
 * All other HolderModels can be added by the general methods
 * 
 * @author evos, freystef (2.0)
 */
public final class Portfolio extends AbstractItem {

    protected static Logger log =
        Logger.getLogger(Portfolio.class.getPackage().getName());

    /** Specific portfolio names */
    public static final String IPO_NAME = "IPO";
    public static final String POOL_NAME = "Pool";
    public static final String SCRAPHEAP_NAME = "ScrapHeap";
    public static final String UNAVAILABLE_NAME = "Unavailable";

    /** Owner */
    private final CashHolder owner;
    
    /** Owned private companies */
    private final PrivatesModel privates = new PrivatesModel(this);

    /** Owned public company certificates */
    private final Map<PublicCompanyI, CertificatesModel> certificates = Maps.newHashMap(); 

    /** Owned trains */
    private final TrainsModel trains = new TrainsModel(this);

    /** Owned tokens */
    // TODO Currently only used to discard expired Bonus tokens.
    private final HolderModel<TokenI> tokens = new HolderModel<TokenI> (this, "tokens");
    
    /**
     * Private-independent special properties. When moved here, a special
     * property no longer depends on the private company being alive. Example:
     * 18AL named train tokens.
     */
    private final HolderModel<SpecialPropertyI> specialProperties = 
        new HolderModel<SpecialPropertyI>(this, "specialProperties");

    private final GameManagerI gameManager;

    public Portfolio(CashHolder holder, String id) {
        super(holder, id);
        this.owner = holder;

        gameManager = GameManager.getInstance();
        gameManager.addPortfolio(this);

        // change display style dependent on owner
        if (owner instanceof PublicCompanyI) {
            trains.setAbbrList(false);
            privates.setLineBreak(false);
        } else if (owner instanceof Bank) {
            trains.setAbbrList(true);
        } else if (owner instanceof Player) {
            privates.setLineBreak(true);
        }
    }

    public void transferAssetsFrom(Portfolio otherPortfolio) {

        // Move trains
        MoveUtils.objectMoveAll(otherPortfolio.getTrainList(), this.getTrainList());

        // Move treasury certificates
        MoveUtils.objectMoveAll(otherPortfolio.getCertificates(), this.getCertificates());
    }

    /** Low-level method, only to be called by the local addObject() method and by initialisation code. */
    public void addPrivate(PrivateCompanyI privateCompany, int position) {

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

    /** Low-level method, only to be called by the local addObject() method and by initialisation code. */
    public void addCertificate(PublicCertificateI certificate){
        addCertificate (certificate, -1);
    }

    /** Low-level method, only to be called by the local addObject() method. */
    private void addCertificate(PublicCertificateI certificate, int position) {
        // When undoing a company start, put the President back at the top.
        if (certificate.isPresidentShare()) position = 0;

        certificates.add(position[0], certificate);

        String companyName = certificate.getCompany().getId();
        if (!certPerCompany.containsKey(companyName)) {
            certPerCompany.put(companyName, new ArrayList<PublicCertificateI>());
        }

        Util.addToList(certPerCompany.get(companyName), certificate, position[1]);

        String certTypeId = certificate.getTypeId();
        if (!certsPerType.containsKey(certTypeId)) {
            certsPerType.put(certTypeId, new ArrayList<PublicCertificateI>());
        }
        Util.addToList(certsPerType.get(certTypeId), certificate, position[2]);

        certificate.setPortfolio(this);

        getShareModel(certificate.getCompany()).addShare(certificate.getShare());
        updatePlayerWorth ();
    }

    /** Low-level method, only to be called by the local addObject() method. */
    private boolean removePrivate(PrivateCompanyI privateCompany) {
        boolean removed = privateCompanies.remove(privateCompany);
        if (removed) {
            privates.notifyModel();
            updatePlayerWorth ();
        }
        return removed;
    }

    /** Low-level method, only to be called by the local addObject() method. */
    private void removeCertificate(PublicCertificateI certificate) {
        certificates.remove(certificate);

        String companyName = certificate.getCompany().getId();

        List<PublicCertificateI> certs = getCertificatesPerCompany(companyName);
        certs.remove(certificate);

        String certTypeId = certificate.getTypeId();
        if (certsPerType.containsKey(certTypeId)) {
            certsPerType.get(certTypeId).remove(0);
            if (certsPerType.get(certTypeId).isEmpty()) {
                certsPerType.remove(certTypeId);
            }
        }

        getShareModel(certificate.getCompany()).addShare(
                -certificate.getShare());
        updatePlayerWorth ();
    }

    protected void updatePlayerWorth () {
        if (owner instanceof Player) {
            ((Player)owner).updateWorth();
        }
    }
    
   public CertificatesModel getShareModel(PublicCompanyI company) {

        if (!shareModelPerCompany.containsKey(company)) {
            shareModelPerCompany.put(company, new CertificatesModel(this, company));
        }
        return shareModelPerCompany.get(company);
    }

    public ArrayListState<PrivateCompanyI> getPrivateCompanies() {
        return privateCompanies;
    }

    public ArrayListState<PublicCertificateI> getCertificates() {
        return certificates;
    }

    /** Get the number of certificates that count against the certificate limit */
    public float getCertificateCount() {

        float number = privateCompanies.size(); // May not hold for all games
        PublicCompanyI comp;

        for (PublicCertificateI cert : certificates) {
            comp = cert.getCompany();
            if (!comp.hasFloated() || !comp.hasStockPrice()
                    || !cert.getCompany().getCurrentSpace().isNoCertLimit())
                number += cert.getCertificateCount();
        }
        return number;
    }

    public Map<String, List<PublicCertificateI>> getCertsPerCompanyMap() {
        return certPerCompany;
    }

    public List<PublicCertificateI> getCertificatesPerCompany(String compName) {
        if (certPerCompany.containsKey(compName)) {
            return certPerCompany.get(compName);
        } else {
            // TODO: This is bad. If we don't find the company name
            // we should check to see if certPerCompany has been loaded
            // or possibly throw a config error.
            return new ArrayList<PublicCertificateI>();
        }
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
    public PublicCertificateI findCertificate(PublicCompanyI company,
            boolean president) {
        return findCertificate(company, 1, president);
    }

    /** Find a certificate for a given company. */
    public PublicCertificateI findCertificate(PublicCompanyI company,
            int shares, boolean president) {
        String companyName = company.getId();
        if (!certPerCompany.containsKey(companyName)) {
            return null;
        }
        for (PublicCertificateI cert : certPerCompany.get(companyName)) {
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

    public Map<String, List<PublicCertificateI>> getCertsPerType() {
        return certsPerType;
    }

    public List<PublicCertificateI> getCertsOfType(String certTypeId) {
        if (certsPerType.containsKey(certTypeId)) {
            return certsPerType.get(certTypeId);
        } else {
            return null;
        }
    }

    public PublicCertificateI getCertOfType(String certTypeId) {
        if (certsPerType.containsKey(certTypeId)) {
            return certsPerType.get(certTypeId).get(0);
        } else {
            return null;
        }
    }

    /**
     * @return
     */
    public CashHolder getOwner() {
        return owner;
    }

    /**
     * @param object
     */
    public void setOwner(CashHolder owner) {
        this.owner = owner;
    }

    /**
     * @return
     */
    public String getId() {
        return name;
    }

    /** Get unique name (prefixed by the owners class type, to avoid Bank, Player and Company
     * namespace clashes).
     * @return
     */
    public String getUniqueName () {
        return uniqueName;
    }

    /**
     * Returns percentage that a portfolio contains of one company.
     *
     * @param company
     * @return
     */
    public int getShare(PublicCompanyI company) {
        return certificates.get(company).getShare();
    }

    public int ownsCertificates(PublicCompanyI company, int unit,
            boolean president) {
        int certs = 0;
        String name = company.getId();
        if (certPerCompany.containsKey(name)) {
            for (PublicCertificateI cert : certPerCompany.get(name)) {
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
    public List<PublicCertificateI> swapPresidentCertificate(
            PublicCompanyI company, Portfolio other) {

        List<PublicCertificateI> swapped = new ArrayList<PublicCertificateI>();
        PublicCertificateI swapCert;

        // Find the President's certificate
        PublicCertificateI cert = this.findCertificate(company, true);
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
        getShareModel(company).setShare();

        return swapped;
    }

    /** Low-level method, only to be called by initialisation code and by the local addObject() method. */
    public void addTrain (TrainI train) {
        addTrain (train, new int[] {-1,-1,-1});
    }

    /** Low-level method, only to be called by the local addObject() method. */
    private void addTrain(TrainI train, int[] position) {

        trains.add(position[0], train);
        
        TrainType type = train.getType();
        if (!trainsPerType.containsKey(type)) {
            trainsPerType.put(type, new ArrayList<TrainI>());
        }
        Util.addToList(trainsPerType.get(type), train, position[1]);
        
        TrainCertificateType certType = train.getCertType();
        if (!trainsPerCertType.containsKey(certType)) {
            trainsPerCertType.put(certType, new ArrayList<TrainI>());
        }
        Util.addToList(trainsPerCertType.get(certType), train, position[2]);
        
        train.setHolder(this);
        trainsModel.notifyModel();
    }

    /** Low-level method, only to be called by Move objects */
    private void removeTrain(TrainI train) {
        trains.remove(train);
        trainsPerType.get(train.getPreviousType()).remove(train);
        trainsPerCertType.get(train.getCertType()).remove(train);
        train.setHolder(null);
        trainsModel.notifyModel();
    }

    public void buyTrain(TrainI train, int price) {
        CashHolder oldOwner = train.getOwner();
        train.moveTo(this);
        if (price > 0) MoveUtils.cashMove(owner, oldOwner, price);
    }

    public void discardTrain(TrainI train) {
        train.moveTo(GameManager.getInstance().getBank().getPool());
        ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                name, train.getId() ));
    }

    public void updateTrainsModel() {
        trainsModel.notifyModel();
    }

    public int getNumberOfTrains() {
        return trains.size();
    }

    public ArrayListState<TrainI> getTrainList() {
        return trains;
    }

    public TrainI[] getTrainsPerType(TrainType type) {

        List<TrainI> trainsFound = new ArrayList<TrainI>();
        for (TrainI train : trains) {
            if (train.getType() == type) trainsFound.add(train);
        }

        return trainsFound.toArray(new TrainI[0]);
    }

    public AbstractModel<String> getTrainsModel() {
        return trainsModel;
    }

    /** Returns one train of any type held */
    public List<TrainI> getUniqueTrains() {

        List<TrainI> trainsFound = new ArrayList<TrainI>();
        Map<TrainType, Object> trainTypesFound =
            new HashMap<TrainType, Object>();
        for (TrainI train : trains) {
            if (!trainTypesFound.containsKey(train.getType())) {
                trainsFound.add(train);
                trainTypesFound.put(train.getType(), null);
            }
        }
        return trainsFound;

    }

    public TrainI getTrainOfType(TrainCertificateType type) {
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
    private boolean addSpecialProperty(SpecialPropertyI property, int position) {


        if (specialProperties == null) {
            specialProperties = new ArrayList<SpecialPropertyI>(2);
        }

        boolean result = Util.addToList(specialProperties, property, position);
        if (!result) return false;

        property.setHolder(this);

        // Special case for bonuses with predefined locations
        // TODO Does this belong here?
        if (owner instanceof PublicCompanyI && property instanceof LocatedBonus) {
            PublicCompanyI company = (PublicCompanyI)owner;
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

        return result;
    }

    /**
     * Remove a special property.
     * Low-level method, only to be called by Move objects.
     * @param property The special property object to remove.
     * @return True if successful.
     */
    private boolean removeSpecialProperty(SpecialPropertyI property) {

        boolean result = false;

        if (specialProperties != null) {
            result = specialProperties.remove(property);

            // Special case for bonuses with predefined locations
            // TODO Does this belong here?
            if (owner instanceof PublicCompanyI && property instanceof LocatedBonus) {
                PublicCompanyI company = (PublicCompanyI)owner;
                LocatedBonus locBonus = (LocatedBonus)property;
                company.removeBonus(locBonus.getId());
            }
        }

        return result;
    }

    /**
     * Add an object.
     * Low-level method, only to be called by Move objects.
     * @param object The object to add.
     * @return True if successful.
     */
    public boolean addObject(Moveable object, int position) {
        if (object instanceof PublicCertificateI) {
            if (position == null) position = new int[] {-1, -1, -1};
            addCertificate((PublicCertificateI) object, position);
            return true;
        } else if (object instanceof PrivateCompanyI) {
            addPrivate((PrivateCompanyI) object, position == null ? -1 : position[0]);
            return true;
        } else if (object instanceof TrainI) {
            if (position == null) position = new int[] {-1, -1, -1};
            addTrain((TrainI) object, position);
            return true;
        } else if (object instanceof SpecialPropertyI) {
            return addSpecialProperty((SpecialPropertyI) object, position == null ? -1 : position[0]);
        } else if (object instanceof TokenI) {
            return addToken((TokenI) object, position == null ? -1 : position[0]);
        } else {
            return false;
        }
    }

    /**
     * Remove an object.
     * Low-level method, only to be called by Move objects.
     *
     * @param object The object to remove.
     * @return True if successful.
     */
    public boolean removeObject(Moveable object) {
        if (object instanceof PublicCertificateI) {
            removeCertificate((PublicCertificateI) object);
            return true;
        } else if (object instanceof PrivateCompanyI) {
            removePrivate((PrivateCompanyI) object);
            return true;
        } else if (object instanceof TrainI) {
            removeTrain((TrainI) object);
            return true;
        } else if (object instanceof SpecialPropertyI) {
            return removeSpecialProperty((SpecialPropertyI) object);
        } else if (object instanceof TokenI) {
            return removeToken((TokenI) object);
        } else {
            return false;
        }
    }

    public int[] getListIndex (Moveable object) {
        if (object instanceof PublicCertificateI) {
            PublicCertificateI cert = (PublicCertificateI) object;
            return new int[] {
                   certificates.indexOf(object),
                   certPerCompany.get(cert.getCompany().getId()).indexOf(cert),
                   certsPerType.get(cert.getTypeId()).indexOf(cert)
            };
        } else if (object instanceof PrivateCompanyI) {
            return new int[] {privateCompanies.indexOf(object)};
        } else if (object instanceof TrainI) {
            TrainI train = (TrainI) object;
            return new int[] {
                    trains.indexOf(train),
                    train.getPreviousType() != null ? trainsPerType.get(train.getPreviousType()).indexOf(train) : -1,
                    trainsPerCertType.get(train.getCertType()).indexOf(train)
            };
        } else if (object instanceof SpecialPropertyI) {
            return new int[] {specialProperties.indexOf(object)};
        } else if (object instanceof TokenI) {
            return new int[] {tokens.indexOf(object)};
        } else {
            return Moveable.AT_END;
        }
    }

    /**
     * @return ArrayList of all special properties we have.
     */
    public List<SpecialPropertyI> getPersistentSpecialProperties() {
        return specialProperties;
    }

    public List<SpecialPropertyI> getAllSpecialProperties() {
        List<SpecialPropertyI> sps = new ArrayList<SpecialPropertyI>();
        if (specialProperties != null) sps.addAll(specialProperties);
        for (PrivateCompanyI priv : privateCompanies.view()) {
            if (priv.getSpecialProperties() != null) {
                sps.addAll(priv.getSpecialProperties());
            }
        }
        return sps;
    }

    /**
     * Do we have any special properties?
     *
     * @return Boolean
     */
    public boolean hasSpecialProperties() {
        return specialProperties != null && !specialProperties.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public <T extends SpecialPropertyI> List<T> getSpecialProperties(
            Class<T> clazz, boolean includeExercised) {
        List<T> result = new ArrayList<T>();
        List<SpecialPropertyI> sps;

        if (owner instanceof Player || owner instanceof PublicCompanyI) {

            for (PrivateCompanyI priv : privateCompanies.view()) {

                sps = priv.getSpecialProperties();
                if (sps == null) continue;

                for (SpecialPropertyI sp : sps) {
                    if ((clazz == null || clazz.isAssignableFrom(sp.getClass()))
                            && sp.isExecutionable()
                            && (!sp.isExercised() || includeExercised)
                            && (owner instanceof Company && sp.isUsableIfOwnedByCompany()
                                    || owner instanceof Player && sp.isUsableIfOwnedByPlayer())) {
                        log.debug("Portfolio "+name+" has SP " + sp);
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
                        log.debug("Portfolio "+name+" has persistent SP " + sp);
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

    /** Low-level method, only to be called by the local addObject() method. */
    public boolean addToken(TokenI token, int position) {
        tokens.add(position, token);
        return true;
    }

    /** Low-level method, only to be called by the local addObject() method. */
    public boolean removeToken(TokenI token) {
        return tokens.remove(token);
    }

    public boolean hasTokens() {
        return tokens != null && !tokens.isEmpty();
    }

    public void rustObsoleteTrains() {

        List<TrainI> trainsToRust = new ArrayList<TrainI>();
        for (TrainI train : trains) {
            if (train.isObsolete()) {
                trainsToRust.add(train);
            }
        }
        // Need to separate selection and execution,
        // otherwise we get a ConcurrentModificationException on trains.
        for (TrainI train : trainsToRust) {
            ReportBuffer.add(LocalText.getText("TrainsObsoleteRusted",
                    train.getId(), name));
            log.debug("Obsolete train " + train.getUniqueId() + " (owned by "
                    + name + ") rusted");
            train.setRusted();
        }
        trainsModel.notifyModel();
    }

}

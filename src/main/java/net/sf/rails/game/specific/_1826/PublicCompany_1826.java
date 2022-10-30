package net.sf.rails.game.specific._1826;

import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.model.PortfolioOwner;
import net.sf.rails.game.special.ExtraTrainRight;
import net.sf.rails.game.state.Model;
import net.sf.rails.game.state.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PublicCompany_1826 extends PublicCompany {

    public static String ETAT = "Etat";
    public static String SNCF = "SNCF";
    public static String BELG = "Belg";

    private int minNumberToExchange = 0;
    /**
     * The maximum number to exchange *per company*; 0 = unlimited
     */
    private int maxNumberToExchange = 0;
    /**
     * False: the numbers to exchange are counted over all old companies,
     * True: the numbers to exchange are counted per old company.
     */
    private boolean exchangeCountPerCompany = true;


    private static final Logger log = LoggerFactory.getLogger(PublicCompany_1826.class);

    public PublicCompany_1826(RailsItem parent, String id) {
        super (parent, id, true);
    }

    public PublicCompany_1826(RailsItem parent, String id, boolean hasStockPrice) {
        super(parent, id, hasStockPrice);
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

        // For national companies
        if (getType().getId().equalsIgnoreCase("National")) {
            Tag formationTag = tag.getChild("Formation");
            if (formationTag != null) {
                minNumberToExchange = formationTag.getAttributeAsInteger("minNumberToExchange");
                maxNumberToExchange = formationTag.getAttributeAsInteger("maxNumberToExchange");
                exchangeCountPerCompany = formationTag.getAttributeAsBoolean(
                        "exchangeCountPerCompany", exchangeCountPerCompany);
            }
        }

        Tag bondsTag = tag.getChild("Bonds");
        if (bondsTag != null) {
            numberOfBonds = bondsTag.getAttributeAsInteger("number", 0);
            portfolio.getBondsModel(this).setBondsCount(numberOfBonds);
            priceOfBonds = bondsTag.getAttributeAsInteger("price", 0);
            bondsInterest = bondsTag.getAttributeAsInteger("interest", 0);
        }
    }


    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {

        super.finishConfiguration(root);

        // 5-share companies have an initial share unit of 20%
        if (isPotentialFiveShareCompany()) {
            setShareUnit(20);
        }
    }

    public int getMinNumberToExchange() {
        return minNumberToExchange;
    }

    public int getMaxNumberToExchange() {
        return maxNumberToExchange;
    }

    public boolean isExchangeCountPerCompany() {
        return exchangeCountPerCompany;
    }

    private boolean isPotentialFiveShareCompany() {
        return getType().getId().equals("Public") && !getId().equals("Belg");
    }

    /** Check if a company must get more tokens that the configured minimal number.*/
    @Override
    public void setFloated() {

        int extraTokens = 0;

        if (isPotentialFiveShareCompany() && shareUnit.value() == 10) {
           // 4 tokens (Belge alrady starts with 4)
            extraTokens++;
        }
        if (getType().getId().equals("Public")  // includes Belge
                && getRoot().getPhaseManager().hasReachedPhase("10H")) {
            // 5 tokens
            extraTokens++;
        }

        for (int i = 0; i < extraTokens; i++) {
            baseTokens.addBaseToken(BaseToken.create(this), false);
        }
        numberOfBaseTokens += extraTokens;

        super.setFloated();
    }

    protected void setCapitalizationShares() {
        if (getId().equals(SNCF)) {
            capitalisationShares = getPortfolioModel().getShares(this);
        } else {  //ETAT
            capitalisationShares = 0;
        }
        log.debug("{} CapFactor set to {}", this, capitalisationShares);
    }

    /** Convert company from a 5-share to a 10-share company */
    /* The intention is to make this code usable for other games as well. */
    public boolean grow (boolean checkDestination) {

        if (!validateGrow(checkDestination)) return false;

        growStep.add(1);
        setShareUnit(shareUnitSizes.get(growStep.value()));

        BankPortfolio reserved = getRoot().getBank().getUnavailable();
        Set<PublicCertificate> last5Shares = reserved.getPortfolioModel().getCertificates(this);
        for (PublicCertificate cert : last5Shares) {
            cert.moveTo(this);
        }

        ReportBuffer.add(this, LocalText.getText("CompanyHasGrown",
                this, getActiveShareCount()));

        currentTrainLimits.setTo(trainLimits.get(growStep.value()));
        ReportBuffer.add(this,
                LocalText.getText("PhaseDependentTrainLimitsSetTo",
                        this, currentTrainLimits.view(), getCurrentTrainLimit()));


        // For some reason the shareUnit change does not update
        // the percentages shown in the GameStatus window.
        // E.g. 60% should become 30%, etc.
        // There must be a nicer way to accomplish that,
        // but for now the below code works.
        Set<Model> modelsToUpdate = new HashSet<>();
        PortfolioOwner owner;
        Model model;
        for (PublicCertificate cert : getCertificates()) {
            owner = (PortfolioOwner) cert.getOwner();
            model = owner.getPortfolioModel().getShareModel(this);
            if (!modelsToUpdate.contains(model)) modelsToUpdate.add(model);
        }
        for (Model m : modelsToUpdate) {
            for (Observer obs : m.getObservers()) {
                obs.update(m.toText());
            }
        }
        return true;
    }

    protected boolean validateGrow(boolean checkDestination) {
        return super.validateGrow()
                && (!checkDestination || hasReachedDestination());
    }

    @Override
    protected int getTrainLimit(int phaseIndex) {

        int limit = super.getTrainLimit(phaseIndex);
        if (rightsModel == null || rightsModel.isEmpty()) return limit;

        ExtraTrainRight etr = rightsModel.getRightType(ExtraTrainRight.class);
        if (etr != null) limit += etr.getExtraTrains();

        return limit;
    }

    /** Check if a company must take a loan to buy a train from the Bank
     * or to pay loan interest or bond revenue.
     * (The player can still decide to buy a cheap train from another
     * company, so loan taking is not really mandatory)
     *
     * @return True if a loan should be taken
     */
    public boolean canTakeLoan() {
        int loanValue = getLoanValueModel().value();

        int interest = loanValue * getLoanInterestPct() / 100;
        int compCash = getCash();
        return (compCash < interest || getNumberOfTrains() == 0)
                && currentNumberOfLoans.value() < 2;
    }
}
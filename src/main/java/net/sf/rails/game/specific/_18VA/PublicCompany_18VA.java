package net.sf.rails.game.specific._18VA;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.model.PortfolioOwner;
import net.sf.rails.game.special.ExtraTrainRight;
import net.sf.rails.game.specific._1826.GameDef_1826;
import net.sf.rails.game.state.Model;
import net.sf.rails.game.state.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class PublicCompany_18VA extends PublicCompany {

    private static final Logger log = LoggerFactory.getLogger(PublicCompany_18VA.class);

    public PublicCompany_18VA(RailsItem parent, String id) {
        super (parent, id, true);
    }

    public PublicCompany_18VA(RailsItem parent, String id, boolean hasStockPrice) {
        super(parent, id, hasStockPrice);
    }

    // Probably redundant
    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

    }


    // Really needed?
    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {

        super.finishConfiguration(root);

        // 5-share companies have an initial share unit of 20%
        if (isPotentialFiveShareCompany()) {
            setShareUnit(20);
        }
    }

    private boolean isPotentialFiveShareCompany() {
        return getType().getId().equals("Public") && !getId().equals(GameDef_18VA.BO);
    }

    /** Check if a company must get more tokens that the configured minimal number.*/
    @Override
    public void setFloated() {

        int extraTokens = 0;
        boolean reachedPhase5 = getType().getId().equals("Public")
                && getRoot().getPhaseManager().hasReachedPhase(GameDef_18VA.PHASE_5);

        if (reachedPhase5) {
            extraTokens += 2;
        }

        for (int i = 0; i < extraTokens; i++) {
            baseTokens.addBaseToken(BaseToken.create(this), false);
        }
        numberOfBaseTokens.add (extraTokens);

        super.setFloated();
        Set<PublicCertificate> certs = getRoot().getBank().getIpo()
                .getPortfolioModel().getCertificates(this);
        for (PublicCertificate cert : certs) {
            cert.moveTo(this);
        };


        // TODO
        if (reachedPhase5) {


        }
    }

    public void addBaseToken () {
        baseTokens.addBaseToken(BaseToken.create(this), false);
        numberOfBaseTokens.add(1);
    }

    // Probably not needed
    protected void setCapitalizationShares() {
        if (getId().equals(GameDef_1826.SNCF)) {
            capitalisationShares = getPortfolioModel().getShares(this);
        } else if (getId().equals(GameDef_1826.ETAT)) {
            capitalisationShares = 0;
        }
        log.debug("{} CapFactor set to {}", this, capitalisationShares);
    }

    @Override // Probably not needed
    public int getCapitalisation() {
        if (getType().getId().equalsIgnoreCase("Public")
                && getRoot().getPhaseManager().hasReachedPhase(GameDef_18VA.PHASE_5)) {
            return CAPITALISE_FULL;
        } else {
            return capitalisation;
        }
    }

    /** Convert company from a 5-share to a 10-share company */
    /* The intention is to make this code usable for other games as well. */
    public boolean grow (boolean checkDestination) {

        if (!validateGrow(checkDestination)) return false;

        growStep.add(1);
        setShareUnit(shareUnitSizes.get(growStep.value()));

        BankPortfolio reserved = getRoot().getBank().getUnavailable();
        BankPortfolio ipo = getRoot().getBank().getIpo();
        Set<PublicCertificate> last5Shares = reserved.getPortfolioModel().getCertificates(this);
        for (PublicCertificate cert : last5Shares) {
            if (hasStarted()) {
                cert.moveTo(this);
            } else {
                // Still in IPO, put the reserved shares there too
                cert.moveTo(ipo);
            }
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

}
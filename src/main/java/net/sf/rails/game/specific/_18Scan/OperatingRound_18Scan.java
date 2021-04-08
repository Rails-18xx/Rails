package net.sf.rails.game.specific._18Scan;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.special.SellBonusToken;
import net.sf.rails.game.special.SpecialSingleTileLay;
import net.sf.rails.game.state.MoneyOwner;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.BuyTrain;
import rails.game.action.ReachDestinations;
import rails.game.action.SetDividend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OperatingRound_18Scan extends OperatingRound {

    protected GameDef.OrStep[] steps = new GameDef.OrStep[]{
            GameDef.OrStep.INITIAL,
            GameDef.OrStep.LAY_TRACK,
            GameDef.OrStep.LAY_TOKEN,
            GameDef.OrStep.CALC_REVENUE,
            GameDef.OrStep.PAYOUT,
            GameDef.OrStep.BUY_TRAIN,
            GameDef.OrStep.FINAL
    };

    private final String SJ = GameDef_18Scan.SJ;

    /**
     * To keep record of train types bought in a turn:
     * per type only one train is allowed.
     */
    private Set<TrainType> trainsBoughtThisTurn = new HashSet<>();

    private static final Logger log = LoggerFactory.getLogger(OperatingRound_18Scan.class);

    public OperatingRound_18Scan(GameManager parent, String id) {
        super(parent, id);
    }

    /**
     * In a normal operating round there are no special token lays.
     * See DestinationRun_18Scan where it does occur
     */
    @ Override
    protected void setSpecialTokenLays () {
        currentSpecialTokenLays.clear();
    }

    /**
     * To raise 10K if a minor has no trains
     * @param step An OR step that is considered for execution
     */
     @Override
    protected void executeTrainlessRevenue (GameDef.OrStep step) {
        // Minors always pay out something.
        PublicCompany company = operatingCompany.value();
        if (step == GameDef.OrStep.CALC_REVENUE && company.isOfType("Minor")
                && !company.hasTrains()) {
            int amount = 10;
            String report = LocalText.getText("NoTrainsButBankPaysAnyway",
                    company.getId(),
                    Bank.format(this, amount),
                    company.getPresident().getId());
            log.debug("OR skips {}: Cannot run trains but still pays {}", step, amount);
            SetDividend action = new SetDividend(getRoot(), 0, false,
                    new int[]{SetDividend.PAYOUT});
            action.setRevenueAllocation(SetDividend.PAYOUT);
            // To evade a failed equals check during reload:
            //action.setAllowedRevenueAllocations(new int[] {SetDividend.PAYOUT});
            action.setActualRevenue(amount);
            company.setLastRevenue(amount);
            executeSetRevenueAndDividend(action, report);
        }
    }

    /**
     * To raise K10 if a minor has a train but no route.
     * @param action A SetDividend action
     * @param checkAllocation Unused in this subclass
     * @return Error message, if applicable
     */
    @Override
    protected String validateSetRevenueAndDividend(SetDividend action, boolean checkAllocation) {

        String errMsg = super.validateSetRevenueAndDividend(action, false);

        PublicCompany company = operatingCompany.value();
        if (!Util.hasValue(errMsg) && company.isOfType("Minor")
                && action.getActualRevenue() == 0) {
            int revenue = 10;
            String report = LocalText.getText("NoTrainsButBankPaysAnyway",
                    company.getId(),
                    Bank.format(this, revenue),
                    company.getPresident().getId());
            log.debug("Cannot run trains but still pays {}", revenue);
            ReportBuffer.add(this, report);
            action.setActualRevenue(revenue);
            action.setRevenueAllocation(SetDividend.PAYOUT);
        }

        return errMsg;
    }

    // Almost identical to the same method in 1856,
    // except that a restriction to the LAY_TOKEN step has been added.
    @Override
    protected void setDestinationActions() {

        if (getStep() == GameDef.OrStep.LAY_TOKEN
                && !getRoot().getPhaseManager().hasReachedPhase("5")) {
            List<PublicCompany> possibleDestinations = new ArrayList<>();
            for (PublicCompany comp : operatingCompanies.view()) {
                if (!comp.isClosed() && comp.hasDestination() && !comp.hasReachedDestination()) {
                    possibleDestinations.add(comp);
                }
            }
            if (possibleDestinations.size() > 0) {
                possibleActions.add(new ReachDestinations(getRoot(), possibleDestinations));
            }
        }
    }

    @Override
    protected void executeDestinationActions(List<PublicCompany> companies) {

        ((GameManager_18Scan)gameManager).StartDestinationRuns (this, companies);

    }

    @Override
    public boolean buyTrain(BuyTrain action) {
        // If the DSB buys its first train, the SJS closes,
        // but its special power is now owned by the Player
        // that holds the SJS, so that would not close.
        // Pending a better solution, we handle that here.
        // TODO: a generic solution is wanted.

        final PrivateCompany sjs =
                (PrivateCompany) companyManager.getCompany("Private", GameDef_18Scan.SJS);
        final PublicCompany dsb =
                (PublicCompany) companyManager.getCompany("Public", GameDef_18Scan.DSB);
        boolean sjsWasOpen = false;
        SpecialSingleTileLay sst = null;

        // We need to record the SJS owner and the SJS special power while they are alive
        if (operatingCompany.value().equals(dsb)) {
            sjsWasOpen = !sjs.isClosed();
            sst = null;
            if (sjsWasOpen) {
                Player sjsOwner = (Player) sjs.getOwner();
                List<SpecialSingleTileLay> ssts = sjsOwner.getPortfolioModel()
                        .getSpecialProperties(SpecialSingleTileLay.class, true);
                if (ssts != null && !ssts.isEmpty()) sst = ssts.get(0);
            }
        }

        // Normal processing
        boolean result = super.buyTrain(action);

        if (result && sjsWasOpen && sjs.isClosed() && sst != null) {
            sst.moveTo(scrapHeap);
        }

        trainsBoughtThisTurn.add (action.getTrain().getType());
        return result;
    }


    public void resume() {
        if (getStep() == GameDef.OrStep.LAY_TOKEN) {
            // After a destination run, to return to normal processing,
            // resume from the end of the track laying step which initiated it all.
            nextStep(GameDef.OrStep.LAY_TRACK);
        } else if (operatingCompany.value().isHibernating()) {
            finishTurn();
        } else {
            super.resume();
        }
    }

    @Override
    protected void newPhaseChecks() {
        Phase phase = Phase.getCurrent(this);
        if ( "5".equals(phase.getId())) {
            formSJ();
            checkForExcessTrains(); // Again, for new SJ
        }
    }

    private void formSJ () {
        PublicCompany sj = companyManager.getPublicCompany(SJ);
        List<Company> minors = companyManager.getCompaniesByType("Minor");
        String message;
        for (Company comp : minors) {
            PublicCompany minor = (PublicCompany) comp;
            MoneyOwner owner = minor.getPresident();
            PublicCertificate minorCert = minor.getPresidentsShare();
            minorCert.moveTo(getRoot().getBank().getScrapHeap());
            PublicCertificate sjCert = getRoot().getBank().getUnavailable()
                    .getPortfolioModel().findCertificate(sj, false);

            int cash = minor.getCash();
            int trains = minor.getPortfolioModel().getNumberOfTrains();
            int bonusTokens = minor.getBonuses().size();
            sjCert.moveTo(owner);
            sj.transferAssetsFrom(minor);
            // Perhaps the below should be added to transferAssetsFrom()
            if (bonusTokens > 0) {
                for (Bonus bonus : minor.getBonuses()) {
                    minor.removeBonus(bonus);
                    sj.addBonus(bonus);
                }
            }

            ReportBuffer.add(this,
                    LocalText.getText("MinorTransfers",
                    minor.getId(),
                    Bank.format(this, cash),
                    trains,
                    bonusTokens,
                    SJ));

            // Exchange minor tokens for SJ tokens
            for (BaseToken token : minor.getBaseTokensModel().getLaidTokens()) {
                // Procedure copied from 1835
                Stop city = (Stop) token.getOwner();
                MapHex hex = city.getParent();
                token.moveTo(minor);
                if (!hex.hasTokenOfCompany(sj) && hex.layBaseToken(sj, city)) {
                    message = LocalText.getText("ExchangesBaseToken",
                            SJ, minor.getId(),
                            city.getComposedId());
                    ReportBuffer.add(this, message);

                    sj.layBaseToken(hex, 0); // Already laid, but this handles some consequences
                }
            }
            minor.setClosed();
            ReportBuffer.add (this,
                    LocalText.getText("MinorClosesAndShareExchanged",
                            minor.getId(), owner.getId(), sjCert.getShare(), SJ));
        }

        // Check if SJ can float
        checkFlotation(sj);
        if (sj.hasFloated()) {
            sj.checkPresidency();
            DisplayBuffer.add(this, LocalText.getText("HasFormedAndFloated", SJ));
        } else {
            DisplayBuffer.add(this, LocalText.getText("HasFormedNotFloated", SJ));
        }

        // Any unsold bonus tokens must now be paid to the Bank
        List<SellBonusToken> sbts = gameManager.getSpecialProperties(SellBonusToken.class, false);
        if (!sbts.isEmpty()) {
            for (SellBonusToken sbt : sbts) {
                sbt.setSeller(bank.getPool());
            }
        }
     }
 }

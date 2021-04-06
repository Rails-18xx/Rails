package net.sf.rails.game.specific._1837;

import net.sf.rails.game.*;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.DiscardTrain;
import rails.game.action.MergeCompanies;
import rails.game.action.PossibleAction;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockRound;

/**
 * @author martin
 *
 */
public class StockRound_1837 extends StockRound {
    private static final Logger log = LoggerFactory.getLogger(StockRound_1837.class);

    protected final ArrayListState<PublicCompany> compWithExcessTrains =
            new ArrayListState<>(this, "compWithExcessTrains");
    protected final IntegerState discardingCompanyIndex = IntegerState.create(
            this, "discardingCompanyIndex");
    protected final BooleanState discardingTrains = new BooleanState(this,
            "discardingTrains");
    //protected final BooleanState exchangedCoalCompanies = new BooleanState(this,
    //        "exchangedCoalCompanies");

    protected PublicCompany[] discardingCompanies;

    public StockRound_1837(GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    public void start() {
        super.start();
        if (discardingTrains.value()) {
            discardingTrains.set(false);
        }
    }

    @Override
    protected void gameSpecificChecks(PortfolioModel boughtFrom,
                                      PublicCompany company,
                                      boolean justStarted) {
        if (justStarted) {
            StockSpace parSpace = company.getCurrentSpace();
            ((StockMarket_1837) stockMarket).addParSpaceUser(parSpace);
        }
    }

    /**
     * Share price goes down 1 space for any number of shares sold.
     */
    @Override
    // Copied from 1835
    protected void adjustSharePrice (PublicCompany company, Owner seller, int numberSold, boolean soldBefore) {
        // No more changes if it has already dropped
        if (!soldBefore) {
            super.adjustSharePrice (company, seller,1, soldBefore);
        }
    }



    @Override
    public boolean setPossibleActions() {
        if (discardingTrains.value()) {
            return setTrainDiscardActions();
        } else {
            return super.setPossibleActions();
        }
    }

    protected boolean setTrainDiscardActions() {

        PublicCompany discardingCompany =
                discardingCompanies[discardingCompanyIndex.value()];
        log.debug("Company {} to discard a train", discardingCompany.getId());
        possibleActions.add(new DiscardTrain(discardingCompany,
                discardingCompany.getPortfolioModel().getUniqueTrains()));
        // We handle one train at at time.
        // We come back here until all excess trains have been discarded.
        return true;
    }

    @Override
    protected boolean processGameSpecificAction(PossibleAction action) {

        log.debug("GameSpecificAction: {}", action);

        boolean result = false;

        if (action instanceof MergeCompanies) {

            result = mergeCompanies((MergeCompanies) action);

        } else if (action instanceof DiscardTrain) {

            result = discardTrain((DiscardTrain) action);
        }

        return result;
    }

    /**
     * Merge a minor into an already started company. <p>Also covers the actions
     * of the Final Minor Exchange Round, in which minors can also be closed (in
     * that case, the MergeCompanies.major attribute is null, which never occurs
     * in normal stock rounds).
     *
     * @param action The MergeCompanies chosen action
     * @return True if the merge was successful
     */
    protected boolean mergeCompanies(MergeCompanies action) {

        PublicCompany minor = action.getMergingCompany();
        PublicCompany major = action.getSelectedTargetCompany();

        return mergeCompanies(minor, major);
    }

    /**
     * Complemented by a shorter version in subclass CoalExchangeRound.
     * TODO: to be reconsidered once Nationals formation has been tested.
     *
     * @param minor The minor (or coal company) to be merged into...
     * @param major ...the related major company
     * @return True if the merge was successful
     */
    protected boolean mergeCompanies(PublicCompany minor, PublicCompany major) {
        PublicCertificate cert = null;
        MoneyOwner cashDestination = null; // Bank

        // TODO Validation to be added?
        if (major != null) {
            cert = unavailable.findCertificate(major, false);
            cashDestination = major;
        }
        //TODO: what happens if the major hasnt operated/founded/Started sofar in the FinalCoalExchangeRound ?

        // Transfer the minor assets
        int minorCash = minor.getCash();
        int minorTrains = minor.getPortfolioModel().getTrainList().size();
        if (cashDestination == null) {
            // Assets go to the bank
            if (minorCash > 0) {
                Currency.toBankAll(minor);
            }
            pool.transferAssetsFrom(minor.getPortfolioModel());
        } else {
            // Assets go to the major company
            major.transferAssetsFrom(minor);
        }

        boolean autoMerge = (currentPlayer == null);
        Player minorPres = minor.getPresident();

        ReportBuffer.add(this, "");
        if (autoMerge) {
            ReportBuffer.add(this, LocalText.getText("AutoMergeMinorLog",
                    minor.getId(), major.getId(),
                    Bank.format(this, minorCash), minorTrains));
        } else {
            ReportBuffer.add(this, LocalText.getText("MERGE_MINOR_LOG",
                    minorPres, minor.getId(), major.getId(),
                    Bank.format(this, minorCash), minorTrains));
        }
        // FIXME: CHeck if this still works correctly
        ReportBuffer.add(this, LocalText.getText("GetShareForMinor",
                minorPres, cert.getShare(), major.getId(),
                minor.getId()));
        cert.moveTo(minorPres);

        minor.setClosed();
        ReportBuffer.add(this, LocalText.getText("MinorCloses", minor.getId()));
        checkFlotation(major);

        hasActed.set(true);

        if (!(this instanceof FinalCoalExchangeRound)) {
            companyBoughtThisTurnWrapper.set(major);

            // If >60% shares owned, lift sell obligation this round.
            if (minorPres.getPortfolioModel().getShare(major)
                    > GameDef.getParmAsInt(this, GameDef.Parm.PLAYER_SHARE_LIMIT)) {
                setSellObligationLifted(major);
            }

            setPriority("MergeCompany");
        }

        return true;
    }

    public boolean discardTrain(DiscardTrain action) {

        Train train = action.getDiscardedTrain();
        if (train == null && !action.isForced()) return true;

        PublicCompany company = action.getCompany();
        String companyName = company.getId();

        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct step
            if (!discardingTrains.value()) {
                errMsg = LocalText.getText("WrongActionNoDiscardTrain");
                break;
            }

            if (train == null) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Does the company own such a train?

            if (!company.getPortfolioModel().getTrainList().contains(train)) {
                errMsg =
                        LocalText.getText("CompanyDoesNotOwnTrain",
                                company.getId(), train.toText());
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(
                    this,
                    LocalText.getText("CannotDiscardTrain", companyName,
                            train.toText(), errMsg));
            return false;
        }

        /* End of validation, start of execution */

        train.discard();

        finishTurn();

        return true;
    }

    @Override
    protected void finishTurn() {

        if (!discardingTrains.value()) {
            super.finishTurn();
        } else {
            PublicCompany comp =
                    discardingCompanies[discardingCompanyIndex.value()];
            if (comp.getNumberOfTrains() <= comp.getCurrentTrainLimit()) {
                discardingCompanyIndex.add(1);
                if (discardingCompanyIndex.value() >= discardingCompanies.length) {
                    // All excess trains have been discarded
                    finishRound();
                    return;
                }
            }
            PublicCompany discardingCompany =
                    discardingCompanies[discardingCompanyIndex.value()];
            setCurrentPlayer(discardingCompany.getPresident());
        }
    }
}

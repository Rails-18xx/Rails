package net.sf.rails.game.specific._1837;

import net.sf.rails.game.*;
import net.sf.rails.game.financial.*;
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

import java.util.Set;

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

            result = mergeCompanies((MergeCompanies) action);  // Always true

        } else if (action instanceof DiscardTrain) {

            result = discardTrain((DiscardTrain) action);
        }

        return result;
    }

    /**
     * Merge a minor into an already started company.
     * @param action The MergeCompanies chosen action
     * @return True if the merge was successful
     */
    protected boolean mergeCompanies(MergeCompanies action) {

        PublicCompany minor = action.getMergingCompany();
        PublicCompany major = action.getSelectedTargetCompany();

        return mergeCompanies(minor, major, false,
                currentPlayer == null);
    }

    /**
     * Complemented by a shorter version in subclass CoalExchangeRound.
     * TODO: to be reconsidered once Nationals formation has been tested.
     *
     * @param minor The minor (or coal company) to be merged...
     * @param major ...into the related major company
     * @return True if the merge was successful
     */
    protected boolean mergeCompanies(PublicCompany minor, PublicCompany major) {
        return mergeCompanies (minor, major, false, false);
    }

    protected boolean mergeCompanies(PublicCompany minor, PublicCompany major,
                                     boolean majorPresident, boolean autoMerge) {
        PublicCertificate cert = null;
        MoneyOwner cashDestination = null; // Bank

        // TODO Validation to be added?
        if (major != null) {
            cert = unavailable.findCertificate(major, majorPresident);
            cashDestination = major;
        }
        //TODO: what happens if the major hasnt operated/founded/Started sofar in the FinalCoalExchangeRound ?

        // Save minor details that are needed after merging
        Set<Train> minorTrains = minor.getPortfolioModel().getTrainList();
        int minorTrainsNo = minorTrains.size();

        // Transfer the minor assets
        int minorCash = minor.getCash();
        if (cashDestination == null) {
            // Assets go to the bank
            if (minorCash > 0) {
                Currency.toBankAll(minor);
            }
            pool.transferAssetsFrom(minor.getPortfolioModel());
        } else {
            // Assets go to the major company
            major.transferAssetsFrom(minor);
            if (minor.hasOperated()) {
                gameManager.blockCertificate(cert);
                for (Train train : minorTrains) {
                    gameManager.blockTrain(train);
                }
            }
        }

        Player minorPres = minor.getPresident();

        ReportBuffer.add(this, "");
        if (autoMerge) {
            ReportBuffer.add(this, LocalText.getText("AutoMergeMinorLog",
                    minor.getId(), major.getId(),
                    Bank.format(this, minorCash), minorTrainsNo));
        } else {
            ReportBuffer.add(this, LocalText.getText("MERGE_MINOR_LOG",
                    minorPres, minor.getId(), major.getId(),
                    Bank.format(this, minorCash), minorTrainsNo));
        }
        ReportBuffer.add(this, LocalText.getText("GetShareForMinor",
                minorPres, cert.getShare(), major.getId(),
                minor.getId()));
        cert.moveTo(minorPres);

        // FIXME: CHeck if this still works correctly

        // Check if minors have more certs (1837 Ug minors 1 and 3)

        if (minor.getCertificates().size() > 1) {
            for (PublicCertificate minorCert : minor.getCertificates()) {
                if (minorCert.isPresidentShare()) continue;
                Owner owner = minorCert.getOwner();
                if (owner instanceof Player) {
                    cert = unavailable.findCertificate(major, false);
                    ReportBuffer.add(this, LocalText.getText("GetShareForMinor",
                            owner, cert.getShare(), major.getId(),
                            minor.getId()));
                    cert.moveTo(owner);
                }
            }
        }

        minor.setClosed();
        ReportBuffer.add(this, LocalText.getText("MinorCloses", minor.getId()));
        checkFlotation(major);

        hasActed.set(true);

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

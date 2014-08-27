/**
 * 
 */
package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import rails.game.action.DiscardTrain;
import rails.game.action.MergeCompanies;
import rails.game.action.PossibleAction;
import rails.game.action.StartCompany;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.Bank;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.PublicCertificate;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.StockRound;
import net.sf.rails.game.Stop;
import net.sf.rails.game.Train;
import net.sf.rails.game.specific._1837.FinalCoalExchangeRound;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.MoneyOwner;

/**
 * @author martin
 *
 */
public class StockRound_1837 extends StockRound {



    protected final ArrayListState<PublicCompany> compWithExcessTrains =
            ArrayListState.create(this, "compWithExcessTrains");
    protected final IntegerState discardingCompanyIndex = IntegerState.create(this, "discardingCompanyIndex");
    protected final BooleanState discardingTrains = BooleanState.create(this, "discardingTrains");

    protected PublicCompany[] discardingCompanies;

    /**
     * @param parent
     * @param id
     */
    public StockRound_1837(GameManager parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void start() {
        super.start();
        if (discardingTrains.value()) {
            discardingTrains.set(false);
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

    @Override
    protected void setGameSpecificActions() {
        if (!mayCurrentPlayerBuyAnything()) return;

        List<PublicCompany> comps =
                companyManager.getAllPublicCompanies();
        List<PublicCompany> minors = new ArrayList<PublicCompany>();
        List<PublicCompany> targetCompanies = new ArrayList<PublicCompany>();
        String type;

        for (PublicCompany comp : comps) {
            type = comp.getType().getId();
            if (type.equals("Major") && comp.hasStarted()
                && !comp.hasOperated()) {
                targetCompanies.add(comp);
            } else if (type.equals("Coal")
                       && comp.getPresident() == currentPlayer) {
                minors.add(comp);
            }
        }
        if (minors.isEmpty() || targetCompanies.isEmpty()) return;

        for (PublicCompany minor : minors) {
            possibleActions.add(new MergeCompanies(minor, targetCompanies));
        }
    }

 
    
    protected boolean setTrainDiscardActions() {

        PublicCompany discardingCompany =
                discardingCompanies[discardingCompanyIndex.value()];
        log.debug("Company " + discardingCompany.getId()
                  + " to discard a train");
        possibleActions.add(new DiscardTrain(discardingCompany,
                discardingCompany.getPortfolioModel().getUniqueTrains()));
        // We handle one train at at time.
        // We come back here until all excess trains have been discarded.
        return true;
    }

 
    @Override
    protected boolean processGameSpecificAction(PossibleAction action) {

        log.debug("GameSpecificAction: " + action.toString());

        boolean result = false;

        if (action instanceof MergeCompanies) {

            result = mergeCompanies((MergeCompanies) action);

        } else if (action instanceof DiscardTrain) {

            result = discardTrain((DiscardTrain) action);
        }

        return result;
    }
    
    /**
     * Merge a minor into an already started company. <p>Also covers the
     * actions of the Final Minor Exchange Round, in which minors can also be
     * closed (in that case, the MergeCompanies.major attribute is null, which
     * never occurs in normal stock rounds).
     *
     * @param action
     * @return
     */
    protected boolean mergeCompanies(MergeCompanies action) {

        PublicCompany minor = action.getMergingCompany();
        PublicCompany major = action.getSelectedTargetCompany();
        PublicCertificate cert = null;
        MoneyOwner cashDestination = null; // Bank

        // TODO Validation to be added?

        

        if (major != null) {
            cert = major.getPortfolioModel().findCertificate(major, false);
            if (cert != null) {
                // Assets go to the major company.
                cashDestination = major;
            } else {
                cert = pool.findCertificate(major, false);
                // If null, player gets nothing in return
            }
        }

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

        MapHex homeHex = minor.getHomeHexes().get(0);
        Stop homeStop  = homeHex.getStopOfBaseToken(minor);
        minor.setClosed();

        if (major != null && action.getReplaceToken()) {
            if (homeHex.layBaseToken(major, homeStop)) {
                major.layBaseToken(homeHex, 0);
            }   
        }

        if (major != null) {
            if (major.getNumberOfTrains() > major.getCurrentTrainLimit()
                && !compWithExcessTrains.contains(major)) {
                compWithExcessTrains.add(major);
            }
        }

        if (cert != null) {
            ReportBuffer.add(this, "");
            ReportBuffer.add(this, LocalText.getText("MERGE_MINOR_LOG",
                    currentPlayer.getId(),
                    minor.getId(),
                    major.getId(),
                    Bank.format(this, minorCash),
                    minorTrains ));
            // FIXME: CHeck if this still works correctly
            ReportBuffer.add(this, LocalText.getText("GetShareForMinor",
                    currentPlayer.getId(),
                    cert.getShare(),
                    major.getId(),
                    cert.getOwner().getId(),
                    minor.getId() ));
            if (major != null) {
                if (action.getReplaceToken()) {
                    ReportBuffer.add(this, LocalText.getText("ExchangesBaseToken",
                            major.getId(),
                            minor.getId(),
                            homeHex.getId()));
                } else {
                    ReportBuffer.add(this, LocalText.getText("NoBaseTokenExchange",
                            major.getId(),
                            minor.getId(),
                            homeHex.getId()));
                }
            }
            cert.moveTo(currentPlayer);
            ReportBuffer.add(this, LocalText.getText("MinorCloses", minor.getId()));
            checkFlotation(major);

        } else {
            ReportBuffer.add(this, "");
            ReportBuffer.add(this, LocalText.getText("CLOSE_MINOR_LOG",
                    currentPlayer.getId(),
                    minor.getId(),
                    Bank.format(this, minorCash),
                    minorTrains ));
        }
        hasActed.set(true);

        if (!(this instanceof FinalCoalExchangeRound)) {
            companyBoughtThisTurnWrapper.set(major);

            // If >60% shares owned, lift sell obligation this round.
            if (currentPlayer.getPortfolioModel().getShare(major)
                    > getGameParameterAsInt(GameDef.Parm.PLAYER_SHARE_LIMIT)) {
                setSellObligationLifted (major);
            }

            setPriority();
        }

        return true;
    }

    public boolean discardTrain(DiscardTrain action) {

        Train train = action.getDiscardedTrain();
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
                                company.getId(),
                                train.toText() );
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotDiscardTrain",
                    companyName,
                    train.toText(),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        
        // FIXME: if (action.isForced()) changeStack.linkToPreviousMoveSet();
        pool.addTrain(train);
        ReportBuffer.add(this, LocalText.getText("CompanyDiscardsTrain",
                companyName,
                train.toText() ));

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
    
    @Override
    protected void finishRound() {

        if (discardingTrains.value()) {

            super.finishRound();

        } else if (!compWithExcessTrains.isEmpty()) {

            discardingTrains.set(true);

            // Make up a list of train discarding companies in operating sequence.
            PublicCompany[] operatingCompanies = setOperatingCompanies().toArray(new PublicCompany[0]);
            discardingCompanies =
                    new PublicCompany[compWithExcessTrains.size()];
            for (int i = 0, j = 0; i < operatingCompanies.length; i++) {
                if (compWithExcessTrains.contains(operatingCompanies[i])) {
                    discardingCompanies[j++] = operatingCompanies[i];
                }
            }

            discardingCompanyIndex.set(0);
            PublicCompany discardingCompany =
                    discardingCompanies[discardingCompanyIndex.value()];
            setCurrentPlayer(discardingCompany.getPresident());

        } else {

            super.finishRound();
        }
    }
    
    /* (non-Javadoc)
     * @see net.sf.rails.game.StockRound#startCompany(java.lang.String, rails.game.action.StartCompany)
     */
    @Override
    public boolean startCompany(String playerName, StartCompany action) {
    
        return super.startCompany(playerName, action);
    }
}

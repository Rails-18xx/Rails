package net.sf.rails.game.specific._18EU;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import rails.game.action.BuyTrain;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Owner;


/**
 * Implements a basic Operating Round. <p> A new instance must be created for
 * each new Operating Round. At the end of a round, the current instance should
 * be discarded. <p> Permanent memory is formed by static attributes.
 */
public class OperatingRound_18EU extends OperatingRound {

    protected final TrainCertificateType pullmannType;

    protected final BooleanState hasPullmannAtStart = BooleanState.create(this, "ORCompanyHasPullmannAtStart");

    /**
     * Constructed via Configure
     */
    public OperatingRound_18EU (GameManager parent, String id) {
        super(parent, id);
        pullmannType = trainManager.getCertTypeByName("P");
    }
    
    @Override
    protected void initTurn() {
        super.initTurn();
        hasPullmannAtStart.set(operatingCompany.value().getPortfolioModel()
                .getTrainOfType(pullmannType) != null);
    }

    /**
     * Modify possibleActions to follow the Pullmann train trading rules.
     */
    @Override
    public void setBuyableTrains() {

        if (operatingCompany.value() == null) return;

        int cash = operatingCompany.value().getCash();

        int cost;
        Set<Train> trains;
        BuyTrain bt;

        boolean hasTrains =
                operatingCompany.value().getPortfolioModel().getNumberOfTrains() > 0;

        // Cannot buy a train without any cash, unless you have to
        if (cash == 0 && hasTrains) return;

        boolean canBuyTrainNow = canBuyTrainNow();
        if (!canBuyTrainNow) return;

        boolean presidentMayHelp = operatingCompany.value().mustOwnATrain();
        Train cheapestTrain = null;
        int costOfCheapestTrain = 0;

        String extraMessage = null;
        boolean mustExchangePullmann = !isBelowTrainLimit()
                && hasPullmannAtStart.value()
                && !possibleActions.contains(BuyTrain.class);
        if (mustExchangePullmann) {
            extraMessage = LocalText.getText("AutodiscardTrain",
                    pullmannType.toText());
        }
         /* New trains */
        trains = trainManager.getAvailableNewTrains();
        for (Train train : trains) {
            cost = train.getCost();
            if (cost <= cash) {
                if (canBuyTrainNow) {
                    bt = new BuyTrain(train, ipo.getParent(), cost);
                    if (mustExchangePullmann) bt.setExtraMessage(extraMessage);
                    possibleActions.add(bt);
                }
            } else if (costOfCheapestTrain == 0 || cost < costOfCheapestTrain) {
                cheapestTrain = train;
                costOfCheapestTrain = cost;
            }

        }

        /* Used trains */
        trains = pool.getUniqueTrains();
        for (Train train : trains) {
            // May not buy Pullmann if one is already owned,
            // or if no train is owned at all
            if (train.getCertType() == pullmannType
                    &&(operatingCompany.value().getPortfolioModel().getTrainOfType(pullmannType) != null
                            || !hasTrains)) {
                continue;
            }
            cost = train.getCost();
            if (cost <= cash) {
                bt = new BuyTrain(train, pool.getParent(), cost);
                if (mustExchangePullmann) bt.setExtraMessage(extraMessage);
                possibleActions.add(bt);
            } else if (costOfCheapestTrain == 0 || cost < costOfCheapestTrain) {
                cheapestTrain = train;
                costOfCheapestTrain = cost;
            }
        }
        if (!hasTrains && presidentMayHelp
                && possibleActions.getType(BuyTrain.class).isEmpty()
                && cheapestTrain != null) {
            bt = new BuyTrain(cheapestTrain, cheapestTrain.getOwner(), costOfCheapestTrain);
            bt.setPresidentMustAddCash(costOfCheapestTrain - cash);
            if (mustExchangePullmann) bt.setExtraMessage(extraMessage);
            possibleActions.add(bt);
        }

        /* Other company trains, sorted by president (current player first) */
        if (Phase.getCurrent(this).isTrainTradingAllowed()) {
            Player p;
            PortfolioModel pf;
            int index;
            int numberOfPlayers = playerManager.getNumberOfPlayers();

            // Set up a list per player of presided companies
            List<List<PublicCompany>> companiesPerPlayer =
                    new ArrayList<List<PublicCompany>>(numberOfPlayers);
            for (int i = 0; i < numberOfPlayers; i++)
                companiesPerPlayer.add(new ArrayList<PublicCompany>(4));
            List<PublicCompany> companies;
            // Sort out which players preside over wich companies.
            for (PublicCompany c : operatingCompanies.view()) {
                if (c == operatingCompany.value() || c.isClosed()) continue;
                p = c.getPresident();
                index = p.getIndex();
                companiesPerPlayer.get(index).add(c);
            }
            // Scan trains per company per player, operating company president
            // first
            int currentPlayerIndex = operatingCompany.value().getPresident().getIndex();
            for (int i = currentPlayerIndex; i < currentPlayerIndex
                                                 + numberOfPlayers; i++) {
                companies = companiesPerPlayer.get(i % numberOfPlayers);
                for (PublicCompany company : companies) {
                    pf = company.getPortfolioModel();
                    trains = pf.getUniqueTrains();

                    for (Train train : trains) {
                        if (train.getCertType() == pullmannType) continue;
                        bt = new BuyTrain(train, pf.getParent(), 0);
                        if (mustExchangePullmann) bt.setExtraMessage(extraMessage);
                        possibleActions.add(bt);
                    }
                }
            }
        }

    }

    /** In 18EU, a company can (effectively) exchange a Pullmann */
    @Override
    protected boolean canBuyTrainNow() {
        return super.canBuyTrainNow() || hasPullmann ()
               && hasPullmannAtStart.value();
    }

    @Override
    public boolean buyTrain(BuyTrain action) {

        boolean mustDiscardPullmann = !super.isBelowTrainLimit() && hasPullmann ();

        boolean result = super.buyTrain(action);

        // If we are at train limit and have a Pullmann, discard it
        if (mustDiscardPullmann) {
            Train pullmann = operatingCompany.value().getPortfolioModel().getTrainOfType(pullmannType);
            if (pullmann != null) {  // must be non-null
                pullmann.discard();
            }
        }

        // If train was bought from another company, check for a lone Pullmann
        Owner seller = action.getFromOwner();
        if (seller instanceof PublicCompany
                && !(action.getTrain().getCertType() == pullmannType)) {
            boolean hasPullmann = false;
            boolean hasNonPullmann = false;
            Train pullmann = null;
            for (Train sellerTrain : ((PublicCompany)seller).getPortfolioModel().getTrainList()) {
                if (sellerTrain.getCertType() == pullmannType) {
                    hasPullmann = true;
                    pullmann = sellerTrain;
                } else if (sellerTrain != null){
                    hasNonPullmann = true;
                }
            }
            if (hasPullmann && !hasNonPullmann) {
                pullmann.discard();
            }
        }

        // Check if we have just started Phase 5 and
        // if we still have at least one Minor operating.
        // If so, record the current player as the first
        // one to act in the Final Minor Exchange Round.
        if (result && getRoot().getPhaseManager().hasReachedPhase("5")
            && operatingCompanies.get(0).getType().getId().equalsIgnoreCase("Minor")
            && ((GameManager_18EU)gameManager).getPlayerToStartFMERound() == null) {
            ((GameManager_18EU)gameManager).setPlayerToStartFMERound(operatingCompany.value().getPresident());
        }

        return result;

    }

    /** Special rules for Pullmann trains */
    @Override
    public boolean checkForExcessTrains() {

        excessTrainCompanies = new HashMap<Player, List<PublicCompany>>();
        Player player;
        Train pullmann;
        PortfolioModel portfolio;
        int numberOfTrains;

        for (PublicCompany comp : operatingCompanies.view()) {
            portfolio = comp.getPortfolioModel();
            numberOfTrains = portfolio.getNumberOfTrains();

            // Check if the company has a Pullmann
            pullmann = portfolio.getTrainOfType(pullmannType);

            // A Pullmann always goes first, and automatically.
            // If the last train is a Pullmann, discard it.
            if ((numberOfTrains > comp.getCurrentTrainLimit() || numberOfTrains == 1)
                && pullmann != null) {
                pool.addTrain(pullmann);
                numberOfTrains--;
            }

            // If we are still above the limit, make the list
            // of trains to select the discarded one from
            if (numberOfTrains > comp.getCurrentTrainLimit()) {
                player = comp.getPresident();
                if (!excessTrainCompanies.containsKey(player)) {
                    excessTrainCompanies.put(player,
                            new ArrayList<PublicCompany>(2));
                }
                excessTrainCompanies.get(player).add(comp);
            }
        }
        return !excessTrainCompanies.isEmpty();
    }

    private boolean hasPullmann () {
        return operatingCompany.value().getPortfolioModel().getTrainOfType(pullmannType) != null;
    }

    @Override
    public void resume() {
        if (playerManager.getCurrentPlayer().isBankrupt()) {
            // Do not complete the train buying action
            savedAction = null;
            finishTurn();
        }
        if (gameManager.getCurrentRound() == this) {
            super.resume();
        }
    }

}

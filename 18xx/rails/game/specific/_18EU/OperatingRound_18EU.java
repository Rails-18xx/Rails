/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_18EU/OperatingRound_18EU.java,v 1.16 2010/05/23 08:18:24 evos Exp $ */
package rails.game.specific._18EU;

import java.util.*;

import rails.game.*;
import rails.game.action.BuyTrain;
import rails.game.state.BooleanState;
import rails.util.LocalText;

/**
 * Implements a basic Operating Round. <p> A new instance must be created for
 * each new Operating Round. At the end of a round, the current instance should
 * be discarded. <p> Permanent memory is formed by static attributes.
 */
public class OperatingRound_18EU extends OperatingRound {

    protected TrainTypeI pullmannType;

    protected BooleanState hasPullmannAtStart =
            new BooleanState("ORCompanyHasPullmannAtStart", false);

    public OperatingRound_18EU (GameManagerI gameManager) {
        super (gameManager);
        pullmannType = trainManager.getTypeByName("P");
    }

    @Override
    protected void initTurn() {
        super.initTurn();
        hasPullmannAtStart.set(operatingCompany.get().getPortfolio()
                .getTrainOfType(pullmannType) != null);
    }

    /**
     * Modify possibleActions to follow the Pullmann train trading rules.
     */
    @Override
    public void setBuyableTrains() {

        if (operatingCompany.get() == null) return;

        int cash = operatingCompany.get().getCash();

        int cost;
        List<TrainI> trains;
        BuyTrain bt;

        boolean hasTrains =
                operatingCompany.get().getPortfolio().getNumberOfTrains() > 0;

        // Cannot buy a train without any cash, unless you have to
        if (cash == 0 && hasTrains) return;

        boolean canBuyTrainNow = canBuyTrainNow();
        if (!canBuyTrainNow) return;

        boolean presidentMayHelp = operatingCompany.get().mustOwnATrain();
        TrainI cheapestTrain = null;
        int costOfCheapestTrain = 0;
        TrainManager trainMgr = gameManager.getTrainManager();

        String extraMessage = null;
        boolean mustExchangePullmann = !isBelowTrainLimit()
                && hasPullmannAtStart.booleanValue()
                && !possibleActions.contains(BuyTrain.class);
        if (mustExchangePullmann) {
            extraMessage = LocalText.getText("AutodiscardTrain",
                    pullmannType.getName());
        }
         /* New trains */
        trains = trainMgr.getAvailableNewTrains();
        for (TrainI train : trains) {
            cost = train.getCost();
            if (cost <= cash) {
                if (canBuyTrainNow) {
                    bt = new BuyTrain(train, ipo, cost);
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
        for (TrainI train : trains) {
            // May not buy Pullmann if one is already owned,
            // or if no train is owned at all
            if (train.getType().getName().equals("P")
                    &&(operatingCompany.get().getPortfolio().getTrainOfType(pullmannType) != null
                            || !hasTrains)) {
                continue;
            }
            cost = train.getCost();
            if (cost <= cash) {
                bt = new BuyTrain(train, pool, cost);
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
            bt = new BuyTrain(cheapestTrain, cheapestTrain.getHolder(), costOfCheapestTrain);
            bt.setPresidentMustAddCash(costOfCheapestTrain - cash);
            if (mustExchangePullmann) bt.setExtraMessage(extraMessage);
            possibleActions.add(bt);
        }

        /* Other company trains, sorted by president (current player first) */
        if (getCurrentPhase().isTrainTradingAllowed()) {
            Player p;
            Portfolio pf;
            int index;
            int numberOfPlayers = getNumberOfPlayers();

            // Set up a list per player of presided companies
            List<List<PublicCompanyI>> companiesPerPlayer =
                    new ArrayList<List<PublicCompanyI>>(numberOfPlayers);
            for (int i = 0; i < numberOfPlayers; i++)
                companiesPerPlayer.add(new ArrayList<PublicCompanyI>(4));
            List<PublicCompanyI> companies;
            // Sort out which players preside over wich companies.
            for (PublicCompanyI c : operatingCompanies.viewList()) {
                if (c == operatingCompany.get() || c.isClosed()) continue;
                p = c.getPresident();
                index = p.getIndex();
                companiesPerPlayer.get(index).add(c);
            }
            // Scan trains per company per player, operating company president
            // first
            int currentPlayerIndex = operatingCompany.get().getPresident().getIndex();
            for (int i = currentPlayerIndex; i < currentPlayerIndex
                                                 + numberOfPlayers; i++) {
                companies = companiesPerPlayer.get(i % numberOfPlayers);
                for (PublicCompanyI company : companies) {
                    pf = company.getPortfolio();
                    trains = pf.getUniqueTrains();

                    for (TrainI train : trains) {
                        if (train.getType().getName().equals("P")) continue;
                        bt = new BuyTrain(train, pf, 0);
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
               && hasPullmannAtStart.booleanValue();
    }

    @Override
    public boolean buyTrain(BuyTrain action) {

        boolean mustDiscardPullmann = !super.isBelowTrainLimit() && hasPullmann ();

        boolean result = super.buyTrain(action);

        // If we are at train limit and have a Pullmann, discard it
        if (mustDiscardPullmann) {
            TrainI pullmann = operatingCompany.get().getPortfolio().getTrainOfType(pullmannType);
            if (pullmann != null) {  // must be non-null
                pullmann.moveTo(pool);
                ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                        operatingCompany.get().getName(),
                        pullmann.getName() ));

            }
        }

        // If train was bought from another company, check for a lone Pullmann
        Portfolio seller = action.getFromPortfolio();
        if (seller.getOwner() instanceof PublicCompanyI
                && !action.getTrain().getName().equalsIgnoreCase("P")) {
            boolean hasPullmann = false;
            boolean hasNonPullmann = false;
            TrainI pullmann = null;
            for (TrainI sellerTrain : seller.getTrainList()) {
                if ("P".equalsIgnoreCase(sellerTrain.getName())) {
                    hasPullmann = true;
                    pullmann = sellerTrain;
                } else if (sellerTrain != null){
                    hasNonPullmann = true;
                }
            }
            if (hasPullmann && !hasNonPullmann) {
                pullmann.moveTo (pool);
                ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                        seller.getOwner().getName(),
                        pullmann.getName() ));
            }
        }

        // Check if we have just started Phase 5 and
        // if we still have at least one Minor operating.
        // If so, record the current player as the first
        // one to act in the Final Minor Exchange Round.
        if (result && gameManager.getPhaseManager().hasReachedPhase("5")
            && operatingCompanies.get(0).getTypeName().equalsIgnoreCase("Minor")
            && ((GameManager_18EU)gameManager).getPlayerToStartFMERound() == null) {
            ((GameManager_18EU)gameManager).setPlayerToStartFMERound(operatingCompany.get().getPresident());
        }

        return result;

    }

    /** Special rules for Pullmann trains */
    @Override
    public boolean checkForExcessTrains() {

        excessTrainCompanies = new HashMap<Player, List<PublicCompanyI>>();
        Player player;
        TrainI pullmann;
        Portfolio portfolio;
        int numberOfTrains;

        for (PublicCompanyI comp : operatingCompanies.viewList()) {
            portfolio = comp.getPortfolio();
            numberOfTrains = portfolio.getNumberOfTrains();

            // Check if the company has a Pullmann
            pullmann = portfolio.getTrainOfType(pullmannType);

            // A Pullmann always goes first, and automatically.
            // If the last train is a Pullmann, discard it.
            if ((numberOfTrains > comp.getTrainLimit(getCurrentPhase().getIndex()) || numberOfTrains == 1)
                && pullmann != null) {
                pullmann.moveTo(pool);
                numberOfTrains--;
            }

            // If we are still above the limit, make the list
            // of trains to select the discarded one from
            if (numberOfTrains > comp.getTrainLimit(getCurrentPhase().getIndex())) {
                player = comp.getPresident();
                if (!excessTrainCompanies.containsKey(player)) {
                    excessTrainCompanies.put(player,
                            new ArrayList<PublicCompanyI>(2));
                }
                excessTrainCompanies.get(player).add(comp);
            }
        }
        return !excessTrainCompanies.isEmpty();
    }

    private boolean hasPullmann () {
        return operatingCompany.get().getPortfolio().getTrainOfType(pullmannType) != null;
    }

    @Override
    public void resume() {
        if (getCurrentPlayer().isBankrupt()) {
            // Do not complete the train buying action
            savedAction = null;
            finishTurn();
        }
        if (gameManager.getCurrentRound() == this) {
            super.resume();
        }
    }

}

/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_18EU/OperatingRound_18EU.java,v 1.11 2010/01/18 22:51:47 evos Exp $ */
package rails.game.specific._18EU;

import java.util.*;

import rails.game.*;
import rails.game.action.BuyTrain;
import rails.game.state.BooleanState;
import rails.game.state.State;

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
        hasPullmannAtStart.set(operatingCompany.getPortfolio()
                .getTrainOfType(pullmannType) != null);
    }

    /**
     * Modify possibleActions to follow the Pullmann train trading rules.
     */
    @Override
    public void setBuyableTrains() {

        if (operatingCompany == null) return;

        TrainManager trainMgr = gameManager.getTrainManager();

        int cash = operatingCompany.getCash();
        int cost;
        List<TrainI> trains;
        // TrainI train;
        boolean hasTrains =
                operatingCompany.getPortfolio().getNumberOfTrains() > 0;
        boolean atTrainLimit =
                operatingCompany.getNumberOfTrains() >= operatingCompany.getCurrentTrainLimit();
        boolean canBuyTrainNow = isBelowTrainLimit();
        boolean presidentMayHelp = operatingCompany.mustOwnATrain();
        TrainI cheapestTrain = null;
        int costOfCheapestTrain = 0;

        // Check if the company already has a Pullmann
        TrainI ownedPTrain = null;
        if (hasTrains) {
            ownedPTrain = operatingCompany.getPortfolio().getTrainOfType(pullmannType);
        }

        // Postpone train limit checking, because an exchange might be possible

        /* New trains */
        trains = trainMgr.getAvailableNewTrains();
        for (TrainI train : trains) {
            cost = train.getCost();
            if (cost <= cash) {
                if (canBuyTrainNow)
                    possibleActions.add(new BuyTrain(train, ipo, cost));
            } else if (costOfCheapestTrain == 0 || cost < costOfCheapestTrain) {
                cheapestTrain = train;
                costOfCheapestTrain = cost;
            }

            // At train limit, exchange of a Pullmann is allowed
            if (atTrainLimit && ownedPTrain != null) {
                BuyTrain action = new BuyTrain(train, ipo, cost);
                List<TrainI> pTrains = new ArrayList<TrainI>();
                pTrains.add(ownedPTrain);
                action.setTrainsForExchange(pTrains);
                action.setForcedExchange(true);
                possibleActions.add(action);
            }
            if (!canBuyTrainNow) return;

        }
        if (!canBuyTrainNow) return;

        /* Used trains */
        trains = pool.getUniqueTrains();
        for (TrainI train : trains) {
            // May not buy Pullmann if one is already owned,
            // or if no train is owned at all
            if ((ownedPTrain != null || !hasTrains)
                && train.getType().getName().equals("P")) {
                continue;
            }
            cost = train.getCost();
            if (cost <= cash) {
                possibleActions.add(new BuyTrain(train, pool, cost));
            } else if (costOfCheapestTrain == 0 || cost < costOfCheapestTrain) {
                cheapestTrain = train;
                costOfCheapestTrain = cost;
            }
        }
        if (!hasTrains && presidentMayHelp
            && possibleActions.getType(BuyTrain.class).isEmpty()
            && cheapestTrain != null) {
            possibleActions.add(new BuyTrain(cheapestTrain,
                    cheapestTrain.getHolder(), costOfCheapestTrain).setPresidentMustAddCash(costOfCheapestTrain
                                                                                            - cash));
        }

        if (!canBuyTrainNow) return;

        /* Other company trains, sorted by president (current player first) */
        if (getCurrentPhase().isTrainTradingAllowed()) {
            PublicCompanyI c;
            BuyTrain bt;
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
            for (int j = 0; j < operatingCompanyArray.length; j++) {
                c = operatingCompanyArray[j];
                if (c == operatingCompany) continue;
                p = c.getPresident();
                index = p.getIndex();
                companiesPerPlayer.get(index).add(c);
            }
            // Scan trains per company per player, operating company president
            // first
            int currentPlayerIndex = operatingCompany.getPresident().getIndex();
            for (int i = currentPlayerIndex; i < currentPlayerIndex
                                                 + numberOfPlayers; i++) {
                companies = companiesPerPlayer.get(i % numberOfPlayers);
                for (PublicCompanyI company : companies) {
                    pf = company.getPortfolio();
                    trains = pf.getUniqueTrains();

                    for (TrainI train : trains) {
                        if (train.getType().getName().equals("P")) continue;
                        bt = new BuyTrain(train, pf, 0);
                        possibleActions.add(bt);
                    }
                }
            }
        }
    }

    /** In 18EU, a company can (effectively) exchange a Pullmann */
    @Override
    protected boolean isBelowTrainLimit() {
        return super.isBelowTrainLimit()
               || operatingCompany.getPortfolio().getTrainOfType(pullmannType) != null
               && hasPullmannAtStart.booleanValue();
    }

    @Override
    public boolean buyTrain(BuyTrain action) {
        boolean result = super.buyTrain(action);

        // Check if we have just started Phase 5 and
        // if we still have at least one Minor operating.
        // If so, record the current player as the first
        // one to act in the Final Minor Exchange Round.
        if (result && gameManager.getPhaseManager().hasReachedPhase("5")
            && operatingCompanyArray[0].getTypeName().equals("Minor")
            && ((GameManager_18EU)gameManager).getPlayerToStartFMERound() == null) {
            ((GameManager_18EU)gameManager).setPlayerToStartFMERound(operatingCompany.getPresident());
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

        for (PublicCompanyI comp : operatingCompanyArray) {
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

}

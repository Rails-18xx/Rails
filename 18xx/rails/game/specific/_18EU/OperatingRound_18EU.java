/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_18EU/OperatingRound_18EU.java,v 1.1 2008/02/23 20:54:39 evos Exp $ */
package rails.game.specific._18EU;


import java.util.*;

import rails.game.*;
import rails.game.action.BuyTrain;
import rails.game.action.DiscardTrain;
import rails.game.state.BooleanState;

/**
 * Implements a basic Operating Round.
 * <p>
 * A new instance must be created for each new Operating Round. At the end of a
 * round, the current instance should be discarded.
 * <p>
 * Permanent memory is formed by static attributes.
 */
public class OperatingRound_18EU extends OperatingRound
{
	
	protected BooleanState hasPullmannAtStart
		= new BooleanState ("ORCompanyHasPullmannAtStart", false);

	protected void initTurn () {
		super.initTurn();
		hasPullmannAtStart.set(operatingCompany.getPortfolio().getTrainOfType("P") != null);
	}
	
    /**
     * Modify possibleActions to follow the Pullmann train
     * trading rules.
     */
    @Override
    public void setBuyableTrains() {

        super.setBuyableTrains();

        // Pullmans may not be bought from other players
        BuyTrain pAction = null;
        for (BuyTrain action : possibleActions.getType(BuyTrain.class)) {
            if (action.getTrain().getType().getName().equals("P")) {
                if (action.getFromPortfolio() == Bank.getPool()) {
                    pAction = action;
                } else {
                    possibleActions.remove(action);
                }
            }
        }

        // Check if the company has any train
        int trainsOwned = operatingCompany.getNumberOfTrains();
        TrainI ownedPTrain = null;
        if (trainsOwned > 0) {
            // Check if the company has a P-train
            ownedPTrain = operatingCompany.getPortfolio().getTrainOfType("P");
        }

        // Remove the P-train buy action if already one is owned
        if (pAction != null
            && (ownedPTrain != null || trainsOwned == 0)) {
                possibleActions.remove(pAction);
        }

        // If the company is at its train limit and has a Pullmann,
        // BuyTrain actions become exchange actions.
        if (ownedPTrain != null &&
        		operatingCompany.getNumberOfTrains()
        			== operatingCompany.getCurrentTrainLimit()) {
        	List<TrainI> pTrains = new ArrayList<TrainI>();
        	pTrains.add(ownedPTrain);
	        for (BuyTrain action : possibleActions.getType(BuyTrain.class)) {
	        	action.setTrainsForExchange(pTrains);
	        	action.setForcedExchange(true);
	        }
        }
    }
    
    /** In 18EU, a company can (effectively) exchange a Pullmann */
    protected boolean canBuyTrain () {
    	return super.canBuyTrain() 
    	|| operatingCompany.getPortfolio().getTrainOfType("P") != null
    		&& hasPullmannAtStart.booleanValue();
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
            pullmann = portfolio.getTrainOfType("P");

            // A Pullmann always goes first, and automatically.
            // If the last train is a Pullmann, discard it.
            if ((numberOfTrains > comp.getTrainLimit(currentPhase.getIndex())
                    || numberOfTrains == 1)
                    && pullmann != null) {
                pullmann.moveTo(Bank.getPool());
            }

            // If we are still above the limit, make the list
            // of trains to select the discarded one from
            if (numberOfTrains > comp.getTrainLimit(currentPhase.getIndex()))
            {
                player = comp.getPresident();
                if (!excessTrainCompanies.containsKey(player)) {
                    excessTrainCompanies.put(player, new ArrayList<PublicCompanyI>(2));
                }
                excessTrainCompanies.get(player).add(comp);
            }
        }
        return !excessTrainCompanies.isEmpty();
   }

}

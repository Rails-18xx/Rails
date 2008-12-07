package rails.game.specific._18EU;

import java.util.ArrayList;
import java.util.List;

import rails.game.*;
import rails.game.action.MergeCompanies;
import rails.game.action.NullAction;
import rails.game.move.MoveSet;
import rails.util.LocalText;

/**
 * Implements a basic Stock Round. <p> A new instance must be created for each
 * new Stock Round. At the end of a round, the current instance should be
 * discarded. <p> Permanent memory is formed by static attributes (like who has
 * the Priority Deal).
 */
public class FinalMinorExchangeRound extends StockRound_18EU {
    OperatingRound_18EU lastOR;

	/**
	 * Constructor with no parameters, will call super class (StockRound_18EU's) Constructor to initialize
	 *
	 */
    public FinalMinorExchangeRound() 
	{
		super ();
	}

	/**
	 * Constructor with the GameManager, will call super class (StockRound_18EU's) Constructor to initialize
	 *
	 * @param aGameManager The GameManager Object needed to initialize the Stock Round
	 *
	 */
	public FinalMinorExchangeRound(GameManager aGameManager) {
		super (aGameManager);
	}
	
    public void start(OperatingRound_18EU lastOR) {
        ReportBuffer.add("\n"
                         + LocalText.getText("StartFinalMinorExchangeRound"));

        this.lastOR = lastOR;
        Player firstPlayer = lastOR.getPlayerToStartExchangeRound();
        setCurrentPlayerIndex(firstPlayer.getIndex());
        initPlayer();
        ReportBuffer.add(LocalText.getText("HasFirstTurn",
                new String[] { currentPlayer.getName() }));
    }

    /*----- General methods -----*/

    @Override
    public boolean setPossibleActions() {

        if (discardingTrains.booleanValue()) {
            return setTrainDiscardActions();
        } else {
            return setMinorMergeActions();
        }

    }

    private boolean setMinorMergeActions() {

        if (hasActed.booleanValue()) {
            possibleActions.add(new NullAction(NullAction.DONE));
            return true;
        }

        List<PublicCompanyI> comps =
                Game.getCompanyManager().getAllPublicCompanies();
        List<PublicCompanyI> minors = new ArrayList<PublicCompanyI>();
        List<PublicCompanyI> targetCompanies = new ArrayList<PublicCompanyI>();
        String type;

        for (PublicCompanyI comp : comps) {
            type = comp.getTypeName();
            if (type.equals("Major") && comp.hasStarted() && !comp.isClosed()) {
                targetCompanies.add(comp);
            } else if (type.equals("Minor")) {
                if (comp.isClosed()) continue;
                if (comp.getPresident() == currentPlayer) {
                    minors.add(comp);
                }
            }
        }

        while (minors.isEmpty()) {
            setNextPlayer();
            for (PublicCompanyI comp : comps) {
                type = comp.getTypeName();
                if (type.equals("Minor")) {
                    if (comp.isClosed()) continue;
                    if (comp.getPresident() == currentPlayer) {
                        minors.add(comp);
                    }
                }
            }
        }
        // Use null target to indicate minor closing is an option
        targetCompanies.add(null);

        for (PublicCompanyI minor : minors) {
            possibleActions.add(new MergeCompanies(minor, targetCompanies));
        }

        return true;
    }

    @Override
    public boolean done(String playerName) {

        MoveSet.start(false);

        for (PublicCompanyI comp : Game.getCompanyManager().getAllPublicCompanies()) {
            if (comp.getTypeName().equals("Minor")) {
                if (!comp.isClosed()) {
                    finishTurn();
                    return true;
                }
            }
        }

        finishRound();
        return true;
    }

    @Override
    protected void initPlayer() {

        currentPlayer = getCurrentPlayer();
        hasActed.set(false);

    }

    /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/

    @Override
    public String toString() {
        return "FinalMinorExchangeRound";
    }
}
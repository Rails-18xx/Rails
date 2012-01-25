/**
 * 
 */
package rails.game.specific._1880;

import rails.game.GameManager;
import rails.game.PhaseI;
import rails.game.RoundI;
import rails.game.StartRound;
import rails.game.StockRound;
import rails.game.state.IntegerState;

/**
 * @author Martin Brumm
 * @date 21.1.2012
 * 
 */
public class GameManager_1880 extends GameManager {
   
    private RoundI previousRound = null;
    public IntegerState numOfORs = new IntegerState("numOfORs");
    //Keeps track of the company that purchased the last train 
    private PublicCompany_1880 lastTrainBuyingCompany;
        /**
     * 
     */
    public GameManager_1880() {
        super();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManager#nextRound(rails.game.RoundI)
     */
    @Override
    public void nextRound(RoundI round) {
        if (round instanceof StartRound) { // BCR Operates only if all privates are sold out
            if (startPacket != null && !startPacket.areAllSold()) {
                startOperatingRound(runIfStartPacketIsNotCompletelySold());
                } else {
                    startStockRound();
                }
            numOfORs.set(10);
            } else if (round instanceof StockRound) {
                if (interruptedRound != null) {
                    setRound(interruptedRound);
                    interruptedRound.resume();
                    interruptedRound = null;
                } else { // First StockRound after StartRound...
                    PhaseI currentPhase = getCurrentPhase();
                    if (currentPhase == null) log.error ("Current Phase is null??", new Exception (""));
                    // Create a new OperatingRound (never more than one Stock Round)
                    // OperatingRound.resetRelativeORNumber();
                    
                    relativeORNumber.set(1);
                    
                    startOperatingRound(true);
                    }
            } else if ( round instanceof OperatingRound_1880) {
                if (gameOverPending.booleanValue() && !gameEndsAfterSetOfORs) {

                    finishGame();

                } else if (relativeORNumber.add(1) <= numOfORs.intValue()) {
                    // There will be another OR
                    startOperatingRound(true);
                } else if (startPacket != null && !startPacket.areAllSold()) {
                    startStartRound();
                } else {
                    if (gameOverPending.booleanValue() && gameEndsAfterSetOfORs) {
                        finishGame();
                    }
                }
            }
    }// End of nextRound

    /**
     * @return the lastTrainBuyingCompany
     */
    public PublicCompany_1880 getLastTrainBuyingCompany() {
        return lastTrainBuyingCompany;
    }

    /**
     * @param lastTrainBuyingCompany the lastTrainBuyingCompany to set
     */
    public void setLastTrainBuyingCompany(PublicCompany_1880 lastTrainBuyingCompany) {
        this.lastTrainBuyingCompany = lastTrainBuyingCompany;
    }

	/* (non-Javadoc)
	 * @see rails.game.GameManager#startStockRound()
	 */
	
	protected void startStockRound_1880(OperatingRound_1880 or) {
		// TODO Auto-generated method stub
		interruptedRound = or;
		super.startStockRound();
	}


    
}

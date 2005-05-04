/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/GameManager.java,v 1.1 2005/05/04 22:48:31 evos Exp $
 * 
 * Created on 04-May-2005
 * Change Log:
 */
package game;

/**
 * This class manages the playing rounds by supervising all implementations of Round.
 * <p>Currently everything is hardcoded à la 1830. 
 * @author Erik Vos
 */
public class GameManager {
    
    protected Round currentRound = null;
    protected int orNumber;
    protected int numOfORs;
    protected int[] orsPerPhase = new int[] {1, 2, 2, 3, 3};
    protected int phase = 0;
    
    protected static GameManager instance;
    
    /**
     * Private constructor.
     *
     */
    private GameManager () {
        currentRound = new StockRound();
    }
    
    public static GameManager getInstance() {
        if (instance == null) instance = new GameManager();
        return instance;
    }
    
    /**
     * Should be called by each Round when it finishes.
     * @param round The object that represents the finishing round.
     */
    public void nextRound (Round round) {
        
        if (round instanceof StockRound) {
            
            // Create a new OperatingRound (never more than one Stock Round)
			OperatingRound.resetRelativeORNumber();
            currentRound = new OperatingRound();
            numOfORs = orsPerPhase[phase];
            orNumber = 1;
            
        } else if (round instanceof OperatingRound) {
            
            if (++orNumber < numOfORs) {
                
                // There will be another OR
                currentRound = new OperatingRound();
                
            } else {
                
                // Time for a new SR
                currentRound = new StockRound();
            }
        }
        
     }
    
    /**
     * Should be called whenever a Phase changes.
     * The effect on the number of ORs is delayed until a StockRound finishes.
     *
     */
    public void nextPhase () {
        if (phase < orsPerPhase.length - 1) phase++;
    }
    
    public int getPhase () {
        return phase;
    }
    
    public Round getCurrentRound() {
        return currentRound;
    }

}

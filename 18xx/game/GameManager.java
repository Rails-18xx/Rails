/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/GameManager.java,v 1.3 2005/05/15 20:47:14 evos Exp $
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
    
    protected static Player[] players;
    protected static int numberOfPlayers;
    protected static int currentPlayerIndex = 0;
    protected static Player currentPlayer = null;
    protected static int priorityPlayerIndex = 0;
    protected static Player priorityPlayer = null;
    
    /**
     * Current round should not be set here but from within the Round classes.
     * This is because in some cases the round has already changed to another
     * one when the constructor terminates. Example: if the privates have not 
     * been sold, it finishes by starting an Operating Round, which handles the
     * privates payout and then immediately starts a new Start Round.
     */
    protected static Round currentRound = null;
    
    protected Round insertingRound = null;
    protected Round insertedRound = null;
    protected int orNumber;
    protected int numOfORs;
    protected int[] orsPerPhase = new int[] {1, 2, 2, 3, 3};
    protected int phase = 0;
    
    protected static GameManager instance;
    protected StartPacket startPacket;
    
    /**
     * Private constructor.
     *	
     */
    private GameManager () {
        instance = this;
        
        players = Game.getPlayerManager().getPlayersArray();
        numberOfPlayers = players.length;

        if (startPacket == null) startPacket = StartPacket.getStartPacket("Initial");
        if (startPacket != null && !startPacket.areAllSold()) {
            // If we have a non-exhausted start packet 
            startStartRound ();
        } else {
            startStockRound();
        }
    }
    
    public static GameManager getInstance() {
        if (instance == null) instance = new GameManager();
        return instance;
    }
    
    public void setRound (Round round) {
        
        currentRound = round;
    }
    
    /**
     * Should be called by each Round when it finishes.
     * @param round The object that represents the finishing round.
     */
    public void nextRound (Round round) {
        
        if (round instanceof StartRound) {
            
            if (startPacket != null && !startPacket.areAllSold()) {

                startOperatingRound();
                
            } else {
                
                startStockRound();
                
            }                
            
        } else if (round instanceof StockRound) {
            
            // Create a new OperatingRound (never more than one Stock Round)
			OperatingRound.resetRelativeORNumber();
            startOperatingRound();
            
            numOfORs = orsPerPhase[phase];
            orNumber = 1;
            
        } else if (round instanceof OperatingRound) {
            
            if (++orNumber < numOfORs) {
                
                // There will be another OR
                startOperatingRound();
                
            } else if (startPacket != null && !startPacket.areAllSold()) {
                
                startStartRound();
                
            } else {

                startStockRound();
            }
        }
        
     }
    
    private void startStartRound () {
        
        String startRoundClassName = startPacket.getRoundClassName();
        ((StartRound)instantiate (startRoundClassName)).start(startPacket);

    }
    
    private void startStockRound () {
        
        new StockRound().start();
    }
    
    private void startOperatingRound() {

        new OperatingRound();
       
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
    /**
     * @return Returns the currentPlayerIndex.
     */
    public static int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    /**
     * @param currentPlayerIndex The currentPlayerIndex to set.
     */
    public static void setCurrentPlayerIndex(int currentPlayerIndex) {
        GameManager.currentPlayerIndex = currentPlayerIndex % numberOfPlayers;
        GameManager.currentPlayer = players[GameManager.currentPlayerIndex];
    }
    public static void setCurrentPlayer (Player player) {
        currentPlayer = player;
        for (int i=0; i<numberOfPlayers; i++) {
            if (player == players[i]) {
                currentPlayerIndex = i;
                break;
            }
        }
    }
    /**
     * @return Returns the priorityPlayerIndex.
     */
    public static int getPriorityPlayerIndex() {
        return priorityPlayerIndex;
    }
    /**
     * @param priorityPlayerIndex The priorityPlayerIndex to set.
     * The value may exceed the number of players; if so, the modulus is taken.
     * This allows giving the next player the priority bu adding +1. 
     */
    public static void setPriorityPlayerIndex(int priorityPlayerIndex) {
        GameManager.priorityPlayerIndex = priorityPlayerIndex % numberOfPlayers;
        GameManager.priorityPlayer = players[GameManager.priorityPlayerIndex];
    }
    /**
     * Set priority deal to the player after the current player.
     *
     */
    public static void setPriorityPlayer () {
        priorityPlayerIndex = (currentPlayerIndex + 1) % numberOfPlayers;
        priorityPlayer = players[priorityPlayerIndex];
       
    }
    /**
     * @return Returns the currentPlayer.
     */
    public static Player getCurrentPlayer() {
        return currentPlayer;
    }
    /**
     * @return Returns the players.
     */
    public static Player[] getPlayers() {
        return players;
    }
    /**
     * @return Returns the priorityPlayer.
     */
    public static Player getPriorityPlayer() {
        return priorityPlayer;
    }
    
    public static void setNextPlayer () {
        currentPlayerIndex = ++currentPlayerIndex % numberOfPlayers;
        currentPlayer = players[currentPlayerIndex];
    }
    
    public StartPacket getStartPacket() {
        return startPacket;
    }
    
    private Object instantiate (String className) {
        try {
            return Class.forName(className).newInstance();
        } catch (Exception e) {
            Log.error ("Cannot instantiate class "+className);
            System.out.println(e.getStackTrace());
            return null;
        }
    }

}

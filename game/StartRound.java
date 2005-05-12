/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/StartRound.java,v 1.1 2005/05/12 22:22:28 evos Exp $
 * 
 * Created on 06-May-2005
 * Change Log:
 */
package game;

import java.util.*;

/**
 * @author Erik Vos
 */
public abstract class StartRound implements StartRoundI {
    
    protected StartPacket startPacket = null;
    protected Map itemMap = null;
    protected int numPasses = 0;
    protected int numPlayers = PlayerManager.getNumberOfPlayers();
    
    
    /**
     * Will be created dynamically.
     *
     */
    public StartRound () {
        
    }
    
    
    
 }

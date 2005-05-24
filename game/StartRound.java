/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/StartRound.java,v 1.2 2005/05/24 21:38:04 evos Exp $
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
    protected int numPlayers;
    protected String variant;
    
	/*----- Start Round states -----*/
	/** The current player must buy, bid or pass */
    public static final int BID_BUY_OR_PASS = 0;
    /** The current player must set a par price */
    public static final int SET_PRICE = 1;
    /** The current player must buy or pass */
    public static final int BUY_OR_PASS = 2;
    /** The current player must buy (pass not allowed) */
    public static final int BUY = 3;
    
    /** A company in need for a par price. */
    PublicCompanyI companyNeedingPrice = null;   
    
     /**
     * Will be created dynamically.
     *
     */
    public StartRound () {
    }
    
    /**
     * Start the start round.
     * @param startPacket The startpacket to be sold in this start round.
     */
     public void start (StartPacket startPacket) {
        
        this.startPacket = startPacket;
        this.variant = GameManager.getVariant();
   	 	numPlayers = PlayerManager.getNumberOfPlayers();
       
        itemMap = new HashMap ();
        Iterator it = startPacket.getItems().iterator();
        StartItem item;
        while (it.hasNext()) {
            item = (StartItem) it.next();
            itemMap.put(item.getName(), item);
        }
        
        GameManager.getInstance().setRound(this);
        GameManager.setCurrentPlayerIndex (GameManager.getPriorityPlayerIndex());
        Log.write (getCurrentPlayer().getName() + " has the Priority Deal");
    }
    
    /** 
     * Buy a start item against the base price.
     * @param playerName Name of the buying player.
     * @param itemName Name of the bought start item.
     * @return False in case of any errors.
     */
    public boolean buy (String playerName, String itemName) {
        StartItem item = null;
        String errMsg = null;
        Player player = GameManager.getCurrentPlayer();
        
        while (true) {
            
            // Check player
             if (!playerName.equals(player.getName())) {
                errMsg = "Wrong player";
                break;
            }
            // Check name of item
            if (!itemMap.containsKey(itemName)) {
                errMsg = "Not found";
                break;
            }

            item = (StartItem) itemMap.get(itemName);

            // Is the item buyable?
            if (!isBuyable(item)) {
                errMsg = "Cannot buy this item";
                break;
            }
            
            break;
        }
        
        if (errMsg != null) {
            Log.error ("Invalid buy by "+playerName+" of "+itemName+": " + errMsg);
            return false;
        }
        
        assignItem (player, item, item.getBasePrice());
        
        // Set priority
        GameManager.setPriorityPlayerIndex(GameManager.getCurrentPlayerIndex() + 1);
        numPasses = 0;
        
        // Next action
        setNextAction();        
        return true;
            
    }
    
    /**
     * This method executes the start item buy action.
     * @param player Buying player.
     * @param item Start item being bought.
     * @param price Buy price.
     */
    protected void assignItem (Player player, StartItem item, int price) {
        
        //Log.write ("***"+player.getName()+" buys "+item.getName()+" for "+Bank.format(price));
        Certificate primary = item.getPrimary();
        player.buy(primary, price);
        checksOnBuying (primary);
        if (item.hasSecondary()) {
            Certificate extra = item.getSecondary();
            Log.write (player.getName()+" also gets "+extra.getName());
            player.buy (extra, 0);
            checksOnBuying (extra);
        }
        item.setSold(true);
    }
    
    protected void checksOnBuying (Certificate cert) {
        if (cert instanceof PublicCertificateI) {
        	PublicCertificateI pubCert = (PublicCertificateI) cert;
        	PublicCompanyI comp = pubCert.getCompany();
        	// Start the company, look for a fixed start price
        	if (!comp.hasStarted() && 
        			(!comp.hasStockPrice() || pubCert.isPresidentShare())) {
        		comp.start();
        	}
        	// If there is no start price, we need to get one
        	if (comp.hasStockPrice() && comp.getParPrice() == null
        			&& pubCert.isPresidentShare()) {
        		// We must set the start price!
        		companyNeedingPrice = comp;
        	} 
        	// Check if the company has floated (also applies to minors)
        	comp.checkFlotation ();
        }
    }
    
    public abstract int nextStep();
 
    /*----- Internal functions -----*/
    protected abstract void setNextAction();
    
    protected abstract boolean isBuyable (StartItem item);
    
    protected abstract boolean isBiddable (StartItem item);
    
 }

/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/StartRoundI.java,v 1.2 2005/05/24 21:38:04 evos Exp $
 * 
 * Created on 06-May-2005
 * Change Log:
 */
package game;

/**
 * @author Erik Vos
 */
public interface StartRoundI extends Round {

    public static final int BID_OR_BUY = 0;
    public static final int SET_PRICE = 1;
    
    public void start (StartPacket startPacket);
    
    public int nextStep ();
    
    public StartItem[] getBuyableItems ();
    
    public StartItem[] getBiddableItems ();
    
    public PublicCompanyI getCompanyNeedingPrice ();
    
    /*----- Action methods -----*/
    
    public boolean bid (String playerName, String itemName, int amount);
    
    public boolean bid5 (String playerName, String itemName); 
    
    /** 
     * Buy a start item against the base price.
     * @param playerName Name of the buying player.
     * @param itemName Name of the bought start item.
     * @return False in case of any errors.
     */
    public boolean buy (String playerName, String itemName);

    public boolean pass (String playerName);

    public boolean setPrice (String playerName, String companyName, int parPrice);

}
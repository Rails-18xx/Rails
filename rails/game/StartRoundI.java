package rails.game;

import java.util.List;

public interface StartRoundI extends RoundI
{

	public static final int BID_OR_BUY = 0;
	public static final int SET_PRICE = 1;

	public void start(StartPacket startPacket);

	public int nextStep();

	public StartItem[] getBuyableItems();

	public StartItem[] getBiddableItems();
	
	public List getStartItems ();

	public StartItem getAuctionedItem();

	public PublicCompanyI getCompanyNeedingPrice();

	public StartPacket getStartPacket();

	public int getCurrentPlayerIndex();

	/*----- MoveSet methods -----*/

	public boolean bid(String playerName, String itemName, int amount);

	public boolean bid5(String playerName, String itemName);

	/**
	 * Buy a start item against the base price.
	 * 
	 * @param playerName
	 *            Name of the buying player.
	 * @param itemName
	 *            Name of the bought start item.
	 * @return False in case of any errors.
	 */
	public boolean buy(String playerName, String itemName);

	public boolean pass(String playerName);

	public boolean setPrice(String playerName, String companyName, int parPrice);

	public boolean isBuyable(StartItem item);

	public boolean isBiddable(StartItem item);

	public boolean hasCompanyJustStarted();

	public void resetCompanyJustStarted();

}

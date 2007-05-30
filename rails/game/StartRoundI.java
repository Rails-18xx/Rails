package rails.game;

import java.util.List;

import rails.game.action.BuyOrBidStartItem;
import rails.game.action.PossibleAction;
import rails.game.model.ModelObject;

public interface StartRoundI extends RoundI
{

	public void start(StartPacket startPacket);

	//public int nextStep();

	public List getStartItems ();

	public StartPacket getStartPacket();

	public int getCurrentPlayerIndex();

	public boolean process (PossibleAction action);
	
	public ModelObject getBidModel (int privateIndex, int playerIndex);
	
	public ModelObject getMinimumBidModel (int privateIndex);

	public ModelObject getFreeCashModel (int playerIndex);

	public ModelObject getBlockedCashModel (int playerIndex);
}

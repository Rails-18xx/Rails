/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/StartRoundI.java,v 1.6 2008/12/23 19:59:06 evos Exp $ */
package rails.game;

import java.util.List;

import rails.game.action.PossibleAction;
import rails.game.model.Model;

public interface StartRoundI extends RoundI {

    public void start();

    public List<StartItem> getStartItems();

    public StartPacket getStartPacket();

    public int getCurrentPlayerIndex();

    public boolean process(PossibleAction action);

    public Model<String> getBidModel(int privateIndex, int playerIndex);

    public Model<String> getMinimumBidModel(int privateIndex);

    public Model<String> getFreeCashModel(int playerIndex);

    public Model<String> getBlockedCashModel(int playerIndex);
}

package net.sf.rails.game.specific._1837;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.financial.PublicCertificate;
import rails.game.action.BuyStartItem;

public class StartRound_1837_2ndEd_buying extends StartRound_1837_2ndEd {

    public StartRound_1837_2ndEd_buying(GameManager parent, String id) {
        super(parent, id, StartRound.Bidding.NO, true, true);
        // buying-only, with base prices
    }

    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {
        StartItem item = boughtItem.getStartItem();
        int status = boughtItem.getStatus();
        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();
        int price = 0;

        while (true) {

            // Is the item buyable?
            if (status == StartItem.SELECTABLE && currentStep.value() == SELECT_STEP) {
                price = item.getBasePrice();
            } else {
                errMsg = LocalText.getText("NotForSale");
                break;
            }

            if (player.getFreeCash() < price) {
                errMsg = LocalText.getText("NoMoney");
                break;
            }

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CantBuyItem",
                    playerName,
                    item.getId(),
                    errMsg ));
            return false;
        }


        assignItem(player, item, price, 0);
        if (item.getPrimary() instanceof PublicCertificate) {
            ((PublicCertificate) item.getPrimary()).getCompany().start();
        }
        getRoot().getPlayerManager().setPriorityPlayerToNext();
        setNextSelectingPlayer();
        getRoot().getPlayerManager().setPriorityPlayer(selectingPlayer.value());
        numPasses.set (0);
        currentStep.set(SELECT_STEP);

        return true;
    }

}

package rails.game.correct;


import java.util.List;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.game.Bank;
import rails.game.GameManager;
import rails.game.Player;
import rails.game.PublicCompany;
import rails.game.ReportBuffer;
import rails.game.model.CashOwner;
import rails.game.model.MoneyModel;


public final class CashCorrectionManager extends CorrectionManager {
    
    protected CashCorrectionManager(GameManager gm) {
        super(gm, CorrectionType.CORRECT_CASH);
    }
    
    @Override
    public List<CorrectionAction> createCorrections() {
        List<CorrectionAction> actions = super.createCorrections();
        
        if (isActive()) {
            List<Player> players = gameManager.getPlayers();
            for(Player pl:players){
                actions.add(new CashCorrectionAction(pl));
            }

            List<PublicCompany> publicCompanies = gameManager.getAllPublicCompanies();
            for(PublicCompany pc:publicCompanies){
                if (pc.hasFloated() && !pc.isClosed())
                    actions.add(new CashCorrectionAction(pc));
            }
        }

        return actions;
    }
    
    @Override
    public boolean executeCorrection(CorrectionAction action){
        if (action instanceof CashCorrectionAction)
            return execute((CashCorrectionAction) action);
        else
             return super.executeCorrection(action);
    }

    private boolean execute(CashCorrectionAction cashAction) {

        boolean result = false;

        CashOwner ch = cashAction.getCashHolder();
        int amount = cashAction.getAmount();

        String errMsg = null;

        while (true) {
            if (amount == 0 ) {
                errMsg =
                    LocalText.getText("CorrectCashZero");
                break;
            }
            if ((amount + ch.getCashModel().value()) < 0) {
                errMsg =
                    LocalText.getText("NotEnoughMoney", 
                            ch.getId(),
                            Bank.format(ch.getCashModel().value()),
                            Bank.format(-amount) 
                    );
                break;
            }
            break;   
        }

        if (errMsg != null) {
            DisplayBuffer.change(LocalText.getText("CorrectCashError",
                    ch.getId(),
                    errMsg));
            result = true;
        } else {
            // no error occured 
            // TODO: gameManager.getChangeStack().start(false);

            Bank bank = gameManager.getBank();

            String msg;
            if (amount < 0) {
                // negative amounts: remove cash from cashholder
                MoneyModel.cashMove(ch, bank , -amount);

                msg = LocalText.getText("CorrectCashSubstractMoney",
                        ch.getId(),
                        Bank.format(-amount) );
            } else {
                // positive amounts: add cash to cashholder
                MoneyModel.cashMove(bank, ch, amount);
                msg = LocalText.getText("CorrectCashAddMoney",
                        ch.getId(),
                        Bank.format(amount));
            }
            ReportBuffer.add(msg);
            gameManager.addToNextPlayerMessages(msg, true);
            result = true;
        }
    
       return result;
    }

}

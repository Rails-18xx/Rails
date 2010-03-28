package rails.game.correct;

import rails.game.*;
import rails.game.move.CashMove;
import rails.util.*;
import java.util.*;

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

            List<PublicCompanyI> publicCompanies = gameManager.getAllPublicCompanies();
            for(PublicCompanyI pc:publicCompanies){
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

        CashHolder ch = cashAction.getCashHolder();
        int amount = cashAction.getAmount();

        String errMsg = null;

        while (true) {
            if (amount == 0 ) {
                errMsg =
                    LocalText.getText("CorrectCashZero");
                break;
            }
            if ((amount + ch.getCash()) < 0) {
                errMsg =
                    LocalText.getText("NotEnoughMoney", 
                            ch.getName(),
                            Bank.format(ch.getCash()),
                            Bank.format(-amount) 
                    );
                break;
            }
            break;   
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CorrectCashError",
                    ch.getName(),
                    errMsg));
            result = true;
        } else {
            // no error occured 
            gameManager.getMoveStack().start(false);

            Bank bank = gameManager.getBank();

            String msg;
            if (amount < 0) {
                // negative amounts: remove cash from cashholder
                new CashMove(ch, bank , -amount);

                msg = LocalText.getText("CorrectCashSubstractMoney",
                        ch.getName(),
                        Bank.format(-amount) );
            } else {
                // positive amounts: add cash to cashholder
                new CashMove(bank, ch, amount);
                msg = LocalText.getText("CorrectCashAddMoney",
                        ch.getName(),
                        Bank.format(amount));
            }
            ReportBuffer.add(msg);
            gameManager.addToNextPlayerMessages(msg, true);
            result = true;
        }
    
       return result;
    }

}

package rails.game.correct;

import rails.game.*;
import rails.game.move.CashMove;
import rails.util.*;
import java.util.*;

public final class CashCorrectionManager implements CorrectionManager {

    private GameManager gameManager;

    private CashCorrectionManager() {
    }
    
    public static CorrectionManager getInstance(GameManager gm) {
        CashCorrectionManager manager = new CashCorrectionManager();
        manager.gameManager = gm;
        return manager;
    }
    
    public List<CorrectionAction> createCorrections() {
        List<CorrectionAction> actions = new ArrayList<CorrectionAction>();
        
        List<Player> players = gameManager.getPlayers();
        for(Player pl:players){
            actions.add(new CashCorrectionAction(pl));
        }

        List<PublicCompanyI> publicCompanies = gameManager.getAllPublicCompanies();
        for(PublicCompanyI pc:publicCompanies){
            if (pc.hasFloated())
                actions.add(new CashCorrectionAction(pc));
        }
        
        return actions;
    }

    public boolean executeCorrection(CorrectionAction action) {

        boolean result = false;
        
        if (action instanceof CashCorrectionAction) {
            CashCorrectionAction cashAction=(CashCorrectionAction)action;

            CashHolder ch = cashAction.getCashHolder();
            int amount = cashAction.getAmount();
            
            String errMsg = null;

            if ((amount + ch.getCash()) < 0) {
                errMsg =
                    LocalText.getText("NotEnoughMoney", 
                            ch.getName(),
                            Bank.format(ch.getCash()),
                            Bank.format(-amount) 
                    );
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

                if (amount < 0) {
                    // negative amounts: remove cash from cashholder
                    new CashMove(ch, bank , -amount);
                    ReportBuffer.add(LocalText.getText("CorrectCashSubstractMoney",
                            ch.getName(),
                            Bank.format(-amount) ));
                } else if (amount > 0) {
                    // positive amounts: add cash to cashholder
                    new CashMove(bank, ch, amount);
                    ReportBuffer.add(LocalText.getText("CorrectCashAddMoney",
                            ch.getName(),
                            Bank.format(amount) ));
                }
                result = true;
            }
        }
    
       return result;
    }

}

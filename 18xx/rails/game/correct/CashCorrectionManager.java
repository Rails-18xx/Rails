package rails.game.correct;

import rails.game.*;
import rails.game.move.CashMove;
import rails.game.move.StateChange;
import rails.game.state.BooleanState;
import rails.util.*;
import java.util.*;

public final class CashCorrectionManager implements CorrectionManager {

    private static CashCorrectionManager ccm;
    
    private GameManager gameManager;
    
    private BooleanState active;
    
    private CashCorrectionManager() {
    }
    
    public static CorrectionManager getInstance(GameManager gm) {
        if (ccm == null) {
            ccm = new CashCorrectionManager();
            ccm.gameManager = gm;
            ccm.active = new BooleanState("CASH_CORRECT", false);
        }
        return ccm;
    }
    
    public boolean isActive(){
        return active.booleanValue();
    }
    
    
    public List<CorrectionAction> createCorrections() {
        List<CorrectionAction> actions = new ArrayList<CorrectionAction>();
        
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
        actions.add(new CorrectionModeAction(CorrectionType.CORRECT_CASH, isActive()));

        return actions;
    }

    public boolean executeCorrection(CorrectionAction action) {

        boolean result = false;
        
        if (action instanceof CorrectionModeAction) {
            gameManager.getMoveStack().start(false);
            if (!isActive()) {
                String text = LocalText.getText("CorrectionModeActivate",
                        gameManager.getCurrentPlayer().getName(),
                        LocalText.getText("CORRECT_CASH")
                );
                ReportBuffer.add(text);
                DisplayBuffer.add(text);
            }
            else {
                ReportBuffer.add(LocalText.getText("CorrectionModeDeactivate",
                        gameManager.getCurrentPlayer().getName(),
                        LocalText.getText("CORRECT_CASH"))
                );
            }
            new StateChange(active, !isActive());
            
            result = true;
        } else if (action instanceof CashCorrectionAction) {
            CashCorrectionAction cashAction=(CashCorrectionAction)action;

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

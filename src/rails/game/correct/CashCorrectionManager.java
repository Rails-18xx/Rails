package rails.game.correct;

import java.util.List;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.ReportBuffer;
import rails.game.GameManager;
import rails.game.Currency;
import rails.game.MoneyOwner;
import rails.game.Player;
import rails.game.PublicCompany;
import rails.game.state.ChangeStack;

public class CashCorrectionManager extends CorrectionManager {
    
    private CashCorrectionManager(GameManager parent) {
        super(parent, CorrectionType.CORRECT_CASH);
    }
    
    public static CashCorrectionManager create(GameManager parent) {
        return new CashCorrectionManager(parent);
    }
    
    @Override
    public List<CorrectionAction> createCorrections() {
        List<CorrectionAction> actions = super.createCorrections();
        
        if (isActive()) {
            List<Player> players = getRoot().getPlayerManager().getPlayers();
            for(Player pl:players){
                actions.add(new CashCorrectionAction(pl));
            }

            List<PublicCompany> publicCompanies = getParent().getAllPublicCompanies();
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

        MoneyOwner ch = cashAction.getCashHolder();
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
                            ch.getId(),
                            Currency.format(this, ch.getCash()),
                            Currency.format(this, -amount) 
                    );
                break;
            }
            break;   
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CorrectCashError",
                    ch.getId(),
                    errMsg));
            result = true;
        } else {
            // no error occured 
            
            ChangeStack.start(this, cashAction);

            String msg;
            if (amount < 0) {
                // negative amounts: remove cash from cashholder
                String text = Currency.toBank(ch, -amount);

                msg = LocalText.getText("CorrectCashSubstractMoney",
                        ch.getId(),
                        text );
            } else {
                // positive amounts: add cash to cashholder
                String text = Currency.fromBank(amount, ch);
                msg = LocalText.getText("CorrectCashAddMoney",
                        ch.getId(),
                        text);
            }
            ReportBuffer.add(this, msg);
            getParent().addToNextPlayerMessages(msg, true);
            result = true;
        }
    
       return result;
    }

}

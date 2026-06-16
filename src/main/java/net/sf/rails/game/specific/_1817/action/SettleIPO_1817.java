package net.sf.rails.game.specific._1817.action;

import java.util.List;
import net.sf.rails.game.RailsRoot;
import rails.game.action.PossibleAction;

public class SettleIPO_1817 extends PossibleAction {

    private static final long serialVersionUID = 1L;
    
    private final String publicCompanyId;
    private List<String> privateCompanyIds;
    private int cashAmount;
    private int shareSize; 

    public SettleIPO_1817(RailsRoot root, String publicCompanyId, List<String> privateCompanyIds, int cashAmount, int shareSize) {
        super(root);
        this.publicCompanyId = publicCompanyId;
        this.privateCompanyIds = privateCompanyIds;
        this.cashAmount = cashAmount;
        this.shareSize = shareSize;
    }

    public String getPublicCompanyId() { return publicCompanyId; }
    
    public List<String> getPrivateCompanyIds() { return privateCompanyIds; }
    public void setPrivateCompanyIds(List<String> privateCompanyIds) { this.privateCompanyIds = privateCompanyIds; }
    
    public int getCashAmount() { return cashAmount; }
    public void setCashAmount(int cashAmount) { this.cashAmount = cashAmount; }
    
    public int getShareSize() { return shareSize; }
    public void setShareSize(int shareSize) { this.shareSize = shareSize; }






    @Override
    public String toString() {
        return "Settle " + shareSize + "-share IPO for " + publicCompanyId + " (Cash: $" + cashAmount + ")";
    }
}
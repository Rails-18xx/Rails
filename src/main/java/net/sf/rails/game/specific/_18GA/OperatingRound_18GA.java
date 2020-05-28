package net.sf.rails.game.specific._18GA;

import rails.game.action.BuyPrivate;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;

public class OperatingRound_18GA extends OperatingRound {

    public final static String OS_NAME = "OSO";
    public final static String OS_EXTRA_TRAIN_TYPE = "2";
    
    /**
     * Constructed via Configure
     */
    public OperatingRound_18GA (GameManager parent, String id) {
        super(parent, id);
    }
    
    public boolean buyPrivate(BuyPrivate action) {
        
        boolean result = super.buyPrivate(action);
        
        // If the Ocilla Southern has been bought, the company gets an extra 2-train, if possible
        if (result 
                && action.getPrivateCompany().getId().equalsIgnoreCase(OS_NAME)
                && isBelowTrainLimit()) {
            PublicCompany company = operatingCompany.value();
            TrainCardType certType = trainManager.getCardTypeByName(OS_EXTRA_TRAIN_TYPE);
            if (!certType.hasRusted()) {  // I.e. before phase "4"
                TrainCard card = trainManager.cloneTrain(certType);
                company.getPortfolioModel().addTrainCard(card);
                card.setTradeable(false);
                ReportBuffer.add(this, LocalText.getText("GetsExtraTrain",
                        company.getId(),
                        OS_EXTRA_TRAIN_TYPE));
                // TODO: do we need a replacement for this?
                // company.getPortfolioModel().getTrainsModel().update();
            }
        }
        
        return result;
    }

}

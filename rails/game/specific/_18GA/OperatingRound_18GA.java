package rails.game.specific._18GA;

import rails.common.LocalText;
import rails.game.*;
import rails.game.action.BuyPrivate;

public class OperatingRound_18GA extends OperatingRound {

    public final static String OS_NAME = "OSO";
    public final static String OS_EXTRA_TRAIN_TYPE = "2";
    
    public OperatingRound_18GA (GameManagerI gameManager) {
        super (gameManager);
    }
    
    public boolean buyPrivate(BuyPrivate action) {
        
        boolean result = super.buyPrivate(action);
        
        // If the Ocilla Southern has been bought, the company gets an extra 2-train, if possible
        if (result 
                && action.getPrivateCompany().getName().equalsIgnoreCase(OS_NAME)
                && isBelowTrainLimit()) {
            PublicCompanyI company = operatingCompany.get();
            TrainCertificateType certType = trainManager.getCertTypeByName(OS_EXTRA_TRAIN_TYPE);
            if (!certType.hasRusted()) {  // I.e. before phase "4"
                TrainI train = trainManager.cloneTrain(certType);
                company.getPortfolio().buyTrain(train, 0);
                train.setTradeable(false);
                ReportBuffer.add(LocalText.getText("GetsExtraTrain",
                        company.getName(),
                        OS_EXTRA_TRAIN_TYPE));
                company.getPortfolio().getTrainsModel().update();
            }
        }
        
        return result;
    }

}

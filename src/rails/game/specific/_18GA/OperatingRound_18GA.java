package rails.game.specific._18GA;

import rails.common.LocalText;
import rails.game.*;
import rails.game.action.BuyPrivate;

public final class OperatingRound_18GA extends OperatingRound {

    public final static String OS_NAME = "OSO";
    public final static String OS_EXTRA_TRAIN_TYPE = "2";
    
    private OperatingRound_18GA (GameManager parent, String id) {
        super(parent, id);
    }
    
    public static OperatingRound_18GA create(GameManager parent, String id) {
        return new OperatingRound_18GA(parent, id);
    }
    
    public boolean buyPrivate(BuyPrivate action) {
        
        boolean result = super.buyPrivate(action);
        
        // If the Ocilla Southern has been bought, the company gets an extra 2-train, if possible
        if (result 
                && action.getPrivateCompany().getId().equalsIgnoreCase(OS_NAME)
                && isBelowTrainLimit()) {
            PublicCompany company = operatingCompany.get();
            TrainCertificateType certType = trainManager.getCertTypeByName(OS_EXTRA_TRAIN_TYPE);
            if (!certType.hasRusted()) {  // I.e. before phase "4"
                Train train = trainManager.cloneTrain(certType);
                company.getPortfolioModel().addTrain(train);
                train.setTradeable(false);
                ReportBuffer.add(LocalText.getText("GetsExtraTrain",
                        company.getId(),
                        OS_EXTRA_TRAIN_TYPE));
                // TODO: is this still required?
                company.getPortfolioModel().getTrainsModel().update();
            }
        }
        
        return result;
    }

}

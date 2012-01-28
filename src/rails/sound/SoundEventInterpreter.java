package rails.sound;

import rails.game.*;
import rails.game.action.*;
import rails.game.model.PresidentModel;
import rails.game.state.*;

/**
 * Converts processed actions and model updates to triggers for playing sounds.
 *  
 * @author Frederick Weld
 *
 */

// FIXME: The observer approach has been changed to be compatible with Rails 2.0
// However it is untested so far, and relays on the issue of toText() methods
public class SoundEventInterpreter {

    private class PresidentModelObserver implements Observer {
        private final PresidentModel model;
        private String formerPresident = null;
        
        private PresidentModelObserver(PublicCompany pc) {
            model = pc.getPresidentModel();
        }
        public void update(String text) {
            if (formerPresident != text) {
                formerPresident = text;
                if (SoundConfig.isSFXEnabled()) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_SR_NewPresident);
                }
            }
        }

        public Observable getObservable() {
            return model;
        }
    }

    private class FloatedModelObserver implements Observer {
        private final BooleanState model;
        private Boolean hasFloated = false;
        
        private FloatedModelObserver(PublicCompany pc) {
            model = pc.getFloatedModel();
            hasFloated = pc.getFloatedModel().value();
        }

        public void update(String text) {
            if (model.value() != hasFloated) {
                hasFloated = model.value();
                if (SoundConfig.isSFXEnabled()) {
                    player.playSFXByConfigKey (
                            SoundConfig.KEY_SFX_SR_CompanyFloats);
                }
            }
        }

        public Observable getObservable() {
            return model;
        }
    }

    private SoundContext context;
    private SoundPlayer player;
    
    
    public SoundEventInterpreter (SoundContext context,SoundPlayer player) {
        this.context = context;
        this.player = player;
    }
    public void notifyOfActionProcessing(GameManager gm,PossibleAction action) {
        
        /**
         * Interpretation of events for which are only sfx is relevant 
         */
        
        if (SoundConfig.isSFXEnabled()) {
            
            //OR actions
            
            if (action instanceof LayTile) {
                player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_LayTile);
                
            } else if (action instanceof LayToken) {
                player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_LayToken);
                
            } else if (action instanceof SetDividend) {
                //set revenue not treated here
                SetDividend sd = (SetDividend)action;
                if (sd.getRevenueAllocation() == SetDividend.PAYOUT) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_Decision_Payout);
                } else if (sd.getRevenueAllocation() == SetDividend.SPLIT) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_Decision_Split);
                } else if (sd.getRevenueAllocation() == SetDividend.WITHHOLD) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_Decision_Withhold);
                }  
                
            } else if (action instanceof BuyTrain) {
                String trainName = ((BuyTrain)action).getType().getName();
                player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_BuyTrain, trainName);
                
            } else if (action instanceof BuyPrivate) {
                player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_BuyPrivate);
                
            } 
            
            //SR actions
            
            else if (action instanceof BuyCertificate) {
                BuyCertificate bc = (BuyCertificate)action;
                String presidentName = "";
                if (bc.getCompany().getPresident() != null) {
                    presidentName = bc.getCompany().getPresident().getId();
                }
                if (presidentName.equals(bc.getPlayerName())) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_SR_BuyShare_President);
                } else {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_SR_BuyShare_NonPresident);
                }

            } else if (action instanceof SellShares) {
                SellShares ss = (SellShares)action;
                String presidentName = "";
                if (ss.getCompany().getPresident() != null) {
                    presidentName = ss.getCompany().getPresident().getId();
                }
                if (presidentName.equals(ss.getPlayerName())) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_SR_SellShare_President);
                } else {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_SR_SellShare_NonPresident);
                }

            }

            //Start Round actions
            
            else if (action instanceof rails.game.action.BidStartItem) {
                player.playSFXByConfigKey (SoundConfig.KEY_SFX_STR_BidStartItem);

            } else if (action instanceof rails.game.action.BuyStartItem) {
                player.playSFXByConfigKey (SoundConfig.KEY_SFX_STR_BuyStartItem);
            
            }
            
        }
    }
    public void notifyOfGameInit(final GameManager gameManager) {
        //subscribe to round changes
        if (gameManager.getCurrentRoundModel() != null) {
            gameManager.getCurrentRoundModel().addObserver(
                    new Observer() {
                       public void update(String text) {
                                context.notifyOfRound(gameManager.getCurrentRound());
                        }
                       public Observable getObservable() {
                           return gameManager.getCurrentRoundModel();
                       }
                    });
        }

        //subscribe to phase changes
        if (gameManager.getPhaseManager() != null) {
            gameManager.getPhaseManager().getCurrentPhaseModel().addObserver(
                    new Observer() {
                        public void update(String text) {
                                 context.notifyOfPhase(gameManager.getCurrentPhase());
                         }
                        public Observable getObservable() {
                            return gameManager.getPhaseManager().getCurrentPhaseModel();
                        }
                    });
        }

        //subscribe to company events
        if (gameManager.getCompanyManager() != null) {
            for (PublicCompany c : gameManager.getCompanyManager().getAllPublicCompanies() ) {
                //presidency changes
                c.getPresidentModel().addObserver(new PresidentModelObserver(c));
                
                //company floats
                c.getFloatedModel().addObserver(new FloatedModelObserver(c));
            }
        }
    }
    public void notifyOfGameSetup() {
        if (SoundConfig.isBGMEnabled()) player.playBGMByConfigKey(SoundConfig.KEY_BGM_GameSetup);
    }
}

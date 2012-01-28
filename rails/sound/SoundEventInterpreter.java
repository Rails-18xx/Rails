package rails.sound;

import java.util.Observable;
import java.util.Observer;

import rails.game.*;
import rails.game.action.*;
import rails.game.state.*;

/**
 * Converts processed actions and model updates to triggers for playing sounds.
 * 
 * Model observers get their own inner classes since their constructors are parameterized
 * (needed to initialize member variables among others - especially important if game is
 * loaded).
 *  
 * @author Frederick Weld
 *
 */
public class SoundEventInterpreter {

    private class PresidentModelObserver implements Observer {
        private PublicCompanyI pc;
        private Player formerPresident = null;
        public PresidentModelObserver(PublicCompanyI pc) {
            this.pc = pc;
            if (pc != null) formerPresident = pc.getPresident();
        }
        public void update(Observable o, Object arg) {
            if (formerPresident != pc.getPresident()) {
                formerPresident = pc.getPresident();
                if (SoundConfig.isSFXEnabled()) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_SR_NewPresident);
                }
            }
        }
    }

    private class CompanyFloatsModelObserver implements Observer {
        private PublicCompanyI pc;
        Boolean hasFloated = false;
        public CompanyFloatsModelObserver(PublicCompanyI pc) {
            this.pc = pc;
            if (pc != null) hasFloated = pc.hasFloated();
        }
        public void update(Observable o, Object arg) {
            if (hasFloated != pc.hasFloated()) {
                hasFloated = pc.hasFloated();
                if (SoundConfig.isSFXEnabled()) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_SR_CompanyFloats);
                }
            }
        }
    }

    private SoundContext context;
    private SoundPlayer player;
    
    
    public SoundEventInterpreter (SoundContext context,SoundPlayer player) {
        this.context = context;
        this.player = player;
    }
    public void notifyOfActionProcessing(GameManagerI gm,PossibleAction action) {
        
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
                    presidentName = bc.getCompany().getPresident().getName();
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
                    presidentName = ss.getCompany().getPresident().getName();
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
    public void notifyOfGameInit(GameManagerI gameManager) {
        //subscribe to round changes
        if (gameManager.getCurrentRoundModel() != null) {
            gameManager.getCurrentRoundModel().addObserver(
                    new Observer() {
                        public void update(Observable o, Object arg) {
                            if (o instanceof State) {
                                State s = (State)o;
                                context.notifyOfRound((RoundI)s.get());
                            }
                        }
                    });
        }

        //subscribe to phase changes
        if (gameManager.getPhaseManager() != null) {
            gameManager.getPhaseManager().getCurrentPhaseModel().addObserver(
                    new Observer() {
                        public void update(Observable o, Object arg) {
                            if (o instanceof State) {
                                State s = (State)o;
                                context.notifyOfPhase((PhaseI)s.get());
                            }
                        }
                    });
        }

        //subscribe to company events
        if (gameManager.getCompanyManager() != null) {
            for (PublicCompanyI c : gameManager.getCompanyManager().getAllPublicCompanies() ) {
                //presidency changes
                c.getPresidentModel().addObserver(new PresidentModelObserver(c));
                //company floats
                c.getFloatedModel().addObserver(new CompanyFloatsModelObserver(c));
            }
        }
    }
    public void notifyOfTimeWarp(boolean timeWarpMode) {
        SoundConfig.setSFXDisabled(timeWarpMode);
    }
}

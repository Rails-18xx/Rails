package net.sf.rails.sound;

import java.util.EnumSet;

import rails.game.action.*;
import net.sf.rails.game.*;
import net.sf.rails.game.model.PresidentModel;
import net.sf.rails.game.state.*;
import net.sf.rails.ui.swing.ORUIManager;
import net.sf.rails.ui.swing.ORUIManager.LocalSteps;
import net.sf.rails.ui.swing.hexmap.HexUpgrade;
import net.sf.rails.ui.swing.hexmap.TileHexUpgrade;

/**
 * Converts processed actions and model updates to triggers for playing sounds.
 * 
 * Some model observers get their own inner classes since their constructors are parameterized
 * (needed to initialize member variables among others - especially important if game is
 * loaded since game status will not be initial upon initialization of the sound framework).
 *  
 * @author Frederick Weld
 *
 */

// FIXME: The observer approach has been changed to be compatible with Rails 2.0
// However it is untested so far, and relays on the issue of toText() methods
public class SoundEventInterpreter {

    private class CurrentPlayerModelObserver implements Observer {
        private Player formerCurrentPlayer = null;
        private PlayerManager pm;
        public CurrentPlayerModelObserver(PlayerManager pm) {
            this.pm = pm;
            if (pm != null) formerCurrentPlayer = pm.getCurrentPlayer();
        }
        public void update(String text) {
            if (formerCurrentPlayer != pm.getCurrentPlayer()) {
                formerCurrentPlayer = pm.getCurrentPlayer();
                if (SoundConfig.isSFXEnabled()) {
                    player.playSFXByConfigKey (
                            SoundConfig.KEY_SFX_GEN_NewCurrentPlayer,
                            pm.getCurrentPlayer().getId());
                }
            }
        }
        public Observable getObservable() {
            return pm.getCurrentPlayerModel();
        }
    }

    private class PresidentModelObserver implements Observer {
        private final PresidentModel model;
        private String formerPresident = null;
        
        private PresidentModelObserver(PublicCompany pc) {
            model = pc.getPresidentModel();
            formerPresident = model.toText();
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

    private class CompanyFloatedObserver implements Observer {
        private final BooleanState model;
        private Boolean hasFloated = false;
        
        private CompanyFloatedObserver(PublicCompany pc) {
            model = pc.getFloatedModel();
            hasFloated = pc.getFloatedModel().value();
        }

        public void update(String text) {
            if (model.value() != hasFloated) {
                hasFloated = model.value();
                if (SoundConfig.isSFXEnabled()) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_SR_CompanyFloats);
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
    public void notifyOfActionProcessing(RailsRoot root,PossibleAction action) {
        
        /**
         * Interpretation of events for which are only sfx is relevant 
         */
        
        if (SoundConfig.isSFXEnabled()) {
            
            //General actions
            
            if (action instanceof NullAction) {
                if (EnumSet.of(NullAction.Mode.PASS, NullAction.Mode.AUTOPASS).contains(
                        ((NullAction)action).getMode() )) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_GEN_Pass);
                }
                
            }
            
            //OR actions
            
            else if (action instanceof LayTile) {
                LayTile lt = (LayTile)action;
                if (lt.getLaidTile().getNumStations() == 0) {
                    //track upgrade
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_LayTile_Track);
                } else {
                    //city upgrade
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_LayTile_City);
                }
                if (!lt.getLaidTile().isUnlimited() && lt.getLaidTile().getFreeCount() == 1) {
                    //last available tile is about to be laid
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_LayTile_LastTileLaid);
                }
                
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
    public void notifyOfGameInit(final RailsRoot root) {
        final PlayerManager pm = root.getPlayerManager();
        //subscribe to current player changes
        if (pm.getCurrentPlayerModel() != null) {
            pm.getCurrentPlayerModel().addObserver(
                    new CurrentPlayerModelObserver(pm));
        }

        //subscribe to round changes
        final GameManager gameManager = root.getGameManager();
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

        //subscribe to changes to game over pending
        final BooleanState gameOverModel = gameManager.getGameOverPendingModel(); 
        if (gameOverModel != null) {
            gameOverModel.addObserver(
                    new Observer() {
                        private boolean gameOverPending = false;
                        public void update(String text) {
                            if (!gameOverPending && gameOverModel.value()) {
                                if (SoundConfig.isSFXEnabled()) {
                                    player.playSFXByConfigKey (
                                            SoundConfig.KEY_SFX_GEN_GameOverPending);
                                }
                            }
                            gameOverPending = gameOverModel.value();
                        }
                        public Observable getObservable() {
                            return gameOverModel;
                        }
                    });
        }

        //subscribe to phase changes
        if (root.getPhaseManager() != null) {
            root.getPhaseManager().getCurrentPhaseModel().addObserver(
                    new Observer() {
                        public void update(String text) {
                                 context.notifyOfPhase(gameManager.getCurrentPhase());
                         }
                        public Observable getObservable() {
                            return root.getPhaseManager().getCurrentPhaseModel();
                        }
                    });
        }

        //subscribe to company events
        if (root.getCompanyManager() != null) {
            for (PublicCompany c : root.getCompanyManager().getAllPublicCompanies() ) {
                //presidency changes
                c.getPresidentModel().addObserver(new PresidentModelObserver(c));
                //company floats
                c.getFloatedModel().addObserver(new CompanyFloatedObserver(c));
            }
        }
    }
    public void notifyOfTimeWarp(boolean timeWarpMode) {
        SoundConfig.setSFXDisabled(timeWarpMode);
    }
    /**
     * Interprets changes/status of OR local steps in order to trigger sfx that
     * are related to neither model changes nor game engine actions.
     * Is triggered whenever some step changes (but priorStep is allowed to be
     * equal to currentStep)
     * @param currentStep Step as defined as constant in ORUIManager
     */
    public void notifyOfORLocalStep(ORUIManager.LocalSteps currentStep) {
        if (SoundConfig.isSFXEnabled()) {
            //play rotate sound if tile has been rotated or is now ready for rotations
//            if (currentStep == LocalSteps.ConfirmUpgrade) {
//                player.playSFXByConfigKey(SoundConfig.KEY_SFX_OR_RotateTile);
//            }
            
            //play hex selection sound if the follow-up step (select tile/token) is active
            //(don't consider whether prior step was "select hex..." because hexes
            // can also be selected during selectTile/Token)
            if ( currentStep == LocalSteps.SelectUpgrade) {
                player.playSFXByConfigKey(SoundConfig.KEY_SFX_GEN_Select);
            }
        }
    }
    /**
     * Interprets selections of ClickFields
     * @param clickFieldAction The action associated with the click field
     */
    public void notifyOfClickFieldSelection(PossibleAction clickFieldAction) {
        if (SoundConfig.isSFXEnabled()) {
            if (clickFieldAction instanceof BidStartItem
                    || clickFieldAction instanceof BuyStartItem
                    || clickFieldAction instanceof BuyCertificate
                    || clickFieldAction instanceof SellShares) {
                player.playSFXByConfigKey(SoundConfig.KEY_SFX_GEN_Select);
            }
        }
    }
    
    public void notifyOfSelectUpgrade(HexUpgrade upgrade) {
        if (SoundConfig.isSFXEnabled()) {
            if (upgrade instanceof TileHexUpgrade) {
                player.playSFXByConfigKey(SoundConfig.KEY_SFX_OR_RotateTile);
            }
        }
    }
    
}

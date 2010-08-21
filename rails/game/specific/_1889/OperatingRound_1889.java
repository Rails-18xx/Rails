/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_1889/OperatingRound_1889.java,v 1.1 2010/02/23 22:21:40 stefanfrey Exp $ */
package rails.game.specific._1889;

import rails.game.*;
import rails.game.action.*;
import rails.game.special.*;
import rails.game.state.*;
import rails.util.*;

/**
 * Adds specific code for 1889 to allow the special timing of the special tile laying private companies
 */
public class OperatingRound_1889 extends OperatingRound {

    private PrivateCompanyI privB;
    private BooleanState activeSpPrivB;
    
    private PrivateCompanyI privC;
    private BooleanState activeSpPrivC;
    private GameDef.OrStep storeActiveStep;
    private String previousOwnerName; 
    
    private boolean beginnerGame;
    
    /**
     * Instantiates a new operating round_1889.
     * 
     * @param gameManager the game manager
     */
    public OperatingRound_1889 (GameManagerI gameManager) {
        super (gameManager);
        
        privB = companyManager.getPrivateCompany("B");
        activeSpPrivB = new BooleanState("ActiveSpPrivB", false);
        
        privC = companyManager.getPrivateCompany("C");
        activeSpPrivC = new BooleanState("ActiveSpPrivC", false);
    
        beginnerGame = GameOption.convertValueToBoolean(getGameOption("BeginnerGame"));
    }
    
    @Override
    protected void setGameSpecificPossibleActions() {
        
        // noMapMode and beginnerGame are not effected
        if (noMapMode || beginnerGame) return;
        
        // private B: lay track at other company tile laying steps
        if (getStep() == GameDef.OrStep.LAY_TRACK) {
            if (!privB.isClosed() && 
                    privB.getPortfolio().getOwner() instanceof Player && 
                    privB.getPortfolio().getOwner() != operatingCompany.get().getPresident()) {
                SpecialPropertyI spPrivB = privB.getSpecialProperties().get(0);
                if (spPrivB != null && !spPrivB.isExercised()) {
                    if (!activeSpPrivB.booleanValue()) 
                        possibleActions.add(new UseSpecialProperty(spPrivB));
                    else {
                        possibleActions.add(new LayTile((SpecialTileLay)spPrivB));
                        DisplayBuffer.add(LocalText.getText("1889PrivateBactive", privB.getPortfolio().getOwner()));
                    }
                }
            }
        } else {
            activeSpPrivB.set(false);
        }
        
        // private C: trigger by purchase of private -- see below
        if (activeSpPrivC.booleanValue()) {
            possibleActions.clear();
            SpecialTileLay spPrivC = (SpecialTileLay)privC.getSpecialProperties().get(0);
            possibleActions.add(new LayTile(spPrivC));
            possibleActions.add(new NullAction(NullAction.SKIP));
            DisplayBuffer.add(LocalText.getText("1889PrivateCactive", previousOwnerName));
        }
        
    }
    
    @Override
    public boolean processGameSpecificAction(PossibleAction action) {
        
        // private B
        if (action instanceof UseSpecialProperty) {
            UseSpecialProperty spAction=(UseSpecialProperty)action;
            if (spAction.getSpecialProperty() == privB.getSpecialProperties().get(0)) {
                moveStack.start(true);
                activeSpPrivB.set(true);
                log.debug("1889 specific: Allows tile lay for B with player request");
                return true;
            }
        }
        return false;
    }
    
  
    @Override
    public boolean buyPrivate(BuyPrivate action){

       // store the seller name, playername in action is the owner of the buying company!
       String sellerName = action.getPrivateCompany().getPortfolio().getOwner().getName();
        
       boolean result = super.buyPrivate(action);
       
       if (!(noMapMode || beginnerGame) && result && (action.getPrivateCompany() == privC)) {
           // moveStack identical to buy private action
            activeSpPrivC.set(true);
            previousOwnerName = sellerName;
            log.debug("1889 specific: Activates tile laying step for C after purchase of C");
            storeActiveStep = getStep();
            stepObject.set(GameDef.OrStep.LAY_TRACK);
        }
       return result;
    }
    
    @Override
    public boolean layTile(LayTile action) {
        
        boolean result = super.layTile(action);

        if (result && activeSpPrivC.booleanValue()) {
            // moveStack identical to tile lay
            log.debug("1889 specific: Tile lay for C executed, return to previous step");
            activeSpPrivC.set(false);
            stepObject.set(storeActiveStep);
        }
        return(result);
    }
    
    @Override
    public void skip() {
        if (activeSpPrivC.booleanValue()) {
            log.debug("1889 specific: Tile lay for C skipped, return to previous step");
            moveStack.start(true);
            activeSpPrivC.set(false);
            stepObject.set(storeActiveStep);
        } else {
            super.skip();
        }
    }
    
}




package rails.game.specific._1889;

import com.google.common.collect.Iterables;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.parser.GameOption;
import rails.game.GameDef;
import rails.game.GameManager;
import rails.game.OperatingRound;
import rails.game.Player;
import rails.game.PrivateCompany;
import rails.game.action.BuyPrivate;
import rails.game.action.LayTile;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.UseSpecialProperty;
import rails.game.special.SpecialProperty;
import rails.game.special.SpecialTileLay;
import rails.game.state.BooleanState;

/**
 * Adds specific code for 1889 to allow the special timing of the special tile laying private companies
 */
public class OperatingRound_1889 extends OperatingRound {

    private final PrivateCompany privB;
    private final BooleanState activeSpPrivB = BooleanState.create(this, "ActiveSpPrivB");
    
    private final PrivateCompany privC;
    private final BooleanState activeSpPrivC = BooleanState.create(this, "ActiveSpPrivC");
    
    private final boolean beginnerGame;

    private GameDef.OrStep storeActiveStep;
    private String previousOwnerName; 
    
    /**
     * Constructed via Configure
     */
    public OperatingRound_1889(GameManager parent, String id) {
        super(parent, id);
        privB = companyManager.getPrivateCompany("B");
        privC = companyManager.getPrivateCompany("C");
        beginnerGame = GameOption.convertValueToBoolean(getGameOption("BeginnerGame"));
    }

    @Override
    protected void setGameSpecificPossibleActions() {
        
        // noMapMode and beginnerGame are not effected
        if (noMapMode || beginnerGame) return;
        
        // private B: lay track at other company tile laying steps
        if (getStep() == GameDef.OrStep.LAY_TRACK) {
            if (!privB.isClosed() && 
                    privB.getOwner() instanceof Player && 
                    privB.getOwner() != operatingCompany.value().getPresident()) {
                SpecialProperty spPrivB = Iterables.get(privB.getSpecialProperties(), 0);
                if (spPrivB != null && !spPrivB.isExercised()) {
                    if (!activeSpPrivB.value()) 
                        possibleActions.add(new UseSpecialProperty(spPrivB));
                    else {
                        possibleActions.add(new LayTile((SpecialTileLay)spPrivB));
                        DisplayBuffer.add(LocalText.getText("1889PrivateBactive", privB.getOwner()));
                    }
                }
            }
        } else {
            activeSpPrivB.set(false);
        }
        
        // private C: trigger by purchase of private -- see below
        if (activeSpPrivC.value()) {
            possibleActions.clear();
            SpecialTileLay spPrivC = (SpecialTileLay)Iterables.get(privC.getSpecialProperties(),0);
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
            if (spAction.getSpecialProperty() == Iterables.get(privB.getSpecialProperties(), 0)) {
                // TODO: changeStack.start(true);
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
       String sellerName = action.getPrivateCompany().getOwner().getId();
        
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

        if (result && activeSpPrivC.value()) {
            // moveStack identical to tile lay
            log.debug("1889 specific: Tile lay for C executed, return to previous step");
            activeSpPrivC.set(false);
            stepObject.set(storeActiveStep);
        }
        return(result);
    }
    
    @Override
    public void skip() {
        if (activeSpPrivC.value()) {
            log.debug("1889 specific: Tile lay for C skipped, return to previous step");
            // TODO: changeStack.start(true);
            activeSpPrivC.set(false);
            stepObject.set(storeActiveStep);
        } else {
            super.skip();
        }
    }
    
}

package net.sf.rails.game.specific._1889;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.action.BuyPrivate;
import net.sf.rails.game.action.LayTile;
import net.sf.rails.game.action.NullAction;
import net.sf.rails.game.action.PossibleAction;
import net.sf.rails.game.action.UseSpecialProperty;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.special.SpecialTileLay;
import net.sf.rails.game.state.BooleanState;

import com.google.common.collect.Iterables;


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
        beginnerGame = GameOption.getAsBoolean(this, "BeginnerGame");
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
                LayTile layTile = new LayTile((SpecialTileLay)spPrivB);
                if (spPrivB != null && !spPrivB.isExercised()
                        && validateSpecialTileLay(layTile)) {
                    if (!activeSpPrivB.value())
                        possibleActions.add(new UseSpecialProperty(spPrivB));
                    else {
                        possibleActions.clear();
                        possibleActions.add(layTile);
                        possibleActions.add(new NullAction(NullAction.SKIP));
                        DisplayBuffer.add(this, LocalText.getText("1889PrivateBactive", privB.getOwner()));

                    }
                }
            }
        } else {
            activeSpPrivB.set(false);
        }

        // private C: trigger by purchase of private -- see below
        if (activeSpPrivC.value()) {
            SpecialTileLay spPrivC = (SpecialTileLay)Iterables.get(privC.getSpecialProperties(),0);
            LayTile layTile = new LayTile(spPrivC);
            if (validateSpecialTileLay(layTile)) {
                possibleActions.clear();
                possibleActions.add(layTile);
                possibleActions.add(new NullAction(NullAction.SKIP));
                DisplayBuffer.add(this, LocalText.getText("1889PrivateCactive", previousOwnerName));
            }
        }

    }

    @Override
    public boolean processGameSpecificAction(PossibleAction action) {

        // private B
        if (action instanceof UseSpecialProperty) {
            UseSpecialProperty spAction=(UseSpecialProperty)action;
            if (spAction.getSpecialProperty() == Iterables.get(privB.getSpecialProperties(), 0)) {
                
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
    public void skip(NullAction action) {
        if (activeSpPrivC.value()) {
            log.debug("1889 specific: Tile lay for C skipped, return to previous step");
            // TODO: This is a GameAction which should not be used here
            
            activeSpPrivC.set(false);
            stepObject.set(storeActiveStep);
        } else {
            super.skip(action);
        }
    }

}

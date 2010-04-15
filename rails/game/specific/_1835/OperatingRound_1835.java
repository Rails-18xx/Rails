package rails.game.specific._1835;

import rails.game.Bank;
import rails.game.DisplayBuffer;
import rails.game.GameDef;
import rails.game.GameManagerI;
import rails.game.OperatingRound;
import rails.game.PhaseI;
import rails.game.action.DiscardTrain;
import rails.game.action.LayBaseToken;
import rails.game.action.LayTile;
import rails.game.special.SpecialTileLay;
import rails.game.state.BooleanState;
import rails.util.LocalText;

public class OperatingRound_1835 extends OperatingRound {
    
    private BooleanState needPrussianFormationCall 
            = new BooleanState ("NeedPrussianFormationCall", false);
    private BooleanState hasLaidExtraOBBTile
            = new BooleanState ("HasLaidExtraOBBTile", false);

    public OperatingRound_1835 (GameManagerI gameManager) {
        super (gameManager);
    }

    protected void setSpecialTileLays() {

        /* Special-property tile lays */
        currentSpecialTileLays.clear();

        if (!operatingCompany.canUseSpecialProperties()) return;

        for (SpecialTileLay stl : getSpecialProperties(SpecialTileLay.class)) {
            if (stl.isExtra() || !currentNormalTileLays.isEmpty()) {
                
                // Exclude the second OBB free tile if the first was laid in this round
                if (stl.getLocationNameString().matches("M1(7|9)")
                        && hasLaidExtraOBBTile.booleanValue()) continue;
                
                currentSpecialTileLays.add(new LayTile(stl));
            }
        }
    }

    public boolean layTile(LayTile action) {

        // The extra OBB tiles may not both be laid in the same round
        if (action.getSpecialProperty() != null
                && action.getSpecialProperty().getLocationNameString().matches("M1(5|7)")) {
            if (hasLaidExtraOBBTile.booleanValue()) {
                String errMsg = LocalText.getText("InvalidTileLay");
                DisplayBuffer.add(LocalText.getText("CannotLayTileOn",
                        action.getCompanyName(),
                        action.getLaidTile().getExternalId(),
                        action.getChosenHex().getName(),
                        Bank.format(0),
                        errMsg ));
                return false;
            }
        }
        
        boolean result = super.layTile(action);
        
        if (result && action.getSpecialProperty() != null
                && action.getSpecialProperty().getLocationNameString().matches("M1(5|7)")) {
            hasLaidExtraOBBTile.set(true);
        }
        
        return result;
    }

    /*
    public boolean layBaseToken(LayBaseToken action) {

        // No tokens may be laid on the BA home hex before BA has done so  
        if (action.getChosenHex().getName().equalsIgnoreCase("L6")
                && !action.getCompanyName().equalsIgnoreCase(GameManager_1835.BA_ID)
                && !gameManager.getCompanyManager().getCompanyByName(GameManager_1835.BA_ID)
                        .hasLaidHomeBaseTokens()) {
            String errMsg = LocalText.getText("NotYetOperated", GameManager_1835.BA_ID);
            DisplayBuffer.add(LocalText.getText("CannotLayBaseTokenOn",
                    action.getCompanyName(),
                    action.getChosenHex().getName(),
                    Bank.format(0),
                    errMsg ));
            return false;
           
        } else {
            return super.layBaseToken(action);
        }
    }
     */
    
    protected void newPhaseChecks() {
        PhaseI phase = getCurrentPhase();
        if (phase.getName().equals("4") || phase.getName().equals("4+4")
                || phase.getName().equals("5")) {
            if (!PrussianFormationRound.prussianIsComplete(gameManager)) {
                if (getStep() == GameDef.OrStep.DISCARD_TRAINS) {
                    // Postpone until trains are discarded
                    needPrussianFormationCall.set(true);
                } else {
                    // Do it immediately
                    ((GameManager_1835)gameManager).startPrussianFormationRound (this);
                }
            }
        }
    }
    
    public boolean discardTrain(DiscardTrain action) {
        
        boolean result = super.discardTrain(action);
        if (result && getStep() == GameDef.OrStep.BUY_TRAIN 
                && needPrussianFormationCall.booleanValue()) {
            // Do the postponed formation calls 
            ((GameManager_1835)gameManager).startPrussianFormationRound (this);
            needPrussianFormationCall.set(false);
        }
        return result;
    }
}

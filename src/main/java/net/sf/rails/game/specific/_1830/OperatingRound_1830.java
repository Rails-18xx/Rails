package net.sf.rails.game.specific._1830;

import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Company;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.special.SpecialBaseTokenLay;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.special.SpecialTileLay;

// Corrected Action Imports
import rails.game.action.LayTile;
import rails.game.action.PossibleAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.game.StartRound;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.GameOption;

import rails.game.action.PossibleAction;
import rails.game.action.BuyStartItem;
import rails.game.action.StartItemAction;
import rails.game.action.NullAction;

import java.util.Collection;
import java.util.List;

/**
 * Operating Round specifically for 1830.
 * Handles the "Use it or Lose it" rule for the Delaware & Hudson private company.
 */
public class OperatingRound_1830 extends OperatingRound {
    
    private static final Logger log = LoggerFactory.getLogger(OperatingRound_1830.class);
    
    // State to track if the D&H special tile was laid in the current operating turn
    private boolean dhTileLaidThisTurn = false;
    private static final String DH_HEX_NAME = "F-16"; 
    private static final String DH_COMPANY_NAME = "Delaware & Hudson";

    public OperatingRound_1830(GameManager gameManager, String roundId) {
        super(gameManager, roundId);
    }

    @Override
    protected void initTurn() {
        super.initTurn();
        // Reset the flag at the start of every company's turn
        dhTileLaidThisTurn = false;
    }

    @Override
    public boolean process(PossibleAction action) {
        // Intercept the processing to detect if the D&H tile is being laid
        if (action instanceof LayTile) {
            LayTile layTile = (LayTile) action;
            SpecialProperty sp = layTile.getSpecialProperty();

            if (sp != null 
                && sp instanceof SpecialTileLay 
                && sp.getOriginalCompany() != null) {
                
                Company originalComp = sp.getOriginalCompany();
                
                // Use getId() because getName() is not guaranteed on the Company interface
                String compName = originalComp.getId(); 
                
                if (compName != null && compName.contains(DH_COMPANY_NAME)) {
                    // LayTile uses getChosenHex(), not getHex()
if (layTile.getChosenHex() != null && layTile.getChosenHex().getId().equals(DH_HEX_NAME)) {                        log.info("1830: D&H Special Tile laid on " + DH_HEX_NAME + ". Token must be placed this turn or ability is lost.");
log.info("1830: D&H Special Tile laid on " + DH_HEX_NAME + ". Token must be placed this turn or ability is lost.");
                        dhTileLaidThisTurn = true;

                    }
                }
            }
        }

        // Proceed with normal processing
        return super.process(action);
    }

    @Override
    protected void finishTurn() {
        // This method is called when the player clicks "Done" or the turn is forced to end.
        
        if (dhTileLaidThisTurn) {
            // Use getOperatingCompany() instead of getCurrentCompany()
            Company currentComp = getOperatingCompany();
            
            // Check if the token was actually placed. 
            boolean tokenPlaced = hasTokenOnHex(currentComp, DH_HEX_NAME);

            if (!tokenPlaced) {
                log.info("1830: D&H Tile laid but token skipped. Expiring remaining special token ability.");
                expireDhTokenAbility(currentComp);
            }
        }

        super.finishTurn();
    }

    /**
     * Helper to check if the current company has a token on the specified hex name.
     */
    private boolean hasTokenOnHex(Company company, String hexName) {
        if (company == null) return false;
        // gameManager is protected in the superclass, so we can access it directly
MapHex hex = gameManager.getRoot().getMapManager().getHex(hexName);
        if (hex == null) return false;
        
        // Use hasTokenOfCompany, not hasStation
        return hex.hasTokenOfCompany((PublicCompany) company);
      }

    /**
     * Finds the SpecialBaseTokenLay property for D&H and marks it as exercised.
     */
    private void expireDhTokenAbility(Company company) {
Collection<SpecialProperty> specials = company.getSpecialProperties();

        if (specials == null) return;

        for (SpecialProperty sp : specials) {
            if (sp instanceof SpecialBaseTokenLay 
                && sp.getOriginalCompany() != null) {
                
                String compName = sp.getOriginalCompany().getId();
                if (compName != null && compName.contains(DH_COMPANY_NAME)) {
                    // Mark the property as exercised (used)
                    sp.setExercised();
                }
            }
        }
    }
}
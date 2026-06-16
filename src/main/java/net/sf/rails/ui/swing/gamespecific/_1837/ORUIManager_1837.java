package net.sf.rails.ui.swing.gamespecific._1837;

import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.ui.swing.elements.RadioButtonDialog;
import net.sf.rails.ui.swing.hexmap.TileHexUpgrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.sound.SoundManager;
import net.sf.rails.ui.swing.ORPanel;
import net.sf.rails.ui.swing.ORUIManager;
import rails.game.action.LayTile;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
import rails.game.action.SetDividend;
import rails.game.specific._1837.SetHomeHexLocation;

import java.util.Set;

/**
 * @author Martin
 *
 */
public class ORUIManager_1837 extends ORUIManager {

    public static final String COMPANY_START_HEX_DIALOG = "CompanyStartHex";
    private static final String[] hexes = {"L2", "L8"};

    private static final Logger log = LoggerFactory.getLogger(ORUIManager_1837.class);

    /**
     *
     */
    public ORUIManager_1837() {
        super();
    }

    /* (non-Javadoc)
     * @see net.sf.rails.ui.swing.ORUIManager#setDividend(java.lang.String, rails.game.action.SetDividend)
     */
    protected void setDividend(String command, SetDividend action) {

        boolean hasDirectCompanyIncomeInOR
                = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.HAS_SPECIAL_COMPANY_INCOME);

        int amount, treasuryAmount, dividend;

        if (command.equals(ORPanel.SET_REVENUE_CMD)) {
            amount = orPanel.getRevenue(orCompIndex);
            treasuryAmount = orPanel.getCompanyTreasuryBonusRevenue(orCompIndex);
            if (hasDirectCompanyIncomeInOR) {
                dividend = amount - treasuryAmount;
                orPanel.setDividend(orCompIndex, dividend);
            }
            orPanel.stopRevenueUpdate();
            log.debug("Set revenue amount is {}", amount);
            log.debug("The Bonus for the company treasury is {}", treasuryAmount);
            action.setActualRevenue(amount);
            action.setActualCompanyTreasuryRevenue(treasuryAmount);

            // notify sound manager of set revenue amount as soon as
            // set revenue is pressed (not waiting for the completion
            // of the set dividend action)
            SoundManager.notifyOfSetRevenue(amount);

            if (amount == 0 || action.getRevenueAllocation() != SetDividend.UNKNOWN) {
                log.debug("Allocation is known: {}", action.getRevenueAllocation());
                orWindow.process(action);
            } else {
                log.debug("Allocation is unknown, asking for it");
                setLocalStep(LocalSteps.SELECT_PAYOUT);
                updateStatus(action, true);

                // Locally update revenue if we don't inform the server yet.
                orPanel.setRevenue(orCompIndex, amount);
                orPanel.setTreasuryBonusRevenue(orCompIndex, treasuryAmount);
            }
        } else {
            // The revenue allocation has been selected
            orWindow.process(action);
        }
    }


    @Override
    public void updateStatus(boolean myTurn) {

        // 1. Run standard parent logic (updates map, panels, etc.)
        super.updateStatus(myTurn);

        // 2. FAILSAFE: Manually check for S5 Home Hex action.
        // This runs even if super.updateStatus() decides to skip the hook.
        if (myTurn) {
            try {
                // Access the current round safely
                RoundFacade round = gameUIManager.getGameManager().getCurrentRound();
                if (round instanceof OperatingRound) {
                    PublicCompany company = ((OperatingRound) round).getOperatingCompany();
                    
                    if (company != null && "S5".equalsIgnoreCase(company.getId())) {
                        
                        PossibleActions actions = gameUIManager.getGameManager().getPossibleActions();
                        if (actions != null && actions.contains(SetHomeHexLocation.class)) {
                            
                            // Check if we are already displaying this dialog to avoid duplicates
                            PossibleAction currentAction = getCurrentDialogAction();
                            if (currentAction == null || !(currentAction instanceof SetHomeHexLocation)) {
                                SetHomeHexLocation action = actions.getType(SetHomeHexLocation.class).get(0);
                                requestHomeHex(action);
                            } else {
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    
    /**
     * Additional TileLay validation:
     * Prevent laying a tile on a blocked hex for
     * a non-owner of the blocking private
     * @param upgrades Upgrades validated so far
     * @param layTile LayTyle action
     */
    protected void gameSpecificTileUpgradeValidation (Set<TileHexUpgrade> upgrades,
                                                      LayTile layTile,
                                                      Phase currentPhase) {
        if (!currentPhase.getId().equals("2")) return;

        if (layTile.getType() == LayTile.GENERIC) {
            for (TileHexUpgrade upgrade : upgrades) {
                MapHex hex = upgrade.getHex().getHex();
                PrivateCompany blockingPrivate = hex.getBlockingPrivateCompany();
                if (upgrade.hexIsBlocked()
                        && blockingPrivate != null
                        // The below check may break the client/server separation.
                        && !layTile.getPlayer().equals(blockingPrivate.getOwner())) {
                    upgrade.setVisible(false);
                    // Note: the private owner gets a separate LayTile instance,
                    // type = SPECIAL_PROPERTY, allowing to lay that tile anyway.
                }
            }
        }
    }

    protected void checkForGameSpecificActions(PublicCompany orComp,
                                               GameDef.OrStep orStep,
                                               PossibleActions possibleActions) {

                                                // DEBUG LOGGING
        if (orComp.getId().equalsIgnoreCase("S5")) {
             if (possibleActions != null && possibleActions.contains(SetHomeHexLocation.class)) {
             }
        }

        if (orComp.getId().equalsIgnoreCase("S5")
              && possibleActions.contains(SetHomeHexLocation.class)) {
            
            SetHomeHexLocation action = possibleActions.getType(SetHomeHexLocation.class).get(0);
            requestHomeHex(action);

        }

    }
    private boolean requestHomeHex(SetHomeHexLocation action) {

        RadioButtonDialog dialog = new RadioButtonDialog(
                COMPANY_START_HEX_DIALOG, this, orWindow,
                LocalText.getText("PleaseSelect"),
                LocalText.getText("StartingHomeHexS5", action.getPlayerName(), action.getCompanyName()),
                hexes, 0);
        
        setCurrentDialog (dialog, action);
        return true;

    }

    @Override
    public void dialogActionPerformed() {

  
        if (getCurrentDialogAction() instanceof SetHomeHexLocation) {
            handleStartHex();
        } else {
            super.dialogActionPerformed();
        }
    }

private void handleStartHex() {
        RadioButtonDialog dialog = (RadioButtonDialog) getCurrentDialog();
        SetHomeHexLocation action =
                (SetHomeHexLocation) getCurrentDialogAction();

        if (dialog == null) {
            return;
        }

        int index = dialog.getSelectedOption();

        if (index >= 0) {
            String hexName = hexes[index];
            
            action.setHomeHex(hexName);

            // COMPILATION FIX: Access MapManager via getRoot()
            // We use this only for logging verification now.
            net.sf.rails.game.MapHex mapHex = gameUIManager.getRoot().getMapManager().getHex(hexName);
            
            if (mapHex != null) {
                // action.setSelectedHomeHex(mapHex); // REMOVED: Method does not exist
            } else {
            }

            gameUIManager.processAction(action);
        } else {
        }
    }

}

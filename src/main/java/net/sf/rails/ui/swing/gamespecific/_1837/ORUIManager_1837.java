/**
 *
 */
package net.sf.rails.ui.swing.gamespecific._1837;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.sound.SoundManager;
import net.sf.rails.ui.swing.ORPanel;
import net.sf.rails.ui.swing.ORUIManager;
import net.sf.rails.ui.swing.gamespecific._1880.StartRoundWindow_1880;
import rails.game.action.SetDividend;

/**
 * @author Martin
 *
 */
public class ORUIManager_1837 extends ORUIManager {
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
        int amount, bonusAmount;

        if (command.equals(ORPanel.SET_REVENUE_CMD)) {
            amount = orPanel.getRevenue(orCompIndex);
            bonusAmount = orPanel.getCompanyTreasuryBonusRevenue(orCompIndex);
            orPanel.stopRevenueUpdate();
            log.debug("Set revenue amount is {}", amount);
            log.debug("The Bonus for the company treasury is {}", bonusAmount);
            action.setActualRevenue(amount);
            action.setActualCompanyTreasuryRevenue(bonusAmount);

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
                orPanel.setTreasuryBonusRevenue(orCompIndex, bonusAmount);
            }
        } else {
            // The revenue allocation has been selected
            orWindow.process(action);
        }
    }

}

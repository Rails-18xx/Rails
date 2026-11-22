package rails.game.correct;

import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;

public class TimeCorrectionManager extends CorrectionManager {

    private TimeCorrectionManager(GameManager parent) {
        super(parent, CorrectionType.CORRECT_TIME);
    }

    public static TimeCorrectionManager create(GameManager parent) {
        return new TimeCorrectionManager(parent);
    }

    @Override
    public boolean executeCorrection(CorrectionAction action) {
        
        // 1. Intercept the Menu Click
        if (action instanceof CorrectionModeAction) {
            if (!isActive()) {
                // [FIX] Do NOT run the Wizard if we are reloading a saved game.
                if (!getParent().isReloading()) {
                    runWizard();
                }
                return true; 
            }
            return super.executeCorrection(action);
        }

        // 2. Execute the actual Time Change
        if (action instanceof TimeCorrectionAction) {
            return execute((TimeCorrectionAction) action);
        }

        return super.executeCorrection(action);
    }

    private void runWizard() {
        // Step 1: Select Player
        List<Player> players = getRoot().getPlayerManager().getPlayers();
        List<String> names = players.stream().map(Player::getName).collect(Collectors.toList());

        String selectedName = (String) JOptionPane.showInputDialog(
            null,
            "Select Player to correct time for:",
            "Time Correction",
            JOptionPane.QUESTION_MESSAGE,
            null,
            names.toArray(),
            names.get(0)
        );

        if (selectedName == null) return; 

        Player p = getRoot().getPlayerManager().getPlayerByName(selectedName);
        if (p == null) return;

        // Step 2: Enter Time
        String input = JOptionPane.showInputDialog(
            null, 
            "Adjust time for " + p.getName() + " in Seconds:\n(Use negative to remove time, e.g. -60)",
            "0"
        );

        if (input == null) return; 

        try {
            int seconds = Integer.parseInt(input.trim());
            if (seconds == 0) return; 

            // Step 3: Create and Process the Action
            TimeCorrectionAction tca = new TimeCorrectionAction(getRoot(), p, seconds);
            getParent().process(tca);

        } catch (NumberFormatException e) {
            DisplayBuffer.add(this, "Invalid number format.");
        }
    }

    private boolean execute(TimeCorrectionAction action) {
        Player p = getRoot().getPlayerManager().getPlayerByName(action.getTargetPlayerName());
        
        if (p == null) {
            DisplayBuffer.add(this, "Error: Target player not found for time correction.");
            return false;
        }

        // Apply Change
        p.getTimeBankModel().add(action.getSeconds());

        // Report
        String msg = String.format("Corrected Time for %s: %s%d seconds", 
                p.getName(), 
                (action.getSeconds() > 0 ? "+" : ""), 
                action.getSeconds());
        
        ReportBuffer.add(this, msg);
        DisplayBuffer.add(this, msg);
        
        // [FIX] Add null check for GameUIManager
        if (getParent().getGameUIManager() != null) {
            getParent().getGameUIManager().forceFullUIRefresh();
        }

        return true;
    }
}
package net.sf.rails.ui.swing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.PossibleActions;
import javax.swing.JPanel;
import java.lang.reflect.Field;

public class UIUndoStressTest {
    private static final Logger log = LoggerFactory.getLogger(UIUndoStressTest.class);

    public static void runTest(GameUIManager gameUIManager, ORUIManager orUIManager) {
        log.info("--- STARTING UNDO STRESS TEST ---");
        
        try {
            // 1. Simulate an empty action list (transient state during undo) via Reflection
            PossibleActions emptyActions = PossibleActions.create();
            Field paField = gameUIManager.getGameManager().getClass().getDeclaredField("possibleActions");
            paField.setAccessible(true);
            paField.set(gameUIManager.getGameManager(), emptyActions);
            
            // 2. Trigger the event-driven update directly
            orUIManager.updateStatus(null, true);
            
            // 3. Verify the panel did not wipe out existing buttons via Reflection
            Field spField = ORPanel.class.getDeclaredField("specialPanel");
            spField.setAccessible(true);
            JPanel specialPanel = (JPanel) spField.get(orUIManager.getORPanel());
            
            int componentCount = specialPanel != null ? specialPanel.getComponentCount() : 0;
            if (componentCount > 0) {
                log.info("SUCCESS: Stability Guard held. Panel retained {} components during empty state.", componentCount);
            } else {
                log.error("FAIL: Stability Guard broken. Panel wiped components.");
            }
        } catch (Exception e) {
            log.error("Test execution failed due to reflection error:", e);
        }
    }
}
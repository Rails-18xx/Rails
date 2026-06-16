package net.sf.rails.ui.swing;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Component;
import java.awt.event.KeyEvent;
import javax.swing.text.JTextComponent;
import javax.swing.AbstractButton;

import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.financial.StockRound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalHotkeyManager implements KeyEventDispatcher {

    private final GameUIManager gameUIManager;
    private static final Logger log = LoggerFactory.getLogger(GlobalHotkeyManager.class);

    // State tracker to prevent "Machine Gun" auto-repeat (Looping)
    private boolean isLKeyPressed = false;
    private boolean isEnterKeyPressed = false;
    private boolean isAKeyPressed = false;

    public GlobalHotkeyManager(GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {

        // 1. Handle Key Release to reset the "Single Shot" flag
        if (e.getID() == KeyEvent.KEY_RELEASED) {
            if (e.getKeyCode() == KeyEvent.VK_L) {
                isLKeyPressed = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                isEnterKeyPressed = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_A) {
                isAKeyPressed = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                return true; 
            }
            return false; // Always let release events propagate
        }

        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return false;
        }
        // --- SAFETY CHECK: DIALOGS ---
        java.awt.Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (activeWindow instanceof javax.swing.JDialog) {
            return false;
        }

        // --- SAFETY CHECK: TYPING ---
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner instanceof JTextComponent && ((JTextComponent) focusOwner).isEditable()) {
            if (!e.isControlDown() && !e.isMetaDown()) {
                return false;
            }
        }

        boolean isCtrlDown = e.isControlDown() || e.isMetaDown();
        int keyCode = e.getKeyCode();

        // 1. UNDO (Ctrl + Z)
        if (isCtrlDown && keyCode == KeyEvent.VK_Z) {
            triggerUndo();
            return true;
        }

        // 2. REDO (Ctrl + Y or Ctrl + Shift + Z)
        // Standard Redo is often Shift+Ctrl+Z, but we keep Ctrl+Y support too
        if (isCtrlDown && (keyCode == KeyEvent.VK_Y || (e.isShiftDown() && keyCode == KeyEvent.VK_Z))) {
            triggerRedo();
            return true;
        }

        // 3. TOGGLE TIME (T)
        if (!isCtrlDown && !e.isAltDown() && !e.isShiftDown() && keyCode == KeyEvent.VK_T) {
            triggerPause();
            return true;
        }

        // 4. AI MOVE (A)
        if (!isCtrlDown && !e.isAltDown() && keyCode == KeyEvent.VK_A) {
            if (!isAKeyPressed) {
                isAKeyPressed = true;
                triggerAI();
            }
            return true;
        }

        // 5. ESCAPE ('No' / 'Skip' / 'Pass')
        if (keyCode == KeyEvent.VK_ESCAPE) {
            triggerEscape();
            return true;
        }

        // 6. ENTER ('Smart Confirm' / 'Discard Train')
        if (!isCtrlDown && !e.isAltDown() && !e.isShiftDown() && keyCode == KeyEvent.VK_ENTER) {
            if (!isEnterKeyPressed) {
                isEnterKeyPressed = true; // Lock until release
                // We are in the main window. Intercept and route to smart handler so 'Done' is
                // default.
                triggerEnter();
            }
            return true;
        }

        // 7. L ('Buy IPO Train') - SINGLE SHOT MODE
        // We only trigger if the key was previously released.
        // This strictly blocks OS key-repeat events from firing multiple buys.
        if (!isCtrlDown && !e.isAltDown() && keyCode == KeyEvent.VK_L) {
            if (!isLKeyPressed) {
                isLKeyPressed = true; // Lock until release
                triggerBuyIPOTrain();
            }
            return true; // Consume the event even if locked
        }
        if (keyCode == KeyEvent.VK_SPACE) {
            if (gameUIManager.getORUIManager() != null) {
                gameUIManager.getORUIManager().toggleMapMarkings();
            }
            return true; 
        }

        return false;
    }

    private void triggerBuyIPOTrain() {
        // Only valid during Operating Rounds
        if (gameUIManager.getCurrentRound() instanceof OperatingRound) {
            ORWindow win = gameUIManager.orWindow;
            if (win != null && win.isVisible()) {
                ORPanel panel = win.getORPanel();
                if (panel != null) {
                    // Delegate to the existing logic in ORPanel which finds the correct 'Buy IPO'
                    // action
                    // log.info("delegate to processIPOBuy...");
                    panel.processIPOBuy();
                }
            }
        }
    }

    private void triggerPause() {
        if (gameUIManager.getStatusWindow() != null
                && gameUIManager.getStatusWindow().pauseButton != null
                && gameUIManager.getStatusWindow().pauseButton.isEnabled()) {
            gameUIManager.getStatusWindow().pauseButton.doClick();
        }
    }

    private void triggerAI() {
        gameUIManager.performAIMove();
    }

    private void triggerEnter() {
        RoundFacade round = gameUIManager.getCurrentRound();

        // 1. Operating Round: Delegate to ORPanel handler (Scoring algorithm for
        // Discards)
        if (round instanceof OperatingRound) {
            ORWindow orWindow = gameUIManager.orWindow;
            if (orWindow != null && orWindow.isVisible() && orWindow.getORPanel() != null) {
                orWindow.getORPanel().handleEnterPress();
                return;
            }
        }

        // 2. Fallback: Status Window Pass/Done Button
        if (gameUIManager.getStatusWindow() != null &&
                gameUIManager.getStatusWindow().passButton != null &&
                gameUIManager.getStatusWindow().passButton.isEnabled()) {
            gameUIManager.getStatusWindow().passButton.doClick();
        }
    }

    private void triggerEscape() {
        RoundFacade round = gameUIManager.getCurrentRound();

        // --- CASE 1: OPERATING ROUND (Skip) ---
        if (round instanceof OperatingRound) {
            ORWindow orWindow = gameUIManager.orWindow;
            if (orWindow != null && orWindow.isVisible()) {
                ORPanel panel = orWindow.getORPanel();
                if (panel != null) {

                    // Delegate completely to ORPanel's centralized handler logic.
                    // This ensures the "Payout/Split/Hold" priority checks in ORPanel are
                    // respected.
                    panel.handleEnterPress();
                    return;

                }
            }
        }

        // --- CASE 2: AUCTION / START ROUND (Pass) ---
        if (round instanceof StartRound) {
            if (gameUIManager.getStatusWindow() != null) {
                if (clickIfEnabled(gameUIManager.getStatusWindow().passButton))
                    return;
            }
        }

        // --- CASE 3: STOCK ROUND (Pass) ---
        if (round instanceof StockRound) {
            if (gameUIManager.getStatusWindow() != null && gameUIManager.getStatusWindow().isVisible()) {
                if (clickIfEnabled(gameUIManager.getStatusWindow().passButton))
                    return;
            }
        }
    }

    private boolean clickIfEnabled(AbstractButton btn) {
        if (btn != null && btn.isEnabled() && btn.isVisible()) {
            btn.doClick();
            return true;
        }
        return false;
    }

    private void triggerUndo() {
        // 1. Priority: Operating Round Local Undo
        if (gameUIManager.getCurrentRound() instanceof OperatingRound) {
            ORWindow win = gameUIManager.orWindow; // Direct field access
            if (win != null && win.isVisible() && win.getORPanel() != null) {
                // Try executing on panel first. If successful, we are done.
                if (win.getORPanel().executeUndo()) {
                    return;
                }
            }
        }

        // 2. Fallback: Status Window (Global Undo)
        if (gameUIManager.getStatusWindow() != null
                && gameUIManager.getStatusWindow().undoItem != null
                && gameUIManager.getStatusWindow().undoItem.isEnabled()) {
            gameUIManager.getStatusWindow().undoItem.doClick();
        }
    }

    private void triggerRedo() {
        // 1. Priority: Operating Round Local Redo
        if (gameUIManager.getCurrentRound() instanceof OperatingRound) {
            ORWindow win = gameUIManager.orWindow; // Direct field access
            if (win != null && win.isVisible() && win.getORPanel() != null) {
                if (win.getORPanel().executeRedo()) {
                    return;
                }
            }
        }

        // 2. Fallback: Status Window
        StatusWindow sw = gameUIManager.getStatusWindow();
        if (sw != null) {
            if (sw.redoItem != null && sw.redoItem.isEnabled()) {
                sw.redoItem.doClick();
            }
        }
    }

}
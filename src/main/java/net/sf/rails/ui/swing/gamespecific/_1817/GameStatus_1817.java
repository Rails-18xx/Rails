package net.sf.rails.ui.swing.gamespecific._1817;

import javax.swing.JOptionPane;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.ui.swing.GameStatus;
import rails.game.action.PossibleAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameStatus_1817 extends GameStatus {
    private static final Logger log = LoggerFactory.getLogger(GameStatus_1817.class);



    @Override
    protected PossibleAction handle1817IPO(final PossibleAction action) {
        try {
            final net.sf.rails.ui.swing.hexmap.HexMap map = gameUIManager.getORUIManager().getMap();
            if (map == null) return null;

            // 1. Identify valid home hexes
            final java.util.List<net.sf.rails.ui.swing.hexmap.GUIHex> validGuiHexes = new java.util.ArrayList<>();
            for (net.sf.rails.ui.swing.hexmap.GUIHex guiHex : map.getGuiHexList()) {
                net.sf.rails.game.MapHex hex = guiHex.getHex();
                if (hex != null && hex.getStops() != null) {
                    for (net.sf.rails.game.Stop stop : hex.getStops()) {
                        if (stop.hasTokenSlotsLeft()) {
                            validGuiHexes.add(guiHex);
                            break;
                        }
                    }
                }
            }

            if (validGuiHexes.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this, "No valid starting hexes available.");
                return null;
            }

            // 2. Highlight valid hexes in purple
            for (net.sf.rails.ui.swing.hexmap.GUIHex h : validGuiHexes) {
                h.setActiveOwnerHighlight(true, "IPO", true);
            }
            map.repaintAll(new java.awt.Rectangle(map.getSize()));

            log.info("waiting for user to click on hex");
            
            // 3. Attach one-off MouseListener
            map.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;

                    net.sf.rails.ui.swing.hexmap.GUIHex clickedHex = null;
                    for (net.sf.rails.ui.swing.hexmap.GUIHex h : map.getGuiHexList()) {
                        if (h.contains(e.getPoint())) {
                            clickedHex = h;
                            break;
                        }
                    }

                    if (clickedHex != null && validGuiHexes.contains(clickedHex)) {
                        map.removeMouseListener(this);

                        // Clear highlights
                        for (net.sf.rails.ui.swing.hexmap.GUIHex h : validGuiHexes) {
                            h.setActiveOwnerHighlight(false, null, false);
                        }
                        map.repaintAll(new java.awt.Rectangle(map.getSize()));

                        // 4. Proceed to Bid Dialog
                        promptForBidAndExecute(action, clickedHex.getHex().getId());
                    }
                }
            });

            // 5. Suspend engine execution until GUI interaction completes
            return null;

        } catch (Exception e) {
            log.error("Failed to setup 1817 IPO map listener", e);
            return null;
        }
    }

    private void promptForBidAndExecute(PossibleAction action, String hexId) {
        String bidStr = javax.swing.JOptionPane.showInputDialog(this,
                "Enter starting bid ($100 - $400, multiples of $5)\nfor location " + hexId + ":",
                "1817 IPO: Initial Bid",
                javax.swing.JOptionPane.QUESTION_MESSAGE);

        if (bidStr == null || bidStr.trim().isEmpty()) return;

        try {
            int bid = Integer.parseInt(bidStr.replaceAll("[^0-9]", ""));

            if (bid < 100 || bid > 400 || bid % 5 != 0) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Invalid bid. Must be between $100 and $400 and a multiple of $5.",
                        "Validation Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }

            action.getClass().getMethod("setHexId", String.class).invoke(action, hexId);
            action.getClass().getMethod("setBid", int.class).invoke(action, bid);

            gameUIManager.processAction(action);

        } catch (Exception ex) {
            log.error("Error setting bid and executing", ex);
            javax.swing.JOptionPane.showMessageDialog(this, "Error processing bid.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected String formatShareText(int percentage, PublicCompany c, boolean isPresident) {
        if (percentage == 0) return "";
        
        int unit = c.getShareUnit();
        if (unit <= 0 || unit > 100) {
            return percentage + "%" + (isPresident ? "P" : "");
        }
        
        int held = percentage / unit;
        int total = 100 / unit;
        
        String text = held + "/" + total;
        if (isPresident) text += "P";
        return text;
    }


}
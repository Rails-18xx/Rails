package net.sf.rails.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import rails.game.action.PossibleAction;
import rails.game.action.GameAction;
import rails.game.correct.CorrectionModeAction;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.StartRound;

/**
 * A supplementary informational display presenting interactive help summaries
 * along with a live snapshot of valid engine actions mapped to clean, flowing prose.
 */
public class HelpTextWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTextPane helpTextPane;

    public HelpTextWindow(String buildTimestamp, GameUIManager gui) {
        super("Game Reference & Action Assistance");
        setSize(550, 500);
        setLocationRelativeTo(gui.getStatusWindow());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        helpTextPane = new JTextPane();
        helpTextPane.setContentType("text/html");
        helpTextPane.setEditable(false);
        helpTextPane.setBackground(new Color(245, 245, 250));

        add(new JScrollPane(helpTextPane), BorderLayout.CENTER);
        
        // Initial draw
        refreshHelp(gui.getGameManager(), buildTimestamp);
    }

    /**
     * Re-hydrates the HTML panel with active round markers and descriptive step assistance.
     */
    public void refreshHelp(GameManager gm, String buildTimestamp) {
        if (gm == null || gm.getPossibleActions() == null) {
            helpTextPane.setText("<html><body style='font-family:sans-serif; padding:12px;'><p style='color:red;'>Engine data currently unavailable.</p></body></html>");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:sans-serif; padding:10px; margin:0;'>");
        
        var round = gm.getCurrentRound();
        Player activePlayer = gm.getCurrentPlayer();
        String playerName = (activePlayer != null) ? activePlayer.getName() : "Unknown Player";
        PublicCompany company = null;
        
        String bgHex = "#eef2f7"; 
        String fgHex = "#000000"; 
        String phaseDetails = "";

        if (round instanceof OperatingRound) {
            company = ((OperatingRound) round).getOperatingCompany();
            if (company != null) {
                try {
                    Color bgColor = company.getBgColour();
                    Color fgColor = company.getFgColour();
                    if (bgColor != null) bgHex = String.format("#%02x%02x%02x", bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue());
                    if (fgColor != null) fgHex = String.format("#%02x%02x%02x", fgColor.getRed(), fgColor.getGreen(), fgColor.getBlue());
                } catch (Exception e) {}
            }
            String stepDesc = ((OperatingRound) round).getStep() != null ? ((OperatingRound) round).getStep().toString().replace("_", " ").toLowerCase() : "operations";
            phaseDetails = "Operating Round " + gm.getORId() + " - " + stepDesc;
        } else if (round instanceof StockRound) {
            phaseDetails = "Stock Round " + gm.getSRNumber();
        } else if (round instanceof StartRound) {
            phaseDetails = "Start Round / Initial Auction";
        }

        // --- BOLD HEADER ---
        sb.append("<div style='background-color:").append(bgHex).append("; color:").append(fgHex)
          .append("; padding:15px; text-align:center; font-size:24px; font-weight:bold; border-radius:6px; margin-bottom:15px; border:1px solid #ccc;'>");
        if (company != null) {
            sb.append(company.getId()).append(" <span style='font-size:16px; font-weight:normal;'>(").append(playerName).append(")</span><br>");
        } else {
            sb.append(playerName).append("<br>");
        }
        sb.append("<span style='font-size:14px; font-weight:normal;'>").append(phaseDetails).append("</span>");
        sb.append("</div>");

        // --- ACTION PARSING & GROUPING ---
        List<PossibleAction> actions = gm.getPossibleActions().getList();
        
        if (actions != null && !actions.isEmpty()) {
            sb.append("<table style='width:100%; border-collapse:collapse; font-size:13px;'>");
            sb.append("<tr style='background-color:#eaeaea; border-bottom:2px solid #bdbdbd; text-align:left;'>");
            sb.append("<th style='padding:8px; width:35%;'>Available Action</th>");
            sb.append("<th style='padding:8px;'>Details</th>");
            sb.append("</tr>");

            PossibleAction passAction = null;
            String floatPrices = null;
            Map<String, List<Integer>> sellMap = new HashMap<>();
            List<String> buyList = new ArrayList<>();
            List<PossibleAction> standardActions = new ArrayList<>();

            for (PossibleAction pa : actions) {
                if (pa instanceof GameAction || pa instanceof CorrectionModeAction || pa.isCorrection() || pa.getClass().getName().endsWith("Short1817")) {
                    continue;
                }

                String aName = pa.getClass().getSimpleName();
                if (aName.equals("NullAction")) {
                    passAction = pa; // Save to ensure it is always last
                } else if (aName.equals("StartCompany")) {
                    if (floatPrices == null) floatPrices = extractValue(pa.toString(), "startPrices=");
                } else if (aName.equals("SellShares")) {
                    String data = pa.toString();
                    try {
                        String[] parts = data.split(" ");
                        int pct = Integer.parseInt(parts[1].replace("%", ""));
                        String comp = parts[3];
                        sellMap.putIfAbsent(comp, new ArrayList<>());
                        sellMap.get(comp).add(pct);
                    } catch (Exception e) {}
                } else if (aName.equals("BuyCertificate")) {
                    String data = pa.toString();
                    String comp = extractValue(data, "company=");
                    String from = extractValue(data, "from=");
                    String price = extractValue(data, "price=");
                    String bstr = comp + " (" + from + ", $" + price + ")";
                    if (!buyList.contains(bstr)) buyList.add(bstr);
                } else {
                    standardActions.add(pa);
                }
            }

            int rowCount = 1;
            
            // 1. Float Company Summary
            if (floatPrices != null) {
                sb.append("<tr style='border-bottom:1px solid #e0e0e0;'><td style='padding:10px; font-weight:bold; color:#333;'>")
                  .append(rowCount++).append(". Float Company</td><td style='padding:10px; color:#555;'>Start Prices: ")
                  .append(floatPrices).append("</td></tr>");
            }
            
            // 2. Buy Shares Summary
            if (!buyList.isEmpty()) {
                sb.append("<tr style='border-bottom:1px solid #e0e0e0;'><td style='padding:10px; font-weight:bold; color:#333;'>")
                  .append(rowCount++).append(". Buy Shares</td><td style='padding:10px; color:#555;'>")
                  .append(String.join(", ", buyList)).append("</td></tr>");
            }

            // 3. Sell Shares Summary (One row per company range)
            for (Map.Entry<String, List<Integer>> entry : sellMap.entrySet()) {
                List<Integer> pcts = entry.getValue();
                Collections.sort(pcts);
                String range = pcts.get(0) + "%";
                if (pcts.size() > 1) {
                    range += "-" + pcts.get(pcts.size() - 1) + "%";
                }
                sb.append("<tr style='border-bottom:1px solid #e0e0e0;'><td style='padding:10px; font-weight:bold; color:#333;'>")
                  .append(rowCount++).append(". Sell Shares</td><td style='padding:10px; color:#555;'>Sell ")
                  .append(entry.getKey()).append(" (").append(range).append(")</td></tr>");
            }

            // 4. Standard Actions
            for (PossibleAction pa : standardActions) {
                String actionName = pa.getClass().getSimpleName();
                String displayAction = translateActionToName(actionName, pa);
                String details = translateActionToDetails(actionName, pa);

                sb.append("<tr style='border-bottom:1px solid #e0e0e0;'>")
                  .append("<td style='padding:10px; font-weight:bold; color:#333;'>")
                  .append(rowCount++).append(". ").append(displayAction)
                  .append("</td>")
                  .append("<td style='padding:10px; color:#555;'>")
                  .append(details)
                  .append("</td></tr>");
            }

            // 5. Pass Action (Always Last)
            if (passAction != null) {
                sb.append("<tr style='border-bottom:1px solid #e0e0e0;'><td style='padding:10px; font-weight:bold; color:#333;'>")
                  .append(rowCount).append(". Pass</td><td style='padding:10px; color:#555;'>Do nothing</td></tr>");
            }

            sb.append("</table>");
            
            // --- AI ADVISOR ---
            String aiRecommendationText = "Analyzing layout...";
            try {
                net.sf.rails.game.ai.AIPlayer aiBrain = new net.sf.rails.game.ai.AIPlayer("AI_Help_Advisor", gm);
                PossibleAction aiAction = aiBrain.chooseMove(
                    company,
                    gm.getPossibleActions(),
                    java.util.Collections.emptyList(),
                    java.util.Collections.emptyList()
                );

                if (aiAction != null) {
                    String aiName = aiAction.getClass().getSimpleName();
                    if (aiName.equals("NullAction")) {
                        aiRecommendationText = "Pass";
                    } else {
                        aiRecommendationText = translateActionToName(aiName, aiAction);
                    }
                } else {
                    aiRecommendationText = "No optimal move found.";
                }
            } catch (Exception e) {
                aiRecommendationText = "Advisor paused.";
            }

            sb.append("<div style='margin-top:20px; padding:10px; background-color:#f1f8e9; border-left:4px solid #7cb342; font-size:12px;'>")
              .append("<b>AI Recommends:</b> ").append(aiRecommendationText)
              .append("</div>");

        } else {
            sb.append("<p style='color:gray; text-align:center;'>No actions currently available.</p>");
        }
        
        sb.append("</body></html>");
        helpTextPane.setText(sb.toString());
    }

    private String translateActionToName(String actionName, PossibleAction pa) {
        String data = pa.toString();
        if (actionName.equals("LayTile")) return "Lay Track";
        if (actionName.contains("LayToken")) return "Lay Station Marker";
        if (actionName.equals("SetDividend")) return "Confirm Revenue";
        if (actionName.equals("TakeLoans")) return "Take Loans";
        if (actionName.equals("BuyPrivate")) return "Buy Private";
        if (actionName.equals("BuyBonusToken")) {
            String name = extractValue(data, "name=");
            String priv = extractValue(data, "privateCompany=Private: ");
            return "Buy " + name + " from " + priv;
        }
        return actionName;
    }

    private String translateActionToDetails(String actionName, PossibleAction pa) {
        String data = pa.toString();
        
        if (actionName.equals("LayTile") || actionName.contains("LayToken") || actionName.equals("SetDividend") || actionName.equals("BuyBonusToken")) {
            return "-";
        }
        if (actionName.equals("BuyPrivate")) {
            String priv = extractValue(data, "privateCompany=Private: ");
            String min = extractValue(data, "minimumPrice=");
            String max = extractValue(data, "maximumPrice=");
            return priv + " ($" + min + " - $" + max + ")";
        }
        if (actionName.equals("TakeLoans")) {
            return "$" + extractValue(data, "price=") + " per loan";
        }
        return data; // Fallback for unmapped variables
    }

    private String extractValue(String data, String key) {
        int start = data.indexOf(key);
        if (start != -1) {
            start += key.length();
            int end = data.indexOf(",", start);
            if (end == -1) end = data.indexOf("]", start);
            if (end == -1) end = data.length();
            return data.substring(start, end).trim();
        }
        return "Unknown";
    }
}
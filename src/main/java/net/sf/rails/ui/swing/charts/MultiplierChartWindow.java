package net.sf.rails.ui.swing.charts;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.PrivateCompany;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

public class MultiplierChartWindow extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(MultiplierChartWindow.class);
    
    // UI Constants
    private static final int BAR_WIDTH = 40;
    private static final int GAP = 20;
    private static final int TOP_MARGIN = 40;
    private static final int BOTTOM_MARGIN = 60;
    private static final int LEFT_MARGIN = 60;
    
    private final GameManager gameManager;
    private List<CompanyMultiplier> dataList;
    private double maxMultiplier = 0;

    // Data structures for Buying Power Charts
    private Map<Integer, Map<String, Double>> buyingPowerMap; // Key: SR Number, Value: {CompanyID -> Cumulative Cash}
    private Map<String, Map<String, Double>> operatingPowerMap; // Key: OR ID, Value: {CompanyID -> Round Cash}
    private Map<String, Color> companyColorMap; // Cache colors
    
    private List<Integer> sortedSRList; 
    private int currentSRIndex = 0; 
    
    private List<String> sortedORList; 
    private int currentORIndex = 0;

    private JPanel mainChartContainer;
    private CardLayout cardLayout;
    private StringBuilder analysisLog;

    public MultiplierChartWindow(JFrame parent, GameManager gm) {
        super(parent, "Investment Analysis", false);
        this.gameManager = gm;
        
        companyColorMap = new HashMap<>();
        buyingPowerMap = new TreeMap<>();
        operatingPowerMap = new HashMap<>();
        sortedORList = new ArrayList<>();
        
        calculateData();
        calculateBuyingPowerData();
        initUI();
    }

    public void calculateData() {
        dataList = new ArrayList<>();
        analysisLog = new StringBuilder();
        maxMultiplier = 0;

        analysisLog.append("=== ROI ANALYSIS LOG ===\n");
        analysisLog.append("Note: Calculations based on 'Pure IPO' basis (10% share Par Price).\n");
        analysisLog.append("Includes cumulative historical dividends + final stock value.\n\n");

        // Use Instantaneous History to avoid double-counting in ROI summation loop
        Map<String, Map<String, Integer>> history = gameManager.getInstantaneousPayoutHistory();
        if (history == null) history = new HashMap<>();

        List<String> roundIds = new ArrayList<>(history.keySet());
        roundIds.sort(new RoundComparator()); // Use robust comparator

        // 2. Process Public Companies (Majors)
        for (PublicCompany pub : gameManager.getAllPublicCompanies()) {
            if (pub.getId().matches("M[1-6]")) continue; 
            if (pub.getParPrice() <= 0) continue;

            double startCost = pub.getParPrice(); 
            double finalStockValue = pub.getCurrentPrice();
            double totalDividends = 0;

            for (String round : roundIds) {
                Map<String, Integer> roundPayouts = history.get(round);
                if (roundPayouts != null && roundPayouts.containsKey(pub.getId())) {
                    totalDividends += (double) roundPayouts.get(pub.getId()) / 10.0;
                }
            }

            double totalReturn = totalDividends + finalStockValue;
            double multiplier = totalReturn / startCost;

            analysisLog.append(String.format("MAJOR: %s\n", pub.getId()));
            analysisLog.append(String.format("  - Basis (Par): %.1f\n", startCost));
            analysisLog.append(String.format("  - Cumulative Divs: %.1f\n", totalDividends));
            analysisLog.append(String.format("  - Final Stock Val: %.1f\n", finalStockValue));
            analysisLog.append(String.format("  - Total Return: %.1f -> %.2fx\n\n", totalReturn, multiplier));

            addMultiplierData(pub.getId(), multiplier, pub.getBgColor());
            companyColorMap.put(pub.getId(), pub.getBgColor());
        }

        // 3. Process Private Companies
        for (PrivateCompany priv : gameManager.getAllPrivateCompanies()) {
            String id = priv.getId();
            double cost = priv.getBasePrice();
            if (cost <= 0) continue;

            Color privateBarColor = Color.YELLOW; // Default
            String associatedMajorId = null;
            double associatedSharePercent = 0.0;

            if ("NF".equals(id) || "PfB".equals(id) || "OBB".equals(id)) { 
                associatedMajorId = "BY";
                associatedSharePercent = 0.10;
                privateBarColor = Color.BLUE; // BY Color
            } else if ("LD".equals(id)) {
                associatedMajorId = "SX";
                associatedSharePercent = 0.20; 
                privateBarColor = Color.RED;  // SX Color
            } else if ("HB".equals(id) || "BB".equals(id)) {
                privateBarColor = Color.BLACK; // Black for Pure Privates
            } else if (id.matches("M[1-6]")) {
                continue; 
            }

            double fixedIncomeTotal = 0;
            double associatedStockReturn = 0;

            for (String round : roundIds) {
                Map<String, Integer> roundPayouts = history.get(round);
                if (roundPayouts == null) continue;

                if (roundPayouts.containsKey(id)) {
                    fixedIncomeTotal += roundPayouts.get(id);
                }

                if (associatedMajorId != null && roundPayouts.containsKey(associatedMajorId)) {
                    double majorRev = roundPayouts.get(associatedMajorId);
                    // Only add shadow dividends if configured (NF/PfB/OBB)
                    // LD is excluded from shadow dividends in Buying Power, but we keep it in ROI for "Total Package Value"
                    // However, to align with Buying Power, maybe strict separation? 
                    // Keeping ROI logic generous as requested previously.
                    double perTenPercent = majorRev / 10.0;
                    associatedStockReturn += (perTenPercent * (associatedSharePercent * 10)); 
                }
            }

            double associatedStockValue = 0;
            if (associatedMajorId != null) {
                PublicCompany major = gameManager.getRoot().getCompanyManager().getPublicCompany(associatedMajorId);
                if (major != null) {
                    associatedStockValue = major.getCurrentPrice() * (associatedSharePercent * 10);
                }
            }

            double totalReturn = fixedIncomeTotal + associatedStockReturn + associatedStockValue;
            double multiplier = totalReturn / cost;

            if (multiplier > 0) {
                analysisLog.append(String.format("PRIVATE: %s\n", id));
                analysisLog.append(String.format("  - Basis (Cost): %.1f\n", cost));
                analysisLog.append(String.format("  - Fixed Income: %.1f\n", fixedIncomeTotal));
                if (associatedMajorId != null) {
                    analysisLog.append(String.format("  - Associated Divs: %.1f\n", associatedStockReturn));
                    analysisLog.append(String.format("  - Associated Stock Val: %.1f\n", associatedStockValue));
                }
                analysisLog.append(String.format("  - Total Return: %.1f -> %.2fx\n\n", totalReturn, multiplier));
                
                addMultiplierData(id, multiplier, privateBarColor);
                companyColorMap.put(id, privateBarColor);
            }
        }

        // 4. Process Minors
        String[] minorIds = {"M1", "M2", "M3", "M4", "M5", "M6"};
        for (String mId : minorIds) {
            PublicCompany minor = gameManager.getRoot().getCompanyManager().getPublicCompany(mId); 
            if (minor == null) continue; 
            
            double cost = minor.getParPrice(); 
            if (cost <= 0) continue;

            double prussianSharePercent = (mId.equals("M2") || mId.equals("M4")) ? 0.10 : 0.05;
            double minorIncomeTotal = 0;
            double prIncomeTotal = 0;

            for (String round : roundIds) {
                Map<String, Integer> roundPayouts = history.get(round);
                if (roundPayouts == null) continue;

                if (roundPayouts.containsKey(mId)) {
                    minorIncomeTotal += (double) roundPayouts.get(mId) * 0.5;
                } else {
                    if (roundPayouts.containsKey("PR")) {
                         double prRev = roundPayouts.get("PR");
                         prIncomeTotal += (prRev / 10.0) * (prussianSharePercent * 10);
                    }
                }
            }

            double prFinalValue = 0;
            PublicCompany pr = gameManager.getRoot().getCompanyManager().getPublicCompany("PR");
            if (pr != null) {
                prFinalValue = pr.getCurrentPrice() * (prussianSharePercent * 10);
            }

            double totalReturn = minorIncomeTotal + prIncomeTotal + prFinalValue;
            double multiplier = totalReturn / cost;

            analysisLog.append(String.format("MINOR: %s (-> PR %d%%)\n", mId, (int)(prussianSharePercent*100)));
            analysisLog.append(String.format("  - Total Return: %.1f -> %.2fx\n\n", totalReturn, multiplier));

            addMultiplierData(mId, multiplier, Color.BLACK); 
            companyColorMap.put(mId, Color.BLACK);
        }
    }

    private void calculateBuyingPowerData() {
        buyingPowerMap.clear();
        operatingPowerMap.clear();
        sortedORList.clear();

        Map<String, Map<String, Integer>> history = gameManager.getInstantaneousPayoutHistory();
        if (history == null) history = new HashMap<>();

        // Sort Round IDs Chronologically
        List<String> roundIds = new ArrayList<>(history.keySet());
        roundIds.sort(new RoundComparator());

        // Keeps track of the total cash generated by each company ID up to the current point
        Map<String, Double> runningTotals = new HashMap<>();

        for (String roundId : roundIds) {
            Map<String, Integer> payouts = history.get(roundId);

            // Only process Operating Rounds for cash flow
            if (roundId.startsWith("OR")) {
                try {
                    // Extract SR Target (OR 1.x -> SR 2, OR 2.x -> SR 3)
                    String[] parts = roundId.split("_")[1].split("\\.");
                    int orMajorNum = Integer.parseInt(parts[0]);
                    int targetSrNum = orMajorNum + 1;

                    // Initialize OR Map (Discrete)
                    operatingPowerMap.putIfAbsent(roundId, new HashMap<>());
                    Map<String, Double> orCash = operatingPowerMap.get(roundId);
                    if (!sortedORList.contains(roundId)) sortedORList.add(roundId);

                    // Initialize SR Map (Cumulative) if not present
                    buyingPowerMap.putIfAbsent(targetSrNum, new HashMap<>());
                    
                    // --- Calculate Contributions ---
                    for (Map.Entry<String, Integer> payout : payouts.entrySet()) {
                        String companyId = payout.getKey();
                        double rawAmount = payout.getValue();
                        double dividendAmount = 0;

                        // 1. Minors: 50% Split
                        if (companyId.matches("M[1-6]")) { 
                            dividendAmount = rawAmount * 0.5;
                        } 
                        // 2. Majors: 10% Share Dividend
                        else {
                            PublicCompany pub = gameManager.getRoot().getCompanyManager().getPublicCompany(companyId);
                            if (pub != null) {
                                double shareDividend = rawAmount / 10.0;
                                dividendAmount = shareDividend;
                                
                                // Shadow Shares (NF, PfB, OBB)
                                // Add 10% BY dividend to them to show "Paper Value > Major Value"
                                if ("BY".equals(companyId)) {
                                    recordFlow(orCash, runningTotals, "NF", shareDividend);
                                    recordFlow(orCash, runningTotals, "PfB", shareDividend);
                                    recordFlow(orCash, runningTotals, "OBB", shareDividend);
                                } 
                                // NOTE: LD excluded here per instruction "LD is exchanged... not together"
                            } else {
                                // 3. Privates: Fixed Income (Raw)
                                dividendAmount = rawAmount;
                            }
                        }
                        
                        if (dividendAmount > 0) {
                            recordFlow(orCash, runningTotals, companyId, dividendAmount);
                        }
                    }
                    
                    // --- Snapshot for Cumulative Chart ---
                    // For the SR chart, we store the state of 'runningTotals' at the end of this OR group.
                    // Since we iterate chronologically, overwriting the SR map entry keeps the latest state.
                    buyingPowerMap.put(targetSrNum, new HashMap<>(runningTotals));

                } catch (Exception e) {
                    log.warn("Failed to parse round ID: " + roundId);
                }
            }
        }
        
        // Prepare Navigation Lists
        sortedSRList = new ArrayList<>(buyingPowerMap.keySet());
        Collections.sort(sortedSRList);
        if (!sortedSRList.isEmpty()) currentSRIndex = 0;
        
        if (!sortedORList.isEmpty()) currentORIndex = 0;
    }
    
    // Helper to update both the Discrete Map and the Running Totals
    private void recordFlow(Map<String, Double> discreteMap, Map<String, Double> runningMap, String key, double val) {
        // Discrete (OR Chart): Add to current round's total
        discreteMap.put(key, discreteMap.getOrDefault(key, 0.0) + val);
        
        // Cumulative (SR Chart): Add to lifetime total
        runningMap.put(key, runningMap.getOrDefault(key, 0.0) + val);
    }

    private void addTo(Map<String, Double> map, String key, double val) {
        map.put(key, map.getOrDefault(key, 0.0) + val);
    }

    private void addMultiplierData(String id, double val, Color c) {
        dataList.add(new CompanyMultiplier(id, val, c));
        if (val > maxMultiplier) maxMultiplier = val;
    }

    private void initUI() {
        this.setLayout(new BorderLayout());
        
        cardLayout = new CardLayout();
        mainChartContainer = new JPanel(cardLayout);

        // 1. ROI Panel
        JPanel roiPanel = new MultiplierPanel();
        roiPanel.setBackground(Color.WHITE);
        int widthROI = LEFT_MARGIN + (dataList.size() * (BAR_WIDTH + GAP)) + GAP;
        roiPanel.setPreferredSize(new Dimension(Math.max(800, widthROI), 500));
        JScrollPane roiScroll = new JScrollPane(roiPanel);
        mainChartContainer.add(roiScroll, "ROI");

        // 2. SR Panel (Cumulative)
        JPanel buyingPowerPanel = new BuyingPowerDetailPanel();
        buyingPowerPanel.setBackground(Color.WHITE);
        buyingPowerPanel.setPreferredSize(new Dimension(800, 500));
        mainChartContainer.add(buyingPowerPanel, "Buying Power");
        
        // 3. OR Panel (Flow)
        JPanel orPowerPanel = new OperatingPowerDetailPanel();
        orPowerPanel.setBackground(Color.WHITE);
        orPowerPanel.setPreferredSize(new Dimension(800, 500));
        mainChartContainer.add(orPowerPanel, "OR Flow");

        this.add(mainChartContainer, BorderLayout.CENTER);

        // Controls
        JPanel buttonPanel = new JPanel();
        
        String[] views = {"Lifetime ROI Analysis", "Cumulative Buying Power (SR)", "Operating Round Cash Flow"};
        JComboBox<String> viewSelector = new JComboBox<>(views);
        
        JButton prevBtn = new JButton("< Previous");
        JButton nextBtn = new JButton("Next >");
        prevBtn.setEnabled(false);

        viewSelector.addActionListener(e -> {
            String selected = (String) viewSelector.getSelectedItem();
            if (selected.contains("ROI")) {
                cardLayout.show(mainChartContainer, "ROI");
                prevBtn.setEnabled(false);
                nextBtn.setEnabled(false);
            } else if (selected.contains("Cumulative")) {
                cardLayout.show(mainChartContainer, "Buying Power");
                prevBtn.setEnabled(true);
                nextBtn.setEnabled(true);
            } else {
                cardLayout.show(mainChartContainer, "OR Flow");
                prevBtn.setEnabled(true);
                nextBtn.setEnabled(true);
            }
        });
        buttonPanel.add(new JLabel("Chart Mode:"));
        buttonPanel.add(viewSelector);

        prevBtn.addActionListener(e -> {
            String selected = (String) viewSelector.getSelectedItem();
            if (selected.contains("Cumulative")) {
                if (sortedSRList != null && !sortedSRList.isEmpty() && currentSRIndex > 0) {
                    currentSRIndex--;
                    buyingPowerPanel.repaint();
                }
            } else if (selected.contains("Operating")) {
                if (sortedORList != null && !sortedORList.isEmpty() && currentORIndex > 0) {
                    currentORIndex--;
                    orPowerPanel.repaint();
                }
            }
        });
        
        nextBtn.addActionListener(e -> {
            String selected = (String) viewSelector.getSelectedItem();
            if (selected.contains("Cumulative")) {
                if (sortedSRList != null && !sortedSRList.isEmpty() && currentSRIndex < sortedSRList.size() - 1) {
                    currentSRIndex++;
                    buyingPowerPanel.repaint();
                }
            } else if (selected.contains("Operating")) {
                if (sortedORList != null && !sortedORList.isEmpty() && currentORIndex < sortedORList.size() - 1) {
                    currentORIndex++;
                    orPowerPanel.repaint();
                }
            }
        });

        buttonPanel.add(prevBtn);
        buttonPanel.add(nextBtn);

        JButton showLogBtn = new JButton("Show Analysis Log");
        showLogBtn.addActionListener((ActionEvent e) -> showAnalysisWindow());
        buttonPanel.add(showLogBtn);
        
        this.add(buttonPanel, BorderLayout.SOUTH);

        this.pack();
        this.setLocationRelativeTo(getOwner());
    }

    private void showAnalysisWindow() {
        JDialog logDialog = new JDialog(this, "Detailed ROI Calculation", false);
        JTextArea textArea = new JTextArea(analysisLog.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(500, 600));
        logDialog.add(scroll);
        logDialog.pack();
        logDialog.setLocationRelativeTo(this);
        logDialog.setVisible(true);
    }

    // --- Comparator for Round IDs (OR_1.1 < OR_1.2 < OR_2.1) ---
    private class RoundComparator implements Comparator<String> {
        @Override
        public int compare(String id1, String id2) {
            try {
                String[] p1 = id1.split("_");
                String[] p2 = id2.split("_");
                
                // Compare Round Type (SR vs OR) - though we usually sort only one type here
                int typeComp = p1[0].compareTo(p2[0]);
                if (typeComp != 0) return typeComp;
                
                // Compare Numbers "1.1" vs "1.2"
                String[] n1 = p1[1].split("\\.");
                String[] n2 = p2[1].split("\\.");
                
                int maj1 = Integer.parseInt(n1[0]);
                int maj2 = Integer.parseInt(n2[0]);
                if (maj1 != maj2) return Integer.compare(maj1, maj2);
                
                // If secondary part exists
                if (n1.length > 1 && n2.length > 1) {
                    return Integer.compare(Integer.parseInt(n1[1]), Integer.parseInt(n2[1]));
                }
                return Integer.compare(n1.length, n2.length);
            } catch (Exception e) {
                return id1.compareTo(id2);
            }
        }
    }

    // --- ROI Bar Chart ---
    private class MultiplierPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (dataList.isEmpty()) {
                g.drawString("No data available.", 20, 30);
                return;
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight(); int w = getWidth();
            int chartBottom = h - BOTTOM_MARGIN; int chartHeight = chartBottom - TOP_MARGIN;

            g2.setColor(Color.BLACK);
            g2.drawLine(LEFT_MARGIN, TOP_MARGIN, LEFT_MARGIN, chartBottom);
            g2.drawLine(LEFT_MARGIN, chartBottom, w - 20, chartBottom);

            int gridLines = 5;
            for (int i = 0; i <= gridLines; i++) {
                double val = maxMultiplier * i / gridLines;
                int y = chartBottom - (int) (val / maxMultiplier * chartHeight);
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawLine(LEFT_MARGIN, y, w - 20, y);
                g2.setColor(Color.BLACK);
                g2.drawString(String.format("%.1fx", val), LEFT_MARGIN - 40, y + 5);
            }

            int x = LEFT_MARGIN + GAP;
            Font boldFont = new Font("SansSerif", Font.BOLD, 12);
            g2.setFont(boldFont);

            for (CompanyMultiplier item : dataList) {
                int barHeight = (int) ((item.multiplier / maxMultiplier) * chartHeight);
                int y = chartBottom - barHeight;
                g2.setColor(item.color);
                g2.fillRect(x, y, BAR_WIDTH, barHeight);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, y, BAR_WIDTH, barHeight);
                String valStr = String.format("%.1f", item.multiplier);
                int strW = g2.getFontMetrics().stringWidth(valStr);
                g2.drawString(valStr, x + (BAR_WIDTH - strW) / 2, y - 5);
                g2.drawString(item.id, x + (BAR_WIDTH - g2.getFontMetrics().stringWidth(item.id)) / 2, chartBottom + 20);
                x += (BAR_WIDTH + GAP);
            }
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("Investment Return per $1 (at Par)", w / 2 - 100, 25);
        }
    }

    // --- Cumulative Panel ---
    private class BuyingPowerDetailPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (buyingPowerMap.isEmpty() || sortedSRList.isEmpty()) {
                g.drawString("No cash flow data available.", 20, 30);
                return;
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight(); int w = getWidth();
            int chartBottom = h - BOTTOM_MARGIN; int chartHeight = chartBottom - TOP_MARGIN;

            Integer srNum = sortedSRList.get(currentSRIndex);
            Map<String, Double> srData = buyingPowerMap.get(srNum);
            
            // Dynamic Scale
            double currentRoundMax = 0;
            double totalCash = 0;
            for (double val : srData.values()) {
                if (val > currentRoundMax) currentRoundMax = val;
                totalCash += val;
            }
            if (currentRoundMax == 0) currentRoundMax = 100;

            g2.setColor(Color.BLACK);
            g2.drawLine(LEFT_MARGIN, TOP_MARGIN, LEFT_MARGIN, chartBottom);
            g2.drawLine(LEFT_MARGIN, chartBottom, w - 20, chartBottom);

            int gridLines = 10;
            for (int i = 0; i <= gridLines; i++) {
                double val = currentRoundMax * i / gridLines;
                int y = chartBottom - (int) (val / currentRoundMax * chartHeight);
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawLine(LEFT_MARGIN, y, w - 20, y);
                g2.setColor(Color.BLACK);
                g2.drawString(String.format("%d", (int)val), LEFT_MARGIN - 45, y + 5);
            }

            int x = LEFT_MARGIN + GAP;
            int barWidth = 40; 
            Font boldFont = new Font("SansSerif", Font.BOLD, 12);
            g2.setFont(boldFont);

            List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(srData.entrySet());
            sortedEntries.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

            for (Map.Entry<String, Double> entry : sortedEntries) {
                String compId = entry.getKey();
                double amount = entry.getValue();
                if (amount <= 0) continue;

                int barHeight = (int) ((amount / currentRoundMax) * chartHeight);
                if (barHeight < 2) barHeight = 2;
                int y = chartBottom - barHeight;

                Color c = companyColorMap.getOrDefault(compId, Color.GRAY);
                g2.setColor(c);
                g2.fillRect(x, y, barWidth, barHeight);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, y, barWidth, barHeight);

                String valStr = String.format("%d", (int)amount);
                g2.drawString(valStr, x + (barWidth - g2.getFontMetrics().stringWidth(valStr))/2, y - 5);
                g2.drawString(compId, x + (barWidth - g2.getFontMetrics().stringWidth(compId))/2, chartBottom + 20);
                x += (barWidth + GAP);
            }
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString(String.format("Cumulative Buying Power for SR %d (Total: %d)", srNum, (int)totalCash), w / 2 - 200, 25);
        }
    }

    // --- OR Flow Panel ---
    private class OperatingPowerDetailPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (operatingPowerMap.isEmpty() || sortedORList.isEmpty()) {
                g.drawString("No cash flow data available.", 20, 30);
                return;
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight(); int w = getWidth();
            int chartBottom = h - BOTTOM_MARGIN; int chartHeight = chartBottom - TOP_MARGIN;

            String orId = sortedORList.get(currentORIndex);
            Map<String, Double> orData = operatingPowerMap.get(orId);
            
            double currentRoundMax = 0;
            double totalCash = 0;
            for (double val : orData.values()) {
                if (val > currentRoundMax) currentRoundMax = val;
                totalCash += val;
            }
            if (currentRoundMax == 0) currentRoundMax = 100;

            g2.setColor(Color.BLACK);
            g2.drawLine(LEFT_MARGIN, TOP_MARGIN, LEFT_MARGIN, chartBottom);
            g2.drawLine(LEFT_MARGIN, chartBottom, w - 20, chartBottom);

            int gridLines = 10;
            for (int i = 0; i <= gridLines; i++) {
                double val = currentRoundMax * i / gridLines;
                int y = chartBottom - (int) (val / currentRoundMax * chartHeight);
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawLine(LEFT_MARGIN, y, w - 20, y);
                g2.setColor(Color.BLACK);
                g2.drawString(String.format("%d", (int)val), LEFT_MARGIN - 45, y + 5);
            }

            int x = LEFT_MARGIN + GAP;
            int barWidth = 40; 
            Font boldFont = new Font("SansSerif", Font.BOLD, 12);
            g2.setFont(boldFont);

            List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(orData.entrySet());
            sortedEntries.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

            for (Map.Entry<String, Double> entry : sortedEntries) {
                String compId = entry.getKey();
                double amount = entry.getValue();
                if (amount <= 0) continue;

                int barHeight = (int) ((amount / currentRoundMax) * chartHeight);
                if (barHeight < 2) barHeight = 2;
                int y = chartBottom - barHeight;

                Color c = companyColorMap.getOrDefault(compId, Color.GRAY);
                g2.setColor(c);
                g2.fillRect(x, y, barWidth, barHeight);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, y, barWidth, barHeight);

                String valStr = String.format("%d", (int)amount);
                g2.drawString(valStr, x + (barWidth - g2.getFontMetrics().stringWidth(valStr))/2, y - 5);
                g2.drawString(compId, x + (barWidth - g2.getFontMetrics().stringWidth(compId))/2, chartBottom + 20);
                x += (barWidth + GAP);
            }
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString(String.format("Cash Flow for %s (Total: %d)", orId, (int)totalCash), w / 2 - 150, 25);
        }
    }

    private static class CompanyMultiplier {
        String id;
        double multiplier;
        Color color;
        CompanyMultiplier(String id, double multiplier, Color color) {
            this.id = id; this.multiplier = multiplier; this.color = color;
        }
    } 

    public static void showChart(JFrame parent, GameManager gm) {
        SwingUtilities.invokeLater(() -> {
            MultiplierChartWindow win = new MultiplierChartWindow(parent, gm);
            win.setVisible(true);
        });
    }
}
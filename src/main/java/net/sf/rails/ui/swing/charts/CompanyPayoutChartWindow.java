package net.sf.rails.ui.swing.charts;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Creates and displays a graphical chart showing the payouts 
 * of all companies over the history of the game.
 * Supports both Cumulative and Instantaneous views via tabs.
 * * Features:
 * - Uses official Company Colors for Majors.
 * - Uses distinct "Fresh" colors for Minors (M1-M6) to avoid black-on-black.
 * - Renders Minors with DASHED lines for visual distinction.
 */
public class CompanyPayoutChartWindow extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(CompanyPayoutChartWindow.class);
    private final ChartDataController controller;
    private final RevealController revealController;

    // --- Chart Components ---
    private PayoutChartPanel cumulativePanel;
    private PayoutChartPanel instantaneousPanel;

    public CompanyPayoutChartWindow(JFrame parentFrame, GameManager gm) {
        super(parentFrame, "Company Payout Charts", false);
        this.controller = new ChartDataController(gm);
        this.revealController = new RevealController(controller.roundKeys.size());
        initializeGUI();
    }

    private void initializeGUI() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        
        // 1. Title
        JLabel titleLabel = new JLabel("Company Revenue History", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // 2. Tabbed Pane for Charts
        JTabbedPane tabbedPane = new JTabbedPane();

        // --- Tab 1: Cumulative ---
        cumulativePanel = new PayoutChartPanel(controller, revealController, true); 
        cumulativePanel.setPreferredSize(new Dimension(800, 500));
        tabbedPane.addTab("Cumulative Totals", null, new JScrollPane(cumulativePanel), "Total revenue earned over time");

        // --- Tab 2: Instantaneous ---
        instantaneousPanel = new PayoutChartPanel(controller, revealController, false); 
        instantaneousPanel.setPreferredSize(new Dimension(800, 500));
        tabbedPane.addTab("Round Payouts", null, new JScrollPane(instantaneousPanel), "Revenue earned in each specific round");

        contentPanel.add(tabbedPane, BorderLayout.CENTER);

        // 3. Legend Panel 
        LegendPanel legendPanel = new LegendPanel(controller);
        JScrollPane legendScroll = new JScrollPane(legendPanel);
        legendScroll.setPreferredSize(new Dimension(800, 100));
        legendScroll.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.add(legendScroll, BorderLayout.SOUTH);
        
        this.setContentPane(contentPanel);
        this.setPreferredSize(new Dimension(900, 750));
        this.pack();
        this.setLocationRelativeTo(getOwner());
        setupHotkeys();
    }

    private void setupHotkeys() {
        JRootPane rootPane = this.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        String REVEAL_ACTION = "revealNextRound";

        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SPACE, 0), REVEAL_ACTION);
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, 0), REVEAL_ACTION);
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), REVEAL_ACTION);
        
        actionMap.put(REVEAL_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                CompanyPayoutChartWindow.this.revealNextRound();
            }
        });
    }

    public void revealNextRound() {
        if (revealController.canReveal()) {
            revealController.revealOne();
            cumulativePanel.repaint();
            instantaneousPanel.repaint();
        }
    }

    /**
     * Static method called by reflection from GameManager
     */
    public static void showChart(JFrame parentFrame, GameManager gm) {
        SwingUtilities.invokeLater(() -> {
            CompanyPayoutChartWindow chart = new CompanyPayoutChartWindow(parentFrame, gm);
            chart.setVisible(true);
            
            // Auto-reveal animation
            Timer revealTimer = new Timer(300, null); 
            revealTimer.addActionListener(e -> {
                if (chart.revealController.canReveal()) {
                    chart.revealNextRound();
                } else {
                    ((Timer)e.getSource()).stop();
                }
            });
            revealTimer.start();
        });
    }

    // ========================================================================
    // --- Helper Classes ---
    // ========================================================================
    
    private class RevealController {
        private int revealedRounds = 1; 
        private final int totalRounds;

        public RevealController(int totalRounds) {
            this.totalRounds = totalRounds;
        }

        public boolean canReveal() {
            return revealedRounds < totalRounds;
        }

        public void revealOne() {
            if (canReveal()) {
                revealedRounds++;
            }
        }
        
        public int getRevealedCount() {
            return revealedRounds;
        }
    }
    
    /**
     * Holds data sets, colors, and identifies Minors vs Majors.
     */
    private class ChartDataController {
        // Data Sets
        public final LinkedHashMap<String, Map<String, Integer>> cumulativeHistory;
        public final LinkedHashMap<String, Map<String, Integer>> instantaneousHistory;
        
        // Metadata
        public final List<String> roundKeys;
        public final List<String> companyIds;
        public final Map<String, Color> companyColors; 
        
        // Scaling
        public final int cumulativeMax;
        public final int instantaneousMax;

        // Fresh Palette for Minors (Cyan, Magenta, Orange, Lime, Pink, Teal)
        private final Color[] MINOR_PALETTE = {
            new Color(0, 191, 255), // Deep Sky Blue
            new Color(255, 0, 255), // Magenta
            new Color(255, 140, 0), // Dark Orange
            new Color(50, 205, 50), // Lime Green
            new Color(255, 105, 180), // Hot Pink
            new Color(0, 128, 128)  // Teal
        };

        public ChartDataController(GameManager gm) {
            this.cumulativeHistory = gm.getCompanyPayoutHistory();
            this.instantaneousHistory = gm.getInstantaneousPayoutHistory();
            
            // 1. Identify all companies
            Set<String> companies = new TreeSet<>();
            if (cumulativeHistory != null) {
                for (Map<String, Integer> map : cumulativeHistory.values()) companies.addAll(map.keySet());
            }
            if (instantaneousHistory != null) {
                for (Map<String, Integer> map : instantaneousHistory.values()) companies.addAll(map.keySet());
            }
            this.companyIds = new ArrayList<>(companies);

            // 2. Setup Round Keys (Filter out SR)
            this.roundKeys = new ArrayList<>();
            if (cumulativeHistory != null) {
                for (String key : cumulativeHistory.keySet()) {
                    if (!key.startsWith("SR")) { 
                        this.roundKeys.add(key);
                    }
                }
            }

            // 3. Calculate Maximums
            this.cumulativeMax = calculateMax(cumulativeHistory);
            this.instantaneousMax = calculateMax(instantaneousHistory);
            
            // 4. Initialize Colors
            this.companyColors = new HashMap<>();
            int minorIndex = 0;

            for (String compId : this.companyIds) {
                if (isMinor(compId)) {
                    // Assign fresh color from palette
                    this.companyColors.put(compId, MINOR_PALETTE[minorIndex % MINOR_PALETTE.length]);
                    minorIndex++;
                } else {
                    // Try to get official color
                    PublicCompany comp = gm.getRoot().getCompanyManager().getPublicCompany(compId);
                    if (comp != null) {
                        this.companyColors.put(compId, comp.getBgColor());
                    } else {
                        // Fallback gray
                        int hash = compId.hashCode();
                        this.companyColors.put(compId, new Color((hash & 0xFF0000) >> 16, (hash & 0x00FF00) >> 8, (hash & 0x0000FF)));
                    }
                }
            }
        }

        /**
         * Heuristic to detect Minors (M1, M2...) or generic numbered companies.
         */
        public boolean isMinor(String compId) {
            // Regex: Starts with 'M' followed by digits (e.g., M1, M12)
            return compId.matches("M\\d+");
        }

        private int calculateMax(Map<String, Map<String, Integer>> history) {
            int max = 0;
            if (history != null) {
                for (Map<String, Integer> snapshot : history.values()) {
                    for (Integer val : snapshot.values()) {
                        if (val > max) max = val;
                    }
                }
            }
            return Math.max(100, (int)(max + (max * 0.1)));
        }
    }

    /**
     * Renders the charts with Dashed Lines for Minors.
     */
    private class PayoutChartPanel extends JPanel {
        private final ChartDataController controller;
        private final RevealController revealController;
        private final boolean isCumulative;
        
        // Define Strokes
        private final BasicStroke SOLID_STROKE = new BasicStroke(2.5f);
        private final BasicStroke DASHED_STROKE = new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f);

        public PayoutChartPanel(ChartDataController controller, RevealController rc, boolean isCumulative) {
            this.controller = controller;
            this.revealController = rc;
            this.isCumulative = isCumulative;
            this.setBackground(Color.WHITE);
            this.setFont(new Font("SansSerif", Font.PLAIN, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Map<String, Map<String, Integer>> activeHistory = isCumulative ? controller.cumulativeHistory : controller.instantaneousHistory;

            if (activeHistory == null || activeHistory.isEmpty()) {
                drawNoDataMessage(g2d);
                return;
            }

            drawChart(g2d, activeHistory);
        }

        private void drawNoDataMessage(Graphics2D g2d) {
            String msg = "No data available.";
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString(msg, 10, 20);
        }

        private void drawChart(Graphics2D g2d, Map<String, Map<String, Integer>> history) {
            int w = getWidth();
            int h = getHeight();
            int padLeft = 60; 
            int padRight = 30; 
            int padTop = 30;
            int padBottom = 80; 

            int chartW = w - padLeft - padRight;
            int chartH = h - padTop - padBottom;
            
            int roundsToPlot = revealController.getRevealedCount();

            // Y-Axis Scaling
            int maxVal = isCumulative ? controller.cumulativeMax : controller.instantaneousMax;
            int range = maxVal;
            int yGridLines = 10;

            // Draw Grid and Y-Axis Labels
            for (int i = 0; i <= yGridLines; i++) {
                int y = padTop + (int)(chartH * i / (double)yGridLines);
                double val = maxVal - (range * i / (double)yGridLines);
                
                g2d.setColor(new Color(220, 220, 220)); 
                g2d.drawLine(padLeft, y, w - padRight, y);
                
                g2d.setColor(Color.BLACK);
                String label = String.format("%,d", (int)val);
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(label, padLeft - fm.stringWidth(label) - 5, y + 5);
            }

            // Draw Axes
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(padLeft, padTop, padLeft, h - padBottom); // Y Axis
            g2d.drawLine(padLeft, h - padBottom, w - padRight, h - padBottom); // X Axis
            
            // X-Axis Labels
            int numTotalRounds = controller.roundKeys.size();
            double xStep = (double) chartW / Math.max(1, numTotalRounds - 1);
            
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i < numTotalRounds; i++) {
                double xPos = padLeft + (i * xStep);
                g2d.setColor(Color.BLACK);
                g2d.drawLine((int)xPos, h - padBottom, (int)xPos, h - padBottom + 5);

                String lbl = controller.roundKeys.get(i);
                AffineTransform at = g2d.getTransform();
                g2d.translate(xPos, h - padBottom + 20);
                g2d.rotate(Math.PI / 4);
                g2d.drawString(lbl, 0, 0);
                g2d.setTransform(at);
            }

            // Plot Lines
            for (String compId : controller.companyIds) {
                Color c = controller.companyColors.getOrDefault(compId, Color.BLACK);
                g2d.setColor(c);
                
                // Determine style: Minors get Dashed lines
                if (controller.isMinor(compId)) {
                    g2d.setStroke(DASHED_STROKE);
                } else {
                    g2d.setStroke(SOLID_STROKE);
                }

                Path2D path = new Path2D.Double();
                boolean firstPoint = true;
                
                for (int i = 0; i < roundsToPlot; i++) {
                    if (i >= controller.roundKeys.size()) break;

                    String roundId = controller.roundKeys.get(i);
                    Map<String, Integer> snapshot = history.get(roundId);
                    
                    int val = (snapshot != null) ? snapshot.getOrDefault(compId, 0) : 0;
                    
                    double x = padLeft + (i * xStep);
                    double y = padTop + chartH - ((double)val / range * chartH);

                    if (firstPoint) {
                        path.moveTo(x, y);
                        firstPoint = false;
                    } else {
                        path.lineTo(x, y);
                    }
                    
                    // Draw dots (solid)
                    Stroke saved = g2d.getStroke();
                    g2d.setStroke(new BasicStroke(1));
                    g2d.fillOval((int)x - 3, (int)y - 3, 6, 6);
                    g2d.setStroke(saved);
                }
                g2d.draw(path);
            }
        }
    }
    
    private class LegendPanel extends JPanel {
        private final ChartDataController controller;

        public LegendPanel(ChartDataController controller) {
            this.controller = controller;
            this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            this.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5)); 
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int startX = 10;
            int startY = 20;
            int lineSpacing = 20;
            int colWidth = 100; 
            
            int x = startX;
            int y = startY;

            for (String compId : controller.companyIds) {
                Color c = controller.companyColors.getOrDefault(compId, Color.BLACK);
                
                g2d.setColor(c);
                g2d.fillRect(x, y - 10, 12, 12);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y - 10, 12, 12);
                
                // Add marker for minors in legend too
                if (controller.isMinor(compId)) {
                    g2d.drawString(compId + " (Minor)", x + 20, y);
                } else {
                    g2d.drawString(compId, x + 20, y);
                }

                x += colWidth + 20; // Extra width for "(Minor)" text
                
                if (x > getWidth() - colWidth) {
                    x = startX;
                    y += lineSpacing;
                }
            }
            this.setPreferredSize(new Dimension(getWidth(), y + 20));
        }
    }
}
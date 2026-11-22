package net.sf.rails.ui.swing.elements;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.*;
import javax.swing.border.Border;
import net.sf.rails.game.PublicCompany;
import rails.game.action.StartCompany;

/**
 * Extended dialog for opening companies with a multiples-grid.
 * Displays costs for various share percentages in a formatted table.
 */
public class StartPriceGridDialog extends RadioButtonDialog {

    private static final long serialVersionUID = 1L;
    private StartCompany action;
    
    // High Contrast & UI Colors
    private final Color COLOR_AFFORDABLE = new Color(0, 180, 0);   // Vibrant, prominent Green
    private final Color COLOR_UNAFFORDABLE = Color.BLACK;          // Pure Black
    private final Color COLOR_FLOAT_BG = new Color(255, 250, 205); // Pale Lemon Chiffon
    private final Color COLOR_GRID = new Color(200, 200, 200);     // Table grid lines

    public StartPriceGridDialog(String key, DialogOwner owner, JFrame window, String title, String message,
                                String[] options, int selectedOption, StartCompany action) {
        super(key, owner, window, title, message, options, selectedOption);
        
        this.action = action;
        
        optionsPane.removeAll();
        optionsPane.setBackground(Color.WHITE);
        optionsPane.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        initializeInput();
        this.pack();
    }

    @Override
    protected void initializeInput() {
        if (this.action == null) {
            super.initializeInput();
            return;
        }

        choiceButtons = new JRadioButton[numOptions];
        group = new ButtonGroup();

        int[] prices = action.getStartPrices();
        int cash = action.getPlayerCash();
        int floatPct = action.getFloatPercentage();
        int unit = action.getShareUnit();
        PublicCompany comp = action.getCompany();

        // --- 1. HEADER AREA (Logo, Action Text, Cash) ---
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JLabel logoLabel = new JLabel(new CompanyLogoIcon(comp, 42)); // Bigger logo
        headerPanel.add(logoLabel, BorderLayout.WEST);

        String pName = action.getPlayerName();
        if (pName == null || pName.isEmpty()) pName = "Player";
        JLabel titleLabel = new JLabel(pName + " opens '" + comp.getId() + "'");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JLabel cashLabel = new JLabel("Cash: " + cash);
        cashLabel.setFont(cashLabel.getFont().deriveFont(Font.BOLD, 18f));
        cashLabel.setForeground(COLOR_AFFORDABLE);
        headerPanel.add(cashLabel, BorderLayout.EAST);

        GridBagConstraints headerGbc = constraints(0, 0);
        headerGbc.gridwidth = 11; 
        optionsPane.add(headerPanel, headerGbc);


        // --- 2. TABLE BORDERS & PADDING ---
        Border cellBorder = BorderFactory.createMatteBorder(0, 0, 1, 1, COLOR_GRID);
        Border topCellBorder = BorderFactory.createMatteBorder(1, 0, 1, 1, COLOR_GRID);
        Border leftCellBorder = BorderFactory.createMatteBorder(0, 1, 1, 1, COLOR_GRID);
        Border topLeftBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, COLOR_GRID);
        Border padding = BorderFactory.createEmptyBorder(8, 14, 8, 14);


        // --- 3. ROW 1: PERCENTAGE HEADERS ---
        JLabel cornerLabel = new JLabel("");
        cornerLabel.setBorder(topLeftBorder);
        optionsPane.add(cornerLabel, constraints(0, 1));

        int headerCol = 1;
        for (int pct = unit * 2; pct <= 100; pct += unit) {
            JLabel header = new JLabel(pct + "%");
            header.setFont(header.getFont().deriveFont(Font.BOLD, 16f)); // Bigger Font
            header.setHorizontalAlignment(SwingConstants.CENTER);
            header.setBorder(BorderFactory.createCompoundBorder(topCellBorder, padding));
            optionsPane.add(header, constraints(headerCol++, 1));
        }


        // --- 4. DATA ROWS (Start Prices & Multiples) ---
        for (int i = 0; i < numOptions; i++) {
            int row = i + 2;
            
            // Radio Button Column (clean numeric string extraction)
            String basePriceStr = options[i].split("\\s+")[0];
            choiceButtons[i] = new JRadioButton(basePriceStr, i == selectedOption);
            choiceButtons[i].setBackground(Color.WHITE);
            choiceButtons[i].setFont(choiceButtons[i].getFont().deriveFont(Font.BOLD, 16f)); // Bigger Font
            choiceButtons[i].setHorizontalAlignment(SwingConstants.CENTER);
            group.add(choiceButtons[i]);

            JPanel radioWrapper = new JPanel(new BorderLayout());
            radioWrapper.setBackground(Color.WHITE);
            radioWrapper.setBorder(BorderFactory.createCompoundBorder(leftCellBorder, padding));
            radioWrapper.add(choiceButtons[i], BorderLayout.CENTER);
            optionsPane.add(radioWrapper, constraints(0, row));

            // Multiples Columns
            int basePrice = prices[i];
            int col = 1;
            
            for (int pct = unit * 2; pct <= 100; pct += unit) {
                int numShares = pct / unit;
                int totalCost = basePrice * numShares;

                JLabel costLabel = new JLabel(String.valueOf(totalCost));
                costLabel.setFont(costLabel.getFont().deriveFont(Font.BOLD, 16f)); // Bigger Font
                costLabel.setHorizontalAlignment(SwingConstants.CENTER);
                costLabel.setOpaque(true);
                costLabel.setBackground(Color.WHITE);
                
                if (totalCost <= cash) {
                    costLabel.setForeground(COLOR_AFFORDABLE);
                } else {
                    costLabel.setForeground(COLOR_UNAFFORDABLE);
                    // No setEnabled(false) to preserve pure black text
                }

                Border baseBorder = cellBorder;
                if (pct == floatPct) {
                    costLabel.setBackground(COLOR_FLOAT_BG);
                    baseBorder = BorderFactory.createCompoundBorder(cellBorder, BorderFactory.createLineBorder(Color.ORANGE, 2));
                }

                costLabel.setBorder(BorderFactory.createCompoundBorder(baseBorder, padding));
                optionsPane.add(costLabel, constraints(col++, row));
            }
        }
    }

    private GridBagConstraints constraints(int x, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        return gbc;
    }

    /**
     * Inner class to draw the company token logo as an icon.
     */
    private static class CompanyLogoIcon implements Icon {
        private final PublicCompany company;
        private final int size;

        public CompanyLogoIcon(PublicCompany company, int size) {
            this.company = company;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(company.getBgColour());
            g2.fillOval(x, y, size, size);

            g2.setColor(Color.BLACK);
            g2.drawOval(x, y, size, size);

            g2.setColor(company.getFgColour());
            g2.setFont(c.getFont().deriveFont(Font.BOLD, size * 0.45f));
            FontMetrics fm = g2.getFontMetrics();
            String label = company.getId();
            
            int tx = x + (size - fm.stringWidth(label)) / 2;
            int ty = y + (size - fm.getAscent()) / 2 + fm.getAscent();
            
            g2.drawString(label, tx, ty);
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }
}
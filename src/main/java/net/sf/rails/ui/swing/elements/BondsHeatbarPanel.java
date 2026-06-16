package net.sf.rails.ui.swing.elements;

import javax.swing.*;
import java.awt.*;

public class BondsHeatbarPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private int totalLoansTaken = 0;
    private static final int TOTAL_TIERS = 14;
    private static final int LOANS_PER_TIER = 5;

    public BondsHeatbarPanel() {
        setOpaque(false);
        // Minimum height to ensure text and dots fit comfortably
        setPreferredSize(new Dimension(400, 40)); 
    }

    public void setTotalLoansTaken(int loansTaken) {
        this.totalLoansTaken = Math.max(0, Math.min(loansTaken, TOTAL_TIERS * LOANS_PER_TIER));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int tierWidth = width / TOTAL_TIERS;

        // The active rate is the tier of the first available loan
        int activeTierIndex = totalLoansTaken / LOANS_PER_TIER;
        if (activeTierIndex >= TOTAL_TIERS) {
            activeTierIndex = TOTAL_TIERS - 1; // Cap at max if all 70 are taken
        }

        Font font = new Font("SansSerif", Font.BOLD, 12);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i < TOTAL_TIERS; i++) {
            int x = i * tierWidth;
            int price = (i + 1) * 5;

            // Highlight active tier
            if (i == activeTierIndex) {
                g2.setColor(new Color(255, 255, 153)); // Pale yellow highlight
                g2.fillRect(x, 0, tierWidth, height);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, 0, tierWidth, height);
            }

            // Draw Price Text
            g2.setColor(Color.BLACK);
            String priceStr = "$" + price;
            int textX = x + (tierWidth - fm.stringWidth(priceStr)) / 2;
            int textY = 15; // Fixed y offset for top alignment
            g2.drawString(priceStr, textX, textY);

            // Draw Dots
            int dotSize = Math.min(6, tierWidth / 8); // Scale dots but keep them small
            int dotSpacing = dotSize + 2;
            int totalDotsWidth = (LOANS_PER_TIER * dotSize) + ((LOANS_PER_TIER - 1) * 2);
            int startX = x + (tierWidth - totalDotsWidth) / 2;
            int dotY = textY + 8;

            for (int j = 0; j < LOANS_PER_TIER; j++) {
                int loanIndex = (i * LOANS_PER_TIER) + j;
                int dotX = startX + (j * dotSpacing);

                if (loanIndex < totalLoansTaken) {
                    // Loan taken - empty gray outline
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.drawOval(dotX, dotY, dotSize, dotSize);
                } else {
                    // Loan available - solid red dot
                    g2.setColor(Color.RED);
                    g2.fillOval(dotX, dotY, dotSize, dotSize);
                    g2.setColor(Color.BLACK);
                    g2.drawOval(dotX, dotY, dotSize, dotSize);
                }
            }
        }
        g2.dispose();
    }
}
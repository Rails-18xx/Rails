package net.sf.rails.ui.swing;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.StockMarket;
import net.sf.rails.game.financial.StockSpace;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Renders the Stock Market as a hexagonal grid as required by 1837.
 */
public class StockMarketHexPanel extends JPanel {

    private final StockMarket stockMarket;
    private final int HEX_RADIUS = 30; 
    private final int X_OFFSET = 40;
    private final int Y_OFFSET = 40;

    // Hardcoded colors to match the 1837 screenshot rules
    private static final Color COLOR_YELLOW = new Color(255, 235, 59);
    private static final Color COLOR_PINK = new Color(255, 182, 193); 
    private static final Color COLOR_WHITE = Color.WHITE;
    private static final Color COLOR_LEDGE = Color.BLACK;

    public StockMarketHexPanel(StockMarket stockMarket) {
        this.stockMarket = stockMarket;
        this.setBackground(new Color(220, 220, 220)); 
        this.setPreferredSize(new Dimension(1000, 800)); 
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int rows = stockMarket.getNumberOfRows();
        int cols = stockMarket.getNumberOfColumns();

        for (int r = 0; r <= rows; r++) {     // Iterate safely through possible coordinate space
            for (int c = 0; c <= cols; c++) {
                StockSpace space = stockMarket.getStockSpace(r, c);
                if (space != null) {
                    drawHexSpace(g2, space, r, c);
                }
            }
        }
    }

    private void drawHexSpace(Graphics2D g2, StockSpace space, int row, int col) {
        // Standard Flat-Topped Hex Grid Math
        double width = HEX_RADIUS * 2;
        double height = Math.sqrt(3) * HEX_RADIUS;
        
        // Horizontal spacing is 3/4 of width
        double x = X_OFFSET + (col * (width * 0.75));
        // Vertical spacing is full height
        double y = Y_OFFSET + (row * height);
        
        // Offset odd columns vertically by half height
        if (col % 2 != 0) {
            y += height / 2.0;
        }

        // 1. Create Polygon
        Polygon hex = createHexagon((int)x, (int)y, HEX_RADIUS);

        // 2. Determine Background Color
        Color fill = COLOR_WHITE;
        // Logic to determine color based on price/type
        if (space.getId().startsWith("P")) { 
            fill = COLOR_PINK; 
        } else if (space.getPrice() > 0 && space.getPrice() <= 60 && !space.getId().startsWith("P")) { 
            // Rough heuristic for yellow zone based on 1837 rules
            fill = COLOR_YELLOW; 
        }

        // 3. Draw Hex Background
        g2.setColor(fill);
        g2.fill(hex);
        g2.setColor(Color.GRAY);
        g2.setStroke(new BasicStroke(1));
        g2.draw(hex);

        // 4. Draw Ledges (Thick Borders)
        g2.setColor(COLOR_LEDGE);
        g2.setStroke(new BasicStroke(3));
        
        // Note: In flat-topped hexes:
        // Point 0: Right-Top, 1: Top, 2: Left-Top, 3: Left-Bottom, 4: Bottom, 5: Right-Bottom
        if (space.isBelowLedge()) {
            // Draw bottom edges (indices 3-4 and 4-5)
            drawLine(g2, hex, 3, 4);
            drawLine(g2, hex, 4, 5);
        }
        if (space.isLeftOfLedge()) {
            // Draw left edges (indices 2-3)
            drawLine(g2, hex, 2, 3);
        }

        // 5. Draw Price Text
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        String priceText = String.valueOf(space.getPrice());
        FontMetrics fm = g2.getFontMetrics();
        int txtX = (int)x - (fm.stringWidth(priceText) / 2);
        int txtY = (int)y + (fm.getAscent() / 3);
        g2.drawString(priceText, txtX, txtY);

        // 6. Draw Company Tokens
        drawTokens(g2, space, x, y);
    }
    
    private void drawLine(Graphics2D g2, Polygon p, int idx1, int idx2) {
        g2.drawLine(p.xpoints[idx1], p.ypoints[idx1], p.xpoints[idx2], p.ypoints[idx2]);
    }

    private void drawTokens(Graphics2D g2, StockSpace space, double x, double y) {
        List<PublicCompany> tokens = space.getTokens();
        if (tokens == null || tokens.isEmpty()) return;

        int tokenSize = (int)(HEX_RADIUS * 0.8);
        int count = 0;
        
        for (PublicCompany comp : tokens) {
            // Stack effect
            int tx = (int)x - (tokenSize/2) + (count * 3);
            int ty = (int)y - (tokenSize/2) - (count * 3); 
            
            // FIX: Use British spelling 'Colour' which is standard in Rails
            g2.setColor(comp.getFgColour()); 
            g2.fillOval(tx, ty, tokenSize, tokenSize);
            
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1));
            g2.drawOval(tx, ty, tokenSize, tokenSize);
            
            // Draw ID
            g2.setColor(comp.getBgColour()); 
            String id = comp.getId();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(id, tx + (tokenSize - fm.stringWidth(id))/2, ty + (tokenSize + fm.getAscent())/2 - 2);
            
            count++;
        }
    }

    /**
     * Helper to generate a flat-topped hexagon.
     * Order of points: Right-Top, Top, Left-Top, Left-Bottom, Bottom, Right-Bottom
     */
    private Polygon createHexagon(int x, int y, int radius) {
        Polygon p = new Polygon();
        for (int i = 0; i < 6; i++) {
            // 0 degrees is Right.
            // Flat-topped starts at -30 deg (or 330), but simpler is:
            // 0, 60, 120... is Pointy Topped.
            // 30, 90, 150... is Flat Topped.
            double angle_deg = 30 + (60 * i); 
            double angle_rad = Math.toRadians(angle_deg);
            int px = (int) (x + radius * Math.cos(angle_rad));
            int py = (int) (y + radius * Math.sin(angle_rad));
            p.addPoint(px, py);
        }
        return p;
    }
}
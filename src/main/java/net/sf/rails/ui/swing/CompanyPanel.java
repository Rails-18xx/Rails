package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

import javax.swing.*;

import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Train;
import net.sf.rails.game.financial.Certificate;
import net.sf.rails.ui.swing.elements.RailCard;

public class CompanyPanel extends JPanel {

    private final PublicCompany company;
    private final GameUIManager gameUIManager;

    private JLabel priceLabel;
    private JLabel cashLabel;
    private JLabel revenueLabel;
    private JPanel portfolioArea;
    private JPanel marketArea;
    
    private int lastCash = Integer.MIN_VALUE;

    public CompanyPanel(PublicCompany company, GameUIManager gameUIManager) {
        this.company = company;
        this.gameUIManager = gameUIManager;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        setBackground(new Color(245, 245, 250));

        initUI();
    }

    private void initUI() {
        // ROW 1: HEADER & STATS
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row1.setOpaque(false);
        
        JLabel idLabel = new JLabel(" " + company.getId() + " ", SwingConstants.CENTER);
        idLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        idLabel.setOpaque(true);
        idLabel.setBackground(company.getBgColour() != null ? company.getBgColour() : Color.LIGHT_GRAY);
        idLabel.setForeground(company.getFgColour() != null ? company.getFgColour() : Color.BLACK);
        idLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        idLabel.setPreferredSize(new Dimension(55, 20));
        
        priceLabel = new JLabel();
        priceLabel.setFont(GameStatus_Alt.FONT_NUMBERS);
        priceLabel.setPreferredSize(new Dimension(50, 20));
        
        cashLabel = new JLabel();
        cashLabel.setFont(GameStatus_Alt.FONT_CURRENCY);
        cashLabel.setPreferredSize(new Dimension(140, 20));

        revenueLabel = new JLabel();
        revenueLabel.setFont(GameStatus_Alt.FONT_CURRENCY);
        revenueLabel.setPreferredSize(new Dimension(160, 20));

        row1.add(idLabel);
        row1.add(priceLabel);
        row1.add(cashLabel);
        row1.add(revenueLabel);
        
        // Tokens
        JPanel tokenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        tokenPanel.setOpaque(false);
        tokenPanel.setPreferredSize(new Dimension(80, 20));
        int availableCount = 0;
        try {
            if (company.getAllBaseTokens() != null) {
                for (net.sf.rails.game.BaseToken token : company.getAllBaseTokens()) {
                    if (!token.isPlaced()) availableCount++;
                }
            }
        } catch (Exception e) {}
        for(int i=0; i<Math.min(availableCount,4); i++) {
            JLabel tl = new JLabel(new SmallTokenIcon(company, company.getId(), 14));
            tokenPanel.add(tl);
        }
        row1.add(tokenPanel);
        
        // Hex
        String destId = "";
        try {
            if (company.getDestinationHex() != null) destId = company.getDestinationHex().getId();
            else if (company.getHomeHexes() != null && !company.getHomeHexes().isEmpty()) destId = company.getHomeHexes().get(0).getId();
        } catch (Exception e) {}
        if(!destId.isEmpty()) {
            JLabel hexLabel = new JLabel(new DestinationHexIcon(destId, company.hasReachedDestination(), 16));
            hexLabel.setPreferredSize(new Dimension(20, 20));
            row1.add(hexLabel);
        }

        add(row1);
        add(Box.createVerticalStrut(5));

        // ROW 2: PORTFOLIO (Treasury & Privates)
        portfolioArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        portfolioArea.setOpaque(false);
        add(portfolioArea);
        add(Box.createVerticalStrut(5));

        // ROW 3: OWNERSHIP & MARKET
        marketArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        marketArea.setOpaque(true);
        marketArea.setBackground(new Color(235, 235, 240));
        marketArea.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        add(marketArea);
    }

    public void refresh() {
        boolean isActive = false;
        try {
            Object round = gameUIManager.getGameManager().getCurrentRound();
            if (round instanceof net.sf.rails.game.OperatingRound) {
                isActive = company.equals(((net.sf.rails.game.OperatingRound) round).getOperatingCompany());
            }
        } catch (Exception e) {}

        if (isActive) {
            setBackground(PlayerPanel.COLOR_ACTIVE_BG);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PlayerPanel.COLOR_ACTIVE_BORDER, 2),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)
            ));
        } else {
            setBackground(PlayerPanel.COLOR_INACTIVE_BG);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PlayerPanel.COLOR_INACTIVE_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
            ));
        }

        int price = (company.getCurrentSpace() != null) ? company.getCurrentSpace().getPrice() : (company.getStartSpace() != null ? company.getStartSpace().getPrice() : 0);
        priceLabel.setText(company.hasStockPrice() ? gameUIManager.format(price) : "Minor");

        int currentCash = company.getCash();
        if (lastCash == Integer.MIN_VALUE) {
            lastCash = currentCash;
            cashLabel.setText("Treasury: " + gameUIManager.format(currentCash));
        } else if (currentCash != lastCash) {
            if (cashLabel.isShowing()) {
                JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
                if (frame != null) new PlayerPanel.MoneySpinnerAnimator(frame, cashLabel, lastCash, currentCash, gameUIManager).start();
            } else {
                cashLabel.setText("Treasury: " + gameUIManager.format(currentCash));
            }
            lastCash = currentCash;
        }

        // Update Revenue / Retained
        int rev = company.getDividendRevenue();
        int ret = company.getRetainedRevenue();
        revenueLabel.setText("Rev: " + gameUIManager.format(rev) + " (Ret: " + gameUIManager.format(ret) + ")");

        portfolioArea.removeAll();
        for (PrivateCompany pc : company.getPortfolioModel().getPrivateCompanies()) {
            RailCard card = new RailCard(pc, new ButtonGroup());
            card.setCustomLabel(pc.getId()); card.setCompactMode(true);
            portfolioArea.add(card);
        }
        for (net.sf.rails.game.financial.PublicCertificate cert : company.getPortfolioModel().getCertificates()) {
            RailCard card = new RailCard(cert, new ButtonGroup());
            card.setCompactMode(true);
            portfolioArea.add(card);
        }

        marketArea.removeAll();
        if (company.hasStockPrice()) {
            int ipoPct = gameUIManager.getRoot().getBank().getIpo().getPortfolioModel().getShare(company);
            int poolPct = gameUIManager.getRoot().getBank().getPool().getPortfolioModel().getShare(company);
            if (ipoPct > 0) {
                JLabel l = new JLabel("IPO: " + ipoPct + "%");
                l.setFont(GameStatus_Alt.FONT_SHARES);
                marketArea.add(l);
            }
            if (poolPct > 0) {
                JLabel l = new JLabel("Pool: " + poolPct + "%");
                l.setFont(GameStatus_Alt.FONT_SHARES);
                marketArea.add(l);
            }
            
            Player president = company.getPresident();
            if (president != null && president.getPortfolioModel().getShare(company) > 0) {
                JLabel l = new JLabel(president.getName() + " (" + president.getPortfolioModel().getShare(company) + "%)");
                l.setFont(GameStatus_Alt.FONT_SHARES); // Bold for president
                marketArea.add(l);
            }

            for (Player p : gameUIManager.getPlayerManager().getPlayers()) {
                if (p.equals(president)) continue;
                int pct = p.getPortfolioModel().getShare(company);
                if (pct > 0) {
                    JLabel l = new JLabel(p.getName() + " (" + pct + "%)");
                    l.setFont(GameStatus_Alt.FONT_NUMBERS);
                    marketArea.add(l);
                }
                if (p.hasSoldThisRound(company)) {
                    JLabel soldLabel = new JLabel("🔴 " + p.getName());
                    soldLabel.setForeground(new Color(180, 0, 0));
                    soldLabel.setFont(GameStatus_Alt.FONT_NUMBERS);
                    soldLabel.setToolTipText("Sold this round.");
                    marketArea.add(soldLabel);
                }
            }
        }
        
        revalidate();
        repaint();
    }

    private static class SmallTokenIcon implements Icon {
        private final PublicCompany company;
        private final String label;
        private final int size;
        public SmallTokenIcon(PublicCompany company, String label, int size) { this.company = company; this.label = label; this.size = size; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(company.getBgColour()); g2.fillOval(x, y, size, size);
            g2.setColor(Color.BLACK); g2.drawOval(x, y, size, size);
            if (label != null) {
                g2.setColor(company.getFgColour()); g2.setFont(c.getFont().deriveFont(Font.BOLD, size * 0.7f));
                FontMetrics fm = g2.getFontMetrics(); g2.drawString(label, x + (size - fm.stringWidth(label))/2, y + (size - fm.getAscent())/2 + fm.getAscent());
            }
            g2.dispose();
        }
        public int getIconWidth() { return size; } public int getIconHeight() { return size; }
    }

    private static class DestinationHexIcon implements Icon {
        private final String text; private final boolean connected; private final int size;
        public DestinationHexIcon(String text, boolean connected, int size) { this.text = text; this.connected = connected; this.size = size; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = (int) (size * 1.15), height = size;
            int[] xP = {x + width/4, x + width*3/4, x + width, x + width*3/4, x + width/4, x};
            int[] yP = {y, y, y + height/2, y + height, y + height, y + height/2};
            Polygon hex = new Polygon(xP, yP, 6);
            if (connected) {
                g2.setColor(new Color(34, 139, 34)); g2.fillPolygon(hex);
                g2.setColor(Color.WHITE); g2.setFont(new Font("Dialog", Font.BOLD, (int)(size * 0.8)));
                g2.drawString("\u2713", x + (width - g2.getFontMetrics().stringWidth("\u2713"))/2, y + ((height - g2.getFontMetrics().getHeight())/2) + g2.getFontMetrics().getAscent());
            } else {
                g2.setColor(new Color(255, 215, 0)); g2.fillPolygon(hex);
                g2.setColor(Color.BLACK); g2.setStroke(new BasicStroke(1.5f)); g2.drawPolygon(hex);
                g2.setFont(c.getFont().deriveFont(Font.BOLD, size * 0.45f));
                g2.drawString(text, x + (width - g2.getFontMetrics().stringWidth(text))/2, y + ((height - g2.getFontMetrics().getHeight())/2) + g2.getFontMetrics().getAscent());
            }
            g2.dispose();
        }
        public int getIconWidth() { return (int) (size * 1.15); } public int getIconHeight() { return size; }
    }
}
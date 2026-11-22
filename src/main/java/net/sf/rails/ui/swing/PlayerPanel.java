package net.sf.rails.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.ui.swing.elements.RailCard;
import rails.game.action.PossibleAction;
import rails.game.action.SellShares;

public class PlayerPanel extends JPanel {

    private final Player player;
    private final GameUIManager gameUIManager;

    private JLabel nameLabel;
    private JLabel cashLabel;
    private JLabel certsLabel;
    private JLabel timeLabel;
    private JLabel worthLabel;
    private JPanel cardArea;
    private JLabel pdLabel;
    private int lastCash = Integer.MIN_VALUE;

    public static final Color COLOR_ACTIVE_BG = Color.WHITE;
    public static final Color COLOR_INACTIVE_BG = new Color(245, 245, 250);
    public static final Color COLOR_ACTIVE_BORDER = new Color(100, 150, 255);
    public static final Color COLOR_INACTIVE_BORDER = new Color(200, 200, 200);

    public boolean showWorth = false;

    public PlayerPanel(Player player, GameUIManager gameUIManager) {
        this.player = player;
        this.gameUIManager = gameUIManager;

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INACTIVE_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        setBackground(COLOR_INACTIVE_BG);

        initUI();
    }

    private void initUI() {
        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
        headerPanel.setBackground(new Color(225, 230, 240));
        headerPanel.setOpaque(true);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel nameRow = new JPanel(new BorderLayout());
        nameRow.setOpaque(false);

        nameLabel = new JLabel();
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        nameLabel.setForeground(new Color(20, 20, 20));

        pdLabel = new JLabel("[🚂]");
        pdLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        pdLabel.setForeground(new Color(200, 50, 50));
        pdLabel.setVisible(false);

        nameRow.add(nameLabel, BorderLayout.WEST);
        nameRow.add(pdLabel, BorderLayout.EAST);

        JPanel statsPanel = new JPanel(new java.awt.GridLayout(2, 2, 4, 2));
        statsPanel.setOpaque(false);

        Color statColor = new Color(50, 50, 50);

        cashLabel = new JLabel();
        cashLabel.setFont(GameStatus_Alt.FONT_CURRENCY);
        cashLabel.setForeground(statColor);
        certsLabel = new JLabel();
        certsLabel.setFont(GameStatus_Alt.FONT_CURRENCY);
        certsLabel.setForeground(statColor);
        timeLabel = new JLabel();
        timeLabel.setFont(GameStatus_Alt.FONT_CURRENCY);
        timeLabel.setForeground(statColor);
        worthLabel = new JLabel();
        worthLabel.setFont(GameStatus_Alt.FONT_CURRENCY);
        worthLabel.setForeground(statColor);

        statsPanel.add(cashLabel);
        statsPanel.add(certsLabel);
        statsPanel.add(timeLabel);
        statsPanel.add(worthLabel);

        headerPanel.add(nameRow, BorderLayout.NORTH);
        headerPanel.add(statsPanel, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        cardArea = new JPanel();
        cardArea.setLayout(new BoxLayout(cardArea, BoxLayout.Y_AXIS));
        cardArea.setOpaque(false);
        add(cardArea, BorderLayout.CENTER);
    }

    public void setActive(boolean isActive) {
        Color bg = isActive ? COLOR_ACTIVE_BG : COLOR_INACTIVE_BG;
        setBackground(bg);

        Component[] comps = getComponents();
        if (comps.length > 0 && comps[0] instanceof JPanel) {
            comps[0].setBackground(isActive ? new Color(240, 248, 255) : new Color(225, 230, 240));
        }
    }

    public void refresh() {
        nameLabel.setText(player.getName());

        int currentCash = player.getCashValue();
        if (lastCash == Integer.MIN_VALUE) {
            lastCash = currentCash;
            cashLabel.setText("Cash: " + gameUIManager.format(currentCash));
        } else if (currentCash != lastCash) {
            triggerMoneySpinner(cashLabel, lastCash, currentCash);
            lastCash = currentCash;
        }

        int certs = (int) player.getPortfolioModel().getCertificateCount();
        int limit = gameUIManager.getGameManager().getPlayerCertificateLimit(player);
        int time = player.getTimeBankModel().value();
        String timeStr = String.format("%02d:%02d", Math.abs(time) / 60, Math.abs(time) % 60);

        int fixedInc = 0;
        try {
            net.sf.rails.game.Phase phase = gameUIManager.getRoot().getPhaseManager().getCurrentPhase();
            for (net.sf.rails.game.PrivateCompany pc : player.getPortfolioModel().getPrivateCompanies()) {
                int r = pc.getRevenueByPhase(phase);
                if (r == 0 && pc.getRevenue() != null && !pc.getRevenue().isEmpty()) {
                    r = pc.getRevenue().get(0);
                }
                fixedInc += r;
            }
        } catch (Exception e) {
        }

        certsLabel.setText("Certs: " + certs + "/" + limit);
        timeLabel.setText("Time: " + (time < 0 ? "-" : "") + timeStr);
        worthLabel.setText("Inc: " + gameUIManager.format(fixedInc));

        layoutCards();
    }

    public void setPriorityDeal(boolean hasPriorityDeal) {
        pdLabel.setVisible(hasPriorityDeal);
    }

    private void layoutCards() {
        cardArea.removeAll();

        List<PossibleAction> actions = null;
        if (gameUIManager.getGameManager().getPossibleActions() != null) {
            actions = gameUIManager.getGameManager().getPossibleActions().getList();
        }

        // 1. PUBLIC COMPANIES
        List<PublicCompany> allCompanies = gameUIManager.getAllPublicCompanies();
        for (PublicCompany company : allCompanies) {
            if (company.isClosed())
                continue;

            int sharePercentage = player.getPortfolioModel().getShare(company);
            if (sharePercentage == 0)
                continue;

            java.util.List<net.sf.rails.game.financial.PublicCertificate> certs = new java.util.ArrayList<>();
            for (net.sf.rails.game.financial.PublicCertificate pubCert : player.getPortfolioModel().getCertificates()) {
                if (company.equals(pubCert.getCompany()))
                    certs.add(pubCert);
            }

            if (certs.isEmpty())
                continue;

            boolean isPresident = player.equals(company.getPresident());

            JPanel holdingPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 2));
            holdingPanel.setOpaque(false);
            holdingPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // 1. Company ID Badge
            JLabel idLabel = new JLabel(" " + company.getId() + " ");
            idLabel.setFont(GameStatus_Alt.FONT_SHARES);
            idLabel.setOpaque(true);
            idLabel.setBackground(company.getBgColour() != null ? company.getBgColour() : Color.LIGHT_GRAY);
            idLabel.setForeground(company.getFgColour() != null ? company.getFgColour() : Color.BLACK);
            idLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            holdingPanel.add(idLabel);

            boolean isActivePlayer = false;
            if (gameUIManager.getGameManager().getCurrentPlayer() != null) {
                isActivePlayer = player.equals(gameUIManager.getGameManager().getCurrentPlayer());
            }

            // 2. Group Certificates by share % and presidency
            certs.sort((c1, c2) -> {
                if (c1.isPresidentShare() != c2.isPresidentShare())
                    return c1.isPresidentShare() ? -1 : 1;
                return Integer.compare(c2.getShare(), c1.getShare());
            });

            java.util.List<java.util.List<net.sf.rails.game.financial.PublicCertificate>> groupedCerts = new java.util.ArrayList<>();
            java.util.List<net.sf.rails.game.financial.PublicCertificate> currentGroup = new java.util.ArrayList<>();

            for (net.sf.rails.game.financial.PublicCertificate cert : certs) {
                if (currentGroup.isEmpty()) {
                    currentGroup.add(cert);
                } else {
                    net.sf.rails.game.financial.PublicCertificate prev = currentGroup.get(0);
                    if (cert.getShare() == prev.getShare() && cert.isPresidentShare() == prev.isPresidentShare()) {
                        currentGroup.add(cert);
                    } else {
                        groupedCerts.add(currentGroup);
                        currentGroup = new java.util.ArrayList<>();
                        currentGroup.add(cert);
                    }
                }
            }
            if (!currentGroup.isEmpty()) {
                groupedCerts.add(currentGroup);
            }

            // Render each stacked group dynamically
            for (java.util.List<net.sf.rails.game.financial.PublicCertificate> group : groupedCerts) {
                CardStackPanel currentStack = new CardStackPanel();
                holdingPanel.add(currentStack);

                for (int i = 0; i < group.size(); i++) {
                    net.sf.rails.game.financial.PublicCertificate cert = group.get(i);
                    boolean isCertPrez = cert.isPresidentShare();
                    RailCard card = new RailCard(cert, new ButtonGroup());

// Implement high-contrast paper cards with company-colored borders
                    card.setBackground(new Color(255, 255, 250)); // Neutral certificate paper
                    card.setForeground(new Color(20, 20, 20)); // High contrast text
                    if (company.getBgColour() != null) {
                        card.setBorder(BorderFactory.createLineBorder(company.getBgColour(), 2));
                    } else {
                        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                    }

                    // Percentage printed on all, but "P" only on the top card
                    if (i == group.size() - 1) {
                        card.setCustomLabel(cert.getShare() + "%" + (isCertPrez ? " P" : ""));
                    } else {
                        card.setCustomLabel(cert.getShare() + "%");
                    }
                    card.setCompactMode(true);

                    if (actions != null && isActivePlayer) {
                        for (PossibleAction pa : actions) {
                            if (pa instanceof SellShares && ((SellShares) pa).getCompany().equals(company)) {
                                card.addPossibleAction(pa);
                                card.setBorder(BorderFactory.createLineBorder(new Color(220, 20, 60), 3));
                                card.setEnabled(true);
                                break;
                            }
                        }
                    }

                    if (getParent() != null && getParent().getParent() instanceof ActionListener) {
                        card.addActionListener((ActionListener) getParent().getParent());
                    }

                    currentStack.add(card);
                }
            }

// 3. Distinct Total Block
            JLabel summaryLabel = new JLabel(" " + sharePercentage + "%" + (isPresident ? " P " : " "));
            summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            summaryLabel.setOpaque(true);
            summaryLabel.setBackground(new Color(235, 235, 240)); // Distinct aggregate background
            summaryLabel.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1));
            
            // Push summary slightly right for visual separation
            holdingPanel.add(Box.createHorizontalStrut(5));
            holdingPanel.add(summaryLabel);

            cardArea.add(holdingPanel);
        }

        // 2. PRIVATES
        java.util.Collection<net.sf.rails.game.PrivateCompany> privates = player.getPortfolioModel()
                .getPrivateCompanies();
        if (!privates.isEmpty()) {
            JLabel privLabel = new JLabel("Privates");
            privLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            privLabel.setForeground(Color.GRAY);
            privLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            privLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 2, 0));
            cardArea.add(privLabel);

            JPanel privPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 2));
            privPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            privPanel.setOpaque(false);

            for (net.sf.rails.game.PrivateCompany pc : privates) {
                RailCard card = new RailCard(pc, new ButtonGroup());
                card.setCompany(pc);
                card.setCustomLabel(pc.getId());
                card.setPrivateCompanyTooltip(pc);
                privPanel.add(card);
            }
            cardArea.add(privPanel);
        }

        cardArea.revalidate();
        cardArea.repaint();
    }

    private void triggerMoneySpinner(JComponent target, int oldVal, int newVal) {
        if (!target.isShowing()) {
            cashLabel.setText("Cash: " + gameUIManager.format(newVal));
            return;
        }
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null)
            new MoneySpinnerAnimator(frame, target, oldVal, newVal, gameUIManager).start();
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * DYNAMIC LAYOUT ENGINE FOR OVERLAPPING CARDS
     * Eliminates pixel hardcoding by using the component's font-derived bounds.
     */
    private static class CardStackPanel extends JPanel {
        public CardStackPanel() {
            setLayout(null); // Absolute positioning is required for overlap math
            setOpaque(false);
        }

        @Override
        public void doLayout() {
            int count = getComponentCount();
            if (count == 0)
                return;

            // Derive scalable bounds from the top card
            Component topCard = getComponent(count - 1);
            Dimension pref = topCard.getPreferredSize();
            int baseW = pref.width > 0 ? pref.width : 50;
            int baseH = pref.height > 0 ? pref.height : 35;

            // Enforce 80% overlap (only 20% visible width for underlying cards)
            int stepX = (int) (baseW * 0.20);
            if (stepX < 2)
                stepX = 2; // Failsafe minimum

            for (int i = 0; i < count; i++) {
                Component c = getComponent(i);
                // Reverse visual order: top card (count-1) gets x=0, bottom gets
                // x=(count-1)*stepX
                int reverseIndex = (count - 1) - i;
                int xPos = reverseIndex * stepX;

                // Add slight vertical asymmetry to distinguish cards
                int yOffset = (reverseIndex % 2 == 1) ? 2 : 0;

                // Full height for all cards in the stack
                c.setBounds(xPos, yOffset, baseW, baseH);

            }
        }

        @Override
        public Dimension getPreferredSize() {
            int count = getComponentCount();
            if (count == 0)
                return new Dimension(0, 0);
            Dimension pref = getComponent(count - 1).getPreferredSize();
            int baseW = pref.width > 0 ? pref.width : 50;
            int baseH = pref.height > 0 ? pref.height : 35;
            int stepX = (int) (baseW * 0.20);
return new Dimension(baseW + (count - 1) * stepX, baseH + 2);
        }

        @Override
        public boolean isOptimizedDrawingEnabled() {
            return false; // Tells Swing to allow components in this panel to overlap
        }
    }

    // UNIVERSAL SPINNER
    public static class MoneySpinnerAnimator {
        private final JFrame frame;
        private final JComponent target;
        private final int startVal, endVal, delta;
        private final GameUIManager uiManager;
        private float progress = 0f;

        public MoneySpinnerAnimator(JFrame frame, JComponent target, int startVal, int endVal,
                GameUIManager uiManager) {
            this.frame = frame;
            this.target = target;
            this.startVal = startVal;
            this.endVal = endVal;
            this.delta = endVal - startVal;
            this.uiManager = uiManager;
        }

        public void start() {
            JLayeredPane lp = frame.getLayeredPane();
            Point pt = SwingUtilities.convertPoint(target.getParent(), target.getLocation(), lp);
            java.awt.image.BufferedImage bgImage = new java.awt.image.BufferedImage(target.getWidth(),
                    target.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = bgImage.createGraphics();
            target.paint(g2d);
            g2d.dispose();

            Color origFg = target.getForeground();
            target.setForeground(new Color(0, 0, 0, 0));

            JComponent animLayer = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    double scale = 1.0 + (0.15 * Math.sin(progress * Math.PI));
                    int w = (int) (target.getWidth() * scale), h = (int) (target.getHeight() * scale);
                    int x = pt.x - (w - target.getWidth()) / 2, y = pt.y - (h - target.getHeight()) / 2;

                    g2.setColor(new Color(0, 0, 0, 50));
                    g2.fillRoundRect(x + 4, y + 4, w, h, 6, 6);
                    g2.drawImage(bgImage, x, y, w, h, null);

                    int currentVal = (int) (startVal + delta * progress);
                    String text = ((JLabel) target).getText().replaceAll("\\d+", "") + uiManager.format(currentVal);
                    g2.setFont(target.getFont().deriveFont(Font.BOLD, (float) (target.getFont().getSize() * scale)));
                    g2.setColor(currentVal < 0 ? Color.RED : new Color(0, 0, 128));

                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(text, x + 5, y + (h - fm.getHeight()) / 2 + fm.getAscent());

                    int alpha = Math.max(0, (int) (255 * (1 - progress)));
                    int bubbleY = (int) (y - (25 * progress));
                    String bubbleText = (delta > 0 ? "+" : "") + uiManager.format(delta);
                    int bw = fm.stringWidth(bubbleText) + 16, bh = fm.getHeight() + 8, bx = x + (w - bw) / 2;

                    g2.setColor(delta > 0 ? new Color(34, 139, 34, alpha) : new Color(220, 20, 60, alpha));
                    g2.fillRoundRect(bx, bubbleY, bw, bh, 10, 10);
                    g2.setColor(new Color(255, 255, 255, alpha));
                    g2.drawString(bubbleText, bx + 8, bubbleY + fm.getAscent() + 4);
                    g2.dispose();
                }
            };
            animLayer.setBounds(0, 0, lp.getWidth(), lp.getHeight());
            lp.add(animLayer, JLayeredPane.DRAG_LAYER);

            long startTime = System.currentTimeMillis();
            javax.swing.Timer timer = new javax.swing.Timer(16, null);
            timer.addActionListener(e -> {
                progress = Math.min(1f, (float) (System.currentTimeMillis() - startTime) / 1200f);
                progress = 1.0f - (1.0f - progress) * (1.0f - progress);
                animLayer.repaint();
                if (progress >= 1f) {
                    timer.stop();
                    lp.remove(animLayer);
                    target.setForeground(origFg);
                    ((JLabel) target)
                            .setText(((JLabel) target).getText().replaceAll("\\d+", "") + uiManager.format(endVal));
                    target.repaint();
                    lp.repaint();
                }
            });
            timer.start();
        }
    }
}
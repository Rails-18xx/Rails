package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import net.sf.rails.game.Player;
import net.sf.rails.game.PlayerManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.ui.swing.elements.RailCard;
import rails.game.action.BuyCertificate;
import rails.game.action.BuyPrivate;
import rails.game.action.BuyTrain;
import rails.game.action.PossibleAction;
import rails.game.action.SellShares;

public class GameStatus_Alt extends GameStatus {

    // Layout Components
    private JPanel playerColumn; 
    private JPanel companyColumn; 
    private JPanel marketColumn; 
    private JScrollPane companyScroll;

    // Parameterized Fonts
    public static final Font FONT_CURRENCY = new Font("Monospaced", Font.BOLD, 14);
    public static final Font FONT_SHARES = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONT_TRAINS = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_TIME = new Font("Monospaced", Font.BOLD, 12);
    public static final Font FONT_NUMBERS = new Font("SansSerif", Font.PLAIN, 12);

    private final Map<Player, PlayerPanel> playerRegistry = new HashMap<>();
    private final Map<PublicCompany, CompanyPanel> companyRegistry = new HashMap<>();
    private BankPanel activeBankPanel;

    @Override
    public void init(StatusWindow parent, GameUIManager gameUIManager) {
        this.parent = parent;
        this.gameUIManager = gameUIManager;

        this.setLayout(new BorderLayout(10, 0));
        this.setBackground(new Color(235, 235, 240));
        this.setOpaque(true);

        playerColumn = new JPanel(); playerColumn.setLayout(new BoxLayout(playerColumn, BoxLayout.Y_AXIS)); playerColumn.setOpaque(false);
        JScrollPane pScroll = new JScrollPane(playerColumn); pScroll.setBorder(null); pScroll.setOpaque(false); pScroll.getViewport().setOpaque(false);
        pScroll.setPreferredSize(new Dimension(280, 0)); this.add(pScroll, BorderLayout.WEST);

        companyColumn = new JPanel(); companyColumn.setLayout(new BoxLayout(companyColumn, BoxLayout.Y_AXIS)); companyColumn.setOpaque(false);
        companyScroll = new JScrollPane(companyColumn); companyScroll.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, Color.LIGHT_GRAY));
        companyScroll.setOpaque(false); companyScroll.getViewport().setOpaque(false); companyScroll.getVerticalScrollBar().setUnitIncrement(16);
        this.add(companyScroll, BorderLayout.CENTER);

marketColumn = new JPanel(new BorderLayout()); marketColumn.setOpaque(false);
        marketColumn.setPreferredSize(new Dimension(500, 0)); this.add(marketColumn, BorderLayout.EAST);

        recreate();
    }

    @Override
    public void recreate() {
        playerColumn.removeAll(); companyColumn.removeAll(); marketColumn.removeAll();
        playerRegistry.clear(); companyRegistry.clear();

        renderPlayers(); renderCompanies(); renderMarket();
        this.revalidate(); this.repaint();
    }

    private void renderPlayers() {
        PlayerManager pm = gameUIManager.getPlayerManager();
        int pdIndex = gameUIManager.getPriorityPlayer() != null ? gameUIManager.getPriorityPlayer().getIndex() : -1;
        for (int i = 0; i < pm.getNumberOfPlayers(); i++) {
            Player p = pm.getPlayerByPosition(i);
            PlayerPanel pPanel = new PlayerPanel(p, gameUIManager);
            pPanel.setPriorityDeal(i == pdIndex); pPanel.refresh();
            attachActionListenersRecursively(pPanel);
            playerRegistry.put(p, pPanel); playerColumn.add(pPanel); playerColumn.add(Box.createVerticalStrut(10));
        }
    }

    private void renderCompanies() {
        for (PublicCompany c : gameUIManager.getGameManager().getCompaniesInDisplayOrder(gameUIManager.getAllPublicCompanies())) {
            if (c.isClosed()) continue;
            CompanyPanel cPanel = new CompanyPanel(c, gameUIManager);
            cPanel.refresh(); attachActionListenersRecursively(cPanel);
            companyRegistry.put(c, cPanel); companyColumn.add(cPanel); companyColumn.add(Box.createVerticalStrut(10));
        }
    }

    private void renderMarket() {
        activeBankPanel = new BankPanel(gameUIManager);
        activeBankPanel.refresh(); attachActionListenersRecursively(activeBankPanel);
        marketColumn.add(activeBankPanel, BorderLayout.NORTH);
    }

    @Override
    public void initTurn(int actorIndex, boolean myTurn) {
        for (PlayerPanel pp : playerRegistry.values()) { pp.refresh(); attachActionListenersRecursively(pp); }
        for (CompanyPanel cp : companyRegistry.values()) { cp.refresh(); attachActionListenersRecursively(cp); }
        if (activeBankPanel != null) { activeBankPanel.refresh(); attachActionListenersRecursively(activeBankPanel); }
    }

    private void attachActionListenersRecursively(java.awt.Container container) {
        for (java.awt.Component c : container.getComponents()) {
            if (c instanceof RailCard) {
                RailCard rc = (RailCard) c;
                rc.removeActionListener(this); rc.addActionListener(this);
            } else if (c instanceof java.awt.Container) attachActionListenersRecursively((java.awt.Container) c);
        }
    }

    @Override
    public void actionPerformed(ActionEvent actor) {
        if (!(actor.getSource() instanceof RailCard)) { super.actionPerformed(actor); return; }

        RailCard sourceCard = (RailCard) actor.getSource();
        List<PossibleAction> actions = sourceCard.getPossibleActions();
        if (actions == null || actions.isEmpty()) return;

        PossibleAction chosenAction = actions.get(0); 
        Object destinationEntity = resolveLogicalDestination(chosenAction);

        if (destinationEntity != null) {
            JComponent destPanel = resolveVisualDestination(destinationEntity);
            if (destPanel != null) {
                if (companyScroll != null && destPanel.getParent() == companyColumn) {
                    companyColumn.scrollRectToVisible(destPanel.getBounds());
                }
                new UniversalFlightAnimator(parent, sourceCard, destPanel).execute(() -> parent.process(chosenAction));
                return;
            }
        }
        parent.process(chosenAction);
    }

    private Object resolveLogicalDestination(PossibleAction action) {
        if (action instanceof BuyCertificate) return ((BuyCertificate) action).getPlayer(); 
        else if (action instanceof SellShares) return gameUIManager.getRoot().getBank();
        else if (action instanceof BuyTrain || action instanceof BuyPrivate) return ((net.sf.rails.game.OperatingRound) gameUIManager.getGameManager().getCurrentRound()).getOperatingCompany();
        return null;
    }

    private JComponent resolveVisualDestination(Object entity) {
        if (entity instanceof Player) return playerRegistry.get(entity);
        if (entity instanceof PublicCompany) return companyRegistry.get(entity);
        if (entity instanceof Bank) return activeBankPanel;
        return null;
    }

    private class UniversalFlightAnimator {
        private final JFrame frame; private final JComponent source, destination;
        public UniversalFlightAnimator(JFrame frame, JComponent source, JComponent destination) { this.frame = frame; this.source = source; this.destination = destination; }
        public void execute(Runnable onComplete) {
            if (!source.isShowing() || !destination.isShowing()) { onComplete.run(); return; }
            JLayeredPane lp = frame.getLayeredPane();
            Point startPt = SwingUtilities.convertPoint(source.getParent(), source.getLocation(), lp);
            Point endPt = SwingUtilities.convertPoint(destination.getParent(), destination.getLocation(), lp);

            java.awt.image.BufferedImage ghost = new java.awt.image.BufferedImage(source.getWidth(), source.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = ghost.createGraphics(); source.paint(g2); g2.dispose();

            JComponent animLayer = new JComponent() { Point cur = new Point(startPt); @Override protected void paintComponent(Graphics g) { g.drawImage(ghost, cur.x, cur.y, null); } };
            animLayer.setBounds(0, 0, lp.getWidth(), lp.getHeight()); lp.add(animLayer, JLayeredPane.DRAG_LAYER);

            long startTime = System.currentTimeMillis();
            javax.swing.Timer timer = new javax.swing.Timer(16, e -> {
                float p = Math.min(1f, (float) (System.currentTimeMillis() - startTime) / 300f);
                float ease = 1.0f - (1.0f - p) * (1.0f - p);
                animLayer.setLocation(startPt.x + (int)((endPt.x - startPt.x) * ease), startPt.y + (int)((endPt.y - startPt.y) * ease));
                if (p >= 1f) { ((javax.swing.Timer) e.getSource()).stop(); lp.remove(animLayer); lp.repaint(); onComplete.run(); }
            });
            timer.start();
        }
    }
    
@Override public boolean initCashCorrectionActions() { return false; }
@Override public boolean initTrainCorrectionActions() { return false; }
@Override public int[] getLastPlayerTimes() { return new int[0]; }
@Override public void setLastPlayerTimes(int[] times) { }

@Override 
public void setPriorityPlayer(int index) { 
    if (gameUIManager == null || gameUIManager.getPlayerManager() == null) return;
    int pdIndex = gameUIManager.getPriorityPlayer() != null ? gameUIManager.getPriorityPlayer().getIndex() : -1;
    for (Map.Entry<Player, PlayerPanel> entry : playerRegistry.entrySet()) {
        entry.getValue().setPriorityDeal(entry.getKey().getIndex() == pdIndex);
    }
}

@Override 
public void highlightCurrentPlayer(int index) { 
    if (gameUIManager == null || gameUIManager.getPlayerManager() == null) return;
    Player currentPlayer = gameUIManager.getGameManager().getCurrentPlayer();
    for (Map.Entry<Player, PlayerPanel> entry : playerRegistry.entrySet()) {
        entry.getValue().setActive(entry.getKey().equals(currentPlayer));
    }
}

@Override 
public void updatePlayerOrder(java.util.List<String> playerNames) { 
    recreate(); 
}

@Override 
public void refreshDashboard() { 
    // Trigger a safe top-down refresh of all active components
    for (PlayerPanel pp : playerRegistry.values()) pp.refresh();
    for (CompanyPanel cp : companyRegistry.values()) cp.refresh();
    if (activeBankPanel != null) activeBankPanel.refresh();
}
}
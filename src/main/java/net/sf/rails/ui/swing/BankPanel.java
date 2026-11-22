package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.TrainCardType;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.ui.swing.elements.Caption;
import net.sf.rails.ui.swing.elements.RailCard;
import rails.game.action.BuyCertificate;
import rails.game.action.BuyTrain;
import rails.game.action.PossibleAction;

public class BankPanel extends JPanel {

    private final GameUIManager gameUIManager;
    private final Bank bank;
    
    private JLabel cashLabel;
    
    private JPanel sharesContainer;
    private JPanel trainsContainer;
    
    private JPanel poolTrainsPanel;
    private JPanel newTrainsPanel;
    private JPanel futureTrainsPanel;

    private int lastCash = Integer.MIN_VALUE;

    private static final Color BG_TRAINS = new Color(255, 222, 173);
    private static final Color BG_CARD_PASSIVE = new Color(255, 255, 240);
    private static final int THICK = 2;
    private static final int THIN = 1;
    private static final Color OUT = Color.BLACK;

    public BankPanel(GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
        this.bank = gameUIManager.getRoot().getBank();

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        setBackground(new Color(245, 245, 250));

        initUI();
    }

    private void initUI() {
        // --- 1. HEADER (Cash) ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(176, 224, 230));
        headerPanel.setOpaque(true);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        cashLabel = new JLabel();
        cashLabel.setFont(GameStatus_Alt.FONT_CURRENCY.deriveFont(22f));
        cashLabel.setForeground(new Color(0, 0, 128));
        cashLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(cashLabel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // --- 2. SHARES CONTAINER ---
        sharesContainer = new JPanel();
        sharesContainer.setLayout(new BoxLayout(sharesContainer, BoxLayout.Y_AXIS));
        sharesContainer.setOpaque(false);
        
        // Share Headers
        JPanel shareHeaderRow = new JPanel(new GridLayout(1, 2, 15, 0));
        shareHeaderRow.setOpaque(false);
        
        JLabel ipoHeader = new JLabel(" IPO");
        ipoHeader.setFont(GameStatus_Alt.FONT_SHARES);
        ipoHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        
        JLabel poolHeader = new JLabel(" Pool");
        poolHeader.setFont(GameStatus_Alt.FONT_SHARES);
        poolHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        
        shareHeaderRow.add(ipoHeader);
        shareHeaderRow.add(poolHeader);
        shareHeaderRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        
        sharesContainer.add(shareHeaderRow);
        sharesContainer.add(Box.createVerticalStrut(5));
        add(sharesContainer, BorderLayout.CENTER);

        // --- 3. TRAINS CONTAINER (GridBagLayout mirroring TrainMarketPanel) ---
        trainsContainer = new JPanel(new GridBagLayout());
        trainsContainer.setOpaque(false);
        trainsContainer.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Train Headers
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        
        gbc.gridx = 0;
        trainsContainer.add(createTrainHeader("Used", BorderFactory.createMatteBorder(THICK, THICK, THIN, THIN, OUT)), gbc);

        gbc.gridx = 1;
        trainsContainer.add(createTrainHeader("Current", BorderFactory.createMatteBorder(THICK, 0, THIN, THIN, OUT)), gbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        trainsContainer.add(createTrainHeader("Future", BorderFactory.createMatteBorder(THICK, 0, THIN, THICK, OUT)), gbc);

        // Train Data Panels
        gbc.gridy = 1;
        gbc.weightx = 0.0;

        poolTrainsPanel = createTrainBasePanel(BorderFactory.createMatteBorder(0, THICK, THICK, THIN, OUT));
        gbc.gridx = 0;
        trainsContainer.add(poolTrainsPanel, gbc);

        newTrainsPanel = createTrainBasePanel(BorderFactory.createMatteBorder(0, 0, THICK, THIN, OUT));
        gbc.gridx = 1;
        trainsContainer.add(newTrainsPanel, gbc);

        futureTrainsPanel = createTrainBasePanel(BorderFactory.createMatteBorder(0, 0, THICK, THICK, OUT));
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        trainsContainer.add(futureTrainsPanel, gbc);

        add(trainsContainer, BorderLayout.SOUTH);
    }

    private Caption createTrainHeader(String text, javax.swing.border.Border border) {
        Caption f = new Caption(text);
        f.setFont(new Font("SansSerif", Font.BOLD, 11));
        f.setBorder(border);
        f.setBackground(BG_TRAINS);
        f.setOpaque(true);
        return f;
    }

    private JPanel createTrainBasePanel(javax.swing.border.Border border) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        panel.setBackground(BG_TRAINS);
        panel.setOpaque(true);
        return panel;
    }

    private JPanel createTrainSlot(String trainName, String infoText, Color bgColor, Color fgColor, boolean isBuyable, PossibleAction pa) {
        JPanel slot = new JPanel(new BorderLayout(0, 2));
        slot.setOpaque(false);
        
        RailCard card = new RailCard((net.sf.rails.game.Train)null, new ButtonGroup());
        card.setCustomLabel(trainName);
        card.setCompactMode(true);
        card.setPreferredSize(new Dimension(50, 32));
        card.setBackground(bgColor);
        card.setForeground(fgColor);
        card.setFont(GameStatus_Alt.FONT_TRAINS.deriveFont(Font.BOLD, 16f));
        
        if (isBuyable && pa != null) {
            card.addPossibleAction(pa);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 160, 0), 3),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)
            ));
            card.setEnabled(true);
            if (getParent() != null && getParent().getParent() instanceof ActionListener) {
                card.addActionListener((ActionListener) getParent().getParent());
            }
        } else {
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 1),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)
            ));
            card.setEnabled(false);
        }
        
        JLabel infoLabel = new JLabel(infoText, SwingConstants.CENTER);
        infoLabel.setFont(GameStatus_Alt.FONT_NUMBERS);
        infoLabel.setForeground(Color.BLACK);
        
        slot.add(card, BorderLayout.NORTH);
        slot.add(infoLabel, BorderLayout.CENTER);
        
        return slot;
    }

    public void refresh() {
        int currentCash = bank.getCash();
        if (lastCash == Integer.MIN_VALUE) {
            lastCash = currentCash;
            cashLabel.setText(gameUIManager.format(currentCash));
        } else if (currentCash != lastCash) {
            if (cashLabel.isShowing()) {
                JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
                if (frame != null) new PlayerPanel.MoneySpinnerAnimator(frame, cashLabel, lastCash, currentCash, gameUIManager).start();
            } else {
                cashLabel.setText(gameUIManager.format(currentCash));
            }
            lastCash = currentCash;
        }

        List<PossibleAction> actions = null;
        if (gameUIManager.getGameManager().getPossibleActions() != null) {
            actions = gameUIManager.getGameManager().getPossibleActions().getList();
        }

        // --- SHARES REFRESH (Parallel Rows) ---
        
        // 1. Preserve Header
        Component header = sharesContainer.getComponent(0);
        sharesContainer.removeAll();
        sharesContainer.add(header);
        sharesContainer.add(Box.createVerticalStrut(5));

        // 2. Collect and Sort Companies
        List<PublicCompany> activeComps = new ArrayList<>();
        for (PublicCompany c : gameUIManager.getAllPublicCompanies()) {
            if (c.isClosed()) continue;
            int ipoPct = bank.getIpo().getPortfolioModel().getShare(c);
            int poolPct = bank.getPool().getPortfolioModel().getShare(c);
            if (ipoPct > 0 || poolPct > 0) activeComps.add(c);
        }

        activeComps.sort((c1, c2) -> {
            int p1 = c1.getCurrentSpace() != null ? c1.getCurrentSpace().getPrice() : c1.getParPrice();
            int p2 = c2.getCurrentSpace() != null ? c2.getCurrentSpace().getPrice() : c2.getParPrice();
            if (p1 != p2) return Integer.compare(p2, p1); // Descending price
            return c1.getId().compareTo(c2.getId());
        });

        // 3. Build Grid Rows
        for (PublicCompany c : activeComps) {
            JPanel row = new JPanel(new GridLayout(1, 2, 15, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

            int ipoPct = bank.getIpo().getPortfolioModel().getShare(c);
            if (ipoPct > 0) {
                java.util.List<net.sf.rails.game.financial.PublicCertificate> certs = new ArrayList<>(bank.getIpo().getPortfolioModel().getCertificates(c));
                row.add(buildBankShareCell(c, certs, ipoPct, c.getParPrice(), actions));
            } else {
                JPanel empty = new JPanel(); empty.setOpaque(false); row.add(empty);
            }

            int poolPct = bank.getPool().getPortfolioModel().getShare(c);
            if (poolPct > 0) {
                java.util.List<net.sf.rails.game.financial.PublicCertificate> certs = new ArrayList<>(bank.getPool().getPortfolioModel().getCertificates(c));
                int price = (c.getCurrentSpace() != null) ? c.getCurrentSpace().getPrice() : c.getParPrice();
                row.add(buildBankShareCell(c, certs, poolPct, price, actions));
            } else {
                JPanel empty = new JPanel(); empty.setOpaque(false); row.add(empty);
            }

            sharesContainer.add(row);
            sharesContainer.add(Box.createVerticalStrut(4));
        }

        // --- TRAINS REFRESH ---
        poolTrainsPanel.removeAll();
        newTrainsPanel.removeAll();
        futureTrainsPanel.removeAll();

        // 1. Used (Pool)
        Map<String, List<net.sf.rails.game.Train>> poolMap = new LinkedHashMap<>();
        for (net.sf.rails.game.Train t : bank.getPool().getPortfolioModel().getTrainList()) {
            String name = t.getName().replaceAll("_\\d+$", "");
            poolMap.computeIfAbsent(name, k -> new ArrayList<>()).add(t);
        }
        
        for (Map.Entry<String, List<net.sf.rails.game.Train>> entry : poolMap.entrySet()) {
            int count = entry.getValue().size();
            net.sf.rails.game.Train firstTrain = entry.getValue().get(0);
            
            // Format: (Qty) / $Price
            String infoText = "<html><center>(" + count + ")<br><font color='#000080'><b>" + gameUIManager.format(firstTrain.getCost()) + "</b></font></center></html>";
            
            boolean isBuyable = false;
            PossibleAction matchedAction = null;
            if (actions != null) {
                for (PossibleAction pa : actions) {
                    if (pa instanceof BuyTrain && ((BuyTrain) pa).getTrain() != null && ((BuyTrain) pa).getTrain().equals(firstTrain)) {
                        isBuyable = true;
                        matchedAction = pa;
                        break;
                    }
                }
            }
            
            poolTrainsPanel.add(createTrainSlot(entry.getKey(), infoText, BG_CARD_PASSIVE, Color.BLACK, isBuyable, matchedAction));
        }

        // 2. Current (IPO) & Future
        
        // Map actual IPO inventory to accurately reflect quantities of available trains
        Map<String, Integer> ipoCounts = new LinkedHashMap<>();
        for (net.sf.rails.game.Train t : bank.getIpo().getPortfolioModel().getTrainList()) {
            String name = t.getName().replaceAll("_\\d+$", "");
            ipoCounts.put(name, ipoCounts.getOrDefault(name, 0) + 1);
        }

        for (TrainCardType tct : gameUIManager.getRoot().getTrainManager().getTrainCardTypes()) {
            if (tct.getPotentialTrainTypes().isEmpty()) continue;
            
            String label = tct.getId().replaceAll("_\\d+$", "");
            int cost = tct.getPotentialTrainTypes().get(0).getCost();
            
            if (tct.isAvailable()) {
                // Render in Current (IPO)
                int qty = ipoCounts.getOrDefault(label, 0);
                if (qty == 0) continue; // Suppress if actually sold out
                
                String infoText = "<html><center>(" + qty + ")<br><font color='#000080'><b>" + gameUIManager.format(cost) + "</b></font></center></html>";
                
                boolean isBuyable = false;
                PossibleAction matchedAction = null;
                if (actions != null) {
                    for (PossibleAction pa : actions) {
                        if (pa instanceof BuyTrain && ((BuyTrain) pa).getTrain() != null && ((BuyTrain) pa).getTrain().getType().getName().equals(tct.getId())) {
                            isBuyable = true;
                            matchedAction = pa;
                            break;
                        }
                    }
                }
                newTrainsPanel.add(createTrainSlot(label, infoText, BG_CARD_PASSIVE, Color.BLACK, isBuyable, matchedAction));
                
            } else {
                // Render in Future
                String qtyStr;
                if (tct.hasInfiniteQuantity()) {
                    qtyStr = "\u221E"; 
                } else {
                    int remaining = tct.getQuantity() - tct.getNumberBoughtFromIPO();
                    if (remaining <= 0) continue; 
                    qtyStr = String.valueOf(remaining);
                }
                
                String infoText = "<html><center>(" + qtyStr + ")<br><font color='#000080'><b>" + gameUIManager.format(cost) + "</b></font></center></html>";
                futureTrainsPanel.add(createTrainSlot(label, infoText, new Color(240, 240, 240), new Color(120, 120, 120), false, null));
            }
        }

        revalidate(); repaint();
    }

    private JPanel buildBankShareCell(PublicCompany company, java.util.List<net.sf.rails.game.financial.PublicCertificate> certs, int totalPct, int price, List<PossibleAction> actions) {
        JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.X_AXIS));
        cell.setOpaque(false);
        cell.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // 1. Company ID Badge (Solid colors restored)
        JLabel idLabel = new JLabel(" " + company.getId() + " ");
        idLabel.setFont(GameStatus_Alt.FONT_SHARES);
        idLabel.setOpaque(true);
        idLabel.setBackground(company.getBgColour() != null ? company.getBgColour() : Color.LIGHT_GRAY);
        idLabel.setForeground(company.getFgColour() != null ? company.getFgColour() : Color.BLACK);
        idLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        cell.add(idLabel);
        cell.add(Box.createHorizontalStrut(8)); 

        // 2. Group Certificates by share % and presidency
        certs.sort((c1, c2) -> {
            if (c1.isPresidentShare() != c2.isPresidentShare()) return c1.isPresidentShare() ? -1 : 1;
            return Integer.compare(c2.getShare(), c1.getShare());
        });

        List<List<net.sf.rails.game.financial.PublicCertificate>> groupedCerts = new ArrayList<>();
        List<net.sf.rails.game.financial.PublicCertificate> currentGroup = new ArrayList<>();

        for (net.sf.rails.game.financial.PublicCertificate cert : certs) {
            if (currentGroup.isEmpty()) {
                currentGroup.add(cert);
            } else {
                net.sf.rails.game.financial.PublicCertificate prev = currentGroup.get(0);
                if (cert.getShare() == prev.getShare() && cert.isPresidentShare() == prev.isPresidentShare()) {
                    currentGroup.add(cert);
                } else {
                    groupedCerts.add(currentGroup);
                    currentGroup = new ArrayList<>();
                    currentGroup.add(cert);
                }
            }
        }
        if (!currentGroup.isEmpty()) {
            groupedCerts.add(currentGroup);
        }

        // Build Visual Stacks
        for (List<net.sf.rails.game.financial.PublicCertificate> group : groupedCerts) {
            CardStackPanel currentStack = new CardStackPanel();
            cell.add(currentStack);

            for (int i = 0; i < group.size(); i++) {
                net.sf.rails.game.financial.PublicCertificate cert = group.get(i);
                boolean isCertPrez = cert.isPresidentShare();
                RailCard card = new RailCard(cert, new ButtonGroup());

                // Restore solid colors to match PlayerPanel
                card.setBackground(company.getBgColour() != null ? company.getBgColour() : Color.LIGHT_GRAY);
                card.setForeground(company.getFgColour() != null ? company.getFgColour() : Color.BLACK);
                card.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

                // Apply text to ALL cards so the RailCard geometry is strictly maintained (no thin stripes)
                card.setCustomLabel(cert.getShare() + "%" + (isCertPrez ? " P" : ""));
                card.setCompactMode(true);
                
                if (actions != null) {
                    for (PossibleAction pa : actions) {
                        if (pa instanceof BuyCertificate && ((BuyCertificate) pa).getCompany().equals(company)) {
                            card.addPossibleAction(pa);
                            card.setBorder(BorderFactory.createLineBorder(new Color(0, 160, 0), 3)); 
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

        // --- RIGHT ALIGNMENT ANCHOR ---
        // Pushes Sum and Price to the extreme right edge of the grid cell
        cell.add(Box.createHorizontalGlue());

        // 3. Overall Sum
        JLabel summaryLabel = new JLabel(totalPct + "% ");
        summaryLabel.setFont(GameStatus_Alt.FONT_NUMBERS);
        cell.add(summaryLabel);

        // 4. Prominent Price Badge (Double $$ fix: gameUIManager.format already provides $)
        JLabel priceBadge = new JLabel(" " + gameUIManager.format(price) + " ");
        priceBadge.setFont(GameStatus_Alt.FONT_CURRENCY.deriveFont(Font.BOLD, 14f));
        priceBadge.setOpaque(true);
        priceBadge.setBackground(new Color(225, 245, 225)); 
        priceBadge.setBorder(BorderFactory.createLineBorder(new Color(34, 139, 34), 2)); 
        priceBadge.setForeground(new Color(0, 100, 0));
        cell.add(priceBadge);

        return cell;
    }

    /**
     * DYNAMIC LAYOUT ENGINE FOR OVERLAPPING CARDS
     * Uses reverse-index math so cards don't "wiggle" and stack widths remain exact.
     */
    private static class CardStackPanel extends JPanel {
        public CardStackPanel() {
            setLayout(null);
            setOpaque(false);
        }

        @Override
        public void doLayout() {
            int count = getComponentCount();
            if (count == 0) return;
            
            Component topCard = getComponent(count - 1);
            Dimension pref = topCard.getPreferredSize();
            int baseW = pref.width > 0 ? pref.width : 50; 
            int baseH = pref.height > 0 ? pref.height : 35;
            
            int stepX = (int) (baseW * 0.20);
            if (stepX < 2) stepX = 2; 

            for (int i = 0; i < count; i++) {
                Component c = getComponent(i);
                int reverseIndex = (count - 1) - i;
                int xPos = reverseIndex * stepX;
                int yOffset = (reverseIndex % 2 == 1) ? 2 : 0;

                c.setBounds(xPos, yOffset, baseW, baseH);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            int count = getComponentCount();
            if (count == 0) return new Dimension(0, 0);
            Dimension pref = getComponent(count - 1).getPreferredSize();
            int baseW = pref.width > 0 ? pref.width : 50;
            int baseH = pref.height > 0 ? pref.height : 35;
            int stepX = (int) (baseW * 0.20);
            return new Dimension(baseW + (count - 1) * stepX, baseH + 2);
        }

        @Override
        public boolean isOptimizedDrawingEnabled() {
            return false; 
        }
    }
}
package net.sf.rails.ui.swing.gamestatus;

import net.sf.rails.ui.swing.elements.Caption;
import net.sf.rails.ui.swing.elements.RailCard;

import javax.swing.*;
import java.awt.*;

public class TrainMarketPanel extends JPanel {

    private final int MAX_POOL_SLOTS = 3;
    private final int MAX_IPO_SLOTS = 3;
    private final int MAX_FUTURE_SLOTS = 6;

    private static final Color BG_TRAINS = new Color(255, 222, 173);
    private static final Color BG_CARD_PASSIVE = new Color(255, 255, 240);
    private static final Color OUT = Color.BLACK;
    private static final int THICK = 2;
    private static final int THIN = 1;

    private static final javax.swing.border.Border B_TOP_L = BorderFactory.createMatteBorder(THICK, THICK, THIN, THIN, OUT);
    private static final javax.swing.border.Border B_TOP_M = BorderFactory.createMatteBorder(THICK, 0, THIN, THIN, OUT);
    private static final javax.swing.border.Border B_TOP_R = BorderFactory.createMatteBorder(THICK, 0, THIN, THICK, OUT);
    private static final javax.swing.border.Border B_BOT_L = BorderFactory.createMatteBorder(0, THICK, THICK, THIN, OUT);
    private static final javax.swing.border.Border B_BOT_M = BorderFactory.createMatteBorder(0, 0, THICK, THIN, OUT);
    private static final javax.swing.border.Border B_BOT_R = BorderFactory.createMatteBorder(0, 0, THICK, THICK, OUT);

    public final JPanel poolTrainsPanel;
    public final JPanel newTrainsPanel;
    public final JPanel futureTrainsPanel;

    public final RailCard[] poolTrainButtons;
    public final RailCard[] newTrainButtons;
    public final RailCard[] futureTrainButtons;

    public final JLabel[] poolTrainInfoLabels;
    public final JLabel[] newTrainQtyLabels;
    public final JLabel[] futureTrainInfoLabels;
    public static final String FONT_FAMILY_CURRENCY = "Monospaced";

    public TrainMarketPanel(ButtonGroup buySellGroup, java.awt.event.ActionListener listener, Font stickyFont) {
        setLayout(new GridBagLayout());
        setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // --- 1. HEADERS ---
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        
        gbc.gridx = 0;
        add(createHeader("Used", B_TOP_L), gbc);

        gbc.gridx = 1;
        add(createHeader("Current", B_TOP_M), gbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0; // Future takes remaining space
        add(createHeader("Future", B_TOP_R), gbc);

        // --- 2. DATA PANELS ---
        gbc.gridy = 1;
        gbc.weightx = 0.0;

        // POOL (USED)
        poolTrainsPanel = createBasePanel(B_BOT_L);
        poolTrainButtons = new RailCard[MAX_POOL_SLOTS];
        poolTrainInfoLabels = new JLabel[MAX_POOL_SLOTS];
        populateSlots(poolTrainsPanel, poolTrainButtons, poolTrainInfoLabels, MAX_POOL_SLOTS, buySellGroup, listener, stickyFont);
        gbc.gridx = 0;
        add(poolTrainsPanel, gbc);

        // IPO (CURRENT)
        newTrainsPanel = createBasePanel(B_BOT_M);
        newTrainButtons = new RailCard[MAX_IPO_SLOTS];
        newTrainQtyLabels = new JLabel[MAX_IPO_SLOTS];
        populateSlots(newTrainsPanel, newTrainButtons, newTrainQtyLabels, MAX_IPO_SLOTS, buySellGroup, listener, stickyFont);
        gbc.gridx = 1;
        add(newTrainsPanel, gbc);

        // FUTURE
        futureTrainsPanel = createBasePanel(B_BOT_R);
        futureTrainButtons = new RailCard[MAX_FUTURE_SLOTS];
        futureTrainInfoLabels = new JLabel[MAX_FUTURE_SLOTS];
        populateSlots(futureTrainsPanel, futureTrainButtons, futureTrainInfoLabels, MAX_FUTURE_SLOTS, buySellGroup, listener, stickyFont);
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        add(futureTrainsPanel, gbc);
    }

    private Caption createHeader(String text, javax.swing.border.Border border) {
        Caption f = new Caption(text);
        f.setFont(new Font("SansSerif", Font.BOLD, 11));
        f.setBorder(border);
        f.setBackground(BG_TRAINS);
        f.setOpaque(true);
        return f;
    }

    private JPanel createBasePanel(javax.swing.border.Border border) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(5, 0, 0, 0)));
        panel.setBackground(BG_TRAINS);
        panel.setOpaque(true);
        return panel;
    }

    private void populateSlots(JPanel parent, RailCard[] cards, JLabel[] labels, int maxSlots, ButtonGroup group, java.awt.event.ActionListener listener, Font font) {
        for (int i = 0; i < maxSlots; i++) {
            JPanel slot = new JPanel(new BorderLayout());
            slot.setOpaque(false);

            cards[i] = createTrainButton(group, listener, font);
            slot.add(cards[i], BorderLayout.NORTH);

            labels[i] = new JLabel(" ", SwingConstants.CENTER);
            int fontSize = (font != null) ? font.getSize() : 12;
            labels[i].setFont(new Font(FONT_FAMILY_CURRENCY, Font.BOLD, fontSize));
            labels[i].setForeground(Color.BLACK);

            // Add scaling protection against global UI font overrides
            final JLabel lbl = labels[i];
            lbl.addPropertyChangeListener("font", evt -> {
                Font f = (Font) evt.getNewValue();
                if (f != null && (!FONT_FAMILY_CURRENCY.equals(f.getFamily()) || f.getStyle() != Font.BOLD)) {
                    lbl.setFont(new Font(FONT_FAMILY_CURRENCY, Font.BOLD, f.getSize()));
                }
            });
            slot.add(labels[i], BorderLayout.CENTER);

            parent.add(slot);
        }
    }

    private RailCard createTrainButton(ButtonGroup group, java.awt.event.ActionListener listener, Font font) {
        RailCard cf = new RailCard((net.sf.rails.game.Train) null, group);
        cf.addActionListener(listener);
        if (font != null) {
            cf.setFont(font);
        }
        cf.setCompactMode(true);
        cf.setBackground(BG_CARD_PASSIVE);
        cf.setVisible(false);
        return cf;
    }
}
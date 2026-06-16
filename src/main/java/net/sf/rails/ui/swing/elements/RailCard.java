/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/RailCard.java,v 1.7 2026/01/01 23:15:00 evos Exp $*/
package net.sf.rails.ui.swing.elements;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.sf.rails.game.Company; // Added for generic access
import net.sf.rails.game.special.SpecialProperty;
import rails.game.action.PossibleAction;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.Train;
import net.sf.rails.game.financial.Certificate;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.Company;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RailCard - Dynamic Geometry Edition.
 * Sizes are calculated strictly based on the Font metrics.
 */
public class RailCard extends ClickField {

    private static final Logger log = LoggerFactory.getLogger(RailCard.class);

    private static final long serialVersionUID = 1L;

    // --- CONFIGURATION CONSTANTS ---
    private static final double HEIGHT_FACTOR = 1.3; // Height = 1.3 * Font Height
    private static final int WIDTH_CHARS_COMPACT = 5; // Trains/Privates = 5 chars
    private static final int WIDTH_GAP_SHARES = 10; // Extra padding for Shares

    public enum State {
        PASSIVE, ACTIONABLE, SELECTED, DISABLED, HIDDEN, HIGHLIGHTED // Added HIGHLIGHTED
    }

    private List<Certificate> certificates = new ArrayList<>();
    private List<Train> trains = new ArrayList<>();
    private List<PrivateCompany> privates = new ArrayList<>();

    private State currentState = State.PASSIVE;
    private boolean compactMode = false;
    private String customLabel = null;

    // Colors
    private static final Color COL_BG_BEIGE = new Color(255, 255, 240);
    private static final Color COL_BG_DIMMED = new Color(224, 224, 224);
    private static final Color COL_FG_DIMMED = new Color(128, 128, 128);
    private static final Color COL_ACTIONABLE_BG = new Color(200, 255, 200);
    private static final Color COL_SELECTED_BG = new Color(100, 200, 100);
    private static final Color COL_SELECTED_BORDER = Color.BLUE;

    // Change "Cyan-ish" to pure CYAN as requested, managed centrally here.
    private static final Color COL_HIGHLIGHT_BG = Color.CYAN;
    private static final Color COL_HIGHLIGHT_BORDER_C = Color.BLUE;

    // Borders (Minimal)
    private static final Border BORDER_ACTIONABLE = BorderFactory.createRaisedBevelBorder();
    private static final Border BORDER_PASSIVE = BorderFactory.createLineBorder(Color.BLACK, 1);
    private static final Border BORDER_DISABLED = BorderFactory.createLoweredBevelBorder();
    private static final Border BORDER_SELECTED = BorderFactory.createLineBorder(COL_SELECTED_BORDER, 2);
    // NEW: Thick Blue Border for "Click Me" state
    private static final Border BORDER_HIGHLIGHT = BorderFactory.createLineBorder(COL_HIGHLIGHT_BORDER_C, 3);

    private double scaleFactor = 1.0;

    private JPanel contentPanel;

    private Company associatedCompany; // The company this card represents

    public void setCompany(Company c) {
        this.associatedCompany = c;
    }

    public Company getCompany() {
        return associatedCompany;
    }

    /**
     * Assigns an action to this card, making it clickable (Blue Highlight mode).
     */
    public void setPossibleAction(PossibleAction action) {
        this.clearPossibleActions();
        if (action != null) {
            this.addPossibleAction(action);
        }
    }

    public RailCard(StartItem startItem, ButtonGroup group) {
        super("", "", "", null, group);
        if (startItem != null) {
            if (startItem.getPrimary() != null)
                certificates.add(startItem.getPrimary());
            if (startItem.hasSecondary())
                certificates.add(startItem.getSecondary());
        }
        initVisuals();
    }

    public net.sf.rails.game.Train getTrain() {
        return trains.isEmpty() ? null : trains.get(0);
    }

    public RailCard(Certificate cert, ButtonGroup group) {
        super("", "", "", null, group);
        if (cert != null)
            certificates.add(cert);
        initVisuals();
    }

    public RailCard(Train train, ButtonGroup group) {
        super("", "", "", null, group);
        if (train != null)
            trains.add(train);
        initVisuals();
    }

    public RailCard(PrivateCompany priv, ButtonGroup group) {
        super("", "", "", null, group);
        if (priv != null)
            privates.add(priv);
        initVisuals();
    }

    public RailCard(Certificate cert) {
        this(cert, null);
    }

    public RailCard(Train train) {
        this(train, null);
    }

    public RailCard(PrivateCompany priv) {
        this(priv, null);
    }

    public void reset() {
        this.certificates.clear();
        this.trains.clear();
        this.privates.clear();
        this.customLabel = null;
        this.setToolTipText(null);
        this.clearPossibleActions();
        setState(State.PASSIVE);
        updateView();
    }

    public State getState() {
        return currentState;
    }

    /**
     * Scales the entire card: Width, Height, and Font.
     * 
     * @param factor 1.0 is standard. e.g., 1.2 is 20% larger.
     */
    public void setScale(double factor) {
        this.scaleFactor = factor;
        // Derive and set the new font size
        Font currentFont = getFont();
        if (currentFont != null) {
            float newSize = (float) (currentFont.getSize() * factor);
            setFont(currentFont.deriveFont(newSize));
        }
        this.revalidate();
    }

    public void setTrain(Train train) {
        reset();
        if (train != null)
            this.trains.add(train);
        updateView();
    }

    public void setCustomLabel(String label) {
        // FIX: Do not clear internal lists (trains/certificates)!
        // We need them to remain populated so getUniqueId() works for action matching.
        this.customLabel = label;
        updateView();
    }

    public void setCompactMode(boolean compact) {
        this.compactMode = compact;
        if (compact) {
            this.setMargin(new Insets(0, 0, 0, 0));
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setVerticalAlignment(SwingConstants.CENTER);
        }
        updateView();
    }

    private void initVisuals() {
        super.setText("");
        this.setLayout(new BorderLayout());
        this.setMargin(new Insets(0, 0, 0, 0));

        contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setOpaque(false);

        this.add(contentPanel, BorderLayout.CENTER);

        setState(State.PASSIVE);
        updateView();
    }

    private void updateView() {
        if (contentPanel == null)
            return;
        contentPanel.removeAll();

        boolean isDimmed = (currentState == State.DISABLED);
        Color fgColor = isDimmed ? COL_FG_DIMMED : getForeground();

        // 1. Determine if this card represents a Short Share
        boolean isShortShare = isShortShare();

        // 2. Override the base background if it is a short share
        if (isShortShare) {
            this.setBackground(Color.PINK);
            contentPanel.setBackground(Color.PINK);
            contentPanel.setOpaque(true);
        }

        if (compactMode) {
            String labelText = getFirstAvailableLabel();
            Color compactBg = isShortShare ? Color.PINK : null;
            JLabel lbl = createStyledLabel(labelText, compactBg, fgColor, true);
            contentPanel.add(lbl);

        } else {
            GridBagConstraints gbc = new GridBagConstraints();

            int totalItems = certificates.size() + trains.size() + privates.size();
            boolean centerSingleItem = (totalItems == 1 && scaleFactor > 1.0);

            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0;

            if (centerSingleItem) {
                // 1. Left Spacer (Weight 1.0)
                gbc.gridx = 0;
                gbc.weightx = 1.2;
                contentPanel.add(javax.swing.Box.createGlue(), gbc);

                // 2. Prepare Constraints for the Item (Weight 2.0)
                // Width Logic: The double items have a 5px gap (insets.right=5).
                // To match their width exactly, we must shrink this single item by 5px too.
                // We split the 5px (2px left, 3px right) to keep it visually centered.
                gbc.weightx = 2.0;
                gbc.insets = new Insets(0, 2, 0, 3);

            } else {
                // Standard behavior: Fill evenly with 5px gap
                gbc.weightx = 1.0;
                gbc.insets = new Insets(0, 0, 0, 5);
            }

            int x = centerSingleItem ? 1 : 0; // Start at 1 if centering

            for (Certificate cert : certificates) {
                String label = cert.getId();
                if (cert instanceof PublicCertificate) {
                    label = ((PublicCertificate) cert).getParent().getId();
                } else if (cert instanceof PrivateCompany) {
                    label = ((PrivateCompany) cert).getId();
                }
                // Strip

                Color bg = getCertColor(cert);
                if (bg == null)
                    bg = isDimmed ? COL_BG_DIMMED : COL_BG_BEIGE;
                Color itemFg = isDark(bg) ? Color.WHITE : Color.BLACK;
                if (isDimmed)
                    itemFg = COL_FG_DIMMED;

                JLabel lbl = createStyledLabel(label, bg, itemFg, false);
                lbl.setBorder(new LineBorder(Color.GRAY, 1));
                // Center text in the wider label
                lbl.setHorizontalAlignment(SwingConstants.CENTER);

                gbc.gridx = x++;
                contentPanel.add(lbl, gbc);
            }

            for (Train train : trains) {
                JLabel lbl = createStyledLabel(train.getName(), null, fgColor, false);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                gbc.gridx = x++;
                contentPanel.add(lbl, gbc);
            }

            for (PrivateCompany priv : privates) {
                JLabel lbl = createStyledLabel(priv.getId(), null, fgColor, false);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                gbc.gridx = x++;
                contentPanel.add(lbl, gbc);
            }

            if (centerSingleItem) {
                // 3. Add Right Spacer (Weight 1.0)
                gbc.gridx = x;
                gbc.weightx = 1.2;
                contentPanel.add(javax.swing.Box.createGlue(), gbc);
            }

        }

        /*
         * // Adjust content panel padding to prevent text overlapping the right-side
         * stripe
         * int rightPadding = 0;
         * java.awt.Color stripeColor = null;
         * boolean isPresident = false;
         * 
         * if (associatedCompany instanceof PublicCompany) {
         * PublicCompany pc = (PublicCompany) associatedCompany;
         * stripeColor = pc.getBgColour();
         * if (!pc.hasStockPrice()) {
         * isPresident = true;
         * } else if (customLabel != null) {
         * String plain = customLabel.replaceAll("\\<.*?\\>", "").trim();
         * if (plain.endsWith("P") || plain.equals("Owner") || plain.contains("%P")) {
         * isPresident = true;
         * }
         * }
         * } else {
         * for (Certificate cert : certificates) {
         * if (cert instanceof PublicCertificate) {
         * PublicCertificate pc = (PublicCertificate) cert;
         * if (pc.getCompany() != null) {
         * stripeColor = pc.getCompany().getBgColour();
         * if (pc.getCompany().getPresidentsShare() != null &&
         * pc.getCompany().getPresidentsShare().equals(pc)) {
         * isPresident = true;
         * } else if (pc.getShare() >= 20) {
         * isPresident = true;
         * }
         * }
         * break;
         * }
         * }
         * }
         * 
         * if (stripeColor != null) {
         * int baseWidth = getPreferredSize().width;
         * rightPadding = isPresident ? (int) (baseWidth * 0.20) : (int) (baseWidth *
         * 0.10);
         * rightPadding += 2; // Add a 2px buffer so text doesn't touch the line
         * }
         * 
         * contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0,
         * rightPadding));
         */

        contentPanel.revalidate();
        contentPanel.repaint();
        this.revalidate();
        this.repaint();
    }

    @Override
    public java.util.List<rails.game.action.PossibleAction> getPossibleActions() {
        java.util.List<rails.game.action.PossibleAction> actions = super.getPossibleActions();
        return (actions == null) ? java.util.Collections.emptyList() : actions;
    }

    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        if (contentPanel != null) {
            for (Component c : contentPanel.getComponents()) {
                c.setForeground(fg);
            }
            updateView();
        }
    }

    private JLabel createStyledLabel(String text, Color bg, Color fg, boolean isCentered) {
        JLabel l = new JLabel(text);
        l.setOpaque(bg != null);
        if (bg != null)
            l.setBackground(bg);
        l.setForeground(fg);
        l.setFont(this.getFont());
        if (isCentered)
            l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setFocusable(false);
        return l;
    }

    private String getFirstAvailableLabel() {
        if (customLabel != null)
            return customLabel;
        if (!certificates.isEmpty())
            return certificates.get(0).getId();
        if (!trains.isEmpty())
            return trains.get(0).getName();
        if (!privates.isEmpty())
            return privates.get(0).getId();
        return "";
    }

    public void setState(State state) {
        this.currentState = state;
        this.setEnabled(true);
        this.setOpaque(true);
        this.setSelected(false);
        this.setVisible(true);

        switch (state) {
            case ACTIONABLE:
                this.setBorder(BORDER_ACTIONABLE);
                this.setBackground(COL_ACTIONABLE_BG);
                break;
            case SELECTED:
                this.setSelected(true);
                this.setBorder(BORDER_SELECTED);
                this.setBackground(COL_SELECTED_BG);
                break;
            case HIGHLIGHTED:
                this.setBorder(BORDER_HIGHLIGHT);
                this.setBackground(COL_HIGHLIGHT_BG);
                break;
            case PASSIVE:
                this.setBorder(BORDER_PASSIVE);
                this.setBackground(COL_BG_BEIGE);
                this.clearPossibleActions();
                break;
            case DISABLED:
                this.setEnabled(false);
                this.setBorder(BORDER_DISABLED);
                this.setBackground(COL_BG_DIMMED);
                break;
            case HIDDEN:
                this.setVisible(false);
                break;

        }
        updateView();
    }

    public boolean isShortShare() {
        boolean isShort = false;
        // Capture to a local variable to prevent thread race conditions
        String safeLabel = this.customLabel;

        if (safeLabel != null && (safeLabel.toLowerCase().contains("short") || safeLabel.contains("-"))) {
            isShort = true;
        } else if (certificates != null) {
            for (Certificate c : certificates) {
                if (isShortCertificate(c)) {
                    isShort = true;
                    break;
                }
            }
        }
        return isShort;
    }

    @Override
    public void setBackground(Color bg) {
        // Intercept standard beige assignments if this is a Short Share to maintain the
        // Pink color.
        if (bg != null && bg.equals(COL_BG_BEIGE) && isShortShare()) {
            super.setBackground(Color.PINK);
            if (contentPanel != null) {
                contentPanel.setBackground(Color.PINK);
            }
        } else {
            super.setBackground(bg);
            if (contentPanel != null) {
                contentPanel.setBackground(bg);
            }
        }
    }

    /**
     * Robust Identification: Checks if this card holds the specific certificate
     * object.
     * Use this instead of comparing string IDs or labels.
     */
    public boolean holdsCertificate(Certificate target) {
        if (target == null || certificates == null)
            return false;

        boolean matchFound = false;

        for (Certificate held : certificates) {
            // 1. Fast Path: Identity
            if (held == target) {
                matchFound = true;
            }
            // 2. Slow Path: Logical Identity (Class + ID + Share%)
            else if (held.getClass().equals(target.getClass()) && held.getId().equals(target.getId())) {
                if (held instanceof PublicCertificate && target instanceof PublicCertificate) {
                    if (((PublicCertificate) held).getShare() == ((PublicCertificate) target).getShare()) {
                        matchFound = true;
                    }
                } else {
                    matchFound = true; // It's a match (Private/Train)
                }
            }

            if (matchFound)
                return true;
        }

        return false;
    }

    @Override
    public void setFont(Font f) {
        super.setFont(f);
        if (contentPanel != null) {
            for (Component c : contentPanel.getComponents()) {
                c.setFont(f);
            }
            updateView();
        }
    }

    public static Dimension calculateBaseSize(Font f, boolean compactMode, double scaleFactor) {
        if (f == null)
            f = new Font("SansSerif", Font.BOLD, 12);

        FontMetrics fm = java.awt.Toolkit.getDefaultToolkit().getFontMetrics(f);
        int fontHeight = (fm != null) ? fm.getHeight() : 14;
        int height = (int) (fontHeight * HEIGHT_FACTOR * scaleFactor);

        // 1. Set Standard Width to "WWWW" (Narrower for GameStatus)
        String referenceString = "WWWW";
        int charW = (fm != null) ? fm.stringWidth(referenceString) : 40;

        int width = (int) ((charW + WIDTH_GAP_SHARES) * scaleFactor);

        return new Dimension(width, height);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension base = calculateBaseSize(getFont(), compactMode, scaleFactor);

        if (scaleFactor == 1.0) {
            // Normal behavior (GameStatus, ORPanel)
            // Compact mode condenses everything into one label, so it should only be 1 item
            // wide.
            int items = compactMode ? 1 : Math.max(1, certificates.size() + trains.size() + privates.size());
            if (items > 1)
                base.width *= items;
        } else {
            // Start Round Window Mode (Scaled > 1.0)
            // FORCE IDENTICAL WIDTH: Always calculate width as if there are 2 items.
            // This ensures single-item cards (M1) are identical width to double-item cards
            // (LD|SX).
            int fixedItems = 2;
            int totalW = base.width * fixedItems;

            // Add gap spacing
            totalW += (fixedItems - 1) * 5;

            return new Dimension(totalW, base.height);
        }
        return base;
    }

    /**
     * CRITICAL FIX: The "squashed" look happens because GridBagLayout
     * shrinks components to their Minimum Size when space is tight.
     * By default, Minimum Size is often (0,0).
     * We must override this to match Preferred Size to prevent squashing.
     */
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    private boolean isDark(Color c) {
        if (c == null)
            return false;
        double y = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
        return y < 128;
    }

    public List<Certificate> getCertificates() {
        return certificates;
    }

    /**
     * Prevents the card from being stretched horizontally by BoxLayout
     * (GameStatus).
     */
    @Override
    public Dimension getMaximumSize() {
        Dimension pref = getPreferredSize();
        return new Dimension(pref.width, pref.height);
    }

    public void setShareStackTooltip(Collection<PublicCertificate> certs) {
        if (certs == null || certs.isEmpty()) {
            setToolTipText(null);
            return;
        }

        Map<Integer, Integer> counts = new TreeMap<>(Collections.reverseOrder());
        PublicCompany company = null;

        for (PublicCertificate cert : certs) {
            if (company == null)
                company = cert.getCompany();
            int s = cert.getShare();
            counts.put(s, counts.getOrDefault(s, 0) + 1);
        }

        StringBuilder text = new StringBuilder();
        text.append("<html><b>");
        if (company != null)
            text.append(company.getId()).append(" ");
        text.append("Shares</b><hr><font size='4'>");

        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            text.append(entry.getKey()).append("% x ").append(entry.getValue()).append("<br>");
        }
        text.append("</font></html>");

        setToolTipText(text.toString());
    }

    public void setPrivateCompanyTooltip(PrivateCompany pc) {
        if (pc == null) {
            setToolTipText(null);
            return;
        }
        String info = pc.getInfoText();
        if (info != null) {
            info = info.replaceAll("(?i)^<html>|</html>$", "");
            info = "<html><div style='width: 250px;'>" + info + "</div></html>";
        }
        setToolTipText(info);
    }

    /**
     * Generic tooltip setter for any Company (Public or Private).
     * Used by StartRoundWindow.
     */
    public void setCompanyDetailsTooltip(Company c) {
        if (c == null) {
            setToolTipText(null);
            return;
        }
        String info = c.getInfoText();
        if (info != null) {
            info = info.replaceAll("(?i)^<html>|</html>$", "");
            info = "<html><div style='width: 250px;'>" + info + "</div></html>";
        }
        setToolTipText(info);
    }

    private boolean isShortCertificate(Certificate cert) {
        if (cert == null)
            return false;

        // 1. Try Reflection (Safest if there's an explicit flag)
        try {
            java.lang.reflect.Method m = cert.getClass().getMethod("isShort");
            if (m != null) {
                return Boolean.TRUE.equals(m.invoke(cert));
            }
        } catch (Exception e) {
        }

        // 2. Fallbacks (Naming conventions)
        if (cert.getClass().getSimpleName().contains("Short"))
            return true;
        if (cert.getId() != null && cert.getId().toLowerCase().contains("short"))
            return true;

        return false;
    }

    public String getUniqueId() {
        if (associatedCompany != null) {
            return associatedCompany.getId();
        }
        if (!certificates.isEmpty()) {
            net.sf.rails.game.financial.Certificate cert = certificates.get(0);
            if (cert instanceof net.sf.rails.game.financial.PublicCertificate) {
                return ((net.sf.rails.game.financial.PublicCertificate) cert).getCompany().getId();
            } else if (cert instanceof net.sf.rails.game.PrivateCompany) {
                return ((net.sf.rails.game.PrivateCompany) cert).getId();
            }
            return cert.getId();
        }
        if (!privates.isEmpty()) {
            return privates.get(0).getId();
        }
        if (!trains.isEmpty()) {
            return trains.get(0).getName();
        }

        // Safety Fallback: Use Swing Name if manually set (e.g. by GameStatus)
        String name = getName();
        if (name != null && !name.isEmpty()) {
            return name;
        }

        return null;
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);

        /*
         * java.awt.Color stripeColor = null;
         * boolean isPresident = false;
         * 
         * if (associatedCompany instanceof PublicCompany) {
         * PublicCompany pc = (PublicCompany) associatedCompany;
         * stripeColor = pc.getBgColour();
         * 
         * if (!pc.hasStockPrice()) {
         * isPresident = true;
         * } else if (customLabel != null) {
         * String plain = customLabel.replaceAll("\\<.*?\\>", "").trim();
         * if (plain.endsWith("P") || plain.equals("Owner") || plain.contains("%P")) {
         * isPresident = true;
         * }
         * }
         * } else {
         * for (Certificate cert : certificates) {
         * if (cert instanceof PublicCertificate) {
         * PublicCertificate pc = (PublicCertificate) cert;
         * if (pc.getCompany() != null) {
         * stripeColor = pc.getCompany().getBgColour();
         * if (pc.getCompany().getPresidentsShare() != null
         * && pc.getCompany().getPresidentsShare().equals(pc)) {
         * isPresident = true;
         * } else if (pc.getShare() >= 20) {
         * isPresident = true;
         * }
         * }
         * break;
         * }
         * }
         * }
         * 
         * if (stripeColor != null) {
         * java.awt.Insets insets = getInsets();
         * int innerY = insets.top;
         * int innerWidth = getWidth() - insets.left - insets.right;
         * int innerHeight = getHeight() - insets.top - insets.bottom;
         * 
         * int stripeWidth = isPresident ? (int) (innerWidth * 0.20) : (int) (innerWidth
         * * 0.10);
         * int stripeX = getWidth() - insets.right - stripeWidth;
         * 
         * g.setColor(stripeColor);
         * g.fillRect(stripeX, innerY, stripeWidth, innerHeight);
         * 
         * g.setColor(java.awt.Color.BLACK);
         * g.drawLine(stripeX, innerY, stripeX, innerY + innerHeight - 1);
         * }
         */
    }

    private Color getCertColor(Certificate cert) {

        if (cert != null) {
            String id = cert.getId();
            String strRep = cert.toString();

            // Aggressive failsafe: Check ID, class name, and string representation
            if ((id != null && id.contains("_SHORT_")) ||
                    (strRep != null && strRep.contains("Short "))) {
                log.info("in PINK");

                return Color.PINK;
            }
        }

        if (cert instanceof PublicCertificate) {
            PublicCertificate pubCert = (PublicCertificate) cert;
            PublicCompany pc = pubCert.getCompany();
            if (pc != null) {
                return pc.getBgColour();
            }
        }
        return null;
    }

}

package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.OperatingRound;

public class LinearRoundTracker extends JComponent {
    private static final long serialVersionUID = 1L;

    // External Dependencies
    private final GameUIManager gameUIManager;
    private final PublicCompany[] companies;

    // --- STATIC PERSISTENCE (The Fix) ---
    // These must be static to survive the component being destroyed/re-created
    private static double persistentTrainX = -1;

    // Animation State
    private double currentTrainX = -1;
    private double targetTrainX = -1;

    // Cached Data
    private List<PublicCompany> cachedTimeline = new ArrayList<>();
    private int cachedActiveIndex = -1;

    // These must be static to survive the component being destroyed/re-created
    private static double persistentTrainIndex = -1;
    private static List<PublicCompany> persistentTimeline = new ArrayList<>();
    private static int persistentActiveIndex = -1;

    // Animation State
    private double currentTrainIndex = -1;
    private double targetTrainIndex = -1;
    private final Timer animTimer;

    // Geometry
    private final int margin = 40;
    private int step = 40;
    private int r = 13;

    private void rebuildTimeline() {
        if (gameUIManager == null || gameUIManager.getGameManager() == null)
            return;

        // --- STEP 0: RESTORE FROM STATIC MEMORY ---
        // If this is a new component (empty list) but we have history, load it.
        // This ensures !cachedTimeline.isEmpty() is TRUE for the freeze check below.
        if (cachedTimeline.isEmpty() && !persistentTimeline.isEmpty()) {
            cachedTimeline.addAll(persistentTimeline);
            cachedActiveIndex = persistentActiveIndex;
        }

        try {
            Object roundObj = gameUIManager.getGameManager().getCurrentRound();
            String roundName = roundObj.getClass().getSimpleName();

            org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LinearRoundTracker.class);

            // --- STEP 1: DETECT SPECIAL ROUNDS (FREEZE) ---
            if (roundName.contains("Prussian") || roundName.contains("Formation") || roundName.contains("Auction")
                    || roundName.contains("ShareSelling")) {
                if (!cachedTimeline.isEmpty()) {
                    return; // FREEZE SUCCESSFUL: We kept the restored static data
                } else {
                }
            } else {
            }

            // --- STEP 2: STANDARD REBUILD ---
            cachedTimeline.clear();
            cachedActiveIndex = -1;
            PublicCompany activeComp = null;

            boolean isOperatingType = false;

            if (roundObj instanceof OperatingRound) {
                // Operating Round: Use Engine Order
                OperatingRound or = (OperatingRound) roundObj;
                List<PublicCompany> sorted = or.getOperatingCompanies();
                if (sorted != null)
                    cachedTimeline.addAll(sorted);
                activeComp = or.getOperatingCompany();
                isOperatingType = true;
            } else {
                // Reflection fallback for 1817 M&A rounds or other custom rounds
                try {
                    java.lang.reflect.Method getComps = roundObj.getClass().getMethod("getOperatingCompanies");
                    @SuppressWarnings("unchecked")
                    List<PublicCompany> sorted = (List<PublicCompany>) getComps.invoke(roundObj);
                    if (sorted != null && !sorted.isEmpty()) {
                        cachedTimeline.addAll(sorted);
                        isOperatingType = true;
                    }

                    java.lang.reflect.Method getComp = roundObj.getClass().getMethod("getOperatingCompany");
                    activeComp = (PublicCompany) getComp.invoke(roundObj);
                } catch (Exception e) {
                    // Not an operating-type round
                }
            }

            if (!isOperatingType) {
                // Stock Round: Predict Order

                List<PublicCompany> activeList = new ArrayList<>();

                for (PublicCompany c : companies) {
                    if (c.isClosed())
                        continue;

                    boolean isEffectivelyActive = c.hasFloated() || (c.getPresidentsShare() != null
                            && c.getPresidentsShare().getOwner() instanceof net.sf.rails.game.Player);

                    if (isEffectivelyActive) {
                        activeList.add(c);
                    }
                }

                // Delegate sorting to the single source of truth in the GameManager
                activeList = gameUIManager.getGameManager().getCompaniesInDisplayOrder(activeList);
                cachedTimeline.addAll(activeList);

            }

            // Generic fallback for activeComp if reflection failed but GUI actions exist
            // CRITICAL FIX: Only run this during actual operating rounds to prevent the
            // train from spuriously jumping to companies during Stock Round IPOs.
            if (isOperatingType && activeComp == null && gameUIManager.getGameManager().getPossibleActions() != null) {
                for (rails.game.action.PossibleAction pa : gameUIManager.getGameManager().getPossibleActions()
                        .getList()) {
                    if (pa instanceof rails.game.action.GuiTargetedAction) {
                        net.sf.rails.game.state.Owner actor = ((rails.game.action.GuiTargetedAction) pa).getActor();
                        if (actor instanceof PublicCompany) {
                            activeComp = (PublicCompany) actor;
                            break;
                        }
                    }
                }
            }

            // Find Index
            if (activeComp != null) {
                for (int i = 0; i < cachedTimeline.size(); i++) {
                    if (cachedTimeline.get(i).equals(activeComp)) {
                        cachedActiveIndex = i;
                        break;
                    }
                }
            }

            // --- STEP 3: UPDATE STATIC MEMORY ---
            // Save the valid state so the next instance can restore it
            persistentTimeline.clear();
            persistentTimeline.addAll(cachedTimeline);
            persistentActiveIndex = cachedActiveIndex;

        } catch (Exception e) {
            cachedTimeline.clear();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
    }

    private void drawTrain(Graphics2D g2, int x, int y) {
        int w = 32;
        int left = x - (w / 2);
        int bottom = y;

        Color locoColor = new Color(40, 40, 40);
        Color accentColor = new Color(200, 50, 50);

        // Wheels
        g2.setColor(Color.BLACK);
        int wheelSize = 8;
        for (int i = 0; i < 3; i++) {
            g2.fillOval(left + 2 + (i * 9), bottom - wheelSize + 2, wheelSize, wheelSize);
        }

        // Chassis
        GeneralPath body = new GeneralPath();
        body.moveTo(left, bottom - 4);
        body.lineTo(left + w, bottom - 4);
        body.lineTo(left + w, bottom - 12);
        body.lineTo(left + 8, bottom - 12);
        body.lineTo(left + 8, bottom - 18);
        body.lineTo(left, bottom - 18);
        body.closePath();

        g2.setColor(locoColor);
        g2.fill(body);

        // Cab Window
        g2.setColor(Color.WHITE);
        g2.fillRect(left + 2, bottom - 16, 4, 4);

        // Funnel
        g2.setColor(locoColor);
        g2.fillRect(left + 22, bottom - 16, 4, 6);
        g2.fillRect(left + 21, bottom - 18, 6, 2);

        // Cowcatcher
        g2.setColor(accentColor);
        GeneralPath cow = new GeneralPath();
        cow.moveTo(left + w, bottom - 4);
        cow.lineTo(left + w + 4, bottom);
        cow.lineTo(left + w, bottom);
        cow.closePath();
        g2.fill(cow);

        // Roof Accent
        g2.fillRect(left - 1, bottom - 19, 10, 2);
    }

    public LinearRoundTracker(GameUIManager gameUIManager, PublicCompany[] companies) {
        super();
        this.gameUIManager = gameUIManager;
        this.companies = companies;

        setOpaque(false);

        // 1. Restore Train Position
        if (persistentTrainIndex != -1) {
            this.currentTrainIndex = persistentTrainIndex;
            this.targetTrainIndex = persistentTrainIndex;
        }

        // Initialize Animation Timer (60 FPS)
        animTimer = new Timer(16, e -> stepAnimation());
    }

    public void updateState() {
        // 1. Rebuild Timeline (Calculates new target)
        rebuildTimeline();

        // Physical layout calculation deferred to paintComponent.
        double newTargetIndex = -1;
        if (cachedActiveIndex != -1) {
            newTargetIndex = cachedActiveIndex;
        }

        // 2. Initial Placement (if brand new and no history)
        if (currentTrainIndex == -1 && newTargetIndex != -1) {
            currentTrainIndex = newTargetIndex;
            targetTrainIndex = newTargetIndex;
            persistentTrainIndex = newTargetIndex;
        }

        // 3. Trigger Animation
        if (newTargetIndex != -1) {
            if (Math.abs(newTargetIndex - targetTrainIndex) > 0.01) {
                targetTrainIndex = newTargetIndex;
                if (!animTimer.isRunning()) {
                    animTimer.start();
                }
            }
        }

        repaint();
    }

    private void stepAnimation() {
        double distance = targetTrainIndex - currentTrainIndex;

        if (Math.abs(distance) < 0.01) {
            currentTrainIndex = targetTrainIndex;
            animTimer.stop();
        } else {
            double stepMove = distance * 0.1;
            if (Math.abs(stepMove) < 0.02) {
                stepMove = Math.signum(distance) * 0.02;
            }
            currentTrainIndex += stepMove;
        }

        // Sync Static
        persistentTrainIndex = currentTrainIndex;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (cachedTimeline.isEmpty())
            return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate dynamic step based on actual width at rendering time
        int numNodes = cachedTimeline.size();
        int effectiveStep = 40; // Default
        if (numNodes > 1) {
            int availableWidth = getWidth() - (margin * 2);
            if (availableWidth > 0) {
                int requiredWidth = (numNodes - 1) * 40;
                if (requiredWidth > availableWidth) {
                    effectiveStep = availableWidth / (numNodes - 1);
                }
            }
        }

        // Calculate dynamic radius based on the current effective step
        int effectiveR = Math.max(8, (int) (13 * (effectiveStep / 40.0)));
        int centerY = getHeight() - 3 - effectiveR;

        // 1. Connector Line
        int lineLength = (cachedTimeline.size() > 1) ? (cachedTimeline.size() - 1) * effectiveStep : 0;
        g2.setColor(Color.GRAY);
        g2.setStroke(new BasicStroke(2));
        if (cachedTimeline.size() > 1) {
            g2.draw(new Line2D.Double(margin, centerY, margin + lineLength, centerY));
        }

        // 2. Nodes
        for (int i = 0; i < cachedTimeline.size(); i++) {
            PublicCompany c = cachedTimeline.get(i);
            int x = margin + (i * effectiveStep);

            boolean isPast = (cachedActiveIndex != -1 && i < cachedActiveIndex);
            boolean isActive = (i == cachedActiveIndex);

            Ellipse2D circle = new Ellipse2D.Double(x - effectiveR, centerY - effectiveR, 2 * effectiveR,
                    2 * effectiveR);

            if (isPast)
                g2.setColor(Color.LIGHT_GRAY);
            else
                g2.setColor(c.getBgColour());
            g2.fill(circle);

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(isActive ? 3 : 1));
            g2.draw(circle);

            String code = c.getId();
            // Scale font size slightly if nodes are small
            float fontSize = (effectiveR < 10) ? 8f : 10f;
            g2.setFont(getFont().deriveFont(Font.BOLD, fontSize));
            FontMetrics fm = g2.getFontMetrics();

            if (isPast)
                g2.setColor(Color.DARK_GRAY);
            else
                g2.setColor(c.getFgColour());

            g2.drawString(code, x - fm.stringWidth(code) / 2, centerY + fm.getAscent() / 2 - 2);
        }

        // 3. Train
        if (currentTrainIndex != -1 && cachedActiveIndex != -1) {
            int trainX = margin + (int) (currentTrainIndex * effectiveStep);
            drawTrain(g2, trainX, centerY - effectiveR - 1);
        }
    }

}
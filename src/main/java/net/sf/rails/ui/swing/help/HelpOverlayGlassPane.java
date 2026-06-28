
/**
 * ============================================================================
 * NET.SF.RAILS HELP ARCHITECTURE PIPELINE OVERVIEW MAP [cite: 2026-05-19]
 * ============================================================================
 * This class serves as the strictly modal spatial dimming mask overlay [cite: 2026-06-13].
 * It punches precise visual holes over active UI dashboard assets and consumes 
 * non-control click inputs to protect user attention focus states.
 * * --- CORE PIPELINE CALL STACK & INTER-FILE COLLABORATORS ---
 * * 1. STATE DRIVER: GameManager.java (net.sf.rails.game)
 * -> Controls engine state transitions via 'toggleHelpMode()'. Shifts the 
 * authoritative mode enum to EngineMode.HELP.
 * * 2. CENTRAL SWITCHBOARD ROUTER: GameUIManager.java [cite: 2026-06-28]
 * -> Intercepts engine changes in 'applyEngineMode(EngineMode.HELP)' [cite: 2026-06-28].
 * -> Instantiates this 'HelpOverlayGlassPane' component.
 * -> Mounts it directly onto the top window stacks:
 * - statusWindow.getRootPane().setGlassPane(statusHelpPane);
 * - orWindow.getRootPane().setGlassPane(orHelpPane); [cite: 2026-06-28]
 * -> Spawns and focuses the side cheatsheet companion frame: HelpTextWindow.java.
 * * 3. SPATIAL POSITION DATA GATHERERS (Going Backwards):
 * When shown, the UI manager calls down to individual layout view panels to 
 * gather component geometries and populate help tooltips [cite: 2026-06-28]:
 * * A. GameStatus.java -> Computes horizontal column coordinate matrices [cite: 2026-06-28]
 * ('compTrainsXOffset', 'compCashXOffset', etc.) via 'getColumnBounds()' [cite: 2026-06-28].
 * Translates coordinate geometry bounds upward using SwingUtilities.convertRectangle() [cite: 2026-06-28].
 * Feeds these translated locations directly into 'helpPane.addSpotlight()' [cite: 2026-06-28].
 * * B. ORPanel.java -> Scans operation contexts during Operating Rounds [cite: 2026-06-28].
 * Locates bounding dimensions of active workflow buttons ('btnTileConfirm', 
 * 'btnRevPayout'), active map phase panels, and valid hex spatial vectors 
 * derived via 'orUIManager.getMap().getHex()' [cite: 2026-06-28]. Maps them 
 * to this overlay to clear dimming zones [cite: 2026-06-28].
 * * 4. SYSTEM STATE RESOLUTION RECOVERY PIPELINE:
 * - To return to active gameplay, this panel utilizes a 'contains(x, y)' 
 * punch-through matrix block.
 * - It identifies the exact frame bounds of the 'helpButton' managed by 
 * StatusWindow.java [cite: 2026-06-28].
 * - When the mouse hovers inside that hole, 'contains()' returns false, allowing 
 * native mouse press/release states to pass cleanly down to the underlying button.
 * - Button triggers 'StatusWindow.actionPerformed()' command "HelpWindowCmd" [cite: 2026-06-28].
 * - Tells GameManager to toggle state, which triggers 'applyEngineMode(PLAY)' [cite: 2026-06-28].
 * - Calls 'glassPane.setVisible(false)' to drop this overlay [cite: 2026-06-28].
 * * --- REQUIRED WORKSPACE RESUMPTION FILE LIST ---
 * To run, modify, or debug this help subsystem loop in a new thread context, you 
 * must upload exactly these 3 local files
 * 1. net/sf/rails/ui/swing/help/HelpOverlayGlassPane.java (This file)
 * 2. net/sf/rails/ui/swing/GameUIManager.java (Propagates state and mounts panes) 
 * 3. net/sf/rails/ui/swing/GameStatus.java  (Generates spotlight boxes in status woidnwo) ]
 * 4. net/sf/rails/ui/swing/ORPanel.java (Generates spotlight boxes in or window) ]
 * status AND Or: Primary Entry Point: public void activateHelpOverlay()
 * ============================================================================
 */

package net.sf.rails.ui.swing.help;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

public class HelpOverlayGlassPane extends JComponent {
    private static final long serialVersionUID = 1L;

    public enum Type {
        ACTION, INFO
    }

    private static class Spotlight {
        Rectangle bounds;
        String text;
        Type type;
        Spotlight(Rectangle bounds, String text, Type type) {
            this.bounds = bounds;
            this.text = text;
            this.type = type;
        }

    }

    private final List<Spotlight> spotlights = new ArrayList<>();
    private Spotlight hoveredSpotlight = null;
    private Point mousePos = null;
    private final Color dimColor = new Color(0, 0, 0, 180); 

    // Caches the bounding box of the active control button to allow native event pass-through [cite: 2025-11-08]
    private Rectangle cutoutBounds = null; 

    @Override
    public boolean contains(int x, int y) {
        // If coordinates match the visual hole, pass event evaluation downstream to the button below [cite: 2025-11-08]
        if (cutoutBounds != null && cutoutBounds.contains(x, y)) {
            return false; 
        }
        return super.contains(x, y);
    }

    public HelpOverlayGlassPane() {
        setOpaque(false);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // --- START FIX ---
                // Strictly Modal: Clicks outside the active button cutout are intentionally 
                // swallowed entirely to prevent accidental dismissals.
                // --- END FIX ---
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mousePos = e.getPoint();
                Spotlight found = null;
                for (Spotlight s : spotlights) {
                    if (s.bounds.contains(mousePos)) {
                        found = s;
                        break;
                    }
                }
                
                if (hoveredSpotlight != found || found != null) {
                    hoveredSpotlight = found;
                    repaint();
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        
        // Swallow mouse wheel events so the user can't zoom the map while in Help Mode
        addMouseWheelListener(e -> {});

        // Escape key dismissal remains active as a power-user fallback shortcut
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeHelp");
        getActionMap().put("closeHelp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                clearSpotlights();
            }
        });
    }

    public void clearSpotlights() {
        spotlights.clear();
        hoveredSpotlight = null;
        repaint();
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!isVisible()) return;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Draw the Dimming Layer with Spotlight and Control Button cutouts [cite: 2025-11-08]
        Area background = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
        for (Spotlight s : spotlights) {
            background.subtract(new Area(s.bounds));
        }

        cutoutBounds = null; // Reset tracking cache for this frame step
        
        Window ancestor = SwingUtilities.getWindowAncestor(this);
        if (ancestor instanceof JFrame) {
            JFrame frame = (JFrame) ancestor;
            java.util.Queue<Component> queue = new java.util.LinkedList<>();
            queue.add(frame.getContentPane());

            while (!queue.isEmpty()) {
                Component c = queue.poll();
                if (c instanceof JButton) {
                    String text = ((JButton) c).getText();
                    if (text != null && (text.trim().equalsIgnoreCase("Help") || text.trim().equalsIgnoreCase("Play"))) {
                        Rectangle btnBounds = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), this);
                        cutoutBounds = btnBounds; // Update boundary mapping [cite: 2025-11-08]
                        background.subtract(new Area(btnBounds));
                    }
                } else if (c instanceof Container) {
                    for (Component child : ((Container) c).getComponents()) {
                        queue.add(child);
                    }
                }
            }
        }

        g2d.setColor(dimColor);
        g2d.fill(background);

        // 2. Draw Spotlight Borders
        g2d.setStroke(new BasicStroke(2));
        for (Spotlight s : spotlights) {
            if (s == hoveredSpotlight) {
                g2d.setColor(Color.WHITE); 
                g2d.drawRect(s.bounds.x - 2, s.bounds.y - 2, s.bounds.width + 4, s.bounds.height + 4);
            } else if (s.type == Type.ACTION) {
                g2d.setColor(new Color(50, 220, 100)); // High-visibility Action Green
                g2d.drawRect(s.bounds.x - 2, s.bounds.y - 2, s.bounds.width + 4, s.bounds.height + 4);
            } else {
                g2d.setColor(Color.ORANGE); // Standard Info Orange
                g2d.drawRect(s.bounds.x - 2, s.bounds.y - 2, s.bounds.width + 4, s.bounds.height + 4);
            }
        }
        // Restore permanent rendering loop across the application, but skip empty entries safely
        for (Spotlight s : spotlights) {
            if (s.text != null && !s.text.trim().isEmpty()) {
                drawAttachedBroadcastPanel(g2d, s);
            }
        }
        g2d.dispose();
    }

    /**
     * Renders descriptions directly adjacent to the illuminated spotlight rectangle asset
     * to preserve immediate cross-room broadcast visualization.
     */
    private void drawAttachedBroadcastPanel(Graphics2D g2d, Spotlight s) {
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        
        int boxWidth = 240;
        java.util.List<String> lines = wrapText(s.text, fm, boxWidth - 20);
        int lineCount = lines.size();
        int boxHeight = (lineCount * fm.getHeight()) + 16;

        // Position Logic: Target right-side deployment first, fallback to left or top/bottom if crowded
        int x = s.bounds.x + s.bounds.width + 10;
        int y = s.bounds.y + (s.bounds.height - boxHeight) / 2;

        if (x + boxWidth > getWidth()) {
            x = s.bounds.x - boxWidth - 10; // Deploy left
        }
        if (x < 10) {
            x = Math.max(10, s.bounds.x); // Fallback overlay inside horizontal boundaries
            y = s.bounds.y - boxHeight - 10; // Deploy above
            if (y < 10) {
                y = s.bounds.y + s.bounds.height + 10; // Deploy below
            }
        }
        
        // Final window boundary clamps
        if (y < 10) y = 10;
        if (y + boxHeight > getHeight() - 10) y = getHeight() - boxHeight - 10;

        RoundRectangle2D box = new RoundRectangle2D.Float(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setColor(new Color(20, 24, 30, 220)); // Soft translucent slate palette
        g2d.fill(box);
        
        g2d.setColor(s.type == Type.ACTION ? new Color(50, 220, 100) : new Color(230, 140, 40));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(box);

        g2d.setColor(Color.WHITE);
        int textY = y + fm.getAscent() + 8;
        for (String line : lines) {
            g2d.drawString(line, x + 10, textY);
            textY += fm.getHeight();
        }
    }

    private void drawHelpBubble(Graphics2D g2d, String text, Point mousePos) {
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g2d.getFontMetrics();
        
        int padding = 10;
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        int bubbleWidth = textWidth + (padding * 2);
        int bubbleHeight = textHeight + (padding * 2);
        
        int x = mousePos.x + 15;
        int y = mousePos.y - bubbleHeight - 10;
        
        if (x + bubbleWidth > getWidth()) x = getWidth() - bubbleWidth - 10;
        if (y < 0) y = mousePos.y + 25; 

        RoundRectangle2D bubble = new RoundRectangle2D.Float(x, y, bubbleWidth, bubbleHeight, 15, 15);
        g2d.setColor(new Color(30, 30, 30, 230)); 
        g2d.fill(bubble);
        g2d.setColor(Color.ORANGE);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(bubble);

        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x + padding, y + fm.getAscent() + padding);
    }

    public void addSpotlight(Rectangle bounds, String text) {
        addSpotlight(bounds, text, Type.INFO);
    }

    public void addSpotlight(Rectangle bounds, String text, Type type) {
        if (bounds != null) {
            spotlights.add(new Spotlight(bounds, text, type));
            repaint();
        }
    }


    /**
     * Renders a static, non-overlapping information block directly next to highlighted zones
     * to preserve absolute stream visibility without requiring user hovers or mouse steps.
     */
    private void drawPermanentBroadcastPanel(Graphics2D g2d, Spotlight s) {
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        
        // Wrap text to fit a standardized broadcast box width of 260px
        int boxWidth = 260;
        java.util.List<String> lines = wrapText(s.text, fm, boxWidth - 20);
        int lineCount = lines.size();
        int boxHeight = (lineCount * fm.getHeight()) + 20;

        // Position Logic: Default to placing the dashboard box to the right or left of the spotlight column bounds
        int x = s.bounds.x + s.bounds.width + 12;
        int y = s.bounds.y + (s.bounds.height - boxHeight) / 2;

        // Boundary adjustments: If pushing off the right screen limit, flip box to the left side of the asset bounds
        if (x + boxWidth > getWidth()) {
            x = s.bounds.x - boxWidth - 12;
        }
        if (y < 10) y = 10;
        if (y + boxHeight > getHeight()) y = getHeight() - boxHeight - 10;

        // Paint background panel box container
        RoundRectangle2D box = new RoundRectangle2D.Float(x, y, boxWidth, boxHeight, 10, 10);
        g2d.setColor(new Color(25, 25, 25, 245)); // High-density dark gray slate
        g2d.fill(box);
        
        // Accent border color based on function intent
        g2d.setColor(s.type == Type.ACTION ? new Color(50, 220, 100) : Color.ORANGE);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(box);

        // Print wrapped information blocks line-by-line
        g2d.setColor(Color.WHITE);
        int textY = y + fm.getAscent() + 10;
        for (String line : lines) {
            g2d.drawString(line, x + 10, textY);
            textY += fm.getHeight();
        }
    }

    /**
     * Splits long multi-line strings cleanly across word boundaries to enforce clear layout columns.
     */
    private java.util.List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        java.util.List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (fm.stringWidth(currentLine.toString() + " " + word) < maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word); // Word itself is wider than column limit bounds
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    /**
     * Renders descriptions stacked down the right edge of the window,
     * guaranteeing zero overlap over active game grid rows.
     */
    private int drawStackedBroadcastPanel(Graphics2D g2d, Spotlight s, int x, int y, int width) {
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        
        java.util.List<String> lines = wrapText(s.text, fm, width - 20);
        int lineCount = lines.size();
        int boxHeight = (lineCount * fm.getHeight()) + 16;

        // Verify we aren't clipping past bottom screen boundary
        if (y + boxHeight > getHeight() - 10) {
            return y; // Drop rendering gracefully if screen height constraints break
        }

        // Paint styled panel container with soft alpha transparency
        RoundRectangle2D box = new RoundRectangle2D.Float(x, y, width, boxHeight, 8, 8);
        g2d.setColor(new Color(20, 24, 30, 210)); // Translucent deep slate palette
        g2d.fill(box);
        
        // Match border accent to functional type taxonomy
        g2d.setColor(s.type == Type.ACTION ? new Color(50, 220, 100) : new Color(230, 140, 40));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(box);

        // Draw individual text tracks
        g2d.setColor(Color.WHITE);
        int textY = y + fm.getAscent() + 8;
        for (String line : lines) {
            g2d.drawString(line, x + 10, textY);
            textY += fm.getHeight();
        }

        // Return next layout anchor line position plus buffer gap margin
        return y + boxHeight + 10;
    }

}
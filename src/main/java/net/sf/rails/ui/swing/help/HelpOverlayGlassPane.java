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

    private static class Spotlight {
        Rectangle bounds;
        String text;

        Spotlight(Rectangle bounds, String text) {
            this.bounds = bounds;
            this.text = text;
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

    public void addSpotlight(Rectangle bounds, String text) {
        if (bounds != null) {
            spotlights.add(new Spotlight(bounds, text));
            repaint();
        }
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
            } else {
                g2d.setColor(Color.ORANGE);
                g2d.drawRect(s.bounds.x - 2, s.bounds.y - 2, s.bounds.width + 4, s.bounds.height + 4);
            }
        }

        // 3. Draw the Custom Help Bubble if hovering
        if (hoveredSpotlight != null && hoveredSpotlight.text != null && mousePos != null) {
            drawHelpBubble(g2d, hoveredSpotlight.text, mousePos);
        }

        g2d.dispose();
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
}
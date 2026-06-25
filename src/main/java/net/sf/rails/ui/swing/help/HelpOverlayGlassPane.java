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

    // Upgraded Spotlight class to hold contextual text
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
    private final Color dimColor = new Color(0, 0, 0, 180); // Slightly darker for contrast

    public HelpOverlayGlassPane() {
        setOpaque(false);

        // Comprehensive Mouse Adapter to handle clicks and hover tracking
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Exit help mode on click
                setVisible(false);
                clearSpotlights();
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
                
                // Only repaint if the hover state changed or mouse moved within a spotlight
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

        // Escape key dismissal
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

        // 1. Draw the Dimming Layer with Spotlight cutouts
        Area background = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
        for (Spotlight s : spotlights) {
            background.subtract(new Area(s.bounds));
        }
        g2d.setColor(dimColor);
        g2d.fill(background);

        // 2. Draw Spotlight Borders
        g2d.setStroke(new BasicStroke(2));
        for (Spotlight s : spotlights) {
            if (s == hoveredSpotlight) {
                g2d.setColor(Color.WHITE); // Highlight hovered spotlight
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
        
        // Position bubble slightly above and to the right of the cursor
        int x = mousePos.x + 15;
        int y = mousePos.y - bubbleHeight - 10;
        
        // Keep bubble on screen
        if (x + bubbleWidth > getWidth()) x = getWidth() - bubbleWidth - 10;
        if (y < 0) y = mousePos.y + 25; 

        // Draw Bubble Background
        RoundRectangle2D bubble = new RoundRectangle2D.Float(x, y, bubbleWidth, bubbleHeight, 15, 15);
        g2d.setColor(new Color(30, 30, 30, 230)); // Dark Gray almost opaque
        g2d.fill(bubble);
        g2d.setColor(Color.ORANGE);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(bubble);

        // Draw Text
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x + padding, y + fm.getAscent() + padding);
    }
}
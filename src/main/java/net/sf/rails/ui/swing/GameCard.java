package net.sf.rails.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.GameInfo;

public class GameCard extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(GameCard.class);

    private final GameInfo gameInfo;
    private boolean isSelected = false;

    // Visual configuration
    private static final Color NORMAL_BG = new Color(240, 240, 240);
    private static final Color SELECTED_BG = new Color(200, 220, 255);
    private static final Border NORMAL_BORDER = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1);
    private static final Border SELECTED_BORDER = BorderFactory.createLineBorder(new Color(50, 100, 200), 2);
    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 160;

    public interface GameCardListener {
        void onGameSelected(GameInfo gameInfo);
    }

    public GameCard(GameInfo gameInfo, GameCardListener listener) {
        this.gameInfo = gameInfo;

        setLayout(new BorderLayout(5, 5));
        setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        setBackground(NORMAL_BG);
        setBorder(NORMAL_BORDER);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 1. Thumbnail Image
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Attempt to load thumbnail based on game name
        String imagePath = "/images/thumbnails/" + gameInfo.getName() + ".png";
        URL imgURL = getClass().getResource(imagePath);
        if (imgURL != null) {
ImageIcon originalIcon = new ImageIcon(imgURL);
            java.awt.Image img = originalIcon.getImage();
            // Scale proportionally to fit within 110x110 bounds without distortion
            int width = originalIcon.getIconWidth();
            int height = originalIcon.getIconHeight();
            if (width > 0 && height > 0) {
                double ratio = Math.min(110.0 / width, 110.0 / height);
                int newWidth = Math.max(1, (int) (width * ratio));
                int newHeight = Math.max(1, (int) (height * ratio));
                java.awt.Image scaledImg = img.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImg));
            } else {
                imageLabel.setIcon(originalIcon);
            }
            
        }
        add(imageLabel, BorderLayout.CENTER);

        // 2. Game Title
        JLabel titleLabel = new JLabel(gameInfo.getName());
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(titleLabel, BorderLayout.SOUTH);

        // 3. Click Listener
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (listener != null) {
                    listener.onGameSelected(gameInfo);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected) {
                    setBackground(new Color(225, 225, 225));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected) {
                    setBackground(NORMAL_BG);
                }
            }
        });
    }

    public void setSelectedState(boolean selected) {
        this.isSelected = selected;
        setBackground(selected ? SELECTED_BG : NORMAL_BG);
        setBorder(selected ? SELECTED_BORDER : NORMAL_BORDER);
    }

    public GameInfo getGameInfo() {
        return gameInfo;
    }
}
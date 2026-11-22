package net.sf.rails.ui.swing.help;

import javax.swing.*;
import java.awt.*;

public class HelpOverlayTest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Help Overlay Break Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLayout(null); // Absolute positioning for strict bounds testing

            // Mock active OR Panel Button
            JButton buildBtn = new JButton("Build Track");
            buildBtn.setBounds(50, 500, 150, 40);
            frame.add(buildBtn);

            // Mock Hex on the Map
            JPanel mockHex = new JPanel();
            mockHex.setBackground(Color.GREEN);
            mockHex.setBounds(300, 200, 100, 100);
            frame.add(mockHex);

            HelpOverlayGlassPane glassPane = new HelpOverlayGlassPane();
            frame.setGlassPane(glassPane);
            
            frame.setVisible(true);

            // Test 1: Standard UI components
glassPane.addSpotlight(buildBtn.getBounds(), "Build Button Tooltip");
glassPane.addSpotlight(mockHex.getBounds(), "Hex Tooltip");
        // Test 2: Breaking parameters (Negative, Massive, Overlapping, Null)
        try {
            glassPane.addSpotlight(new Rectangle(-50, -50, 100, 100), "Off-screen neg"); // Off-screen negative
            glassPane.addSpotlight(new Rectangle(2000, 2000, 50, 50), "Off-screen pos"); // Off-screen positive
            glassPane.addSpotlight(new Rectangle(280, 180, 140, 140), "Overlap"); // Overlapping the hex
            glassPane.addSpotlight(null, "Null check"); // Null safety check
        } catch (Exception e) {
            System.err.println("Subtractor geometry broken: " + e.getMessage());
        }

            glassPane.setVisible(true);
        });
    }
}
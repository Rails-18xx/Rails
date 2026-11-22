package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// --- DELETE ---
// import java.awt.event.ComponentAdapter;
// import java.awt.event.ComponentEvent;
// --- END DELETE ---

import javax.swing.*;

import net.sf.rails.game.financial.StockMarket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import net.sf.rails.javafx.stockchart.FXHexStockChart;
import net.sf.rails.javafx.stockchart.FXStockChart;

/**
 * Wrapper around the JavaFX version of the StockChartWindow
 */
public class StockChartWindow extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(StockChartWindow.class);
    
    private GameUIManager gameUIManager;

    public StockChartWindow(GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
        
        final JFXPanel fxPanel = new JFXPanel();
        add(fxPanel);
        setTitle("Rails: Stock Chart");

        StockMarket.ChartType type = gameUIManager.getRoot().getStockMarket().getStockChartType();
        if (type == StockMarket.ChartType.LINEAR) {
            setPreferredSize(new Dimension(600, 150));
        } else {
            setPreferredSize(new Dimension(600, 400));
        }

        setVisible(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        final JFrame frame = this;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gameUIManager.uncheckMenuItemBox(StatusWindow.REPORT_CMD);
                frame.dispose();
            }
        });


        Platform.runLater(() -> {

            javafx.scene.Parent chartRoot;
            if (type == StockMarket.ChartType.HEXAGONAL) {
                log.info("Initializing Hexagonal Stock Chart for 1837.");
chartRoot = new net.sf.rails.javafx.stockchart.FXHexStockChart(gameUIManager);
            } else {
                chartRoot = new FXStockChart(gameUIManager);
            }
            Scene scene = new Scene(chartRoot);


            fxPanel.setScene(scene);
            
            // This is correct: Restore position after JavaFX is ready
            gameUIManager.packAndApplySizing(frame);
        });
    }

    /**
     * Toggles an independent help overlay layer directly across the stock market chart window bounds.
     * Highlights key trading zones matching the exact design taxonomy.
     */
    public void toggleHelpOverlayState(boolean active) {
        Component glass = getGlassPane();
        net.sf.rails.ui.swing.help.HelpOverlayGlassPane helpPane;

        if (glass instanceof net.sf.rails.ui.swing.help.HelpOverlayGlassPane) {
            helpPane = (net.sf.rails.ui.swing.help.HelpOverlayGlassPane) glass;
        } else {
            helpPane = new net.sf.rails.ui.swing.help.HelpOverlayGlassPane();
            setGlassPane(helpPane);
        }

        helpPane.clearSpotlights();

if (active && gameUIManager != null && gameUIManager.getGameManager() != null) {
            // 1. Authoritative lookup of the currently operating company matching GameStatus
            net.sf.rails.game.PublicCompany activeComp = null;
            net.sf.rails.game.round.RoundFacade currentRound = gameUIManager.getGameManager().getCurrentRound();
            if (currentRound instanceof net.sf.rails.game.OperatingRound) {
                activeComp = ((net.sf.rails.game.OperatingRound) currentRound).getOperatingCompany();
            }

            final net.sf.rails.game.PublicCompany targetComp = activeComp;
            
            if (targetComp != null && targetComp.getCurrentSpace() != null) {
                // 2. Fetch the JavaFX panel's internal scene graph root container safely
                final JFXPanel activeFxPanel = (JFXPanel) getContentPane().getComponent(0);
                
                Platform.runLater(() -> {
                    javafx.scene.Scene scene = activeFxPanel.getScene();
                    if (scene != null && scene.getRoot() != null) {
                        // 3. Scan the JavaFX Node graph to find the FXStockField instance matching our target space
                        javafx.scene.Node foundNode = findFieldNodeForSpace(scene.getRoot(), targetComp.getCurrentSpace());
                        
                        if (foundNode != null) {
                            // Determine bounds relative to the JFXPanel scene viewport coordinates
                            javafx.geometry.Bounds sceneBounds = foundNode.localToScene(foundNode.getBoundsInLocal());
                            
                            // 4. Map scene pixels 1:1 back into standard Swing layout rectangle bounds
                            final Rectangle elementBounds = new Rectangle(
                                (int) Math.round(sceneBounds.getMinX()),
                                (int) Math.round(sceneBounds.getMinY()),
                                (int) Math.round(sceneBounds.getWidth()),
                                (int) Math.round(sceneBounds.getHeight())
                            );
                            
                            // Re-route to Swing thread to safely append the isolated highlight cutout frame
                            SwingUtilities.invokeLater(() -> {
                                Rectangle paneBounds = SwingUtilities.convertRectangle(activeFxPanel, elementBounds, helpPane);
                               helpPane.addSpotlight(paneBounds, "", net.sf.rails.ui.swing.help.HelpOverlayGlassPane.Type.INFO);
                            });
                        }
                    }
                });
            } else {
                // Fallback layout if no company is active (e.g. during specific setup or early rounds)
                Rectangle marketBounds = new Rectangle(10, 30, getWidth() - 20, getHeight() - 40);
helpPane.addSpotlight(marketBounds, "", net.sf.rails.ui.swing.help.HelpOverlayGlassPane.Type.INFO);
            }
        }

        helpPane.setVisible(active);
        helpPane.repaint();
    }

    /**
     * Traverses the JavaFX scene tree container branch recursively to look up the active FXStockField node.
     */
    private javafx.scene.Node findFieldNodeForSpace(javafx.scene.Parent root, net.sf.rails.game.financial.StockSpace targetSpace) {
        for (javafx.scene.Node node : root.getChildrenUnmodifiable()) {
            if (node instanceof net.sf.rails.javafx.stockchart.FXStockField) {
                // Look up internal model state
                net.sf.rails.javafx.stockchart.FXStockField field = (net.sf.rails.javafx.stockchart.FXStockField) node;
                if (field.getObservable() == targetSpace) {
                    return field;
                }
            } else if (node instanceof javafx.scene.Parent) {
                javafx.scene.Node found = findFieldNodeForSpace((javafx.scene.Parent) node, targetSpace);
                if (found != null) return found;
            }
        }
        return null;
    }
}
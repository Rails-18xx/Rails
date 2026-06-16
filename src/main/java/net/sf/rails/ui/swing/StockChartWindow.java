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
}
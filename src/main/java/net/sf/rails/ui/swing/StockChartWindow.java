package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import net.sf.rails.javafx.stockchart.FXStockChart;

/**
 * Wrapper around the JavaFX version of the StockChartWindow
 */
public class StockChartWindow extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(StockChartWindow.class);

    public StockChartWindow(GameUIManager gameUIManager) {
        final JFXPanel fxPanel = new JFXPanel();
        add(fxPanel);
        setTitle("Rails: Stock Chart");
        setPreferredSize(new Dimension(600, 400));
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
            Scene scene = new Scene(new FXStockChart(gameUIManager));
            fxPanel.setScene(scene);
            frame.pack();
        });
    }

}

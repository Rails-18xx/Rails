package net.sf.rails.javafx.stockchart;

import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * A label contained inside a {@link FXStockChart} component.
 * The labels are located in the first row and the first column of a {@link FXStockChart} component
 */
public class FXStockChartLabel extends BorderPane {
    private final String label;

    public FXStockChartLabel(String label) {
        super();

        this.label = label;

        populate();
    }

    private void populate() {
        setStyle("-fx-background-color: rgb(230,230,230);");

        Text text = new Text(label);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setFill(Color.BLACK);

        setCenter(text);
    }
}

package net.sf.rails.javafx.stockchart;

import javafx.scene.layout.Pane;

/**
 * An empty stack-chart field, which is outside the used stock range for the game
 */
public class FXEmptyStockField extends Pane {
    public FXEmptyStockField() {
        super();

        initialize();
    }

    private void initialize() {
        setStyle("-fx-background-color: rgb(200,200,200);");
    }
}

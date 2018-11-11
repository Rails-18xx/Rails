package net.sf.rails.javafx.stockchart;

import javafx.beans.binding.Bindings;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

/**
 * A token inside a {@link FXStockField} component
 */
public class FXStockToken extends StackPane {
    private Color backgroundColor;
    private Color foregroundColor;

    private String name;

    public FXStockToken(Color fc, Color bc, String name) {
        super();

        this.backgroundColor = bc;
        this.foregroundColor = fc;

        this.name = name;

        this.populate();
    }

    private void populate() {
        // fill the background
        Circle circle = new Circle();

        circle.centerXProperty().bind(Bindings.divide(widthProperty(), 2));
        circle.centerYProperty().bind(Bindings.divide(heightProperty(), 2));
        circle.radiusProperty().bind(Bindings.divide(widthProperty(), 2));

        circle.setFill(backgroundColor);

        Text text = new Text(name);

        text.setStroke(foregroundColor);
        text.setBoundsType(TextBoundsType.VISUAL);

        getChildren().addAll(circle, text);
    }
}

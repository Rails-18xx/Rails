package net.sf.rails.javafx.stockchart;

import com.google.common.collect.Lists;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Observer;
import net.sf.rails.javafx.ColorUtils;
import net.sf.rails.util.Util;

import java.util.ArrayList;
import java.util.List;


/**
 * A populated stock field inside a {@link FXStockChart} component
 */
public class FXStockField extends StackPane implements Observer {
    private final StockSpace model;
private VBox tokenContainer;

    
    
    public FXStockField(StockSpace model) {
        super();

        this.model = model;

        initialize();
        populate();
    }

    private void initialize() {
       setStyle("-fx-background-color: " + ColorUtils.toRGBString(model.getColour()));
        
        // Initialize the container for tokens
        tokenContainer = new VBox(2); // 2px spacing between tokens
        tokenContainer.setAlignment(Pos.CENTER);

        if ((model.isStart())&& model.isLeftOfLedge() ) {
            setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1,5,1,1))));
        } else {
            if (model.isStart()) {
                setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
            }
            if (model.isBelowLedge()) {
                setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(4, 0, 0, 0))));
            }

            if (model.isLeftOfLedge()) {
                setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0, 4, 0, 0))));
            }
        }

        model.addObserver(this);
    }

    /**
     * Create the stock price text for this stock field
     *
     * @return The stock price text
     */
    private Text createStockSpacePrice() {
        
String displayValue;

    // 1. Check if a custom label was successfully parsed from XML
    if (model.getLabel() != null && !model.getLabel().trim().isEmpty()) {
        displayValue = model.getLabel();
    } 
    // 2. Fallback: If it is a special illiquid zone, auto-capitalize the type name 
    else if ("liquidation".equals(model.getType().getName()) || "acquisition".equals(model.getType().getName())) {
        String typeName = model.getType().getName();
        displayValue = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
    } 
    // 3. Default: Show the numerical price
    else {
        displayValue = Integer.toString(model.getPrice());
    }

    Text text = new Text(displayValue);
        text.setFill(Util.isDark(model.getColour()) ? Color.WHITE : Color.BLACK);

        return text;
    }

   /**
* Create the token objects based on the model of this stock field
*
* @return A list containing all tokens
*/

private List<FXStockToken> createTokens() {
List<FXStockToken> tokens = new ArrayList<>();

    if (model.hasTokens()) {
        List<PublicCompany> publicCompanies = Lists.reverse(model.getTokens());
        boolean isLinear = model.getParent().getStockChartType() == net.sf.rails.game.financial.StockMarket.ChartType.LINEAR;

        for (int companyIndex = 0; companyIndex < publicCompanies.size(); companyIndex++) {
            PublicCompany publicCompany = publicCompanies.get(companyIndex);

            FXStockToken token = new FXStockToken(
                    ColorUtils.toColor(publicCompany.getFgColour()),
                    ColorUtils.toColor(publicCompany.getBgColour()),
                    publicCompany.getId()
            );

            if (isLinear) {
                // 1817 Linear Layout
                token.setStyle("-fx-border-color: black; -fx-border-width: 0;");
                DoubleBinding diameter = Bindings.multiply(Bindings.min(widthProperty(), heightProperty()), 0.65);

                token.minWidthProperty().bind(diameter);
                token.prefWidthProperty().bind(diameter);
                token.maxWidthProperty().bind(diameter);

                token.minHeightProperty().bind(diameter);
                token.prefHeightProperty().bind(diameter);
                token.maxHeightProperty().bind(diameter);
            } else {
                // Standard 2D Overlapping Layout
                DoubleBinding diameter = Bindings.multiply(Bindings.min(widthProperty(), heightProperty()), 0.5);

                token.minWidthProperty().bind(diameter);
                token.prefWidthProperty().bind(diameter);
                token.maxWidthProperty().bind(diameter);

                token.minHeightProperty().bind(diameter);
                token.prefHeightProperty().bind(diameter);
                token.maxHeightProperty().bind(diameter);

                StackPane.setAlignment(token, Pos.TOP_RIGHT);
                StackPane.setMargin(token, new Insets(5 + (companyIndex * 5), 5, 5, 5));
            }

            tokens.add(token);
        }
    }

    return tokens;
}

public void populate() {
    getChildren().clear();

    if (model.getParent().getStockChartType() == net.sf.rails.game.financial.StockMarket.ChartType.LINEAR) {
        // Linear Market: Use centered VBox to stack tokens cleanly without overlap
        VBox layout = new VBox(5);
        layout.setAlignment(Pos.CENTER);
        
                   layout.setAlignment(Pos.TOP_CENTER);
            layout.setPadding(new Insets(5, 0, 5, 0)); // Add padding to keep text slightly off the top edge

        layout.getChildren().add(createStockSpacePrice());
        
        tokenContainer.getChildren().clear();
        tokenContainer.getChildren().addAll(createTokens());
        layout.getChildren().add(tokenContainer);
        
        getChildren().add(layout);
    } else {
        // Standard Market: Add directly to the StackPane root to allow margins and overlaps
        getChildren().add(createStockSpacePrice());
        getChildren().addAll(createTokens());
    }
}


    

    @Override
    public void update(String text) {
        Platform.runLater(this::populate);
    }

    @Override
    public Observable getObservable() {
        return model;
    }
}

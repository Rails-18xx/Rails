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

    public FXStockField(StockSpace model) {
        super();

        this.model = model;

        initialize();
        populate();
    }

    private void initialize() {
        setStyle("-fx-background-color: " + ColorUtils.toRGBString(model.getColour()));

        if (model.isStart()) {
            setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        }

        model.addObserver(this);
    }

    /**
     * Create the stock price text for this stock field
     *
     * @return The stock price text
     */
    private Text createStockSpacePrice() {
        Text text = new Text(Integer.toString(model.getPrice()));

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

            for (int companyIndex = 0; companyIndex < publicCompanies.size(); companyIndex++) {
                PublicCompany publicCompany = publicCompanies.get(companyIndex);

                FXStockToken token = new FXStockToken(
                        ColorUtils.toColor(publicCompany.getFgColour()),
                        ColorUtils.toColor(publicCompany.getBgColour()),
                        publicCompany.getId()
                );

                DoubleBinding diameter = Bindings.multiply(Bindings.min(widthProperty(), heightProperty()), 0.5);

                token.minWidthProperty().bind(diameter);
                token.prefWidthProperty().bind(diameter);
                token.maxWidthProperty().bind(diameter);

                token.minHeightProperty().bind(diameter);
                token.prefHeightProperty().bind(diameter);
                token.maxHeightProperty().bind(diameter);

                StackPane.setAlignment(token, Pos.TOP_RIGHT);
                StackPane.setMargin(token, new Insets(5 + (companyIndex * 5), 5, 5, 5));

                tokens.add(token);
            }
        }

        return tokens;
    }

    public void populate() {
        getChildren().clear();
        getChildren().add(createStockSpacePrice());
        getChildren().addAll(createTokens());
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

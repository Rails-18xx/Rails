package net.sf.rails.javafx.stockchart;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.ui.swing.GameUIManager;

/**
 * The stock-chart component, containing a grid with all stock fields composing the stock-chart
 */
public class FXStockChart extends GridPane {
    /**
     * The stock fields of the model
     */
    private final StockSpace[][] market;

    /**
     * Constructor
     *
     * @param gameUIManager The ui manager
     */
    public FXStockChart(GameUIManager gameUIManager) {
        super();

        this.market = gameUIManager.getRoot().getStockMarket().getStockChart();

        initialize();
        populateStockPanel();
    }

    /**
     * Initializes the component
     */
    private void initialize() {
        setHgap(1);
        setVgap(1);

        setStyle("-fx-background-color: black;");
    }

    /**
     * Populates the stock field grid
     */
    private void populateStockPanel() {
        // initialize the top left corner
        FXStockChartLabel corner = new FXStockChartLabel("");

        GridPane.setHgrow(corner, Priority.ALWAYS);
        GridPane.setVgrow(corner, Priority.ALWAYS);

        getColumnConstraints().add(createColumn(1));
        getRowConstraints().add(createRow(1));

        add(corner, 0, 0);

        // initialize the header column
        for (int row = 0; row < market.length; row++) {
            FXStockChartLabel l = new FXStockChartLabel(Integer.toString(row + 1));

            GridPane.setHgrow(l, Priority.ALWAYS);
            GridPane.setVgrow(l, Priority.ALWAYS);

            add(l, 0, row + 1);
            getRowConstraints().add(createRow(2));
        }

        // initialize the header row
        for (int column = 0; column < market[0].length; column++) {
            FXStockChartLabel l = new FXStockChartLabel(Character.toString((char) ('A' + column)));

            GridPane.setHgrow(l, Priority.ALWAYS);
            GridPane.setVgrow(l, Priority.ALWAYS);

            add(l, column + 1, 0);
            getColumnConstraints().add(createColumn(2));
        }

        // initialize the stock field grid
        for (int row = 0; row < market.length; row++) {
            for (int column = 0; column < market[0].length; column++) {
                if (market[row][column] != null) {
                    // the market field exists
                    FXStockField stockSpace = new FXStockField(market[row][column]);

                    GridPane.setHgrow(stockSpace, Priority.ALWAYS);
                    GridPane.setVgrow(stockSpace, Priority.ALWAYS);

                    add(stockSpace, column + 1, row + 1);
                } else {
                    // the market field is null and therefore unused in the game
                    FXEmptyStockField stockSpace = new FXEmptyStockField();

                    GridPane.setHgrow(stockSpace, Priority.ALWAYS);
                    GridPane.setVgrow(stockSpace, Priority.ALWAYS);

                    add(stockSpace, column + 1, row + 1);
                }
            }
        }
    }

    /**
     * Creates a {@link ColumnConstraints} object with the given width factor.
     * The width factor defines, how much wider the column is compared to other columns.
     * A factor of 2 means, that the column is twice as wide as a column with the factor 1
     *
     * @param factor The width factor
     * @return The created constraint
     */
    private ColumnConstraints createColumn(int factor) {
        ColumnConstraints constraints = new ColumnConstraints();

        constraints.setPercentWidth(100d / ((market[0].length * 2) + 1) * factor);

        return constraints;
    }

    /**
     * Creates a {@link RowConstraints} object with the given height factor.
     * The height factor defines, how much higher the row is compared to other columns.
     * A factor of 2 means, that the row is twice as high as a row with the factor 1
     *
     * @param factor The height factor
     * @return The created constraint
     */
    private RowConstraints createRow(int factor) {
        RowConstraints constraints = new RowConstraints();

        constraints.setPercentHeight(100d / ((market.length * 2) + 1) * factor);

        return constraints;
    }
}

package br.com.estoqueti.util;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.Arrays;
import java.util.List;

public final class ResponsiveLayoutSupport {

    private ResponsiveLayoutSupport() {
    }

    public static void configureResponsiveSplit(GridPane gridPane,
                                                Region primaryCard,
                                                double primaryWeight,
                                                double primaryMinWidth,
                                                Region secondaryCard,
                                                double secondaryWeight,
                                                double secondaryMinWidth,
                                                double stackBreakpoint) {
        gridPane.setMaxWidth(Double.MAX_VALUE);
        primaryCard.setMaxWidth(Double.MAX_VALUE);
        secondaryCard.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(primaryCard, Priority.ALWAYS);
        GridPane.setHgrow(secondaryCard, Priority.ALWAYS);

        ChangeListener<Number> listener = (observable, oldValue, newValue) ->
                updateGridSplit(gridPane, primaryCard, primaryWeight, primaryMinWidth, secondaryCard, secondaryWeight, secondaryMinWidth, stackBreakpoint);

        gridPane.widthProperty().addListener(listener);
        gridPane.sceneProperty().addListener((observable, oldScene, newScene) ->
                Platform.runLater(() -> updateGridSplit(gridPane, primaryCard, primaryWeight, primaryMinWidth, secondaryCard, secondaryWeight, secondaryMinWidth, stackBreakpoint)));

        Platform.runLater(() -> updateGridSplit(gridPane, primaryCard, primaryWeight, primaryMinWidth, secondaryCard, secondaryWeight, secondaryMinWidth, stackBreakpoint));
    }

    public static void configureResponsiveSplit(FlowPane flowPane,
                                                Region primaryCard,
                                                double primaryWeight,
                                                double primaryMinWidth,
                                                Region secondaryCard,
                                                double secondaryWeight,
                                                double secondaryMinWidth) {
        configureResponsiveWeightedFlow(
                flowPane,
                List.of(primaryCard, secondaryCard),
                List.of(primaryWeight, secondaryWeight),
                List.of(primaryMinWidth, secondaryMinWidth)
        );
    }

    public static void configureResponsiveWeightedFlow(FlowPane flowPane,
                                                       List<? extends Region> cards,
                                                       List<Double> weights,
                                                       List<Double> minWidths) {
        ChangeListener<Number> listener = (observable, oldValue, newValue) ->
                updateWeightedFlow(flowPane, cards, weights, minWidths);

        flowPane.widthProperty().addListener(listener);
        flowPane.sceneProperty().addListener((observable, oldScene, newScene) ->
                Platform.runLater(() -> updateWeightedFlow(flowPane, cards, weights, minWidths)));

        Platform.runLater(() -> updateWeightedFlow(flowPane, cards, weights, minWidths));
    }

    public static void configureResponsiveTiles(FlowPane flowPane,
                                                double minTileWidth,
                                                double maxTileWidth,
                                                Region... cards) {
        List<Region> regions = Arrays.asList(cards);
        ChangeListener<Number> listener = (observable, oldValue, newValue) ->
                updateTileFlow(flowPane, regions, minTileWidth, maxTileWidth);

        flowPane.widthProperty().addListener(listener);
        flowPane.sceneProperty().addListener((observable, oldScene, newScene) ->
                Platform.runLater(() -> updateTileFlow(flowPane, regions, minTileWidth, maxTileWidth)));

        Platform.runLater(() -> updateTileFlow(flowPane, regions, minTileWidth, maxTileWidth));
    }

    private static void updateGridSplit(GridPane gridPane,
                                        Region primaryCard,
                                        double primaryWeight,
                                        double primaryMinWidth,
                                        Region secondaryCard,
                                        double secondaryWeight,
                                        double secondaryMinWidth,
                                        double stackBreakpoint) {
        double availableWidth = resolveAvailableWidth(gridPane);
        if (availableWidth <= 0) {
            return;
        }

        if (availableWidth < stackBreakpoint) {
            gridPane.getColumnConstraints().setAll(createColumnConstraint(100));
            setGridPosition(primaryCard, 0, 0);
            setGridPosition(secondaryCard, 0, 1);
            primaryCard.setPrefWidth(availableWidth);
            secondaryCard.setPrefWidth(availableWidth);
            return;
        }

        double totalWeight = primaryWeight + secondaryWeight;
        gridPane.getColumnConstraints().setAll(
                createColumnConstraint((primaryWeight / totalWeight) * 100),
                createColumnConstraint((secondaryWeight / totalWeight) * 100)
        );

        setGridPosition(primaryCard, 0, 0);
        setGridPosition(secondaryCard, 1, 0);

        double usableWidth = Math.max(0, availableWidth - gridPane.getHgap());
        primaryCard.setPrefWidth(Math.max(primaryMinWidth, usableWidth * (primaryWeight / totalWeight)));
        secondaryCard.setPrefWidth(Math.max(secondaryMinWidth, usableWidth * (secondaryWeight / totalWeight)));
    }

    private static ColumnConstraints createColumnConstraint(double percentWidth) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(percentWidth);
        constraints.setHgrow(Priority.ALWAYS);
        constraints.setFillWidth(true);
        return constraints;
    }

    private static void setGridPosition(Region card, int columnIndex, int rowIndex) {
        GridPane.setColumnIndex(card, columnIndex);
        GridPane.setRowIndex(card, rowIndex);
        GridPane.setColumnSpan(card, 1);
    }

    private static void updateWeightedFlow(FlowPane flowPane,
                                           List<? extends Region> cards,
                                           List<Double> weights,
                                           List<Double> minWidths) {
        double availableWidth = resolveAvailableWidth(flowPane);
        if (availableWidth <= 0 || cards.isEmpty()) {
            return;
        }

        flowPane.setPrefWrapLength(availableWidth);
        double gap = flowPane.getHgap();
        double totalMinWidth = minWidths.stream().mapToDouble(Double::doubleValue).sum() + gap * (cards.size() - 1);

        if (availableWidth >= totalMinWidth) {
            double usableWidth = availableWidth - gap * (cards.size() - 1);
            double totalWeight = weights.stream().mapToDouble(Double::doubleValue).sum();

            for (int index = 0; index < cards.size(); index++) {
                double cardWidth = usableWidth * (weights.get(index) / totalWeight);
                cards.get(index).setPrefWidth(Math.max(minWidths.get(index), cardWidth));
            }
            return;
        }

        for (Region card : cards) {
            card.setPrefWidth(availableWidth);
        }
    }

    private static void updateTileFlow(FlowPane flowPane,
                                       List<Region> cards,
                                       double minTileWidth,
                                       double maxTileWidth) {
        double availableWidth = resolveAvailableWidth(flowPane);
        if (availableWidth <= 0 || cards.isEmpty()) {
            return;
        }

        flowPane.setPrefWrapLength(availableWidth);
        double gap = flowPane.getHgap();
        int columns = Math.max(1, (int) Math.floor((availableWidth + gap) / (minTileWidth + gap)));
        columns = Math.min(columns, cards.size());

        double tileWidth = (availableWidth - gap * (columns - 1)) / columns;
        if (maxTileWidth > 0) {
            tileWidth = Math.min(tileWidth, maxTileWidth);
        }

        for (Region card : cards) {
            card.setPrefWidth(tileWidth);
        }
    }

    private static double resolveAvailableWidth(Region region) {
        if (region.getWidth() > 0) {
            return region.getWidth();
        }

        if (region.getParent() instanceof Region parent && parent.getWidth() > 0) {
            return parent.getWidth();
        }

        return region.getPrefWidth();
    }
}
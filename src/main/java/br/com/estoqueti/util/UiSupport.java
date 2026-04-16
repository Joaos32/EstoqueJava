package br.com.estoqueti.util;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.List;
import java.util.function.Function;

public final class UiSupport {

    private static final List<String> BADGE_STYLES = List.of(
            "badge-neutral",
            "badge-info",
            "badge-success",
            "badge-warning",
            "badge-danger",
            "badge-muted"
    );

    private UiSupport() {
    }

    public static VBox createTablePlaceholder(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("table-placeholder-title");
        titleLabel.setWrapText(true);

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("table-placeholder-subtitle");
        subtitleLabel.setWrapText(true);

        VBox container = new VBox(6, titleLabel, subtitleLabel);
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(280);
        container.getStyleClass().add("table-placeholder");
        return container;
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> badgeCellFactory(Function<String, String> styleResolver) {
        return badgeCellFactory(styleResolver, Pos.CENTER_LEFT);
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> centeredBadgeCellFactory(Function<String, String> styleResolver) {
        return badgeCellFactory(styleResolver, Pos.CENTER);
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> alignedTextCellFactory(Pos alignment) {
        return ignored -> new TableCell<>() {
            private final Label valueLabel = new Label();
            private final StackPane container = new StackPane(valueLabel);

            {
                valueLabel.textFillProperty().bind(textFillProperty());
                container.setAlignment(alignment);
                container.setMaxWidth(Double.MAX_VALUE);
                container.prefWidthProperty().bind(Bindings.createDoubleBinding(
                        () -> Math.max(0, getWidth() - snappedLeftInset() - snappedRightInset()),
                        widthProperty()
                ));
                setAlignment(alignment);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(null);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                valueLabel.setText(item);
                setText(null);
                setGraphic(container);
            }
        };
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> centeredTextCellFactory() {
        return alignedTextCellFactory(Pos.CENTER);
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> badgeCellFactory(
            Function<String, String> styleResolver,
            Pos alignment
    ) {
        return ignored -> new TableCell<>() {
            private final Label badge = new Label();
            private final StackPane container = new StackPane(badge);

            {
                badge.getStyleClass().add("table-badge");
                setAlignment(alignment);
                container.setAlignment(alignment);
                container.setMaxWidth(Double.MAX_VALUE);
                container.prefWidthProperty().bind(Bindings.createDoubleBinding(
                        () -> Math.max(0, getWidth() - snappedLeftInset() - snappedRightInset()),
                        widthProperty()
                ));
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(null);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                badge.setText(item);
                badge.getStyleClass().removeAll(BADGE_STYLES);

                String styleClass = styleResolver.apply(item);
                if (styleClass != null && !styleClass.isBlank()) {
                    badge.getStyleClass().add(styleClass);
                }

                setText(null);
                setGraphic(container);
            }
        };
    }
}

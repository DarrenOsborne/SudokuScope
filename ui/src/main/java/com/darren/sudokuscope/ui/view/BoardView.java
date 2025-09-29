package com.darren.sudokuscope.ui.view;

import com.darren.sudokuscope.ui.viewmodel.BoardViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public final class BoardView {
  private final BorderPane root = new BorderPane();
  private final GridPane grid = new GridPane();
  private final TextField[][] inputs = new TextField[9][9];

  public BoardView(BoardViewModel viewModel) {
    grid.getStyleClass().add("sudoku-grid");
    grid.setAlignment(Pos.CENTER);

    Label headline = createSolutionHeadline(viewModel);
    BorderPane.setAlignment(headline, Pos.CENTER);
    BorderPane.setMargin(headline, new Insets(12, 16, 12, 16));
    root.setTop(headline);

    createCells(viewModel);
    VBox content = new VBox(16, grid, buildStatusBar(viewModel));
    content.setPadding(new Insets(8, 16, 16, 16));
    root.setCenter(content);
  }

  public Parent getRoot() {
    return root;
  }

  private Label createSolutionHeadline(BoardViewModel viewModel) {
    Label label = new Label();
    label.getStyleClass().add("solution-count");
    label.textProperty().bind(viewModel.solutionCountTextProperty());
    return label;
  }

  private void createCells(BoardViewModel viewModel) {
    grid.setHgap(4);
    grid.setVgap(4);
    grid.setPadding(new Insets(8));
    for (int row = 0; row < 9; row++) {
      for (int col = 0; col < 9; col++) {
        TextField field = new TextField();
        field.getStyleClass().add("sudoku-cell");
        field.setAlignment(Pos.CENTER);
        field.setPrefColumnCount(1);
        field.setTextFormatter(
            new TextFormatter<>(
                new SingleDigitStringConverter(),
                "",
                change -> {
                  String newText = change.getControlNewText();
                  if (newText.length() > 1) {
                    return null;
                  }
                  if (newText.isEmpty() || newText.matches("[1-9]")) {
                    return change;
                  }
                  return null;
                }));
        field.textProperty().bindBidirectional(viewModel.cellProperty(row, col));
        grid.add(field, col, row);
        inputs[row][col] = field;
      }
    }

    viewModel
        .boardValidProperty()
        .addListener((obs, wasValid, isValid) -> updateValidityStyles(isValid));
    updateValidityStyles(viewModel.boardValidProperty().get());
  }

  private void updateValidityStyles(boolean boardValid) {
    for (TextField[] rowFields : inputs) {
      for (TextField field : rowFields) {
        if (boardValid) {
          field.getStyleClass().remove("invalid");
        } else if (!field.getStyleClass().contains("invalid")) {
          field.getStyleClass().add("invalid");
        }
      }
    }
  }

  private VBox buildStatusBar(BoardViewModel viewModel) {
    Label statusMessage = new Label();
    statusMessage.getStyleClass().add("status-label");
    statusMessage.textProperty().bind(viewModel.solverMessageProperty());

    Label uniqueBadge = new Label("Unique solution");
    uniqueBadge.getStyleClass().add("status-label");
    uniqueBadge.visibleProperty().bind(viewModel.uniqueSolutionProperty());
    uniqueBadge.managedProperty().bind(viewModel.uniqueSolutionProperty());

    HBox controls = new HBox(8);
    controls.getStyleClass().add("controls");

    Button undo = new Button("Undo");
    undo.disableProperty().bind(viewModel.undoAvailableProperty().not());
    undo.setOnAction(event -> viewModel.undo());

    Button redo = new Button("Redo");
    redo.disableProperty().bind(viewModel.redoAvailableProperty().not());
    redo.setOnAction(event -> viewModel.redo());

    Button clear = new Button("Clear");
    clear.getStyleClass().add("button-primary");
    clear.setOnAction(event -> viewModel.clearBoard());

    controls.getChildren().addAll(undo, redo, clear);

    VBox statusBar = new VBox(8);
    statusBar.getStyleClass().add("status-bar");
    statusBar.getChildren().addAll(statusMessage, uniqueBadge, controls);
    return statusBar;
  }

  private static final class SingleDigitStringConverter extends StringConverter<String> {
    @Override
    public String toString(String object) {
      return object == null ? "" : object;
    }

    @Override
    public String fromString(String string) {
      return string == null ? "" : string;
    }
  }
}

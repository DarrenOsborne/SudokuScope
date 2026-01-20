package com.darren.sudokuscope.ui.view;

import com.darren.sudokuscope.ui.viewmodel.BoardViewModel;
import com.darren.sudokuscope.ui.viewmodel.TargetCountViewModel;
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

public final class TargetCountView {
  private final BorderPane root = new BorderPane();
  private final GridPane grid = new GridPane();
  private final TextField[][] inputs = new TextField[9][9];

  public TargetCountView(TargetCountViewModel viewModel) {
    BoardViewModel boardViewModel = viewModel.boardViewModel();
    grid.getStyleClass().add("sudoku-grid");
    grid.setAlignment(Pos.CENTER);

    Label headline = createSolutionHeadline(boardViewModel);
    VBox searchBox = buildSearchBox(viewModel);
    VBox header = new VBox(8, headline, searchBox);
    BorderPane.setAlignment(header, Pos.CENTER);
    BorderPane.setMargin(header, new Insets(12, 16, 12, 16));
    root.setTop(header);

    createCells(boardViewModel);
    VBox content = new VBox(16, grid, buildStatusBar(boardViewModel));
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

  private VBox buildSearchBox(TargetCountViewModel viewModel) {
    TextField targetField = new TextField();
    targetField.getStyleClass().add("search-field");
    targetField.setPromptText("Target solutions");
    targetField.setTextFormatter(new TextFormatter<>(change -> {
      String text = change.getControlNewText();
      return text.matches("\\d*") ? change : null;
    }));
    targetField.textProperty().bindBidirectional(viewModel.targetInputProperty());

    TextField timeLimitField = new TextField();
    timeLimitField.getStyleClass().add("search-field");
    timeLimitField.setPromptText("Seconds (e.g. 8)");
    timeLimitField.setTextFormatter(new TextFormatter<>(change -> {
      String text = change.getControlNewText();
      return text.matches("\\d*(\\.\\d*)?") ? change : null;
    }));
    timeLimitField.textProperty().bindBidirectional(viewModel.timeLimitInputProperty());

    TextField seedField = new TextField();
    seedField.getStyleClass().add("search-field");
    seedField.setPromptText("Seed (optional)");
    seedField.setTextFormatter(new TextFormatter<>(change -> {
      String text = change.getControlNewText();
      return text.matches("\\d*") ? change : null;
    }));
    seedField.textProperty().bindBidirectional(viewModel.seedInputProperty());

    Button find = new Button("Find closest");
    find.getStyleClass().add("button-primary");
    find.disableProperty().bind(viewModel.searchRunningProperty());
    find.setOnAction(event -> viewModel.startSearch());

    Button cancel = new Button("Cancel");
    cancel.disableProperty().bind(viewModel.searchRunningProperty().not());
    cancel.setOnAction(event -> viewModel.cancelSearch());

    HBox fields = new HBox(10, targetField, timeLimitField, seedField);
    fields.getStyleClass().add("search-bar");
    fields.setAlignment(Pos.CENTER);

    HBox actions = new HBox(10, find, cancel);
    actions.getStyleClass().add("search-bar");
    actions.setAlignment(Pos.CENTER);

    Label status = new Label();
    status.getStyleClass().add("status-label");
    status.textProperty().bind(viewModel.searchStatusProperty());

    VBox box = new VBox(6, fields, actions, status);
    box.setAlignment(Pos.CENTER);
    return box;
  }

  private void createCells(BoardViewModel viewModel) {
    grid.setHgap(0);
    grid.setVgap(0);
    grid.setPadding(new Insets(12));
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
        applyCellBorders(field, row, col);
        grid.add(field, col, row);
        inputs[row][col] = field;
      }
    }

    viewModel
        .boardValidProperty()
        .addListener((obs, wasValid, isValid) -> updateValidityStyles(isValid));
    updateValidityStyles(viewModel.boardValidProperty().get());
  }

  private void applyCellBorders(TextField field, int row, int col) {
    int top = row == 0 ? 4 : (row % 3 == 0 ? 2 : 1);
    int bottom = row == 8 ? 4 : ((row + 1) % 3 == 0 ? 2 : 1);
    int left = col == 0 ? 4 : (col % 3 == 0 ? 2 : 1);
    int right = col == 8 ? 4 : ((col + 1) % 3 == 0 ? 2 : 1);
    String style =
        String.format(
            "-fx-border-width: %d %d %d %d; -fx-border-color: #1f3c88;", top, right, bottom, left);
    field.setStyle(style);
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

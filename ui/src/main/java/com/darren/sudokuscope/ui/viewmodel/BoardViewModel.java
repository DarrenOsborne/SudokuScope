package com.darren.sudokuscope.ui.viewmodel;

import com.darren.sudokuscope.core.BoardValidator;
import com.darren.sudokuscope.core.CellPosition;
import com.darren.sudokuscope.core.GameState;
import com.darren.sudokuscope.core.SudokuBoard;
import com.darren.sudokuscope.core.command.SetValueCommand;
import com.darren.sudokuscope.core.solver.SolverOptions;
import com.darren.sudokuscope.core.solver.SolverService;
import com.darren.sudokuscope.core.solver.SolverStatus;
import com.darren.sudokuscope.core.solver.SudokuAnalysis;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

public final class BoardViewModel {
  private static final Duration ANALYSIS_DEBOUNCE = Duration.millis(200);

  private final StringProperty[][] cells = new StringProperty[SudokuBoard.SIZE][SudokuBoard.SIZE];
  private final BooleanProperty undoAvailable = new SimpleBooleanProperty(false);
  private final BooleanProperty redoAvailable = new SimpleBooleanProperty(false);
  private final BooleanProperty boardValid = new SimpleBooleanProperty(true);
  private final BooleanProperty uniqueSolution = new SimpleBooleanProperty(false);
  private final StringProperty solutionCountText = new SimpleStringProperty("Solutions: —");
  private final StringProperty solverMessage = new SimpleStringProperty("Awaiting input…");
  private final ObjectProperty<SolverStatus> solverStatus =
      new SimpleObjectProperty<>(SolverStatus.MULTIPLE_SOLUTIONS);

  private final SolverService solverService = SolverService.createDefault();
  private final PauseTransition analysisDebounce = new PauseTransition(ANALYSIS_DEBOUNCE);
  private final AtomicReference<CompletableFuture<SudokuAnalysis>> inFlight =
      new AtomicReference<>();

  private GameState gameState = new GameState();
  private boolean suppressListeners;

  public BoardViewModel() {
    analysisDebounce.setOnFinished(event -> startAnalysis());
    initialiseCells();
    refreshFromBoard();
    triggerAnalysis();
  }

  private void initialiseCells() {
    for (int row = 0; row < SudokuBoard.SIZE; row++) {
      for (int col = 0; col < SudokuBoard.SIZE; col++) {
        final int r = row;
        final int c = col;
        StringProperty property = new SimpleStringProperty("");
        property.addListener((obs, oldValue, newValue) -> handleCellEdit(r, c, newValue));
        cells[row][col] = property;
      }
    }
  }

  public StringProperty cellProperty(int row, int col) {
    return cells[row][col];
  }

  public BooleanProperty undoAvailableProperty() {
    return undoAvailable;
  }

  public BooleanProperty redoAvailableProperty() {
    return redoAvailable;
  }

  public BooleanProperty boardValidProperty() {
    return boardValid;
  }

  public BooleanProperty uniqueSolutionProperty() {
    return uniqueSolution;
  }

  public StringProperty solutionCountTextProperty() {
    return solutionCountText;
  }

  public StringProperty solverMessageProperty() {
    return solverMessage;
  }

  public ObjectProperty<SolverStatus> solverStatusProperty() {
    return solverStatus;
  }

  public void undo() {
    if (!gameState.canUndo()) {
      return;
    }
    gameState.undo();
    refreshFromBoard();
    triggerAnalysis();
  }

  public void redo() {
    if (!gameState.canRedo()) {
      return;
    }
    gameState.redo();
    refreshFromBoard();
    triggerAnalysis();
  }

  public void clearBoard() {
    gameState = new GameState();
    refreshFromBoard();
    solutionCountText.set("Solutions: —");
    solverMessage.set("Awaiting input…");
    uniqueSolution.set(false);
    solverStatus.set(SolverStatus.MULTIPLE_SOLUTIONS);
    boardValid.set(true);
    triggerAnalysis();
  }

  public void shutdown() {
    cancelInFlight();
    solverService.close();
  }

  private void handleCellEdit(int row, int col, String rawValue) {
    if (suppressListeners) {
      return;
    }
    String value = rawValue == null ? "" : rawValue.trim();
    if (value.isEmpty()) {
      applyValue(row, col, 0);
      return;
    }
    if (value.length() > 1 || !Character.isDigit(value.charAt(0)) || value.charAt(0) == '0') {
      revertCell(row, col);
      return;
    }
    applyValue(row, col, value.charAt(0) - '0');
  }

  private void applyValue(int row, int col, int value) {
    try {
      gameState.apply(new SetValueCommand(new CellPosition(row, col), value));
    } catch (IllegalArgumentException ex) {
      revertCell(row, col);
      return;
    }
    refreshFromBoard();
    triggerAnalysis();
  }

  private void revertCell(int row, int col) {
    suppressListeners = true;
    try {
      int boardValue = gameState.board().valueAt(row, col);
      cells[row][col].set(boardValue == 0 ? "" : Integer.toString(boardValue));
    } finally {
      suppressListeners = false;
    }
  }

  private void refreshFromBoard() {
    SudokuBoard board = gameState.board();
    suppressListeners = true;
    try {
      for (int row = 0; row < SudokuBoard.SIZE; row++) {
        for (int col = 0; col < SudokuBoard.SIZE; col++) {
          int value = board.valueAt(row, col);
          cells[row][col].set(value == 0 ? "" : Integer.toString(value));
        }
      }
    } finally {
      suppressListeners = false;
    }
    undoAvailable.set(gameState.canUndo());
    redoAvailable.set(gameState.canRedo());
    updateValidation(board);
  }

  private void updateValidation(SudokuBoard board) {
    var validation = BoardValidator.validate(board);
    boardValid.set(validation.valid());
    if (!validation.valid()) {
      solverMessage.set(validation.message());
    }
  }

  private void triggerAnalysis() {
    analysisDebounce.playFromStart();
  }

  private void startAnalysis() {
    SudokuBoard snapshot = gameState.board();
    cancelInFlight();
    solverMessage.set("Analysing…");
    CompletableFuture<SudokuAnalysis> future =
        solverService.analyzeAsync(snapshot, SolverOptions.defaultOptions());
    inFlight.set(future);
    future.whenComplete(
        (analysis, throwable) -> {
          if (!inFlight.compareAndSet(future, null)) {
            return; // A newer request is running
          }
          Platform.runLater(() -> handleAnalysisResult(snapshot, analysis, throwable));
        });
  }

  private void cancelInFlight() {
    CompletableFuture<SudokuAnalysis> existing = inFlight.getAndSet(null);
    if (existing != null) {
      existing.cancel(true);
    }
  }

  private void handleAnalysisResult(
      SudokuBoard board, SudokuAnalysis analysis, Throwable throwable) {
    if (throwable != null) {
      solverMessage.set("Analysis failed: " + throwable.getMessage());
      return;
    }
    Objects.requireNonNull(analysis, "analysis");
    solverStatus.set(analysis.status());
    boardValid.set(analysis.valid());
    uniqueSolution.set(analysis.hasUniqueSolution());
    solutionCountText.set("Solutions: " + analysis.solutionCount());
    if (analysis.limitReached()) {
      solverMessage.set("Stopped after " + analysis.solutionCount() + " solutions (limit reached)");
    } else {
      solverMessage.set(analysis.message());
    }
  }
}

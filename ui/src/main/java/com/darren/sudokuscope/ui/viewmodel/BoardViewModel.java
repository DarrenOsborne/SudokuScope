package com.darren.sudokuscope.ui.viewmodel;

import com.darren.sudokuscope.core.BoardValidator;
import com.darren.sudokuscope.core.CellPosition;
import com.darren.sudokuscope.core.GameState;
import com.darren.sudokuscope.core.SudokuBoard;
import com.darren.sudokuscope.core.SudokuFacts;
import com.darren.sudokuscope.core.command.SetValueCommand;
import com.darren.sudokuscope.core.solver.SolverOptions;
import com.darren.sudokuscope.core.solver.SolverService;
import com.darren.sudokuscope.core.solver.SolverStatus;
import com.darren.sudokuscope.core.solver.SudokuAnalysis;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

public final class BoardViewModel {
  private static final Duration ANALYSIS_DEBOUNCE = Duration.millis(200);
  private static final BigInteger MILLION = BigInteger.valueOf(1_000_000L);
  private static final SolverOptions UI_SOLVER_OPTIONS =
      SolverOptions.defaultOptions().withMaxSolutions(1_000_000);
  private static final DecimalFormat NUMBER_FORMAT =
      (DecimalFormat) NumberFormat.getIntegerInstance(Locale.US);
  private static final MathContext ESTIMATE_CONTEXT = new MathContext(40, RoundingMode.HALF_UP);

  private final StringProperty[][] cells = new StringProperty[SudokuBoard.SIZE][SudokuBoard.SIZE];
  private final BooleanProperty undoAvailable = new SimpleBooleanProperty(false);
  private final BooleanProperty redoAvailable = new SimpleBooleanProperty(false);
  private final BooleanProperty boardValid = new SimpleBooleanProperty(true);
  private final BooleanProperty uniqueSolution = new SimpleBooleanProperty(false);
  private final BooleanProperty approximateDisplay = new SimpleBooleanProperty(true);
  private final StringProperty solutionCountText = new SimpleStringProperty();
  private final StringProperty solverMessage = new SimpleStringProperty("Awaiting input...");
  private final ObjectProperty<SolverStatus> solverStatus =
      new SimpleObjectProperty<>(SolverStatus.MULTIPLE_SOLUTIONS);

  private final DoubleProperty displayCount = new SimpleDoubleProperty(0);

  private final SolverService solverService = SolverService.createDefault();
  private final PauseTransition analysisDebounce = new PauseTransition(ANALYSIS_DEBOUNCE);
  private final Timeline countAnimation = new Timeline();
  private final AtomicReference<CompletableFuture<SudokuAnalysis>> inFlight =
      new AtomicReference<>();

  private GameState gameState = new GameState();
  private boolean suppressListeners;

  static {
    NUMBER_FORMAT.setGroupingUsed(true);
    NUMBER_FORMAT.setMaximumFractionDigits(0);
    NUMBER_FORMAT.setMinimumFractionDigits(0);
  }

  public BoardViewModel() {
    analysisDebounce.setOnFinished(event -> startAnalysis());
    initialiseCells();
    displayCount.addListener((obs, oldValue, newValue) -> updateSolutionCountText());
    approximateDisplay.addListener((obs, oldValue, newValue) -> updateSolutionCountText());
    displayCount.set(safeBigIntegerToDouble(SudokuFacts.TOTAL_COMPLETED_GRIDS));
    updateSolutionCountText();
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
    solverMessage.set("Awaiting input...");
    uniqueSolution.set(false);
    solverStatus.set(SolverStatus.MULTIPLE_SOLUTIONS);
    boardValid.set(true);
    animateSolutionCount(SudokuFacts.TOTAL_COMPLETED_GRIDS, true);
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
    handleBoardEdit();
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

  private void handleBoardEdit() {
    SudokuBoard board = gameState.board();
    undoAvailable.set(gameState.canUndo());
    redoAvailable.set(gameState.canRedo());
    updateValidation(board);
    if (!boardValid.get()) {
      uniqueSolution.set(false);
      solverStatus.set(SolverStatus.INVALID);
      animateSolutionCount(BigInteger.ZERO, false);
      cancelInFlight();
      return;
    }
    updateImmediateEstimate(board);
    triggerAnalysis();
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
    updateImmediateEstimate(board);
  }

  private void updateValidation(SudokuBoard board) {
    var validation = BoardValidator.validate(board);
    boardValid.set(validation.valid());
    if (!validation.valid()) {
      solverMessage.set(validation.message());
    }
  }

  private void updateImmediateEstimate(SudokuBoard board) {
    if (!boardValid.get()) {
      return;
    }
    int filled = countFilledCells(board);
    if (filled == 0 || filled >= 40) {
      return;
    }
    BigInteger estimate = computeSequentialEstimate(board);
    if (estimate.signum() <= 0) {
      estimate = BigInteger.ONE;
    }
    animateSolutionCount(estimate, true);
    uniqueSolution.set(false);
  }

  private void triggerAnalysis() {
    analysisDebounce.playFromStart();
  }

  private void startAnalysis() {
    SudokuBoard snapshot = gameState.board();
    cancelInFlight();
    solverMessage.set("Analysing...");
    CompletableFuture<SudokuAnalysis> future =
        solverService.analyzeAsync(snapshot, UI_SOLVER_OPTIONS);
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

    if (!analysis.valid()) {
      uniqueSolution.set(false);
      animateSolutionCount(BigInteger.ZERO, false);
      solverMessage.set(analysis.message());
      return;
    }

    boolean limitReached = analysis.limitReached();
    BigInteger exactCount = analysis.solutionCount();
    if (analysis.status() == SolverStatus.NO_SOLUTION && !limitReached) {
      animateSolutionCount(BigInteger.ZERO, false);
      uniqueSolution.set(false);
      boardValid.set(false);
      solverMessage.set("Current entries lead to zero solutions.");
      return;
    }
    boolean useExact = exactCount.compareTo(MILLION) <= 0 && !limitReached;

    if (useExact) {
      animateSolutionCount(exactCount, false);
      uniqueSolution.set(analysis.hasUniqueSolution());
      solverMessage.set(analysis.message());
      return;
    }

    BigInteger estimate = computeSequentialEstimate(board);
    if (estimate.signum() <= 0) {
      estimate = BigInteger.ONE;
    }
    animateSolutionCount(estimate, true);
    uniqueSolution.set(false);

    if (limitReached) {
      solverMessage.set("Estimating after reaching solver limit");
    } else {
      int filled = countFilledCells(board);
      solverMessage.set(
          "Estimated possibilities with " + filled + " filled cell" + (filled == 1 ? "" : "s"));
    }
  }

  private BigInteger computeSequentialEstimate(SudokuBoard board) {
    List<CellPosition> filledPositions = new ArrayList<>();
    for (int row = 0; row < SudokuBoard.SIZE; row++) {
      for (int col = 0; col < SudokuBoard.SIZE; col++) {
        if (board.valueAt(row, col) != 0) {
          filledPositions.add(new CellPosition(row, col));
        }
      }
    }
    if (filledPositions.isEmpty()) {
      return SudokuFacts.TOTAL_COMPLETED_GRIDS;
    }

    SudokuBoard progressive = SudokuBoard.empty();
    BigDecimal estimate = new BigDecimal(SudokuFacts.TOTAL_COMPLETED_GRIDS);
    for (CellPosition position : filledPositions) {
      int candidates = countCandidates(progressive, position);
      if (candidates <= 0) {
        return BigInteger.ZERO;
      }
      estimate = estimate.divide(BigDecimal.valueOf(candidates), ESTIMATE_CONTEXT);
      progressive = progressive.withValue(position, board.valueAt(position));
    }
    return estimate.setScale(0, RoundingMode.HALF_UP).toBigInteger();
  }

  private int countCandidates(SudokuBoard board, CellPosition position) {
    boolean[] used = new boolean[10];
    int row = position.row();
    int col = position.column();
    for (int c = 0; c < SudokuBoard.SIZE; c++) {
      int value = board.valueAt(row, c);
      if (value != 0) {
        used[value] = true;
      }
    }
    for (int r = 0; r < SudokuBoard.SIZE; r++) {
      int value = board.valueAt(r, col);
      if (value != 0) {
        used[value] = true;
      }
    }
    int boxRow = (row / SudokuBoard.REGION_SIZE) * SudokuBoard.REGION_SIZE;
    int boxCol = (col / SudokuBoard.REGION_SIZE) * SudokuBoard.REGION_SIZE;
    for (int r = 0; r < SudokuBoard.REGION_SIZE; r++) {
      for (int c = 0; c < SudokuBoard.REGION_SIZE; c++) {
        int value = board.valueAt(boxRow + r, boxCol + c);
        if (value != 0) {
          used[value] = true;
        }
      }
    }
    int count = 0;
    for (int digit = 1; digit <= 9; digit++) {
      if (!used[digit]) {
        count++;
      }
    }
    return count;
  }

  private int countFilledCells(SudokuBoard board) {
    int count = 0;
    for (int row = 0; row < SudokuBoard.SIZE; row++) {
      for (int col = 0; col < SudokuBoard.SIZE; col++) {
        if (board.valueAt(row, col) != 0) {
          count++;
        }
      }
    }
    return count;
  }

  private void animateSolutionCount(BigInteger target, boolean approximate) {
    double start = displayCount.get();
    double end = safeBigIntegerToDouble(target);
    double delta = Math.abs(end - start);
    Duration duration = computeDuration(delta);
    approximateDisplay.set(approximate);
    countAnimation.stop();
    countAnimation.getKeyFrames().setAll(new KeyFrame(duration, new KeyValue(displayCount, end)));
    countAnimation.play();
  }

  private Duration computeDuration(double delta) {
    if (delta < 1d) {
      return Duration.seconds(0.2);
    }
    double magnitude = Math.log10(delta + 1);
    double seconds = 0.5 * Math.min(1.5, 1.0 + magnitude / 6.0);
    return Duration.seconds(Math.max(0.2, seconds));
  }

  private double safeBigIntegerToDouble(BigInteger value) {
    double asDouble = value.doubleValue();
    if (Double.isInfinite(asDouble) || Double.isNaN(asDouble)) {
      return Double.MAX_VALUE / 2;
    }
    return asDouble;
  }

  private void updateSolutionCountText() {
    long rounded = Math.max(0L, Math.round(displayCount.get()));
    String formatted = NUMBER_FORMAT.format(rounded);
    if (approximateDisplay.get()) {
      solutionCountText.set("Solutions: ~" + formatted);
    } else {
      solutionCountText.set("Solutions: " + formatted);
    }
  }
}

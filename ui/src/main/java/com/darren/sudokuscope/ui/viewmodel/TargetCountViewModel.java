package com.darren.sudokuscope.ui.viewmodel;

import com.darren.sudokuscope.core.SudokuBoard;
import com.darren.sudokuscope.core.solver.TargetPuzzleSearch;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class TargetCountViewModel {
  private static final long DEFAULT_TIME_LIMIT_MS = 8_000L;
  private static final int MAX_SOLUTIONS = 200_000;
  private static final DecimalFormat NUMBER_FORMAT =
      (DecimalFormat) NumberFormat.getIntegerInstance(Locale.US);

  private final BoardViewModel boardViewModel = new BoardViewModel();
  private final TargetPuzzleSearch searcher = new TargetPuzzleSearch();
  private final ExecutorService executor = newSingleThreadExecutor("target-search");
  private final AtomicReference<CompletableFuture<TargetPuzzleSearch.SearchResult>> inFlight =
      new AtomicReference<>();
  private final AtomicReference<CompletableFuture<SudokuBoard>> baseInFlight =
      new AtomicReference<>();
  private volatile SudokuBoard baseSolution;

  private final StringProperty targetInput = new SimpleStringProperty("");
  private final StringProperty timeLimitInput = new SimpleStringProperty("8");
  private final StringProperty seedInput = new SimpleStringProperty("");
  private final StringProperty searchStatus =
      new SimpleStringProperty("Enter a target and press Find closest.");
  private final BooleanProperty searchRunning = new SimpleBooleanProperty(false);

  static {
    NUMBER_FORMAT.setGroupingUsed(true);
    NUMBER_FORMAT.setMaximumFractionDigits(0);
    NUMBER_FORMAT.setMinimumFractionDigits(0);
  }

  public BoardViewModel boardViewModel() {
    return boardViewModel;
  }

  public StringProperty targetInputProperty() {
    return targetInput;
  }

  public StringProperty timeLimitInputProperty() {
    return timeLimitInput;
  }

  public StringProperty seedInputProperty() {
    return seedInput;
  }

  public StringProperty searchStatusProperty() {
    return searchStatus;
  }

  public BooleanProperty searchRunningProperty() {
    return searchRunning;
  }

  public void ensureBaseSolution() {
    if (baseSolution != null || baseInFlight.get() != null) {
      return;
    }
    CompletableFuture<SudokuBoard> future =
        CompletableFuture.supplyAsync(
            () -> searcher.generateRandomSolved(0L, System.nanoTime()), executor);
    if (!baseInFlight.compareAndSet(null, future)) {
      return;
    }
    future.whenComplete(
        (board, throwable) -> {
          baseInFlight.compareAndSet(future, null);
          Platform.runLater(() -> handleBaseSolution(board, throwable));
        });
  }

  public void refreshBaseSolution() {
    if (searchRunning.get()) {
      return;
    }
    baseSolution = null;
    cancelBaseGeneration();
    ensureBaseSolution();
  }

  public void startSearch() {
    BigInteger target = parseTarget();
    if (target == null) {
      return;
    }
    long timeLimitMs = parseTimeLimitMs();
    long seed = parseSeed();
    cancelSearch();
    searchRunning.set(true);
    updateStatus("Preparing base solution...");
    boardViewModel.solverMessageProperty().set("Preparing base solution...");
    CompletableFuture<SudokuBoard> baseFuture = ensureBaseSolutionFuture();
    CompletableFuture<TargetPuzzleSearch.SearchResult> future =
        baseFuture.thenApplyAsync(
            board -> {
              if (board == null) {
                return new TargetPuzzleSearch.SearchResult(
                    SudokuBoard.empty(),
                    BigInteger.ZERO,
                    true,
                    0L,
                    0L,
                    target);
              }
              Platform.runLater(
                  () -> {
                    updateStatus("Searching for closest puzzle...");
                    boardViewModel.solverMessageProperty().set("Searching for closest puzzle...");
                  });
              return searcher.findClosestFromSolved(
                  board, target, timeLimitMs, MAX_SOLUTIONS, seed);
            },
            executor);
    inFlight.set(future);
    future.whenComplete(
        (result, throwable) -> {
          if (!inFlight.compareAndSet(future, null)) {
            return;
          }
          Platform.runLater(() -> handleResult(target, result, throwable));
        });
  }

  public void cancelSearch() {
    CompletableFuture<TargetPuzzleSearch.SearchResult> existing = inFlight.getAndSet(null);
    if (existing != null) {
      existing.cancel(true);
    }
    searchRunning.set(false);
    updateStatus("Search cancelled.");
    boardViewModel.solverMessageProperty().set("Search cancelled.");
  }

  public void shutdown() {
    cancelSearch();
    cancelBaseGeneration();
    executor.shutdownNow();
    boardViewModel.shutdown();
  }

  private void cancelBaseGeneration() {
    CompletableFuture<SudokuBoard> existing = baseInFlight.getAndSet(null);
    if (existing != null) {
      existing.cancel(true);
    }
  }

  private CompletableFuture<SudokuBoard> ensureBaseSolutionFuture() {
    if (baseSolution != null) {
      return CompletableFuture.completedFuture(baseSolution);
    }
    CompletableFuture<SudokuBoard> existing = baseInFlight.get();
    if (existing != null) {
      return existing;
    }
    CompletableFuture<SudokuBoard> future =
        CompletableFuture.supplyAsync(
            () -> searcher.generateRandomSolved(0L, System.nanoTime()), executor);
    if (!baseInFlight.compareAndSet(null, future)) {
      return baseInFlight.get();
    }
    future.whenComplete(
        (board, throwable) -> {
          baseInFlight.compareAndSet(future, null);
          Platform.runLater(() -> handleBaseSolution(board, throwable));
        });
    return future;
  }

  private void handleBaseSolution(SudokuBoard board, Throwable throwable) {
    if (throwable != null || board == null) {
      updateStatus("Failed to generate a base solution.");
      boardViewModel.solverMessageProperty().set("Base solution generation failed.");
      return;
    }
    baseSolution = board;
    String message = "Generated base solution.";
    updateStatus(message);
    boardViewModel.loadSearchResult(board, BigInteger.ONE, false, message);
    boardViewModel.solverMessageProperty().set(message);
  }

  private void handleResult(
      BigInteger target, TargetPuzzleSearch.SearchResult result, Throwable throwable) {
    searchRunning.set(false);
    if (throwable != null) {
      updateStatus("Search failed: " + throwable.getMessage());
      return;
    }
    Objects.requireNonNull(result, "result");
    String formattedCount = format(result.solutionCount());
    String formattedDelta = format(result.delta());
    String message =
        "Closest count " + formattedCount + " (delta " + formattedDelta + ") in "
            + result.elapsedMillis() + "ms";
    updateStatus(message);
    boardViewModel.loadSearchResult(
        result.board(), result.solutionCount(), result.approximate(), message);
    if (result.delta().equals(BigInteger.ZERO)) {
      boardViewModel.solverMessageProperty().set("Exact match found.");
    } else {
      String targetText = format(target);
      boardViewModel
          .solverMessageProperty()
          .set(
              "Target " + targetText + ", best " + formattedCount + " (delta " + formattedDelta + ")");
    }
  }

  private BigInteger parseTarget() {
    String raw = targetInput.get() == null ? "" : targetInput.get().trim();
    if (raw.isEmpty()) {
      updateStatus("Enter a target solution count.");
      return null;
    }
    raw = raw.replace(",", "");
    if (!raw.matches("\\d+")) {
      updateStatus("Target must be a positive integer.");
      return null;
    }
    BigInteger target = new BigInteger(raw);
    if (target.signum() <= 0) {
      updateStatus("Target must be greater than zero.");
      return null;
    }
    return target;
  }

  private long parseTimeLimitMs() {
    String raw = timeLimitInput.get() == null ? "" : timeLimitInput.get().trim();
    if (raw.isEmpty()) {
      return DEFAULT_TIME_LIMIT_MS;
    }
    try {
      double seconds = Double.parseDouble(raw);
      if (seconds <= 0) {
        return DEFAULT_TIME_LIMIT_MS;
      }
      return (long) (seconds * 1000d);
    } catch (NumberFormatException ex) {
      return DEFAULT_TIME_LIMIT_MS;
    }
  }

  private long parseSeed() {
    String raw = seedInput.get() == null ? "" : seedInput.get().trim();
    if (raw.isEmpty()) {
      return System.nanoTime();
    }
    try {
      return Long.parseLong(raw);
    } catch (NumberFormatException ex) {
      updateStatus("Seed must be a number. Using random seed.");
      return System.nanoTime();
    }
  }

  private void updateStatus(String message) {
    searchStatus.set(message);
  }

  private String format(BigInteger value) {
    return NUMBER_FORMAT.format(value);
  }

  private static ExecutorService newSingleThreadExecutor(String prefix) {
    ThreadFactory factory =
        new ThreadFactory() {
          private final AtomicInteger counter = new AtomicInteger(1);

          @Override
          public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
          }
        };
    return Executors.newSingleThreadExecutor(factory);
  }
}

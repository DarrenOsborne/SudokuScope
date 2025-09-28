package com.darren.sudokuscope.core.solver;

import com.darren.sudokuscope.core.SudokuBoard;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Thin asynchronous wrapper around a {@link SudokuSolver}. */
public final class SolverService implements AutoCloseable {
  private final SudokuSolver solver;
  private final ExecutorService executor;
  private final boolean shutdownOnClose;

  public SolverService(SudokuSolver solver, ExecutorService executor, boolean shutdownOnClose) {
    this.solver = Objects.requireNonNull(solver, "solver");
    this.executor = Objects.requireNonNull(executor, "executor");
    this.shutdownOnClose = shutdownOnClose;
  }

  public static SolverService createDefault() {
    return new SolverService(
        SudokuSolver.createDefault(), newSingleThreadExecutor("sudoku-solver"), true);
  }

  public CompletableFuture<SudokuAnalysis> analyzeAsync(SudokuBoard board) {
    return analyzeAsync(board, SolverOptions.defaultOptions());
  }

  public CompletableFuture<SudokuAnalysis> analyzeAsync(SudokuBoard board, SolverOptions options) {
    Objects.requireNonNull(board, "board");
    Objects.requireNonNull(options, "options");
    return CompletableFuture.supplyAsync(() -> solver.analyze(board, options), executor);
  }

  public SudokuAnalysis analyzeBlocking(SudokuBoard board, SolverOptions options) {
    return solver.analyze(board, options);
  }

  @Override
  public void close() {
    if (shutdownOnClose) {
      executor.shutdownNow();
    }
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

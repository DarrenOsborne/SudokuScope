package com.darren.sudokuscope.bench;

import com.darren.sudokuscope.core.BoardBuilder;
import com.darren.sudokuscope.core.SudokuBoard;
import com.darren.sudokuscope.core.solver.SolverOptions;
import com.darren.sudokuscope.core.solver.SudokuAnalysis;
import com.darren.sudokuscope.core.solver.SudokuSolver;
import java.util.List;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class SolverBenchmark {
  private SudokuSolver solver;
  private SudokuBoard puzzle;

  @Setup
  public void setUp() {
    solver = SudokuSolver.createDefault();
    puzzle =
        new BoardBuilder()
            .withStringRows(
                List.of(
                    "530070000",
                    "600195000",
                    "098000060",
                    "800060003",
                    "400803001",
                    "700020006",
                    "060000280",
                    "000419005",
                    "000080079"))
            .build();
  }

  @Benchmark
  public SudokuAnalysis analyzePuzzle() {
    return solver.analyze(puzzle, SolverOptions.findFirstSolution());
  }
}

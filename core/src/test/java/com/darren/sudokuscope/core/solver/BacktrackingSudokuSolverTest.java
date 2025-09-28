package com.darren.sudokuscope.core.solver;

import static org.assertj.core.api.Assertions.assertThat;

import com.darren.sudokuscope.core.BoardBuilder;
import com.darren.sudokuscope.core.BoardValidator;
import com.darren.sudokuscope.core.SudokuBoard;
import com.darren.sudokuscope.core.SudokuFacts;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

class BacktrackingSudokuSolverTest {
  private final SudokuSolver solver = SudokuSolver.createDefault();

  @Test
  void solvesClassicPuzzleWithUniqueSolution() {
    SudokuBoard puzzle =
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

    SudokuAnalysis analysis = solver.analyze(puzzle, SolverOptions.defaultOptions());

    assertThat(analysis.valid()).isTrue();
    assertThat(analysis.hasUniqueSolution()).isTrue();
    assertThat(analysis.exemplarSolution()).isPresent();
    assertThat(BoardValidator.isValid(analysis.exemplarSolution().orElseThrow())).isTrue();
  }

  @Test
  void invalidBoardIsReported() {
    SudokuBoard invalid = new BoardBuilder().withRow(0, 1, 1, 0, 0, 0, 0, 0, 0, 0).build();

    SudokuAnalysis analysis = solver.analyze(invalid, SolverOptions.defaultOptions());

    assertThat(analysis.valid()).isFalse();
    assertThat(analysis.status()).isEqualTo(SolverStatus.INVALID);
  }

  @Test
  void emptyBoardReturnsKnownCount() {
    SudokuAnalysis analysis = solver.analyze(SudokuBoard.empty(), SolverOptions.defaultOptions());

    assertThat(analysis.solutionCount()).isEqualByComparingTo(SudokuFacts.TOTAL_COMPLETED_GRIDS);
    assertThat(analysis.status()).isEqualTo(SolverStatus.MULTIPLE_SOLUTIONS);
  }

  @Test
  void limitReachedWhenLimitingSolutions() {
    SudokuBoard sparse = new BoardBuilder().withRow(0, 1, 0, 0, 0, 0, 0, 0, 0, 0).build();
    SolverOptions options = SolverOptions.defaultOptions().withMaxSolutions(2);

    SudokuAnalysis analysis = solver.analyze(sparse, options);

    assertThat(analysis.status()).isEqualTo(SolverStatus.LIMIT_REACHED);
    assertThat(analysis.solutionCount()).isEqualByComparingTo(BigInteger.valueOf(2));
    assertThat(analysis.limitReached()).isTrue();
  }
}

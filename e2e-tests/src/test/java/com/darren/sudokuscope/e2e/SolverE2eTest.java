package com.darren.sudokuscope.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.darren.sudokuscope.core.BoardBuilder;
import com.darren.sudokuscope.core.solver.SolverOptions;
import com.darren.sudokuscope.core.solver.SolverService;
import com.darren.sudokuscope.core.solver.SudokuAnalysis;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SolverE2eTest {
  private static SolverService solverService;

  @BeforeAll
  static void setUp() {
    solverService = SolverService.createDefault();
  }

  @AfterAll
  static void tearDown() {
    solverService.close();
  }

  @Test
  void analyzePipelineFindsUniqueSolution() {
    SudokuAnalysis analysis =
        solverService.analyzeBlocking(
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
                .build(),
            SolverOptions.defaultOptions());

    assertThat(analysis.valid()).isTrue();
    assertThat(analysis.hasUniqueSolution()).isTrue();
    assertThat(analysis.exemplarSolution()).isPresent();
  }
}

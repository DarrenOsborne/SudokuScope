package com.darren.sudokuscope.web.api;

import com.darren.sudokuscope.core.solver.SolverOptions;
import com.darren.sudokuscope.core.solver.SolverService;
import com.darren.sudokuscope.core.solver.SudokuAnalysis;
import com.darren.sudokuscope.core.solver.TargetPuzzleSearch;
import java.math.BigInteger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class SolverController {
  private static final long DEFAULT_TIME_LIMIT_MS = 8_000L;
  private static final int DEFAULT_MAX_SOLUTIONS = 200_000;

  private final SolverService solverService;
  private final TargetPuzzleSearch targetSearch = new TargetPuzzleSearch();

  public SolverController(SolverService solverService) {
    this.solverService = solverService;
  }

  @PostMapping("/analyze")
  public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request) {
    try {
      SolverOptions options =
          SolverOptions.defaultOptions().withMaxSolutions(-1).withTimeLimitMillis(30_000);
      SudokuAnalysis analysis =
          solverService.analyzeBlocking(request.toBoard(), options);
      return AnalyzeResponse.from(analysis);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
    }
  }

  @PostMapping("/target")
  public TargetCountResponse target(@RequestBody TargetCountRequest request) {
    try {
      BigInteger target = request.parseTarget();
      long timeLimitMs = request.timeLimitMsOrDefault(DEFAULT_TIME_LIMIT_MS);
      long seed = request.seedOrDefault(System.nanoTime());
      int maxSolutions = request.maxSolutionsOrDefault(DEFAULT_MAX_SOLUTIONS);
      TargetPuzzleSearch.SearchResult result =
          targetSearch.findClosest(target, timeLimitMs, maxSolutions, seed);
      return TargetCountResponse.from(result);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
    }
  }
}

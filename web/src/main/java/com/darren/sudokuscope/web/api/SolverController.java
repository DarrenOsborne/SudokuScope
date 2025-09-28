package com.darren.sudokuscope.web.api;

import com.darren.sudokuscope.core.solver.SolverOptions;
import com.darren.sudokuscope.core.solver.SolverService;
import com.darren.sudokuscope.core.solver.SudokuAnalysis;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class SolverController {
  private final SolverService solverService;

  public SolverController(SolverService solverService) {
    this.solverService = solverService;
  }

  @PostMapping("/analyze")
  public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request) {
    try {
      SudokuAnalysis analysis =
          solverService.analyzeBlocking(request.toBoard(), SolverOptions.defaultOptions());
      return AnalyzeResponse.from(analysis);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
    }
  }
}

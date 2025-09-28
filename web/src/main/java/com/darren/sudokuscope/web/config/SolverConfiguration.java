package com.darren.sudokuscope.web.config;

import com.darren.sudokuscope.core.solver.SolverService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolverConfiguration {
  @Bean(destroyMethod = "close")
  public SolverService solverService() {
    return SolverService.createDefault();
  }
}

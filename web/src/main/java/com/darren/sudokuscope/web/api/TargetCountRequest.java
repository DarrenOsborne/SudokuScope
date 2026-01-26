package com.darren.sudokuscope.web.api;

import java.math.BigInteger;

public record TargetCountRequest(String target, Long timeLimitMs, Long seed, Integer maxSolutions) {

  public BigInteger parseTarget() {
    String raw = target == null ? "" : target.trim();
    if (raw.isEmpty()) {
      throw new IllegalArgumentException("Target must be provided.");
    }
    raw = raw.replace(",", "");
    if (!raw.matches("\\d+")) {
      throw new IllegalArgumentException("Target must be a positive integer.");
    }
    BigInteger value = new BigInteger(raw);
    if (value.signum() <= 0) {
      throw new IllegalArgumentException("Target must be greater than zero.");
    }
    return value;
  }

  public long timeLimitMsOrDefault(long fallback) {
    if (timeLimitMs == null || timeLimitMs <= 0) {
      return fallback;
    }
    return timeLimitMs;
  }

  public long seedOrDefault(long fallback) {
    if (seed == null) {
      return fallback;
    }
    return seed;
  }

  public int maxSolutionsOrDefault(int fallback) {
    if (maxSolutions == null || maxSolutions == 0) {
      return fallback;
    }
    return maxSolutions;
  }
}

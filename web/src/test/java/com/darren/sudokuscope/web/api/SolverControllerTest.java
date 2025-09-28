package com.darren.sudokuscope.web.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SolverControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void analyzeSolvableBoardReturnsUnique() throws Exception {
    int[] cells =
        new int[] {
          5, 3, 0, 0, 7, 0, 0, 0, 0,
          6, 0, 0, 1, 9, 5, 0, 0, 0,
          0, 9, 8, 0, 0, 0, 0, 6, 0,
          8, 0, 0, 0, 6, 0, 0, 0, 3,
          4, 0, 0, 8, 0, 3, 0, 0, 1,
          7, 0, 0, 0, 2, 0, 0, 0, 6,
          0, 6, 0, 0, 0, 0, 2, 8, 0,
          0, 0, 0, 4, 1, 9, 0, 0, 5,
          0, 0, 0, 0, 8, 0, 0, 7, 9
        };

    String payload = objectMapper.writeValueAsString(Map.of("cells", cells));

    mockMvc
        .perform(post("/api/analyze").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true))
        .andExpect(jsonPath("$.status").value("UNIQUE_SOLUTION"));
  }

  @Test
  void analyzeRejectsInvalidPayload() throws Exception {
    String payload = objectMapper.writeValueAsString(Map.of("cells", new int[] {1, 2, 3}));
    mockMvc
        .perform(post("/api/analyze").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isBadRequest());
  }
}

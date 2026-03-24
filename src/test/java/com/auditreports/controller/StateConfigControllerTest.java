package com.auditreports.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class StateConfigControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void getAllStates_returnsSeededStates() throws Exception {
        mockMvc.perform(get("/api/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].name").value("Queued"))
                .andExpect(jsonPath("$[0].isInitial").value(true));
    }

    @Test
    void createState_success() throws Exception {
        mockMvc.perform(post("/api/states")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "On Hold", "displayOrder": 10, "isInitial": false, "isTerminal": false}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("On Hold"));
    }

    @Test
    void createState_duplicateName_returns400() throws Exception {
        mockMvc.perform(post("/api/states")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Queued", "displayOrder": 1, "isInitial": false, "isTerminal": false}
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("already exists")));
    }

    @Test
    void getAllTransitions_returnsSeededTransitions() throws Exception {
        mockMvc.perform(get("/api/transitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    void createTransition_success() throws Exception {
        // Create a reverse transition (Assigned -> Queued) which doesn't exist
        mockMvc.perform(post("/api/transitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fromStateId": 2, "toStateId": 1}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromState.name").value("Assigned"))
                .andExpect(jsonPath("$.toState.name").value("Queued"));
    }

    @Test
    void deleteTransition_success() throws Exception {
        // Delete the first seeded transition (id=1: Queued -> Assigned)
        mockMvc.perform(delete("/api/transitions/1"))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/api/transitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void deleteState_inUse_returns409() throws Exception {
        // First create a report to put state in use
        mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Test", "region": "East", "reportPath": "\\\\\\\\path"}
                    """))
                .andExpect(status().isCreated());

        // Try to delete Queued state (id=1) which is now in use
        mockMvc.perform(delete("/api/states/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("in use")));
    }

    @Test
    void updateState_success() throws Exception {
        mockMvc.perform(put("/api/states/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "In Queue", "displayOrder": 1, "isInitial": true, "isTerminal": false}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("In Queue"));
    }
}

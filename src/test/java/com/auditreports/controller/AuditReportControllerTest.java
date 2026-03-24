package com.auditreports.controller;

import com.auditreports.model.*;
import com.auditreports.repository.*;
import org.junit.jupiter.api.BeforeEach;
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
class AuditReportControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditReportRepository reportRepository;
    @Autowired private EditorRepository editorRepository;
    @Autowired private ReportStateRepository stateRepository;

    @BeforeEach
    void setUp() {
        // Seed data is loaded from data.sql
    }

    @Test
    void createReport_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "FY2026 Financial Audit",
                        "region": "Northeast",
                        "auditType": "Financial",
                        "fiscalYear": "2026",
                        "reportPath": "\\\\\\\\server\\\\reports\\\\fy2026.docx"
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("FY2026 Financial Audit"))
                .andExpect(jsonPath("$.currentState.name").value("Queued"))
                .andExpect(jsonPath("$.editor").isEmpty());
    }

    @Test
    void getQueue_returnsUnassignedReports() throws Exception {
        // Create a report
        mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Test Report", "region": "West", "reportPath": "\\\\\\\\server\\\\test.docx"}
                    """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reports/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].editor").isEmpty());
    }

    @Test
    void assignEditor_success() throws Exception {
        // Create a report
        String reportJson = mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Assign Test", "region": "South", "reportPath": "\\\\\\\\path"}
                    """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extract report ID (simple parsing)
        Long reportId = extractId(reportJson);

        // Assign editor (Alice has ID 1 from seed data)
        mockMvc.perform(post("/api/reports/" + reportId + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"editorId": 1}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editor.name").value("Alice Johnson"));
    }

    @Test
    void assignEditor_alreadyAssigned_returns409() throws Exception {
        // Create and assign a report
        String reportJson = mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "No Reassign", "region": "East", "reportPath": "\\\\\\\\path"}
                    """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reportId = extractId(reportJson);

        mockMvc.perform(post("/api/reports/" + reportId + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"editorId\": 1}"))
                .andExpect(status().isOk());

        // Try to reassign
        mockMvc.perform(post("/api/reports/" + reportId + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"editorId\": 2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("cannot be re-assigned")));
    }

    @Test
    void transitionState_validTransition_success() throws Exception {
        // Create a report (starts in Queued state, id=1)
        String reportJson = mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Transition Test", "region": "North", "reportPath": "\\\\\\\\path"}
                    """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reportId = extractId(reportJson);

        // Transition from Queued (1) to Assigned (2)
        mockMvc.perform(post("/api/reports/" + reportId + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"toStateId\": 2, \"changedBy\": \"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState.name").value("Assigned"));
    }

    @Test
    void transitionState_invalidTransition_returns409() throws Exception {
        String reportJson = mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Bad Transition", "region": "South", "reportPath": "\\\\\\\\path"}
                    """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reportId = extractId(reportJson);

        // Try to transition from Queued directly to Initial Edit (3) — not allowed
        mockMvc.perform(post("/api/reports/" + reportId + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"toStateId\": 3, \"changedBy\": \"admin\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("not allowed")));
    }

    @Test
    void getReportDetail_includesHistoryAndMetrics() throws Exception {
        String reportJson = mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Detail Test", "region": "Central", "reportPath": "\\\\\\\\path"}
                    """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reportId = extractId(reportJson);

        mockMvc.perform(get("/api/reports/" + reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.title").value("Detail Test"))
                .andExpect(jsonPath("$.history", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.metrics").exists())
                .andExpect(jsonPath("$.allowedTransitions").exists());
    }

    private Long extractId(String json) {
        // Simple ID extraction from JSON
        int idx = json.indexOf("\"id\":");
        if (idx == -1) return null;
        String sub = json.substring(idx + 5).trim();
        StringBuilder num = new StringBuilder();
        for (char c : sub.toCharArray()) {
            if (Character.isDigit(c)) num.append(c);
            else break;
        }
        return Long.parseLong(num.toString());
    }
}

package com.auditreports.controller;

import com.auditreports.model.AuditReport;
import com.auditreports.model.ReportStateHistory;
import com.auditreports.model.StateTransition;
import com.auditreports.service.AuditReportService;
import com.auditreports.service.StateConfigService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
public class AuditReportController {

    private final AuditReportService reportService;
    private final StateConfigService stateConfigService;

    public AuditReportController(AuditReportService reportService, StateConfigService stateConfigService) {
        this.reportService = reportService;
        this.stateConfigService = stateConfigService;
    }

    @GetMapping
    public List<AuditReport> getAllReports() {
        return reportService.getAllReports();
    }

    @GetMapping("/queue")
    public List<AuditReport> getQueue() {
        return reportService.getQueue();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getReport(@PathVariable Long id) {
        AuditReport report = reportService.getReport(id);
        List<ReportStateHistory> history = reportService.getReportHistory(id);
        Map<String, Object> metrics = reportService.getReportMetrics(id);
        List<StateTransition> allowedTransitions = stateConfigService.getTransitionsFrom(report.getCurrentState());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("report", report);
        result.put("history", history);
        result.put("metrics", metrics);
        result.put("allowedTransitions", allowedTransitions);
        return result;
    }

    @PostMapping
    public ResponseEntity<AuditReport> createReport(@Valid @RequestBody AuditReport report) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createReport(report));
    }

    @PutMapping("/{id}")
    public AuditReport updateReport(@PathVariable Long id, @Valid @RequestBody AuditReport report) {
        return reportService.updateReport(id, report);
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<AuditReport> assignEditor(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long editorId = body.get("editorId");
        return ResponseEntity.ok(reportService.assignEditor(id, editorId));
    }

    @PostMapping("/{id}/transition")
    public ResponseEntity<AuditReport> transitionState(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long toStateId = ((Number) body.get("toStateId")).longValue();
        String changedBy = (String) body.getOrDefault("changedBy", "user");
        LocalDateTime changedAt = null;
        if (body.containsKey("changedAt") && body.get("changedAt") != null) {
            changedAt = LocalDateTime.parse((String) body.get("changedAt"));
        }
        return ResponseEntity.ok(reportService.transitionState(id, toStateId, changedBy, changedAt));
    }

    @GetMapping("/{id}/history")
    public List<ReportStateHistory> getHistory(@PathVariable Long id) {
        return reportService.getReportHistory(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}

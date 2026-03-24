package com.auditreports.service;

import com.auditreports.model.*;
import com.auditreports.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class AuditReportService {

    private final AuditReportRepository reportRepository;
    private final EditorRepository editorRepository;
    private final ReportStateRepository stateRepository;
    private final StateTransitionRepository transitionRepository;
    private final ReportStateHistoryRepository historyRepository;

    public AuditReportService(AuditReportRepository reportRepository,
                              EditorRepository editorRepository,
                              ReportStateRepository stateRepository,
                              StateTransitionRepository transitionRepository,
                              ReportStateHistoryRepository historyRepository) {
        this.reportRepository = reportRepository;
        this.editorRepository = editorRepository;
        this.stateRepository = stateRepository;
        this.transitionRepository = transitionRepository;
        this.historyRepository = historyRepository;
    }

    public List<AuditReport> getAllReports() {
        return reportRepository.findAll();
    }

    public AuditReport getReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + id));
    }

    public List<AuditReport> getQueue() {
        return reportRepository.findByEditorIsNull();
    }

    public AuditReport createReport(AuditReport report) {
        ReportState initialState = stateRepository.findByIsInitialTrue()
                .orElseThrow(() -> new IllegalStateException("No initial state configured. Please configure states first."));
        report.setCurrentState(initialState);

        AuditReport saved = reportRepository.save(report);

        ReportStateHistory history = new ReportStateHistory(saved, null, initialState, "system");
        historyRepository.save(history);

        return saved;
    }

    public AuditReport updateReport(Long id, AuditReport updated) {
        AuditReport existing = getReport(id);
        existing.setTitle(updated.getTitle());
        existing.setRegion(updated.getRegion());
        existing.setAuditType(updated.getAuditType());
        existing.setFiscalYear(updated.getFiscalYear());
        existing.setReportPath(updated.getReportPath());
        existing.setNotes(updated.getNotes());
        return reportRepository.save(existing);
    }

    public AuditReport assignEditor(Long reportId, Long editorId) {
        AuditReport report = getReport(reportId);

        if (report.getEditor() != null) {
            throw new IllegalStateException("Report is already assigned to editor: " + report.getEditor().getName()
                    + ". Reports cannot be re-assigned.");
        }

        Editor editor = editorRepository.findById(editorId)
                .orElseThrow(() -> new IllegalArgumentException("Editor not found: " + editorId));

        report.setEditor(editor);
        report.setAssignedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    public AuditReport transitionState(Long reportId, Long toStateId, String changedBy) {
        AuditReport report = getReport(reportId);
        ReportState currentState = report.getCurrentState();
        ReportState toState = stateRepository.findById(toStateId)
                .orElseThrow(() -> new IllegalArgumentException("Target state not found: " + toStateId));

        transitionRepository.findByFromStateAndToState(currentState, toState)
                .orElseThrow(() -> new IllegalStateException(
                        "Transition from '" + currentState.getName() + "' to '" + toState.getName() + "' is not allowed"));

        ReportStateHistory history = new ReportStateHistory(report, currentState, toState, changedBy);
        historyRepository.save(history);

        report.setCurrentState(toState);
        return reportRepository.save(report);
    }

    public List<ReportStateHistory> getReportHistory(Long reportId) {
        AuditReport report = getReport(reportId);
        return historyRepository.findByReportOrderByChangedAtAsc(report);
    }

    public Map<String, Object> getReportMetrics(Long reportId) {
        AuditReport report = getReport(reportId);
        Map<String, Object> metrics = new LinkedHashMap<>();

        if (report.getAssignedAt() != null) {
            long daysWithEditor = ChronoUnit.DAYS.between(report.getAssignedAt(), LocalDateTime.now());
            metrics.put("daysWithEditor", daysWithEditor);
        }

        List<ReportStateHistory> history = historyRepository.findByReportOrderByChangedAtAsc(report);
        List<Map<String, Object>> stateTimings = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            ReportStateHistory entry = history.get(i);
            Map<String, Object> timing = new LinkedHashMap<>();
            timing.put("state", entry.getToState().getName());
            timing.put("enteredAt", entry.getChangedAt());

            if (i + 1 < history.size()) {
                LocalDateTime exitedAt = history.get(i + 1).getChangedAt();
                timing.put("exitedAt", exitedAt);
                long hours = ChronoUnit.HOURS.between(entry.getChangedAt(), exitedAt);
                timing.put("hoursInState", hours);
            } else {
                long hours = ChronoUnit.HOURS.between(entry.getChangedAt(), LocalDateTime.now());
                timing.put("hoursInState", hours);
                timing.put("current", true);
            }
            stateTimings.add(timing);
        }
        metrics.put("stateTimings", stateTimings);

        long totalHours = ChronoUnit.HOURS.between(report.getCreatedAt(), LocalDateTime.now());
        metrics.put("totalHoursElapsed", totalHours);

        return metrics;
    }

    public List<AuditReport> getReportsByEditor(Editor editor) {
        return reportRepository.findByEditor(editor);
    }
}

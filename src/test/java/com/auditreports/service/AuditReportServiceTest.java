package com.auditreports.service;

import com.auditreports.model.*;
import com.auditreports.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditReportServiceTest {

    @Mock private AuditReportRepository reportRepository;
    @Mock private EditorRepository editorRepository;
    @Mock private ReportStateRepository stateRepository;
    @Mock private StateTransitionRepository transitionRepository;
    @Mock private ReportStateHistoryRepository historyRepository;

    @InjectMocks
    private AuditReportService service;

    private ReportState queuedState;
    private ReportState assignedState;
    private ReportState editState;
    private Editor editor;
    private AuditReport report;

    @BeforeEach
    void setUp() {
        queuedState = new ReportState("Queued", 1, true, false);
        queuedState.setId(1L);

        assignedState = new ReportState("Assigned", 2, false, false);
        assignedState.setId(2L);

        editState = new ReportState("Initial Edit", 3, false, false);
        editState.setId(3L);

        editor = new Editor("Alice Johnson", "alice@example.com", "alice@example.com");
        editor.setId(1L);

        report = new AuditReport();
        report.setId(1L);
        report.setTitle("FY2026 Financial Audit");
        report.setRegion("Northeast");
        report.setReportPath("\\\\server\\reports\\fy2026.docx");
        report.setCurrentState(queuedState);
        report.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createReport_setsInitialStateAndCreatesHistory() {
        when(stateRepository.findByIsInitialTrue()).thenReturn(Optional.of(queuedState));
        when(reportRepository.save(any(AuditReport.class))).thenAnswer(inv -> {
            AuditReport r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(historyRepository.save(any(ReportStateHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditReport newReport = new AuditReport();
        newReport.setTitle("Test Report");
        newReport.setRegion("West");
        newReport.setReportPath("\\\\server\\test.docx");

        AuditReport result = service.createReport(newReport);

        assertEquals(queuedState, result.getCurrentState());
        verify(historyRepository).save(any(ReportStateHistory.class));
    }

    @Test
    void createReport_failsWhenNoInitialState() {
        when(stateRepository.findByIsInitialTrue()).thenReturn(Optional.empty());

        AuditReport newReport = new AuditReport();
        newReport.setTitle("Test");
        newReport.setRegion("East");
        newReport.setReportPath("\\\\path");

        assertThrows(IllegalStateException.class, () -> service.createReport(newReport));
    }

    @Test
    void assignEditor_setsEditorAndTimestamp() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(editorRepository.findById(1L)).thenReturn(Optional.of(editor));
        when(reportRepository.save(any(AuditReport.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditReport result = service.assignEditor(1L, 1L);

        assertEquals(editor, result.getEditor());
        assertNotNull(result.getAssignedAt());
    }

    @Test
    void assignEditor_failsWhenAlreadyAssigned() {
        report.setEditor(editor);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThrows(IllegalStateException.class, () -> service.assignEditor(1L, 2L));
        verify(reportRepository, never()).save(any());
    }

    @Test
    void transitionState_validTransition() {
        StateTransition transition = new StateTransition(queuedState, assignedState);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(stateRepository.findById(2L)).thenReturn(Optional.of(assignedState));
        when(transitionRepository.findByFromStateAndToState(queuedState, assignedState))
                .thenReturn(Optional.of(transition));
        when(historyRepository.save(any(ReportStateHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reportRepository.save(any(AuditReport.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditReport result = service.transitionState(1L, 2L, "admin");

        assertEquals(assignedState, result.getCurrentState());
        verify(historyRepository).save(any(ReportStateHistory.class));
    }

    @Test
    void transitionState_invalidTransition() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(stateRepository.findById(3L)).thenReturn(Optional.of(editState));
        when(transitionRepository.findByFromStateAndToState(queuedState, editState))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.transitionState(1L, 3L, "admin"));
        verify(reportRepository, never()).save(any());
    }

    @Test
    void getQueue_returnsUnassignedReports() {
        when(reportRepository.findByEditorIsNull()).thenReturn(List.of(report));

        List<AuditReport> queue = service.getQueue();

        assertEquals(1, queue.size());
        assertNull(queue.get(0).getEditor());
    }

    @Test
    void getReportMetrics_computesDaysWithEditor() {
        report.setEditor(editor);
        report.setAssignedAt(LocalDateTime.now().minusDays(5));
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(historyRepository.findByReportOrderByChangedAtAsc(report)).thenReturn(List.of());

        var metrics = service.getReportMetrics(1L);

        assertEquals(5L, metrics.get("daysWithEditor"));
    }
}

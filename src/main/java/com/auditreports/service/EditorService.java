package com.auditreports.service;

import com.auditreports.model.AuditReport;
import com.auditreports.model.Editor;
import com.auditreports.repository.AuditReportRepository;
import com.auditreports.repository.EditorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class EditorService {

    private final EditorRepository editorRepository;
    private final AuditReportRepository reportRepository;

    public EditorService(EditorRepository editorRepository, AuditReportRepository reportRepository) {
        this.editorRepository = editorRepository;
        this.reportRepository = reportRepository;
    }

    public List<Editor> getAllEditors() {
        return editorRepository.findAll();
    }

    public Editor getEditor(Long id) {
        return editorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Editor not found: " + id));
    }

    public Editor createEditor(Editor editor) {
        if (editorRepository.findByEmail(editor.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Editor with email '" + editor.getEmail() + "' already exists");
        }
        if (editor.getCalendarId() == null || editor.getCalendarId().isBlank()) {
            editor.setCalendarId(editor.getEmail());
        }
        return editorRepository.save(editor);
    }

    public Map<String, Object> getEditorWorkload(Long editorId) {
        Editor editor = getEditor(editorId);
        List<AuditReport> reports = reportRepository.findByEditor(editor);

        Map<String, Object> workload = new LinkedHashMap<>();
        workload.put("editor", editor);
        workload.put("totalReports", reports.size());

        List<Map<String, Object>> reportDetails = new ArrayList<>();
        for (AuditReport report : reports) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("reportId", report.getId());
            detail.put("title", report.getTitle());
            detail.put("currentState", report.getCurrentState().getName());
            if (report.getAssignedAt() != null) {
                long days = ChronoUnit.DAYS.between(report.getAssignedAt(), LocalDateTime.now());
                detail.put("daysAssigned", days);
            }
            reportDetails.add(detail);
        }
        workload.put("reports", reportDetails);

        return workload;
    }
}

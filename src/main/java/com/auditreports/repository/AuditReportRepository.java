package com.auditreports.repository;

import com.auditreports.model.AuditReport;
import com.auditreports.model.Editor;
import com.auditreports.model.ReportState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditReportRepository extends JpaRepository<AuditReport, Long> {

    List<AuditReport> findByEditorIsNull();

    List<AuditReport> findByEditor(Editor editor);

    List<AuditReport> findByCurrentState(ReportState state);

    List<AuditReport> findByRegion(String region);

    boolean existsByCurrentState(ReportState state);
}

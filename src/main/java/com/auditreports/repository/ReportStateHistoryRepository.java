package com.auditreports.repository;

import com.auditreports.model.AuditReport;
import com.auditreports.model.ReportStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportStateHistoryRepository extends JpaRepository<ReportStateHistory, Long> {

    List<ReportStateHistory> findByReportOrderByChangedAtAsc(AuditReport report);
}

package com.auditreports.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_state_history")
public class ReportStateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private AuditReport report;

    @ManyToOne
    @JoinColumn(name = "from_state_id")
    private ReportState fromState;

    @ManyToOne(optional = false)
    @JoinColumn(name = "to_state_id", nullable = false)
    private ReportState toState;

    private String changedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
    }

    public ReportStateHistory() {}

    public ReportStateHistory(AuditReport report, ReportState fromState, ReportState toState, String changedBy) {
        this.report = report;
        this.fromState = fromState;
        this.toState = toState;
        this.changedBy = changedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AuditReport getReport() { return report; }
    public void setReport(AuditReport report) { this.report = report; }

    public ReportState getFromState() { return fromState; }
    public void setFromState(ReportState fromState) { this.fromState = fromState; }

    public ReportState getToState() { return toState; }
    public void setToState(ReportState toState) { this.toState = toState; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}

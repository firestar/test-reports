package com.auditreports.model;

import jakarta.persistence.*;

@Entity
@Table(name = "state_transitions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"from_state_id", "to_state_id"})
})
public class StateTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "from_state_id", nullable = false)
    private ReportState fromState;

    @ManyToOne(optional = false)
    @JoinColumn(name = "to_state_id", nullable = false)
    private ReportState toState;

    public StateTransition() {}

    public StateTransition(ReportState fromState, ReportState toState) {
        this.fromState = fromState;
        this.toState = toState;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ReportState getFromState() { return fromState; }
    public void setFromState(ReportState fromState) { this.fromState = fromState; }

    public ReportState getToState() { return toState; }
    public void setToState(ReportState toState) { this.toState = toState; }
}

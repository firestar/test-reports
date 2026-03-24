package com.auditreports.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "report_states")
public class ReportState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private Boolean isInitial = false;

    @Column(nullable = false)
    private Boolean isTerminal = false;

    public ReportState() {}

    public ReportState(String name, Integer displayOrder, Boolean isInitial, Boolean isTerminal) {
        this.name = name;
        this.displayOrder = displayOrder;
        this.isInitial = isInitial;
        this.isTerminal = isTerminal;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Boolean getIsInitial() { return isInitial; }
    public void setIsInitial(Boolean isInitial) { this.isInitial = isInitial; }

    public Boolean getIsTerminal() { return isTerminal; }
    public void setIsTerminal(Boolean isTerminal) { this.isTerminal = isTerminal; }
}

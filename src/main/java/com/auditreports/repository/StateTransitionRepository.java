package com.auditreports.repository;

import com.auditreports.model.ReportState;
import com.auditreports.model.StateTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StateTransitionRepository extends JpaRepository<StateTransition, Long> {

    List<StateTransition> findByFromState(ReportState fromState);

    Optional<StateTransition> findByFromStateAndToState(ReportState fromState, ReportState toState);

    boolean existsByFromStateOrToState(ReportState fromState, ReportState toState);
}

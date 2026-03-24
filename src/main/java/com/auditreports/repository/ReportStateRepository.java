package com.auditreports.repository;

import com.auditreports.model.ReportState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportStateRepository extends JpaRepository<ReportState, Long> {

    Optional<ReportState> findByName(String name);

    Optional<ReportState> findByIsInitialTrue();

    List<ReportState> findAllByOrderByDisplayOrderAsc();
}

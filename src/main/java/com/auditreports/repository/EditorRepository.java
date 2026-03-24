package com.auditreports.repository;

import com.auditreports.model.Editor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EditorRepository extends JpaRepository<Editor, Long> {

    Optional<Editor> findByEmail(String email);
}

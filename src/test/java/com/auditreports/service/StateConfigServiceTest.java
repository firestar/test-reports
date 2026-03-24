package com.auditreports.service;

import com.auditreports.model.ReportState;
import com.auditreports.model.StateTransition;
import com.auditreports.repository.AuditReportRepository;
import com.auditreports.repository.ReportStateRepository;
import com.auditreports.repository.StateTransitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateConfigServiceTest {

    @Mock private ReportStateRepository stateRepository;
    @Mock private StateTransitionRepository transitionRepository;
    @Mock private AuditReportRepository reportRepository;

    @InjectMocks
    private StateConfigService service;

    private ReportState queued;
    private ReportState assigned;

    @BeforeEach
    void setUp() {
        queued = new ReportState("Queued", 1, true, false);
        queued.setId(1L);
        assigned = new ReportState("Assigned", 2, false, false);
        assigned.setId(2L);
    }

    @Test
    void createState_success() {
        ReportState newState = new ReportState("Review", 3, false, false);
        when(stateRepository.findByName("Review")).thenReturn(Optional.empty());
        when(stateRepository.save(any(ReportState.class))).thenAnswer(inv -> {
            ReportState s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        ReportState result = service.createState(newState);

        assertEquals("Review", result.getName());
        verify(stateRepository).save(newState);
    }

    @Test
    void createState_duplicateName_fails() {
        ReportState newState = new ReportState("Queued", 1, true, false);
        when(stateRepository.findByName("Queued")).thenReturn(Optional.of(queued));

        assertThrows(IllegalArgumentException.class, () -> service.createState(newState));
    }

    @Test
    void deleteState_inUse_fails() {
        when(stateRepository.findById(1L)).thenReturn(Optional.of(queued));
        when(reportRepository.existsByCurrentState(queued)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteState(1L));
        verify(stateRepository, never()).delete(any());
    }

    @Test
    void deleteState_referencedInTransitions_fails() {
        when(stateRepository.findById(1L)).thenReturn(Optional.of(queued));
        when(reportRepository.existsByCurrentState(queued)).thenReturn(false);
        when(transitionRepository.existsByFromStateOrToState(queued, queued)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteState(1L));
        verify(stateRepository, never()).delete(any());
    }

    @Test
    void deleteState_unused_success() {
        when(stateRepository.findById(1L)).thenReturn(Optional.of(queued));
        when(reportRepository.existsByCurrentState(queued)).thenReturn(false);
        when(transitionRepository.existsByFromStateOrToState(queued, queued)).thenReturn(false);

        service.deleteState(1L);

        verify(stateRepository).delete(queued);
    }

    @Test
    void createTransition_success() {
        when(stateRepository.findById(1L)).thenReturn(Optional.of(queued));
        when(stateRepository.findById(2L)).thenReturn(Optional.of(assigned));
        when(transitionRepository.findByFromStateAndToState(queued, assigned)).thenReturn(Optional.empty());
        when(transitionRepository.save(any(StateTransition.class))).thenAnswer(inv -> {
            StateTransition t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        StateTransition result = service.createTransition(1L, 2L);

        assertEquals(queued, result.getFromState());
        assertEquals(assigned, result.getToState());
    }

    @Test
    void createTransition_duplicate_fails() {
        StateTransition existing = new StateTransition(queued, assigned);
        when(stateRepository.findById(1L)).thenReturn(Optional.of(queued));
        when(stateRepository.findById(2L)).thenReturn(Optional.of(assigned));
        when(transitionRepository.findByFromStateAndToState(queued, assigned)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.createTransition(1L, 2L));
    }

    @Test
    void getAllStates_orderedByDisplayOrder() {
        when(stateRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(queued, assigned));

        List<ReportState> states = service.getAllStates();

        assertEquals(2, states.size());
        assertEquals("Queued", states.get(0).getName());
        assertEquals("Assigned", states.get(1).getName());
    }
}
